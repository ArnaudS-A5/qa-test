package org.automation.websync.action;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.LectureSteps;
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
 * fixture dédiée {@code websync/javascript.html}.
 *
 * <p>Contrat du socle : le JS <b>saute</b> les contrôles de l'interaction native (défilement, visibilité,
 * recouvrement) mais <b>garde</b> l'attente, le refus d'un élément désactivé et le diagnostic. Une erreur
 * qui n'est pas un problème de synchronisation (ex. script refusé) est remontée <b>telle quelle et
 * immédiatement</b>, sans boucle jusqu'au timeout.
 *
 * <p>Les appels dont on attend l'échec restent hors étape ({@code assertThrows} sur un {@code WebSync} direct).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ActionsJavaScriptTest {

    private static final String FIXTURE = "websync/javascript.html";
    private static final By RECOUVERT = By.id("recouvert");
    private static final By STATUT_RECOUVERT = By.id("statut-recouvert");
    private static final By DESACTIVE = By.id("desactive");
    private static final By STATUT_DESACTIVE = By.id("statut-desactive");
    private static final By CHAMP_JS = By.id("champ-js");
    private static final By ZONE_JS = By.id("zone-js");
    private static final By EVENEMENTS_CHAMP = By.id("evenements-champ");
    private static final By EVENEMENTS_ZONE = By.id("evenements-zone");
    private static final By NON_CHAMP = By.id("non-champ");

    /** Une remontée immédiate se mesure en millisecondes ; le profil MEDIUM, lui, aurait attendu 15 s. */
    private static final long REMONTEE_IMMEDIATE_MS = 3_000;

    @Steps
    ActionSteps actions;

    @Steps
    LectureSteps lecture;

    // ---- T27 : clickViaJS traverse un recouvrement qui intercepte le clic natif ----
    @Test
    @DisplayName("T27 - clickViaJS sur element recouvert : le clic natif est intercepte, le clic JS aboutit")
    void T27_click_js_traverse_recouvrement() {
        actions.ouvrir(FIXTURE);

        // Prealable : le clic natif doit etre intercepte par #voile, sinon le clic JS ne prouverait rien.
        WebSync web = new WebSync(SyncTimeout.SHORT);
        SyncException natif = assertThrows(SyncException.class, () -> web.click(RECOUVERT),
                "T27 : prealable, le clic natif doit etre intercepte par #voile");
        assertTrue(natif.getMessage().contains("[INTERCEPTED]"),
                "T27 : prealable, code [INTERCEPTED] attendu, message=" + natif.getMessage());
        assertEquals("aucun clic", lecture.texte(STATUT_RECOUVERT), "T27 : prealable, le clic natif n'a rien declenche");

        actions.cliquerEnJs(RECOUVERT);
        String statut = lecture.texte(STATUT_RECOUVERT);
        assertEquals("clicked", statut, "T27 : clickViaJS doit declencher le gestionnaire de clic malgre #voile");

        System.out.println("[T27] RESULTAT : clic natif=SyncException [INTERCEPTED] (prealable)"
                + " | clickViaJS => statut=" + statut + " | aucune exception => cas passant confirme");
    }

    // ---- T28 : clickViaJS refuse un element desactive (garde d'activation conservee) ----
    @Test
    @DisplayName("T28 - clickViaJS sur element disabled : SyncException [DISABLED], aucun effet")
    void T28_click_js_refuse_desactive() {
        actions.ouvrir(FIXTURE);
        WebSync web = new WebSync(SyncTimeout.SHORT);

        SyncException ex = assertThrows(SyncException.class, () -> web.clickViaJS(DESACTIVE),
                "T28 : clickViaJS doit refuser un element desactive");
        assertTrue(ex.getMessage().contains("[DISABLED]"), "T28 : code [DISABLED] attendu, message=" + ex.getMessage());
        assertEquals("aucun clic", lecture.texte(STATUT_DESACTIVE), "T28 : aucun gestionnaire ne doit avoir ete declenche");

        System.out.println("[T28] RESULTAT : exception attendue=SyncException porteuse de [DISABLED] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | message=\"" + ex.getMessage()
                + "\" | statut inchange => cas non passant confirme");
    }

    // ---- T29 : typeViaJS remplace la valeur et notifie la page (input puis change) ----
    @Test
    @DisplayName("T29 - typeViaJS sur input et textarea : valeur remplacee, evenements input puis change")
    void T29_saisie_js_remplace_et_notifie() {
        actions.ouvrir(FIXTURE);

        actions.saisirEnJs(CHAMP_JS, "nouveau");
        assertEquals("nouveau", lecture.valeur(CHAMP_JS), "T29 : la valeur de l'input doit etre remplacee (etait 'ancien')");
        assertEquals("input,change", lecture.texte(EVENEMENTS_CHAMP), "T29 : l'input doit recevoir input puis change");

        actions.saisirEnJs(ZONE_JS, "texte js");
        assertEquals("texte js", lecture.valeur(ZONE_JS), "T29 : la valeur du textarea doit etre remplacee (etait 'ancien')");
        assertEquals("input,change", lecture.texte(EVENEMENTS_ZONE), "T29 : le textarea doit recevoir input puis change");

        System.out.println("[T29] RESULTAT : input=nouveau, textarea=texte js | evenements input,change sur les deux"
                + " | aucune exception => cas passant confirme");
    }

    // ---- T30 : typeViaJS refuse ce qui n'est ni un input ni un textarea, immediatement ----
    @Test
    @DisplayName("T30 - typeViaJS sur un div : JavascriptException immediate, contenu inchange")
    void T30_saisie_js_refuse_non_champ() {
        actions.ouvrir(FIXTURE);
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        JavascriptException ex = assertThrows(JavascriptException.class, () -> web.typeViaJS(NON_CHAMP, "x"),
                "T30 : typeViaJS doit refuser un element qui n'est ni un input ni un textarea");
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ex.getMessage().contains("ni un input ni un textarea"),
                "T30 : le message doit expliquer le refus, message=" + ex.getMessage());
        assertTrue(ms < REMONTEE_IMMEDIATE_MS,
                "T30 : erreur non transitoire attendue immediatement (sans attendre le profil MEDIUM), ms=" + ms);
        assertEquals("bloc non saisissable", lecture.texte(NON_CHAMP), "T30 : le contenu du div doit rester inchange");

        System.out.println("[T30] RESULTAT : exception attendue=JavascriptException (refus du script) | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | en " + ms + " ms | message=\""
                + ex.getMessage().lines().findFirst().orElse("") + "\" => cas non passant confirme");
    }
}
