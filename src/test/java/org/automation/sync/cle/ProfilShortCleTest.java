package org.automation.sync.cle;

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
 * Groupe {@code cle} — profil SHORT surchargé par {@code qa.sync.timeout.short} (exécution « cle » du
 * pom, cf. {@link BornesProfil} pour la règle ± 5 s). Le non-passant chronométré prouve que la borne
 * appliquée est la clé et non le défaut (5 s).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ProfilShortCleTest {

    @Test
    @DisplayName("L5 - profil SHORT (cle) borne basse : cible a timeout-5s => clic abouti")
    void L5_short_borne_basse() {
        Duration timeout = Timeouts.configured(SyncTimeout.SHORT);
        long cible = BornesProfil.ouvrirCibleA(timeout.minus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.SHORT);
        long start = System.nanoTime();
        web.click(BornesProfil.CIBLE);
        long ms = BornesProfil.depuis(start);

        assertTrue(ms >= cible - BornesProfil.TOLERANCE_MS,
                "le socle doit avoir attendu la cible (~" + cible + " ms), mesure=" + ms + " ms");
        assertTrue(ms < timeout.toMillis(),
                "clic attendu avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(web.containsText(BornesProfil.STATUT, "clicked"), "clic effectif attendu");
        System.out.println("[L5-SHORT-basse] RESULTAT : WebSync.click abouti en " + ms + " ms | cible a " + cible
                + " ms, timeout SHORT=" + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("L5 - profil SHORT (cle) borne haute : cible a timeout+5s => SyncException [ABSENT] au timeout")
    void L5_short_borne_haute() {
        Duration timeout = Timeouts.configured(SyncTimeout.SHORT);
        long cible = BornesProfil.ouvrirCibleA(timeout.plus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.SHORT);
        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.click(BornesProfil.CIBLE),
                "SyncException attendue au timeout (" + timeout.toMillis() + " ms)");
        long ms = BornesProfil.depuis(start);

        assertTrue(String.valueOf(ex.getMessage()).contains("[ABSENT]"), "code [ABSENT] attendu, message=" + ex.getMessage());
        assertTrue(ms >= timeout.toMillis(), "levee avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(ms < cible, "levee apres l'arrivee de la cible (" + cible + " ms), mesure=" + ms + " ms");
        System.out.println("[L5-SHORT] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | apres " + ms + " ms (timeout SHORT=" + timeout.toMillis()
                + " ms, cible a " + cible + " ms) | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }
}
