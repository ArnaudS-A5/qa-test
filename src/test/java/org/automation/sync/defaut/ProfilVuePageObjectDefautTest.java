package org.automation.sync.defaut;

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
import static org.automation.support.BornesProfil.depuis;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Groupe {@code defaut} — vue de profil du PageObject : {@code QaPageObject.web(SyncTimeout)} renvoie une
 * vue <b>distincte</b>, et l'utiliser ne modifie pas {@code web} (profil MEDIUM). Profils aux défauts du
 * socle : SHORT 5 s (marge 2 s), MEDIUM 15 s (marge 5 s). Fixture {@code websync/etats.html}.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ProfilVuePageObjectDefautTest {

    private static final By ABSENT = By.id("absent");

    /** PageObject sonde : expose la synchronisation héritée de {@code QaPageObject} pour l'observer. */
    static final class PageSonde extends QaPageObject {

        WebSync parDefaut() {
            return web;
        }

        WebSync auProfil(SyncTimeout profil) {
            return web(profil);
        }
    }

    // ---- T77 : vue de profil distincte, sans effet sur web ----
    @Test
    @DisplayName("T77 - web(SHORT) : vue distincte qui expire a 5s ; web reste MEDIUM et expire a 15s ensuite")
    void T77_vue_profil_distincte_sans_effet() {
        getDriver().get(Fixtures.url("websync/etats.html"));
        Duration court = Timeouts.defaultOf(SyncTimeout.SHORT);
        Duration moyen = Timeouts.defaultOf(SyncTimeout.MEDIUM);
        PageSonde page = new PageSonde();

        WebSync vueCourte = page.auProfil(SyncTimeout.SHORT);
        assertNotSame(page.parDefaut(), vueCourte, "T77 : web(SHORT) doit rendre une vue distincte de web");
        assertNotSame(vueCourte, page.auProfil(SyncTimeout.SHORT), "T77 : chaque appel a web(SHORT) rend une vue neuve");

        long start = System.nanoTime();
        SyncException exCourte = assertThrows(SyncException.class, () -> vueCourte.click(ABSENT), "T77 : vue SHORT sur absent");
        long msCourte = depuis(start);
        assertTrue(exCourte.getMessage().contains("[ABSENT]"), "T77 : code [ABSENT] attendu, message=" + exCourte.getMessage());
        assertTrue(msCourte >= court.toMillis() && msCourte < court.plus(Timeouts.MARGE_COURTE).toMillis(),
                "T77 : la vue doit expirer au profil SHORT (" + court.toMillis() + " ms), ms=" + msCourte);

        WebSync parDefaut = page.parDefaut();
        start = System.nanoTime();
        SyncException exDefaut = assertThrows(SyncException.class, () -> parDefaut.click(ABSENT),
                "T77 : web sur absent, apres usage de la vue");
        long msDefaut = depuis(start);
        assertTrue(exDefaut.getMessage().contains("[ABSENT]"), "T77 : code [ABSENT] attendu, message=" + exDefaut.getMessage());
        assertTrue(msDefaut >= moyen.toMillis() && msDefaut < moyen.plus(Timeouts.MARGE).toMillis(),
                "T77 : web doit rester au profil MEDIUM (" + moyen.toMillis() + " ms) apres usage de la vue, ms=" + msDefaut);

        System.out.println("[T77] RESULTAT : exception attendue=SyncException [ABSENT] via web(SHORT) | levee=OUI | type reel="
                + exCourte.getClass().getName() + " | en " + msCourte + " ms (SHORT " + court.toMillis() + ") | message=\""
                + exCourte.getMessage() + "\" => cas non passant confirme");
        System.out.println("[T77] RESULTAT : exception attendue=SyncException [ABSENT] via web apres la vue | levee=OUI | type reel="
                + exDefaut.getClass().getName() + " | en " + msDefaut + " ms (MEDIUM " + moyen.toMillis()
                + ", inchange) | message=\"" + exDefaut.getMessage() + "\" => cas non passant confirme");
    }
}
