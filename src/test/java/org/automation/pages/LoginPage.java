package org.automation.pages;

import net.serenitybdd.core.pages.PageObject;
import net.serenitybdd.core.pages.WebElementFacade;
import net.serenitybdd.core.annotations.findby.FindBy;
import net.serenitybdd.annotations.DefaultUrl;

/**
 * Page Object de l'écran de connexion (https://the-internet.herokuapp.com/login).
 *
 * <p>Page Object Model imposé par le socle : aucune logique de test ici, uniquement
 * la cartographie des éléments et les interactions de bas niveau. L'orchestration est
 * portée par les {@code Steps} (cf. {@link org.automation.steps.LoginSteps}).
 *
 * <p>{@link PageObject} provient de Serenity, fourni <b>transitivement</b> par
 * {@code qa-socle} (aucune dépendance Serenity n'est déclarée par ce projet).
 */
@DefaultUrl("/login")
public class LoginPage extends PageObject {

    @FindBy(id = "username")
    private WebElementFacade usernameField;

    @FindBy(id = "password")
    private WebElementFacade passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElementFacade submitButton;

    @FindBy(id = "flash")
    private WebElementFacade flashMessage;

    public void enterCredentials(String username, String password) {
        usernameField.type(username);
        passwordField.type(password);
    }

    public void submit() {
        submitButton.click();
    }

    public String flashMessageText() {
        return flashMessage.waitUntilVisible().getText().trim();
    }
}