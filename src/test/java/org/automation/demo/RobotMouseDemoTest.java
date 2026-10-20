package org.automation.demo;

// DÉMO conservée mais VOLONTAIREMENT COMMENTÉE (bloc /* */) — hors validation socle.
//
// Elle montre qu'un pilotage OS-level (java.awt.Robot) déplace le VRAI curseur de l'écran,
// contrairement à WebSync.click / Actions (protocole W3C : événements pointeur synthétisés
// dans la page, curseur OS immobile — cf. T10/T10bis et window.__pointerLog).
//
// Constat mesuré : au TOUT PREMIER appel OS-level, macOS affiche la demande de permission
// « Accessibilité » ; tant qu'elle n'est pas accordée, le curseur ne bouge pas. Une fois
// accordée (réponse à la boîte de dialogue), les déplacements fonctionnent : curseur qui se
// déplace réellement + clic Robot qui atteint #target (status=clicked). Ce n'était donc PAS
// un souci de flakiness ni de coordonnées multi-écrans, mais bien la permission.
//
// Robot n'a besoin d'AUCUN WebDriver : il coexiste avec l'unique getDriver() du socle
// (pas de 2e WebDriver à instancier). Pour la réactiver : décommenter ce bloc et accorder
// la permission Accessibilité au process qui lance les tests (Terminal / IDE).

/*
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.JavascriptExecutor;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SerenityJUnit5Extension.class)
class RobotMouseDemoTest {

    @Test
    @DisplayName("DEMO - java.awt.Robot deplace le VRAI curseur OS (preuve via MouseInfo) + clic")
    void robot_moves_os_cursor() throws Exception {
        // Le socle ouvre la page (un SEUL WebDriver, gere par Serenity).
        getDriver().get(Fixtures.url("offscreen.html", null));
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        js.executeScript("document.getElementById('target').scrollIntoView({block:'center'});");

        // Traduction position navigateur -> ecran (approx : window.screenX/Y + barre d'outils).
        Long sx = (Long) js.executeScript(
                "var r=document.getElementById('target').getBoundingClientRect();"
                        + "return Math.round(window.screenX + r.left + r.width/2);");
        Long sy = (Long) js.executeScript(
                "var r=document.getElementById('target').getBoundingClientRect();"
                        + "return Math.round(window.screenY + (window.outerHeight - window.innerHeight)"
                        + " + r.top + r.height/2);");
        assertNotNull(sx);
        assertNotNull(sy);

        // Robot : deplacement RÉEL du curseur OS, prouve par MouseInfo avant/apres.
        // NB : au 1er appel OS-level, macOS demande la permission Accessibilite ; une fois
        //      accordee, le curseur bouge pour de vrai.
        Robot robot = new Robot();
        Point before = MouseInfo.getPointerInfo().getLocation();
        robot.mouseMove(sx.intValue(), sy.intValue());
        robot.delay(500);
        Point after = MouseInfo.getPointerInfo().getLocation();
        System.out.println("[ROBOT] curseur OS avant=" + before + " apres=" + after
                + " (cible ecran=(" + sx + "," + sy + "))  deplace=" + !before.equals(after));

        // Clic OS-level a la position atteinte, puis lecture de l'effet cote page.
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(300);
        String status = (String) js.executeScript(
                "return document.getElementById('status').textContent;");
        System.out.println("[ROBOT] status apres clic Robot = " + status);
    }
}
*/
