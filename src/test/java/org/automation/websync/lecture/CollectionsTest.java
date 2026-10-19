package org.automation.websync.lecture;

import com.example.qa.sync.WebSync;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.automation.steps.websync.EtatSteps;
import org.automation.steps.websync.LectureSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface {@code WebSync} — lectures de <b>collections</b> (fixture {@code websync/collection.html}) :
 * {@code count}, {@code getTexts}, {@code containsElements}, seules et dans une vue {@code within}.
 *
 * <p>Contrat du socle : état courant, <b>sans attente</b> ; un élément ou un conteneur absent donne 0, une
 * liste vide ou {@code false}, jamais une exception. Les lectures chronométrées (T43) restent hors étape.
 */
@ExtendWith(SerenityJUnit5Extension.class)
class CollectionsTest {

    private static final String FIXTURE = "websync/collection.html";
    private static final By LISTE = By.id("liste");
    private static final By LI = By.tagName("li");
    private static final By ITEM = By.cssSelector(".item");
    private static final By CONTENEUR = By.id("conteneur");
    private static final By VIDE = By.id("vide");
    private static final By ABSENT = By.id("absent");

    /** Sans attente : une lecture sur absent se mesure en millisecondes (l'implicit wait, lui, tiendrait 3 s). */
    private static final long LECTURE_IMMEDIATE_MS = 1_000;

    @Steps
    LectureSteps lecture;

    @Steps
    EtatSteps etat;

    // ---- T42 : compter et lire plusieurs elements, a la racine puis dans une vue within ----
    @Test
    @DisplayName("T42 - count et getTexts : 3 elements Un/Deux/Trois, a la racine et dans within(#liste)")
    void T42_compter_et_lire() {
        lecture.ouvrir(FIXTURE);

        assertEquals(3, lecture.compter(LI), "T42 : count a la racine");
        List<String> textes = lecture.textes(LI);
        assertEquals(List.of("Un", "Deux", "Trois"), textes, "T42 : getTexts a la racine, dans l'ordre du DOM");
        assertEquals(3, lecture.compterDans(LISTE, LI), "T42 : count dans within(#liste)");
        assertEquals(textes, lecture.textesDans(LISTE, LI), "T42 : getTexts dans within(#liste)");

        System.out.println("[T42] RESULTAT : count=3 | getTexts=" + textes + " | identiques dans within(#liste)"
                + " | aucune exception => cas passant confirme");
    }

    // ---- T43 : element ou conteneur absent => 0 / liste vide, immediatement, sans exception ----
    @Test
    @DisplayName("T43 - count et getTexts sur absent (element ou conteneur within) : 0 et liste vide, immediats")
    void T43_absent_sans_exception() {
        lecture.ouvrir(FIXTURE);
        WebSync web = new WebSync();

        long start = System.nanoTime();
        int nombre = web.count(ABSENT);
        long msCount = (System.nanoTime() - start) / 1_000_000;
        assertEquals(0, nombre, "T43 : count sur absent = 0");
        assertTrue(msCount < LECTURE_IMMEDIATE_MS, "T43 : count sur absent doit etre immediat, ms=" + msCount);

        start = System.nanoTime();
        List<String> textes = web.getTexts(ABSENT);
        long msTexts = (System.nanoTime() - start) / 1_000_000;
        assertEquals(List.of(), textes, "T43 : getTexts sur absent = liste vide");
        assertTrue(msTexts < LECTURE_IMMEDIATE_MS, "T43 : getTexts sur absent doit etre immediat, ms=" + msTexts);

        start = System.nanoTime();
        int dansAbsent = web.within(ABSENT).count(LI);
        long msWithin = (System.nanoTime() - start) / 1_000_000;
        assertEquals(0, dansAbsent, "T43 : count dans un conteneur within absent = 0, sans exception");
        assertTrue(msWithin < LECTURE_IMMEDIATE_MS, "T43 : count dans un conteneur absent doit etre immediat, ms=" + msWithin);

        System.out.println("[T43] RESULTAT : count=0 (" + msCount + " ms) | getTexts=[] (" + msTexts + " ms)"
                + " | within(absent).count=0 (" + msWithin + " ms) | aucune exception => cas passant confirme");
    }

    // ---- T44 : un conteneur contient-il des elements ----
    @Test
    @DisplayName("T44 - containsElements : vrai sur #conteneur, faux sur #vide et sur un conteneur absent")
    void T44_contient_elements() {
        lecture.ouvrir(FIXTURE);

        assertTrue(etat.contientElements(CONTENEUR, ITEM), "T44 : #conteneur contient des .item");
        assertFalse(etat.contientElements(VIDE, ITEM), "T44 : #vide ne contient aucun .item");
        assertFalse(etat.contientElements(ABSENT, ITEM), "T44 : conteneur absent => faux, sans exception");

        System.out.println("[T44] RESULTAT : #conteneur=vrai | #vide=faux | absent=faux"
                + " | aucune exception => cas passant confirme");
    }
}
