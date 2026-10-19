#!/usr/bin/env python3
"""Jenkins CI: build the socle, run its browser harness, analyse, then publish."""
import argparse
import functools
import hashlib
import http.server
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import threading
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

WORK = Path(os.environ.get("WORKSPACE", Path(__file__).resolve().parents[2])).resolve()
TEMPLATE = WORK / "qa-template"
TESTS = WORK / "qa-test"
REPORTS = WORK / "reports"
COVERAGE = WORK / "coverage" / "jacoco.exec"
SONAR = os.environ.get("SONAR_URL", "http://sonarqube.home.arpa:9000").rstrip("/")
KEYS = {
    "template": "ArnaudS-A5_qa-template_a3e1a8d8-dbd2-4fa7-8567-43f6dbb28964",
    "tests": "ArnaudS-A5_qa-test_b2fd8bbf-79cd-45c7-9ded-f07ba0f013df",
}
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def prepare():
    """Use develop immediately; stop reapplying a CI patch once its PR is merged."""
    revisions = {}
    for project in (TEMPLATE, TESTS):
        patch = Path(__file__).parent / (project.name + ".patch")
        check = subprocess.run(["git", "apply", "--check", str(patch)], cwd=project,
                               capture_output=True)
        if check.returncode == 0:
            subprocess.run(["git", "apply", str(patch)], cwd=project, check=True)
        else:
            reverse = subprocess.run(["git", "apply", "--reverse", "--check", str(patch)],
                                     cwd=project, capture_output=True)
            if reverse.returncode:
                raise SystemExit("CI configuration conflicts with branch in " + project.name)
        revisions[project.name] = {
            "commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=project, text=True).strip(),
            "ciPatchSha256": hashlib.sha256(patch.read_bytes()).hexdigest(),
        }
    REPORTS.mkdir(exist_ok=True)
    (REPORTS / "source-revisions.json").write_text(json.dumps(revisions, indent=2))
    print("Sources and versioned CI configuration:", json.dumps(revisions), flush=True)


def maven(project, *args, scan=False):
    """Keep Maven credentials outside the checkout, even on failed builds."""
    tmp = Path(os.environ.get("WORKSPACE_TMP", str(WORK) + "@tmp"))
    tmp.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="maven-auth-", dir=tmp) as directory:
        settings = Path(directory) / "settings.xml"
        root = ET.Element("settings", xmlns="http://maven.apache.org/SETTINGS/1.2.0")
        servers = ET.SubElement(root, "servers")
        for server_id in ("artifactory-releases", "artifactory-snapshots"):
            server = ET.SubElement(servers, "server")
            for name, value in (("id", server_id),
                                ("username", os.environ.get("ARTIFACTORY_USER", "admin")),
                                ("password", os.environ.get("ARTIFACTORY_TOKEN", ""))):
                ET.SubElement(server, name).text = value
        ET.ElementTree(root).write(settings, encoding="utf-8", xml_declaration=True)
        settings.chmod(0o600)
        command = [str(TEMPLATE / "mvnw"), "-B", "-ntp", "-s", str(settings),
                   "-f", str(project / "pom.xml"), "-Pci",
                   "-Dmaven.repo.local=" + str(WORK / ".m2"),
                   "-Dqa.coverage.dataFile=" + str(COVERAGE), *args]
        env = os.environ.copy()
        if scan:
            env["JAVA_HOME"] = env["SONAR_JAVA_HOME"]
            env["PATH"] = env["JAVA_HOME"] + "/bin:" + env["PATH"]
        print("Maven:", project.name, " ".join(args), flush=True)
        return subprocess.run(command, cwd=project, env=env).returncode


def require_success(code):
    if code:
        raise SystemExit(code)


def save_tests(project, label):
    for source in project.rglob("target/surefire-reports"):
        destination = REPORTS / label / source.relative_to(project)
        destination.mkdir(parents=True, exist_ok=True)
        for item in source.glob("TEST-*.xml"):
            shutil.copy2(item, destination / item.name)


def build():
    COVERAGE.parent.mkdir(parents=True, exist_ok=True)
    COVERAGE.unlink(missing_ok=True)
    code = maven(TEMPLATE, "clean", "install")
    save_tests(TEMPLATE, "socle")
    require_success(code)


class FixtureHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *_):
        pass


def grid():
    browser = os.environ.get("BROWSER", "chrome")
    if browser not in ("chrome", "firefox", "edge"):
        raise SystemExit("Unsupported browser")
    bind = os.environ.get("FIXTURE_BIND", "192.168.1.50")
    port = int(os.environ.get("FIXTURE_PORT", "8765"))
    handler = functools.partial(FixtureHandler, directory=str(TESTS / "src/test/resources/fixtures"))
    server = http.server.ThreadingHTTPServer((bind, port), handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    common = ["-Dwebdriver.driver=remote",
              "-Dwebdriver.remote.url=" + os.environ.get("GRID_URL", "http://grid.home.arpa:4444"),
              "-Dwebdriver.remote.driver=" + browser, "-Dwebdriver.autodownload=false",
              "-Dheadless.mode=true", "-Dqa.fixtures.baseUrl=http://" + bind + ":" + str(port),
              "-Dwebdriver.base.url=" + os.environ.get("BASE_URL", "https://the-internet.herokuapp.com")]
    campaign = [
        ("01-fixtures", ["clean", "test", "-Dqa.sync.timeout.short=1s", "-Dqa.sync.timeout.medium=2s",
                         "-Dqa.sync.timeout.long=3s", "-Dqa.sync.pageLoadTimeout=5s",
                         "-Dtest=BornesLimitesTest,DiagnosticTest,FormEtSecretTest,PageLoadEtConfigTest#T6_page_load_timeout,!BornesLimitesTest#L5_medium_vraies_valeurs"]),
        ("02-medium", ["test", "-Dtest=BornesLimitesTest#L5_medium_vraies_valeurs"]),
        ("03-precedence", ["test", "-Dtest=PageLoadEtConfigTest#T24_precedence_D", "-Denvironment=override-D"]),
        ("04-site", ["test", "-Dtest=AuthenticationTest,DynamicWaitTest", "-Dqa.sync.timeout.short=1s"]),
        ("05-throttling", ["test", "-Dtest=ThrottledLoadTest", "-Dqa.sync.pageLoadTimeout=2s"]),
    ]
    failures = []
    try:
        for label, command in campaign:
            for report in (TESTS / "target/surefire-reports").glob("TEST-*.xml"):
                report.unlink()
            code = maven(TESTS, *command, *common)
            save_tests(TESTS, label)
            if code:
                failures.append(label)
        if failures:
            raise SystemExit("Failed test runs: " + ", ".join(failures))
        require_success(maven(TESTS, "-DskipTests", "package", "serenity:aggregate"))
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=5)


def coverage():
    # The same exec file receives the unit tests and the harness loading qa-socle.
    require_success(maven(TEMPLATE, "-pl", "qa-socle", "org.jacoco:jacoco-maven-plugin:0.8.15:report"))
    report = TEMPLATE / "qa-socle/target/site/jacoco/jacoco.xml"
    if not report.is_file():
        raise SystemExit("Missing JaCoCo report")
    root = ET.parse(report).getroot()
    counters = {e.get("type"): {"covered": int(e.get("covered")), "missed": int(e.get("missed"))}
                for e in root.findall("counter")}
    print("JaCoCo measured coverage:", json.dumps(counters), flush=True)
    (COVERAGE.parent / "counters.json").write_text(json.dumps(counters, indent=2))


def analyse(name):
    project = TEMPLATE if name == "template" else TESTS
    args = ["org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar",
            "-Dsonar.host.url=" + SONAR, "-Dsonar.projectKey=" + KEYS[name],
            "-Dsonar.scanner.skipJreProvisioning=true"]
    if name == "template":
        args += ["-Dsonar.coverage.jacoco.xmlReportPaths=" +
                 str(TEMPLATE / "qa-socle/target/site/jacoco/jacoco.xml")]
    require_success(maven(project, *args, scan=True))


def api(path, **params):
    url = SONAR + path + "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=30) as response:
        return json.load(response)


def gate(name):
    project = TEMPLATE if name == "template" else TESTS
    candidates = list(project.glob("target/sonar/report-task.txt"))
    if len(candidates) != 1:
        raise RuntimeError("Missing SonarScanner task report for " + name)
    task = dict(line.split("=", 1) for line in candidates[0].read_text().splitlines() if "=" in line)
    deadline = time.monotonic() + 600
    while time.monotonic() < deadline:
        result = api("/api/ce/task", id=task["ceTaskId"])["task"]
        if result["status"] == "SUCCESS":
            break
        if result["status"] in ("FAILED", "CANCELED"):
            raise RuntimeError("Sonar analysis " + result["status"])
        time.sleep(3)
    else:
        raise RuntimeError("Sonar analysis timed out")
    result = api("/api/qualitygates/project_status", analysisId=result["analysisId"])["projectStatus"]
    print("Quality Gate", name, json.dumps(result), flush=True)
    REPORTS.mkdir(exist_ok=True)
    (REPORTS / ("quality-gate-" + name + ".json")).write_text(json.dumps(result, indent=2))
    if name == "template":
        evaluated = {c["metricKey"]: c for c in result.get("conditions", [])}
        if "coverage" not in evaluated:
            raise RuntimeError("Overall coverage condition was not evaluated")
    if result["status"] != "OK":
        raise RuntimeError("Quality Gate failed for " + name)


def gates():
    failures = []
    for name in KEYS:
        try:
            gate(name)
        except Exception as error:
            failures.append(str(error))
    if failures:
        raise SystemExit("; ".join(failures))


def deploy():
    for project in (TEMPLATE, TESTS):
        version = ET.parse(project / "pom.xml").findtext("m:version", namespaces=NS)
        if not version:
            raise SystemExit("Cannot determine project version")
        if not version.endswith("-SNAPSHOT") and os.environ.get("ALLOW_RELEASE", "false") != "true":
            raise SystemExit("Release deployment requires ALLOW_RELEASE=true")
    for project in (TEMPLATE, TESTS):
        require_success(maven(project, "-DskipTests", "deploy"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("step", choices=["prepare", "build", "grid", "coverage", "sonar-template", "sonar-tests", "gates", "deploy"])
    step = parser.parse_args().step
    if step.startswith("sonar-"):
        analyse(step.removeprefix("sonar-"))
    else:
        globals()[step]()
