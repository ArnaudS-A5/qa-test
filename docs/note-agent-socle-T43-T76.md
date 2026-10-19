# Note pour l'agent du socle — corriger deux écarts révélés par `qa-test` (T43, T76)

**Destinataire** : agent IA du dépôt `qa-template` (module `qa-socle`), branche `feature/package-by-feature`.
**Émetteur** : projet consommateur `qa-test`, qui valide le socle au navigateur réel.
**Date** : 2026-10-18. **Socle observé** : commit `fa9f6e9`. Serenity 4.2.22, Selenium 4.25.0, Java 17.

> **Suite (2026-10-19)** : les deux écarts sont **corrigés** par le socle — écart 1 par `8736508`
> (« exécute count et getTexts sans implicit wait », test `sync/CollectionsTest`), écart 2 par `1dda2d7`
> (option A, « résout le locator de setWindowFocus avant le focus fenêtre », test `sync/SetWindowFocusTest`).
> T43 et T76 sont verts sur Chrome et Firefox (campagne `qa-test` du 2026-10-18). Note conservée telle que
> transmise.

## Ce qu'on attend de toi

Corriger dans le socle les deux défauts ci-dessous, dont la cause est entièrement côté socle. Les tests
`qa-test` qui les révèlent (T43, T76) font foi et **ne seront pas modifiés** : ils doivent passer au vert
avec le socle corrigé, sur Chrome **et** Firefox. Tout le reste de la campagne `qa-test` est vert.

Contraintes du socle à respecter :
- **une seule horloge** (point critique C8) : aucune attente supplémentaire hors de la boucle `poll` ;
- **contrat no-leak** : aucun `WebElement` conservé ni renvoyé ;
- **API publique gelée** : ne pas changer une signature publique sans décision explicite (cf. écart 2).

---

## Écart 1 — `count` / `getTexts` / `within(…)` subissent l'implicit wait sur un élément absent (T43)

**Contrat documenté** (`AbstractSyncManager`) :
- `count(By)` : « Nombre d'éléments correspondant au locator — état courant, **sans attente**. »
- `findAll(By)` : « État courant, **sans attente** : container introuvable → liste vide (jamais d'exception). »

**Constat** (`qa-test`, `CollectionsTest#T43_absent_sans_exception`, Chrome et Firefox, Java 17) :
`count(By.id("absent"))` rend bien `0`, mais en **3 030 à 4 533 ms** au lieu de quelques millisecondes. Le
test exige moins d'1 s. Par comparaison, les prédicats (`isDisabled`, `hasClass`, `containsElements`…) rendent
`false` sur absent en **10 à 120 ms** (T47).

**Cause** :
- `AbstractSyncManager.findAll` (l. 919), qui sert `count` (l. 906) et `getTexts` (l. 911), appelle
  `resolveScope(driver).findElements(locator)` **hors** de `withoutImplicitWait` (l. 295).
- L'implicit wait de Serenity (`webdriver.timeouts.implicitlywait = 3000` dans le `serenity.conf` du
  consommateur, porté par le `WebDriverFacade`) s'applique donc :
  - `findElements` attend jusqu'à 3 s qu'au moins un élément apparaisse ;
  - pour une vue `within`, `resolveScope` (l. 350) fait d'abord un `findElement(container)`, lui aussi soumis
    à cette attente, avant que l'absence soit rattrapée en liste vide.
- `query` (l. 382), lui, est bien enveloppé : c'est pour cela que les prédicats sont immédiats.

**Correctif attendu** : exécuter `findAll` sous la même neutralisation que `query`.

```java
private List<WebElement> findAll(By locator) {
    return withoutImplicitWait(() -> {
        try {
            return resolveScope(driver()).findElements(locator);
        } catch (NoSuchElementException | StaleElementReferenceException containerGone) {
            return List.of();
        }
    });
}
```

**Résultat attendu** :

| Appel | Avant | Après |
|---|---|---|
| `count(absent)`, `getTexts(absent)` | `0` / `[]` en ~3 s | `0` / `[]` en quelques ms |
| `within(absent).count(…)` | `0` en ~3 s | `0` en quelques ms |
| `count` / `getTexts` sur éléments présents, y compris dans `within` (T42) | immédiat | inchangé |

---

## Écart 2 — `setWindowFocus(By)` ne résout jamais son locator : faux succès sur absent (T76)

**Contrat attendu** : comme toute méthode qui prend un `By`, attendre l'élément au profil de l'instance et,
au timeout, lever `SyncException` avec le diagnostic (`[ABSENT]` ici). C'est aussi le principe posé par la
Javadoc de `requireEnabled` : un faux succès est « plus nuisible qu'un échec franc ».

**Constat** (`qa-test`, `FenetreTest#T76_set_window_focus_absent`, Chrome et Firefox, profil SHORT) :
`setWindowFocus(By.id("absent"))` **rend la main sans exception**, en environ 1 s. Sur un élément présent,
`window.focus()` est bien exécuté dans la fenêtre courante : T75 le vérifie par un espion sur `window.focus`.

**Cause** :
- `WebSync.setWindowFocus` (l. 131-136) fait `fluentWait(locator, element -> { element.setWindowFocus(); … })`.
- `resolve(locator)` fournit un `WebElementFacade` **paresseux**.
- `WebElementFacadeImpl.setWindowFocus` (bytecode Serenity 4.2.22) se limite à
  `if (driverIsDisabled()) return; getJavascriptExecutorFacade().executeScript("window.focus()")` : il ne
  touche **jamais** l'élément.
- Aucune `NoSuchElementException` n'est donc levée : `poll` réussit au premier tour, sans attente ni
  diagnostic. Le `By` est ignoré.

**Correctif, décision de conception à prendre** :
- **Option A (recommandée, garde la signature)** : forcer la résolution de l'élément dans la boucle, avant
  `window.focus()`. Un absent lève alors `NoSuchElementException` (classée ABSENT) : retry jusqu'au timeout,
  puis `SyncException [ABSENT]`. Exemple, à valider contre l'API Serenity 4.2.22 :
  ```java
  public void setWindowFocus(By locator) {
      fluentWait(locator, element -> {
          element.getWrappedElement(); // résolution réelle : absent → NoSuchElementException → [ABSENT]
          element.setWindowFocus();
          return null;
      });
  }
  ```
  Ne pas passer par `actRaw` sans y réfléchir : sa garde `requireEnabled` refuserait un élément désactivé,
  ce qui n'a pas de sens pour un focus de fenêtre.
- **Option B** : assumer la sémantique réelle (focus de la fenêtre, indépendant de l'élément) en exposant
  `setWindowFocus()` sans `By` et en dépréciant la variante actuelle. C'est un changement de l'API gelée.

**Résultat attendu (option A)** :

| Cas | Avant | Après |
|---|---|---|
| élément présent (T75) | `window.focus()` appelé dans la fenêtre courante | inchangé |
| élément absent (T76) | retour silencieux | `SyncException [ABSENT]` au timeout, `window.focus()` **non** appelé |

---

## Vérification

1. Dans le socle : tests unitaires (`./mvnw clean test`), puis publication locale avec Java 17
   (`./mvnw clean install`). Si l'outillage le permet (driver simulé), ajouter un test unitaire par écart,
   dans `qa-socle/src/test/java/com/example/qa/sync/`.
2. Dans `qa-test`, avec Java 17 : `mvn test -Dtest='CollectionsTest,FenetreTest'`, puis la même chose avec
   `-Dwebdriver.driver=firefox`. Attendu : **5 tests verts sur 5** dans chaque navigateur (T42 à T44, T75, T76).
3. Puis la campagne complète `qa-test` (`mvn clean verify`, Chrome puis Firefox) : plus aucun rouge.

## Pour aller plus loin (hors de cette correction)

Les autres retours du consommateur sont décrits dans `qa-test/docs/qa-socle-suggestions.md` : dump HTML (§1),
nom de fichier (§2), translittération des accents (§3), faux positif `[OUT_OF_BOUNDS]` sur `display:none`
(§4). Les deux écarts de cette note y figurent aussi (§5 et §6).
