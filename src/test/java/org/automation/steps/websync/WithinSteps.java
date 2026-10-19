package org.automation.steps.websync;

import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Étapes des vues {@code within} <b>imbriquées</b> : la chaîne de conteneurs est rapportée en entier dans le titre
 * de l'étape, du plus externe au plus interne.
 */
public class WithinSteps extends EtapesWebSync {

    @Step("Lire le texte de {1} à travers les conteneurs {0}")
    public String texteDans(List<By> conteneurs, By cible) {
        return vue(conteneurs).getText(cible);
    }

    @Step("Compter les éléments {1} à travers les conteneurs {0}")
    public int compterDans(List<By> conteneurs, By cible) {
        return vue(conteneurs).count(cible);
    }

    @Step("Cliquer sur {1} à travers les conteneurs {0}")
    public void cliquerDans(List<By> conteneurs, By cible) {
        vue(conteneurs).click(cible);
    }

    /** Vue bornée par la chaîne de conteneurs : un {@code within} par maillon, ré-résolu à chaque appel. */
    private WebSync vue(List<By> conteneurs) {
        WebSync vue = web;
        for (By conteneur : conteneurs) {
            vue = vue.within(conteneur);
        }
        return vue;
    }
}
