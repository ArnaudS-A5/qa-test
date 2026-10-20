package org.automation.steps.websync;

import com.example.qa.sync.SyncTimeout;
import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Step;
import org.automation.support.Fixtures;

import static net.serenitybdd.core.Serenity.getDriver;

/**
 * Socle commun des étapes de la surface {@code WebSync} : ouverture d'une fixture et profil de synchronisation.
 *
 * <p>Une étape = <b>un appel au socle</b>, rapporté par Serenity avec ses paramètres et sa capture d'écran
 * ({@code serenity.take.screenshots = AFTER_EACH_STEP}). Les étapes tiennent directement un {@code WebSync} : sur la
 * surface du socle, un PageObject ne ferait que relayer l'appel (le chaînage PageObject → étape est couvert par la
 * PHASE 2 et par T77).
 *
 * <p>Écrites « attendu passant » (convention 1) : l'appel dont on attend l'échec reste dans le test, hors étape
 * ({@code assertThrows}). Une étape n'attrape jamais d'exception.
 */
public abstract class WebSyncSteps {

    /** Profil MEDIUM par défaut : un appel passant aboutit dès que l'élément est prêt, sans dépendre du profil. */
    protected WebSync web = new WebSync();

    /** Change le profil des étapes suivantes. Réglage, pas une action : non rapporté. */
    public void withProfile(SyncTimeout profile) {
        web = new WebSync(profile);
    }

    @Step("Ouvrir la fixture {0}")
    public void open(String fixture) {
        getDriver().get(Fixtures.url(fixture));
    }

    /** Fixture paramétrée : {@code query} sans le {@code ?} (ex. {@code "delay=0"}). */
    @Step("Ouvrir la fixture {0} avec ?{1}")
    public void open(String fixture, String query) {
        getDriver().get(Fixtures.url(fixture, query));
    }
}
