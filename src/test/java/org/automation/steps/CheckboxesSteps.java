package org.automation.steps;

import net.serenitybdd.annotations.Step;
import org.automation.pages.CheckboxesPage;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Steps de la page /checkboxes pour le spike de capture DOM. La step finale échoue
 * <b>volontairement</b> APRÈS chargement réel de la page, pour que le {@code stepFailed} du socle
 * capte le DOM brut de /checkboxes avec un driver vivant.
 */
public class CheckboxesSteps {

    CheckboxesPage checkboxesPage;

    @Step("Ouvrir la page des cases à cocher")
    public void openCheckboxesPage() {
        checkboxesPage.open();
    }

    @Step("Échec volontaire après chargement de /checkboxes")
    public void failAfterPageLoaded() {
        boolean loaded = checkboxesPage.isFormDisplayed(); // touche le DOM réel (/checkboxes chargé)
        fail("Échec volontaire (page chargée=" + loaded + ") pour capturer le DOM brut de /checkboxes");
    }
}
