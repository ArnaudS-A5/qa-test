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
 * Groupe {@code defaults} — profil MEDIUM aux vraies valeurs du socle (15 s, aucune clé ; exécution
 * « defaults » du pom), règle ± 5 s ({@link ProfileBounds}).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ProfileMediumDefaultTest {

    @Test
    @DisplayName("L5 - PASS - profil MEDIUM aux vraies valeurs (defaut 15s) : cible a timeout-5s => clic abouti")
    void L5_medium_low() {
        Duration timeout = Timeouts.defaultOf(SyncTimeout.MEDIUM);
        long targetAt = ProfileBounds.openTargetAt(timeout.minus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);
        long start = System.nanoTime();
        web.click(ProfileBounds.TARGET);
        long ms = ProfileBounds.since(start);

        assertTrue(ms >= targetAt - ProfileBounds.TOLERANCE_MS,
                "le socle doit avoir attendu la cible (~" + targetAt + " ms), mesure=" + ms + " ms");
        assertTrue(ms < timeout.toMillis(),
                "clic attendu avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(web.containsText(ProfileBounds.STATUS, "clicked"), "clic effectif attendu");
        System.out.println("[L5-MEDIUM] RESULTAT : WebSync.click abouti en " + ms + " ms | cible a " + targetAt
                + " ms, timeout MEDIUM=" + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("L5 - NON PASS - profil MEDIUM (defaut 15s) borne haute : cible a timeout+5s => SyncException [ABSENT] au timeout")
    void L5_medium_high() {
        Duration timeout = Timeouts.defaultOf(SyncTimeout.MEDIUM);
        long targetAt = ProfileBounds.openTargetAt(timeout.plus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);
        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.click(ProfileBounds.TARGET),
                "SyncException attendue au timeout (" + timeout.toMillis() + " ms)");
        long ms = ProfileBounds.since(start);

        assertTrue(String.valueOf(ex.getMessage()).contains("[ABSENT]"), "code [ABSENT] attendu, message=" + ex.getMessage());
        assertTrue(ms >= timeout.toMillis(), "levee avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(ms < targetAt, "levee apres l'arrivee de la cible (" + targetAt + " ms), mesure=" + ms + " ms");
        System.out.println("[L5-MEDIUM-defaut-haute] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | apres " + ms + " ms (timeout MEDIUM=" + timeout.toMillis()
                + " ms, cible a " + targetAt + " ms) | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }
}
