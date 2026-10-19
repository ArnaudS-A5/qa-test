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
public class LectureSteps extends EtapesWebSync {

    @Step("Lire le texte de {0}")
    public String texte(By locator) {
        return web.getText(locator);
    }

    @Step("Lire la valeur de {0}")
    public String valeur(By locator) {
        return web.getValue(locator);
    }

    @Step("Lire le texte brut (textContent) de {0}")
    public String texteBrut(By locator) {
        return web.getTextContent(locator);
    }

    @Step("Lire le texte effectif (valeur ou texte) de {0}")
    public String texteEffectif(By locator) {
        return web.getTextValue(locator);
    }

    @Step("Lire la balise de {0}")
    public String balise(By locator) {
        return web.getTagName(locator);
    }

    @Step("Lire l'attribut résolu « {1} » de {0}")
    public String attribut(By locator, String nom) {
        return web.getAttribute(locator, nom);
    }

    @Step("Lire l'attribut HTML « {1} » de {0}")
    public String attributDom(By locator, String nom) {
        return web.getDomAttribute(locator, nom);
    }

    @Step("Lire la propriété DOM « {1} » de {0}")
    public String proprieteDom(By locator, String nom) {
        return web.getDomProperty(locator, nom);
    }

    @Step("Lire la propriété CSS « {1} » de {0}")
    public String css(By locator, String propriete) {
        return web.getCssValue(locator, propriete);
    }

    @Step("Lire le libellé ARIA de {0}")
    public String labelAria(By locator) {
        return web.getAriaLabel(locator);
    }

    @Step("Lire le rôle ARIA de {0}")
    public String roleAria(By locator) {
        return web.getAriaRole(locator);
    }

    @Step("Lire le nom accessible de {0}")
    public String nomAccessible(By locator) {
        return web.getAccessibleName(locator);
    }

    @Step("Lire la position de {0}")
    public Point position(By locator) {
        return web.getLocation(locator);
    }

    @Step("Lire la taille de {0}")
    public Dimension taille(By locator) {
        return web.getSize(locator);
    }

    @Step("Lire le rectangle (position + taille) de {0}")
    public Rectangle rectangle(By locator) {
        return web.getRect(locator);
    }

    @Step("Capturer l'élément {0} en PNG")
    public byte[] capture(By locator) {
        return web.getScreenshotAs(locator, OutputType.BYTES);
    }

    @Step("Compter les éléments {0}")
    public int compter(By locator) {
        return web.count(locator);
    }

    @Step("Lire les textes des éléments {0}")
    public List<String> textes(By locator) {
        return web.getTexts(locator);
    }

    @Step("Compter les éléments {1} dans {0}")
    public int compterDans(By conteneur, By locator) {
        return web.within(conteneur).count(locator);
    }

    @Step("Lire les textes des éléments {1} dans {0}")
    public List<String> textesDans(By conteneur, By locator) {
        return web.within(conteneur).getTexts(locator);
    }
}
