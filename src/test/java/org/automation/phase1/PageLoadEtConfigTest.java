package org.automation.phase1;

import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.thucydides.model.util.EnvironmentVariables;
import net.thucydides.model.environment.SystemEnvironmentVariables;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        // 1er contact WebSync : c'est lui qui pose le pageLoadTimeout sur le driver (cf. briefing).
        // Un simple getDriver() n'arme rien -> la navigation ne serait jamais bornee.
        WebSync web = new WebSync();
        web.isPresent(By.tagName("html"));
        // Page bloquee 8s ; pageLoadTimeout abaisse a 5s via -Dqa.sync.pageLoadTimeout=5s.
        String slowPage = Fixtures.url("slowload.html", "ms=8000");
        WebDriver driver = getDriver();
        long start = System.nanoTime();
        WebDriverException ex = assertThrows(WebDriverException.class,
                () -> driver.get(slowPage),
                "T6 : la navigation doit echouer (timeout) avant les 8s de blocage");
        long ms = (System.nanoTime() - start) / 1_000_000;

        assertTrue(ms < 7500, "T6 : borne ~5s attendue (< 8s de blocage), mesure=" + ms + "ms");
        System.out.println("[T6] RESULTAT : exception attendue=WebDriverException (borne pageLoadTimeout) | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | navigation bornee en " + ms + " ms (< 8s)"
                + " => cas non passant confirme");
    }

    // ---- T24 : precedence -D > serenity.conf ----
    @Test
    @DisplayName("T24 - precedence -D > serenity.conf : la valeur -D gagne")
    void T24_precedence_D() {
        // On lit environment : serenity.conf = 'qatest', on lance ce test avec -Denvironment=override-D.
        EnvironmentVariables env = SystemEnvironmentVariables.createEnvironmentVariables();
        String environment = env.getProperty("environment");
        assertEquals("override-D", environment,
                "T24 : la valeur -D (override-D) doit primer sur serenity.conf (qatest)");
        // Sanity : un WebSync s'instancie sans souci avec cette config.
        new WebSync().isPresent(By.id("nimporte"));
        System.out.println("[T24] RESULTAT : environment resolu=" + environment
                + " (attendu override-D via -D, serenity.conf=qatest) | aucune exception => cas passant confirme");
    }
}
