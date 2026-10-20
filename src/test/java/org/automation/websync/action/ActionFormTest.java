package org.automation.websync.action;

import com.example.qa.secret.Secret;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.ReadSteps;
import org.automation.steps.websync.SelectSteps;
import org.automation.steps.websync.StateSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — actions sur un formulaire complet (saisie, liste, case, soumission) et saisie d'un
 * {@code Secret}. Fixture {@code form.html}, une étape par appel au socle.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ActionFormTest {

    private static final String FIXTURE = "form.html";
    private static final By TEXT = By.id("text");
    private static final By PASSWORD = By.id("password");
    private static final By SELECT = By.id("select");
    private static final By CHECKBOX = By.id("checkbox");
    private static final By SUBMIT = By.id("submit");
    private static final By SUBMITTED = By.id("submitted");

    @Steps
    ActionSteps actions;

    @Steps
    ReadSteps read;

    @Steps
    SelectSteps select;

    @Steps
    StateSteps state;

    // ---- Surface (support de S1/S2 en local) : type, select, checkbox, submit, lectures ----
    @Test
    @DisplayName("T84 - PASS - Surface : type/select/checkbox/submit + lectures getValue/getText via WebSync")
    void T84_form_interactions() {
        actions.open(FIXTURE);

        actions.type(TEXT, "bonjour");
        assertEquals("bonjour", read.getValue(TEXT), "getValue apres type");

        select.selectByValue(SELECT, "b");
        assertEquals("Option B", select.getFirstSelectedOptionVisibleText(SELECT),
                "select par value");

        actions.click(CHECKBOX);
        assertTrue(state.isSelected(CHECKBOX), "checkbox coche apres click");

        actions.click(SUBMIT);
        assertTrue(state.containsText(SUBMITTED, "submitted"), "submit effectif");
    }

    // ---- T22 : type(By, Secret) ----
    // A9 (briefing) : le projet qui VALIDE le masquage ne doit pas l'ENFREINDRE.
    // => aucune sortie du clair, secret jamais dans un message d'assertion, comparaison
    //    sans restituer les operandes, valeur fictive dediee non journalisee.
    @Test
    @DisplayName("T22 - PASS - type(By, Secret) : saisie effective, sans jamais restituer le secret")
    void T22_type_secret() {
        actions.open(FIXTURE);
        // Valeur fictive dediee, jamais journalisee ni affichee dans un message ; l'etape n'affiche que le rendu masque.
        String expected = "Fixture-Secret-T22";
        actions.type(PASSWORD, Secret.of(expected));
        // Comparaison booleenne : pas de println, pas de secret dans le message d'echec.
        String actual = read.getDomProperty(PASSWORD, "value");
        boolean typedAsExpected = expected.equals(actual);
        assertTrue(typedAsExpected,
                "T22 : saisie du secret effective (valeur volontairement non restituee)");
    }
}
