pipeline {
    agent { label 'grid.home.arpa' }
    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        timestamps()
    }
    parameters {
        string(name: 'TEMPLATE_BRANCH', defaultValue: 'develop', description: 'Branche du socle qa-template')
        string(name: 'TEST_BRANCH', defaultValue: 'develop', description: 'Branche du harnais qa-test')
        choice(name: 'JDK_VERSION', choices: ['17', '21', '25'], description: 'JDK du build ; compilation cible Java 17')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge'], description: 'Navigateur distant sur le Grid')
        string(name: 'BASE_URL', defaultValue: 'https://the-internet.herokuapp.com', description: 'Application cible des tests de démonstration')
        booleanParam(name: 'PUBLISH_ARTIFACTS', defaultValue: true, description: 'Publier après succès des tests et des Quality Gates')
        booleanParam(name: 'ALLOW_RELEASE', defaultValue: false, description: 'Autoriser une version sans suffixe SNAPSHOT')
    }
    environment {
        GRID_URL = 'http://grid.home.arpa:4444'
        SONAR_URL = 'http://sonarqube.home.arpa:9000'
        FIXTURE_BIND = '192.168.1.50'
        FIXTURE_PORT = '8765'
        BROWSER = "${params.BROWSER}"
        BASE_URL = "${params.BASE_URL}"
        ALLOW_RELEASE = "${params.ALLOW_RELEASE}"
    }
    stages {
        stage('Sources GitHub') {
            steps {
                dir('ci-config') {
                    deleteDir()
                    checkout scm
                }
                sh 'mkdir -p ci; cp ci-config/ci/*.py ci-config/ci/*.patch ci/'
                dir('reports') { deleteDir() }
                dir('qa-template') {
                    deleteDir()
                    git branch: params.TEMPLATE_BRANCH, url: 'https://github.com/ArnaudS-A5/qa-template.git'
                }
                dir('qa-test') {
                    deleteDir()
                    git branch: params.TEST_BRANCH, url: 'https://github.com/ArnaudS-A5/qa-test.git'
                }
                sh 'python3 ci/pipeline.py prepare'
            }
        }
        stage('JDK et prérequis') {
            steps {
                script {
                    env.JAVA_HOME = tool(name: "jdk${params.JDK_VERSION}", type: 'hudson.model.JDK')
                    env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
                    env.SONAR_JAVA_HOME = tool(name: 'jdk21', type: 'hudson.model.JDK')
                }
                sh 'java -version && javac -version && python3 --version'
                sh 'curl --fail --silent --show-error "$GRID_URL/status" > grid-status.json'
                sh '''python3 - <<'PY'
import json
with open('grid-status.json') as f:
    status = json.load(f)['value']
if not status.get('ready'):
    raise SystemExit('Selenium Grid is not ready')
print('Selenium Grid ready:', len(status.get('nodes', [])), 'nodes')
PY'''
            }
        }
        stage('Build et tests unitaires du socle') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactory-ci', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_TOKEN')]) {
                    sh 'python3 ci/pipeline.py build'
                }
            }
        }
        stage('Tests sur Selenium Grid') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE', catchInterruptions: false) {
                    withCredentials([usernamePassword(credentialsId: 'artifactory-ci', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_TOKEN')]) {
                        sh 'python3 ci/pipeline.py grid'
                    }
                }
            }
        }
        stage('Couverture et analyses SonarQube') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory-ci', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_TOKEN'),
                    string(credentialsId: 'sonarqube-ci', variable: 'SONAR_TOKEN')
                ]) {
                    sh 'python3 ci/pipeline.py coverage'
                    sh 'python3 ci/pipeline.py sonar-template'
                    sh 'python3 ci/pipeline.py sonar-tests'
                }
            }
        }
        stage('Quality Gates : couverture globale ≥ 80 %') {
            steps {
                sh 'python3 ci/pipeline.py gates'
            }
        }
        stage('Publication Artifactory') {
            when { expression { params.PUBLISH_ARTIFACTS && currentBuild.currentResult == 'SUCCESS' } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactory-ci', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_TOKEN')]) {
                    sh 'python3 ci/pipeline.py deploy'
                }
            }
        }
    }
    post {
        always {
            junit testResults: 'reports/**/TEST-*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'reports/**,coverage/counters.json,grid-status.json,qa-template/**/target/*.jar,qa-template/**/target/site/jacoco/**,qa-test/target/*.jar,qa-test/target/site/serenity/**,qa-test/target/qa-results/**', allowEmptyArchive: true
        }
    }
}
