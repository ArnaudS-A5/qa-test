package org.automation.steps;

import com.example.qa.sync.SyncTimeout;
import net.serenitybdd.annotations.Step;
import org.automation.pages.DelayedButtonPage;

/**
 * Steps de validation du profil {@link SyncTimeout} : orchestrent {@link DelayedButtonPage}.
 * Les steps sont écrits comme du code <b>normal</b> (attendu passant) ; c'est le TEST qui décide,
 * selon le profil, si l'appel doit aboutir ou lever une {@code SyncException} (attente trop courte).
 */
public class DelayedButtonSteps {

    DelayedButtonPage page;

    @Step("Ouvrir la page et programmer l'apparition de #delayed à {0}s")
    public void openAndScheduleDelayedButton(int seconds) {
        page.open();
        page.scheduleDelayedButton(seconds);
    }

    @Step("Attendre #delayed avec le profil {0}")
    public void waitForDelayedButton(SyncTimeout profile) {
        page.waitForDelayedButton(profile);
    }

    public boolean isDelayedVisible() {
        return page.isDelayedButtonVisible();
    }
}
