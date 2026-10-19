package org.automation.support;

import org.openqa.selenium.By;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;

/**
 * Outillage de la fixture {@code websync/transitions.html} : un seul élément {@code #cible}, qui change
 * d'état après un délai selon le {@link Scenario}. Le minuteur démarre au chargement de la page.
 *
 * <p>Volontairement <b>sans assertion</b> : chaque test porte ses propres vérifications, visibles.
 */
public final class Transitions {

    public static final By CIBLE = By.id("cible");
    public static final By STATUT = By.id("statut");

    /** Changement d'état appliqué à {@code #cible} après le délai. */
    public enum Scenario {
        /** {@code #cible} inséré visible. */
        APPARITION("apparition"),
        /** {@code #cible} inséré en {@code display:none} : présent, jamais visible. */
        APPARITION_CACHEE("apparition-cachee"),
        /** {@code #cible} visible et désactivé dès le chargement, activé après le délai. */
        ACTIVATION("activation"),
        /** {@code #cible} visible et actif dès le chargement, désactivé après le délai. */
        DESACTIVATION("desactivation"),
        /** {@code #cible} visible dès le chargement, retiré du DOM après le délai. */
        DISPARITION("disparition");

        private final String parametre;

        Scenario(String parametre) {
            this.parametre = parametre;
        }
    }

    private Transitions() {
    }

    /** Ouvre la fixture avec la transition {@code scenario} après {@code delai} ; renvoie ce délai en ms. */
    public static long ouvrir(Scenario scenario, Duration delai) {
        long ms = delai.toMillis();
        getDriver().get(Fixtures.url("websync/transitions.html", "scenario=" + scenario.parametre + "&delai=" + ms));
        return ms;
    }
}
