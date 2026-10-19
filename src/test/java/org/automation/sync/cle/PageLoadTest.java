package org.automation.sync.cle;

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
 * Groupe {@code cle} — plafond de chargement de page {@code qa.sync.pageLoadTimeout} (exécution « cle »
 * du pom ; défaut socle 2 min). Fixture {@code slowload.html} : un script synchrone bloque le chargement
 * pendant {@code ?ms=N}. Règle ± 5 s autour du plafond, comme les profils.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class PageLoadTest {

    /** 1er contact WebSync : c'est lui qui pose le pageLoadTimeout sur le driver (un getDriver() n'arme rien). */
    private static void armerPageLoadTimeout() {
        new WebSync().isPresent(By.tagName("html"));
    }

    // ---- T6 : page bloquee au-dela du plafond, navigation bornee ----
    @Test
    @DisplayName("T6 - page bloquee plafond+5s : navigation bornee par pageLoadTimeout (exception au plafond)")
    void T6_page_load_timeout() {
        Duration plafond = Timeouts.pageLoadTimeout();
        long blocage = plafond.plus(Timeouts.MARGE).toMillis();
        armerPageLoadTimeout();
        String slowPage = Fixtures.url("slowload.html", "ms=" + blocage);
        WebDriver driver = getDriver();
        long start = System.nanoTime();
        WebDriverException ex = assertThrows(WebDriverException.class,
                () -> driver.get(slowPage),
                "T6 : la navigation doit echouer au plafond (" + plafond.toMillis() + " ms)");
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms >= plafond.toMillis(),
                "T6 : levee avant le plafond (" + plafond.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(ms < blocage,
                "T6 : levee apres la fin du blocage (" + blocage + " ms), mesure=" + ms + " ms");
        System.out.println("[T6] RESULTAT : exception attendue=WebDriverException (borne pageLoadTimeout) | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | navigation bornee en " + ms + " ms (plafond "
                + plafond.toMillis() + " ms, page bloquee " + blocage + " ms) => cas non passant confirme");
    }

    // ---- T6bis : page bloquee sous le plafond, navigation aboutie ----
    @Test
    @DisplayName("T6bis - page bloquee plafond-5s : navigation aboutie sous le plafond pageLoadTimeout")
    void T6bis_page_load_sous_le_plafond() {
        Duration plafond = Timeouts.pageLoadTimeout();
        long blocage = plafond.minus(Timeouts.MARGE).toMillis();
        armerPageLoadTimeout();
        String slowPage = Fixtures.url("slowload.html", "ms=" + blocage);
        WebDriver driver = getDriver();
        long start = System.nanoTime();
        driver.get(slowPage);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms >= blocage, "T6bis : la page doit avoir bloque ~" + blocage + " ms, mesure=" + ms + " ms");
        assertTrue(ms < plafond.toMillis(),
                "T6bis : navigation attendue sous le plafond (" + plafond.toMillis() + " ms), mesure=" + ms + " ms");
        assertTrue(new WebSync().isPresent(By.id("ms-txt")), "T6bis : page chargee attendue");
        System.out.println("[T6bis] RESULTAT : navigation aboutie en " + ms + " ms (page bloquee " + blocage
                + " ms, plafond " + plafond.toMillis() + " ms) | aucune exception => cas passant confirme");
    }
}
