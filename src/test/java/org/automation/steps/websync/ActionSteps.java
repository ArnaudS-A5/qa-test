package org.automation.steps.websync;

import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;

import static net.serenitybdd.core.Serenity.getDriver;

/** Étapes d'<b>action</b> de la surface {@code WebSync} : saisie, clic, JavaScript, gestes pointeur, focus fenêtre. */
public class ActionSteps extends EtapesWebSync {

    @Step("Saisir « {1} » dans {0}")
    public void saisir(By locator, String valeur) {
        web.type(locator, valeur);
    }

    @Step("Saisir « {1} » dans {0} puis Tabulation")
    public void saisirPuisTab(By locator, String valeur) {
        web.typeAndTab(locator, valeur);
    }

    @Step("Saisir « {1} » dans {0} puis Entrée")
    public void saisirPuisEntree(By locator, String valeur) {
        web.typeAndEnter(locator, valeur);
    }

    @Step("Vider {0}")
    public void vider(By locator) {
        web.clear(locator);
    }

    @Step("Soumettre le formulaire contenant {0}")
    public void soumettre(By locator) {
        web.submit(locator);
    }

    @Step("Cliquer sur {0}")
    public void cliquer(By locator) {
        web.click(locator);
    }

    @Step("Cliquer sur {0} par JavaScript")
    public void cliquerEnJs(By locator) {
        web.clickViaJS(locator);
    }

    @Step("Saisir « {1} » dans {0} par JavaScript")
    public void saisirEnJs(By locator, String valeur) {
        web.typeViaJS(locator, valeur);
    }

    @Step("Double-cliquer sur {0}")
    public void doubleCliquer(By locator) {
        web.doubleClick(locator);
    }

    @Step("Cliquer droit sur {0}")
    public void cliquerDroit(By locator) {
        web.contextClick(locator);
    }

    @Step("Donner le focus à la fenêtre courante via {0}")
    public void donnerLeFocusFenetre(By locator) {
        web.setWindowFocus(locator);
    }

    /** Bascule du driver (Selenium, hors socle), rapportée pour que l'alternance des fenêtres se lise. */
    @Step("Basculer le driver sur la fenêtre {0}")
    public void basculerVersFenetre(String libelle, String handle) {
        getDriver().switchTo().window(handle);
    }
}
