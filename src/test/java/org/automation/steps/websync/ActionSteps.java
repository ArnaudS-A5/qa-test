package org.automation.steps.websync;

import com.example.qa.secret.Secret;
import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;

import static net.serenitybdd.core.Serenity.getDriver;

/** Étapes d'<b>action</b> de la surface {@code WebSync} : saisie, clic, JavaScript, gestes pointeur, focus fenêtre. */
public class ActionSteps extends WebSyncSteps {

    @Step("Saisir « {1} » dans {0}")
    public void type(By locator, String value) {
        web.type(locator, value);
    }

    /**
     * Le titre affiche le rendu masqué du socle ({@code Secret.toString()}), jamais le clair. Il reste
     * <b>identique</b> à celui de {@code type(By, String)} : Serenity retrouve le titre d'une étape par son nom et
     * son nombre de paramètres, et prendrait sinon l'un pour l'autre (constaté le 2026-10-20).
     */
    @Step("Saisir « {1} » dans {0}")
    public void type(By locator, Secret secret) {
        web.type(locator, secret);
    }

    @Step("Saisir « {1} » dans {0} puis Tabulation")
    public void typeAndTab(By locator, String value) {
        web.typeAndTab(locator, value);
    }

    @Step("Saisir « {1} » dans {0} puis Entrée")
    public void typeAndEnter(By locator, String value) {
        web.typeAndEnter(locator, value);
    }

    @Step("Vider {0}")
    public void clear(By locator) {
        web.clear(locator);
    }

    @Step("Soumettre le formulaire contenant {0}")
    public void submit(By locator) {
        web.submit(locator);
    }

    @Step("Cliquer sur {0}")
    public void click(By locator) {
        web.click(locator);
    }

    @Step("Cliquer sur {0} par JavaScript")
    public void clickViaJS(By locator) {
        web.clickViaJS(locator);
    }

    @Step("Saisir « {1} » dans {0} par JavaScript")
    public void typeViaJS(By locator, String value) {
        web.typeViaJS(locator, value);
    }

    @Step("Double-cliquer sur {0}")
    public void doubleClick(By locator) {
        web.doubleClick(locator);
    }

    @Step("Cliquer droit sur {0}")
    public void contextClick(By locator) {
        web.contextClick(locator);
    }

    @Step("Donner le focus à la fenêtre courante via {0}")
    public void setWindowFocus(By locator) {
        web.setWindowFocus(locator);
    }

    /** Bascule du driver (Selenium, hors socle), rapportée pour que l'alternance des fenêtres se lise. */
    @Step("Basculer le driver sur la fenêtre {0}")
    public void switchToWindow(String label, String handle) {
        getDriver().switchTo().window(handle);
    }
}
