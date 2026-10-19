package org.automation.sync.polling;

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

import static org.automation.support.BornesProfil.depuis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code polling} — clé {@code qa.sync.pollingInterval}, posée par l'exécution « polling » du pom
 * (différente du défaut du socle, 250 ms). Fixture {@code websync/transitions.html}.
 *
 * <p>Principe : la boucle du socle sonde à 0, 1 intervalle, 2 intervalles… La cible apparaît à
 * <b>1,5 intervalle</b> : le 2e sondage la manque, le 3e la trouve, donc le clic survient à <b>2 intervalles</b>
 * au plus tôt (les attentes entre sondages durent au moins l'intervalle). Avec le défaut de 250 ms, il
 * surviendrait dès l'apparition (au plus 250 ms après) : la borne basse prouve que la clé est lue.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class PollingIntervalTest {

    // ---- T78 : intervalle de sondage configure ----
    @Test
    @DisplayName("T78 - qa.sync.pollingInterval : cible apparue entre deux sondages, cliquee au sondage suivant")
    void T78_polling_interval_cle() {
        Duration intervalle = Timeouts.pollingInterval();
        long apparition = Transitions.ouvrir(Scenario.APPARITION, intervalle.multipliedBy(3).dividedBy(2));
        long troisiemeSondage = intervalle.multipliedBy(2).toMillis();
        WebSync web = new WebSync(SyncTimeout.MEDIUM);

        long start = System.nanoTime();
        web.click(Transitions.CIBLE);
        long ms = depuis(start);
        assertEquals("clicked", web.getText(Transitions.STATUT), "T78 : le clic doit avoir abouti");
        assertTrue(ms >= troisiemeSondage, "T78 : clic au 3e sondage au plus tot (" + troisiemeSondage + " ms) ;"
                + " avec le defaut " + Timeouts.POLLING_INTERVAL_DEFAUT.toMillis() + " ms il aurait eu lieu vers "
                + apparition + " ms, ms=" + ms);
        assertTrue(ms < troisiemeSondage + intervalle.toMillis() / 2, "T78 : clic au 3e sondage, pas au-dela, ms=" + ms);

        System.out.println("[T78] RESULTAT : intervalle=" + intervalle.toMillis() + " ms | apparition a " + apparition
                + " ms | clic en " + ms + " ms (3e sondage a " + troisiemeSondage + " ms) | aucune exception => cas passant confirme");
    }
}
