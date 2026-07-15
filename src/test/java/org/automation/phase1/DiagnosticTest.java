package org.automation.phase1;

import com.example.qa.api.exception.SyncException;
import com.example.qa.api.sync.SyncTimeout;
import com.example.qa.api.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PHASE 1 — Diagnostic actif (SyncCondition) : codes stables remontés au timeout.
 * Ordre de sondage : ABSENT → STALE → HIDDEN → DISABLED → OUT_OF_BOUNDS → INTERCEPTED.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class DiagnosticTest {

    private static final By TARGET = By.id("target");

    private WebSync openAndSync(String fixture) {
        getDriver().get(Fixtures.url(fixture, null));
        return new WebSync(SyncTimeout.SHORT); // profil court (abaisse via -D) => timeout rapide
    }

    // ---- T7 : HIDDEN ----
    @Test
    @DisplayName("T7 - element present mais display:none : SyncException [HIDDEN]")
    void T7_hidden() {
        WebSync web = openAndSync("hidden.html");
        SyncException ex = assertThrows(SyncException.class, () -> web.click(TARGET));
        System.out.println("[T7] " + ex.getMessage());
        assertTrue(ex.getMessage().contains("[HIDDEN]"), "T7 : [HIDDEN] attendu, msg=" + ex.getMessage());
    }

    // ---- T8 : DISABLED ----
    @Test
    @DisplayName("T8 - element visible mais disabled : SyncException [DISABLED]")
    void T8_disabled() {
        WebSync web = openAndSync("disabled.html");
        SyncException ex = assertThrows(SyncException.class, () -> web.click(TARGET));
        System.out.println("[T8] " + ex.getMessage());
        assertTrue(ex.getMessage().contains("[DISABLED]"), "T8 : [DISABLED] attendu, msg=" + ex.getMessage());
    }

    // ---- T9 : INTERCEPTED ----
    @Test
    @DisplayName("T9 - clic recouvert par overlay : SyncException [INTERCEPTED]")
    void T9_intercepted() {
        WebSync web = openAndSync("overlay.html");
        SyncException ex = assertThrows(SyncException.class, () -> web.click(TARGET));
        System.out.println("[T9] " + ex.getMessage());
        assertTrue(ex.getMessage().contains("[INTERCEPTED]"),
                "T9 : [INTERCEPTED] attendu, msg=" + ex.getMessage());
    }

    // ---- T10 : OUT_OF_BOUNDS ----
    @Test
    @DisplayName("T10 - element hors viewport : SyncException [OUT_OF_BOUNDS]")
    void T10_out_of_bounds() {
        WebSync web = openAndSync("offscreen.html");
        SyncException ex = assertThrows(SyncException.class, () -> web.click(TARGET));
        System.out.println("[T10] " + ex.getMessage());
        assertTrue(ex.getMessage().contains("[OUT_OF_BOUNDS]"),
                "T10 : [OUT_OF_BOUNDS] attendu, msg=" + ex.getMessage());
    }
}

