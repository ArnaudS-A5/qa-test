package org.automation.sync.defaut;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.BornesProfil;
import org.automation.support.Timeouts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code defaut} — profil MEDIUM aux vraies valeurs du socle (15 s, aucune clé ; exécution
 * « defaut » du pom), règle ± 5 s ({@link BornesProfil}).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ProfilMediumDefautTest {

    @Test
    @DisplayName("L5 - profil MEDIUM aux vraies valeurs (defaut 15s) : cible a timeout-5s => clic abouti")
    void L5_medium_vraies_valeurs() {
        Duration timeout = Timeouts.defaultOf(SyncTimeout.MEDIUM);
        long cible = BornesProfil.ouvrirCibleA(timeout.minus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);
        long start = System.nanoTime();
        web.click(BornesProfil.CIBLE);
        long ms = BornesProfil.depuis(start);

        assertTrue(ms >= cible - BornesProfil.TOLERANCE_MS,
                "le socle doit avoir attendu la cible (~" + cible + " ms), mesure=" + ms + " ms");
        assertTrue(ms < timeout.toMillis(),
                "clic attendu avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(web.containsText(BornesProfil.STATUT, "clicked"), "clic effectif attendu");
        System.out.println("[L5-MEDIUM] RESULTAT : WebSync.click abouti en " + ms + " ms | cible a " + cible
                + " ms, timeout MEDIUM=" + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("L5 - profil MEDIUM (defaut 15s) borne haute : cible a timeout+5s => SyncException [ABSENT] au timeout")
    void L5_medium_defaut_borne_haute() {
        Duration timeout = Timeouts.defaultOf(SyncTimeout.MEDIUM);
        long cible = BornesProfil.ouvrirCibleA(timeout.plus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);
        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.click(BornesProfil.CIBLE),
                "SyncException attendue au timeout (" + timeout.toMillis() + " ms)");
        long ms = BornesProfil.depuis(start);

        assertTrue(String.valueOf(ex.getMessage()).contains("[ABSENT]"), "code [ABSENT] attendu, message=" + ex.getMessage());
        assertTrue(ms >= timeout.toMillis(), "levee avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(ms < cible, "levee apres l'arrivee de la cible (" + cible + " ms), mesure=" + ms + " ms");
        System.out.println("[L5-MEDIUM-defaut-haute] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | apres " + ms + " ms (timeout MEDIUM=" + timeout.toMillis()
                + " ms, cible a " + cible + " ms) | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }
}
