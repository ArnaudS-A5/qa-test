package org.automation.config.configured;

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
 * Groupe {@code configured} — profil EXTRA_LONG surchargé par {@code qa.sync.timeout.extraLong} (exécution
 * « configured » du pom, cf. {@link ProfileBounds} pour la règle ± 5 s). Clé sous le défaut (120 s) : le
 * non-passant prouve la lecture de la clé.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ProfileExtraLongConfiguredTest {

    @Test
    @DisplayName("L5 - PASS - profil EXTRA_LONG (cle) borne basse : cible a timeout-5s => clic abouti")
    void L5_extra_long_low() {
        Duration timeout = Timeouts.configured(SyncTimeout.EXTRA_LONG);
        long targetAt = ProfileBounds.openTargetAt(timeout.minus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.EXTRA_LONG);
        long start = System.nanoTime();
        web.click(ProfileBounds.TARGET);
        long ms = ProfileBounds.since(start);

        assertTrue(ms >= targetAt - ProfileBounds.TOLERANCE_MS,
                "le socle doit avoir attendu la cible (~" + targetAt + " ms), mesure=" + ms + " ms");
        assertTrue(ms < timeout.toMillis(),
                "clic attendu avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(web.containsText(ProfileBounds.STATUS, "clicked"), "clic effectif attendu");
        System.out.println("[L5-EXTRA_LONG-basse] RESULTAT : WebSync.click abouti en " + ms + " ms | cible a " + targetAt
                + " ms, timeout EXTRA_LONG=" + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("L5 - NON PASS - profil EXTRA_LONG (cle) borne haute : cible a timeout+5s => SyncException [ABSENT] au timeout")
    void L5_extra_long_high() {
        Duration timeout = Timeouts.configured(SyncTimeout.EXTRA_LONG);
        long targetAt = ProfileBounds.openTargetAt(timeout.plus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.EXTRA_LONG);
        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.click(ProfileBounds.TARGET),
                "SyncException attendue au timeout (" + timeout.toMillis() + " ms)");
        long ms = ProfileBounds.since(start);

        assertTrue(String.valueOf(ex.getMessage()).contains("[ABSENT]"), "code [ABSENT] attendu, message=" + ex.getMessage());
        assertTrue(ms >= timeout.toMillis(), "levee avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(ms < targetAt, "levee apres l'arrivee de la cible (" + targetAt + " ms), mesure=" + ms + " ms");
        System.out.println("[L5-EXTRA_LONG-haute] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | apres " + ms + " ms (timeout EXTRA_LONG=" + timeout.toMillis()
                + " ms, cible a " + targetAt + " ms) | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }
}
