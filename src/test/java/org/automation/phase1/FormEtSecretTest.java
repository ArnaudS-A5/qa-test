package org.automation.phase1;

import com.example.qa.api.secret.Secret;
import com.example.qa.api.sync.SyncTimeout;
import com.example.qa.api.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.util.concurrent.atomic.AtomicBoolean;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PHASE 1 — Surface d'interaction sur formulaire, Secret, et should* dur.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class FormEtSecretTest {

    private WebSync openForm() {
        getDriver().get(Fixtures.url("form.html", null));
        return new WebSync(SyncTimeout.LONG);
    }

    // ---- Surface (support de S1/S2 en local) : type, select, checkbox, submit, lectures ----
    @Test
    @DisplayName("Surface - type/select/checkbox/submit + lectures getValue/getText via WebSync")
    void surface_interactions() {
        WebSync web = openForm();

        web.type(By.id("text"), "bonjour");
        assertEquals("bonjour", web.getValue(By.id("text")), "getValue apres type");

        web.selectByValue(By.id("select"), "b");
        assertEquals("Option B", web.getFirstSelectedOptionVisibleText(By.id("select")),
                "select par value");

        web.click(By.id("checkbox"));
        assertTrue(web.isSelected(By.id("checkbox")), "checkbox coche apres click");

        web.click(By.id("submit"));
        assertTrue(web.containsText(By.id("submitted"), "submitted"), "submit effectif");
    }

    // ---- T22 : type(By, Secret) ----
    // A9 (briefing) : le projet qui VALIDE le masquage ne doit pas l'ENFREINDRE.
    // => aucune sortie du clair, secret jamais dans un message d'assertion, comparaison
    //    sans restituer les operandes, valeur fictive dediee non journalisee.
    @Test
    @DisplayName("T22 - type(By, Secret) : saisie effective, sans jamais restituer le secret")
    void T22_type_secret() {
        WebSync web = openForm();
        // Valeur fictive dediee, jamais journalisee ni affichee dans un message.
        String expected = "Fixture-Secret-T22";
        web.type(By.id("password"), Secret.of(expected));
        // Comparaison booleenne : pas de println, pas de secret dans le message d'echec.
        String actual = web.getDomProperty(By.id("password"), "value");
        boolean saisieConforme = expected.equals(actual);
        assertTrue(saisieConforme,
                "T22 : saisie du secret effective (valeur volontairement non restituee)");
    }

    // ---- T16 : should* dur (soft=false) : leve AssertionError et stoppe la suite ----
    // Correction (briefing) : le contrat (BF-SYNC-09 + soft-assert D22-bis) impose une
    // AssertionError en mode dur ; le socle est CORRECT. On verifie : type d'erreur,
    // message citant le locator, et NON-execution de l'instruction suivante.
    @Test
    @DisplayName("T16 - should* dur (soft=false) : leve AssertionError et stoppe la suite")
    void T16_should_dur() {
        openForm(); // charge form.html (#missing est volontairement absent)
        AtomicBoolean apres = new AtomicBoolean(false);
        AssertionError err = assertThrows(AssertionError.class, () -> {
            new WebSync(SyncTimeout.SHORT).shouldBeVisible(By.id("missing"));
            apres.set(true); // ne doit jamais etre atteint : l'assertion dure stoppe la suite
        }, "T16 : should* dur (soft=false) doit lever AssertionError");
        System.out.println("[T16] " + err.getMessage());
        assertFalse(apres.get(),
                "T16 : l'instruction apres un should* dur ne doit pas s'executer");
        assertTrue(err.getMessage() != null && err.getMessage().contains("missing"),
                "T16 : le message doit citer le locator (#missing), msg=" + err.getMessage());
    }
}

