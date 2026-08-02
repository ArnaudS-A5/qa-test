# Retour de campagne PHASE 1 — briefing pour `qa-test`

> À transmettre à la conversation IA du projet `qa-test`. Objet : trancher « qui corrige quoi »
> après la campagne PHASE 1, lister les corrections côté test, cadrer le secret.
> Source socle : `qa-template`, branche `develop`, `qa-socle 1.0.0-SNAPSHOT`.

## ✅ État de traitement (2026-08-02) — briefing traité

> - **Côté SOCLE** (L4, L5, T2, T8, T14) : **corrigé côté socle** (`develop`) — moteur SYNC implémenté
>   (profils/overrides appliqués, diagnostic `[ABSENT]/[DISABLED]/[OUT_OF_BOUNDS]` produit). Ces tests
>   **passent** désormais.
> - **Côté TEST** : **T16** → attend `AssertionError` (fait) · **T6** → contact `WebSync` avant la
>   navigation, le socle borne bien (fait) · **T10** → reclassé (scroll-into-view) + ajout **T10bis**
>   (mesure : `WebSync.click` ≡ `Actions.moveToElement`, même trace pointeur `trusted` ; `OUT_OF_BOUNDS`
>   confirmé **inatteignable** comme cause d'un clic — la note T10 ci-dessous est validée empiriquement).
> - **Hygiène A9 (secret en clair)** : **corrigé** (T22 : zéro `println`, comparaison sans restitution).
> - **Bonus** : PageObjects PHASE 2 **migrés vers `WebSync`** (l'API Serenity native ne court-circuite
>   plus la synchro du socle — le principe « chaque PageObject détient un `WebSync` » est respecté).
>
> Détail par test : `qa-test-suivi-phase1.md`, section « Mise à jour 2026-08-02 ».

## Contexte

Campagne PHASE 1 rejouée : **25 tests, 8 KO identiques sous Chrome et Firefox** (donc indépendants du
navigateur). Après recoupement avec le code du socle et une revue externe, les 8 KO se répartissent en
**écarts socle** (à laisser tels quels) et **problèmes de test** (à corriger ici, dans `qa-test`).

**Principe directeur** : le **code du socle fait foi**. Un écart socle n'est **pas** à « corriger » en
trafiquant le test — il reste **rouge, documenté**, jusqu'à la reprise côté socle.

## Répartition des 8 KO — action par action

| KO | Cause racine | Côté | Action dans `qa-test` |
|---|---|---|---|
| **L4, L5 (SHORT/MEDIUM)** | Les profils `SyncTimeout` sont **court-circuités par l'attente interne de `WebElementFacade`** (Serenity attend ~4-5 s sous notre polling → cause racine « une seule horloge », C8). | **SOCLE** | **Aucune.** Garder rouge, annoté « écart socle connu C8 ». Ne pas modifier le test. |
| **T2, T8** | `ABSENT`/`DISABLED` non diagnostiqués : la facade lève ses **propres** exceptions (`ElementShouldBeEnabledException`) que le socle **ne classe pas** → remontée brute, pas de `SyncException [ABSENT]`/`[DISABLED]`. | **SOCLE** | **Aucune.** Garder rouge, annoté « écart socle connu (classification C8) ». |
| **T14** | `containsText(By,…)` **fuit `NoSuchElement`** sur élément absent (il ne passe pas par `query()`) → viole le contrat BF-SYNC-08 « ne lève jamais ». | **SOCLE** | **Aucune.** Garder rouge, annoté « bug socle connu ». |
| **T16** | ⚠️ **Attente fausse dans le test.** Le test attend une `SyncException`, mais le contrat (BF-SYNC-09 + soft-assert D22-bis) impose une **`AssertionError`**. Le socle est **correct**. | **TEST** | **CORRIGER** : attendre `AssertionError` (mode hard), vérifier message + locator + non-exécution de l'instruction suivante. Un éventuel code `[ABSENT]` serait une *amélioration*, pas le contrat. |
| **T6** | Le test navigue via `getDriver().get(...)` **sans jamais solliciter le socle avant** → il ne prouve **pas** un échec socle (le `pageLoadTimeout` est posé au 1ᵉʳ contact `WebSync`). | **TEST** (+ question de design socle ouverte) | **CORRIGER/CLARIFIER** : soit provoquer un contact `WebSync` **avant** la navigation (voir snippet), soit remonter « il faut protéger la 1ʳᵉ navigation » comme **besoin socle** à trancher. Ne plus présenter T6 comme « preuve de bug ». |
| **T10** | Élément à `top:5000px` → **Selenium/Serenity scrolle et clique** : le succès est **légitime**. Un `click()` **sans déplacement de souris** ne peut pas produire `OUT_OF_BOUNDS`. | **TEST** | **RECLASSER / à éprouver** : ne plus compter T10 comme défaut socle. Voir la **note T10** ci-dessous — le test est **possiblement obsolète**. |

**Bilan : 5 KO = écarts socle (ne rien toucher), 3 KO = à corriger/reclasser ici (T6, T10, T16).**

Snippet pour **T6** (si le contrat retenu reste « application au 1ᵉʳ contact WebSync ») :

```java
WebSync web = new WebSync();
web.isPresent(By.tagName("html")); // 1er contact → pose le pageLoadTimeout sur le driver
getDriver().get(slowPage);         // navigation ensuite → bornée
```

> **Note T10 — `OUT_OF_BOUNDS` est-il seulement atteignable ?**
> `MoveTargetOutOfBoundsException` vient de l'**API Actions / déplacement de pointeur** (mouvement de
> souris vers une coordonnée hors viewport), **pas** d'un `element.click()` simple — ce dernier fait un
> **scroll-into-view** avant de cliquer (spec WebDriver + Serenity). Donc pour même *tenter* de déclencher
> l'erreur, il faudrait un `new Actions(driver).moveToElement(...) / moveByOffset(...).click()` visant un
> point hors champ — **et encore**, Selenium/Serenity risque de **neutraliser** ça par un scroll
> automatique vers l'objet. **Conclusion probable** : le **déclenchement autonome** d'`OUT_OF_BOUNDS`
> (comme *cause* d'un clic en échec) est **quasi inatteignable** → **T10 est peut-être obsolète**.
> **À déterminer par un essai** (`Actions.moveToElement`) avant de le réécrire ou de le retirer.
> À noter : `OUT_OF_BOUNDS` reste **légitime comme code *latent*** dans le diagnostic au timeout (observé
> sous Firefox : message `[HIDDEN] [OUT_OF_BOUNDS]`) — le code n'est donc **pas mort**, il n'est simplement
> **jamais la cause d'un clic bloqué**.

## Corrections d'hygiène à faire ici (indépendantes des 8 KO)

1. **Secret loggé en clair (à corriger absolument)** : `FormEtSecretTest` fait
   `System.out.println("[T22] password value ... = " + value)`. Un `assertEquals(secret, value)`
   afficherait aussi le secret en cas d'échec. → **Supprimer toute sortie du clair**, ne jamais mettre le
   secret dans un message d'assertion, **comparer sans restituer** les opérandes, utiliser une valeur
   fictive dédiée non journalisée. (Le projet qui **valide** le masquage ne doit pas le **contourner**
   dans son propre code.)
2. **Traçabilité de la version exacte du socle** : `1.0.0-SNAPSHOT` + une date de rebuild ne suffisent
   pas. À chaque campagne, **enregistrer dans le rapport** : SHA de `qa-template` (`develop`), checksum du
   jar `qa-socle`, version Maven résolue, navigateur + version driver, JDK + Maven, et les `-D` effectifs.
   Rebuild du socle depuis `develop` (`mvn install`) avant toute nouvelle campagne.

## Points d'attention — SECRET

- **A9 ci-dessus** = le seul vrai correctif secret côté test.
- **Périmètre du masquage socle (résidu assumé, PAS un bug)** : le masquage « zéro clair » ne couvre
  **que** `input[type=password]`. Un secret **reflété ailleurs** (span, `data-*`, JSON inline, `<script>`)
  reste **en clair** dans le dump `.html` — c'est un **résidu documenté et assumé** côté socle (PA-2), pas
  un défaut à remonter. → Garder la validation « zéro clair » **cadrée sur les champs password** ; un
  secret en clair reflété ailleurs = ce résidu connu.
- **Format `masked()`** (2 premiers caractères + longueur via étoiles + 16 hexa de SHA-256) :
  **intentionnel et acté** (D12). Environnement de test + rotation CyberArk automatique → **ne pas** le
  signaler comme « insuffisant » (une revue externe l'a fait ; **écarté** en connaissance de cause). La
  traçabilité est **voulue** pour l'analyse post-mortem.

## Ce qu'il ne faut PAS faire

- Ne **pas** « verdir » les écarts socle (L4, L5, T2, T8, T14) en modifiant les tests : ils sont
  **attendus rouges** jusqu'à la reprise du moteur côté socle (chantier « une seule horloge » C8).
- Ne **pas** toucher au contrat de sortie (`KO__…`, 3 fichiers) ni aux attentes pour faire passer un
  test artificiellement.
