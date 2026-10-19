package org.automation.websync.lecture;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.EtatSteps;
import org.automation.steps.websync.LectureSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.support.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — <b>lectures</b> DOM (fixture {@code websync/attributs.html}) : texte visible ou
 * brut, attribut contre propriété, style, classes, accessibilité, géométrie et capture d'élément.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class LecturesDomTest {

    private static final String FIXTURE = "websync/attributs.html";
    private static final By MIXTE = By.id("mixte");
    private static final By CHAMP = By.id("champ");
    private static final By ALERTE = By.id("alerte");
    private static final By FERMER = By.id("fermer");
    private static final By NAVIGATION = By.id("navigation");
    private static final By EMAIL = By.id("email");
    private static final By BOITE = By.id("boite");
    private static final By ABSENT = By.id("absent");

    /** Géométrie de #boite, fixée en CSS par la fixture (position absolue, sans bordure ni marge). */
    private static final Rectangle BOITE_ATTENDUE = new Rectangle(500, 150, 40, 150);

    @Steps
    ActionSteps actions;

    @Steps
    LectureSteps lecture;

    @Steps
    EtatSteps etat;

    // ---- T37 : texte visible contre texte brut ----
    @Test
    @DisplayName("T37 - getText exclut le texte masque, getTextContent l'inclut, getTextValue suit la nature de l'element")
    void T37_texte_visible_contre_brut() {
        lecture.ouvrir(FIXTURE);

        String visible = lecture.texte(MIXTE);
        String brut = lecture.texteBrut(MIXTE);
        assertEquals("Texte visible", visible, "T37 : getText ne doit rendre que le texte visible");
        assertEquals("Texte visible et masque", brut, "T37 : getTextContent doit inclure le texte masque");
        assertEquals("Texte visible", lecture.texteEffectif(MIXTE), "T37 : getTextValue d'un paragraphe = son texte");
        assertEquals("initial", lecture.texteEffectif(CHAMP), "T37 : getTextValue d'un champ = sa valeur");

        System.out.println("[T37] RESULTAT : getText=\"" + visible + "\" | getTextContent=\"" + brut
                + "\" | getTextValue paragraphe/champ conformes | aucune exception => cas passant confirme");
    }

    // ---- T38 : attribut HTML contre propriete DOM ----
    @Test
    @DisplayName("T38 - apres saisie : getDomAttribute garde la valeur initiale, getDomProperty et getAttribute la valeur courante")
    void T38_attribut_contre_propriete() {
        lecture.ouvrir(FIXTURE);

        actions.saisir(CHAMP, "modifie");
        String attribut = lecture.attributDom(CHAMP, "value");
        String propriete = lecture.proprieteDom(CHAMP, "value");
        String resolu = lecture.attribut(CHAMP, "value");
        assertEquals("initial", attribut, "T38 : getDomAttribute lit l'attribut HTML, inchange par la saisie");
        assertEquals("modifie", propriete, "T38 : getDomProperty lit la propriete DOM, mise a jour par la saisie");
        assertEquals("modifie", resolu, "T38 : getAttribute resout la propriete quand elle existe");
        assertEquals("input", lecture.balise(CHAMP), "T38 : getTagName");

        System.out.println("[T38] RESULTAT : attribut=" + attribut + " | propriete=" + propriete
                + " | getAttribute=" + resolu + " | balise=input | aucune exception => cas passant confirme");
    }

    // ---- T39 : style calcule et classes CSS ----
    @Test
    @DisplayName("T39 - getCssValue (couleur normalisee) et hasClass, faux sans exception sur element absent")
    void T39_style_et_classes() {
        lecture.ouvrir(FIXTURE);

        // Chrome rend rgba(...), Firefox rgb(...) : on compare la couleur, pas son format.
        String couleur = lecture.css(ALERTE, "color");
        assertEquals(Color.fromString("rgb(200, 30, 30)"), Color.fromString(couleur), "T39 : couleur calculee de #alerte, recu=" + couleur);
        assertTrue(etat.aLaClasse(ALERTE, "alerte"), "T39 : hasClass vrai sur une classe portee");
        assertTrue(etat.aLaClasse(ALERTE, "important"), "T39 : hasClass vrai sur la seconde classe portee");
        assertFalse(etat.aLaClasse(ALERTE, "absente"), "T39 : hasClass faux sur une classe non portee");
        assertFalse(etat.aLaClasse(ABSENT, "alerte"), "T39 : hasClass faux, sans exception, sur un element absent");

        System.out.println("[T39] RESULTAT : color=" + couleur + " | hasClass alerte/important=vrai, absente=faux,"
                + " element absent=faux | aucune exception => cas passant confirme");
    }

    // ---- T40 : accessibilite (ARIA) ----
    @Test
    @DisplayName("T40 - getAriaLabel, getAriaRole et getAccessibleName exacts")
    void T40_accessibilite() {
        lecture.ouvrir(FIXTURE);

        String label = lecture.labelAria(FERMER);
        String role = lecture.roleAria(NAVIGATION);
        String nomChamp = lecture.nomAccessible(EMAIL);
        assertEquals("Fermer la fenetre", label, "T40 : getAriaLabel lit aria-label");
        assertEquals("navigation", role, "T40 : getAriaRole rend le role calcule");
        assertEquals("Adresse e-mail", nomChamp, "T40 : getAccessibleName d'un champ = son <label for>");
        assertEquals("Fermer la fenetre", lecture.nomAccessible(FERMER),
                "T40 : getAccessibleName d'un bouton = son aria-label (prime sur le texte 'X')");

        System.out.println("[T40] RESULTAT : aria-label=" + label + " | role=" + role + " | nom accessible champ="
                + nomChamp + " | aucune exception => cas passant confirme");
    }

    // ---- T41 : geometrie et capture d'element ----
    @Test
    @DisplayName("T41 - getLocation, getSize, getRect exacts ; getScreenshotAs rend un PNG aux proportions de l'element")
    void T41_geometrie_et_capture() throws IOException {
        lecture.ouvrir(FIXTURE);

        Point position = lecture.position(BOITE);
        Dimension taille = lecture.taille(BOITE);
        Rectangle rect = lecture.rectangle(BOITE);
        assertEquals(BOITE_ATTENDUE.getPoint(), position, "T41 : getLocation");
        assertEquals(BOITE_ATTENDUE.getDimension(), taille, "T41 : getSize");
        assertEquals(BOITE_ATTENDUE, rect, "T41 : getRect = position + taille");

        byte[] png = lecture.capture(BOITE);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image, "T41 : la capture doit etre une image lisible");
        assertTrue(png[0] == (byte) 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G', "T41 : la capture doit etre un PNG");
        // Les pixels reels dependent de la densite de l'ecran (x2 sur Retina) : on compare les proportions.
        double ratio = (double) image.getWidth() / image.getHeight();
        double ratioAttendu = (double) taille.getWidth() / taille.getHeight();
        assertTrue(Math.abs(ratio - ratioAttendu) < 0.15,
                "T41 : proportions de la capture " + image.getWidth() + "x" + image.getHeight() + " attendues " + ratioAttendu);

        System.out.println("[T41] RESULTAT : position=" + position + " | taille=" + taille + " | capture PNG "
                + image.getWidth() + "x" + image.getHeight() + " (ratio " + String.format("%.2f", ratio)
                + ") | aucune exception => cas passant confirme");
    }
}
