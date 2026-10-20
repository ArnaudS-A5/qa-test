package org.automation.websync.diagnostic;

import com.example.qa.sync.SyncException;
import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;

import java.util.Arrays;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structure de la {@code SyncException} levée au timeout d'une action (fixture {@code states.html},
 * profil SHORT). Complète T2 et T7 à T9, qui ne vérifient que les codes du message.
 *
 * <p>Contrat du socle : {@code cause} = <b>dernière</b> exception Selenium réellement ignorée par la boucle ;
 * message = codes stables {@code [CODE]} des conditions latentes, dans l'ordre du flux d'interaction ;
 * {@code suppressed} = une exception Selenium typée par condition latente (ABSENT → NoSuchElement,
 * HIDDEN → ElementNotInteractable, DISABLED → InvalidElementState, INTERCEPTED → ElementClickIntercepted).
 *
 * <p>On ne vérifie jamais l'<b>absence</b> d'autres codes : le diagnostic ajoute un faux positif
 * {@code [OUT_OF_BOUNDS]} sur un élément en {@code display:none} (écart socle connu, suggestions §4).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class DiagnosticCauseTest {

    private static final By ABSENT = By.id("absent");
    private static final By HIDDEN = By.id("cache");
    private static final By COVERED = By.id("recouvert");
    private static final By HIDDEN_DISABLED = By.id("cache-desactive");

    private WebSync openStates() {
        getDriver().get(Fixtures.url("states.html"));
        return new WebSync(SyncTimeout.SHORT);
    }

    // ---- T71 : element absent ----
    @Test
    @DisplayName("T71 - NON PASS - absent : cause NoSuchElementException, code [ABSENT], suppressed type")
    void T71_cause_absent() {
        WebSync web = openStates();

        SyncException ex = assertThrows(SyncException.class, () -> web.click(ABSENT), "T71 : clic sur absent");
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof NoSuchElementException, "T71 : cause attendue NoSuchElementException, recue=" + cause);
        assertTrue(ex.getMessage().contains("[ABSENT]") && ex.getMessage().contains("By.id: absent"),
                "T71 : message avec le code [ABSENT] et le locator, message=" + ex.getMessage());
        assertTrue(Arrays.stream(ex.getSuppressed()).anyMatch(s -> s instanceof NoSuchElementException),
                "T71 : un suppressed NoSuchElementException attendu, suppressed=" + Arrays.toString(ex.getSuppressed()));

        System.out.println("[T71] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI | cause="
                + cause.getClass().getName() + " | suppressed=" + ex.getSuppressed().length
                + " | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T72 : element cache ----
    @Test
    @DisplayName("T72 - NON PASS - cache : cause ElementNotInteractableException, code [HIDDEN], suppressed type")
    void T72_cause_hidden() {
        WebSync web = openStates();

        SyncException ex = assertThrows(SyncException.class, () -> web.click(HIDDEN), "T72 : clic sur element cache");
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof ElementNotInteractableException && !(cause instanceof ElementClickInterceptedException),
                "T72 : cause attendue ElementNotInteractableException (non interceptee), recue=" + cause);
        assertTrue(ex.getMessage().contains("[HIDDEN]") && ex.getMessage().contains("By.id: cache"),
                "T72 : message avec le code [HIDDEN] et le locator, message=" + ex.getMessage());
        assertTrue(Arrays.stream(ex.getSuppressed()).anyMatch(s -> s.getClass() == ElementNotInteractableException.class),
                "T72 : un suppressed ElementNotInteractableException attendu, suppressed=" + Arrays.toString(ex.getSuppressed()));

        System.out.println("[T72] RESULTAT : exception attendue=SyncException [HIDDEN] | levee=OUI | cause="
                + cause.getClass().getName() + " | suppressed=" + ex.getSuppressed().length
                + " | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T73 : element recouvert ----
    @Test
    @DisplayName("T73 - NON PASS - recouvert : cause ElementClickInterceptedException, code [INTERCEPTED], suppressed type")
    void T73_cause_intercepted() {
        WebSync web = openStates();

        SyncException ex = assertThrows(SyncException.class, () -> web.click(COVERED), "T73 : clic sur element recouvert");
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof ElementClickInterceptedException,
                "T73 : cause attendue ElementClickInterceptedException, recue=" + cause);
        assertTrue(ex.getMessage().contains("[INTERCEPTED]") && ex.getMessage().contains("By.id: recouvert"),
                "T73 : message avec le code [INTERCEPTED] et le locator, message=" + ex.getMessage());
        assertTrue(Arrays.stream(ex.getSuppressed()).anyMatch(s -> s instanceof ElementClickInterceptedException),
                "T73 : un suppressed ElementClickInterceptedException attendu, suppressed=" + Arrays.toString(ex.getSuppressed()));

        System.out.println("[T73] RESULTAT : exception attendue=SyncException [INTERCEPTED] | levee=OUI | cause="
                + cause.getClass().getName() + " | suppressed=" + ex.getSuppressed().length
                + " | message=\"" + ex.getMessage() + "\" => cas non passant confirme");
    }

    // ---- T74 : plusieurs conditions latentes (cache ET desactive) ----
    @Test
    @DisplayName("T74 - NON PASS - cache et desactive : codes [HIDDEN] puis [DISABLED], cause = garde d'activation, un suppressed par code")
    void T74_codes_multiple() {
        WebSync web = openStates();

        SyncException ex = assertThrows(SyncException.class, () -> web.click(HIDDEN_DISABLED),
                "T74 : clic sur element cache et desactive");
        String message = ex.getMessage();
        int hidden = message.indexOf("[HIDDEN]");
        int disabled = message.indexOf("[DISABLED]");
        assertTrue(hidden >= 0 && disabled >= 0, "T74 : les deux codes attendus, message=" + message);
        assertTrue(hidden < disabled, "T74 : codes dans l'ordre du flux (HIDDEN avant DISABLED), message=" + message);
        // La garde d'activation du socle passe AVANT l'interaction : c'est elle qui a ete ignoree en dernier.
        assertEquals(InvalidElementStateException.class, ex.getCause().getClass(),
                "T74 : cause = refus de la garde d'activation, recue=" + ex.getCause());
        assertTrue(Arrays.stream(ex.getSuppressed()).anyMatch(s -> s.getClass() == ElementNotInteractableException.class),
                "T74 : un suppressed par code, HIDDEN manquant, suppressed=" + Arrays.toString(ex.getSuppressed()));
        assertTrue(Arrays.stream(ex.getSuppressed()).anyMatch(s -> s.getClass() == InvalidElementStateException.class),
                "T74 : un suppressed par code, DISABLED manquant, suppressed=" + Arrays.toString(ex.getSuppressed()));
        assertFalse(message.contains("[ABSENT]"), "T74 : l'element existe, [ABSENT] ne doit pas apparaitre, message=" + message);

        System.out.println("[T74] RESULTAT : exception attendue=SyncException [HIDDEN] + [DISABLED] | levee=OUI | cause="
                + ex.getCause().getClass().getName() + " | suppressed=" + ex.getSuppressed().length
                + " | message=\"" + message + "\" => cas non passant confirme");
    }
}
