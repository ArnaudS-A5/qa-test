package org.automation.config.precedence;

import com.example.qa.sync.WebSync;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.thucydides.model.environment.SystemEnvironmentVariables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code precedence} — précédence -D &gt; serenity.conf (T24). Exécution « precedence » du pom,
 * qui surcharge {@code environment} : isolée pour que le reste de la campagne garde {ENV}=QATEST dans
 * le nommage des artefacts KO__.
 *
 * <p>La preuve exige deux sources en <b>désaccord</b> sur la même clé : les deux préalables (valeur -D
 * posée, valeur serenity.conf différente) sont vérifiés avant la précédence elle-même. Aucune valeur
 * n'est écrite ici : -D est lu (posé par le pom), serenity.conf est lu seul (Typesafe Config).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class PrecedenceConfigTest {

    private static final String ENVIRONMENT = "environment";

    // ---- T24 : precedence -D > serenity.conf ----
    @Test
    @DisplayName("T24 - PASS - precedence -D > serenity.conf : la valeur -D gagne")
    void T24_system_property_wins() {
        // Prealable 1 : la valeur -D, posee par l'execution « precedence » du pom.
        String systemValue = System.getProperty(ENVIRONMENT);
        assertNotNull(systemValue, "T24 : -D" + ENVIRONMENT + " absent : ce test appartient au groupe 'precedence'"
                + " (package config.precedence) et tourne via `mvn verify` (execution precedence du pom),"
                + " ou avec -D" + ENVIRONMENT + "=<valeur>.");

        // Prealable 2 : la valeur de serenity.conf SEUL, qui doit differer de -D (sinon rien n'est prouve).
        Config conf = ConfigFactory.parseResources("serenity.conf");
        assertTrue(conf.hasPath(ENVIRONMENT), "T24 : serenity.conf doit definir '" + ENVIRONMENT + "'");
        String confValue = conf.getString(ENVIRONMENT);
        assertNotEquals(confValue, systemValue, "T24 : -D et serenity.conf valent tous deux '" + systemValue
                + "' : la precedence ne serait pas prouvee");

        // Preuve : la valeur resolue par la configuration (celle que lit le socle) est celle de -D.
        String resolved = SystemEnvironmentVariables.createEnvironmentVariables().getProperty(ENVIRONMENT);
        assertEquals(systemValue, resolved, "T24 : la valeur -D (" + systemValue + ") doit primer sur serenity.conf ("
                + confValue + ")");

        // Sanity : avec cette config, un WebSync fonctionne (predicat sur absent => false, sans exception).
        assertFalse(new WebSync().isPresent(By.id("nimporte")),
                "T24 : WebSync operationnel attendu (isPresent sur element inexistant => false)");
        System.out.println("[T24] RESULTAT : serenity.conf=" + confValue + " | -D=" + systemValue
                + " | resolue=" + resolved + " | aucune exception => precedence -D confirmee, cas passant confirme");
    }
}
