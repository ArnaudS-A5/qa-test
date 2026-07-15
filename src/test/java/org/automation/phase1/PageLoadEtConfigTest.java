package org.automation.phase1;

import com.example.qa.api.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.thucydides.model.util.EnvironmentVariables;
import net.thucydides.model.environment.SystemEnvironmentVariables;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PHASE 1 — pageLoadTimeout (T6) et précédence -D > serenity.conf (T24).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class PageLoadEtConfigTest {

    // ---- T6 : pageLoadTimeout pose auto, echec de navigation borne ----
    @Test
    @DisplayName("T6 - slowload bloque 8s avec pageLoadTimeout abaisse a 5s : navigation bornee ~5s")
    void T6_page_load_timeout() {
        // Force le 1er contact driver pour que le socle pose le pageLoadTimeout auto.
        getDriver();
        long start = System.nanoTime();
        boolean bounded = false;
        try {
            // Page bloquee 8s ; pageLoadTimeout abaisse a 5s via -Dqa.sync.pageLoadTimeout=5s.
            getDriver().get(Fixtures.url("slowload.html", "ms=8000"));
        } catch (WebDriverException e) {
            bounded = true;
            System.out.println("[T6] navigation bornee : " + e.getClass().getSimpleName()
                    + " - " + e.getMessage());
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("[T6] duree navigation = " + ms + " ms (attendu ~5s, < 8s)");
        assertTrue(bounded, "T6 : la navigation doit echouer (timeout) avant les 8s de blocage");
        assertTrue(ms < 7500, "T6 : borne ~5s attendue (< 8s de blocage), mesure=" + ms + "ms");
    }

    // ---- T24 : precedence -D > serenity.conf ----
    @Test
    @DisplayName("T24 - precedence -D > serenity.conf : la valeur -D gagne")
    void T24_precedence_D() {
        // On lit environment : serenity.conf = 'qatest', on lance ce test avec -Denvironment=override-D.
        EnvironmentVariables env = SystemEnvironmentVariables.createEnvironmentVariables();
        String environment = env.getProperty("environment");
        System.out.println("[T24] environment resolu = " + environment);
        assertTrue("override-D".equals(environment),
                "T24 : la valeur -D (override-D) doit primer sur serenity.conf (qatest), obtenu=" + environment);
        // Sanity : un WebSync s'instancie sans souci avec cette config.
        new WebSync().isPresent(By.id("nimporte"));
    }
}
