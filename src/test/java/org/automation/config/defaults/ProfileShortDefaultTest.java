package org.automation.config.defaults;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.ProfileBounds;
import org.automation.support.Timeouts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code defaults} — profil SHORT au défaut du socle (5 s, aucune clé ; exécution « defaults » du
 * pom). Marge réduite à 2 s ({@link Timeouts#SHORT_MARGIN}) : avec 5 s, la cible du cas passant serait
 * présente dès le départ et le test ne serait plus aux limites.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ProfileShortDefaultTest {

    @Test
    @DisplayName("L5 - PASS - profil SHORT (defaut 5s) borne basse : cible a timeout-2s => clic abouti")
    void L5_short_low() {
        Duration timeout = Timeouts.defaultOf(SyncTimeout.SHORT);
        long targetAt = ProfileBounds.openTargetAt(timeout.minus(Timeouts.SHORT_MARGIN));
        WebSync web = new WebSync(SyncTimeout.SHORT);
        long start = System.nanoTime();
        web.click(ProfileBounds.TARGET);
        long ms = ProfileBounds.since(start);

        assertTrue(ms >= targetAt - ProfileBounds.TOLERANCE_MS,
                "le socle doit avoir attendu la cible (~" + targetAt + " ms), mesure=" + ms + " ms");
        assertTrue(ms < timeout.toMillis(),
                "clic attendu avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(web.containsText(ProfileBounds.STATUS, "clicked"), "clic effectif attendu");
        System.out.println("[L5-SHORT-defaut-basse] RESULTAT : WebSync.click abouti en " + ms + " ms | cible a "
                + targetAt + " ms, timeout SHORT=" + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("L5 - NON PASS - profil SHORT (defaut 5s) borne haute : cible a timeout+2s => SyncException [ABSENT] au timeout")
    void L5_short_high() {
        Duration timeout = Timeouts.defaultOf(SyncTimeout.SHORT);
        long targetAt = ProfileBounds.openTargetAt(timeout.plus(Timeouts.SHORT_MARGIN));
        WebSync web = new WebSync(SyncTimeout.SHORT);
        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.click(ProfileBounds.TARGET),
                "SyncException attendue au timeout (" + timeout.toMillis() + " ms)");
        long ms = ProfileBounds.since(start);

        assertTrue(String.valueOf(ex.getMessage()).contains("[ABSENT]"), "code [ABSENT] attendu, message=" + ex.getMessage());
        assertTrue(ms >= timeout.toMillis(), "levee avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(ms < targetAt, "levee apres l'arrivee de la cible (" + targetAt + " ms), mesure=" + ms + " ms");
        System.out.println("[L5-SHORT-defaut-haute] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | apres " + ms + " ms (timeout SHORT=" + timeout.toMillis()
                + " ms, cible a " + targetAt + " ms) | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }
}
