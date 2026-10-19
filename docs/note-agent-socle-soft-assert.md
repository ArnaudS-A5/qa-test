# Note pour l'agent du socle — soft-assert : rendre visibles dans Serenity les `should*` en échec

**Destinataire** : agent IA du dépôt `qa-template` (module `qa-socle`), branche `feature/package-by-feature`.
**Émetteur** : projet consommateur `qa-test`, qui valide le socle au navigateur réel.
**Date** : 2026-10-18. **Socle observé** : commit `5729331`. Serenity 4.2.22, Selenium 4.25.0, Java 17.
**Écart correspondant** : `qa-test/docs/qa-socle-suggestions.md`, section « 7. Soft-assert : un `should*` en
échec n'est pas signalé à Serenity ».

> **Suite (2026-10-19)** : **corrigé** par le socle (`81f66d5`, `da57c34`). Le « T83 » annoncé ci-dessous n'a
> pas été créé : c'est **T82** (`qa-test/src/test/java/org/automation/websync/etat/soft/SoftAssertionsTest.java`)
> qui a été refondu pour vérifier ce comportement, avec 16 `should*` (13 KO, 3 OK, le dernier OK) au lieu de 8.
> Critères 1, 2, 3, 5 et 6 vérifiés sur Chrome et Firefox. Critère 4 : le vote de fin lève bien l'agrégat des
> 13 échecs dans l'ordre ; le message de Serenity, lui, compte 14 entrées (les 13 échecs, plus l'agrégat qui les
> reprend), laissé en l'état. Détail : section « 7 », sous-section « Suite », de `qa-socle-suggestions.md`.
> Note conservée telle que transmise.

## Ce qu'on attend de toi

1. **Analyser** le défaut décrit ci-dessous (les faits sont mesurés, les pistes sont à valider).
2. **Corriger l'implémentation** du soft-assert pour obtenir le comportement attendu (section « Comportement
   attendu »), qui est celui que vérifiera le test `qa-test` **T83** (test manuel `@Disabled`, rouge à dessein).
3. **Mettre à jour la décision D22-bis** (`qa-template/qa-socle/docs/decisions.md`, section « D22-bis —
   Soft-assert : extension Jupiter (pas le listener) + validation du point de lecture (C1/C2) », paragraphe
   « Solutions existantes examinées ») avec le fait mesuré sur `enableSoftAsserts()` (cf. « Mesures », mode B).

Contraintes à respecter :
- **aucun échec perdu** : le verdict final reste FAILED si au moins un `should*` a échoué (contrat D22) ;
- **le mode dur ne change pas** (`qa.assertions.soft=false`) ;
- un `should*` appelé **hors étape** (corps du test, PageObject) reste collecté comme aujourd'hui ;
- état confiné au **thread** du test (exécution parallèle, PHASE 3 de `qa-test`) ;
- **API publique gelée** ; « zéro ligne côté test » (activation par la seule clé `qa.assertions.soft`).

## Le défaut

En mode soft, `SoftAssertions.check` (`qa-socle/src/main/java/com/example/qa/sync/SoftAssertions.java`,
méthode `check`) intercepte l'`AssertionError` d'un `should*` et la range dans le collecteur `ThreadLocal`,
**sans rien signaler à Serenity**. L'étape `@Step` qui contient ce `should*` se termine donc normalement et
s'affiche **verte**. En fin de test, `SoftAssertionExtension` (`…/sync/SoftAssertionExtension.java`,
`afterTestExecution`) lève l'agrégat : Serenity l'impute à la **dernière** étape exécutée.

Conséquence pour l'utilisateur du rapport : il ne voit pas **quelles** vérifications ont échoué (étapes vertes),
il voit une étape rouge qui peut avoir réussi (la dernière), et il n'a aucun dump `{step}.html` des échecs soft.
C'est l'inverse du but d'un soft-assert : voir d'un coup d'œil toutes les vérifications fausses d'un test.

## Mesures (sonde `qa-test` du 2026-10-18, Chrome, Java 17)

Même suite dans chaque mode : 4 `should*` alternés, **chacun dans une `@Step`** :
`shouldBeVisible(#visible)` OK → `shouldBeVisible(#cache)` KO → `shouldBeEnabled(#visible)` OK →
`shouldBePresent(#absent)` KO (fixture `qa-test/src/test/resources/fixtures/websync/etats.html`, profil SHORT).

| Mode | Étapes dans le rapport Serenity | Verdict JUnit / Serenity | Artefacts `KO__` |
|---|---|---|---|
| **A** — soft du socle (`qa.assertions.soft=true`) | 4 exécutées ; « #cache visible » (KO) **verte** ; « #absent présent » (dernière) **rouge**, porteuse de l'agrégat | FAILED, message « 2 assertion(s) souple(s) en échec : 1. … #cache 2. … #absent » | `ERROR.log`, `FAIL.log`, **pas** de `{step}.html` |
| **B** — soft natif Serenity (`StepEventBus.getEventBus().enableSoftAsserts()`, `should*` durs) | 4 exécutées, **toutes vertes** | **VERT** : les 2 échecs sont **perdus** | aucun |
| **C** — Serenity sans mode soft (`should*` durs) | 1re KO rouge, les 2 suivantes **SKIPPED** | FAILED sur le 1er échec | `ERROR.log`, `FAIL.log`, `Verifier_que_By.id_cache_est_visible.html` |

Lecture :
- **A** est le défaut à corriger.
- **B** écarte le mode natif comme solution de remplacement : il lève bien le saut des étapes
  (`StepInterceptor.shouldSkip` = `aPreviousStepHasFailed() && !isSoftAssert()`), mais les échecs disparaissent.
  D22-bis l'écartait pour une raison incomplète (« ne sert qu'à ne pas suspendre les appels WebDriver ») : sa
  conclusion tient, son motif est à corriger.
- **C** montre ce que Serenity sait déjà faire pour **un** échec : étape rouge, capture, `{step}.html` (par
  `RawDomCaptureListener` au `stepFailed`). C'est ce rendu qu'on veut pour **chaque** échec soft.

Reproduire (dans `qa-test`, avec la base d'étapes `qa-test/src/test/java/org/automation/steps/websync/EtatSteps.java`) :

```java
@ExtendWith(SerenityJUnit5Extension.class)
class SondeSoftTest {
    @Steps EtatSteps etat;

    @Test
    void A_socle_soft() {                        // lancer avec -Dqa.assertions.soft=true
        etat.ouvrir("websync/etats.html");
        etat.avecProfil(SyncTimeout.SHORT);
        etat.verifierVisible(By.id("visible"));  // OK
        etat.verifierVisible(By.id("cache"));    // KO
        etat.verifierActive(By.id("visible"));   // OK
        etat.verifierPresent(By.id("absent"));   // KO
    }
}
```
`mvn test -Dtest='SondeSoftTest#A_socle_soft' -Dqa.assertions.soft=true`, puis lire le rapport Serenity
(`target/site/serenity/index.html`) et `target/qa-results/KO__…/`.

## Comportement attendu (celui que vérifiera T83)

T83 : 8 `should*` en **alternance KO / OK**, chacun dans une `@Step`, la suite **finissant par un OK** (pour
qu'une imputation à la dernière étape se voie), avec `qa.assertions.soft=true`.

| # | Critère | Attendu |
|---|---|---|
| 1 | Exécution | les 8 étapes s'exécutent, aucune SKIPPED |
| 2 | Étapes KO (1, 3, 5, 7) | **FAILURE** (rouges) dans le rapport, chacune avec sa capture |
| 3 | Étapes OK (2, 4, 6, 8) | **SUCCESS** (vertes), y compris la **dernière** |
| 4 | Verdict du test | FAILED (JUnit et Serenity), message agrégé listant **exactement** les 4 échecs, dans l'ordre |
| 5 | Artefacts `KO__` | `ERROR.log`, `FAIL.log` listant les 4 échecs, et **un `{step}.html` par étape en échec** |
| 6 | Mode dur (`soft=false`), même suite | inchangé : 1re étape KO rouge, suivantes SKIPPED, FAILED (comme le mode C) |

## Pistes (à valider, le choix t'appartient)

- **Signaler l'échec à Serenity sans interrompre** : dans la branche soft de `SoftAssertions.check`, quand une
  étape est en cours, notifier l'échec sur l'étape courante (bus d'événements Serenity, `stepFailed` /
  `StepFailure`) en plus de la collecte, sans que les étapes suivantes soient sautées pour autant (le test C
  montre que Serenity saute les étapes après un échec). À vérifier : effet sur `shouldSkip`, captures,
  `RawDomCaptureListener`, et absence de double comptage avec l'agrégat final.
- **Agrégat final** : éviter que `SoftAssertionExtension` ne fasse imputer l'agrégat à la dernière étape
  (échec rattaché au test, pas à une étape).
- **Hors étape** : conserver la collecte actuelle (rien à marquer dans le rapport).

## Vérification

1. Dans le socle : tests unitaires (`./mvnw clean test`), puis publication locale en Java 17
   (`./mvnw clean install`).
2. Dans `qa-test` : la sonde ci-dessus doit donner « #cache visible » **rouge** et « #absent présent » rouge,
   les autres vertes, verdict FAILED avec 2 échecs ; puis T83 (une fois écrit) doit satisfaire les 6 critères.
3. Non-régression : campagne `qa-test` complète, Chrome puis Firefox (`qa-test/README.md`, « Lancer les
   tests », §3) : T82 (`qa-test/src/test/java/org/automation/websync/etat/soft/SoftAssertionsTest.java`)
   et les `should*` durs (T16, T34, T48 à T58) restent verts.
