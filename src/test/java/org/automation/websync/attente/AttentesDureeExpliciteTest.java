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
 * Surface {@code WebSync} — attentes à <b>durée explicite</b> {@code waitForElement(By, Duration)} et
 * {@code waitForElementToDisappear(By, Duration)}, règle aux limites <b>± 5 s</b> autour de la durée passée
 * (8 s), sur la fixture {@code websync/transitions.html}.
 *
 * <p>L'instance est au profil MEDIUM (15 s par défaut), <b>plus long</b> que la durée + 5 s : une exception
 * entre 8 et 13 s prouve que c'est la <b>durée passée</b> qui borne l'attente, et non le profil.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class AttentesDureeExpliciteTest {

    /** Durée passée en paramètre : volontairement différente de tout profil du socle. */
    private static final Duration DUREE = Duration.ofSeconds(8);

    /** Profil de l'instance, au défaut du socle ; doit dépasser DUREE + MARGE pour que le test discrimine. */
    private static Duration profil() {
        return Timeouts.defaultOf(SyncTimeout.MEDIUM);
    }

    // ---- T67 / T68 : waitForElement(By, Duration) ----
    @Test
    @DisplayName("T67 - waitForElement(8s), apparition a 3s : aboutit avant 8s, element visible")
    void T67_wait_for_element_passant() {
        long cible = Transitions.ouvrir(Scenario.APPARITION, DUREE.minus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitForElement(CIBLE, DUREE);
        long ms = depuis(start);
        assertTrue(web.isVisible(CIBLE), "T67 : #cible doit etre visible apres l'attente");
        assertTrue(ms >= cible - TOLERANCE_MS, "T67 : l'attente doit avoir dure jusqu'a l'apparition (" + cible + " ms), ms=" + ms);
        assertTrue(ms < DUREE.toMillis(), "T67 : l'attente doit aboutir avant la duree passee (" + DUREE.toMillis() + " ms), ms=" + ms);

        System.out.println("[T67] RESULTAT : visible en " + ms + " ms | apparition a " + cible + " ms | duree "
                + DUREE.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T68 - waitForElement(8s), apparition a 13s : SyncException entre 8 et 13s, avant le profil MEDIUM")
    void T68_wait_for_element_non_passant() {
        Duration profil = profil();
        long cible = Transitions.ouvrir(Scenario.APPARITION, DUREE.plus(Timeouts.MARGE));
        assertTrue(profil.toMillis() > cible, "T68 : prealable, le profil (" + profil.toMillis()
                + " ms) doit depasser l'apparition (" + cible + " ms) pour distinguer duree et profil");
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitForElement(CIBLE, DUREE),
                "T68 : l'attente doit expirer a la duree passee");
        long ms = depuis(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T68 : le message doit citer le locator, message=" + ex.getMessage());
        assertFalse(web.isPresent(CIBLE), "T68 : #cible encore absent a l'expiration");
        assertTrue(ms >= DUREE.toMillis(), "T68 : l'exception ne doit pas preceder la duree passee, ms=" + ms);
        assertTrue(ms < cible, "T68 : l'exception doit preceder l'apparition (" + cible + " ms), donc le profil, ms=" + ms);

        System.out.println("[T68] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (duree " + DUREE.toMillis() + ", apparition " + cible + ", profil " + profil.toMillis()
                + ") | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T69 / T70 : waitForElementToDisappear(By, Duration) ----
    @Test
    @DisplayName("T69 - waitForElementToDisappear(8s), disparition a 3s : aboutit avant 8s, element retire")
    void T69_wait_for_element_to_disappear_passant() {
        long cible = Transitions.ouvrir(Scenario.DISPARITION, DUREE.minus(Timeouts.MARGE));
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.waitForElementToDisappear(CIBLE, DUREE);
        long ms = depuis(start);
        assertFalse(web.isPresent(CIBLE), "T69 : #cible doit avoir ete retire apres l'attente");
        assertTrue(ms >= cible - TOLERANCE_MS, "T69 : l'attente doit avoir dure jusqu'a la disparition (" + cible + " ms), ms=" + ms);
        assertTrue(ms < DUREE.toMillis(), "T69 : l'attente doit aboutir avant la duree passee (" + DUREE.toMillis() + " ms), ms=" + ms);

        System.out.println("[T69] RESULTAT : disparu en " + ms + " ms | disparition a " + cible + " ms | duree "
                + DUREE.toMillis() + " ms | aucune exception => cas passant confirme");
    }

    @Test
    @DisplayName("T70 - waitForElementToDisappear(8s), disparition a 13s : SyncException entre 8 et 13s, avant le profil MEDIUM")
    void T70_wait_for_element_to_disappear_non_passant() {
        Duration profil = profil();
        long cible = Transitions.ouvrir(Scenario.DISPARITION, DUREE.plus(Timeouts.MARGE));
        assertTrue(profil.toMillis() > cible, "T70 : prealable, le profil (" + profil.toMillis()
                + " ms) doit depasser la disparition (" + cible + " ms) pour distinguer duree et profil");
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        SyncException ex = assertThrows(SyncException.class, () -> web.waitForElementToDisappear(CIBLE, DUREE),
                "T70 : l'attente doit expirer a la duree passee");
        long ms = depuis(start);
        assertTrue(ex.getMessage().contains("By.id: cible"), "T70 : le message doit citer le locator, message=" + ex.getMessage());
        assertTrue(web.isVisible(CIBLE), "T70 : #cible encore visible a l'expiration");
        assertTrue(ms >= DUREE.toMillis(), "T70 : l'exception ne doit pas preceder la duree passee, ms=" + ms);
        assertTrue(ms < cible, "T70 : l'exception doit preceder la disparition (" + cible + " ms), donc le profil, ms=" + ms);

        System.out.println("[T70] RESULTAT : exception attendue=SyncException | levee=OUI | type reel=" + ex.getClass().getName()
                + " | en " + ms + " ms (duree " + DUREE.toMillis() + ", disparition " + cible + ", profil " + profil.toMillis()
                + ") | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }
}
