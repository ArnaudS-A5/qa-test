package org.automation.steps;

import com.example.qa.api.secret.Secret;
import net.serenitybdd.annotations.Step;
import org.automation.pages.LoginPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps métier de connexion (couche d'orchestration du Page Object Model).
 *
 * <p><b>Consommation du socle</b> : le mot de passe transite en {@link Secret} (API
 * {@code com.example.qa.api.secret}, fournie par {@code qa-socle}). Le placeholder
 * {@code {1}} de l'annotation {@link Step} rend {@code secret.toString()}, donc la
 * description de step remontée à Serenity — et reprise telle quelle dans le
 * {@code FAIL.log} produit par le socle en cas d'échec — affiche la valeur
 * <b>masquée</b> (« prévention à la source » de D12), jamais le clair. La seule
 * déballe du clair est {@code secret.value()}, à l'unique point de saisie.
 */
public class LoginSteps {

    LoginPage loginPage;

    @Step("Ouvrir la page de connexion")
    public void openLoginPage() {
        loginPage.open();
    }

    @Step("Se connecter en tant que {0} avec le secret {1}")
    public void loginAs(String username, Secret password) {
        loginPage.enterCredentials(username, password.value());
        loginPage.submit();
    }

    @Step("Vérifier que la connexion a réussi")
    public void shouldSeeLoginSuccess() {
        String flash = loginPage.flashMessageText();
        assertTrue(flash.contains("You logged into a secure area!"),
                "Message de succès attendu, obtenu : " + flash);
    }
}
