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
 * Groupe {@code configured} — profil MEDIUM surchargé par {@code qa.sync.timeout.medium} (exécution « configured » du
 * pom, cf. {@link ProfileBounds} pour la règle ± 5 s). Clé au-dessus du défaut (15 s) : le passant aboutit
 * au-delà du défaut, le non-passant chronométré borne à la clé.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ProfileMediumConfiguredTest {

    @Test
    @DisplayName("L3 - PASS - profil MEDIUM (cle) borne basse : cible a timeout-5s => clic abouti")
    void L3_medium_low() {
        Duration timeout = Timeouts.configured(SyncTimeout.MEDIUM);
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
        System.out.println("[L3] RESULTAT : WebSync.click abouti en " + ms + " ms | cible a " + targetAt
                + " ms, timeout MEDIUM=" + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("L4 - NON PASS - profil MEDIUM (cle) borne haute : cible a timeout+5s => SyncException [ABSENT] au timeout")
    void L4_medium_high() {
        Duration timeout = Timeouts.configured(SyncTimeout.MEDIUM);
        long targetAt = ProfileBounds.openTargetAt(timeout.plus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);
        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.click(ProfileBounds.TARGET),
                "SyncException attendue au timeout (" + timeout.toMillis() + " ms)");
        long ms = ProfileBounds.since(start);

        assertTrue(String.valueOf(ex.getMessage()).contains("[ABSENT]"), "code [ABSENT] attendu, message=" + ex.getMessage());
        assertTrue(ms >= timeout.toMillis(), "levee avant le timeout (" + timeout.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(ms < targetAt, "levee apres l'arrivee de la cible (" + targetAt + " ms), mesure=" + ms + " ms");
        System.out.println("[L4] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | apres " + ms + " ms (timeout MEDIUM=" + timeout.toMillis()
                + " ms, cible a " + targetAt + " ms) | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }
}
