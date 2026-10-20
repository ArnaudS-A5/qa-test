package org.automation.config.configured;

import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.automation.support.Timeouts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code configured} — plafond de chargement de page {@code qa.sync.pageLoadTimeout} (exécution « configured »
 * du pom ; défaut socle 2 min). Fixture {@code config/configured/slowload.html} : un script synchrone bloque le chargement
 * pendant {@code ?ms=N}. Règle ± 5 s autour du plafond, comme les profils.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class PageLoadConfiguredTest {

    /** 1er contact WebSync : c'est lui qui pose le pageLoadTimeout sur le driver (un getDriver() n'arme rien). */
    private static void armPageLoadTimeout() {
        new WebSync().isPresent(By.tagName("html"));
    }

    // ---- T6 : page bloquee au-dela du plafond, navigation bornee ----
    @Test
    @DisplayName("T6 - NON PASS - page bloquee plafond+5s : navigation bornee par pageLoadTimeout (exception au plafond)")
    void T6_pageLoadTimeout_high() {
        Duration ceiling = Timeouts.pageLoadTimeout();
        long blockMs = ceiling.plus(Timeouts.MARGIN).toMillis();
        armPageLoadTimeout();
        String slowPage = Fixtures.url("config/configured/slowload.html", "ms=" + blockMs);
        WebDriver driver = getDriver();
        long start = System.nanoTime();
        WebDriverException ex = assertThrows(WebDriverException.class,
                () -> driver.get(slowPage),
                "T6 : la navigation doit echouer au plafond (" + ceiling.toMillis() + " ms)");
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms >= ceiling.toMillis(),
                "T6 : levee avant le plafond (" + ceiling.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(ms < blockMs,
                "T6 : levee apres la fin du blocage (" + blockMs + " ms), mesure=" + ms + " ms");
        System.out.println("[T6] RESULTAT : exception attendue=WebDriverException (borne pageLoadTimeout) | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | navigation bornee en " + ms + " ms (plafond "
                + ceiling.toMillis() + " ms, page bloquee " + blockMs + " ms) => cas non passant confirme");
    }

    // ---- T6bis : page bloquee sous le plafond, navigation aboutie ----
    @Test
    @DisplayName("T6bis - PASS - page bloquee plafond-5s : navigation aboutie sous le plafond pageLoadTimeout")
    void T6bis_pageLoadTimeout_low() {
        Duration ceiling = Timeouts.pageLoadTimeout();
        long blockMs = ceiling.minus(Timeouts.MARGIN).toMillis();
        armPageLoadTimeout();
        String slowPage = Fixtures.url("config/configured/slowload.html", "ms=" + blockMs);
        WebDriver driver = getDriver();
        long start = System.nanoTime();
        driver.get(slowPage);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms >= blockMs, "T6bis : la page doit avoir bloque ~" + blockMs + " ms, mesure=" + ms + " ms");
        assertTrue(ms < ceiling.toMillis(),
                "T6bis : navigation attendue sous le plafond (" + ceiling.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(new WebSync().isPresent(By.id("ms-txt")), "T6bis : page chargee attendue");
        System.out.println("[T6bis] RESULTAT : navigation aboutie en " + ms + " ms (page bloquee " + blockMs
                + " ms, plafond " + ceiling.toMillis() + " ms) | aucune exception => cas passant confirme");
    }
}
