package org.automation.websync.action;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.StateSteps;
import org.automation.steps.websync.ReadSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — actions <b>clavier</b> sur formulaire (fixture {@code websync/action/keyboard.html}).
 *
 * <p>Chaque test reproduit un comportement utilisateur complet, une étape par appel au socle, et vérifie l'effet de
 * <b>chaque</b> action juste après elle : une méthode du socle n'est couverte que si son effet est prouvé.
 * Couvre : {@code typeAndTab}, {@code typeAndEnter}, {@code hasFocus}, {@code clear}, {@code submit},
 * {@code containsValue}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ActionKeyboardTest {

    private static final String FIXTURE = "websync/action/keyboard.html";
    private static final By LAST_NAME = By.id("nom");
    private static final By FIRST_NAME = By.id("prenom");
    private static final By CITY = By.id("ville");
    private static final By RECAP = By.id("recap");
    private static final By SUBMIT_COUNT = By.id("nb-submit");

    @Steps
    ActionSteps actions;

    @Steps
    ReadSteps read;

    @Steps
    StateSteps state;

    // ---- T25 : remplir au clavier (Tab, Tab) puis soumettre par Entree ----
    @Test
    @DisplayName("T25 - PASS - typeAndTab x2 puis typeAndEnter : focus suivi, valeurs remplacees, formulaire soumis")
    void T25_typeAndTab_typeAndEnter() {
        actions.open(FIXTURE);

        actions.typeAndTab(LAST_NAME, "Dupont");
        assertEquals("Dupont", read.getValue(LAST_NAME),
                "T25 : typeAndTab doit remplacer la valeur (#nom etait prerempli 'ancien')");
        assertTrue(state.hasFocus(FIRST_NAME), "T25 : apres typeAndTab sur #nom, le focus doit etre sur #prenom");

        actions.typeAndTab(FIRST_NAME, "Jean");
        assertEquals("Jean", read.getValue(FIRST_NAME), "T25 : valeur saisie dans #prenom");
        assertTrue(state.hasFocus(CITY), "T25 : apres typeAndTab sur #prenom, le focus doit etre sur #ville");

        actions.typeAndEnter(CITY, "Lyon");
        assertEquals("1", read.getText(SUBMIT_COUNT), "T25 : Entree doit soumettre le formulaire une seule fois");
        String recap = read.getText(RECAP);
        assertEquals("Dupont|Jean|Lyon", recap, "T25 : les trois valeurs saisies doivent parvenir a la soumission");

        System.out.println("[T25] RESULTAT : focus #nom -> #prenom -> #ville | soumission par Entree=1"
                + " | recap=" + recap + " | aucune exception => cas passant confirme");
    }

    // ---- T26 : effacer un champ puis soumettre le formulaire ----
    @Test
    @DisplayName("T26 - PASS - clear puis submit : champ vide, formulaire soumis sans la valeur effacee")
    void T26_clear_submit() {
        actions.open(FIXTURE);
        actions.type(LAST_NAME, "Martin");
        actions.type(FIRST_NAME, "Temporaire");
        assertTrue(state.containsValue(FIRST_NAME, "Temporaire"), "T26 : containsValue doit etre vrai apres la saisie");

        actions.clear(FIRST_NAME);
        assertEquals("", read.getValue(FIRST_NAME), "T26 : clear doit vider #prenom");
        assertFalse(state.containsValue(FIRST_NAME, "Temporaire"), "T26 : containsValue doit etre faux apres clear");

        actions.type(CITY, "Paris");
        actions.submit(CITY);
        assertEquals("1", read.getText(SUBMIT_COUNT), "T26 : submit doit soumettre le formulaire une seule fois");
        String recap = read.getText(RECAP);
        assertEquals("Martin||Paris", recap, "T26 : la soumission doit transmettre #prenom vide");

        System.out.println("[T26] RESULTAT : #prenom vide apres clear | soumission par submit=1"
                + " | recap=" + recap + " | aucune exception => cas passant confirme");
    }
}
