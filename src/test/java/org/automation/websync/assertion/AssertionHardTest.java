package org.automation.websync.assertion;

import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.StateSteps;
import org.automation.support.Timeouts;
import org.automation.support.Transitions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.automation.support.ProfileBounds.TOLERANCE_MS;
import static org.automation.support.ProfileBounds.since;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — assertions <b>dures</b> {@code should*} ({@code qa.assertions.soft=false}), sur la
 * fixture {@code states.html}. T16 (fixture {@code form.html}, constat en étape puis échec hors étape) vérifie qu'un
 * {@code should*} dur stoppe la suite du test ; {@code shouldContainSelectedOption} est couvert par T34
 * ({@code SelectListTest}).
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
class AssertionHardTest {

    private static final String FIXTURE = "states.html";
    private static final By VISIBLE = By.id("visible");
    private static final By HIDDEN = By.id("cache");
    private static final By DISABLED = By.id("desactive");
    private static final By CHECKED = By.id("coche");
    private static final By UNCHECKED = By.id("non-coche");
    private static final By TEXT = By.id("texte");
    private static final By ABSENT = By.id("absent");

    /** T16 : fixture du formulaire, où {@code #missing} est volontairement absent. */
    private static final String FORM_FIXTURE = "form.html";
    private static final By MISSING = By.id("missing");

    /** Un échec sans attente se mesure en millisecondes. */
    private static final long IMMEDIATE_FAILURE_MS = 1_000;

    @Steps
    StateSteps state;

    /** Ouvre la fixture (étape) et rend le {@code WebSync} direct, au profil SHORT, des cas non passants. */
    private WebSync openStates() {
        state.open(FIXTURE);
        return new WebSync(SyncTimeout.SHORT);
    }

    /** Timeout SHORT au défaut du socle (groupe {@code defaults}), en ms. */
    private static long timeoutMs() {
        return Timeouts.defaultOf(SyncTimeout.SHORT).toMillis();
    }

    /** Borne haute d'un échec avec attente : au-delà, une seconde horloge serait à l'œuvre (C8). */
    private static long upperBoundMs() {
        return timeoutMs() + Timeouts.SHORT_MARGIN.toMillis();
    }

    // ---- T16 : should* dur (soft=false) : leve AssertionError et stoppe la suite (fixture form.html) ----
    // Correction (briefing) : le contrat (BF-SYNC-09 + soft-assert D22-bis) impose une
    // AssertionError en mode dur ; le socle est CORRECT. On verifie : type d'erreur,
    // message citant le locator, et NON-execution de l'instruction suivante.
    @Test
    @DisplayName("T16 - NON PASS - should* dur (soft=false) : leve AssertionError et stoppe la suite")
    void T16_should_stops_at_first() {
        state.open(FORM_FIXTURE);
        assertFalse(state.isPresent(MISSING), "T16 : constat, #missing est absent");
        AtomicBoolean nextReached = new AtomicBoolean(false);
        AssertionError err = assertThrows(AssertionError.class, () -> {
            new WebSync(SyncTimeout.SHORT).shouldBeVisible(MISSING);
            nextReached.set(true); // ne doit jamais etre atteint : l'assertion dure stoppe la suite
        }, "T16 : should* dur (soft=false) doit lever AssertionError");
        assertFalse(nextReached.get(),
                "T16 : l'instruction apres un should* dur ne doit pas s'executer");
        assertTrue(err.getMessage() != null && err.getMessage().contains("missing"),
                "T16 : le message doit citer le locator (#missing), msg=" + err.getMessage());
        System.out.println("[T16] RESULTAT : exception attendue=AssertionError (should* dur, soft=false)"
                + " | levee=OUI | type reel=" + err.getClass().getName()
                + " | message=\"" + err.getMessage() + "\" | instruction suivante non executee"
                + " => cas non passant confirme");
    }

    // ---- T48 : shouldBeCurrentlyVisible (sans attente) ----
    @Test
    @DisplayName("T48 - PASS + NON PASS - shouldBeCurrentlyVisible : passe sur #visible, AssertionError immediate sur #cache")
    void T48_shouldBeCurrentlyVisible() {
        WebSync web = openStates();
        state.shouldBeCurrentlyVisible(VISIBLE);

        assertFalse(state.isCurrentlyVisible(HIDDEN), "T48 : constat, #cache n'est pas visible");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldBeCurrentlyVisible(HIDDEN),
                "T48 : doit echouer sur #cache (display:none)");
        long ms = since(start);
        assertTrue(err.getMessage().contains("By.id: cache"), "T48 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms < IMMEDIATE_FAILURE_MS, "T48 : echec immediat attendu (aucune attente), ms=" + ms);

        System.out.println("[T48] RESULTAT : #visible => aucune exception => cas passant confirme");
        System.out.println("[T48] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T49 : shouldNotBeVisible (attend la disparition) ----
    @Test
    @DisplayName("T49 - PASS + NON PASS - shouldNotBeVisible : passe sur #cache et sur un element qui disparait a 2 s, echoue sur #visible")
    void T49_shouldNotBeVisible() {
        WebSync web = openStates();
        state.shouldNotBeVisible(HIDDEN);

        assertTrue(state.isVisible(VISIBLE), "T49 : constat, #visible est visible");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotBeVisible(VISIBLE),
                "T49 : doit echouer sur #visible, qui le reste");
        long ms = since(start);
        assertTrue(err.getMessage().contains("By.id: visible"), "T49 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T49 : echec au timeout SHORT, ms=" + ms);

        // Attente d'une disparition : chronometree, donc appel direct.
        long disappearAt = Transitions.open(Transitions.Scenario.DISAPPEAR, Duration.ofSeconds(2));
        long waitStart = System.nanoTime();
        assertDoesNotThrow(() -> web.shouldNotBeVisible(Transitions.TARGET), "T49 : doit attendre la disparition a 2 s");
        long waited = since(waitStart);
        assertTrue(waited >= disappearAt - TOLERANCE_MS && waited < timeoutMs(),
                "T49 : passe apres avoir attendu la disparition (" + disappearAt + " ms), avant le timeout, ms=" + waited);

        System.out.println("[T49] RESULTAT : #cache => passe | disparition a " + disappearAt + " ms => passe en " + waited
                + " ms | aucune exception => cas passant confirme");
        System.out.println("[T49] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T50 : shouldBePresent ----
    @Test
    @DisplayName("T50 - PASS + NON PASS - shouldBePresent : passe sur #cache (present meme invisible), echoue sur #absent")
    void T50_shouldBePresent() {
        WebSync web = openStates();
        state.shouldBePresent(HIDDEN);

        assertFalse(state.isPresent(ABSENT), "T50 : constat, #absent n'est pas present");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldBePresent(ABSENT),
                "T50 : doit echouer sur #absent");
        long ms = since(start);
        assertTrue(err.getMessage().contains("By.id: absent"), "T50 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T50 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T50] RESULTAT : #cache => aucune exception => cas passant confirme");
        System.out.println("[T50] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T51 : shouldNotBePresent ----
    @Test
    @DisplayName("T51 - PASS + NON PASS - shouldNotBePresent : passe sur #absent, echoue sur #cache (present meme invisible)")
    void T51_shouldNotBePresent() {
        WebSync web = openStates();
        state.shouldNotBePresent(ABSENT);

        assertTrue(state.isPresent(HIDDEN), "T51 : constat, #cache est present dans le DOM");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotBePresent(HIDDEN),
                "T51 : doit echouer sur #cache, present dans le DOM");
        long ms = since(start);
        assertTrue(err.getMessage().contains("By.id: cache"), "T51 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T51 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T51] RESULTAT : #absent => aucune exception => cas passant confirme");
        System.out.println("[T51] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T52 : shouldBeEnabled ----
    @Test
    @DisplayName("T52 - PASS + NON PASS - shouldBeEnabled : passe sur #visible, echoue sur #desactive")
    void T52_shouldBeEnabled() {
        WebSync web = openStates();
        state.shouldBeEnabled(VISIBLE);

        assertFalse(state.isEnabled(DISABLED), "T52 : constat, #desactive n'est pas active");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldBeEnabled(DISABLED),
                "T52 : doit echouer sur #desactive");
        long ms = since(start);
        assertTrue(err.getMessage().contains("By.id: desactive"), "T52 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T52 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T52] RESULTAT : #visible => aucune exception => cas passant confirme");
        System.out.println("[T52] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T53 : shouldNotBeEnabled ----
    @Test
    @DisplayName("T53 - PASS + NON PASS - shouldNotBeEnabled : passe sur #desactive, echoue sur #visible")
    void T53_shouldNotBeEnabled() {
        WebSync web = openStates();
        state.shouldNotBeEnabled(DISABLED);

        assertTrue(state.isEnabled(VISIBLE), "T53 : constat, #visible est active");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotBeEnabled(VISIBLE),
                "T53 : doit echouer sur #visible, actif");
        long ms = since(start);
        assertTrue(err.getMessage().contains("By.id: visible"), "T53 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T53 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T53] RESULTAT : #desactive => aucune exception => cas passant confirme");
        System.out.println("[T53] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T54 : shouldBeSelected ----
    @Test
    @DisplayName("T54 - PASS + NON PASS - shouldBeSelected : passe sur #coche, echoue sur #non-coche")
    void T54_shouldBeSelected() {
        WebSync web = openStates();
        state.shouldBeSelected(CHECKED);

        assertFalse(state.isSelected(UNCHECKED), "T54 : constat, #non-coche n'est pas coche");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldBeSelected(UNCHECKED),
                "T54 : doit echouer sur #non-coche");
        long ms = since(start);
        assertTrue(err.getMessage().contains("By.id: non-coche"), "T54 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T54 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T54] RESULTAT : #coche => aucune exception => cas passant confirme");
        System.out.println("[T54] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T55 : shouldNotBeSelected ----
    @Test
    @DisplayName("T55 - PASS + NON PASS - shouldNotBeSelected : passe sur #non-coche, echoue sur #coche")
    void T55_shouldNotBeSelected() {
        WebSync web = openStates();
        state.shouldNotBeSelected(UNCHECKED);

        assertTrue(state.isSelected(CHECKED), "T55 : constat, #coche est coche");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotBeSelected(CHECKED),
                "T55 : doit echouer sur #coche");
        long ms = since(start);
        assertTrue(err.getMessage().contains("By.id: coche"), "T55 : le message doit citer le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T55 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T55] RESULTAT : #non-coche => aucune exception => cas passant confirme");
        System.out.println("[T55] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T56 : shouldContainText ----
    @Test
    @DisplayName("T56 - PASS + NON PASS - shouldContainText : passe sur 'Bonjour', echoue sur 'Au revoir'")
    void T56_shouldContainText() {
        WebSync web = openStates();
        state.shouldContainText(TEXT, "Bonjour");

        assertFalse(state.containsText(TEXT, "Au revoir"), "T56 : constat, #texte ne contient pas 'Au revoir'");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldContainText(TEXT, "Au revoir"),
                "T56 : doit echouer sur un texte non contenu");
        long ms = since(start);
        assertTrue(err.getMessage().contains("Au revoir") && err.getMessage().contains("By.id: texte"),
                "T56 : le message doit citer le texte et le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T56 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T56] RESULTAT : 'Bonjour' => aucune exception => cas passant confirme");
        System.out.println("[T56] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T57 : shouldContainOnlyText ----
    @Test
    @DisplayName("T57 - PASS + NON PASS - shouldContainOnlyText : passe sur le texte exact, echoue sur un texte partiel")
    void T57_shouldContainOnlyText() {
        WebSync web = openStates();
        state.shouldContainOnlyText(TEXT, "Bonjour le monde");

        assertFalse(state.containsOnlyText(TEXT, "Bonjour"), "T57 : constat, le texte de #texte n'est pas exactement 'Bonjour'");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldContainOnlyText(TEXT, "Bonjour"),
                "T57 : doit echouer sur un texte seulement partiel");
        long ms = since(start);
        assertTrue(err.getMessage().contains("Bonjour") && err.getMessage().contains("By.id: texte"),
                "T57 : le message doit citer le texte et le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T57 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T57] RESULTAT : 'Bonjour le monde' => aucune exception => cas passant confirme");
        System.out.println("[T57] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T58 : shouldNotContainText ----
    @Test
    @DisplayName("T58 - PASS + NON PASS - shouldNotContainText : passe sur 'Au revoir', echoue sur 'Bonjour'")
    void T58_shouldNotContainText() {
        WebSync web = openStates();
        state.shouldNotContainText(TEXT, "Au revoir");

        assertTrue(state.containsText(TEXT, "Bonjour"), "T58 : constat, #texte contient 'Bonjour'");
        long start = System.nanoTime();
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldNotContainText(TEXT, "Bonjour"),
                "T58 : doit echouer sur un texte contenu");
        long ms = since(start);
        assertTrue(err.getMessage().contains("Bonjour") && err.getMessage().contains("By.id: texte"),
                "T58 : le message doit citer le texte et le locator, message=" + err.getMessage());
        assertTrue(ms >= timeoutMs() && ms < upperBoundMs(), "T58 : echec au timeout SHORT, ms=" + ms);

        System.out.println("[T58] RESULTAT : 'Au revoir' => aucune exception => cas passant confirme");
        System.out.println("[T58] RESULTAT : exception attendue=AssertionError | levee=OUI | type reel="
                + err.getClass().getName() + " | en " + ms + " ms | message=\"" + err.getMessage() + "\" => cas non passant confirme");
    }
}
