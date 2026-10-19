package org.automation;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.DelayedButtonSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PHASE 2 — validation du PASSAGE du profil {@link SyncTimeout} au {@code WebSync} du socle,
 * via un PageObject dédié ({@code DelayedButtonPage}, champ {@code web} hérité de {@code QaPageObject}).
 *
 * <p>Un bouton {@code #delayed} est injecté dans une page réelle après <b>10 s</b> :
 * <ul>
 *   <li><b>SHORT</b> (abaissé à 1 s) → l'attente EXPIRE avant l'arrivée → {@code SyncException} (cas non passant) ;</li>
 *   <li><b>MEDIUM</b> (15 s) → l'attente ABOUTIT (10 s &lt; 15 s) → bouton trouvé (cas passant).</li>
 * </ul>
 * Cross-browser (aucun throttling). À lancer avec {@code -Dqa.sync.timeout.short=1s}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class DynamicWaitTest {

    private static final int DELAY_SECONDS = 10;

    @Steps
    DelayedButtonSteps delayed;

    @Test
    @DisplayName("SyncTimeout SHORT (1s) : #delayed a 10s => SyncException (cas non passant)")
    void short_profile_times_out_before_delayed_element() {
        delayed.openAndScheduleDelayedButton(DELAY_SECONDS);
        System.out.println("=== [SyncTimeout/SHORT] CAS NON PASSANT : #delayed arrive a "
                + DELAY_SECONDS + "s, budget SHORT=1s -> exception attendue ===");

        SyncException ex = assertThrows(SyncException.class,
                () -> delayed.waitForDelayedButton(SyncTimeout.SHORT),
                "SHORT (1s) doit lever une SyncException avant l'arrivee de #delayed (" + DELAY_SECONDS + "s)");

        System.out.println("[SyncTimeout/SHORT] RESULTAT : exception attendue=SyncException"
                + " | levee=OUI | type reel=" + ex.getClass().getName()
                + " | message=\"" + firstLine(ex.getMessage()) + "\" => cas non passant confirme");
    }

    @Test
    @DisplayName("SyncTimeout MEDIUM (15s) : #delayed a 10s => trouve (cas passant)")
    void medium_profile_finds_delayed_element() {
        delayed.openAndScheduleDelayedButton(DELAY_SECONDS);
        System.out.println("=== [SyncTimeout/MEDIUM] CAS PASSANT : #delayed arrive a "
                + DELAY_SECONDS + "s, budget MEDIUM=15s -> succes attendu ===");

        long start = System.nanoTime();
        delayed.waitForDelayedButton(SyncTimeout.MEDIUM);
        long ms = (System.nanoTime() - start) / 1_000_000;

        assertTrue(delayed.isDelayedVisible(),
                "MEDIUM (15s) doit trouver #delayed apparu a " + DELAY_SECONDS + "s");
        System.out.println("[SyncTimeout/MEDIUM] RESULTAT : #delayed trouve en " + ms + " ms"
                + " | aucune exception | budget 15s => cas passant confirme");
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int nl = message.indexOf('\n');
        return nl < 0 ? message : message.substring(0, nl);
    }
}
