package org.automation.websync.state;

import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.StateSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — <b>prédicats</b> d'état ({@code is*}, {@code contains*}, {@code has*}) sur la
 * fixture {@code states.html} (un élément par état, aucun minuteur). T14 couvre
 * {@code isPresent}/{@code isVisible}/{@code isEnabled}/{@code containsText} sur absent (fixture
 * {@code appear.html}), T47 les 9 autres prédicats.
 *
 * <p>Contrat du socle : un prédicat rend l'état <b>courant</b> et ne lève <b>jamais</b> ; sur absent, il
 * rend {@code false} immédiatement (implicit wait neutralisé). Les prédicats chronométrés de T14 et T47 restent
 * hors étape (ouverture de la fixture en étape).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class StatePredicateTest {

    private static final String FIXTURE = "states.html";
    private static final By VISIBLE = By.id("visible");
    private static final By HIDDEN = By.id("cache");
    private static final By DISABLED = By.id("desactive");
    private static final By TEXT = By.id("texte");
    private static final By FIELD = By.id("champ");
    private static final By ABSENT = By.id("absent");

    /** Un prédicat sur absent se mesure en millisecondes ; l'implicit wait de serenity.conf tiendrait 3 s. */
    private static final long IMMEDIATE_PREDICATE_MS = 1_000;

    @Steps
    StateSteps state;

    // ---- T14 : is*/contains* sur absent, false immediat (fixture appear.html ; ouverture en etape, predicats
    //      chronometres hors etape) ----
    @Test
    @DisplayName("T14 - PASS - is* sur absent : false immediat (pas d'attente de 3s)")
    void T14_predicates_absent() {
        state.open("appear.html", "delay=0");
        WebSync web = new WebSync(SyncTimeout.MEDIUM);
        By missing = By.id("nexistepas");
        long start = System.nanoTime();
        boolean visible = web.isVisible(missing);
        boolean present = web.isPresent(missing);
        boolean enabled = web.isEnabled(missing);
        boolean contains = web.containsText(missing, "x");
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertFalse(visible || present || enabled || contains, "T14 : tous false attendus");
        assertTrue(ms < 2000, "T14 : doit etre quasi-instantane (< 2s), mesure=" + ms + "ms");
        System.out.println("[T14] RESULTAT : is*/contains sur absent = tous false en " + ms + " ms"
                + " | aucune exception (contrat 'ne leve jamais') | visible=" + visible + " present=" + present
                + " enabled=" + enabled + " contains=" + contains + " => cas passant confirme");
    }

    // ---- T45 : predicats d'etat sur elements visible, cache et desactive ----
    @Test
    @DisplayName("T45 - PASS - isDisabled, isClickable, isCurrentlyVisible : vrai et faux selon l'etat de l'element")
    void T45_predicates_state() {
        state.open(FIXTURE);

        assertTrue(state.isDisabled(DISABLED), "T45 : isDisabled vrai sur #desactive");
        assertFalse(state.isDisabled(VISIBLE), "T45 : isDisabled faux sur #visible");
        assertTrue(state.isClickable(VISIBLE), "T45 : isClickable vrai sur #visible (visible et actif)");
        assertFalse(state.isClickable(DISABLED), "T45 : isClickable faux sur #desactive");
        assertFalse(state.isClickable(HIDDEN), "T45 : isClickable faux sur #cache");
        assertTrue(state.isCurrentlyVisible(VISIBLE), "T45 : isCurrentlyVisible vrai sur #visible");
        assertFalse(state.isCurrentlyVisible(HIDDEN), "T45 : isCurrentlyVisible faux sur #cache (display:none)");

        System.out.println("[T45] RESULTAT : isDisabled desactive=vrai/visible=faux | isClickable visible=vrai,"
                + " desactive=faux, cache=faux | isCurrentlyVisible visible=vrai/cache=faux"
                + " | aucune exception => cas passant confirme");
    }

    // ---- T46 : texte exact et valeur de champ ----
    @Test
    @DisplayName("T46 - PASS - containsOnlyText (texte exact) et containsValue (valeur de champ)")
    void T46_predicates_text() {
        state.open(FIXTURE);

        assertTrue(state.containsOnlyText(TEXT, "Bonjour le monde"), "T46 : containsOnlyText vrai sur le texte exact");
        assertFalse(state.containsOnlyText(TEXT, "Bonjour"), "T46 : containsOnlyText faux sur un texte partiel");
        assertTrue(state.containsText(TEXT, "Bonjour"), "T46 : contraste, containsText vrai sur le meme texte partiel");
        assertTrue(state.containsValue(FIELD, "valeur initiale"), "T46 : containsValue vrai sur la valeur du champ");
        assertFalse(state.containsValue(FIELD, "absente"), "T46 : containsValue faux sur une valeur non contenue");

        System.out.println("[T46] RESULTAT : containsOnlyText exact=vrai, partiel=faux (containsText partiel=vrai)"
                + " | containsValue present=vrai, absent=faux | aucune exception => cas passant confirme");
    }

    // ---- T47 : sur absent, chaque predicat rend faux immediatement (chronometre : hors etape) ----
    @Test
    @DisplayName("T47 - PASS - predicats sur element absent : faux, sans exception, chacun en moins d'1 s")
    void T47_predicates_absent_all() {
        state.open(FIXTURE);
        WebSync web = new WebSync();
        Map<String, BooleanSupplier> predicates = new LinkedHashMap<>();
        predicates.put("isDisabled", () -> web.isDisabled(ABSENT));
        predicates.put("isCurrentlyVisible", () -> web.isCurrentlyVisible(ABSENT));
        predicates.put("isClickable", () -> web.isClickable(ABSENT));
        predicates.put("hasFocus", () -> web.hasFocus(ABSENT));
        predicates.put("containsOnlyText", () -> web.containsOnlyText(ABSENT, "x"));
        predicates.put("containsValue", () -> web.containsValue(ABSENT, "x"));
        predicates.put("hasClass", () -> web.hasClass(ABSENT, "x"));
        predicates.put("containsElements", () -> web.containsElements(ABSENT, By.tagName("li")));
        predicates.put("containsSelectOption", () -> web.containsSelectOption(ABSENT, "x"));

        StringBuilder timings = new StringBuilder();
        for (Map.Entry<String, BooleanSupplier> predicate : predicates.entrySet()) {
            long start = System.nanoTime();
            boolean result = predicate.getValue().getAsBoolean();
            long ms = (System.nanoTime() - start) / 1_000_000;
            assertFalse(result, "T47 : " + predicate.getKey() + " sur absent doit rendre faux");
            assertTrue(ms < IMMEDIATE_PREDICATE_MS, "T47 : " + predicate.getKey() + " sur absent doit etre immediat, ms=" + ms);
            timings.append(predicate.getKey()).append('=').append(ms).append("ms ");
        }

        System.out.println("[T47] RESULTAT : " + predicates.size() + " predicats sur absent => faux | " + timings.toString().trim()
                + " | aucune exception => cas passant confirme");
    }
}
