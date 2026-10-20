package org.automation.steps.websync;

import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;

import java.util.List;

/** Étapes des listes déroulantes {@code <select>} de la surface {@code WebSync} : sélection, désélection, lectures. */
public class SelectSteps extends WebSyncSteps {

    @Step("Choisir l'option « {1} » de {0}")
    public void selectByVisibleText(By dropdown, String label) {
        web.selectByVisibleText(dropdown, label);
    }

    @Step("Choisir l'option de valeur « {1} » dans {0}")
    public void selectByValue(By dropdown, String value) {
        web.selectByValue(dropdown, value);
    }

    @Step("Choisir l'option n°{1} de {0}")
    public void selectByIndex(By dropdown, int index) {
        web.selectByIndex(dropdown, index);
    }

    @Step("Désélectionner l'option « {1} » de {0}")
    public void deselectByVisibleText(By dropdown, String label) {
        web.deselectByVisibleText(dropdown, label);
    }

    @Step("Désélectionner l'option de valeur « {1} » dans {0}")
    public void deselectByValue(By dropdown, String value) {
        web.deselectByValue(dropdown, value);
    }

    @Step("Désélectionner l'option n°{1} de {0}")
    public void deselectByIndex(By dropdown, int index) {
        web.deselectByIndex(dropdown, index);
    }

    @Step("Tout désélectionner dans {0}")
    public void deselectAll(By dropdown) {
        web.deselectAll(dropdown);
    }

    @Step("Lire les libellés des options de {0}")
    public List<String> getSelectOptions(By dropdown) {
        return web.getSelectOptions(dropdown);
    }

    @Step("Lire les valeurs des options de {0}")
    public List<String> getSelectOptionValues(By dropdown) {
        return web.getSelectOptionValues(dropdown);
    }

    @Step("Lire la valeur de la 1re option choisie dans {0}")
    public String getFirstSelectedOptionValue(By dropdown) {
        return web.getFirstSelectedOptionValue(dropdown);
    }

    @Step("Lire le libellé de la 1re option choisie dans {0}")
    public String getFirstSelectedOptionVisibleText(By dropdown) {
        return web.getFirstSelectedOptionVisibleText(dropdown);
    }

    @Step("Lire les libellés choisis dans {0}")
    public List<String> getSelectedVisibleTexts(By dropdown) {
        return web.getSelectedVisibleTexts(dropdown);
    }

    @Step("Lire les valeurs choisies dans {0}")
    public List<String> getSelectedValues(By dropdown) {
        return web.getSelectedValues(dropdown);
    }

    @Step("Lire si {0} propose l'option « {1} »")
    public boolean containsSelectOption(By dropdown, String label) {
        return web.containsSelectOption(dropdown, label);
    }

    @Step("Vérifier que {0} propose l'option « {1} »")
    public void shouldContainSelectedOption(By dropdown, String label) {
        web.shouldContainSelectedOption(dropdown, label);
    }
}
