package org.automation.support;

import java.net.URL;
import java.nio.file.Paths;

/**
 * Utilitaire de résolution des fixtures HTML locales (PHASE 1).
 *
 * <p>Les fixtures vivent sous {@code src/test/resources/fixtures/} et sont donc copiées
 * au build dans le classpath de test ({@code target/test-classes/fixtures/}). On les
 * ouvre en {@code file://} via {@code getDriver().get(Fixtures.url("appear.html", "delay=2800"))},
 * sans aucun serveur en local. En Grid, {@code -Dqa.fixtures.baseUrl=http://.../}
 * permet de servir les mêmes fichiers aux navigateurs distants.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /**
     * Construit une URL {@code file://} vers une fixture du classpath de test.
     *
     * @param name  nom de fichier de la fixture (ex. {@code "appear.html"})
     * @param query éventuels paramètres de requête, sans le {@code ?} (ex. {@code "delay=2800"}),
     *              ou {@code null}/vide pour aucune query
     * @return URL {@code file://...fixtures/<name>[?<query>]}
     */
    public static String url(String name, String query) {
        URL resource = Fixtures.class.getClassLoader().getResource("fixtures/" + name);
        if (resource == null) {
            throw new IllegalStateException(
                    "Fixture introuvable dans le classpath de test : fixtures/" + name
                            + " (vérifier src/test/resources/fixtures/).");
        }
        String base;
        try {
            // Passe par un java.io.File pour un file:// canonique multi-plateforme.
            String remoteBase = System.getProperty("qa.fixtures.baseUrl");
            base = (remoteBase == null || remoteBase.isBlank())
                    ? Paths.get(resource.toURI()).toUri().toString()
                    : remoteBase.replaceAll("/+$", "") + "/" + name;
        } catch (Exception e) {
            throw new IllegalStateException("URL de fixture invalide pour : " + name, e);
        }
        if (query != null && !query.isBlank()) {
            return base + "?" + query;
        }
        return base;
    }

    /** Variante sans query. */
    public static String url(String name) {
        return url(name, null);
    }
}

