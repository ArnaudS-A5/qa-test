package org.automation.failure;

import com.example.qa.secret.Secret;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.CheckboxesSteps;
import org.automation.steps.LoginSteps;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Spike — PRODUCTEUR d'artefacts KO pour valider la capture du DOM brut du socle (option A).
 *
 * <p>Deux tests qui échouent <b>volontairement</b>, chacun sur une <b>page distincte</b>
 * (LOGIN vs CHECKBOXES), exécutés <b>en parallèle</b> ({@link Execution} CONCURRENT) afin de
 * stresser le pont {@code ThreadLocal} du socle entre {@code stepFailed} (capture) et
 * {@code executionFinished} (écriture). Sur chaque échec, le socle écrit
 * {@code target/qa-results/KO__{ENV}__{Test}__{ts}/{step}.html}.
 *
 * <p>Les assertions de validation (points 1, 2, 3) sont dans {@link RawDomVerifierTest},
 * qui relit ces artefacts. Découplage producteur/vérificateur car l'artefact n'est écrit qu'au
 * {@code executionFinished}, donc APRÈS la méthode de test.
 *
 * <p><b>Garde-fou</b> : {@code @Disabled} (échoue à dessein → hors build normal). Lancer explicitement :
 * <pre>
 * rm -rf target/qa-results
 * mvn test -Dtest=RawDomProducerTest \
 *   -Djunit.jupiter.conditions.deactivate='*DisabledCondition' \
 *   -Dmaven.test.failure.ignore=true -Denvironment=recette
 * </pre>
 */
@ExtendWith(SerenityJUnit5Extension.class)
@Execution(ExecutionMode.CONCURRENT)
@Disabled("Spike capture DOM brut : échoue à dessein. Lancer via "
        + "-Djunit.jupiter.conditions.deactivate='*DisabledCondition'. Cf. javadoc / README.")
class RawDomProducerTest {

    @Steps
    LoginSteps loginSteps;

    @Steps
    CheckboxesSteps checkboxesSteps;

    @Test
    @DisplayName("T19 - NON PASS - Echec page LOGIN - capture du DOM brut")
    void T19_fail_login_page() {
        loginSteps.openLoginPage();
        loginSteps.loginAs("tomsmith", Secret.of("mauvais-mot-de-passe"));
        loginSteps.shouldSeeLoginSuccess(); // échoue sur la page de login
    }

    @Test
    @DisplayName("T19 - NON PASS - Echec page CHECKBOXES - capture du DOM brut")
    void T19_fail_checkboxes_page() {
        checkboxesSteps.openCheckboxesPage();
        checkboxesSteps.failAfterPageLoaded(); // échoue sur la page checkboxes
    }
}
