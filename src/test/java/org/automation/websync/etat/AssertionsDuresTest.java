package org.automation.websync.etat;

import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.EtatSteps;
import org.automation.support.Timeouts;
import org.automation.support.Transitions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.time.Duration;

import static org.automation.support.BornesProfil.TOLERANCE_MS;
import static org.automation.support.BornesProfil.depuis;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — assertions <b>dures</b> {@code should*} ({@code qa.assertions.soft=false}), sur la
 * fixture {@code websync/etats.html}. Complète T16 ({@code shouldBeVisible}) et T34
 * ({@code shouldContainSelectedOption}).
 *
 * <p>Chaque test vérifie le contrat entier d'une méthode :
 * <ul>
 *   <li>cas passant, en <b>étape</b> : le {@code should*} passe sur le bon état (capture dans le rapport) ;</li>
 *   <li>cas non passant : une étape de <b>constat</b> montre d'abord le mauvais état (capture, et préalable : si la
 *       fixture changeait, le test le dirait) ; puis le {@code should*} lève une {@code AssertionError} citant le
 *       locator, appel direct hors étape (chronométré, et une étape avalerait l'exception).</li>
 * </ul>
 * Une assertion attend la condition jusqu'au timeout du profil (SHORT, 5 s par défaut) et <b>pas au-delà</b> : une
 * seule horloge (point critique C8), d'où la borne haute à timeout + 2 s. Seule {@code shouldBeCurrentlyVisible}
 * échoue sans attendre.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class AssertionsDuresTest {

    private static final String FIXTURE = "websync/etats.html";
    private static final By VISIBLE = By.id("visible");
    private static final By CACHE = By.id("cache");
    private static final By DESACTIVE = By.id("desactive");
    private static final By COCHE = By.id("coche");
    private static final By NON_COCHE = By.id("non-coche");
    private static final By TEXTE = By.id("texte");
    private static final By ABSENT = By.id("absent");

    /** Un échec sans attente se mesure en millisecondes. */
    private static final long ECHEC_IMMEDIAT_MS = 1_000;

    @Steps
    EtatSteps etat;

    /** Ouvre la fixture (étape) et rend le {@code WebSync} direct, au profil SHORT, des cas non passants. */
    private WebSync ouvrirEtats() {
        etat.ouvrir(FIXTURE);
        return new WebSync(SyncTimeout.SHORT);
    }

    /** Timeout SHORT au défaut du socle (groupe {@code defaut}), en ms. */
    private static long timeoutMs() {
        return Timeouts.defaultOf(SyncTimeout.SHORT).toMillis();
    }

    /** Borne haute d'un échec avec attente : au-delà, une seconde horloge serait à l'œuvre (C8). */
    private static long borneHauteMs() {
        return timeoutMs() + Timeouts.MARGE_COURTE.toMillis();
    }

    // ---- T48 : shouldBeCurrentlyVisible (sans attente) ----
    @Test
    @DisplayName("T48 - shouldBeCurrentlyVisible : passe sur #visible, AssertionError immediate sur #cache")
    void T48_should_be_currently_visible() {
        WebSync web = ouvrirEtats();
        etat.verifierVisibleMaintenant(VISIBLE);

        assertFalse(etat.estVisibleMaintenant(CACHE), "T48 : constat, #cache n'est pas visible");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldBeCurrentlyVisible(CACHE),
                "T48 : doit echouer sur #cache (display:none)");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("By.id: cache"), "T48 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms < ECHEC_IMMEDIAT_MS, "T48 : echec immediat attendu (aucune attente), ms=" + ms);

        System.out.println("[T48] RESULTAT : #visible => aucune exception => cas passant confirme");
        System.out.println("[T48] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T49 : shouldNotBeVisible (attend la disparition) ----
    @Test
    @DisplayName("T49 - shouldNotBeVisible : passe sur #cache et sur un element qui disparait a 2 s, echoue sur #visible")
    void T49_should_not_be_visible() {
        WebSync web = ouvrirEtats();
        etat.verifierNonVisible(CACHE);

        assertTrue(etat.estVisible(VISIBLE), "T49 : constat, #visible est visible");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotBeVisible(VISIBLE),
                "T49 : doit echouer sur #visible, qui le reste");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("By.id: visible"), "T49 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T49 : echec au timeout SHORT, ms=" + ms);

        // Attente d'une disparition : chronometree, donc appel direct.
        long disparition = Transitions.ouvrir(Transitions.Scenario.DISPARITION, Duration.ofSeconds(2));
        long debut = System.nanoTime();
        assertDoesNotThrow(() -> web.shouldNotBeVisible(Transitions.CIBLE), "T49 : doit attendre la disparition a 2 s");
        long attente = depuis(debut);
        assertTrue(attente >= disparition - TOLERANCE_MS && attente < timeoutMs(),
                "T49 : passe apres avoir attendu la disparition (" + disparition + " ms), avant le timeout, ms=" + attente);

        System.out.println("[T49] RESULTAT : #cache => passe | disparition a " + disparition + " ms => passe en " + attente
                + " ms | aucune exception => cas passant confirme");
        System.out.println("[T49] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T50 : shouldBePresent ----
    @Test
    @DisplayName("T50 - shouldBePresent : passe sur #cache (present meme invisible), echoue sur #absent")
    void T50_should_be_present() {
        WebSync web = ouvrirEtats();
        etat.verifierPresent(CACHE);

        assertFalse(etat.estPresent(ABSENT), "T50 : constat, #absent n'est pas present");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldBePresent(ABSENT),
                "T50 : doit echouer sur #absent");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("By.id: absent"), "T50 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T50 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T50] RESULTAT : #cache => aucune exception => cas passant confirme");
        System.out.println("[T50] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T51 : shouldNotBePresent ----
    @Test
    @DisplayName("T51 - shouldNotBePresent : passe sur #absent, echoue sur #cache (present meme invisible)")
    void T51_should_not_be_present() {
        WebSync web = ouvrirEtats();
        etat.verifierAbsent(ABSENT);

        assertTrue(etat.estPresent(CACHE), "T51 : constat, #cache est present dans le DOM");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotBePresent(CACHE),
                "T51 : doit echouer sur #cache, present dans le DOM");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("By.id: cache"), "T51 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T51 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T51] RESULTAT : #absent => aucune exception => cas passant confirme");
        System.out.println("[T51] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T52 : shouldBeEnabled ----
    @Test
    @DisplayName("T52 - shouldBeEnabled : passe sur #visible, echoue sur #desactive")
    void T52_should_be_enabled() {
        WebSync web = ouvrirEtats();
        etat.verifierActive(VISIBLE);

        assertFalse(etat.estActive(DESACTIVE), "T52 : constat, #desactive n'est pas active");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldBeEnabled(DESACTIVE),
                "T52 : doit echouer sur #desactive");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("By.id: desactive"), "T52 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T52 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T52] RESULTAT : #visible => aucune exception => cas passant confirme");
        System.out.println("[T52] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T53 : shouldNotBeEnabled ----
    @Test
    @DisplayName("T53 - shouldNotBeEnabled : passe sur #desactive, echoue sur #visible")
    void T53_should_not_be_enabled() {
        WebSync web = ouvrirEtats();
        etat.verifierDesactive(DESACTIVE);

        assertTrue(etat.estActive(VISIBLE), "T53 : constat, #visible est active");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotBeEnabled(VISIBLE),
                "T53 : doit echouer sur #visible, actif");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("By.id: visible"), "T53 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T53 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T53] RESULTAT : #desactive => aucune exception => cas passant confirme");
        System.out.println("[T53] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T54 : shouldBeSelected ----
    @Test
    @DisplayName("T54 - shouldBeSelected : passe sur #coche, echoue sur #non-coche")
    void T54_should_be_selected() {
        WebSync web = ouvrirEtats();
        etat.verifierSelectionne(COCHE);

        assertFalse(etat.estSelectionne(NON_COCHE), "T54 : constat, #non-coche n'est pas coche");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldBeSelected(NON_COCHE),
                "T54 : doit echouer sur #non-coche");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("By.id: non-coche"), "T54 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T54 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T54] RESULTAT : #coche => aucune exception => cas passant confirme");
        System.out.println("[T54] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T55 : shouldNotBeSelected ----
    @Test
    @DisplayName("T55 - shouldNotBeSelected : passe sur #non-coche, echoue sur #coche")
    void T55_should_not_be_selected() {
        WebSync web = ouvrirEtats();
        etat.verifierNonSelectionne(NON_COCHE);

        assertTrue(etat.estSelectionne(COCHE), "T55 : constat, #coche est coche");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotBeSelected(COCHE),
                "T55 : doit echouer sur #coche");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("By.id: coche"), "T55 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T55 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T55] RESULTAT : #non-coche => aucune exception => cas passant confirme");
        System.out.println("[T55] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T56 : shouldContainText ----
    @Test
    @DisplayName("T56 - shouldContainText : passe sur 'Bonjour', echoue sur 'Au revoir'")
    void T56_should_contain_text() {
        WebSync web = ouvrirEtats();
        etat.verifierContientTexte(TEXTE, "Bonjour");

        assertFalse(etat.contientTexte(TEXTE, "Au revoir"), "T56 : constat, #texte ne contient pas 'Au revoir'");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldContainText(TEXTE, "Au revoir"),
                "T56 : doit echouer sur un texte non contenu");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("Au revoir") && err.getMessage().contains("By.id: texte"),
                "T56 : le message doit citer le texte et le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T56 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T56] RESULTAT : 'Bonjour' => aucune exception => cas passant confirme");
        System.out.println("[T56] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T57 : shouldContainOnlyText ----
    @Test
    @DisplayName("T57 - shouldContainOnlyText : passe sur le texte exact, echoue sur un texte partiel")
    void T57_should_contain_only_text() {
        WebSync web = ouvrirEtats();
        etat.verifierContientExactement(TEXTE, "Bonjour le monde");

        assertFalse(etat.contientExactement(TEXTE, "Bonjour"), "T57 : constat, le texte de #texte n'est pas exactement 'Bonjour'");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldContainOnlyText(TEXTE, "Bonjour"),
                "T57 : doit echouer sur un texte seulement partiel");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("Bonjour") && err.getMessage().contains("By.id: texte"),
                "T57 : le message doit citer le texte et le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T57 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T57] RESULTAT : 'Bonjour le monde' => aucune exception => cas passant confirme");
        System.out.println("[T57] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T58 : shouldNotContainText ----
    @Test
    @DisplayName("T58 - shouldNotContainText : passe sur 'Au revoir', echoue sur 'Bonjour'")
    void T58_should_not_contain_text() {
        WebSync web = ouvrirEtats();
        etat.verifierNeContientPas(TEXTE, "Au revoir");

        assertTrue(etat.contientTexte(TEXTE, "Bonjour"), "T58 : constat, #texte contient 'Bonjour'");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotContainText(TEXTE, "Bonjour"),
                "T58 : doit echouer sur un texte contenu");
        long ms = depuis(start);
        assertTrue(err.getMessage().contains("Bonjour") && err.getMessage().contains("By.id: texte"),
                "T58 : le message doit citer le texte et le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < borneHauteMs(), "T58 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T58] RESULTAT : 'Au revoir' => aucune exception => cas passant confirme");
        System.out.println("[T58] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }
}
