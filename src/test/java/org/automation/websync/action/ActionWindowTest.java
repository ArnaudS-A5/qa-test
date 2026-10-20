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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — {@code setWindowFocus}, sur une fenêtre <b>mère</b> et la fenêtre <b>fille</b> qu'elle
 * ouvre (fixtures {@code websync/action/window-parent.html} et {@code websync/action/window-child.html}).
 *
 * <p>Ce que le socle maîtrise : {@code setWindowFocus} exécute {@code window.focus()} dans la fenêtre <b>courante</b>
 * du driver. Chaque fixture espionne {@code window.focus} et compte les appels reçus ({@code #nb-appels}).
 *
 * <p>Ce que le test ne vérifie <b>pas</b>, mesures à l'appui (sonde du 2026-10-18, Chrome et Firefox) : le
 * gain de focus lui-même. {@code switchTo().window()} donne déjà le focus à la fenêtre visée (il serait acquis
 * même si {@code setWindowFocus} ne faisait rien) ; et un {@code window.focus()} lancé par script est soumis à
 * la politique anti-vol de focus du navigateur (honoré de façon irrégulière par Chrome, jamais pour une fille
 * sur Firefox), en plus d'être disputé entre les navigateurs de la campagne parallèle.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ActionWindowTest {

    private static final String PARENT_FIXTURE = "websync/action/window-parent.html";
    private static final By OPEN_CHILD = By.id("ouvrir-fille");
    private static final By WINDOW_NAME = By.id("nom-fenetre");
    private static final By CALL_COUNT = By.id("nb-appels");
    private static final By ABSENT = By.id("absent");

    @Steps
    ActionSteps actions;

    @Steps
    ReadSteps read;

    // ---- T75 : alternance mere / fille / mere / fille ----
    @Test
    @DisplayName("T75 - PASS - setWindowFocus en alternance mere/fille/mere/fille : seule la fenetre courante recoit window.focus()")
    void T75_setWindowFocus_alternate() {
        WebDriver driver = getDriver();
        actions.open(PARENT_FIXTURE);
        String parent = driver.getWindowHandle();
        actions.click(OPEN_CHILD);
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.numberOfWindowsToBe(2));
        String child = driver.getWindowHandles().stream().filter(handle -> !handle.equals(parent)).findFirst().orElseThrow();
        StringBuilder trace = new StringBuilder();

        for (int cycle = 1; cycle <= 2; cycle++) {
            // Mere : avant l'appel, son compteur n'a bouge que par ses propres appels (aucun ne vient de la fille).
            actions.switchToWindow("mere", parent);
            assertEquals("mere", read.getText(WINDOW_NAME), "T75 : le driver doit etre sur la mere");
            assertEquals(String.valueOf(cycle - 1), read.getText(CALL_COUNT),
                    "T75 : cycle " + cycle + ", la mere ne doit pas avoir recu l'appel destine a la fille");
            actions.setWindowFocus(WINDOW_NAME);
            assertEquals(String.valueOf(cycle), read.getText(CALL_COUNT),
                    "T75 : cycle " + cycle + ", setWindowFocus doit appeler window.focus() dans la mere");
            trace.append("mere=").append(cycle).append(' ');

            // Fille : meme verification, symetrique.
            actions.switchToWindow("fille", child);
            assertEquals("fille", read.getText(WINDOW_NAME), "T75 : le driver doit etre sur la fille");
            assertEquals(String.valueOf(cycle - 1), read.getText(CALL_COUNT),
                    "T75 : cycle " + cycle + ", la fille ne doit pas avoir recu l'appel destine a la mere");
            actions.setWindowFocus(WINDOW_NAME);
            assertEquals(String.valueOf(cycle), read.getText(CALL_COUNT),
                    "T75 : cycle " + cycle + ", setWindowFocus doit appeler window.focus() dans la fille");
            trace.append("fille=").append(cycle).append(' ');
        }

        System.out.println("[T75] RESULTAT : alternance mere/fille/mere/fille, appels recus " + trace.toString().trim()
                + " (chaque fenetre uniquement les siens) | aucune exception => cas passant confirme");
    }

    // ---- T76 : setWindowFocus sur element absent ----
    @Test
    @DisplayName("T76 - NON PASS - setWindowFocus sur element absent : SyncException [ABSENT], aucun appel a window.focus()")
    void T76_setWindowFocus_absent() {
        actions.open(PARENT_FIXTURE);
        WebSync web = new WebSync(SyncTimeout.SHORT);

        SyncException ex = assertThrows(SyncException.class, () -> web.setWindowFocus(ABSENT),
                "T76 : setWindowFocus doit attendre l'element puis echouer");
        assertTrue(ex.getMessage().contains("[ABSENT]"), "T76 : code [ABSENT] attendu, message=" + ex.getMessage());
        assertEquals("0", read.getText(CALL_COUNT), "T76 : window.focus() ne doit pas avoir ete appele");

        System.out.println("[T76] RESULTAT : exception attendue=SyncException porteuse de [ABSENT] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | message=\"" + ex.getMessage()
                + "\" | aucun appel a window.focus() => cas non passant confirme");
    }
}
