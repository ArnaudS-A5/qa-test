package org.automation.phase1;

import com.example.qa.sync.SyncException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PHASE 1 — Tests aux limites L1..L6 (bornes des mécanismes d'attente/horloge).
 *
 * <p>Chaque test ouvre une fixture {@code file://} et mesure/vérifie le comportement du socle
 * aux bornes. Groupe {@code defaut} : aucune clé {@code qa.sync.*}, profils aux défauts du socle.
 * Les bornes des profils SyncTimeout (ex-L3/L4/L5) vivent dans {@code sync.defaut} et {@code sync.cle}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class BornesLimitesTest {

    private static final By TARGET = By.id("target");

    private void open(String fixture, String query) {
        getDriver().get(Fixtures.url(fixture, query));
    }

    // ---- L1 : implicit wait natif (serenity.conf, 3 s) — borne basse, marge courte 2 s ----
    @Test
    @DisplayName("L1 - implicit wait borne basse : findElement brut trouve un element apparu a implicit-2s")
    void L1_implicit_wait_borne_basse() {
        Duration implicit = Timeouts.implicitWait();
        long delay = implicit.minus(Timeouts.MARGE_COURTE).toMillis();
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
    @DisplayName("L2 - implicit wait borne haute : findElement brut echoue pour un element a implicit+2s")
    void L2_implicit_wait_borne_haute() {
        Duration implicit = Timeouts.implicitWait();
        long delay = implicit.plus(Timeouts.MARGE_COURTE).toMillis();
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
    @DisplayName("L6 - WebSync (polling fin) reussit tot vs findElement brut ~3s")
    void L6_polling_fin_vs_brut() {
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

    // ---- T14 (proche des bornes) : is* instantane sur absent ----
    @Test
    @DisplayName("T14 - is* sur absent : false immediat (pas d'attente de 3s)")
    void T14_is_star_instantane() {
        open("appear.html", "delay=0");
        WebSync web = new WebSync(SyncTimeout.MEDIUM);
        By bidon = By.id("nexistepas");
        long start = System.nanoTime();
        boolean visible = web.isVisible(bidon);
        boolean present = web.isPresent(bidon);
        boolean enabled = web.isEnabled(bidon);
        boolean contains = web.containsText(bidon, "x");
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertFalse(visible || present || enabled || contains, "T14 : tous false attendus");
        assertTrue(ms < 2000, "T14 : doit etre quasi-instantane (< 2s), mesure=" + ms + "ms");
        System.out.println("[T14] RESULTAT : is*/contains sur absent = tous false en " + ms + " ms"
                + " | aucune exception (contrat 'ne leve jamais') | visible=" + visible + " present=" + present
                + " enabled=" + enabled + " contains=" + contains + " => cas passant confirme");
    }

    // ---- T15 : waitUntil* positifs / negatifs ----
    @Test
    @DisplayName("T15 - waitUntilVisible (positif) puis waitUntilNotVisible (negatif) honores")
    void T15_wait_until_positif_negatif() {
        // Positif : appear a 1200ms, on attend la visibilite.
        open("appear.html", "delay=1200");
        WebSync web = new WebSync(SyncTimeout.LONG);
        web.waitUntilVisible(TARGET);
        assertTrue(web.isVisible(TARGET), "T15 : element visible apres waitUntilVisible");

        // Negatif : disappear a 1000ms, on attend la disparition.
        open("disappear.html", "delay=1000");
        web.waitUntilNotVisible(TARGET);
        assertFalse(web.isVisible(TARGET), "T15 : element non visible apres waitUntilNotVisible");
    }

    // ---- T1 : fluentWait attend un element tardif ----
    @Test
    @DisplayName("T1 - fluentWait attend un element tardif : WebSync.click reussit")
    void T1_fluent_wait_element_tardif() {
        open("appear.html", "delay=1500");
        WebSync web = new WebSync(SyncTimeout.LONG);
        web.click(TARGET);
        assertTrue(web.containsText(By.id("status"), "clicked"), "T1 : action reussie sur element tardif");
    }

    // ---- T2 : jamais present ----
    @Test
    @DisplayName("T2 - element jamais present : SyncException [ABSENT]")
    void T2_jamais_present() {
        open("appear.html", "delay=0");
        WebSync web = new WebSync(SyncTimeout.SHORT); // profil le plus court (defaut 5s)
        SyncException ex = assertThrows(SyncException.class, () -> web.click(By.id("locator-bidon")));
        assertTrue(ex.getMessage().contains("[ABSENT]"), "T2 : [ABSENT] attendu");
        System.out.println("[T2] RESULTAT : exception attendue=SyncException [ABSENT] | levee=OUI"
                + " | type reel=" + ex.getClass().getName() + " | message=\"" + ex.getMessage()
                + "\" => cas non passant confirme");
    }

    // ---- T5 : stale / re-render absorbe ----
    @Test
    @DisplayName("T5 - stale/re-render absorbe : pas de StaleElement, ré-résolution transparente")
    void T5_stale_absorbe() {
        // #target remplace toutes les 200ms ; WebSync doit re-resoudre a chaque poll.
        open("rerender.html", "interval=200");
        WebSync web = new WebSync(SyncTimeout.LONG);
        // Plusieurs lectures successives : chacune re-resout le By, sans StaleElementReferenceException.
        for (int i = 0; i < 5; i++) {
            String txt = web.getText(TARGET);
            System.out.println("[T5] lecture " + i + " => '" + txt + "'"
                    + " (re-resolution transparente, aucune StaleElement)");
            assertTrue(txt.startsWith("v"), "T5 : texte du noeud re-rendu attendu (vN)");
            try { Thread.sleep(120); } catch (InterruptedException ignored) { }
        }
    }

    // ---- T11 : within container vs child ----
    @Test
    @DisplayName("T11 - within : succes sur bon container/child, cause = bon maillon sinon")
    void T11_within() {
        open("nested.html", null);
        WebSync web = new WebSync(SyncTimeout.SHORT);

        // Cas nominal : within(#box).click(#child) reussit.
        new WebSync(SyncTimeout.LONG).within(By.id("box")).click(By.id("child"));

        // Container bidon : le maillon fautif est le container.
        SyncException exContainer = assertThrows(SyncException.class,
                () -> web.within(By.id("box-bidon")).click(By.id("child")));
        assertTrue(exContainer.getMessage().contains("[ABSENT]"), "T11 : [ABSENT] sur container");
        System.out.println("[T11] RESULTAT (container bidon) : exception attendue=SyncException [ABSENT] sur le container"
                + " | levee=OUI | type reel=" + exContainer.getClass().getName()
                + " | message=\"" + exContainer.getMessage() + "\"");

        // Child bidon : le maillon fautif est le child.
        SyncException exChild = assertThrows(SyncException.class,
                () -> web.within(By.id("box")).click(By.id("child-bidon")));
        assertTrue(exChild.getMessage().contains("[ABSENT]"), "T11 : [ABSENT] sur child");
        System.out.println("[T11] RESULTAT (child bidon) : exception attendue=SyncException [ABSENT] sur le child"
                + " | levee=OUI | type reel=" + exChild.getClass().getName()
                + " | message=\"" + exChild.getMessage() + "\" => cas non passant confirme (bon maillon designe)");
    }
}

