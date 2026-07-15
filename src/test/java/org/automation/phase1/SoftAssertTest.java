package org.automation.phase1;

import com.example.qa.api.sync.SyncTimeout;
import com.example.qa.api.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static net.serenitybdd.core.Serenity.getDriver;

/**
 * PHASE 1 — T17 : soft-assert (qa.assertions.soft=true).
 *
 * <p>Avec le mode soft ACTIF, plusieurs {@code should*} en échec ne stoppent pas au premier :
 * ils sont <b>collectés</b>, et l'échec global est levé <b>en fin</b> d'exécution par
 * l'extension JUnit auto-détectée ({@code SoftAssertionExtension}) — aucune annotation côté test.
 *
 * <p>Ce test échoue donc à dessein (deux {@code should*} KO agrégés). {@code @Disabled} par défaut ;
 * lancer avec :
 * {@code -Dqa.assertions.soft=true -Djunit.jupiter.conditions.deactivate='*DisabledCondition'
 * -Dmaven.test.failure.ignore=true}. La preuve d'agrégation = les deux échecs figurent dans FAIL.log.
 */
@ExtendWith(SerenityJUnit5Extension.class)
@Disabled("T17 soft-assert : echoue a dessein. Lancer avec -Dqa.assertions.soft=true et "
        + "-Djunit.jupiter.conditions.deactivate='*DisabledCondition'.")
class SoftAssertTest {

    @Test
    @DisplayName("T17 - plusieurs should* KO agreges, echec en fin (soft=true)")
    void soft_assertions_agregees() {
        getDriver().get(Fixtures.url("form.html", null));
        WebSync web = new WebSync(SyncTimeout.SHORT);

        // En mode soft, ces deux assertions KO doivent etre collectees (pas d'arret a la 1re).
        web.shouldBeVisible(By.id("premier-absent"));   // KO 1 collecte
        web.shouldContainText(By.id("text"), "IMPOSSIBLE"); // KO 2 collecte

        // Marqueur : si on arrive ici, c'est que le mode soft n'a PAS stoppe a la 1re (attendu).
        System.out.println("[T17] les deux should* KO ont ete collectes (soft actif) — echec attendu en fin.");
    }
}

