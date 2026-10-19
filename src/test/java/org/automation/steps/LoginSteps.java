package org.automation.steps;

import com.example.qa.secret.Secret;
import net.serenitybdd.annotations.Step;
import org.automation.pages.LoginPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps métier de connexion (couche d'orchestration du Page Object Model).
 *
 * <p><b>Consommation du socle</b> : le mot de passe transite en {@link Secret} (API
 * {@code com.example.qa.secret}, fournie par {@code qa-socle}). Le placeholder
 * {@code {1}} de l'annotation {@link Step} rend {@code secret.toString()}, donc la
 * description de step remontée à Serenity — et reprise telle quelle dans le
 * {@code FAIL.log} produit par le socle en cas d'échec — affiche la valeur
 * <b>masquée</b> (« prévention à la source » de D12), jamais le clair. Depuis la
 * migration vers {@code WebSync}, <b>aucune déballe du clair</b> n'a lieu dans ce projet :
 * le {@link Secret} est transmis tel quel à {@code WebSync.type(By, Secret)}, qui gère le
 * masquage à la saisie (socle).
 */
public class LoginSteps {

    LoginPage loginPage;

    @Step("Ouvrir la page de connexion")
    public void openLoginPage() {
        loginPage.open();
    }

    @Step("Se connecter en tant que {0} avec le secret {1}")
    public void loginAs(String username, Secret password) {
        loginPage.enterCredentials(username, password);
        loginPage.submit();
    }

    @Step("Vérifier que la connexion a réussi")
    public void shouldSeeLoginSuccess() {
        String flash = loginPage.flashMessageText();
        assertTrue(flash.contains("You logged into a secure area!"),
                "Message de succès attendu, obtenu : " + flash);
    }
}
