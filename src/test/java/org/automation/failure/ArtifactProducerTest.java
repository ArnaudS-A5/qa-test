package org.automation.failure;

import com.example.qa.secret.Secret;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static net.serenitybdd.core.Serenity.getDriver;

/**
 * PHASE 1 — PRODUCTEUR d'artefacts KO (support de T18, T19, T20, T23).
 *
 * <p>Ce test échoue <b>volontairement</b> APRÈS avoir rempli un {@code input[type=password]}, sur la
 * fixture {@code form.html}. Il déclenche le socle (3 services actifs par présence du jar, T23) :
 * production d'un dossier {@code KO__{ENV}__...} avec {@code ERROR.log}, {@code FAIL.log} et le dump
 * HTML de la step en échec (DOM réel, T19), où la {@code value} du password est blanchie (T20).
 *
 * <p>{@code @Disabled} par défaut (échoue à dessein). Réactiver via
 * {@code -Djunit.jupiter.conditions.deactivate='*DisabledCondition' -Dmaven.test.failure.ignore=true}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
@Disabled("Producteur d'artefacts KO (echec volontaire). Reactiver via "
        + "-Djunit.jupiter.conditions.deactivate='*DisabledCondition'.")
class ArtifactProducerTest {

    @Test
    @DisplayName("T18 - NON PASS - Echec volontaire apres saisie password - produit artefacts KO du socle")
    void T18_fail_after_password() {
        getDriver().get(Fixtures.url("form.html", null));
        WebSync web = new WebSync(SyncTimeout.LONG);

        // Saisie d'un password (via Secret) : sa value doit etre blanchie dans le dump HTML (T20).
        web.type(By.id("password"), Secret.of("SuperSecretPassword!"));
        web.type(By.id("text"), "valeur-visible");

        // Echec volontaire : element absent => SyncException => test KO => TestFailureManager.
        web.click(By.id("bouton-inexistant"));
    }
}

