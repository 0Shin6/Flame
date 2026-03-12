// Ce fichier regroupe l'ensemble des modèles de données de l'application


package com.example.flame

import java.util.*

enum class EtatTache { A_FAIRE, EN_RETARD, FAIT, EN_COURS }
enum class Periodicite { AUCUNE, QUOTIDIEN, HEBDOMADAIRE, MENSUEL }
enum class Priorite(val valeur: Int, val label: String) {
    FAIBLE(0, "Faible"), MODEREE(1, "Modérée"), FORTE(2, "Forte")
}

data class Tache(
    val id: Long = System.currentTimeMillis(),
    val titre: String,
    val description: String = "",
    val dateDebut: Long,
    val dateFin: Long,
    val etat: EtatTache = EtatTache.A_FAIRE,
    val estTouteLaJournee: Boolean = false,
    val rappels: Int? = null,
    val periodicite: Periodicite = Periodicite.AUCUNE,
    val priorite: Priorite = Priorite.FAIBLE,
    val objectifId: Long? = null
)

data class Objectif(
    val id: Long = System.currentTimeMillis(),
    val titre: String,
    val description: String = "",
    val emoji: String = "🎯"
)

fun obtenirEtat(tache: Tache): EtatTache {
    val maintenant = System.currentTimeMillis()
    if (tache.estTouteLaJournee) return if (tache.etat == EtatTache.FAIT) EtatTache.FAIT else EtatTache.A_FAIRE
    return when {
        tache.etat == EtatTache.FAIT -> EtatTache.FAIT
        maintenant > tache.dateFin -> EtatTache.EN_RETARD
        maintenant in tache.dateDebut..tache.dateFin -> EtatTache.EN_COURS
        else -> EtatTache.A_FAIRE
    }
}
