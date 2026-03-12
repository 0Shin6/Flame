# Flame - To-Do list - GRANIER Mathis GB


### 1. Gestion des Tâches
- **Création et Edition** : Ajouter des tâches avec titre, description, image, date et plage horaire.
- **Priorisation** : Définir un niveau d'importance de faible à forte pour chaque tâche.
- **Périodicité** : périodiser les tâches récurrentes. Une nouvelle tâche est générée automatiquement dès la validation de la précédente.
- **États des Tâches** : À faire, En cours, En retard, Fait.
- **Tri Intelligent** : Les tâches sont triées par heure, puis par niveau de priorisation.
- **Suppression par Swipe** : Mais aussi sur la page de modification.

### 2. Système d'Objectifs
- **Regroupement** : Lier des tâches à des objectifs spécifiques pour un meilleur suivi de projet.
- **Personnalisation** : Chaque objectif possède son propre titre, description et emoji représentatif.
- **Vue Détail** : Consulter la progression et la liste des tâches dédiées à un objectif.

### 3. Calendrier et Organisation
- **Vue Calendrier** : Visualisation globale des tâches planifiées.
- **Filtrage** : Filtrer les tâches par état.

### 4. Expérience Utilisateur (UX)
- **Splash Screen (Loading)** : Introduction fluide au lancement de l'application.
- **Effet Flame** : Animation de flammes montantes lors de la validation d'une tâche pour célébrer la réussite.
- **Notifications & Rappels** : Rappels programmables (5mn, 15mn, 30mn, 1h avant) pour ne jamais rien oublier.
- **Retour Haptique** : Vibration lorsqu'une tâche est valid".
- **Design Adaptatif** : Icône d'application personnalisée avec un logo.

## 🛠️ Comparatif Technique

| Type | Implémentation | Avantages |
| :--- | :--- | :--- |
| **Langage** | Kotlin | Code sécurisé et fait pour android |
| **Interface UI** | Jetpack Compose | UI réactive et facile à maintenir. |
| **Navigation** | Compose Navigation | Gestion fluide des transitions entre écrans et passage d'arguments. |
| **Stockage** | SharedPreferences + Gson | Stockage locale légère et rapide des données. |
| **Gestion d'Images** | Coil | Chargement d'images asynchrone avec gestion du cache. |
| **Architecture** | State-driven | Synchronisation en temps réel de l'UI |
| **Planification** | AlarmManager | Programmation précise des notifications même en arrière-plan. |
| **Gestion Temps** | Java Calendar API | Calcul des dates pour la périodicité et les tris. |