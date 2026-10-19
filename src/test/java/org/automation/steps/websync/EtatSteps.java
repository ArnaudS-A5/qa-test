package org.automation.steps.websync;

import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;

/**
 * Étapes d'<b>état</b> de la surface {@code WebSync} : prédicats ({@code is*}, {@code contains*}, {@code has*}), qui
 * rendent l'état lu, et assertions {@code should*}, qui échouent elles-mêmes si l'état n'est pas le bon (seule
 * exception à « les assertions sont dans le test », cf. CLAUDE.md).
 */
public class EtatSteps extends EtapesWebSync {

    // ---- Prédicats : l'état lu, affirmé par le test ----

    @Step("Lire si {0} est présent")
    public boolean estPresent(By locator) {
        return web.isPresent(locator);
    }

    @Step("Lire si {0} est visible")
    public boolean estVisible(By locator) {
        return web.isVisible(locator);
    }

    @Step("Lire si {0} est activé")
    public boolean estActive(By locator) {
        return web.isEnabled(locator);
    }

    @Step("Lire si {0} est sélectionné")
    public boolean estSelectionne(By locator) {
        return web.isSelected(locator);
    }

    @Step("Lire si {0} est désactivé")
    public boolean estDesactive(By locator) {
        return web.isDisabled(locator);
    }

    @Step("Lire si {0} est cliquable")
    public boolean estCliquable(By locator) {
        return web.isClickable(locator);
    }

    @Step("Lire si {0} est visible immédiatement")
    public boolean estVisibleMaintenant(By locator) {
        return web.isCurrentlyVisible(locator);
    }

    @Step("Lire si {0} a le focus")
    public boolean aLeFocus(By locator) {
        return web.hasFocus(locator);
    }

    @Step("Lire si {0} contient le texte « {1} »")
    public boolean contientTexte(By locator, String texte) {
        return web.containsText(locator, texte);
    }

    @Step("Lire si {0} a exactement le texte « {1} »")
    public boolean contientExactement(By locator, String texte) {
        return web.containsOnlyText(locator, texte);
    }

    @Step("Lire si la valeur de {0} contient « {1} »")
    public boolean contientValeur(By locator, String valeur) {
        return web.containsValue(locator, valeur);
    }

    @Step("Lire si {0} porte la classe « {1} »")
    public boolean aLaClasse(By locator, String classe) {
        return web.hasClass(locator, classe);
    }

    @Step("Lire si {0} contient des éléments {1}")
    public boolean contientElements(By conteneur, By enfants) {
        return web.containsElements(conteneur, enfants);
    }

    // ---- Assertions should* : passent sur le bon état. En mode dur, le cas en échec reste dans le test, hors étape ;
    //      en mode soft, l'échec ne lève rien, l'étape est appelée aussi en échec (T82) ----

    @Step("Vérifier que {0} est visible")
    public void verifierVisible(By locator) {
        web.shouldBeVisible(locator);
    }

    @Step("Vérifier que {0} est visible immédiatement")
    public void verifierVisibleMaintenant(By locator) {
        web.shouldBeCurrentlyVisible(locator);
    }

    @Step("Vérifier que {0} n'est pas visible")
    public void verifierNonVisible(By locator) {
        web.shouldNotBeVisible(locator);
    }

    @Step("Vérifier que {0} est présent")
    public void verifierPresent(By locator) {
        web.shouldBePresent(locator);
    }

    @Step("Vérifier que {0} n'est pas présent")
    public void verifierAbsent(By locator) {
        web.shouldNotBePresent(locator);
    }

    @Step("Vérifier que {0} est activé")
    public void verifierActive(By locator) {
        web.shouldBeEnabled(locator);
    }

    @Step("Vérifier que {0} n'est pas activé")
    public void verifierDesactive(By locator) {
        web.shouldNotBeEnabled(locator);
    }

    @Step("Vérifier que {0} est sélectionné")
    public void verifierSelectionne(By locator) {
        web.shouldBeSelected(locator);
    }

    @Step("Vérifier que {0} n'est pas sélectionné")
    public void verifierNonSelectionne(By locator) {
        web.shouldNotBeSelected(locator);
    }

    @Step("Vérifier que {0} contient le texte « {1} »")
    public void verifierContientTexte(By locator, String texte) {
        web.shouldContainText(locator, texte);
    }

    @Step("Vérifier que {0} a exactement le texte « {1} »")
    public void verifierContientExactement(By locator, String texte) {
        web.shouldContainOnlyText(locator, texte);
    }

    @Step("Vérifier que {0} ne contient pas le texte « {1} »")
    public void verifierNeContientPas(By locator, String texte) {
        web.shouldNotContainText(locator, texte);
    }
}
