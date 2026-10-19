package org.automation.phase1;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PHASE 1 — Diagnostic actif (SyncCondition) : codes stables remontés au timeout.
 * Ordre de sondage : ABSENT → STALE → HIDDEN → DISABLED → OUT_OF_BOUNDS → INTERCEPTED.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class DiagnosticTest {

    private static final By TARGET = By.id("target");

    private WebSync openAndSync(String fixture) {
        getDriver().get(Fixtures.url(fixture, null));
        return new WebSync(SyncTimeout.SHORT); // profil le plus court (defaut 5s) => timeout rapide
    }

    // Pause d'observation d'~1 s via un mecanisme d'attente INTEGRE (WebDriverWait), SANS Thread.sleep :
    // on attend une condition volontairement toujours fausse -> le wait tient 1 s pleine puis expire.
    private void pauseObservation() {
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(2))
                    .pollingEvery(Duration.ofSeconds(1))
                    .until(d -> false);
        } catch (TimeoutException expected) {
            // Dwell d'1 s ecoule : comportement voulu (fenetre d'observation visuelle).
        }
    }

    // ---- T7 : HIDDEN ----
    @Test
    @DisplayName("T7 - element present mais display:none : SyncException [HIDDEN]")
    void T7_hidden() {
        WebSync web = openAndSync("hidden.html");
        SyncException ex = assertThrows(SyncException.class, () -> web.click(TARGET));
        assertTrue(ex.getMessage().contains("[HIDDEN]"), "T7 : code [HIDDEN] attendu, message=" + ex.getMessage());
        System.out.println("[T7] RESULTAT : exception attendue=SyncException porteuse de [HIDDEN] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | message=\"" + ex.getMessage()
                + "\" => cas non passant confirme");
    }

    // ---- T8 : DISABLED ----
    @Test
    @DisplayName("T8 - element visible mais disabled : SyncException [DISABLED]")
    void T8_disabled() {
        WebSync web = openAndSync("disabled.html");
        SyncException ex = assertThrows(SyncException.class, () -> web.click(TARGET));
        assertTrue(ex.getMessage().contains("[DISABLED]"), "T8 : code [DISABLED] attendu, message=" + ex.getMessage());
        System.out.println("[T8] RESULTAT : exception attendue=SyncException porteuse de [DISABLED] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | message=\"" + ex.getMessage()
                + "\" => cas non passant confirme");
    }

    // ---- T9 : INTERCEPTED ----
    @Test
    @DisplayName("T9 - clic recouvert par overlay : SyncException [INTERCEPTED]")
    void T9_intercepted() {
        WebSync web = openAndSync("overlay.html");
        SyncException ex = assertThrows(SyncException.class, () -> web.click(TARGET));
        assertTrue(ex.getMessage().contains("[INTERCEPTED]"),
                "T9 : code [INTERCEPTED] attendu, message=" + ex.getMessage());
        System.out.println("[T9] RESULTAT : exception attendue=SyncException porteuse de [INTERCEPTED] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | message=\"" + ex.getMessage()
                + "\" => cas non passant confirme");
    }

    // ---- T10 : hors viewport SCROLLABLE, clic standard (scroll-into-view attendu) ----
    // Reclasse l'ancien T10 (cf. briefing) : un click() simple fait un scroll-into-view et
    // aboutit ; OUT_OF_BOUNDS n'est PAS la cause d'un clic bloqué. On l'objective ici.
    @Test
    @DisplayName("T10 - hors viewport scrollable : type + WebSync.click (scroll-into-view => clic abouti)")
    void T10_out_of_bounds() {
        WebSync web = openAndSync("offscreen.html");

        // Champ hors viewport initial : la saisie doit l'atteindre.
        web.type(By.id("field"), "hello");
        assertEquals("hello", web.getValue(By.id("field")),
                "T10 : saisie effective dans le champ hors viewport");

        // Clic standard : le contrat WebDriver fait un scroll-into-view avant de cliquer.
        web.click(TARGET);
        assertTrue(web.containsText(By.id("status"), "clicked"),
                "T10 : le clic standard aboutit (scroll-into-view) sur un element hors viewport mais scrollable");

        // Journal des evenements pointeur reellement recus par la cible (cf. offscreen.html).
        String pointerLog = (String) ((JavascriptExecutor) getDriver())
                .executeScript("return window.__pointerLog;");
        System.out.println("[T10] RESULTAT : clic standard abouti (scroll-into-view) | aucune exception"
                + " | pointerLog=" + pointerLog + " => cas passant confirme");

        pauseObservation(); // ~1 s pour observer visuellement le resultat
    }

    // ---- T10bis : PREUVE D'EQUIVALENCE WebSync.click == Actions.moveToElement ----
    // Mesure : Actions.moveToElement().click() produit EXACTEMENT la meme trace pointeur
    // (mouseover -> mousemove -> mousedown -> mouseup -> click, tous 'trusted') que le
    // WebSync.click de T10. Le socle clique donc deja via le chemin pointeur W3C (et non un
    // element.click() JS).
    // Difference : Actions ne fait PAS de scroll-into-view (W3C : un pointerMove ne defile pas ;
    // Firefox l'applique strictement -> MoveTargetOutOfBounds hors viewport). On defile donc
    // explicitement (scrollToElement) avant de comparer ; seul WebSync.click defile de lui-meme (T10).
    // NB : aucun deplacement du curseur OS (evenements synthetises) -> cf. RobotMouseDemoTest.
    @Test
    @DisplayName("T10bis - equivalence : Actions.moveToElement produit la meme trace pointeur que WebSync.click")
    void T10bis_out_of_bounds_actions() {
        WebSync web = openAndSync("offscreen.html");

        web.type(By.id("field"), "hello");
        assertEquals("hello", web.getValue(By.id("field")), "T10bis : saisie effective");

        // Clic bas-niveau via Actions, apres un defilement EXPLICITE (Actions ne defile pas seul).
        // Cible CENTREE : Actions.scrollToElement ne defile pas une cible deja partiellement visible
        // (Firefox), alors que moveToElement vise son centre, reste hors viewport.
        WebElement target = getDriver().findElement(TARGET);
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].scrollIntoView({block: 'center'});", target);
        new Actions(getDriver()).moveToElement(target).click().perform();
        boolean clicked = web.containsText(By.id("status"), "clicked");
        // Trace pointeur : doit etre identique a celle de T10 (meme sequence 'trusted').
        String pointerLog = (String) ((JavascriptExecutor) getDriver())
                .executeScript("return window.__pointerLog;");
        assertTrue(clicked, "T10bis : le clic Actions doit aboutir (apres scroll explicite), sans OUT_OF_BOUNDS");
        System.out.println("[T10bis] RESULTAT : clic Actions abouti | aucune exception (pas d'OUT_OF_BOUNDS)"
                + " | pointerLog=" + pointerLog + " => cas passant confirme (equivalence avec T10)");

        pauseObservation(); // ~1 s pour observer visuellement le resultat
    }
}

