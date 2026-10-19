package org.automation.websync.action;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.LectureSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Surface {@code WebSync} — gestes <b>pointeur</b> ({@code doubleClick}, {@code contextClick}) sur la fixture
 * {@code websync/pointeur.html}. La page compte les {@code click} reçus : la séquence d'événements prouve
 * la nature réelle du geste (deux clics + dblclick ; clic droit sans clic gauche).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class PointeurTest {

    private static final String FIXTURE = "websync/pointeur.html";
    private static final By CIBLE = By.id("cible");
    private static final By STATUT = By.id("statut");
    private static final By NB_CLICS = By.id("nb-clics");

    @Steps
    ActionSteps actions;

    @Steps
    LectureSteps lecture;

    // ---- T31 : double-clic ----
    @Test
    @DisplayName("T31 - doubleClick : evenement dblclick recu, precede de deux click")
    void T31_double_clic() {
        actions.ouvrir(FIXTURE);

        actions.doubleCliquer(CIBLE);
        String statut = lecture.texte(STATUT);
        String clics = lecture.texte(NB_CLICS);
        assertEquals("dblclick", statut, "T31 : la cible doit recevoir un dblclick");
        assertEquals("2", clics, "T31 : un double-clic reel emet deux click avant le dblclick");

        System.out.println("[T31] RESULTAT : statut=" + statut + " | click recus=" + clics
                + " | aucune exception => cas passant confirme");
    }

    // ---- T32 : clic droit ----
    @Test
    @DisplayName("T32 - contextClick : evenement contextmenu bouton droit, aucun click gauche")
    void T32_clic_droit() {
        actions.ouvrir(FIXTURE);

        actions.cliquerDroit(CIBLE);
        String statut = lecture.texte(STATUT);
        String clics = lecture.texte(NB_CLICS);
        assertEquals("contextmenu bouton=2", statut, "T32 : la cible doit recevoir un contextmenu du bouton droit (2)");
        assertEquals("0", clics, "T32 : un clic droit ne doit emettre aucun click gauche");

        System.out.println("[T32] RESULTAT : statut=" + statut + " | click recus=" + clics
                + " | aucune exception => cas passant confirme");
    }
}
