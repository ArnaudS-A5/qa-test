package org.automation.websync.action;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.ReadSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — actions <b>JavaScript</b> ({@code clickViaJS}, {@code typeViaJS}), toutes sur la
 * fixture dédiée {@code websync/action/javascript.html}.
 *
 * <p>Contrat du socle : le JS <b>saute</b> les contrôles de l'interaction native (défilement, visibilité,
 * recouvrement) mais <b>garde</b> l'attente, le refus d'un élément désactivé et le diagnostic. Une erreur
 * qui n'est pas un problème de synchronisation (ex. script refusé) est remontée <b>telle quelle et
 * immédiatement</b>, sans boucle jusqu'au timeout.
 *
 * <p>Les appels dont on attend l'échec restent hors étape ({@code assertThrows} sur un {@code WebSync} direct).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ActionJavaScriptTest {

    private static final String FIXTURE = "websync/action/javascript.html";
    private static final By COVERED = By.id("recouvert");
    private static final By COVERED_STATUS = By.id("statut-recouvert");
    private static final By DISABLED = By.id("desactive");
    private static final By DISABLED_STATUS = By.id("statut-desactive");
    private static final By JS_FIELD = By.id("champ-js");
    private static final By JS_AREA = By.id("zone-js");
    private static final By FIELD_EVENTS = By.id("evenements-champ");
    private static final By AREA_EVENTS = By.id("evenements-zone");
    private static final By NOT_A_FIELD = By.id("non-champ");

    /** Une remontée immédiate se mesure en millisecondes ; le profil MEDIUM, lui, aurait attendu 15 s. */
    private static final long IMMEDIATE_ERROR_MS = 3_000;

    @Steps
    ActionSteps actions;

    @Steps
    ReadSteps read;

    // ---- T27 : clickViaJS traverse un recouvrement qui intercepte le clic natif ----
    @Test
    @DisplayName("T27 - PASS - clickViaJS sur element recouvert : le clic natif est intercepte, le clic JS aboutit")
    void T27_clickViaJS_covered() {
        actions.open(FIXTURE);

        // Prealable : le clic natif doit etre intercepte par #voile, sinon le clic JS ne prouverait rien.
        WebSync web = new WebSync(SyncTimeout.SHORT);
        SyncException nativeError = assertThrows(SyncException.class, () -> web.click(COVERED),
                "T27 : prealable, le clic natif doit etre intercepte par #voile");
        assertTrue(nativeError.getMessage().contains("[INTERCEPTED]"),
                "T27 : prealable, code [INTERCEPTED] attendu, message=" + nativeError.getMessage());
        assertEquals("aucun clic", read.getText(COVERED_STATUS), "T27 : prealable, le clic natif n'a rien declenche");

        actions.clickViaJS(COVERED);
        String status = read.getText(COVERED_STATUS);
        assertEquals("clicked", status, "T27 : clickViaJS doit declencher le gestionnaire de clic malgre #voile");

        System.out.println("[T27] RESULTAT : clic natif=SyncException [INTERCEPTED] (prealable)"
                + " | clickViaJS => statut=" + status + " | aucune exception => cas passant confirme");
    }

    // ---- T28 : clickViaJS refuse un element desactive (garde d'activation conservee) ----
    @Test
    @DisplayName("T28 - NON PASS - clickViaJS sur element disabled : SyncException [DISABLED], aucun effet")
    void T28_clickViaJS_disabled() {
        actions.open(FIXTURE);
        WebSync web = new WebSync(SyncTimeout.SHORT);

        SyncException ex = assertThrows(SyncException.class, () -> web.clickViaJS(DISABLED),
                "T28 : clickViaJS doit refuser un element desactive");
        assertTrue(ex.getMessage().contains("[DISABLED]"), "T28 : code [DISABLED] attendu, message=" + ex.getMessage());
        assertEquals("aucun clic", read.getText(DISABLED_STATUS), "T28 : aucun gestionnaire ne doit avoir ete declenche");

        System.out.println("[T28] RESULTAT : exception attendue=SyncException porteuse de [DISABLED] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | message=\"" + ex.getMessage()
                + "\" | statut inchange => cas non passant confirme");
    }

    // ---- T29 : typeViaJS remplace la valeur et notifie la page (input puis change) ----
    @Test
    @DisplayName("T29 - PASS - typeViaJS sur input et textarea : valeur remplacee, evenements input puis change")
    void T29_typeViaJS_field() {
        actions.open(FIXTURE);

        actions.typeViaJS(JS_FIELD, "nouveau");
        assertEquals("nouveau", read.getValue(JS_FIELD), "T29 : la valeur de l'input doit etre remplacee (etait 'ancien')");
        assertEquals("input,change", read.getText(FIELD_EVENTS), "T29 : l'input doit recevoir input puis change");

        actions.typeViaJS(JS_AREA, "texte js");
        assertEquals("texte js", read.getValue(JS_AREA), "T29 : la valeur du textarea doit etre remplacee (etait 'ancien')");
        assertEquals("input,change", read.getText(AREA_EVENTS), "T29 : le textarea doit recevoir input puis change");

        System.out.println("[T29] RESULTAT : input=nouveau, textarea=texte js | evenements input,change sur les deux"
                + " | aucune exception => cas passant confirme");
    }

    // ---- T30 : typeViaJS refuse ce qui n'est ni un input ni un textarea, immediatement ----
    @Test
    @DisplayName("T30 - NON PASS - typeViaJS sur un div : JavascriptException immediate, contenu inchange")
    void T30_typeViaJS_div() {
        actions.open(FIXTURE);
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        JavascriptException ex = assertThrows(JavascriptException.class, () -> web.typeViaJS(NOT_A_FIELD, "x"),
                "T30 : typeViaJS doit refuser un element qui n'est ni un input ni un textarea");
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ex.getMessage().contains("ni un input ni un textarea"),
                "T30 : le message doit expliquer le refus, message=" + ex.getMessage());
        assertTrue(ms < IMMEDIATE_ERROR_MS,
                "T30 : erreur non transitoire attendue immediatement (sans attendre le profil MEDIUM), ms=" + ms);
        assertEquals("bloc non saisissable", read.getText(NOT_A_FIELD), "T30 : le contenu du div doit rester inchange");

        System.out.println("[T30] RESULTAT : exception attendue=JavascriptException (refus du script) | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | en " + ms + " ms | message=\""
                + ex.getMessage().lines().findFirst().orElse("") + "\" => cas non passant confirme");
    }
}
