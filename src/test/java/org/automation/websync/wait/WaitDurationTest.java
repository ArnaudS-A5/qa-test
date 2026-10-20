package org.automation.websync.wait;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Timeouts;
import org.automation.support.Transitions;
import org.automation.support.Transitions.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static org.automation.support.ProfileBounds.TOLERANCE_MS;
import static org.automation.support.ProfileBounds.since;
import static org.automation.support.Transitions.TARGET;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — attentes à <b>durée explicite</b> {@code waitForElement(By, Duration)} et
 * {@code waitForElementToDisappear(By, Duration)}, règle aux limites <b>± 5 s</b> autour de la durée passée
 * (8 s), sur la fixture {@code transitions.html}.
 *
 * <p>L'instance est au profil MEDIUM (15 s par défaut), <b>plus long</b> que la durée + 5 s : une exception
 * entre 8 et 13 s prouve que c'est la <b>durée passée</b> qui borne l'attente, et non le profil.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class WaitDurationTest {

    /** Durée passée en paramètre : volontairement différente de tout profil du socle. */
    private static final Duration DURATION = Duration.ofSeconds(8);

    /** Profil de l'instance, au défaut du socle ; doit dépasser DURATION + MARGIN pour que le test discrimine. */
    private static Duration profile() {
        return Timeouts.defaultOf(SyncTimeout.MEDIUM);
    }

    // ---- T67 / T68 : waitForElement(By, Duration) ----
    @Test
    @DisplayName("T67 - PASS - waitForElement(8s), apparition a 3s : aboutit avant 8s, element visible")
    void T67_waitForElement() {
        long targetAt = Transitions.open(Scenario.APPEAR, DURATION.minus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitForElement(TARGET, DURATION);
        long ms = since(start);
        assertTrue(web.isVisible(TARGET), "T67 : #cible doit etre visible apres l'attente");
        assertTrue(ms >= targetAt - TOLERANCE_MS, "T67 : l'attente doit avoir dure jusqu'a l'apparition (" + targetAt + " ms), ms=" + ms);
        assertTrue(ms < DURATION.toMillis(), "T67 : l'attente doit aboutir avant la duree passee (" + DURATION.toMillis() + " ms), ms=" + ms);

        System.out.println("[T67] RESULTAT : visible en " + ms + " ms | apparition a " + targetAt + " ms | duree "
                + DURATION.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T68 - NON PASS - waitForElement(8s), apparition a 13s : SyncException entre 8 et 13s, avant le profil MEDIUM")
    void T68_waitForElement() {
        Duration profileTimeout = profile();
        long targetAt = Transitions.open(Scenario.APPEAR, DURATION.plus(Timeouts.MARGIN));
        assertTrue(profileTimeout.toMillis() > targetAt, "T68 : prealable, le profil (" + profileTimeout.toMillis()
                + " ms) doit depasser l'apparition (" + targetAt + " ms) pour distinguer duree et profil");
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitForElement(TARGET, DURATION),
                "T68 : l'attente doit expirer a la duree passee");
        long ms = since(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T68 : le message doit citer le locator, message=" + ex.getMessage());
        assertFalse(web.isPresent(TARGET), "T68 : #cible encore absent a l'expiration");
        assertTrue(ms >= DURATION.toMillis(), "T68 : l'exception ne doit pas preceder la duree passee, ms=" + ms);
        assertTrue(ms < targetAt, "T68 : l'exception doit preceder l'apparition (" + targetAt + " ms), donc le profil, ms=" + ms);

        System.out.println("[T68] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (duree " + DURATION.toMillis() + ", apparition " + targetAt + ", profil " + profileTimeout.toMillis()
                + ") | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T69 / T70 : waitForElementToDisappear(By, Duration) ----
    @Test
    @DisplayName("T69 - PASS - waitForElementToDisappear(8s), disparition a 3s : aboutit avant 8s, element retire")
    void T69_waitForElementToDisappear() {
        long targetAt = Transitions.open(Scenario.DISAPPEAR, DURATION.minus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitForElementToDisappear(TARGET, DURATION);
        long ms = since(start);
        assertFalse(web.isPresent(TARGET), "T69 : #cible doit avoir ete retire apres l'attente");
        assertTrue(ms >= targetAt - TOLERANCE_MS, "T69 : l'attente doit avoir dure jusqu'a la disparition (" + targetAt + " ms), ms=" + ms);
        assertTrue(ms < DURATION.toMillis(), "T69 : l'attente doit aboutir avant la duree passee (" + DURATION.toMillis() + " ms), ms=" + ms);

        System.out.println("[T69] RESULTAT : disparu en " + ms + " ms | disparition a " + targetAt + " ms | duree "
                + DURATION.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T70 - NON PASS - waitForElementToDisappear(8s), disparition a 13s : SyncException entre 8 et 13s, avant le profil MEDIUM")
    void T70_waitForElementToDisappear() {
        Duration profileTimeout = profile();
        long targetAt = Transitions.open(Scenario.DISAPPEAR, DURATION.plus(Timeouts.MARGIN));
        assertTrue(profileTimeout.toMillis() > targetAt, "T70 : prealable, le profil (" + profileTimeout.toMillis()
                + " ms) doit depasser la disparition (" + targetAt + " ms) pour distinguer duree et profil");
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitForElementToDisappear(TARGET, DURATION),
                "T70 : l'attente doit expirer a la duree passee");
        long ms = since(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T70 : le message doit citer le locator, message=" + ex.getMessage());
        assertTrue(web.isVisible(TARGET), "T70 : #cible encore visible a l'expiration");
        assertTrue(ms >= DURATION.toMillis(), "T70 : l'exception ne doit pas preceder la duree passee, ms=" + ms);
        assertTrue(ms < targetAt, "T70 : l'exception doit preceder la disparition (" + targetAt + " ms), donc le profil, ms=" + ms);

        System.out.println("[T70] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (duree " + DURATION.toMillis() + ", disparition " + targetAt + ", profil " + profileTimeout.toMillis()
                + ") | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }
}
