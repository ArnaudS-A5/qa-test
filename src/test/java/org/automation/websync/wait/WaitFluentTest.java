package org.automation.websync.wait;

import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — attente implicite du moteur ({@code fluentWait}) derrière chaque appel : un élément
 * tardif est attendu, un nœud re-rendu est ré-résolu sans {@code StaleElementReferenceException}. Fixtures
 * {@code appear.html} et {@code websync/wait/rerender.html}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class WaitFluentTest {

    private static final By TARGET = By.id("target");

    private void open(String fixture, String query) {
        getDriver().get(Fixtures.url(fixture, query));
    }

    // ---- T1 : fluentWait attend un element tardif ----
    @Test
    @DisplayName("T1 - PASS - fluentWait attend un element tardif : WebSync.click reussit")
    void T1_fluentWait_late_element() {
        open("appear.html", "delay=1500");
        WebSync web = new WebSync(SyncTimeout.LONG);
        web.click(TARGET);
        assertTrue(web.containsText(By.id("status"), "clicked"), "T1 : action reussie sur element tardif");
    }

    // ---- T5 : stale / re-render absorbe ----
    @Test
    @DisplayName("T5 - PASS - stale/re-render absorbe : pas de StaleElement, re-resolution transparente")
    void T5_fluentWait_stale() {
        // #target remplace toutes les 200ms ; WebSync doit re-resoudre a chaque poll.
        open("websync/wait/rerender.html", "interval=200");
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
}
