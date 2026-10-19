package org.automation.websync.etat;

import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.EtatSteps;
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
 * fixture {@code websync/etats.html} (un élément par état, aucun minuteur). Complète T14, qui couvre
 * {@code isPresent}/{@code isVisible}/{@code isEnabled}/{@code containsText} sur absent.
 *
 * <p>Contrat du socle : un prédicat rend l'état <b>courant</b> et ne lève <b>jamais</b> ; sur absent, il
 * rend {@code false} immédiatement (implicit wait neutralisé). T47, chronométré, reste hors étape.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class PredicatsTest {

    private static final String FIXTURE = "websync/etats.html";
    private static final By VISIBLE = By.id("visible");
    private static final By CACHE = By.id("cache");
    private static final By DESACTIVE = By.id("desactive");
    private static final By TEXTE = By.id("texte");
    private static final By CHAMP = By.id("champ");
    private static final By ABSENT = By.id("absent");

    /** Un prédicat sur absent se mesure en millisecondes ; l'implicit wait de serenity.conf tiendrait 3 s. */
    private static final long PREDICAT_IMMEDIAT_MS = 1_000;

    @Steps
    EtatSteps etat;

    // ---- T45 : predicats d'etat sur elements visible, cache et desactive ----
    @Test
    @DisplayName("T45 - isDisabled, isClickable, isCurrentlyVisible : vrai et faux selon l'etat de l'element")
    void T45_predicats_etat() {
        etat.ouvrir(FIXTURE);

        assertTrue(etat.estDesactive(DESACTIVE), "T45 : isDisabled vrai sur #desactive");
        assertFalse(etat.estDesactive(VISIBLE), "T45 : isDisabled faux sur #visible");
        assertTrue(etat.estCliquable(VISIBLE), "T45 : isClickable vrai sur #visible (visible et actif)");
        assertFalse(etat.estCliquable(DESACTIVE), "T45 : isClickable faux sur #desactive");
        assertFalse(etat.estCliquable(CACHE), "T45 : isClickable faux sur #cache");
        assertTrue(etat.estVisibleMaintenant(VISIBLE), "T45 : isCurrentlyVisible vrai sur #visible");
        assertFalse(etat.estVisibleMaintenant(CACHE), "T45 : isCurrentlyVisible faux sur #cache (display:none)");

        System.out.println("[T45] RESULTAT : isDisabled desactive=vrai/visible=faux | isClickable visible=vrai,"
                + " desactive=faux, cache=faux | isCurrentlyVisible visible=vrai/cache=faux"
                + " | aucune exception => cas passant confirme");
    }

    // ---- T46 : texte exact et valeur de champ ----
    @Test
    @DisplayName("T46 - containsOnlyText (texte exact) et containsValue (valeur de champ)")
    void T46_predicats_texte_valeur() {
        etat.ouvrir(FIXTURE);

        assertTrue(etat.contientExactement(TEXTE, "Bonjour le monde"), "T46 : containsOnlyText vrai sur le texte exact");
        assertFalse(etat.contientExactement(TEXTE, "Bonjour"), "T46 : containsOnlyText faux sur un texte partiel");
        assertTrue(etat.contientTexte(TEXTE, "Bonjour"), "T46 : contraste, containsText vrai sur le meme texte partiel");
        assertTrue(etat.contientValeur(CHAMP, "valeur initiale"), "T46 : containsValue vrai sur la valeur du champ");
        assertFalse(etat.contientValeur(CHAMP, "absente"), "T46 : containsValue faux sur une valeur non contenue");

        System.out.println("[T46] RESULTAT : containsOnlyText exact=vrai, partiel=faux (containsText partiel=vrai)"
                + " | containsValue present=vrai, absent=faux | aucune exception => cas passant confirme");
    }

    // ---- T47 : sur absent, chaque predicat rend faux immediatement (chronometre : hors etape) ----
    @Test
    @DisplayName("T47 - predicats sur element absent : faux, sans exception, chacun en moins d'1 s")
    void T47_predicats_absent_instantanes() {
        etat.ouvrir(FIXTURE);
        WebSync web = new WebSync();
        Map<String, BooleanSupplier> predicats = new LinkedHashMap<>();
        predicats.put("isDisabled", () -> web.isDisabled(ABSENT));
        predicats.put("isCurrentlyVisible", () -> web.isCurrentlyVisible(ABSENT));
        predicats.put("isClickable", () -> web.isClickable(ABSENT));
        predicats.put("hasFocus", () -> web.hasFocus(ABSENT));
        predicats.put("containsOnlyText", () -> web.containsOnlyText(ABSENT, "x"));
        predicats.put("containsValue", () -> web.containsValue(ABSENT, "x"));
        predicats.put("hasClass", () -> web.hasClass(ABSENT, "x"));
        predicats.put("containsElements", () -> web.containsElements(ABSENT, By.tagName("li")));
        predicats.put("containsSelectOption", () -> web.containsSelectOption(ABSENT, "x"));

        StringBuilder mesures = new StringBuilder();
        for (Map.Entry<String, BooleanSupplier> predicat : predicats.entrySet()) {
            long start = System.nanoTime();
            boolean resultat = predicat.getValue().getAsBoolean();
            long ms = (System.nanoTime() - start) / 1_000_000;
            assertFalse(resultat, "T47 : " + predicat.getKey() + " sur absent doit rendre faux");
            assertTrue(ms < PREDICAT_IMMEDIAT_MS, "T47 : " + predicat.getKey() + " sur absent doit etre immediat, ms=" + ms);
            mesures.append(predicat.getKey()).append('=').append(ms).append("ms ");
        }

        System.out.println("[T47] RESULTAT : " + predicats.size() + " predicats sur absent => faux | " + mesures.toString().trim()
                + " | aucune exception => cas passant confirme");
    }
}
