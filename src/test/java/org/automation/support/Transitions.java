package org.automation.support;

import org.openqa.selenium.By;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;

/**
 * Outillage de la fixture {@code transitions.html} : un seul élément {@code #cible}, qui change
 * d'état après un délai selon le {@link Scenario}. Le minuteur démarre au chargement de la page.
 *
 * <p>Volontairement <b>sans assertion</b> : chaque test porte ses propres vérifications, visibles.
 */
public final class Transitions {

    public static final By TARGET = By.id("cible");
    public static final By STATUS = By.id("statut");

    /** Changement d'état appliqué à {@code #cible} après le délai. */
    public enum Scenario {
        /** {@code #cible} inséré visible. */
        APPEAR("apparition"),
        /** {@code #cible} inséré en {@code display:none} : présent, jamais visible. */
        APPEAR_HIDDEN("apparition-cachee"),
        /** {@code #cible} visible et désactivé dès le chargement, activé après le délai. */
        ENABLE("activation"),
        /** {@code #cible} visible et actif dès le chargement, désactivé après le délai. */
        DISABLE("desactivation"),
        /** {@code #cible} visible dès le chargement, retiré du DOM après le délai. */
        DISAPPEAR("disparition");

        private final String parameter;

        Scenario(String parameter) {
            this.parameter = parameter;
        }
    }

    private Transitions() {
    }

    /** Ouvre la fixture avec la transition {@code scenario} après {@code delay} ; renvoie ce délai en ms. */
    public static long open(Scenario scenario, Duration delay) {
        long ms = delay.toMillis();
        getDriver().get(Fixtures.url("transitions.html", "scenario=" + scenario.parameter + "&delai=" + ms));
        return ms;
    }
}
