package org.automation.websync.wait;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.automation.support.Timeouts;
import org.automation.support.Transitions;
import org.automation.support.Transitions.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.automation.support.ProfileBounds.TOLERANCE_MS;
import static org.automation.support.ProfileBounds.since;
import static org.automation.support.Transitions.TARGET;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — <b>attentes d'état</b> {@code waitUntilPresent/Enabled/Clickable/Disabled}, selon
 * la règle aux limites <b>± 5 s</b> autour du profil MEDIUM au défaut du socle (15 s), sur la fixture
 * {@code transitions.html}. T15 couvre {@code waitUntilVisible} puis {@code waitUntilNotVisible} (fixtures
 * {@code appear.html} et {@code websync/wait/disappear.html}).
 *
 * <p>Cas <b>passant</b> : la transition survient à timeout − 5 s, l'attente aboutit après l'avoir
 * réellement attendue et avant le timeout. Cas <b>non passant</b> : la transition survient à timeout + 5 s,
 * {@code SyncException} levée <b>entre</b> le timeout et la transition, l'élément encore dans l'état initial.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class WaitStateTest {

    private static Duration timeout() {
        return Timeouts.defaultOf(SyncTimeout.MEDIUM);
    }

    // ---- T59 / T60 : waitUntilPresent ----
    // ---- T15 : waitUntilVisible puis waitUntilNotVisible (fixtures appear.html et websync/wait/disappear.html) ----
    @Test
    @DisplayName("T15 - PASS - waitUntilVisible (positif) puis waitUntilNotVisible (negatif) honores")
    void T15_waitUntilVisible() {
        By target = By.id("target");
        // Positif : appear a 1200ms, on attend la visibilite.
        getDriver().get(Fixtures.url("appear.html", "delay=1200"));
        WebSync web = new WebSync(SyncTimeout.LONG);
        web.waitUntilVisible(target);
        assertTrue(web.isVisible(target), "T15 : element visible apres waitUntilVisible");

        // Negatif : disappear a 1000ms, on attend la disparition.
        getDriver().get(Fixtures.url("websync/wait/disappear.html", "delay=1000"));
        web.waitUntilNotVisible(target);
        assertFalse(web.isVisible(target), "T15 : element non visible apres waitUntilNotVisible");
    }

    @Test
    @DisplayName("T59 - PASS - waitUntilPresent, insertion invisible a T-5s : aboutit avant T, element present mais pas visible")
    void T59_waitUntilPresent() {
        Duration timeout = timeout();
        long targetAt = Transitions.open(Scenario.APPEAR_HIDDEN, timeout.minus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitUntilPresent(TARGET);
        long ms = since(start);
        assertTrue(web.isPresent(TARGET), "T59 : #cible doit etre present apres l'attente");
        assertFalse(web.isCurrentlyVisible(TARGET), "T59 : present ne veut pas dire visible (#cible en display:none)");
        assertTrue(ms >= targetAt - TOLERANCE_MS, "T59 : l'attente doit avoir dure jusqu'a l'insertion (" + targetAt + " ms), ms=" + ms);
        assertTrue(ms < timeout.toMillis(), "T59 : l'attente doit aboutir avant le timeout (" + timeout.toMillis() + " ms), ms=" + ms);

        System.out.println("[T59] RESULTAT : present (invisible) en " + ms + " ms | insertion a " + targetAt
                + " ms | timeout " + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T60 - NON PASS - waitUntilPresent, insertion a T+5s : SyncException entre T et l'insertion")
    void T60_waitUntilPresent() {
        Duration timeout = timeout();
        long targetAt = Transitions.open(Scenario.APPEAR_HIDDEN, timeout.plus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitUntilPresent(TARGET),
                "T60 : l'attente doit expirer avant l'insertion");
        long ms = since(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T60 : le message doit citer le locator, message=" + ex.getMessage());
        assertFalse(web.isPresent(TARGET), "T60 : #cible encore absent a l'expiration");
        assertTrue(ms >= timeout.toMillis(), "T60 : l'exception ne doit pas preceder le timeout, ms=" + ms);
        assertTrue(ms < targetAt, "T60 : l'exception doit preceder l'insertion (" + targetAt + " ms), ms=" + ms);

        System.out.println("[T60] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (timeout " + timeout.toMillis() + ", insertion " + targetAt + ") | message=\""
                + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T61 / T62 : waitUntilEnabled ----
    @Test
    @DisplayName("T61 - PASS - waitUntilEnabled, activation a T-5s : aboutit avant T, element actif")
    void T61_waitUntilEnabled() {
        Duration timeout = timeout();
        long targetAt = Transitions.open(Scenario.ENABLE, timeout.minus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitUntilEnabled(TARGET);
        long ms = since(start);
        assertTrue(web.isEnabled(TARGET), "T61 : #cible doit etre actif apres l'attente");
        assertTrue(ms >= targetAt - TOLERANCE_MS, "T61 : l'attente doit avoir dure jusqu'a l'activation (" + targetAt + " ms), ms=" + ms);
        assertTrue(ms < timeout.toMillis(), "T61 : l'attente doit aboutir avant le timeout (" + timeout.toMillis() + " ms), ms=" + ms);

        System.out.println("[T61] RESULTAT : actif en " + ms + " ms | activation a " + targetAt + " ms | timeout "
                + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T62 - NON PASS - waitUntilEnabled, activation a T+5s : SyncException entre T et l'activation")
    void T62_waitUntilEnabled() {
        Duration timeout = timeout();
        long targetAt = Transitions.open(Scenario.ENABLE, timeout.plus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitUntilEnabled(TARGET),
                "T62 : l'attente doit expirer avant l'activation");
        long ms = since(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T62 : le message doit citer le locator, message=" + ex.getMessage());
        assertTrue(web.isDisabled(TARGET), "T62 : #cible encore desactive a l'expiration");
        assertTrue(ms >= timeout.toMillis(), "T62 : l'exception ne doit pas preceder le timeout, ms=" + ms);
        assertTrue(ms < targetAt, "T62 : l'exception doit preceder l'activation (" + targetAt + " ms), ms=" + ms);

        System.out.println("[T62] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (timeout " + timeout.toMillis() + ", activation " + targetAt + ") | message=\""
                + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T63 / T64 : waitUntilClickable ----
    @Test
    @DisplayName("T63 - PASS - waitUntilClickable, activation a T-5s : aboutit avant T, element cliquable")
    void T63_waitUntilClickable() {
        Duration timeout = timeout();
        long targetAt = Transitions.open(Scenario.ENABLE, timeout.minus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitUntilClickable(TARGET);
        long ms = since(start);
        assertTrue(web.isClickable(TARGET), "T63 : #cible doit etre cliquable apres l'attente");
        assertTrue(ms >= targetAt - TOLERANCE_MS, "T63 : l'attente doit avoir dure jusqu'a l'activation (" + targetAt + " ms), ms=" + ms);
        assertTrue(ms < timeout.toMillis(), "T63 : l'attente doit aboutir avant le timeout (" + timeout.toMillis() + " ms), ms=" + ms);

        System.out.println("[T63] RESULTAT : cliquable en " + ms + " ms | activation a " + targetAt + " ms | timeout "
                + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T64 - NON PASS - waitUntilClickable, activation a T+5s : SyncException entre T et l'activation")
    void T64_waitUntilClickable() {
        Duration timeout = timeout();
        long targetAt = Transitions.open(Scenario.ENABLE, timeout.plus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitUntilClickable(TARGET),
                "T64 : l'attente doit expirer avant l'activation");
        long ms = since(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T64 : le message doit citer le locator, message=" + ex.getMessage());
        assertFalse(web.isClickable(TARGET), "T64 : #cible encore non cliquable a l'expiration");
        assertTrue(ms >= timeout.toMillis(), "T64 : l'exception ne doit pas preceder le timeout, ms=" + ms);
        assertTrue(ms < targetAt, "T64 : l'exception doit preceder l'activation (" + targetAt + " ms), ms=" + ms);

        System.out.println("[T64] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (timeout " + timeout.toMillis() + ", activation " + targetAt + ") | message=\""
                + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T65 / T66 : waitUntilDisabled ----
    @Test
    @DisplayName("T65 - PASS - waitUntilDisabled, desactivation a T-5s : aboutit avant T, element desactive")
    void T65_waitUntilDisabled() {
        Duration timeout = timeout();
        long targetAt = Transitions.open(Scenario.DISABLE, timeout.minus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitUntilDisabled(TARGET);
        long ms = since(start);
        assertTrue(web.isDisabled(TARGET), "T65 : #cible doit etre desactive apres l'attente");
        assertTrue(ms >= targetAt - TOLERANCE_MS, "T65 : l'attente doit avoir dure jusqu'a la desactivation (" + targetAt + " ms), ms=" + ms);
        assertTrue(ms < timeout.toMillis(), "T65 : l'attente doit aboutir avant le timeout (" + timeout.toMillis() + " ms), ms=" + ms);

        System.out.println("[T65] RESULTAT : desactive en " + ms + " ms | desactivation a " + targetAt + " ms | timeout "
                + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T66 - NON PASS - waitUntilDisabled, desactivation a T+5s : SyncException entre T et la desactivation")
    void T66_waitUntilDisabled() {
        Duration timeout = timeout();
        long targetAt = Transitions.open(Scenario.DISABLE, timeout.plus(Timeouts.MARGIN));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitUntilDisabled(TARGET),
                "T66 : l'attente doit expirer avant la desactivation");
        long ms = since(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T66 : le message doit citer le locator, message=" + ex.getMessage());
        assertTrue(web.isEnabled(TARGET), "T66 : #cible encore actif a l'expiration");
        assertTrue(ms >= timeout.toMillis(), "T66 : l'exception ne doit pas preceder le timeout, ms=" + ms);
        assertTrue(ms < targetAt, "T66 : l'exception doit preceder la desactivation (" + targetAt + " ms), ms=" + ms);

        System.out.println("[T66] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (timeout " + timeout.toMillis() + ", desactivation " + targetAt + ") | message=\""
                + ex.getMessage() + "\" => cas non passant confirme");
    }
}
