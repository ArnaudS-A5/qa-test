package org.automation.websync.action;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.EtatSteps;
import org.automation.steps.websync.LectureSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — actions <b>clavier</b> sur formulaire (fixture {@code websync/saisie.html}).
 *
 * <p>Chaque test reproduit un comportement utilisateur complet, une étape par appel au socle, et vérifie l'effet de
 * <b>chaque</b> action juste après elle : une méthode du socle n'est couverte que si son effet est prouvé.
 * Couvre : {@code typeAndTab}, {@code typeAndEnter}, {@code hasFocus}, {@code clear}, {@code submit},
 * {@code containsValue}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class FormulaireClavierTest {

    private static final String FIXTURE = "websync/saisie.html";
    private static final By NOM = By.id("nom");
    private static final By PRENOM = By.id("prenom");
    private static final By VILLE = By.id("ville");
    private static final By RECAP = By.id("recap");
    private static final By NB_SUBMIT = By.id("nb-submit");

    @Steps
    ActionSteps actions;

    @Steps
    LectureSteps lecture;

    @Steps
    EtatSteps etat;

    // ---- T25 : remplir au clavier (Tab, Tab) puis soumettre par Entree ----
    @Test
    @DisplayName("T25 - typeAndTab x2 puis typeAndEnter : focus suivi, valeurs remplacees, formulaire soumis")
    void T25_saisie_tab_tab_entree() {
        actions.ouvrir(FIXTURE);

        actions.saisirPuisTab(NOM, "Dupont");
        assertEquals("Dupont", lecture.valeur(NOM),
                "T25 : typeAndTab doit remplacer la valeur (#nom etait prerempli 'ancien')");
        assertTrue(etat.aLeFocus(PRENOM), "T25 : apres typeAndTab sur #nom, le focus doit etre sur #prenom");

        actions.saisirPuisTab(PRENOM, "Jean");
        assertEquals("Jean", lecture.valeur(PRENOM), "T25 : valeur saisie dans #prenom");
        assertTrue(etat.aLeFocus(VILLE), "T25 : apres typeAndTab sur #prenom, le focus doit etre sur #ville");

        actions.saisirPuisEntree(VILLE, "Lyon");
        assertEquals("1", lecture.texte(NB_SUBMIT), "T25 : Entree doit soumettre le formulaire une seule fois");
        String recap = lecture.texte(RECAP);
        assertEquals("Dupont|Jean|Lyon", recap, "T25 : les trois valeurs saisies doivent parvenir a la soumission");

        System.out.println("[T25] RESULTAT : focus #nom -> #prenom -> #ville | soumission par Entree=1"
                + " | recap=" + recap + " | aucune exception => cas passant confirme");
    }

    // ---- T26 : effacer un champ puis soumettre le formulaire ----
    @Test
    @DisplayName("T26 - clear puis submit : champ vide, formulaire soumis sans la valeur effacee")
    void T26_effacer_puis_soumettre() {
        actions.ouvrir(FIXTURE);
        actions.saisir(NOM, "Martin");
        actions.saisir(PRENOM, "Temporaire");
        assertTrue(etat.contientValeur(PRENOM, "Temporaire"), "T26 : containsValue doit etre vrai apres la saisie");

        actions.vider(PRENOM);
        assertEquals("", lecture.valeur(PRENOM), "T26 : clear doit vider #prenom");
        assertFalse(etat.contientValeur(PRENOM, "Temporaire"), "T26 : containsValue doit etre faux apres clear");

        actions.saisir(VILLE, "Paris");
        actions.soumettre(VILLE);
        assertEquals("1", lecture.texte(NB_SUBMIT), "T26 : submit doit soumettre le formulaire une seule fois");
        String recap = lecture.texte(RECAP);
        assertEquals("Martin||Paris", recap, "T26 : la soumission doit transmettre #prenom vide");

        System.out.println("[T26] RESULTAT : #prenom vide apres clear | soumission par submit=1"
                + " | recap=" + recap + " | aucune exception => cas passant confirme");
    }
}
