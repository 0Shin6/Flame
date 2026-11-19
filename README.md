# Flame - To-Do list - Projet universitaire

![Logo Flame](logo.svg)

**Flame** est une application mobile Android de gestion de tâches (To-Do list) et de suivi d'objectifs développée en **Kotlin** avec **Jetpack Compose**. Elle vise à rendre l'organisation quotidienne simple, fluide et motivante grâce à une interface moderne, un système d'objectifs structuré et des retours visuels et haptiques soignés.

---

## Fonctionnalités

### 1. Gestion des Tâches
- **Création et édition** : Ajout et modification de tâches avec titre, description, image, date, plage horaire ou mode journée entière.
- **Priorisation** : Trois niveaux d'importance (*Faible*, *Modérée*, *Forte*) pour hiérarchiser ses priorités.
- **Périodicité** : Planification de tâches récurrentes (*Quotidienne*, *Hebdomadaire*, *Mensuelle*) avec génération automatique de l'occurrence suivante dès la validation.
- **Suivi automatique des états** : Transition dynamique entre *À faire*, *En cours*, *En retard* et *Fait*.
- **Tri intelligent** : Classement automatique des tâches par horaire, puis par niveau de priorité.
- **Suppression rapide** : Suppression par glissement (*swipe*) directement depuis la liste ou depuis la page de modification.

### 2. Système d'Objectifs
- **Regroupement** : Association de plusieurs tâches à un même objectif pour suivre l'avancement d'un projet.
- **Personnalisation** : Titre, description et emoji dédiés pour chaque objectif.
- **Vue détaillée** : Consultation de la progression globale et de la liste des tâches rattachées.

### 3. Calendrier et Organisation
- **Vue Calendrier** : Visualisation globale des tâches planifiées jour par jour.
- **Filtrage** : Filtrage rapide des tâches selon leur état.

### 4. Expérience Utilisateur
- **Écran de chargement** : Animation d'accueil au lancement de l'application.
- **Effet Flame** : Animation de flammes montantes lors de la validation d'une tâche pour célébrer la réussite.
- **Notifications & rappels** : Rappels programmables pour ne manquer aucune échéance.
- **Retour haptique** : Vibration lors de la validation d'une tâche.
- **Identité visuelle** : Icône d'application adaptative personnalisée avec le logo Flame.

---

## Démonstration

Une vidéo de démonstration complète de l'application est disponible dans le dépôt : [demonstration.mp4](demonstration.mp4).

---

## Stack Technique

| Domaine | Technologie | Rôle / Avantages |
| :--- | :--- | :--- |
| **Langage** | Kotlin (`2.0.21`) | Langage moderne, concis et sécurisé, natif pour Android. |
| **Interface UI** | Jetpack Compose & Material 3 | Interface déclarative, réactive et facile à maintenir. |
| **Navigation** | Compose Navigation | Gestion fluide des transitions entre écrans et passage d'arguments. |
| **Stockage** | SharedPreferences + Gson | Persistance locale légère, simple et rapide des données. |
| **Gestion d'images** | Coil | Chargement asynchrone des images avec mise en cache intégrée. |
| **Architecture** | State-driven | Synchronisation en temps réel entre les données et l'interface. |
| **Planification** | AlarmManager | Déclenchement précis des rappels et notifications en arrière-plan. |
| **Gestion du temps** | Java Calendar API | Calcul des dates pour la périodicité, les états et les tris. |

---

## Structure du projet

```text
Flame/
├── logo.svg                        # Logo de l'application
├── demonstration.mp4               # Vidéo de démonstration
└── app/                            # Projet Android (Gradle)
    └── app/src/main/java/com/example/flame/
        ├── MainActivity.kt         # Point d'entrée et navigation principale
        ├── Models.kt               # Modèles de données (Tache, Objectif, états, priorités)
        ├── TachesScreen.kt         # Écran de gestion des tâches
        ├── ObjectifsScreen.kt      # Écran de gestion et suivi des objectifs
        ├── CalendarScreen.kt       # Vue calendrier et filtrage
        ├── Components.kt           # Composants UI réutilisables (animation Flame, cartes, etc.)
        ├── ReminderReceiver.kt     # Réception et affichage des notifications programmées
        └── ui/theme/               # Thème Material 3 (couleurs, typographie)
```

---

## Installation et lancement

### Prérequis
- **Android Studio** (version récente compatible AGP 9.0)
- **JDK 11** ou supérieur
- Un émulateur ou appareil physique sous **Android 7.0 (API 24)** minimum (cible : **API 36**)

### Étapes
1. Cloner le dépôt :
   ```bash
   git clone <url-du-depot>
   cd Flame
   ```
2. Ouvrir le dossier `app/` dans **Android Studio**.
3. Laisser Gradle synchroniser les dépendances.
4. Lancer l'application sur un émulateur ou un appareil Android via le bouton **Run**.
