package org.automation.websync.attente;

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

import static org.automation.support.BornesProfil.TOLERANCE_MS;
import static org.automation.support.BornesProfil.depuis;
import static org.automation.support.Transitions.CIBLE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — <b>attentes d'état</b> {@code waitUntilPresent/Enabled/Clickable/Disabled}, selon
 * la règle aux limites <b>± 5 s</b> autour du profil MEDIUM au défaut du socle (15 s), sur la fixture
 * {@code websync/transitions.html}. Complète T15 ({@code waitUntilVisible}/{@code waitUntilNotVisible}).
 *
 * <p>Cas <b>passant</b> : la transition survient à timeout − 5 s, l'attente aboutit après l'avoir
 * réellement attendue et avant le timeout. Cas <b>non passant</b> : la transition survient à timeout + 5 s,
 * {@code SyncException} levée <b>entre</b> le timeout et la transition, l'élément encore dans l'état initial.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class AttentesEtatTest {

    private static Duration timeout() {
        return Timeouts.defaultOf(SyncTimeout.MEDIUM);
    }

    // ---- T59 / T60 : waitUntilPresent ----
    @Test
    @DisplayName("T59 - waitUntilPresent, insertion invisible a T-5s : aboutit avant T, element present mais pas visible")
    void T59_wait_until_present_passant() {
        Duration timeout = timeout();
        long cible = Transitions.ouvrir(Scenario.APPARITION_CACHEE, timeout.minus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitUntilPresent(CIBLE);
        long ms = depuis(start);
        assertTrue(web.isPresent(CIBLE), "T59 : #cible doit etre present apres l'attente");
        assertFalse(web.isCurrentlyVisible(CIBLE), "T59 : present ne veut pas dire visible (#cible en display:none)");
        assertTrue(ms >= cible - TOLERANCE_MS, "T59 : l'attente doit avoir dure jusqu'a l'insertion (" + cible + " ms), ms=" + ms);
        assertTrue(ms < timeout.toMillis(), "T59 : l'attente doit aboutir avant le timeout (" + timeout.toMillis() + " ms), ms=" + ms);

        System.out.println("[T59] RESULTAT : present (invisible) en " + ms + " ms | insertion a " + cible
                + " ms | timeout " + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T60 - waitUntilPresent, insertion a T+5s : SyncException entre T et l'insertion")
    void T60_wait_until_present_non_passant() {
        Duration timeout = timeout();
        long cible = Transitions.ouvrir(Scenario.APPARITION_CACHEE, timeout.plus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitUntilPresent(CIBLE),
                "T60 : l'attente doit expirer avant l'insertion");
        long ms = depuis(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T60 : le message doit citer le locator, message=" + ex.getMessage());
        assertFalse(web.isPresent(CIBLE), "T60 : #cible encore absent a l'expiration");
        assertTrue(ms >= timeout.toMillis(), "T60 : l'exception ne doit pas preceder le timeout, ms=" + ms);
        assertTrue(ms < cible, "T60 : l'exception doit preceder l'insertion (" + cible + " ms), ms=" + ms);

        System.out.println("[T60] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (timeout " + timeout.toMillis() + ", insertion " + cible + ") | message=\""
                + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T61 / T62 : waitUntilEnabled ----
    @Test
    @DisplayName("T61 - waitUntilEnabled, activation a T-5s : aboutit avant T, element actif")
    void T61_wait_until_enabled_passant() {
        Duration timeout = timeout();
        long cible = Transitions.ouvrir(Scenario.ACTIVATION, timeout.minus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitUntilEnabled(CIBLE);
        long ms = depuis(start);
        assertTrue(web.isEnabled(CIBLE), "T61 : #cible doit etre actif apres l'attente");
        assertTrue(ms >= cible - TOLERANCE_MS, "T61 : l'attente doit avoir dure jusqu'a l'activation (" + cible + " ms), ms=" + ms);
        assertTrue(ms < timeout.toMillis(), "T61 : l'attente doit aboutir avant le timeout (" + timeout.toMillis() + " ms), ms=" + ms);

        System.out.println("[T61] RESULTAT : actif en " + ms + " ms | activation a " + cible + " ms | timeout "
                + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T62 - waitUntilEnabled, activation a T+5s : SyncException entre T et l'activation")
    void T62_wait_until_enabled_non_passant() {
        Duration timeout = timeout();
        long cible = Transitions.ouvrir(Scenario.ACTIVATION, timeout.plus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitUntilEnabled(CIBLE),
                "T62 : l'attente doit expirer avant l'activation");
        long ms = depuis(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T62 : le message doit citer le locator, message=" + ex.getMessage());
        assertTrue(web.isDisabled(CIBLE), "T62 : #cible encore desactive a l'expiration");
        assertTrue(ms >= timeout.toMillis(), "T62 : l'exception ne doit pas preceder le timeout, ms=" + ms);
        assertTrue(ms < cible, "T62 : l'exception doit preceder l'activation (" + cible + " ms), ms=" + ms);

        System.out.println("[T62] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (timeout " + timeout.toMillis() + ", activation " + cible + ") | message=\""
                + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T63 / T64 : waitUntilClickable ----
    @Test
    @DisplayName("T63 - waitUntilClickable, activation a T-5s : aboutit avant T, element cliquable")
    void T63_wait_until_clickable_passant() {
        Duration timeout = timeout();
        long cible = Transitions.ouvrir(Scenario.ACTIVATION, timeout.minus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitUntilClickable(CIBLE);
        long ms = depuis(start);
        assertTrue(web.isClickable(CIBLE), "T63 : #cible doit etre cliquable apres l'attente");
        assertTrue(ms >= cible - TOLERANCE_MS, "T63 : l'attente doit avoir dure jusqu'a l'activation (" + cible + " ms), ms=" + ms);
        assertTrue(ms < timeout.toMillis(), "T63 : l'attente doit aboutir avant le timeout (" + timeout.toMillis() + " ms), ms=" + ms);

        System.out.println("[T63] RESULTAT : cliquable en " + ms + " ms | activation a " + cible + " ms | timeout "
                + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T64 - waitUntilClickable, activation a T+5s : SyncException entre T et l'activation")
    void T64_wait_until_clickable_non_passant() {
        Duration timeout = timeout();
        long cible = Transitions.ouvrir(Scenario.ACTIVATION, timeout.plus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitUntilClickable(CIBLE),
                "T64 : l'attente doit expirer avant l'activation");
        long ms = depuis(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T64 : le message doit citer le locator, message=" + ex.getMessage());
        assertFalse(web.isClickable(CIBLE), "T64 : #cible encore non cliquable a l'expiration");
        assertTrue(ms >= timeout.toMillis(), "T64 : l'exception ne doit pas preceder le timeout, ms=" + ms);
        assertTrue(ms < cible, "T64 : l'exception doit preceder l'activation (" + cible + " ms), ms=" + ms);

        System.out.println("[T64] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (timeout " + timeout.toMillis() + ", activation " + cible + ") | message=\""
                + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T65 / T66 : waitUntilDisabled ----
    @Test
    @DisplayName("T65 - waitUntilDisabled, desactivation a T-5s : aboutit avant T, element desactive")
    void T65_wait_until_disabled_passant() {
        Duration timeout = timeout();
        long cible = Transitions.ouvrir(Scenario.DESACTIVATION, timeout.minus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitUntilDisabled(CIBLE);
        long ms = depuis(start);
        assertTrue(web.isDisabled(CIBLE), "T65 : #cible doit etre desactive apres l'attente");
        assertTrue(ms >= cible - TOLERANCE_MS, "T65 : l'attente doit avoir dure jusqu'a la desactivation (" + cible + " ms), ms=" + ms);
        assertTrue(ms < timeout.toMillis(), "T65 : l'attente doit aboutir avant le timeout (" + timeout.toMillis() + " ms), ms=" + ms);

        System.out.println("[T65] RESULTAT : desactive en " + ms + " ms | desactivation a " + cible + " ms | timeout "
                + timeout.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T66 - waitUntilDisabled, desactivation a T+5s : SyncException entre T et la desactivation")
    void T66_wait_until_disabled_non_passant() {
        Duration timeout = timeout();
        long cible = Transitions.ouvrir(Scenario.DESACTIVATION, timeout.plus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitUntilDisabled(CIBLE),
                "T66 : l'attente doit expirer avant la desactivation");
        long ms = depuis(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T66 : le message doit citer le locator, message=" + ex.getMessage());
        assertTrue(web.isEnabled(CIBLE), "T66 : #cible encore actif a l'expiration");
        assertTrue(ms >= timeout.toMillis(), "T66 : l'exception ne doit pas preceder le timeout, ms=" + ms);
        assertTrue(ms < cible, "T66 : l'exception doit preceder la desactivation (" + cible + " ms), ms=" + ms);

        System.out.println("[T66] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (timeout " + timeout.toMillis() + ", desactivation " + cible + ") | message=\""
                + ex.getMessage() + "\" => cas non passant confirme");
    }
}
