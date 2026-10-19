package org.automation.support;

import com.example.qa.sync.SyncTimeout;
import net.serenitybdd.model.environment.EnvironmentSpecificConfiguration;
import net.thucydides.model.environment.SystemEnvironmentVariables;
import net.thucydides.model.util.EnvironmentVariables;

import java.time.Duration;
import java.util.Optional;

/**
 * Timeouts réellement appliqués, pour que les tests aux limites <b>déduisent</b> leurs délais
 * (cible à timeout ± marge) au lieu de recopier des valeurs.
 *
 * <p><b>Une seule source de vérité</b> : les clés posées par le {@code pom.xml} (exécution {@code cle})
 * ou, à défaut, le défaut du socle ({@link SyncTimeout#defaultValue()}). Lecture faite comme le socle
 * ({@code serenity.conf} + propriétés système), même format de durée ({@code "15s"}, {@code "500ms"},
 * nombre nu = secondes).
 *
 * <p><b>Garde</b> : un test du groupe {@code cle} lancé sans sa clé, ou un test du groupe {@code defaut}
 * lancé avec une clé, échoue immédiatement avec un message explicite au lieu d'un faux résultat.
 */
public final class Timeouts {

    /** Marge de la règle aux limites : cible à timeout − marge (passant), timeout + marge (non passant). */
    public static final Duration MARGE = Duration.ofSeconds(5);

    /** Marge réduite quand le timeout est trop court pour 5 s (SHORT par défaut = 5 s, implicit wait = 3 s). */
    public static final Duration MARGE_COURTE = Duration.ofSeconds(2);

    static final String PAGE_LOAD_TIMEOUT_KEY = "qa.sync.pageLoadTimeout";
    static final String IMPLICIT_WAIT_KEY = "webdriver.timeouts.implicitlywait";

    private Timeouts() {
    }

    /**
     * Valeur de la clé {@code qa.sync.timeout.<profil>} (groupe {@code cle}).
     *
     * @throws IllegalStateException si la clé est absente, ou égale au défaut (le test ne prouverait
     *                               alors pas que la clé est lue)
     */
    public static Duration configured(SyncTimeout profile) {
        Duration value = config(profile.key()).map(Timeouts::parseDuration).orElseThrow(() ->
                new IllegalStateException("Cle " + profile.key() + " absente : ce test appartient au groupe"
                        + " 'cle' (package sync.cle) et tourne via `mvn verify` (execution cle du pom),"
                        + " ou avec -D" + profile.key() + "=<duree>."));
        if (value.equals(profile.defaultValue())) {
            throw new IllegalStateException("Cle " + profile.key() + "=" + value.toMillis() + " ms egale au"
                    + " defaut du socle : le test ne prouverait pas que la cle est lue.");
        }
        return value;
    }

    /**
     * Défaut du socle pour le profil (groupe {@code defaut}).
     *
     * @throws IllegalStateException si une clé surcharge le profil (le test ne mesurerait pas le défaut)
     */
    public static Duration defaultOf(SyncTimeout profile) {
        Optional<String> override = config(profile.key());
        if (override.isPresent()) {
            throw new IllegalStateException("Cle " + profile.key() + "=" + override.get() + " posee : ce test"
                    + " mesure le DEFAUT du socle et doit tourner sans cle (execution defaut du pom).");
        }
        return profile.defaultValue();
    }

    /**
     * Plafond de chargement de page {@code qa.sync.pageLoadTimeout} (groupe {@code cle}).
     *
     * @throws IllegalStateException si la clé est absente (défaut socle 2 min : test inexploitable)
     */
    public static Duration pageLoadTimeout() {
        return config(PAGE_LOAD_TIMEOUT_KEY).map(Timeouts::parseDuration).orElseThrow(() ->
                new IllegalStateException("Cle " + PAGE_LOAD_TIMEOUT_KEY + " absente : ce test appartient au"
                        + " groupe 'cle' (package sync.cle) et tourne via `mvn verify` (execution cle du pom)."));
    }

    /** Implicit wait natif de {@code serenity.conf} ({@code webdriver.timeouts.implicitlywait}, en ms). */
    public static Duration implicitWait() {
        return config(IMPLICIT_WAIT_KEY).map(v -> Duration.ofMillis(Long.parseLong(v.trim()))).orElseThrow(() ->
                new IllegalStateException(IMPLICIT_WAIT_KEY + " absent de serenity.conf (requis par L1/L2)."));
    }

    private static Optional<String> config(String key) {
        EnvironmentVariables env = SystemEnvironmentVariables.createEnvironmentVariables();
        return EnvironmentSpecificConfiguration.from(env).getOptionalProperty(key);
    }

    /** Même format que le socle : {@code "15s"}, {@code "500ms"}, ou un nombre nu (secondes). */
    static Duration parseDuration(String raw) {
        String value = raw.trim();
        if (value.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2).trim()));
        }
        if (value.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1).trim()));
        }
        return Duration.ofSeconds(Long.parseLong(value));
    }
}
