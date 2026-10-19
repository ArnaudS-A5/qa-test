package org.automation.steps.websync;

import net.serenitybdd.annotations.Step;
import org.openqa.selenium.By;

import java.util.List;

/** Étapes des listes déroulantes {@code <select>} de la surface {@code WebSync} : sélection, désélection, lectures. */
public class SelectSteps extends EtapesWebSync {

    @Step("Choisir l'option « {1} » de {0}")
    public void choisirParLibelle(By liste, String libelle) {
        web.selectByVisibleText(liste, libelle);
    }

    @Step("Choisir l'option de valeur « {1} » dans {0}")
    public void choisirParValeur(By liste, String valeur) {
        web.selectByValue(liste, valeur);
    }

    @Step("Choisir l'option n°{1} de {0}")
    public void choisirParIndex(By liste, int index) {
        web.selectByIndex(liste, index);
    }

    @Step("Désélectionner l'option « {1} » de {0}")
    public void deselectionnerParLibelle(By liste, String libelle) {
        web.deselectByVisibleText(liste, libelle);
    }

    @Step("Désélectionner l'option de valeur « {1} » dans {0}")
    public void deselectionnerParValeur(By liste, String valeur) {
        web.deselectByValue(liste, valeur);
    }

    @Step("Désélectionner l'option n°{1} de {0}")
    public void deselectionnerParIndex(By liste, int index) {
        web.deselectByIndex(liste, index);
    }

    @Step("Tout désélectionner dans {0}")
    public void toutDeselectionner(By liste) {
        web.deselectAll(liste);
    }

    @Step("Lire les libellés des options de {0}")
    public List<String> libellesOptions(By liste) {
        return web.getSelectOptions(liste);
    }

    @Step("Lire les valeurs des options de {0}")
    public List<String> valeursOptions(By liste) {
        return web.getSelectOptionValues(liste);
    }

    @Step("Lire la valeur de la 1re option choisie dans {0}")
    public String premiereValeurChoisie(By liste) {
        return web.getFirstSelectedOptionValue(liste);
    }

    @Step("Lire le libellé de la 1re option choisie dans {0}")
    public String premierLibelleChoisi(By liste) {
        return web.getFirstSelectedOptionVisibleText(liste);
    }

    @Step("Lire les libellés choisis dans {0}")
    public List<String> libellesChoisis(By liste) {
        return web.getSelectedVisibleTexts(liste);
    }

    @Step("Lire les valeurs choisies dans {0}")
    public List<String> valeursChoisies(By liste) {
        return web.getSelectedValues(liste);
    }

    @Step("Lire si {0} propose l'option « {1} »")
    public boolean contientOption(By liste, String libelle) {
        return web.containsSelectOption(liste, libelle);
    }

    @Step("Vérifier que {0} propose l'option « {1} »")
    public void verifierOptionProposee(By liste, String libelle) {
        web.shouldContainSelectedOption(liste, libelle);
    }
}
