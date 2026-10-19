# Fichier de suivi — PHASE 1 (fixtures, bornes précises) — chapitre 3 du prompt

> Périmètre : validation navigateur du socle `qa-socle` via **fixtures HTML locales** (`file://`).
> Source : `docs/qa-test-prompt.md`, chapitre 3 (PHASE 1). Objectif : **aucun trou dans la raquette** —
> chaque test du document figure ici avec son statut. Un test non exécuté ou en échec est **notifié**.

## ⏱️ Mise à jour 2026-10-19 — surface `WebSync` complète, étapes `@Step`, soft-assert visible

> Cette section **prime** sur tout ce qui suit (sections antérieures conservées comme trace).

**Ce qui a changé depuis le 2026-10-18 (matin) :**
- **Surface de l'API `WebSync` couverte** : T25 à T82 dans `org.automation.websync` (un sous-package par
  domaine : `action`, `within`, `select`, `lecture`, `etat`, `etat/soft`, `attente`, `exception`), T77 et T78
  dans `sync/`, sur des fixtures dédiées (`fixtures/websync/`). Couverture fonctionnelle **100 % (78/78)**
  méthodes publiques de synchro, contre 20 % (16/78). Détail des tests : `qa-test-prompt.md` §3.
- **Cinq groupes** dans le profil `campagne` : `defaut`, `cle`, `precedence`, plus `polling`
  (`pollingInterval=2000ms`, T78) et `soft` (`qa.assertions.soft=true`, SHORT 1 s, T82) ; `forkCount` 15.
- **Étapes `@Step`** dans les tests `websync/` (hors appels chronométrés et appels dont on attend l'échec),
  capture après chaque étape (`AFTER_EACH_STEP`).
- **Socle corrigé** sur les trois écarts révélés : T43 (§5, `8736508`), T76 (§6, `1dda2d7`), soft-assert (§7,
  `81f66d5` et `da57c34`). Voir `qa-socle-suggestions.md`, en-tête « État ».
- **T82 refondu** : rouge à dessein, `@Disabled`, lancé à la main (README « Lancer les tests » §6) ; 16
  `should*` (13 KO, 3 OK) sur `soft.html`, en bloc hors étape puis en étapes.

**Résultat de la dernière campagne complète (2026-10-18 vers 23 h 50, Java 17, socle corrigé)** :

| Navigateur | Tests | OK | KO | Ignorés | Durée |
|---|---|---|---|---|---|
| Chrome | 104 | 93 | 1 : l'**ancien** T82 | 10 | 4 min 47 |
| Firefox | 104 | 92 | 1 : l'**ancien** T82 | 11 (dont `ThrottledLoadTest`, attendu) | 4 min 41 |

L'ancien T82 vérifiait le vote de fin en l'appelant lui-même dans un `assertThrows` ; depuis la correction §7,
ce vote déclare aussi le test en échec à Serenity, d'où le KO. Il a été refondu (ci-dessous). Campagne
complète **non relancée** depuis la refonte.

**T82 refondu (2026-10-19, lancé seul par l'exécution `soft`)** :

| Partie | Attendu | Chrome | Firefox |
|---|---|---|---|
| Bloc (hors étape) | un agrégat, 13 échecs dans l'ordre, 3 OK exclus, collecteur purgé | ✅ | ✅ |
| Étapes | 13 rouges, 3 vertes (n° 8, 14, 16), aucune sautée | ✅ | ✅ |
| Artefacts `KO__` | `ERROR.log`, `FAIL.log`, 13 `{step}.html` | ✅ | ✅ |
| Fin | test FAILED (vote de fin de JUnit) | ✅ 36,6 s | ✅ 29,3 s |

Premier essai : les 13 étapes KO étaient **vertes**. Cause : l'`assertThrows(AssertionError.class, …)` du bloc,
qui fait repeindre par Serenity, en fin de test, les étapes en échec par ce type. Remplacé par un `try/catch`.

**Reste à faire** : relancer la campagne complète ; convention de nommage (proposition à valider) ;
producteurs d'artefacts en prérequis ; §4 (socle) ; PHASE 2 (S1 à S4) ; PHASE 3 (parallélisme).

## ⏱️ Mise à jour 2026-10-18 — socle package-by-feature, campagne unique, bornes ± 5 s, Chrome + Firefox verts

> Section antérieure, conservée comme trace (elle primait sur les sections 2026-08-02 et 2026-07-15).

**Ce qui a changé :**
- **Socle** consommé depuis `qa-template` branche `feature/package-by-feature` : imports migrés
  (`api.sync` → `sync`, `api.secret` → `secret`, `api.page` → `page`, `api.exception.SyncException` →
  `sync.SyncException`). Aucune modification de comportement.
- **Campagne en une commande par navigateur** (`mvn clean verify [-Dwebdriver.driver=firefox]`) : profil
  Maven `campagne`, trois exécutions Surefire sélectionnées par package — `defaut` (aucune clé), `cle`
  (`short=10s`, `medium=25s`, `long=30s`, `extraLong=40s`, `pageLoadTimeout=10s`), `precedence`
  (`environment=override-D`) — 10 JVM en parallèle par exécution ; un échec ne stoppe plus la campagne.
- **Tests aux limites réécrits** selon la règle **± 5 s** (2 s pour SHORT par défaut et l'implicit wait),
  un cas passant **et** un cas non passant chronométrés par borne ; les valeurs sont lues (pas recopiées).
  Les anciennes valeurs abaissées (1 / 2 / 3 s) ont disparu.
- **Assertions visibles** dans chaque méthode ; `RESULTAT` écrit **après** les assertions.
- **Fenêtre 800x600** pour Chrome et Firefox ; **T10bis** défile explicitement avant `Actions` (Firefox
  levait `MoveTargetOutOfBounds` : `Actions` ne défile pas seul, contrairement à `WebSync.click`).

**Résultat campagne 2026-10-18 (Java 17)** — Chrome : 36 tests exécutés, 0 KO ; Firefox : 35 exécutés,
0 KO, `ThrottledLoadTest` « Aborted » (pas de CDP hors Chrome, attendu). Mesures (secondes) :

| Test | Borne | Cas | Attendu | Chrome | Firefox |
|---|---|---|---|---|---|
| L1 | implicit wait 3 s | passant | trouvé vers 1 s | 1,01 | 1,00 |
| L2 | implicit wait 3 s | non passant | `NoSuchElement` à 3 s | 3,05 | 3,02 |
| `L5_short_defaut_borne_basse` | SHORT défaut 5 s | passant | clic vers 3 s | 3,17 | 3,15 |
| `L5_short_defaut_borne_haute` | SHORT défaut 5 s | non passant | exception à 5 s | 5,16 | 5,15 |
| `L5_medium_vraies_valeurs` | MEDIUM défaut 15 s | passant | clic vers 10 s | 10,22 | 10,41 |
| `L5_medium_defaut_borne_haute` | MEDIUM défaut 15 s | non passant | exception à 15 s | 15,18 | 15,22 |
| `DynamicWaitTest` MEDIUM (PageObject) | MEDIUM 15 s | passant | trouvé vers 10 s | 10,03 | 10,01 |
| `DynamicWaitTest` SHORT (PageObject) | SHORT 5 s | non passant | exception à 5 s | « après 5 s » | « après 5 s » |
| `L5_short_borne_basse` | clé SHORT 10 s | passant | clic vers 5 s | 5,24 | 5,23 |
| `L5_short_borne_haute` | clé SHORT 10 s | non passant | exception à 10 s | 10,22 | 10,06 |
| L3 | clé MEDIUM 25 s | passant | clic vers 20 s | 20,05 | 20,46 |
| L4 | clé MEDIUM 25 s | non passant | exception à 25 s | 25,14 | 25,20 |
| `L5_long_borne_basse` | clé LONG 30 s | passant | clic vers 25 s | 25,10 | 25,57 |
| `L5_long_borne_haute` | clé LONG 30 s | non passant | exception à 30 s | 30,14 | 30,23 |
| `L5_extralong_borne_basse` | clé EXTRA_LONG 40 s | passant | clic vers 35 s | 35,12 | 35,51 |
| `L5_extralong_borne_haute` | clé EXTRA_LONG 40 s | non passant | exception à 40 s | 40,07 | 40,17 |
| T6bis | `pageLoadTimeout` 10 s | passant | page bloquée 5 s chargée | 5,02 | 5,07 |
| T6 | `pageLoadTimeout` 10 s | non passant | `TimeoutException` à 10 s | 10,01 | 10,01 |
| `ThrottledLoadTest` (2G) | `pageLoadTimeout` 10 s | non passant | `TimeoutException` à 10 s | 10,05 | Aborted |

Diagnostic (SHORT 5 s, Chrome et Firefox) : T2 `[ABSENT]` ✅, T11 `[ABSENT]` sur le bon maillon ✅,
T7 `[HIDDEN]` ✅ (avec le faux positif `[OUT_OF_BOUNDS]`, cf. `qa-socle-suggestions.md` §4), T8
`[DISABLED]` ✅, T9 `[INTERCEPTED]` ✅, T10 / T10bis clic abouti ✅.

**Reste à faire** : producteurs d'artefacts `@Disabled` à transformer en prérequis de leurs
vérificateurs ; **PHASE 3** (parallélisme JUnit — un DOM par test KO, pas de fuite `ThreadLocal`).

## ⏱️ Mise à jour 2026-08-02 — socle corrigé, tests migrés, campagne tout-vert

> Cette section **prime** sur les tableaux plus bas, qui documentent la campagne **historique du
> 2026-07-15** (socle pré-correctif, 8 KO). Ils sont conservés comme trace, **non réécrits** (pas
> d'invention de timings ni de passe Firefox non rejouée).

**Ce qui a changé depuis le 2026-07-15 :**
- **Socle corrigé** (`develop`) : moteur SYNC implémenté — profils `SyncTimeout` et overrides
  `-Dqa.sync.timeout.*` **appliqués**, diagnostic `[ABSENT]/[DISABLED]/[OUT_OF_BOUNDS]` **produit**,
  `pageLoadTimeout` **borné**. Les ÉCART-1 à 5 (côté socle) sont **levés**.
- **Corrections côté test** (cf. `qa-test-briefing-erreurs-phase1.md`) : **T6** (contact `WebSync` avant
  navigation), **T10** reclassé (scroll-into-view + **T10bis**, preuve d'équivalence pointeur), **T16**
  (attend `AssertionError`), **A9** (secret : zéro sortie du clair).
- **PHASE 2 basée sur `QaPageObject`** (`LoginPage`/`CheckboxesPage`/`DelayedButtonPage`, champ `web`
  hérité), relancée verte. Ajout de deux validations réelles : **`DynamicWaitTest`** (passage du profil
  `SyncTimeout` — `#delayed` à 10 s : SHORT échoue / MEDIUM réussit, cross-browser) et
  **`ThrottledLoadTest`** (throttle **2G réel** CDP, Chrome only, `pageLoadTimeout` borné).
- **Conventions appliquées à tout le projet** : Steps « attendu passant » (l'exception est vérifiée par
  `assertThrows` côté test, jamais renvoyée par une Step) ; logs uniformes `RESULTAT : exception
  attendue=… | levee=OUI | type reel=… | message="…"` (non passant) / `… | aucune exception` (passant).

**Résultat campagne 2026-08-02 (Chrome, Java 17) — 24 tests PHASE 1 exécutables : tous ✅.** Commandes
exactes : README, section « Lancer une campagne complète ». Les 8 anciens KO passent désormais :

| Ancien KO | Cause d'origine | Statut 2026-08-02 |
|---|---|---|
| L4, L5-SHORT | profils `SyncTimeout` non appliqués (ÉCART-1) | ✅ socle corrigé |
| T2, T8 | diagnostic `[ABSENT]/[DISABLED]` non produit (ÉCART-2) | ✅ socle corrigé |
| T14 | `containsText` fuyait `NoSuchElement` (ÉCART-5) | ✅ socle corrigé |
| T6 | `pageLoadTimeout` non armé | ✅ correctif test (contact `WebSync` avant nav) |
| T10 | `OUT_OF_BOUNDS` inatteignable | ✅ reclassé (scroll-into-view) + T10bis |
| T16 | `should*` attendait `SyncException` | ✅ correctif test (attend `AssertionError`) |

**Reste à rafraîchir** : re-passe **Firefox** avec le socle corrigé pour actualiser les timings des
tableaux ci-dessous, puis **PHASE 3** (parallélisme).

## Périmètre exact des tests de la PHASE 1

Le chapitre 3 comporte **deux tableaux**. Les identifiants absents du document (T3, T4, T12, T13, T21)
ne sont **pas** des tests : ils n'existent pas dans les tableaux et ne sont donc pas inventés ici.
- Tableau « Tests aux limites » : **L1, L2, L3, L4, L5, L6** → 6 tests.
- Tableau « Comportements précis » : **T1, T2, T5, T6, T7, T8, T9, T10, T11, T14, T15, T16, T17, T18,
  T19, T20, T22, T23, T24** → 19 tests.
- **Total attendu PHASE 1 = 25 tests.**

## État des prérequis

| Prérequis | État | Détail |
|---|---|---|
| Socle publié dans `~/.m2` | ✅ Prêt | `qa-parent` + `qa-socle` 1.0.0-SNAPSHOT présents |
| POM (`qa-parent` parent, `qa-socle` sans version) | ✅ Prêt | conforme, inchangé |
| `serenity.conf` (environment=qatest, implicit 3s, soft=false, page-load normal, reporting off) | ✅ Prêt | aligné sur le prompt |
| Fixtures HTML (10) sous `src/test/resources/fixtures/` | ✅ Prêt | appear, disappear, rerender, hidden, disabled, overlay, offscreen, slowload, form, nested |
| Utilitaire `Fixtures.url(...)` (résolution `file://`) | ✅ Prêt | `org.automation.support.Fixtures` |
| Chrome / Firefox / Maven | ✅ Prêt | Chrome 150, Firefox 152, Maven 3.9.12 |
| Tests JUnit L1–L6 / T* | ✅ Écrits + exécutés (Chrome **et** Firefox) | 25 tests couverts sur les deux navigateurs |

> ⚠️ Point d'attention environnement : JDK par défaut de `mvn` = Java 25 ; JDK 17 disponible
> (`ms-17.0.19`). Tous les runs ci-dessous ont été forcés en **Java 17** via `JAVA_HOME`
> (cible du socle). Compilation et exécution OK en Java 17.

## Tableau de résultats — Tests aux limites

| # | Mécanisme | Attendu | Statut | Chrome | Firefox | Écart / Note |
|---|---|---|---|---|---|---|
| L1 | implicit wait 3s — borne basse (`appear?delay=2800`, findElement brut) | trouvé (< 3 s) | ✅ OK | ✅ trouvé en **2856 ms** | ✅ trouvé en **2816 ms** | Implicit wait natif 3 s opérationnel sur appel Selenium brut. |
| L2 | implicit wait 3s — borne haute (`appear?delay=3200`, findElement brut) | `NoSuchElement` (> 3 s) | ✅ OK | ✅ `NoSuchElement` en **3056 ms** | ✅ `NoSuchElement` en **3020 ms** | Borne haute de l'implicit wait respectée. |
| L3 | profil — borne basse (`appear?delay=T-200`, `WebSync.click`) | succès | ✅ OK | ✅ click en **1826 ms** | ✅ click en **1884 ms** | Passe, mais voir **ÉCART-1** : la valeur du profil n'est pas déterminante (l'attente effective est le défaut Serenity ~5 s, pas `medium`). Le succès tient au fait que la cible (1800 ms) < ~5 s. |
| L4 | profil — borne haute (`appear?delay=T+200`, `WebSync.click`) | `SyncException [ABSENT]` | ❌ KO | ❌ **aucune exception** (le click réussit) | ❌ idem (aucune exception) | **ÉCART-1** : `-Dqa.sync.timeout.medium=2s` ignoré → attente effective ~5 s > cible 2200 ms → pas de timeout, pas de `SyncException`. Profils `SyncTimeout` non appliqués. |
| L5 | répéter L3/L4 pour SHORT/MEDIUM/LONG/EXTRA_LONG (dont vraies valeurs MEDIUM) | bornes respectées | ❌ KO (partiel) | LONG borne basse ✅ (click 2576 ms) ; SHORT borne haute ❌ (aucune exception) ; MEDIUM vraies valeurs (14 s) ❌ `ElementShouldBeEnabledException` à **~4 s** | LONG ✅ (2588 ms) ; SHORT ❌ ; MEDIUM ❌ `ElementShouldBeEnabled` ~5 s | **ÉCART-1** : SHORT=1s et MEDIUM=15s non appliqués. L'attente plafonne à ~5 s (défaut Serenity) → SHORT ne timeoute pas (cible 1500 ms), MEDIUM abandonne à ~4 s au lieu d'attendre 14 s. EXTRA_LONG non testé isolément (même moteur). |
| L6 | polling fin sous WebSync malgré implicit 3s | WebSync tôt (~250ms) vs brut ~3s | ✅ OK | ✅ WebSync **2615 ms** vs brut **2547 ms** (assertion `msWeb ≤ msRaw+500` tenue) | ✅ WebSync **2578 ms** vs brut **2508 ms** | Les deux attentes ne s'additionnent pas ; le toggle d'implicit wait fonctionne. Cible à 2500 ms → l'avantage du polling fin n'est pas spectaculaire ici (les deux rendent la main vers ~2,5 s). |

## Tableau de résultats — Comportements précis

| # | Quoi | Attendu | Statut | Chrome | Firefox | Écart / Note |
|---|---|---|---|---|---|---|
| T1 | `fluentWait` attend un élément tardif | action réussit | ✅ OK | ✅ click sur cible à 1500 ms | ✅ idem | Attente d'un élément tardif honorée. |
| T2 | jamais présent (locator bidon) | `SyncException [ABSENT]` | ❌ KO | ❌ `ElementShouldBeEnabledException` (pas `SyncException`) | ❌ idem (`ElementShouldBeEnabledException`) | **ÉCART-2** : sur locator absent, l'exception Serenity brute remonte ; pas de `SyncException [ABSENT]`. Le code `[ABSENT]` n'est pas produit. |
| T5 | stale/re-render absorbé (`rerender`) | ré-résolution transparente | ✅ OK | ✅ lectures v0→v2 sans `StaleElement` | ✅ idem (v0→v2) | Ré-résolution du `By` transparente, transitoires absorbés. |
| T6 | `pageLoadTimeout` posé auto (borne, `slowload?ms=8000`) | échec borné ~5 s | ❌ KO | ❌ navigation **8714 ms** (blocage complet), pas de timeout | ❌ nav **9341 ms**, pas de timeout | **ÉCART-3** : `-Dqa.sync.pageLoadTimeout=5s` non honoré ; la navigation n'est pas bornée (attend les 8 s complets). |
| T7 | `HIDDEN` (`hidden`) | `[HIDDEN]` | ✅ OK | ✅ `SyncException [HIDDEN]` | ✅ `[HIDDEN]` (message multi-codes `[HIDDEN] [OUT_OF_BOUNDS]`) | Diagnostic actif opérationnel pour HIDDEN. |
| T8 | `DISABLED` (`disabled`) | `[DISABLED]` | ❌ KO | ❌ `ElementShouldBeEnabledException` (pas `SyncException`) | ❌ idem (`ElementShouldBeEnabledException`) | **ÉCART-2** : élément visible mais `disabled` → exception Serenity brute, pas `SyncException [DISABLED]`. |
| T9 | `INTERCEPTED` (`overlay`) | `[INTERCEPTED]` | ✅ OK | ✅ `SyncException [INTERCEPTED]` | ✅ `[INTERCEPTED]` | Diagnostic actif opérationnel pour INTERCEPTED (hit-test). |
| T10 | `OUT_OF_BOUNDS` (`offscreen`) | `[OUT_OF_BOUNDS]` | ❌ KO | ❌ **aucune exception** (le click aboutit) | ❌ idem (aucune exception) | **ÉCART-4** : élément à `top:5000px` → Selenium scrolle et clique ; le code `[OUT_OF_BOUNDS]` n'est jamais déclenché. |
| T11 | `within` container vs child absent (`nested`) | `cause` = bon maillon | ✅ OK | ✅ succès bon container/child | ✅ idem (`cause [ABSENT]` = bon maillon) | Résolution deux-temps `within` fonctionnelle. |
| T14 | `is*`/`contains*` : absent → `false`, instantané | `false` immédiat (pas de 3 s) | ❌ KO | ❌ `containsText` fuit `NoSuchElement` sur absent | ❌ idem (`containsText` fuit `NoSuchElement`) | **ÉCART-5** : `isVisible/isPresent/isEnabled` OK (false), mais `containsText(By, …)` sur locator absent **lève** `NoSuchElement` au lieu de rendre `false`. Contrat « ne lève jamais » violé pour `containsText`. |
| T15 | `waitUntil*` positifs/négatifs (`appear`/`disappear`) | attentes honorées | ✅ OK | ✅ `waitUntilVisible` puis `waitUntilNotVisible` honorés | ✅ idem | Attentes explicites positives et négatives OK. |
| T16 | `should*` dur (`soft=false`, `form`) | échoue à la 1re | ❌ KO | ❌ `AssertionError` (pas `SyncException` avec code) | ❌ idem (`AssertionError`) | **ÉCART-6** : `shouldBeVisible` sur absent lève une `AssertionError` Serenity sans code diagnostic `[ABSENT]`/`[HIDDEN]`. L'échec « dur » à la 1re assertion a bien lieu, mais pas sous forme de `SyncException` diagnostiquée. |
| T17 | `should*` soft (`soft=true`) | agrégé, échec en fin (extension auto) | ✅ OK | ✅ 2 `should*` KO **collectés**, échec agrégé **en fin** | ✅ idem (« 2 assertion(s) souple(s) en échec ») | Mode soft opérationnel : pas d'arrêt à la 1re, agrégation « 2 assertion(s) souple(s) en échec » via extension auto-détectée (aucune annotation côté test). |
| T18 | KO → dossier `KO__…/` + 3 fichiers | dossier + ERROR.log + FAIL.log + {step}.html | ✅ OK | ✅ dossier `KO__QATEST__…` + ERROR.log + FAIL.log + `{step}.html` (via `@Steps`) | ✅ dossier + 3 fichiers (run propre) | Validé par `RawDomCaptureProducer/Verifier` (site réel, PageObjects `@Step`). **Note** : le producteur phase-1 `ArtefactsProducerTest` appelle `WebSync` **hors `@Step`** → seul `TestFailureManager` s'active (ERROR.log + FAIL.log) mais **pas** de `{step}.html` (dump lié au `stepFailed`, conforme doc). |
| T19 | dump = DOM réel à l'échec | page source du moment | ✅ OK | ✅ vrai DOM `<html>` de la page à l'échec (pas la vue Prism échappée) | ✅ vrai DOM `<html>` | Validé via `RawDomCaptureVerifier` (4 tests verts en Chrome ET Firefox). |
| T20 | masquage `input[type=password]` (zéro clair, `form`) | `value` blanchie dans le `.html` | ✅ OK | ✅ mot de passe clair **absent** du dump (0 occurrence), `value` blanchie | ✅ mot de passe clair **absent** (0 occurrence) | Validé via `RawDomCaptureVerifier` (login `Secret.of("mauvais-mot-de-passe")` → 0 occurrence en clair). |
| T22 | `type(By, Secret)` (`form`) | saisie OK + masquage | ✅ OK | ✅ `value` DOM = `SuperSecretPassword!` (saisie effective) | ✅ `value` = `SuperSecretPassword!` | Saisie via `Secret` fonctionnelle ; masquage à l'échec couvert par T20. |
| T23 | 3 services actifs par présence du jar | listeners actifs (aucun `@ExtendWith` custom) | ✅ OK | ✅ 3 services actifs sans `@ExtendWith` custom | ✅ 3 services actifs | `TestFailureManager` (dossiers/logs) + `RawDomCaptureListener` (dump DOM au `stepFailed`) + `SoftAssertionExtension` (agrégation T17) confirmés par simple présence du jar. |
| T24 | précédence `-D` > `serenity.conf` | `-D` gagne | ✅ OK | ✅ `environment` résolu = `override-D` (conf = `qatest`) | ✅ `environment` = `override-D` | Précédence `-D` > `serenity.conf` confirmée. |

## Légende statut

- ⬜ non exécuté
- ✅ OK (attendu observé)
- ❌ KO (attendu non observé — à notifier + écart)
- ⚠️ partiel / à confirmer

## Synthèse Chrome + Firefox (2026-07-15) — *historique (socle pré-correctif)*

- **25 tests couverts. Chrome : 17 ✅ / 8 ❌. Firefox : 17 ✅ / 8 ❌ — résultats identiques.**
  Les 8 écarts sont de niveau **socle** (indépendants du navigateur), ce que la passe Firefox confirme.
- ✅ (17) : L1, L2, L3, L5(LONG), L6, T1, T5, T7, T9, T11, T15, T17, T18, T19, T20, T22, T23, T24.
  *(L3 et L5-LONG passent, mais sans valider réellement la borne du profil — cf. ÉCART-1.)*
- ❌ (8) : **L4, L5(SHORT+MEDIUM), T2, T6, T8, T10, T14, T16**.
- Écarts de timing entre navigateurs négligeables (L1 2856/2816 ms, L2 3056/3020 ms, L6 ~2,6/2,5 s).
  Seule nuance Firefox : le diagnostic HIDDEN remonte un message **multi-codes** `[HIDDEN] [OUT_OF_BOUNDS]`
  (contient bien `[HIDDEN]` → T7 ✅).

> Les parties **réellement implémentées** du socle (capture d'artefacts `KO__`, dump DOM au `stepFailed`,
> masquage `password`, soft-assert, activation par ServiceLoader, diagnostic HIDDEN/INTERCEPTED, `within`,
> stale-absorb, waits explicites, toggle implicit-wait) **fonctionnent**. Les KO se concentrent sur le
> **moteur de timeout par profils** et sur **une partie du diagnostic** (ABSENT/DISABLED/OUT_OF_BOUNDS) et
> des `should*`.

## Écarts socle à signaler (backlog socle — ne pas corriger côté test)

> Conformément au chapitre 5 du prompt (« le code du socle fait foi ; toute divergence doc/comportement
> est un écart à signaler »). Cause racine cohérente avec l'état connu du socle (moteur SYNC en partie
> squelette ; capture d'échec + masquage réels).

- **ÉCART-1 — Profils `SyncTimeout` non appliqués (impacte L3, L4, L5).** Les valeurs `SHORT/MEDIUM/LONG/
  EXTRA_LONG` (défauts 5/15/60/120 s) et surtout les overrides `-Dqa.sync.timeout.<profil>` **n'ont aucun
  effet** sur l'attente réelle de `WebSync`. L'attente effective plafonne au défaut Serenity (~5 s).
  Preuves : `L3` avec `-Dqa.sync.timeout.medium=1s` attend quand même 1852 ms (cible 1800 ms) ; `L4`
  (cible 2200 ms, `medium=2s`) réussit au lieu de timeouter ; `L5-MEDIUM` (cible 14 s) abandonne à ~4 s.
  Le mécanisme `-D` lui-même fonctionne (T24 ✅) → c'est bien le socle qui n'exploite pas la clé.
- **ÉCART-2 — Diagnostic ABSENT/DISABLED non produit (T2, T8).** Sur élément **absent** ou **disabled**,
  l'exception Serenity brute (`ElementShouldBeEnabledException`) remonte au lieu d'une `SyncException`
  portant `[ABSENT]` / `[DISABLED]`. (HIDDEN et INTERCEPTED, eux, sont bien diagnostiqués — T7/T9 ✅.)
- **ÉCART-3 — `pageLoadTimeout` non borné (T6).** `-Dqa.sync.pageLoadTimeout=5s` n'est pas honoré ;
  `slowload?ms=8000` bloque 8714 ms sans échec de navigation borné.
- **ÉCART-4 — `OUT_OF_BOUNDS` non détecté (T10).** Élément hors viewport → Selenium scrolle et clique ;
  aucun `SyncException [OUT_OF_BOUNDS]`.
- **ÉCART-5 — `containsText` viole le contrat « ne lève jamais » (T14).** Sur locator absent,
  `containsText(By, …)` propage `NoSuchElement` au lieu de rendre `false`. `isVisible/isPresent/isEnabled`
  respectent le contrat.
- **ÉCART-6 — `should*` sans code diagnostic (T16).** `shouldBeVisible` sur absent lève une `AssertionError`
  Serenity, pas une `SyncException` avec code `[ABSENT]`/`[HIDDEN]`. L'échec « dur » a lieu, mais non
  diagnostiqué.

## Journal d'exécution

**Environnement.** `JAVA_HOME` forcé sur Java 17 (`ms-17.0.19`) ; Maven 3.9.12 ; Chrome 150 /
chromedriver 150 (Selenium Manager) ; socle `qa-socle 1.0.0-SNAPSHOT` (jar reconstruit le 2026-07-15 02:05).
Tous les runs : `-Dmaven.test.failure.ignore=true` pour collecter tous les résultats.

**Runs Chrome (2026-07-15, ~03:24–03:45).** Groupés par valeurs de profil compatibles (contrainte :
L4 exige `medium=2s`, L5-MEDIUM exige `medium=15s` réel → runs séparés) :

- **A** — `BornesLimitesTest` (sauf L5-MEDIUM) `-Dqa.sync.timeout.medium=2s -Dqa.sync.timeout.short=1s
  -Dqa.sync.timeout.long=10s` → 13 tests, 3 échecs (L4, L5-SHORT, T2) + 1 erreur (T14). L1=2856 ms,
  L2=3056 ms, L3=1826 ms, L5-LONG=2576 ms, L6=2615/2547 ms.
- **B** — `BornesLimitesTest#L5_medium_vraies_valeurs` (aucun override, `medium`=15 s réel) →
  1 erreur `ElementShouldBeEnabledException` à ~4 s (au lieu d'attendre 14 s).
- **C** — `DiagnosticTest` `-Dqa.sync.timeout.short=1s` → 4 tests, 2 échecs : T8 (DISABLED),
  T10 (OUT_OF_BOUNDS). T7 (HIDDEN) et T9 (INTERCEPTED) ✅.
- **D** — `FormEtSecretTest` `-Dqa.sync.timeout.short=1s` → 3 tests, 1 échec : T16. Surface (type/select/
  checkbox/submit + lectures) ✅, T22 (`Secret`) ✅.
- **E** — `PageLoadEtConfigTest` `-Dqa.sync.pageLoadTimeout=5s -Denvironment=override-D` → 2 tests,
  1 échec : T6 (nav 8714 ms). T24 ✅ (`environment=override-D`).
- **F/G** — `ArtefactsProducerTest` puis `ArtefactsVerificationTest`
  (`-Djunit.jupiter.conditions.deactivate=*DisabledCondition`) : producteur KO volontaire → dossier
  `KO__QATEST__…` avec ERROR.log + FAIL.log **mais sans `{step}.html`** (échec hors `@Step`) → vérificateur
  KO (« dump {step}.html manquant »). ⇒ T18/T19/T20 revalidés par les tests `@Steps` ci-dessous.
- **H** — `SoftAssertTest` `-Dqa.assertions.soft=true` + deactivate → T17 ✅ : 2 `should*` collectés,
  échec agrégé « 2 assertion(s) souple(s) en échec » en fin.
- **RawDomCapture** (site réel, PageObjects `@Step`) — `RawDomCaptureProducerTest` (2 KO volontaires
  LOGIN/CHECKBOXES) puis `RawDomCaptureVerificationTest` (**4 ✅**) : dossiers avec `{step}.html`, DOM réel
  `<html>` (T19), mot de passe clair `mauvais-mot-de-passe` **absent** du dump = masquage (T20), 3 services
  actifs (T23). ⇒ **T18/T19/T20/T23 validés**.

**Runs Firefox (2026-07-15, ~12:06–12:17).** Mêmes groupes A–E, H et RawDomCapture, avec
`-Dwebdriver.driver=firefox` (geckodriver via Selenium Manager). Résultats **identiques à Chrome** test par
test (8 KO aux mêmes endroits, mêmes causes). Timings : L1=2816, L2=3020, L3=1884, L5-LONG=2588,
L6=2578/2508 ms, T6 nav=9341 ms. Sur un **run propre** (après nettoyage de `target/qa-results`), le
producteur `RawDomCapture` génère bien les dumps `{step}.html` (LOGIN + CHECKBOXES) et le vérificateur
passe **4/4** (DOM `<html>` réel, mot de passe clair **absent** → masquage OK) — comme Chrome.

> ⚠️ Piège rencontré : un premier lancement Firefox a échoué au **paramétrage du driver** (bug de quoting
> zsh : `-Dwebdriver.driver` avait avalé toute la ligne). Ces échecs « avant navigateur » ont tout de même
> fait créer des dossiers `KO__` **sans dump** ; le vérificateur, tombant sur ces dossiers périmés, a
> d'abord signalé à tort « dump `.html` manquant ». Après nettoyage de `target/qa-results` et relance
> propre, T18/T19/T20/T23 sont **verts** en Firefox. Enseignement : **nettoyer `target/qa-results` avant
> une campagne producteur/vérificateur**, sinon les artefacts d'un run antérieur faussent la vérification.

**Reste à faire.** ~~PHASE 2 (sites réels)~~ **faite** (PageObjects migrés vers `WebSync`,
`AuthenticationTest` vert) ; **PHASE 3** (parallélisme — un DOM par test KO, pas de fuite `ThreadLocal`)
selon le prompt. Rafraîchir aussi les timings ci-dessus par une re-passe Chrome+Firefox sur socle corrigé
(cf. « Mise à jour 2026-08-02 » en tête).

