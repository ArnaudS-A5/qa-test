package org.automation.phase1;

import com.example.qa.api.exception.SyncException;
import com.example.qa.api.secret.Secret;
import com.example.qa.api.sync.SyncTimeout;
import com.example.qa.api.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Test
    @DisplayName("T22 - type(By, Secret) : la saisie du password fonctionne")
    void T22_type_secret() {
        WebSync web = openForm();
        Secret secret = Secret.of("SuperSecretPassword!");
        web.type(By.id("password"), secret);
        // La value reelle est bien saisie (verifiable via getDomProperty("value")).
        String value = web.getDomProperty(By.id("password"), "value");
        System.out.println("[T22] password value (via DOM property) = " + value);
        assertEquals("SuperSecretPassword!", value, "T22 : saisie du secret effective");
    }

    // ---- T16 : should* dur (soft=false) : echoue a la 1re assertion ----
    @Test
    @DisplayName("T16 - should* dur (soft=false) : echec a la 1re assertion sur element absent")
    void T16_should_dur() {
        WebSync web = openForm();
        // Element absent : shouldBeVisible doit lever (dur, soft=false par defaut).
        SyncException ex = assertThrows(SyncException.class,
                () -> new WebSync(SyncTimeout.SHORT).shouldBeVisible(By.id("missing")),
                "T16 : should* dur doit lever immediatement (soft=false)");
        System.out.println("[T16] " + ex.getMessage());
        assertTrue(ex.getMessage().contains("[ABSENT]") || ex.getMessage().contains("[HIDDEN]"),
                "T16 : diagnostic attendu, msg=" + ex.getMessage());
    }
}

