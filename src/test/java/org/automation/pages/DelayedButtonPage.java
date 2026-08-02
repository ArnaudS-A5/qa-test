package org.automation.pages;

import com.example.qa.api.page.QaPageObject;
import com.example.qa.api.sync.SyncTimeout;
import net.serenitybdd.annotations.DefaultUrl;
import org.openqa.selenium.By;

/**
 * Page Object dédié à la validation du <b>profil {@link SyncTimeout}</b> passé à {@code WebSync}.
 *
 * <p>Sur une page réelle, un bouton {@code #delayed} est injecté après un délai contrôlé : cela
 * permet d'éprouver le budget d'attente du socle contre un vrai navigateur (pas de fixture locale).
 * Toutes les interactions passent par le {@code WebSync} du socle — champ {@code web} et méthode
 * {@code web(SyncTimeout)} <b>hérités de {@link QaPageObject}</b> (aucun {@code new WebSync(...)} ici).
 */
@DefaultUrl("/")
public class DelayedButtonPage extends QaPageObject {

    private static final By DELAYED = By.id("delayed");

    /** Programme l'apparition du bouton {@code #delayed} après {@code seconds} secondes (JS, page réelle). */
    public void scheduleDelayedButton(int seconds) {
        evaluateJavascript(
                "var ms = arguments[0] * 1000;"
                        + "setTimeout(function () {"
                        + "  var b = document.createElement('button');"
                        + "  b.id = 'delayed'; b.textContent = 'delayed';"
                        + "  document.body.appendChild(b);"
                        + "}, ms);",
                seconds);
    }

    /**
     * Attend {@code #delayed} avec le profil demandé, via {@code web(profil)} du socle.
     * Lève {@code SyncException} si le budget du profil est dépassé.
     */
    public void waitForDelayedButton(SyncTimeout profile) {
        web(profile).waitUntilVisible(DELAYED);
    }

    public boolean isDelayedButtonVisible() {
        return web.isVisible(DELAYED);
    }
}
