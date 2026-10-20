package org.automation.config.configured;

import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.thucydides.core.webdriver.WebDriverFacade;
import org.automation.support.Timeouts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;
import org.openqa.selenium.chromium.HasNetworkConditions;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PHASE 2 — throttling reseau REEL (profil 2G) contre le site dedie, cote CHROME uniquement.
 *
 * <p>Le throttling passe par le CDP ({@link HasNetworkConditions}), disponible seulement sur
 * Chromium : Firefox/geckodriver n'a pas d'equivalent. Le test s'auto-ignore hors Chrome via
 * {@code assumeTrue} (il s'execute puis se skip selon le navigateur — <b>pas</b> {@code @Disabled}).
 * Il y a donc, volontairement, <b>un test de moins sur Firefox</b>.
 *
 * <p>Objectif : prouver que, sous une vraie lenteur reseau (2G), la navigation reelle vers
 * {@code /login} depasse le {@code pageLoadTimeout} du socle et est bornee (WebDriverException).
 * Groupe {@code configured} : le plafond {@code qa.sync.pageLoadTimeout} est pose par l'execution « configured » du pom
 * (la navigation 2G dure ~45 s, bien au-dela).
 */
@ExtendWith(SerenityJUnit5Extension.class)
class PageLoadThrottledTest {

    private static final String LOGIN_URL = "https://the-internet.herokuapp.com/login";

    /** Deballe le WebDriver reel derriere le facade Serenity (porte HasNetworkConditions si Chromium). */
    private static WebDriver proxied() {
        WebDriver d = getDriver();
        return (d instanceof WebDriverFacade) ? ((WebDriverFacade) d).getProxiedDriver() : d;
    }

    @Test
    @DisplayName("T83 - NON PASS - Sous 2G reel (Chrome), la navigation vers /login est bornee par le pageLoadTimeout du socle")
    void T83_pageLoadTimeout_slow_2g() {
        WebDriver driver = proxied();
        assumeTrue(driver instanceof HasNetworkConditions,
                "Throttling reseau (CDP) disponible seulement sur Chromium -> test ignore hors Chrome");
        HasNetworkConditions chromium = (HasNetworkConditions) driver;
        Duration ceiling = Timeouts.pageLoadTimeout(); // garde : sans la cle, plafond socle 2 min

        // Profil 2G : latence 300 ms, ~250 kbit/s down, ~50 kbit/s up (debits en octets/s pour le CDP).
        ChromiumNetworkConditions cond = new ChromiumNetworkConditions();
        cond.setOffline(false);
        cond.setLatency(Duration.ofMillis(300));
        cond.setDownloadThroughput(250 * 1000 / 8);
        cond.setUploadThroughput(50 * 1000 / 8);
        chromium.setNetworkConditions(cond);
        try {
            // 1er contact WebSync -> arme le pageLoadTimeout sur le driver (cf. T6).
            WebSync web = new WebSync();
            web.isPresent(By.tagName("html"));

            System.out.println("=== [2G-throttle] CAS NON PASSANT : navigation reelle /login sous 2G,"
                    + " pageLoadTimeout abaisse -> exception attendue ===");
            long start = System.nanoTime();
            WebDriverException ex = assertThrows(WebDriverException.class,
                    () -> driver.get(LOGIN_URL),
                    "Sous 2G, la navigation reelle doit etre bornee (timeout) par le socle");
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.println("[2G-throttle] RESULTAT : exception attendue=WebDriverException (borne pageLoadTimeout)"
                    + " | levee=OUI | type reel=" + ex.getClass().getName()
                    + " | navigation NON terminee, bornee apres " + ms + " ms (plafond " + ceiling.toMillis() + " ms)"
                    + " | message=\"" + firstLine(ex.getMessage()) + "\" => cas non passant confirme");
        } finally {
            chromium.setNetworkConditions(disabledConditions()); // on retire le throttle du process
        }
    }

    private static ChromiumNetworkConditions disabledConditions() {
        ChromiumNetworkConditions off = new ChromiumNetworkConditions();
        off.setOffline(false);
        off.setLatency(Duration.ZERO);
        off.setDownloadThroughput(-1);
        off.setUploadThroughput(-1);
        return off;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int nl = message.indexOf('\n');
        return nl < 0 ? message : message.substring(0, nl);
    }
}
