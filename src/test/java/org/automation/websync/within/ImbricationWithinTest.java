package org.automation.websync.within;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.LectureSteps;
import org.automation.steps.websync.WithinSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — vues {@code within} <b>imbriquées sur 5 niveaux</b> (fixture
 * {@code websync/imbrication.html}). Complète T11 (un seul niveau).
 *
 * <p>La fixture contient deux branches de même structure et, dans la branche B, un <b>leurre</b> placé avant
 * le vrai chemin : seule une chaîne qui respecte chacun des 5 conteneurs atteint « cible B ». Chaque niveau est
 * ré-résolu à chaque appel (aucun handle conservé).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ImbricationWithinTest {

    private static final String FIXTURE = "websync/imbrication.html";
    private static final By BRANCHE_B = By.id("branche-b");
    private static final By NIVEAU_2 = By.className("niveau-2");
    private static final By NIVEAU_3 = By.className("niveau-3");
    private static final By NIVEAU_4 = By.className("niveau-4");
    private static final By NIVEAU_5 = By.className("niveau-5");
    private static final By NIVEAU_3_ABSENT = By.className("niveau-3-absent");
    private static final By CIBLE = By.className("cible");
    private static final By ACTION = By.className("action");
    private static final By STATUT = By.id("statut");

    /** Les 5 conteneurs du vrai chemin, du plus externe au plus interne. */
    private static final List<By> CHEMIN_B = List.of(BRANCHE_B, NIVEAU_2, NIVEAU_3, NIVEAU_4, NIVEAU_5);

    @Steps
    WithinSteps within;

    @Steps
    LectureSteps lecture;

    // ---- T79 : lecture de l'element le plus encapsule, a travers 5 within ----
    @Test
    @DisplayName("T79 - within x5 : lit 'cible B' au 5e niveau, la ou la racine et une chaine courte voient d'autres cibles")
    void T79_within_cinq_niveaux_lecture() {
        within.ouvrir(FIXTURE);

        String cible = within.texteDans(CHEMIN_B, CIBLE);
        assertEquals("cible B", cible, "T79 : la chaine des 5 within doit atteindre la cible de la branche B");
        assertEquals(1, within.compterDans(CHEMIN_B, CIBLE), "T79 : une seule cible dans le 5e niveau");
        List<String> racine = lecture.textes(CIBLE);
        assertEquals(List.of("cible A", "leurre B", "cible B"), racine, "T79 : a la racine, les trois cibles de la page");
        String chaineCourte = within.texteDans(List.of(BRANCHE_B, NIVEAU_2), CIBLE);
        assertEquals("leurre B", chaineCourte, "T79 : sans le niveau 3, la premiere cible trouvee est le leurre");

        System.out.println("[T79] RESULTAT : 5 within => \"" + cible + "\" | racine=" + racine + " | 2 within => \""
                + chaineCourte + "\" | aucune exception => cas passant confirme");
    }

    // ---- T80 : action sur l'element le plus encapsule, a travers 5 within ----
    @Test
    @DisplayName("T80 - within x5 : le clic atteint le bouton du 5e niveau de la branche B, pas le leurre")
    void T80_within_cinq_niveaux_action() {
        within.ouvrir(FIXTURE);

        within.cliquerDans(CHEMIN_B, ACTION);
        String statut = lecture.texte(STATUT);
        assertEquals("B", statut, "T80 : le clic doit declencher le bouton de la cible B (ni A, ni le leurre)");

        System.out.println("[T80] RESULTAT : clic a travers 5 within => statut=" + statut
                + " | aucune exception => cas passant confirme");
    }

    // ---- T81 : un maillon absent au milieu de la chaine ----
    @Test
    @DisplayName("T81 - within x5 avec le niveau 3 absent : SyncException [ABSENT], la cause designe le maillon manquant")
    void T81_within_maillon_absent() {
        within.ouvrir(FIXTURE);
        WebSync chaineRompue = new WebSync(SyncTimeout.SHORT)
                .within(BRANCHE_B).within(NIVEAU_2).within(NIVEAU_3_ABSENT).within(NIVEAU_4).within(NIVEAU_5);

        SyncException ex = assertThrows(SyncException.class, () -> chaineRompue.click(ACTION),
                "T81 : un maillon absent doit faire echouer l'action au timeout");
        assertTrue(ex.getMessage().contains("[ABSENT]"), "T81 : code [ABSENT] attendu, message=" + ex.getMessage());
        assertTrue(ex.getCause() instanceof NoSuchElementException, "T81 : cause NoSuchElementException attendue, recue=" + ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("niveau-3-absent"),
                "T81 : la cause doit designer le maillon manquant (niveau-3-absent), cause=" + ex.getCause().getMessage());
        assertEquals("aucune", lecture.texte(STATUT), "T81 : aucun bouton ne doit avoir ete clique");

        System.out.println("[T81] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI | type reel="
                + ex.getClass().getName() + " | cause designe niveau-3-absent | message=\"" + ex.getMessage()
                + "\" => cas non passant confirme");
    }
}
