package org.automation.websync.select;

import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.SelectSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — listes déroulantes HTML {@code <select>} (fixture {@code websync/select.html}) :
 * sélection, désélection et lectures, sur une liste simple et une liste multiple.
 *
 * <p>Les appels dont on attend l'échec restent hors étape ({@code assertThrows} sur un {@code WebSync} direct).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ListeDeroulanteTest {

    private static final String FIXTURE = "websync/select.html";
    private static final By SIMPLE = By.id("simple");
    private static final By MULTIPLE = By.id("multiple");

    /** Une remontée immédiate se mesure en millisecondes ; le profil MEDIUM, lui, aurait attendu 15 s. */
    private static final long REMONTEE_IMMEDIATE_MS = 3_000;

    @Steps
    SelectSteps select;

    // ---- T33 : selection sur liste simple, par libelle puis par index ----
    @Test
    @DisplayName("T33 - liste simple : selectByVisibleText puis selectByIndex, option lue par valeur et libelle")
    void T33_liste_simple_selection() {
        select.ouvrir(FIXTURE);

        select.choisirParLibelle(SIMPLE, "Option B");
        assertEquals("b", select.premiereValeurChoisie(SIMPLE), "T33 : selectByVisibleText('Option B') => valeur b");

        select.choisirParIndex(SIMPLE, 2);
        assertEquals("c", select.premiereValeurChoisie(SIMPLE), "T33 : selectByIndex(2) => valeur c");
        assertEquals("Option C", select.premierLibelleChoisi(SIMPLE), "T33 : selectByIndex(2) => libelle Option C");

        System.out.println("[T33] RESULTAT : 'Option B' -> b | index 2 -> c / Option C"
                + " | aucune exception => cas passant confirme");
    }

    // ---- T34 : lectures de la liste simple et assertion shouldContainSelectedOption ----
    @Test
    @DisplayName("T34 - liste simple : options, valeurs, containsSelectOption et shouldContainSelectedOption")
    void T34_liste_simple_lectures() {
        select.ouvrir(FIXTURE);

        assertEquals(List.of("Option A", "Option B", "Option C"), select.libellesOptions(SIMPLE), "T34 : libelles des options");
        assertEquals(List.of("a", "b", "c"), select.valeursOptions(SIMPLE), "T34 : valeurs des options");
        assertTrue(select.contientOption(SIMPLE, "Option B"), "T34 : containsSelectOption vrai sur une option existante");
        assertFalse(select.contientOption(SIMPLE, "Option Z"), "T34 : containsSelectOption faux sur une option inexistante");
        select.verifierOptionProposee(SIMPLE, "Option A");

        WebSync web = new WebSync(SyncTimeout.SHORT);
        AssertionError err = assertThrows(AssertionError.class, () -> web.shouldContainSelectedOption(SIMPLE, "Option Z"),
                "T34 : shouldContainSelectedOption doit echouer sur une option inexistante (soft=false)");
        assertTrue(err.getMessage().contains("Option Z"), "T34 : le message doit citer l'option, message=" + err.getMessage());

        System.out.println("[T34] RESULTAT : options [Option A, Option B, Option C] / valeurs [a, b, c]"
                + " | containsSelectOption B=vrai, Z=faux | shouldContainSelectedOption('Option A') passe"
                + " | aucune exception => cas passant confirme");
        System.out.println("[T34] RESULTAT : exception attendue=AssertionError (shouldContainSelectedOption 'Option Z')"
                + " | levee=OUI | type reel=" + err.getClass().getName() + " | message=\"" + err.getMessage()
                + "\" => cas non passant confirme");
    }

    // ---- T35 : selection et deselection sur liste multiple ----
    @Test
    @DisplayName("T35 - liste multiple : trois selections, trois deselections, puis deselectAll")
    void T35_liste_multiple_selection_deselection() {
        select.ouvrir(FIXTURE);

        select.choisirParLibelle(MULTIPLE, "Rouge");
        select.choisirParValeur(MULTIPLE, "v");
        select.choisirParIndex(MULTIPLE, 2);
        assertEquals(List.of("Rouge", "Vert", "Bleu"), select.libellesChoisis(MULTIPLE), "T35 : trois options selectionnees (libelles)");
        assertEquals(List.of("r", "v", "b"), select.valeursChoisies(MULTIPLE), "T35 : trois options selectionnees (valeurs)");

        select.deselectionnerParLibelle(MULTIPLE, "Rouge");
        assertEquals(List.of("v", "b"), select.valeursChoisies(MULTIPLE), "T35 : deselectByVisibleText('Rouge')");
        select.deselectionnerParValeur(MULTIPLE, "v");
        assertEquals(List.of("b"), select.valeursChoisies(MULTIPLE), "T35 : deselectByValue('v')");
        select.deselectionnerParIndex(MULTIPLE, 2);
        assertEquals(List.of(), select.valeursChoisies(MULTIPLE), "T35 : deselectByIndex(2)");

        select.choisirParIndex(MULTIPLE, 0);
        select.choisirParIndex(MULTIPLE, 3);
        assertEquals(List.of("r", "j"), select.valeursChoisies(MULTIPLE), "T35 : deux options reselectionnees avant deselectAll");
        select.toutDeselectionner(MULTIPLE);
        assertEquals(List.of(), select.valeursChoisies(MULTIPLE), "T35 : deselectAll doit tout deselectionner");

        System.out.println("[T35] RESULTAT : [r, v, b] -> [v, b] -> [b] -> [] | [r, j] -> deselectAll -> []"
                + " | aucune exception => cas passant confirme");
    }

    // ---- T36 : deselection refusee sur liste simple, immediatement ----
    @Test
    @DisplayName("T36 - deselectByVisibleText sur liste simple : refus immediat, selection inchangee")
    void T36_deselection_liste_simple_refusee() {
        select.ouvrir(FIXTURE);
        select.choisirParValeur(SIMPLE, "b");
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> web.deselectByVisibleText(SIMPLE, "Option B"),
                "T36 : une liste simple ne se deselectionne pas");
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms < REMONTEE_IMMEDIATE_MS,
                "T36 : erreur non transitoire attendue immediatement (sans attendre le profil MEDIUM), ms=" + ms);
        assertEquals("b", select.premiereValeurChoisie(SIMPLE), "T36 : la selection doit rester inchangee");

        System.out.println("[T36] RESULTAT : exception attendue=UnsupportedOperationException | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | en " + ms + " ms | message=\"" + ex.getMessage()
                + "\" | selection inchangee => cas non passant confirme");
    }
}
