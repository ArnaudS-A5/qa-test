package org.automation.steps.websync;

import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;

import java.util.List;

/**
 * Étapes de <b>lecture</b> de la surface {@code WebSync} : texte, valeur, attributs, style, ARIA, géométrie,
 * capture et collections. Chaque lecture rend la valeur lue : c'est le test qui l'affirme.
 */
public class ReadSteps extends WebSyncSteps {

    @Step("Lire le texte de {0}")
    public String getText(By locator) {
        return web.getText(locator);
    }

    @Step("Lire la valeur de {0}")
    public String getValue(By locator) {
        return web.getValue(locator);
    }

    @Step("Lire le texte brut (textContent) de {0}")
    public String getTextContent(By locator) {
        return web.getTextContent(locator);
    }

    @Step("Lire le texte effectif (valeur ou texte) de {0}")
    public String getTextValue(By locator) {
        return web.getTextValue(locator);
    }

    @Step("Lire la balise de {0}")
    public String getTagName(By locator) {
        return web.getTagName(locator);
    }

    @Step("Lire l'attribut résolu « {1} » de {0}")
    public String getAttribute(By locator, String name) {
        return web.getAttribute(locator, name);
    }

    @Step("Lire l'attribut HTML « {1} » de {0}")
    public String getDomAttribute(By locator, String name) {
        return web.getDomAttribute(locator, name);
    }

    @Step("Lire la propriété DOM « {1} » de {0}")
    public String getDomProperty(By locator, String name) {
        return web.getDomProperty(locator, name);
    }

    @Step("Lire la propriété CSS « {1} » de {0}")
    public String getCssValue(By locator, String property) {
        return web.getCssValue(locator, property);
    }

    @Step("Lire le libellé ARIA de {0}")
    public String getAriaLabel(By locator) {
        return web.getAriaLabel(locator);
    }

    @Step("Lire le rôle ARIA de {0}")
    public String getAriaRole(By locator) {
        return web.getAriaRole(locator);
    }

    @Step("Lire le nom accessible de {0}")
    public String getAccessibleName(By locator) {
        return web.getAccessibleName(locator);
    }

    @Step("Lire la position de {0}")
    public Point getLocation(By locator) {
        return web.getLocation(locator);
    }

    @Step("Lire la taille de {0}")
    public Dimension getSize(By locator) {
        return web.getSize(locator);
    }

    @Step("Lire le rectangle (position + taille) de {0}")
    public Rectangle getRect(By locator) {
        return web.getRect(locator);
    }

    @Step("Capturer l'élément {0} en PNG")
    public byte[] getScreenshotAs(By locator) {
        return web.getScreenshotAs(locator, OutputType.BYTES);
    }

    @Step("Compter les éléments {0}")
    public int count(By locator) {
        return web.count(locator);
    }

    @Step("Lire les textes des éléments {0}")
    public List<String> getTexts(By locator) {
        return web.getTexts(locator);
    }

    @Step("Compter les éléments {1} dans {0}")
    public int countWithin(By container, By locator) {
        return web.within(container).count(locator);
    }

    @Step("Lire les textes des éléments {1} dans {0}")
    public List<String> getTextsWithin(By container, By locator) {
        return web.within(container).getTexts(locator);
    }
}
