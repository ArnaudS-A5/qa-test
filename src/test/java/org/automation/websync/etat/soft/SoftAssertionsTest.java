package org.automation.websync.etat.soft;

import com.example.qa.sync.SoftAssertionExtension;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.model.environment.EnvironmentSpecificConfiguration;
import net.thucydides.model.environment.SystemEnvironmentVariables;
import org.automation.steps.websync.EtatSteps;
import org.automation.steps.websync.SelectSteps;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code soft} — assertions <b>souples</b> ({@code qa.assertions.soft=true} et SHORT à 1 s, posés par
 * l'exécution « soft » du pom). Fixture {@code websync/soft.html}, dont les éléments sont rangés dans l'ordre des
 * vérifications.
 *
 * <p>Contrat du socle : en mode soft, un {@code should*} en échec est <b>collecté</b> et l'exécution continue ; dans
 * une étape, il la rend <b>rouge</b> (sous-étape portant le message, capture, dump {@code {step}.html}) sans faire
 * sauter les suivantes ; en fin de test, l'extension {@code SoftAssertionExtension} (auto-détectée) <b>vote le KO</b>
 * en levant une {@code AssertionError} agrégée (un {@code suppressed} par échec, dans l'ordre) et purge le collecteur.
 *
 * <p>Une même suite de 16 {@code should*} (13 KO, 3 OK, la dernière OK) est jouée deux fois :
 * <ol>
 *   <li><b>le bloc</b>, hors étape : le test déclenche lui-même le vote de fin et <b>vérifie dans le code</b> qu'il
 *       rend un seul agrégat de exactement 13 échecs, dans l'ordre, les 3 OK exclus, collecteur purgé ;</li>
 *   <li><b>les étapes</b> : la même suite, une étape par {@code should*}, à <b>contrôler dans le rapport</b> : 13
 *       étapes rouges avec leur capture, 3 vertes dont la dernière, aucune sautée, un {@code {step}.html} par échec
 *       dans {@code qa-results/KO__…} ;</li>
 *   <li><b>la fin</b> : JUnit déclenche le vrai vote de fin, le test finit <b>rouge</b> avec les 13 échecs des
 *       étapes.</li>
 * </ol>
 * Le bloc passe <b>avant</b> les étapes : sinon son agrégat compterait aussi les échecs des étapes (26).
 *
 * <p><b>Pas d'{@code assertThrows} pour le vote du bloc</b> : Serenity 4.2.22 intercepte tout {@code assertThrows}
 * et retient son type comme exception « attendue » du test ({@code SerenityTestExecutionListener
 * .addExpectedException}). En fin de test, si le test a échoué sur ce type, il <b>efface</b> toutes les étapes en échec
 * par ce type ({@code BaseStepListener.exceptionExpected} → {@code TestOutcome.resetFailingStepsCausedBy}) : les 13
 * étapes rouges redeviendraient vertes. L'agrégat est donc capturé par un {@code try/catch}.
 *
 * <p><b>Rouge à dessein</b>, donc {@code @Disabled} : hors campagne, lancé à la main par l'exécution « soft » du pom,
 * qui porte ses clés (commande : README, « Lancer les tests » §6). Un vrai défaut du bloc se distingue du rouge
 * attendu par son message (« T82 : … ») : le test s'arrête alors avant les étapes.
 */
@ExtendWith(SerenityJUnit5Extension.class)
@Disabled("T82 soft-assert : rouge a dessein. Lancer a la main (README, « Lancer les tests » §6) : mvn clean"
        + " test-compile surefire:test@soft serenity:aggregate -Djunit.jupiter.conditions.deactivate='*DisabledCondition'")
class SoftAssertionsTest {

    /** Clé du mode soft ({@code SoftAssertions.SOFT_KEY}, classe non publique du socle). */
    private static final String SOFT_KEY = "qa.assertions.soft";

    private static final String FIXTURE = "websync/soft.html";
    private static final By ERREUR = By.id("erreur");
    private static final By CONFIRMATION = By.id("confirmation");
    private static final By CODE_PROMO = By.id("code-promo");
    private static final By ANNULER = By.id("annuler");
    private static final By PAYER = By.id("payer");
    private static final By NEWSLETTER = By.id("newsletter");
    private static final By CGV = By.id("cgv");
    private static final By LIVRAISON = By.id("livraison");

    @Steps
    EtatSteps etat;

    @Steps
    SelectSteps select;

    // ---- T82 : 16 should* (13 KO, 3 OK) en bloc puis en etapes ----
    @Test
    @DisplayName("T82 - soft : 16 should* (13 KO, 3 OK) en bloc puis en etapes, bloc = 13 echecs dans l'ordre, rouge a dessein")
    void T82_should_soft_bloc_puis_etapes() {
        String soft = EnvironmentSpecificConfiguration.from(SystemEnvironmentVariables.createEnvironmentVariables())
                .getOptionalProperty(SOFT_KEY).orElse("absent");
        assertEquals("true", soft, "T82 : prealable, " + SOFT_KEY + " doit valoir true : lancer par l'execution"
                + " soft du pom (README, « Lancer les tests » §6)");

        etat.avecProfil(SyncTimeout.SHORT);
        select.avecProfil(SyncTimeout.SHORT);
        etat.ouvrir(FIXTURE);

        // ---- Le bloc : la suite en appels directs, hors etape ----
        WebSync web = new WebSync(SyncTimeout.SHORT);
        web.shouldBeVisible(ERREUR);                                // 1  KO
        web.shouldBeCurrentlyVisible(ERREUR);                       // 2  KO
        web.shouldNotBeVisible(CONFIRMATION);                       // 3  KO
        web.shouldBePresent(CODE_PROMO);                            // 4  KO
        web.shouldNotBePresent(CONFIRMATION);                       // 5  KO
        web.shouldBeEnabled(ANNULER);                               // 6  KO
        web.shouldNotBeEnabled(PAYER);                              // 7  KO
        web.shouldBeVisible(CONFIRMATION);                          // 8  OK
        web.shouldBeSelected(NEWSLETTER);                           // 9  KO
        web.shouldNotBeSelected(CGV);                               // 10 KO
        web.shouldContainText(CONFIRMATION, "annulée");             // 11 KO
        web.shouldContainOnlyText(CONFIRMATION, "Commande");        // 12 KO
        web.shouldNotContainText(CONFIRMATION, "validée");          // 13 KO
        web.shouldContainText(CONFIRMATION, "validée");             // 14 OK
        web.shouldContainSelectedOption(LIVRAISON, "Express");      // 15 KO
        web.shouldBeEnabled(PAYER);                                 // 16 OK
        // Arriver ici prouve deja qu'aucun echec n'a stoppe le test ; le nombre d'echecs collectes (13) le confirme.

        // Vote de fin declenche par le test (le contexte n'est pas utilise) : un seul agregat, 13 echecs, dans l'ordre.
        // Capture explicite, pas d'assertThrows : sous Serenity, il effacerait en fin de test les etapes rouges (Javadoc).
        SoftAssertionExtension voteDeFin = new SoftAssertionExtension();
        AssertionError agregat = null;
        try {
            voteDeFin.afterTestExecution(null);
        } catch (AssertionError leve) {
            agregat = leve;
        }
        assertNotNull(agregat, "T82 : le vote de fin doit lever le KO agrege");
        List<String> attendus = List.of("By.id: erreur", "By.id: erreur", "By.id: confirmation", "By.id: code-promo",
                "By.id: confirmation", "By.id: annuler", "By.id: payer", "By.id: newsletter", "By.id: cgv",
                "« annulée » : By.id: confirmation", "« Commande » : By.id: confirmation",
                "« validée » : By.id: confirmation", "« Express » : By.id: livraison");
        Throwable[] echecs = agregat.getSuppressed();
        assertEquals(attendus.size(), echecs.length, "T82 : un suppressed par should* en echec, les 3 OK exclus");
        for (int i = 0; i < echecs.length; i++) {
            assertTrue(echecs[i] instanceof AssertionError && echecs[i].getMessage().endsWith(attendus.get(i)),
                    "T82 : echec n°" + (i + 1) + " attendu sur " + attendus.get(i) + ", recu=" + echecs[i].getMessage());
        }
        assertDoesNotThrow(() -> voteDeFin.afterTestExecution(null), "T82 : le collecteur doit etre purge apres le vote");

        System.out.println("[T82] RESULTAT bloc : exception attendue=AssertionError agregee (vote de fin, soft=true)"
                + " | levee=OUI | type reel=" + agregat.getClass().getName() + " | " + echecs.length + " echecs dans"
                + " l'ordre, 3 OK non collectes | collecteur purge => bloc confirme");

        // ---- Les etapes : la meme suite, une etape par should* (a controler dans le rapport) ----
        etat.verifierVisible(ERREUR);                               // 1  KO
        etat.verifierVisibleMaintenant(ERREUR);                     // 2  KO
        etat.verifierNonVisible(CONFIRMATION);                      // 3  KO
        etat.verifierPresent(CODE_PROMO);                           // 4  KO
        etat.verifierAbsent(CONFIRMATION);                          // 5  KO
        etat.verifierActive(ANNULER);                               // 6  KO
        etat.verifierDesactive(PAYER);                              // 7  KO
        etat.verifierVisible(CONFIRMATION);                         // 8  OK
        etat.verifierSelectionne(NEWSLETTER);                       // 9  KO
        etat.verifierNonSelectionne(CGV);                           // 10 KO
        etat.verifierContientTexte(CONFIRMATION, "annulée");        // 11 KO
        etat.verifierContientExactement(CONFIRMATION, "Commande");  // 12 KO
        etat.verifierNeContientPas(CONFIRMATION, "validée");        // 13 KO
        etat.verifierContientTexte(CONFIRMATION, "validée");        // 14 OK
        select.verifierOptionProposee(LIVRAISON, "Express");        // 15 KO
        etat.verifierActive(PAYER);                                 // 16 OK

        // ---- La fin : le vote de JUnit leve les 13 echecs des etapes, le test finit rouge (a dessein) ----
        System.out.println("[T82] RESULTAT etapes : 16 etapes jouees (13 KO, 3 OK) => a controler dans le rapport ;"
                + " echec de fin attendu : AssertionError agregee de 13 echecs (cas non passant volontaire)");
    }
}
