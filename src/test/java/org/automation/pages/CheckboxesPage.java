package org.automation.pages;

import net.serenitybdd.annotations.DefaultUrl;
import net.serenitybdd.core.annotations.findby.FindBy;
import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.core.pages.WebElementFacade;

/**
 * Page Object d'une <b>seconde</b> page distincte (https://the-internet.herokuapp.com/checkboxes),
 * utilisée par le spike de capture DOM pour vérifier qu'en parallèle chaque artefact KO porte le DOM
 * de SA page (pas de fuite entre threads via le {@code ThreadLocal} du socle).
 *
 * <p>Marqueur unique de cette page : l'élément {@code #checkboxes} (absent de la page de login).
 */
@DefaultUrl("/checkboxes")
public class CheckboxesPage extends PageObject {

    @FindBy(id = "checkboxes")
    private WebElementFacade checkboxesForm;

    /** Force la résolution réelle de l'élément → garantit que le DOM de /checkboxes est chargé. */
    public boolean isFormDisplayed() {
        return checkboxesForm.isCurrentlyVisible();
    }
}
