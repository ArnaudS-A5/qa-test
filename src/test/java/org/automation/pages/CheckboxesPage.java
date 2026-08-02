package org.automation.pages;

import com.example.qa.api.sync.WebSync;
import net.serenitybdd.annotations.DefaultUrl;
import net.serenitybdd.core.pages.PageObject;
import org.openqa.selenium.By;

/**
 * Page Object d'une <b>seconde</b> page distincte (https://the-internet.herokuapp.com/checkboxes),
 * utilisée par le spike de capture DOM pour vérifier qu'en parallèle chaque artefact KO porte le DOM
 * de SA page (pas de fuite entre threads via le {@code ThreadLocal} du socle).
 *
 * <p>Marqueur unique de cette page : l'élément {@code #checkboxes} (absent de la page de login).
 *
 * <p><b>Migré vers {@code WebSync}</b> (locators {@link By}, synchro du socle) — cf.
 * {@link LoginPage}. La navigation ({@code open()}) reste fournie par {@link PageObject}.
 */
@DefaultUrl("/checkboxes")
public class CheckboxesPage extends PageObject {

    private static final By CHECKBOXES_FORM = By.id("checkboxes");

    private WebSync web;

    private WebSync web() {
        if (web == null) {
            web = new WebSync();
        }
        return web;
    }

    /** Force la résolution réelle de l'élément → garantit que le DOM de /checkboxes est chargé. */
    public boolean isFormDisplayed() {
        return web().isVisible(CHECKBOXES_FORM);
    }
}
