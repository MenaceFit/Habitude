# Habitude

[![Android CI](https://github.com/MenaceFit/Habitude/actions/workflows/android-ci.yml/badge.svg)](https://github.com/MenaceFit/Habitude/actions/workflows/android-ci.yml)

**Habitude** est une application Android native de suivi d'habitudes (*habit tracker*), conçue pour fonctionner **100 % hors-ligne** : aucune création de compte, aucun serveur, aucune donnée envoyée où que ce soit. Toutes les données (habitudes, complétions, séries, XP, journal, objectifs...) vivent uniquement sur l'appareil, dans une base de données locale.

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Structure du projet](#structure-du-projet)
- [Prérequis](#prérequis)
- [Compilation](#compilation)
- [Tests](#tests)
- [Intégration continue](#intégration-continue)
- [Export / Import des données](#export--import-des-données)
- [Confidentialité](#confidentialité)
- [Internationalisation](#internationalisation)

## Fonctionnalités

- **Onboarding** guidé : choix de catégories d'objectifs → suggestions d'habitudes pré-remplies (37 modèles répartis sur 12 catégories) → objectif personnel → création du profil.
- **Tableau de bord « Aujourd'hui »** : liste des habitudes du jour avec complétion en un geste, animations de célébration, quêtes quotidiennes.
- **7 types d'habitudes** : booléen, quantité, durée, compteur, limite (à ne pas dépasser), heure cible, checklist.
- **5 modes de fréquence** : tous les jours, jours spécifiques, X fois/semaine, X fois/mois, tous les N jours.
- **Système de progression** : XP et niveaux (courbe fermée `seuil(N) = 25×(N-1)×(N+2)`), séries (*streaks*) avec « gel de série » optionnel, 16 badges/succès, défis personnalisés.
- **Calendrier** : grille mensuelle colorée selon la performance du jour, et heatmap de type GitHub sur 12 mois.
- **Statistiques** : score de régularité pondéré, insights en langage naturel calculés localement, et 7 visualisations (progression quotidienne, évolution de l'XP, évolution des séries, taux de réussite par habitude, comparaison par catégorie, grille mensuelle, heatmap 12 mois).
- **Routines** : regroupement d'habitudes sous un nom commun (« routine du matin »...).
- **Journal & humeur** : entrée quotidienne de texte + humeur/énergie, historique, corrélation humeur/performance.
- **Objectifs long terme** avec suivi de progression.
- **Widget d'écran d'accueil** avec complétion rapide sans ouvrir l'application.
- **Export / import** complet (JSON) et export des complétions au format CSV.
- **Réglages** : thème (clair/sombre/système), couleur d'accent, retours haptiques, sons, notifications, premier jour de la semaine, format d'heure, langue.
- **Français et anglais**, changement de langue en direct depuis les réglages, sans redémarrage.
- **Aucune fonctionnalité factice** : tout ce qui est affiché est branché sur de vraies données persistées localement (pas de placeholder « bientôt disponible »).

## Architecture

Le projet suit une architecture **Clean/MVVM** répartie sur deux modules Gradle :

```
:domain   → module Kotlin pur (aucune dépendance Android), compile sur la JVM seule.
            Modèles métier, calculateurs (séries, XP, score de régularité, achievements,
            quêtes), logique 100 % testable sans Android.

:app      → module Android (Jetpack Compose + Material 3).
            UI, ViewModels, persistance Room, notifications, widget, DataStore.
```

**Pourquoi cette séparation ?** Toute la logique métier sensible (calcul de séries, formule d'XP, score de régularité...) vit dans `:domain`, sans aucune dépendance Android — elle peut donc être testée par de simples tests JUnit exécutés sur la JVM, sans émulateur ni Robolectric, et reste réutilisable si l'application évolue un jour vers d'autres plateformes.

### Choix techniques principaux

| Domaine | Choix | Pourquoi |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Déclaratif, thème clair/sombre/couleur d'accent piloté par le code |
| Architecture | MVVM + Clean Architecture, DI manuelle (`AppContainer`) | Pas besoin de Hilt/Dagger pour ce périmètre ; toutes les dépendances sont visibles dans un seul fichier |
| Persistance | Room 2.6 + KSP | SQL typé, migrations versionnées, `TypeConverters` pour les types polymorphes (`Frequency`, `HabitTarget`) sérialisés en JSON via kotlinx.serialization |
| Navigation | Navigation Compose | Graphe unique : onboarding → coquille à barre de navigation basse (Aujourd'hui / Habitudes / Stats / Progression / Profil) → écrans secondaires empilés |
| Notifications | WorkManager (et non `AlarmManager`) | Évite la permission d'alarme exacte ; suffisant pour des rappels quotidiens |
| Widget | `AppWidgetProvider` + `RemoteViews` classique (pas Glance) | Dépendance minimale, contrôle total du rendu |
| Préférences | DataStore Preferences | Remplaçant moderne et asynchrone de `SharedPreferences` |
| Graphiques | Canvas Compose fait main (`LineChart`, `HorizontalBarChart`) | Pas de dépendance tierce non vérifiable ; interactions tactiles (tap/drag) sur mesure |
| Hors-ligne | Aucune permission `INTERNET` dans le manifeste | Garantie au niveau du système, pas seulement au niveau du code |

### Flux de navigation

```
Splash (lecture des préférences)
 ├─ onboarding non terminé → Accueil / Catégories / Suggestions / Objectif perso / Profil
 └─ onboarding terminé     → Coquille principale (barre de navigation basse)
                              ├─ Aujourd'hui   (tab)
                              ├─ Habitudes     (tab)
                              ├─ Stats         (tab)
                              ├─ Progression   (tab — quêtes / défis / succès)
                              └─ Profil        (tab)
                                   ├─ Réglages
                                   ├─ Journal
                                   ├─ Objectifs
                                   ├─ Routines
                                   └─ Confidentialité
             + Calendrier et Créer/Éditer une habitude, accessibles depuis plusieurs écrans
```

## Structure du projet

```
domain/src/main/kotlin/.../domain/
├── model/            Habit, Frequency, HabitTarget, énumérations, ExportedData...
├── streak/           StreakCalculator (séries, gel de série, fréquences par période)
├── xp/                LevelCurve, XpEngine
├── stats/             ConsistencyScoreCalculator, CalendarDayScorer, StatsAggregator, insights
├── achievement/       AchievementCatalog, AchievementEvaluator
├── quest/             QuestGenerator
├── onboarding/        HabitSuggestionCatalog
└── serialization/     ExportedData (schéma d'export JSON)

app/src/main/kotlin/.../
├── data/
│   ├── local/          AppDatabase (Room), DAOs, TypeConverters
│   ├── mapper/         Entity ↔ modèle de domaine
│   ├── preferences/     DataStore (thème, langue, notifications...)
│   └── repository/      Implémentations des repositories
├── di/                 AppContainer (composition root, DI manuelle)
├── gamification/       CompletionCoordinator (orchestration streak + XP + quêtes + widget)
├── notifications/      WorkManager, canaux, planification des rappels
├── widget/              Widget d'écran d'accueil
├── ui/
│   ├── theme/           Couleurs, typographie, formes
│   ├── components/      Composants réutilisables (dont les graphiques Canvas)
│   ├── navigation/       Routes + graphe de navigation
│   ├── dashboard/, habits/, stats/, calendar/, progress/,
│   │   profile/, journal/, goals/, routines/, onboarding/
│   └── common/           Aides DI pour Compose (`appViewModel`, `LocalAppContainer`)
└── util/                 Horodatage, résolution de clés de chaînes, haptique, sons
```

## Prérequis

- JDK 17
- Android SDK (`compileSdk`/`targetSdk` 34, `minSdk` 26 — Android 8.0+)
- Le [wrapper Gradle](./gradlew) fourni ; aucune installation Gradle séparée n'est nécessaire.

## Compilation

Depuis la racine du projet :

```bash
# APK de debug, installable directement sur un appareil/émulateur
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# APK de release (minifiée/réduite par R8), non signée par défaut
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release-unsigned.apk
```

Le projet ne contient volontairement aucun keystore. Pour produire une **release signée**, définissez les propriétés Gradle suivantes (par exemple via des variables d'environnement en CI, voir [`.github/workflows/android-ci.yml`](./.github/workflows/android-ci.yml)) :

```bash
./gradlew :app:assembleRelease \
  -Pandroid.injected.signing.store.file=/chemin/vers/keystore.jks \
  -Pandroid.injected.signing.store.password=*** \
  -Pandroid.injected.signing.key.alias=*** \
  -Pandroid.injected.signing.key.password=***
```

## Tests

```bash
# Logique métier (module :domain, pur JVM — rapide, aucun SDK Android requis)
./gradlew :domain:test

# Tests unitaires du module :app (Robolectric pour les tests Room)
./gradlew :app:testDebugUnitTest
```

- **`:domain`** — couvre le calcul des séries (jours consécutifs, fréquences par période, gel de série, cas limites de fuseau horaire/année bissextile), la formule d'XP et de niveaux, le score de régularité, la notation des jours du calendrier, le déblocage des succès, la génération des quêtes, la sérialisation de l'export JSON.
- **`:app`** — tests Room (via Robolectric) qui vérifient que les types polymorphes (`Frequency`, `HabitTarget`) survivent réellement à un aller-retour dans SQLite, que l'archivage/désarchivage met à jour les bons champs, et que les requêtes par plage de dates comparent correctement des dates ISO (y compris à cheval sur un changement d'année).

## Intégration continue

Le workflow [`android-ci.yml`](./.github/workflows/android-ci.yml) s'exécute sur chaque push et chaque pull request : il fait tourner les deux suites de tests, construit l'APK de debug **et** l'APK de release, puis publie les deux comme artefacts téléchargeables (onglet *Actions* du dépôt). C'est la vérification de référence pour le module `:app`, qui a été développé sans accès à un SDK Android en local.

## Export / Import des données

Accessible depuis **Profil → Réglages → Données** :

- **Export JSON** : instantané complet (profil, habitudes, complétions, succès débloqués, statistiques journalières, humeurs, entrées de journal, objectifs, défis, routines) dans un fichier `.json` versionné (numéro de schéma), à sauvegarder où l'utilisateur le souhaite via le sélecteur de fichiers système.
- **Export CSV** : liste plate des complétions (`date, habitude, complété, valeur, horodatage`), pratique pour une analyse dans un tableur.
- **Import JSON** : réinjecte un export précédent en mode fusion (les habitudes/complétions existantes sont conservées, les nouvelles sont ajoutées).
- **Réinitialisation** : efface toutes les données locales (habitudes, complétions, statistiques...) après confirmation ; les préférences (thème, langue...) ne sont pas concernées.

Aucun de ces flux ne transite par un serveur : tout se passe entre l'application et le fichier choisi par l'utilisateur sur son appareil.

## Confidentialité

- Le manifeste ne demande **aucune permission `INTERNET`** : l'application ne peut techniquement rien envoyer sur le réseau.
- Aucun compte, aucune authentification.
- Toutes les données restent dans la base Room locale et les préférences DataStore de l'appareil.
- Le détail est visible dans l'application (Profil → Confidentialité) et correspond exactement à ce qui est décrit ci-dessus.

## Internationalisation

Toutes les chaînes affichées sont externalisées dans `res/values/strings.xml` (français, langue par défaut) et `res/values-en/strings.xml` (anglais) — les deux fichiers couvrent exactement le même jeu de clés. Le réglage « Langue » (Système / Français / English) prend effet immédiatement, sans redémarrer l'application.
