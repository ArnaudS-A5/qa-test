package org.automation.websync.action;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.ReadSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Surface {@code WebSync} — gestes <b>pointeur</b> ({@code doubleClick}, {@code contextClick}) sur la fixture
 * {@code websync/action/pointer.html}. La page compte les {@code click} reçus : la séquence d'événements prouve
 * la nature réelle du geste (deux clics + dblclick ; clic droit sans clic gauche).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ActionPointerTest {

    private static final String FIXTURE = "websync/action/pointer.html";
    private static final By TARGET = By.id("cible");
    private static final By STATUS = By.id("statut");
    private static final By CLICK_COUNT = By.id("nb-clics");

    @Steps
    ActionSteps actions;

    @Steps
    ReadSteps read;

    // ---- T31 : double-clic ----
    @Test
    @DisplayName("T31 - PASS - doubleClick : evenement dblclick recu, precede de deux click")
    void T31_doubleClick() {
        actions.open(FIXTURE);

        actions.doubleClick(TARGET);
        String status = read.getText(STATUS);
        String clicks = read.getText(CLICK_COUNT);
        assertEquals("dblclick", status, "T31 : la cible doit recevoir un dblclick");
        assertEquals("2", clicks, "T31 : un double-clic reel emet deux click avant le dblclick");

        System.out.println("[T31] RESULTAT : statut=" + status + " | click recus=" + clicks
                + " | aucune exception => cas passant confirme");
    }

    // ---- T32 : clic droit ----
    @Test
    @DisplayName("T32 - PASS - contextClick : evenement contextmenu bouton droit, aucun click gauche")
    void T32_contextClick() {
        actions.open(FIXTURE);

        actions.contextClick(TARGET);
        String status = read.getText(STATUS);
        String clicks = read.getText(CLICK_COUNT);
        assertEquals("contextmenu bouton=2", status, "T32 : la cible doit recevoir un contextmenu du bouton droit (2)");
        assertEquals("0", clicks, "T32 : un clic droit ne doit emettre aucun click gauche");

        System.out.println("[T32] RESULTAT : statut=" + status + " | click recus=" + clicks
                + " | aucune exception => cas passant confirme");
    }
}
