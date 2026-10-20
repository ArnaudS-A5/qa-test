package org.automation.websync.assertion;

import com.example.qa.sync.SoftAssertionExtension;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.model.environment.EnvironmentSpecificConfiguration;
import net.thucydides.model.environment.SystemEnvironmentVariables;
import org.automation.steps.websync.StateSteps;
import org.automation.steps.websync.SelectSteps;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.util.List;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code soft} — assertions <b>souples</b> ({@code qa.assertions.soft=true} et SHORT à 1 s, posés par
 * l'exécution « soft » du pom, qui sélectionne cette classe par son nom). Fixtures {@code form.html} (T17) et
 * {@code websync/assertion/soft.html} (T82), dont les éléments sont rangés dans l'ordre des vérifications de T82.
 *
 * <p>T17 : deux {@code should*} en échec hors étape, sans vote déclenché par le test ; seul le vrai vote de fin de
 * JUnit les agrège (preuve : les deux échecs dans {@code FAIL.log}).
 *
 * <p>Contrat du socle : en mode soft, un {@code should*} en échec est <b>collecté</b> et l'exécution continue ; dans
 * une étape, il la rend <b>rouge</b> (sous-étape portant le message, capture, dump {@code {step}.html}) sans faire
 * sauter les suivantes ; en fin de test, l'extension {@code SoftAssertionExtension} (auto-détectée) <b>vote le KO</b>
 * en levant une {@code AssertionError} agrégée (un {@code suppressed} par échec, dans l'ordre) et purge le collecteur.
 *
 * <p>T82 : une même suite de 16 {@code should*} (13 KO, 3 OK, la dernière OK) est jouée deux fois :
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
class AssertionSoftTest {

    /** Clé du mode soft ({@code SoftAssertions.SOFT_KEY}, classe non publique du socle). */
    private static final String SOFT_KEY = "qa.assertions.soft";

    private static final String FIXTURE = "websync/assertion/soft.html";
    private static final By ERROR = By.id("erreur");
    private static final By CONFIRMATION = By.id("confirmation");
    private static final By PROMO_CODE = By.id("code-promo");
    private static final By CANCEL = By.id("annuler");
    private static final By PAY = By.id("payer");
    private static final By NEWSLETTER = By.id("newsletter");
    private static final By TERMS = By.id("cgv");
    private static final By DELIVERY = By.id("livraison");

    @Steps
    StateSteps state;

    @Steps
    SelectSteps select;

    // ---- T17 : plusieurs should* KO agreges, echec en fin par le vrai vote de JUnit (fixture form.html) ----
    // Rouge a dessein : la preuve d'agregation = les deux echecs figurent dans FAIL.log.
    @Test
    @DisplayName("T17 - NON PASS - plusieurs should* KO agreges, echec en fin (soft=true)")
    void T17_should_aggregated() {
        getDriver().get(Fixtures.url("form.html", null));
        WebSync web = new WebSync(SyncTimeout.SHORT);

        // En mode soft, ces deux assertions KO doivent etre collectees (pas d'arret a la 1re).
        web.shouldBeVisible(By.id("premier-absent"));   // KO 1 collecte
        web.shouldContainText(By.id("text"), "IMPOSSIBLE"); // KO 2 collecte

        // Marqueur : si on arrive ici, c'est que le mode soft n'a PAS stoppe a la 1re (attendu).
        System.out.println("[T17] RESULTAT : 2 should* soft collectes (soft=true)"
                + " => echec agrege attendu EN FIN de test (cas non passant volontaire)");
    }

    // ---- T82 : 16 should* (13 KO, 3 OK) en bloc puis en etapes ----
    @Test
    @DisplayName("T82 - NON PASS - soft : 16 should* (13 KO, 3 OK) en bloc puis en etapes, bloc = 13 echecs dans l'ordre, rouge a dessein")
    void T82_should_block_then_steps() {
        String soft = EnvironmentSpecificConfiguration.from(SystemEnvironmentVariables.createEnvironmentVariables())
                .getOptionalProperty(SOFT_KEY).orElse("absent");
        assertEquals("true", soft, "T82 : prealable, " + SOFT_KEY + " doit valoir true : lancer par l'execution"
                + " soft du pom (README, « Lancer les tests » §6)");

        state.withProfile(SyncTimeout.SHORT);
        select.withProfile(SyncTimeout.SHORT);
        state.open(FIXTURE);

        // ---- Le bloc : la suite en appels directs, hors etape ----
        WebSync web = new WebSync(SyncTimeout.SHORT);
        web.shouldBeVisible(ERROR);                            // 1  KO
        web.shouldBeCurrentlyVisible(ERROR);                   // 2  KO
        web.shouldNotBeVisible(CONFIRMATION);                  // 3  KO
        web.shouldBePresent(PROMO_CODE);                       // 4  KO
        web.shouldNotBePresent(CONFIRMATION);                  // 5  KO
        web.shouldBeEnabled(CANCEL);                           // 6  KO
        web.shouldNotBeEnabled(PAY);                           // 7  KO
        web.shouldBeVisible(CONFIRMATION);                     // 8  OK
        web.shouldBeSelected(NEWSLETTER);                      // 9  KO
        web.shouldNotBeSelected(TERMS);                        // 10 KO
        web.shouldContainText(CONFIRMATION, "annulée");        // 11 KO
        web.shouldContainOnlyText(CONFIRMATION, "Commande");   // 12 KO
        web.shouldNotContainText(CONFIRMATION, "validée");     // 13 KO
        web.shouldContainText(CONFIRMATION, "validée");        // 14 OK
        web.shouldContainSelectedOption(DELIVERY, "Express");  // 15 KO
        web.shouldBeEnabled(PAY);                              // 16 OK
        // Arriver ici prouve deja qu'aucun echec n'a stoppe le test ; le nombre d'echecs collectes (13) le confirme.

        // Vote de fin declenche par le test (le contexte n'est pas utilise) : un seul agregat, 13 echecs, dans l'ordre.
        // Capture explicite, pas d'assertThrows : sous Serenity, il effacerait en fin de test les etapes rouges (Javadoc).
        SoftAssertionExtension endVote = new SoftAssertionExtension();
        AssertionError aggregate = null;
        try {
            endVote.afterTestExecution(null);
        } catch (AssertionError thrown) {
            aggregate = thrown;
        }
        assertNotNull(aggregate, "T82 : le vote de fin doit lever le KO agrege");
        List<String> expected = List.of("By.id: erreur", "By.id: erreur", "By.id: confirmation", "By.id: code-promo",
                "By.id: confirmation", "By.id: annuler", "By.id: payer", "By.id: newsletter", "By.id: cgv",
                "« annulée » : By.id: confirmation", "« Commande » : By.id: confirmation",
                "« validée » : By.id: confirmation", "« Express » : By.id: livraison");
        Throwable[] failures = aggregate.getSuppressed();
        assertEquals(expected.size(), failures.length, "T82 : un suppressed par should* en echec, les 3 OK exclus");
        for (int i = 0; i < failures.length; i++) {
            assertTrue(failures[i] instanceof AssertionError && failures[i].getMessage().endsWith(expected.get(i)),
                    "T82 : echec n°" + (i + 1) + " attendu sur " + expected.get(i) + ", recu=" + failures[i].getMessage());
        }
        assertDoesNotThrow(() -> endVote.afterTestExecution(null), "T82 : le collecteur doit etre purge apres le vote");

        System.out.println("[T82] RESULTAT bloc : exception attendue=AssertionError agregee (vote de fin, soft=true)"
                + " | levee=OUI | type reel=" + aggregate.getClass().getName() + " | " + failures.length + " echecs dans"
                + " l'ordre, 3 OK non collectes | collecteur purge => bloc confirme");

        // ---- Les etapes : la meme suite, une etape par should* (a controler dans le rapport) ----
        state.shouldBeVisible(ERROR);                             // 1  KO
        state.shouldBeCurrentlyVisible(ERROR);                    // 2  KO
        state.shouldNotBeVisible(CONFIRMATION);                   // 3  KO
        state.shouldBePresent(PROMO_CODE);                        // 4  KO
        state.shouldNotBePresent(CONFIRMATION);                   // 5  KO
        state.shouldBeEnabled(CANCEL);                            // 6  KO
        state.shouldNotBeEnabled(PAY);                            // 7  KO
        state.shouldBeVisible(CONFIRMATION);                      // 8  OK
        state.shouldBeSelected(NEWSLETTER);                       // 9  KO
        state.shouldNotBeSelected(TERMS);                         // 10 KO
        state.shouldContainText(CONFIRMATION, "annulée");         // 11 KO
        state.shouldContainOnlyText(CONFIRMATION, "Commande");    // 12 KO
        state.shouldNotContainText(CONFIRMATION, "validée");      // 13 KO
        state.shouldContainText(CONFIRMATION, "validée");         // 14 OK
        select.shouldContainSelectedOption(DELIVERY, "Express");  // 15 KO
        state.shouldBeEnabled(PAY);                               // 16 OK

        // ---- La fin : le vote de JUnit leve les 13 echecs des etapes, le test finit rouge (a dessein) ----
        System.out.println("[T82] RESULTAT etapes : 16 etapes jouees (13 KO, 3 OK) => a controler dans le rapport ;"
                + " echec de fin attendu : AssertionError agregee de 13 echecs (cas non passant volontaire)");
    }
}
