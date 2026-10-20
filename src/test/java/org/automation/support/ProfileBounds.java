package org.automation.support;

import org.openqa.selenium.By;

import java.time.Duration;

import static net.serenitybdd.core.Serenity.getDriver;

/**
 * Outillage des tests aux limites d'un profil {@code SyncTimeout} (groupes {@code defaults} et {@code configured}).
 * Fixture {@code appear.html} : {@code #target} inséré après {@code ?delay=N} ms.
 *
 * <p>Règle : cas <b>passant</b> = cible à {@code timeout − marge} (le clic aboutit après avoir réellement
 * attendu) ; cas <b>non passant</b> = cible à {@code timeout + marge} ({@code SyncException [ABSENT]} levée
 * entre le timeout et l'arrivée de la cible, preuve que la borne appliquée est la bonne).
 *
 * <p>Volontairement <b>sans assertion</b> : chaque test porte ses propres vérifications, visibles.
 */
public final class ProfileBounds {

    public static final By TARGET = By.id("target");
    public static final By STATUS = By.id("status");

    /** La minuterie de la fixture démarre au chargement, juste avant la mesure : tolérance basse. */
    public static final long TOLERANCE_MS = 1000;

    private ProfileBounds() {
    }

    /** Ouvre {@code appear.html} avec la cible insérée après {@code delay} ; renvoie ce délai en ms. */
    public static long openTargetAt(Duration delay) {
        long ms = delay.toMillis();
        getDriver().get(Fixtures.url("appear.html", "delay=" + ms));
        return ms;
    }

    /** Millisecondes écoulées depuis {@code startNanos} ({@link System#nanoTime()}). */
    public static long since(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
