package org.automation.steps.websync;

import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;

/**
 * Étapes d'<b>état</b> de la surface {@code WebSync} : prédicats ({@code is*}, {@code contains*}, {@code has*}), qui
 * rendent l'état lu, et assertions {@code should*}, qui échouent elles-mêmes si l'état n'est pas le bon (seule
 * exception à « les assertions sont dans le test », cf. CLAUDE.md).
 */
public class StateSteps extends WebSyncSteps {

    // ---- Prédicats : l'état lu, affirmé par le test ----

    @Step("Lire si {0} est présent")
    public boolean isPresent(By locator) {
        return web.isPresent(locator);
    }

    @Step("Lire si {0} est visible")
    public boolean isVisible(By locator) {
        return web.isVisible(locator);
    }

    @Step("Lire si {0} est activé")
    public boolean isEnabled(By locator) {
        return web.isEnabled(locator);
    }

    @Step("Lire si {0} est sélectionné")
    public boolean isSelected(By locator) {
        return web.isSelected(locator);
    }

    @Step("Lire si {0} est désactivé")
    public boolean isDisabled(By locator) {
        return web.isDisabled(locator);
    }

    @Step("Lire si {0} est cliquable")
    public boolean isClickable(By locator) {
        return web.isClickable(locator);
    }

    @Step("Lire si {0} est visible immédiatement")
    public boolean isCurrentlyVisible(By locator) {
        return web.isCurrentlyVisible(locator);
    }

    @Step("Lire si {0} a le focus")
    public boolean hasFocus(By locator) {
        return web.hasFocus(locator);
    }

    @Step("Lire si {0} contient le texte « {1} »")
    public boolean containsText(By locator, String text) {
        return web.containsText(locator, text);
    }

    @Step("Lire si {0} a exactement le texte « {1} »")
    public boolean containsOnlyText(By locator, String text) {
        return web.containsOnlyText(locator, text);
    }

    @Step("Lire si la valeur de {0} contient « {1} »")
    public boolean containsValue(By locator, String value) {
        return web.containsValue(locator, value);
    }

    @Step("Lire si {0} porte la classe « {1} »")
    public boolean hasClass(By locator, String className) {
        return web.hasClass(locator, className);
    }

    @Step("Lire si {0} contient des éléments {1}")
    public boolean containsElements(By container, By children) {
        return web.containsElements(container, children);
    }

    // ---- Assertions should* : passent sur le bon état. En mode dur, le cas en échec reste dans le test, hors étape ;
    //      en mode soft, l'échec ne lève rien, l'étape est appelée aussi en échec (T82) ----

    @Step("Vérifier que {0} est visible")
    public void shouldBeVisible(By locator) {
        web.shouldBeVisible(locator);
    }

    @Step("Vérifier que {0} est visible immédiatement")
    public void shouldBeCurrentlyVisible(By locator) {
        web.shouldBeCurrentlyVisible(locator);
    }

    @Step("Vérifier que {0} n'est pas visible")
    public void shouldNotBeVisible(By locator) {
        web.shouldNotBeVisible(locator);
    }

    @Step("Vérifier que {0} est présent")
    public void shouldBePresent(By locator) {
        web.shouldBePresent(locator);
    }

    @Step("Vérifier que {0} n'est pas présent")
    public void shouldNotBePresent(By locator) {
        web.shouldNotBePresent(locator);
    }

    @Step("Vérifier que {0} est activé")
    public void shouldBeEnabled(By locator) {
        web.shouldBeEnabled(locator);
    }

    @Step("Vérifier que {0} n'est pas activé")
    public void shouldNotBeEnabled(By locator) {
        web.shouldNotBeEnabled(locator);
    }

    @Step("Vérifier que {0} est sélectionné")
    public void shouldBeSelected(By locator) {
        web.shouldBeSelected(locator);
    }

    @Step("Vérifier que {0} n'est pas sélectionné")
    public void shouldNotBeSelected(By locator) {
        web.shouldNotBeSelected(locator);
    }

    @Step("Vérifier que {0} contient le texte « {1} »")
    public void shouldContainText(By locator, String text) {
        web.shouldContainText(locator, text);
    }

    @Step("Vérifier que {0} a exactement le texte « {1} »")
    public void shouldContainOnlyText(By locator, String text) {
        web.shouldContainOnlyText(locator, text);
    }

    @Step("Vérifier que {0} ne contient pas le texte « {1} »")
    public void shouldNotContainText(By locator, String text) {
        web.shouldNotContainText(locator, text);
    }
}
