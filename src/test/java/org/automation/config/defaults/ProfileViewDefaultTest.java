package org.automation.config.defaults;

import com.example.qa.page.QaPageObject;
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

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;
import static org.automation.support.ProfileBounds.since;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code defaults} — vue de profil du PageObject : {@code QaPageObject.web(SyncTimeout)} renvoie une
 * vue <b>distincte</b>, et l'utiliser ne modifie pas {@code web} (profil MEDIUM). Profils aux défauts du
 * socle : SHORT 5 s (marge 2 s), MEDIUM 15 s (marge 5 s). Fixture {@code states.html}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ProfileViewDefaultTest {

    private static final By ABSENT = By.id("absent");

    /** PageObject sonde : expose la synchronisation héritée de {@code QaPageObject} pour l'observer. */
    static final class ProbePage extends QaPageObject {

        WebSync defaultSync() {
            return web;
        }

        WebSync profileSync(SyncTimeout profile) {
            return web(profile);
        }
    }

    // ---- T77 : vue de profil distincte, sans effet sur web ----
    @Test
    @DisplayName("T77 - NON PASS - web(SHORT) : vue distincte qui expire a 5s ; web reste MEDIUM et expire a 15s ensuite")
    void T77_web_isolated() {
        getDriver().get(Fixtures.url("states.html"));
        Duration shortTimeout = Timeouts.defaultOf(SyncTimeout.SHORT);
        Duration mediumTimeout = Timeouts.defaultOf(SyncTimeout.MEDIUM);
        ProbePage page = new ProbePage();

        WebSync shortView = page.profileSync(SyncTimeout.SHORT);
        assertNotSame(page.defaultSync(), shortView, "T77 : web(SHORT) doit rendre une vue distincte de web");
        assertNotSame(shortView, page.profileSync(SyncTimeout.SHORT), "T77 : chaque appel a web(SHORT) rend une vue neuve");

        long start = System.nanoTime();
        SyncException exShort = assertThrows(SyncException.class, () -> shortView.click(ABSENT), "T77 : vue SHORT sur absent");
        long msShort = since(start);
        assertTrue(exShort.getMessage().contains("[ABSENT]"), "T77 : code [ABSENT] attendu, message=" + exShort.getMessage());
        assertTrue(msShort >= shortTimeout.toMillis() && msShort < shortTimeout.plus(Timeouts.SHORT_MARGIN).toMillis(),
                "T77 : la vue doit expirer au profil SHORT (" + shortTimeout.toMillis() + " ms), ms=" + msShort);

        WebSync defaultView = page.defaultSync();
        start = System.nanoTime();
        SyncException exDefault = assertThrows(SyncException.class, () -> defaultView.click(ABSENT),
                "T77 : web sur absent, apres usage de la vue");
        long msDefault = since(start);
        assertTrue(exDefault.getMessage().contains("[ABSENT]"), "T77 : code [ABSENT] attendu, message=" + exDefault.getMessage());
        assertTrue(msDefault >= mediumTimeout.toMillis() && msDefault < mediumTimeout.plus(Timeouts.MARGIN).toMillis(),
                "T77 : web doit rester au profil MEDIUM (" + mediumTimeout.toMillis() + " ms) apres usage de la vue, ms=" + msDefault);

        System.out.println("[T77] RESULTAT : exception attendue=SyncException [ABSENT] via web(SHORT) | levee=OUI | type reel="
                + exShort.getClass().getName() + " | en " + msShort + " ms (SHORT " + shortTimeout.toMillis() + ") | message=\""
                + exShort.getMessage() + "\" => cas non passant confirme");
        System.out.println("[T77] RESULTAT : exception attendue=SyncException [ABSENT] via web apres la vue | levee=OUI | type reel="
                + exDefault.getClass().getName() + " | en " + msDefault + " ms (MEDIUM " + mediumTimeout.toMillis()
                + ", inchange) | message=\"" + exDefault.getMessage() + "\" => cas non passant confirme");
    }
}
