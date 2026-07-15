package org.automation;

import com.example.qa.api.secret.Secret;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.LoginSteps;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests d'authentification — démonstration de la <b>consommation du socle qa-socle</b>.
 *
 * <p>Deux registres :
 * <ul>
 *   <li><b>{@link #valid_user_can_log_in()}</b> — passe : smoke prouvant que la chaîne
 *       Selenium/Serenity/JUnit 5 (toutes <i>transitives</i> via {@code qa-socle}) tourne
 *       de bout en bout. Le mot de passe est un {@link Secret} du socle (masqué dans les rapports).</li>
 *   <li><b>{@link #invalid_login_is_captured_by_qa_socle()}</b> — échoue <b>volontairement</b>
 *       pour exercer le <i>seul</i> comportement runtime réellement implémenté du socle :
 *       {@code TestFailureManager}. Auto-enregistré comme {@code TestExecutionListener} JUnit
 *       (via {@code META-INF/services}, activation par simple présence du jar — D16), il produit
 *       sans une ligne de code ici le dossier
 *       {@code target/qa-results/KO__{ENV}__{Test}__{timestamp}/} avec {@code ERROR.log},
 *       {@code FAIL.log} (historique des steps — le {@link Secret} y apparaît masqué) et le dump
 *       HTML de la step en échec.</li>
 * </ul>
 *
 * <p>Le test KO est {@code @Disabled} (il échoue à dessein → sinon le build serait rouge). Pour le
 * voir produire les artefacts, le réactiver à la demande :
 * {@code mvn test -Dtest='AuthenticationTest#invalid_login_is_captured_by_qa_socle'
 * -Djunit.jupiter.conditions.deactivate='*DisabledCondition'
 * -Dmaven.test.failure.ignore=true -Denvironment=recette}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class AuthenticationTest {

    @Steps
    LoginSteps loginSteps;

    @Test
    @DisplayName("Un utilisateur valide accède à la zone sécurisée")
    void valid_user_can_log_in() {
        loginSteps.openLoginPage();
        loginSteps.loginAs("tomsmith", Secret.of("SuperSecretPassword!"));
        loginSteps.shouldSeeLoginSuccess();
    }

    @Test
    @Disabled("Échoue à dessein (démo capture KO du socle). Réactiver via "
            + "-Djunit.jupiter.conditions.deactivate='*DisabledCondition'. Cf. javadoc / README.")
    @DisplayName("Un échec de connexion est capturé en artefacts par le socle")
    void invalid_login_is_captured_by_qa_socle() {
        loginSteps.openLoginPage();
        loginSteps.loginAs("tomsmith", Secret.of("mauvais-mot-de-passe"));
        loginSteps.shouldSeeLoginSuccess(); // échec attendu : déclenche TestFailureManager
    }
}
