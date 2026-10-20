package org.automation.websync.read;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.ActionSteps;
import org.automation.steps.websync.StateSteps;
import org.automation.steps.websync.ReadSteps;
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
 * Surface {@code WebSync} — <b>lectures</b> DOM (fixture {@code websync/read/attributes.html}) : texte visible ou
 * brut, attribut contre propriété, style, classes, accessibilité, géométrie et capture d'élément.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class ReadDomTest {

    private static final String FIXTURE = "websync/read/attributes.html";
    private static final By MIXED = By.id("mixte");
    private static final By FIELD = By.id("champ");
    private static final By ALERT = By.id("alerte");
    private static final By CLOSE = By.id("fermer");
    private static final By NAVIGATION = By.id("navigation");
    private static final By EMAIL = By.id("email");
    private static final By BOX = By.id("boite");
    private static final By ABSENT = By.id("absent");

    /** Géométrie de #boite, fixée en CSS par la fixture (position absolue, sans bordure ni marge). */
    private static final Rectangle EXPECTED_BOX = new Rectangle(500, 150, 40, 150);

    @Steps
    ActionSteps actions;

    @Steps
    ReadSteps read;

    @Steps
    StateSteps state;

    // ---- T37 : texte visible contre texte brut ----
    @Test
    @DisplayName("T37 - PASS - getText exclut le texte masque, getTextContent l'inclut, getTextValue suit la nature de l'element")
    void T37_visible_text() {
        read.open(FIXTURE);

        String visible = read.getText(MIXED);
        String raw = read.getTextContent(MIXED);
        assertEquals("Texte visible", visible, "T37 : getText ne doit rendre que le texte visible");
        assertEquals("Texte visible et masque", raw, "T37 : getTextContent doit inclure le texte masque");
        assertEquals("Texte visible", read.getTextValue(MIXED), "T37 : getTextValue d'un paragraphe = son texte");
        assertEquals("initial", read.getTextValue(FIELD), "T37 : getTextValue d'un champ = sa valeur");

        System.out.println("[T37] RESULTAT : getText=\"" + visible + "\" | getTextContent=\"" + raw
                + "\" | getTextValue paragraphe/champ conformes | aucune exception => cas passant confirme");
    }

    // ---- T38 : attribut HTML contre propriete DOM ----
    @Test
    @DisplayName("T38 - PASS - apres saisie : getDomAttribute garde la valeur initiale, getDomProperty et getAttribute la valeur courante")
    void T38_attribute() {
        read.open(FIXTURE);

        actions.type(FIELD, "modifie");
        String attribute = read.getDomAttribute(FIELD, "value");
        String property = read.getDomProperty(FIELD, "value");
        String resolved = read.getAttribute(FIELD, "value");
        assertEquals("initial", attribute, "T38 : getDomAttribute lit l'attribut HTML, inchange par la saisie");
        assertEquals("modifie", property, "T38 : getDomProperty lit la propriete DOM, mise a jour par la saisie");
        assertEquals("modifie", resolved, "T38 : getAttribute resout la propriete quand elle existe");
        assertEquals("input", read.getTagName(FIELD), "T38 : getTagName");

        System.out.println("[T38] RESULTAT : attribut=" + attribute + " | propriete=" + property
                + " | getAttribute=" + resolved + " | balise=input | aucune exception => cas passant confirme");
    }

    // ---- T39 : style calcule et classes CSS ----
    @Test
    @DisplayName("T39 - PASS - getCssValue (couleur normalisee) et hasClass, faux sans exception sur element absent")
    void T39_style() {
        read.open(FIXTURE);

        // Chrome rend rgba(...), Firefox rgb(...) : on compare la couleur, pas son format.
        String color = read.getCssValue(ALERT, "color");
        assertEquals(Color.fromString("rgb(200, 30, 30)"), Color.fromString(color), "T39 : couleur calculee de #alerte, recu=" + color);
        assertTrue(state.hasClass(ALERT, "alerte"), "T39 : hasClass vrai sur une classe portee");
        assertTrue(state.hasClass(ALERT, "important"), "T39 : hasClass vrai sur la seconde classe portee");
        assertFalse(state.hasClass(ALERT, "absente"), "T39 : hasClass faux sur une classe non portee");
        assertFalse(state.hasClass(ABSENT, "alerte"), "T39 : hasClass faux, sans exception, sur un element absent");

        System.out.println("[T39] RESULTAT : color=" + color + " | hasClass alerte/important=vrai, absente=faux,"
                + " element absent=faux | aucune exception => cas passant confirme");
    }

    // ---- T40 : accessibilite (ARIA) ----
    @Test
    @DisplayName("T40 - PASS - getAriaLabel, getAriaRole et getAccessibleName exacts")
    void T40_aria() {
        read.open(FIXTURE);

        String label = read.getAriaLabel(CLOSE);
        String role = read.getAriaRole(NAVIGATION);
        String fieldName = read.getAccessibleName(EMAIL);
        assertEquals("Fermer la fenetre", label, "T40 : getAriaLabel lit aria-label");
        assertEquals("navigation", role, "T40 : getAriaRole rend le role calcule");
        assertEquals("Adresse e-mail", fieldName, "T40 : getAccessibleName d'un champ = son <label for>");
        assertEquals("Fermer la fenetre", read.getAccessibleName(CLOSE),
                "T40 : getAccessibleName d'un bouton = son aria-label (prime sur le texte 'X')");

        System.out.println("[T40] RESULTAT : aria-label=" + label + " | role=" + role + " | nom accessible champ="
                + fieldName + " | aucune exception => cas passant confirme");
    }

    // ---- T41 : geometrie et capture d'element ----
    @Test
    @DisplayName("T41 - PASS - getLocation, getSize, getRect exacts ; getScreenshotAs rend un PNG aux proportions de l'element")
    void T41_geometry() throws IOException {
        read.open(FIXTURE);

        Point position = read.getLocation(BOX);
        Dimension size = read.getSize(BOX);
        Rectangle rect = read.getRect(BOX);
        assertEquals(EXPECTED_BOX.getPoint(), position, "T41 : getLocation");
        assertEquals(EXPECTED_BOX.getDimension(), size, "T41 : getSize");
        assertEquals(EXPECTED_BOX, rect, "T41 : getRect = position + taille");

        byte[] png = read.getScreenshotAs(BOX);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image, "T41 : la capture doit etre une image lisible");
        assertTrue(png[0] == (byte) 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G', "T41 : la capture doit etre un PNG");
        // Les pixels reels dependent de la densite de l'ecran (x2 sur Retina) : on compare les proportions.
        double ratio = (double) image.getWidth() / image.getHeight();
        double expectedRatio = (double) size.getWidth() / size.getHeight();
        assertTrue(Math.abs(ratio - expectedRatio) < 0.15,
                "T41 : proportions de la capture " + image.getWidth() + "x" + image.getHeight() + " attendues " + expectedRatio);

        System.out.println("[T41] RESULTAT : position=" + position + " | taille=" + size + " | capture PNG "
                + image.getWidth() + "x" + image.getHeight() + " (ratio " + String.format("%.2f", ratio)
                + ") | aucune exception => cas passant confirme");
    }
}
