package org.automation.steps.websync;

import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Étapes des vues {@code within} <b>imbriquées</b> : la chaîne de conteneurs est rapportée en entier dans le titre
 * de l'étape, du plus externe au plus interne.
 */
public class WithinSteps extends WebSyncSteps {

    @Step("Lire le texte de {1} à travers les conteneurs {0}")
    public String getText(List<By> containers, By target) {
        return view(containers).getText(target);
    }

    @Step("Compter les éléments {1} à travers les conteneurs {0}")
    public int count(List<By> containers, By target) {
        return view(containers).count(target);
    }

    @Step("Cliquer sur {1} à travers les conteneurs {0}")
    public void click(List<By> containers, By target) {
        view(containers).click(target);
    }

    /** Vue bornée par la chaîne de conteneurs : un {@code within} par maillon, ré-résolu à chaque appel. */
    private WebSync view(List<By> containers) {
        WebSync scoped = web;
        for (By container : containers) {
            scoped = scoped.within(container);
        }
        return scoped;
    }
}
