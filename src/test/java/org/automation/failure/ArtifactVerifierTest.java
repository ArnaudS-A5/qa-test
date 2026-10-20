package org.automation.failure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PHASE 1 — VÉRIFICATEUR d'artefacts KO. Relit {@code target/qa-results/KO__.../} produit par
 * {@link ArtifactProducerTest} et valide :
 * <ul>
 *   <li><b>T18</b> — le dossier {@code KO__{ENV}__...} existe avec {@code ERROR.log}, {@code FAIL.log}
 *       et un {@code {step}.html}.</li>
 *   <li><b>T19</b> — le dump {@code .html} est le DOM réel de la page à l'échec (contient les marqueurs
 *       de {@code form.html}).</li>
 *   <li><b>T20</b> — la {@code value} de l'{@code input[type=password]} est blanchie (aucun clair).</li>
 *   <li><b>T23</b> — la seule présence des artefacts prouve que les 3 services (dont TestFailureManager
 *       et RawDomCaptureListener) sont actifs sans aucune annotation côté test.</li>
 * </ul>
 *
 * <p><b>Strict</b> : artefact attendu manquant ⇒ échec (jamais de skip silencieux).
 * {@code @Disabled} car dépend du run préalable du producteur ; réactiver via
 * {@code -Djunit.jupiter.conditions.deactivate='*DisabledCondition'}.
 */
@Disabled("Verificateur d'artefacts : lancer APRES le producteur. Reactiver via "
        + "-Djunit.jupiter.conditions.deactivate='*DisabledCondition'.")
class ArtifactVerifierTest {

    private static final Path RESULTS = Path.of("target", "qa-results");
    private static final String CLEAR_PASSWORD = "SuperSecretPassword!";

    private Path latestKoFolderForProducer() throws IOException {
        if (!Files.isDirectory(RESULTS)) {
            fail("T18 : dossier target/qa-results absent — le producteur n'a pas tourne.");
        }
        try (Stream<Path> dirs = Files.list(RESULTS)) {
            return dirs.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("KO__"))
                    .filter(p -> p.getFileName().toString().contains("password")
                            || p.getFileName().toString().contains("Echec_volontaire"))
                    .max((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .orElseGet(() -> {
                        fail("T18 : aucun dossier KO__ du producteur trouve sous target/qa-results.");
                        return null;
                    });
        }
    }

    @Test
    @DisplayName("T18 - PASS - artefacts KO complets (T18, T19, T20, T23), DOM reel, password blanchi")
    void T18_artifacts_complete() throws IOException {
        Path folder = latestKoFolderForProducer();
        System.out.println("[T18] INFO : dossier KO analyse = " + folder);

        // T18 : 3 fichiers attendus.
        Path errorLog = folder.resolve("ERROR.log");
        Path failLog = folder.resolve("FAIL.log");
        assertTrue(Files.exists(errorLog), "T18 : ERROR.log manquant dans " + folder);
        assertTrue(Files.exists(failLog), "T18 : FAIL.log manquant dans " + folder);

        Path html;
        try (Stream<Path> files = Files.list(folder)) {
            html = files.filter(p -> p.getFileName().toString().endsWith(".html"))
                    .findFirst()
                    .orElseGet(() -> {
                        fail("T18 : dump {step}.html manquant dans " + folder);
                        return null;
                    });
        }
        System.out.println("[T18] INFO : dump HTML present = " + html.getFileName());

        // T19 : DOM reel de form.html (non echappe, vrai markup).
        String dom = Files.readString(html);
        assertFalse(dom.isBlank(), "T19 : dump HTML vide");
        assertTrue(dom.contains("<html") || dom.contains("<HTML"),
                "T19 : le dump doit etre un vrai DOM <html> (pas la vue Prism echappee)");
        assertFalse(dom.contains("language-html"),
                "T19 : le dump ne doit PAS etre la vue 'code' Prism echappee");
        assertTrue(dom.contains("id=\"password\"") || dom.contains("id='password'"),
                "T19 : le dump doit contenir le DOM de form.html (input password)");

        // T20 : password blanchi — la valeur claire ne doit JAMAIS apparaitre.
        assertFalse(dom.contains(CLEAR_PASSWORD),
                "T20 : la valeur claire du password ne doit PAS apparaitre dans le dump HTML !");

        // T23 : la seule existence de ces artefacts (dossier + logs + dump DOM brut) prouve que
        // TestFailureManager ET RawDomCaptureListener sont actifs par presence du jar.
        String failContent = Files.readString(failLog);
        assertFalse(failContent.isBlank(), "T23/FAIL.log : contenu attendu");
        System.out.println("[T23] RESULTAT : services actifs (artefacts + dump DOM brut) confirmes"
                + " | aucune exception => cas passant confirme");
    }
}

