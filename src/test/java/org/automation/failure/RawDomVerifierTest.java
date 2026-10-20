package org.automation.failure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spike — VÉRIFICATEUR : relit les dossiers {@code KO__} produits par {@link RawDomProducerTest}
 * et valide les 3 points de la revue (option A, capture du DOM brut) :
 *
 * <ol>
 *   <li><b>Point 1</b> — {@code stepFailed} a capté un DOM <b>non vide</b> (driver vivant) ;</li>
 *   <li><b>Point 2</b> — ce DOM est <b>brut / rendable</b> (vrai {@code <html>…</html>} non échappé),
 *       PAS la vue source Prism échappée du repli ;</li>
 *   <li><b>Point 3</b> — {@code stepFailed} et {@code executionFinished} sur le même thread
 *       (pont {@code ThreadLocal}) même en parallèle : chaque artefact porte le DOM de SA page,
 *       sans fuite entre threads.</li>
 * </ol>
 *
 * <p><b>Rigueur</b> : on attend EXACTEMENT les deux artefacts LOGIN et CHECKBOXES, chacun avec son
 * dump HTML. Un artefact manquant (ex. capture /checkboxes ratée) fait <b>ÉCHOUER</b> le test —
 * jamais de skip silencieux. Aucun navigateur ici : on relit le disque.
 *
 * <p>{@code @Disabled} (à lancer après le producteur) :
 * <pre>
 * mvn test -Dtest=RawDomVerifierTest -Djunit.jupiter.conditions.deactivate='*DisabledCondition'
 * </pre>
 */
@Disabled("Spike capture DOM brut : à lancer après RawDomProducerTest, via "
        + "-Djunit.jupiter.conditions.deactivate='*DisabledCondition'. Cf. javadoc / README.")
class RawDomVerifierTest {

    private static final Path RESULTS = Path.of("target/qa-results");

    // Marqueurs Prism (= repli Serenity, NON souhaité ici) et DOM échappé.
    private static final String PRISM_MARKER = "language-html";
    private static final String ESCAPED_MARKER = "&lt;html";
    // Marqueurs uniques de chaque page (the-internet).
    private static final String LOGIN_MARKER = "id=\"login\"";
    private static final String CHECKBOXES_MARKER = "id=\"checkboxes\"";

    private static List<Path> koFolders;

    @BeforeAll
    static void loadArtifacts() throws IOException {
        koFolders = listKoFolders();
    }

    @Test
    @DisplayName("T19 - PASS - Pre-requis : les DEUX pages (LOGIN + CHECKBOXES) ont produit un dump HTML")
    void T19_dumps_present() {
        // Échoue (et n'IGNORE PAS) si un artefact manque : c'est le filet qui empêche
        // de "passer à côté" d'une capture ratée.
        assertNotNull(requireFolder("LOGIN"), "Artefact LOGIN absent — producteur lancé ?");
        assertNotNull(requireFolder("CHECKBOXES"), "Artefact CHECKBOXES absent — la capture DOM de /checkboxes a échoué");
        assertNotNull(htmlDump(requireFolder("LOGIN")), "Dump .html manquant côté LOGIN");
        assertNotNull(htmlDump(requireFolder("CHECKBOXES")), "Dump .html manquant côté CHECKBOXES");
    }

    @Test
    @DisplayName("T19 - PASS - Point 1 : chaque artefact contient un DOM non vide")
    void T19_dom_not_empty() {
        for (Path folder : expectedFolders()) {
            String html = readDump(folder);
            assertTrue(html.strip().length() > 200,
                    () -> "DOM trop court / vide pour " + folder.getFileName() + " (" + html.length() + " car.)");
        }
    }

    @Test
    @DisplayName("T19 - PASS - Point 2 : le DOM est brut/rendable, pas la vue Prism echappee (repli)")
    void T19_dom_raw() {
        for (Path folder : expectedFolders()) {
            String html = readDump(folder).toLowerCase();
            String who = folder.getFileName().toString();
            assertTrue(html.contains("<html") && html.contains("</html>"),() -> "DOM non rendable (balises <html> absentes) pour " + who);
            assertFalse(html.contains(PRISM_MARKER), () -> "Repli Prism détecté (capture DOM brut KO) pour " + who);
            assertFalse(html.contains(ESCAPED_MARKER), () -> "DOM échappé (&lt;html) détecté pour " + who);
        }
    }

    @Test
    @DisplayName("T19 - PASS - Point 3 : chaque artefact porte le DOM de SA page (pont ThreadLocal, meme en parallele)")
    void T19_dom_no_thread_leak() {
        String loginHtml = readDump(requireFolder("LOGIN"));
        assertTrue(loginHtml.contains(LOGIN_MARKER), "Le DOM du dossier LOGIN doit être celui de la page de login");
        assertFalse(loginHtml.contains(CHECKBOXES_MARKER), "Fuite : le DOM de checkboxes a contaminé l'artefact LOGIN (pont ThreadLocal cassé)");

        String checkboxesHtml = readDump(requireFolder("CHECKBOXES"));
        assertTrue(checkboxesHtml.contains(CHECKBOXES_MARKER), "Le DOM du dossier CHECKBOXES doit être celui de la page checkboxes");
        assertFalse(checkboxesHtml.contains(LOGIN_MARKER), "Fuite : le DOM de login a contaminé l'artefact CHECKBOXES (pont ThreadLocal cassé)");
    }

    // ---------------------------------------------------------------------------------------------

    /** Les deux dossiers attendus ; échoue si l'un manque (pas de skip). */
    private static List<Path> expectedFolders() {
        return List.of(requireFolder("LOGIN"), requireFolder("CHECKBOXES"));
    }

    private static Path requireFolder(String token) {
        Path folder = koFolders.stream()
                .filter(p -> p.getFileName().toString().toUpperCase().contains(token))
                .findFirst().orElse(null);
        assertNotNull(folder, () -> "Aucun artefact KO pour la page " + token
                + " dans target/qa-results — lancer d'abord RawDomProducerTest (cf. README).");
        return folder;
    }

    private static List<Path> listKoFolders() throws IOException {
        if (!Files.isDirectory(RESULTS)) {
            return List.of();
        }
        try (var s = Files.list(RESULTS)) {
            return s.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("KO__"))
                    .toList();
        }
    }

    private static Path htmlDump(Path folder) {
        try (var s = Files.list(folder)) {
            return s.filter(p -> p.toString().endsWith(".html")).findFirst().orElse(null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String readDump(Path folder) {
        Path dump = htmlDump(folder);
        assertNotNull(dump, () -> "Pas de dump .html dans " + folder.getFileName());
        try {
            return Files.readString(dump);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
