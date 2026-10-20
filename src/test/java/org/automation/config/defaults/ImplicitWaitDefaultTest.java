package org.automation.config.defaults;

import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.automation.support.Timeouts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code defaults} — bornes de l'<b>implicit wait</b> natif ({@code serenity.conf}, 3 s) et du sondage de
 * {@code WebSync} à sa valeur par défaut (aucune clé {@code qa.sync.*}). Fixture {@code appear.html}.
 *
 * <p>Tests chronométrés, donc sans étape : l'instrumentation d'une étape s'ajouterait à la mesure.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ImplicitWaitDefaultTest {

    private static final By TARGET = By.id("target");

    private void open(String fixture, String query) {
        getDriver().get(Fixtures.url(fixture, query));
    }

    // ---- L1 : implicit wait natif (serenity.conf, 3 s) — borne basse, marge courte 2 s ----
    @Test
    @DisplayName("L1 - PASS - implicit wait borne basse : findElement brut trouve un element apparu a implicit-2s")
    void L1_implicit_wait_low() {
        Duration implicit = Timeouts.implicitWait();
        long delay = implicit.minus(Timeouts.SHORT_MARGIN).toMillis();
        open("appear.html", "delay=" + delay);
        WebDriver driver = getDriver();
        long start = System.nanoTime();
        var el = driver.findElement(TARGET); // brut : s'appuie sur l'implicit wait natif
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(el.isDisplayed(), "L1 : element trouve attendu");
        assertTrue(ms < implicit.toMillis(),
                "L1 : doit trouver avant l'implicit wait (" + implicit.toMillis() + " ms), mesure=" + ms + "ms");
        System.out.println("[L1] RESULTAT : findElement brut trouve en " + ms + " ms (cible a " + delay
                + " ms, implicit wait " + implicit.toMillis() + " ms) | aucune exception => cas passant confirme");
    }

    // ---- L2 : implicit wait natif (serenity.conf, 3 s) — borne haute, marge courte 2 s ----
    @Test
    @DisplayName("L2 - NON PASS - implicit wait borne haute : findElement brut echoue pour un element a implicit+2s")
    void L2_implicit_wait_high() {
        Duration implicit = Timeouts.implicitWait();
        long delay = implicit.plus(Timeouts.SHORT_MARGIN).toMillis();
        open("appear.html", "delay=" + delay);
        WebDriver driver = getDriver();
        long start = System.nanoTime();
        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> driver.findElement(TARGET), "L2 : NoSuchElement attendu au-dela de l'implicit wait");
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms >= implicit.toMillis(),
                "L2 : levee avant l'implicit wait (" + implicit.toMillis() + " ms), mesure=" + ms + "ms");
        assertTrue(ms < delay, "L2 : levee apres l'arrivee de la cible (" + delay + " ms), mesure=" + ms + "ms");
        System.out.println("[L2] RESULTAT : exception attendue=NoSuchElementException | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | apres " + ms + " ms (implicit wait "
                + implicit.toMillis() + " ms, cible a " + delay + " ms) => cas non passant confirme");
    }

    // ---- L6 : polling fin sous WebSync malgre implicit 3 s ----
    @Test
    @DisplayName("L6 - PASS - WebSync (polling fin) reussit tot vs findElement brut ~3s")
    void L6_polling_beats_implicit_wait() {
        // WebSync profil LONG (defaut socle 60s, largement au-dela) ; cible a 2500ms.
        open("appear.html", "delay=2500");
        WebSync web = new WebSync(SyncTimeout.LONG);
        long startWeb = System.nanoTime();
        web.click(TARGET);
        long msWeb = (System.nanoTime() - startWeb) / 1_000_000;
        assertTrue(web.containsText(By.id("status"), "clicked"), "L6 : clic WebSync effectif attendu");

        // Nouvelle page pour comparer le brut sur un element apparaissant a 2500ms.
        open("appear.html", "delay=2500");
        long startRaw = System.nanoTime();
        var el = getDriver().findElement(TARGET); // brut : attend via implicit wait ~ jusqu'a 3s
        long msRaw = (System.nanoTime() - startRaw) / 1_000_000;
        System.out.println("[L6] INFO : findElement brut (comparaison) abouti en " + msRaw + " ms");
        assertTrue(el.isDisplayed(), "L6 : element brut affiche attendu");
        // Le brut ne rend la main qu'apres ~2.5s (attente implicite) ; WebSync doit avoir ete <= brut+marge.
        assertTrue(msWeb <= msRaw + 500,
                "L6 : WebSync (" + msWeb + "ms) attendu <= brut (" + msRaw + "ms) + marge");
        System.out.println("[L6] RESULTAT : WebSync.click (polling ~250ms) abouti en " + msWeb + " ms"
                + " <= brut " + msRaw + " ms + 500 ms | aucune exception => cas passant confirme");
    }
}
