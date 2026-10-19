package org.automation.pages;

import com.example.qa.page.QaPageObject;
import net.serenitybdd.annotations.DefaultUrl;
import org.openqa.selenium.By;

/**
 * Page Object d'une <b>seconde</b> page distincte (https://the-internet.herokuapp.com/checkboxes),
 * utilisée par le spike de capture DOM pour vérifier qu'en parallèle chaque artefact KO porte le DOM
 * de SA page (pas de fuite entre threads via le {@code ThreadLocal} du socle).
 *
 * <p>Marqueur unique de cette page : l'élément {@code #checkboxes} (absent de la page de login).
 *
 * <p><b>Basé sur {@link QaPageObject} du socle</b> (champ {@code web} hérité, locators {@link By},
 * synchro maison) — cf. {@link LoginPage}. La navigation ({@code open()}) reste fournie par la
 * {@code PageObject} Serenity sous-jacente.
 */
@DefaultUrl("/checkboxes")
public class CheckboxesPage extends QaPageObject {

    private static final By CHECKBOXES_FORM = By.id("checkboxes");

    /** Force la résolution réelle de l'élément → garantit que le DOM de /checkboxes est chargé. */
    public boolean isFormDisplayed() {
        return web.isVisible(CHECKBOXES_FORM);
    }
}
