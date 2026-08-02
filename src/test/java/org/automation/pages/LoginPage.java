package org.automation.pages;

import com.example.qa.api.secret.Secret;
import com.example.qa.api.sync.WebSync;
import net.serenitybdd.annotations.DefaultUrl;
import com.example.qa.api.page.QaPageObject;
import net.serenitybdd.core.pages.PageObject;
import org.openqa.selenium.By;

/**
 * Page Object de l'écran de connexion (https://the-internet.herokuapp.com/login).
 *
 * <p>Page Object Model imposé par le socle : aucune logique de test ici, uniquement la
 * cartographie des locators ({@link By}) et les interactions de bas niveau, portées par le
 * {@link WebSync} du socle. L'orchestration est dans les {@code Steps}
 * (cf. {@link org.automation.steps.LoginSteps}).
 *
 * <p><b>Migré de l'API Serenity native ({@code WebElementFacade}) vers {@code WebSync}</b> : les
 * attentes sont désormais gouvernées par le moteur de synchro du socle (profils
 * {@code qa.sync.timeout.*}, diagnostic {@code [ABSENT]/[HIDDEN]/…}, contrat no-leak), et non plus
 * par les timeouts Serenity par défaut. La navigation ({@code open()}) reste fournie par
 * {@link PageObject} (via {@code @DefaultUrl} + {@code webdriver.base.url}).
 */
@DefaultUrl("/login")
public class LoginPage extends QaPageObject {

    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT = By.cssSelector("button[type='submit']");
    private static final By FLASH = By.id("flash");


    public void enterCredentials(String username, Secret password) {
        web.type(USERNAME, username);
        // Secret transmis tel quel : le masquage à la saisie est géré par le socle (aucun clair ici).
        web.type(PASSWORD, password);
    }

    public void submit() {
        web.click(SUBMIT);
    }

    public String flashMessageText() {
        web.waitUntilVisible(FLASH);
        return web.getText(FLASH).trim();
    }
}
