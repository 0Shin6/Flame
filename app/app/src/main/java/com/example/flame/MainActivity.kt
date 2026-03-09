package com.example.flame

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flame.ui.theme.FlameTheme
import java.util.*

// Les différents états d'une tâche
enum class EtatTache { A_FAIRE, EN_RETARD, FAIT, EN_COURS }

data class Tache(
    val id: Long = System.currentTimeMillis(),
    val titre: String,
    val description: String = "",
    val dateDebut: Long,
    val dateFin: Long,
    val etat: EtatTache = EtatTache.A_FAIRE
)

fun obtenirEtat(tache: Tache): EtatTache {
    val maintenant = System.currentTimeMillis()
    return when {
        tache.etat == EtatTache.FAIT -> EtatTache.FAIT
        maintenant > tache.dateFin -> EtatTache.EN_RETARD
        maintenant in tache.dateDebut..tache.dateFin -> EtatTache.EN_COURS
        else -> EtatTache.A_FAIRE
    }
}

// Design global du projet
val FlameBackground = Color(0xFF1A0033)
val FlameCard = Color(0xFF2E004B)
val FlamePurple = Color(0xFF9D00FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlameTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FlameBackground) {
                    val navController = rememberNavController()
                    //Mets à jour l'IHM quand une tâche est ajouté / modifié
                    val taches = remember { mutableStateListOf<Tache>() }

                    NavHost(navController = navController, startDestination = "liste") {
                        composable("liste") {
                            // On trie la liste par date de début avant de l'afficher
                            ListeTaches(
                                taches = taches.sortedBy { it.dateDebut },
                                //Action de bascule entre fait et à faire
                                onToggleEtat = { indexOriginal ->
                                    val t = taches[indexOriginal]
                                    val nouvelEtat = if (t.etat == EtatTache.FAIT) EtatTache.A_FAIRE else EtatTache.FAIT
                                    taches[indexOriginal] = t.copy(etat = nouvelEtat)
                                },
                                // Navigation vers AjoutTache avec un index pour la modification
                                onNavigateToEdit = { index: Int -> navController.navigate("ajout?index=$index") },
                                onNavigateToAdd = { navController.navigate("ajout") }
                            )
                        }

                        //Route permettant l'ajout et la modification d'une tâche
                        composable(
                            route = "ajout?index={index}",
                            arguments = listOf(navArgument("index") { nullable = true })
                        ) { backStackEntry ->
                            val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                            val tacheExistante = if (index != null) taches[index] else null

                            AjoutTache(
                                tacheExistante = tacheExistante,
                                onTaskAction = { nouveauTitre: String, nouvelleDesc: String, timestampDebut: Long, timestampFin: Long ->
                                    val tacheMaj = Tache(
                                        titre = nouveauTitre,
                                        description = nouvelleDesc,
                                        dateDebut = timestampDebut,
                                        dateFin = timestampFin,
                                        etat = tacheExistante?.etat ?: EtatTache.A_FAIRE
                                    )
                                    if (index != null) {
                                        taches[index] = tacheMaj //Tâche à modifier
                                    } else {
                                        taches.add(tacheMaj) // Création de la tâche
                                    }
                                    navController.popBackStack()
                                },
                                // Supprimer la tâche
                                onTaskDelete = {
                                    if (index != null) {
                                        taches.removeAt(index)
                                    }
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListeTaches(
    taches: List<Tache>,
    onToggleEtat: (Int) -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    //État pour mémoriser le filtre
    var filtreSelectionne by remember { mutableStateOf<EtatTache?>(null) }
    var menuOuvert by remember { mutableStateOf(false) }

    val tachesFiltrees = if (filtreSelectionne == null) taches
    else taches.filter { obtenirEtat(it) == filtreSelectionne }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = FlamePurple,
                shape = RoundedCornerShape(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = Color.White)
            }
        },
        containerColor = FlameBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                "Bonjour 2pac 👋",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { menuOuvert = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FlamePurple.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    val labelBouton = filtreSelectionne?.name
                        ?.replace("_", " ")
                        ?.lowercase()
                        ?.replaceFirstChar { it.uppercase() }
                        ?: "Tous les états"

                    Text(labelBouton, fontSize = 14.sp)
                }

                DropdownMenu(
                    expanded = menuOuvert,
                    onDismissRequest = { menuOuvert = false },
                    modifier = Modifier.background(FlameCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("Tous", color = Color.White) },
                        onClick = {
                            filtreSelectionne = null
                            menuOuvert = false
                        }
                    )

                    EtatTache.entries.forEach { etat ->
                        DropdownMenuItem(
                            text = {
                                val labelMenu = etat.name
                                    .replace("_", " ")
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() }
                                Text(labelMenu, color = Color.White)
                            },
                            onClick = {
                                filtreSelectionne = etat
                                menuOuvert = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Vos tâches", color = Color.White, fontSize = 20.sp)

            //Affichage du message "Aucune tâche" si le filtre est vide
            if (tachesFiltrees.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune tâche", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    items(tachesFiltrees) { tache ->
                        val indexOriginal = taches.indexOf(tache)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clickable { onNavigateToEdit(indexOriginal) },
                            shape = RoundedCornerShape(25.dp),
                            colors = CardDefaults.cardColors(containerColor = FlameCard)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onToggleEtat(indexOriginal) }) {
                                    Icon(
                                        imageVector = if (tache.etat == EtatTache.FAIT) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        tint = FlamePurple,
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    tache.titre,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Medium
                                )
                                BadgeEtat(tache)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeEtat(tache: Tache) {
    val maintenant = System.currentTimeMillis()
    val etatReel = obtenirEtat(tache)

    val (label, color) = when (etatReel) {
        EtatTache.FAIT -> {
            if (maintenant < tache.dateDebut) "En avance" to Color(0xFFE91E63)
            else "Fait" to Color(0xFFB39DDB)
        }
        EtatTache.EN_RETARD -> "En retard" to Color(0xFF9C27B0)
        EtatTache.EN_COURS -> "En cours" to Color(0xFFFF9800)
        else -> {
            val differenceMn = (tache.dateDebut - maintenant) / 60000
            if (differenceMn < 60) {
                "Dans ${differenceMn}mn" to Color(0xFF03A9F4)
            } else {
                val cal = Calendar.getInstance().apply { timeInMillis = tache.dateDebut }
                String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) to Color(0xFF9E9E9E)
            }
        }
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AjoutTache(
    tacheExistante: Tache? = null,
    onTaskAction: (String, String, Long, Long) -> Unit,
    onTaskDelete: () -> Unit,
    onBack: () -> Unit
) {
    var titre by remember { mutableStateOf(tacheExistante?.titre ?: "") }
    var desc by remember { mutableStateOf(tacheExistante?.description ?: "") }

    val calDebut = remember {
        Calendar.getInstance().apply {
            if (tacheExistante != null) timeInMillis = tacheExistante.dateDebut
        }
    }
    val calFin = remember {
        Calendar.getInstance().apply {
            if (tacheExistante != null) timeInMillis = tacheExistante.dateFin
        }
    }
    val context = LocalContext.current

    var dateTexte by remember { mutableStateOf(tacheExistante?.let { "${calDebut.get(Calendar.DAY_OF_MONTH)}/${calDebut.get(Calendar.MONTH) + 1}" } ?: "Choisir la date") }
    var heureDebutTexte by remember { mutableStateOf(tacheExistante?.let { String.format("%02d:%02d", calDebut.get(Calendar.HOUR_OF_DAY), calDebut.get(Calendar.MINUTE)) } ?: "Choisir l'heure de début") }
    var heureFinTexte by remember { mutableStateOf(tacheExistante?.let { String.format("%02d:%02d", calFin.get(Calendar.HOUR_OF_DAY), calFin.get(Calendar.MINUTE)) } ?: "Choisir l'heure de fin") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlameBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (tacheExistante == null) "Créer une tâche" else "Modifier la tâche",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = titre, onValueChange = { titre = it },
            label = { Text("Titre", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = FlamePurple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = desc, onValueChange = { desc = it },
            label = { Text("Description", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = FlamePurple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FlameCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                LignePicker(Icons.Default.DateRange, "Date", dateTexte) {
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            calDebut.set(y, m, d)
                            calFin.set(y, m, d)
                            dateTexte = "$d/${m + 1}"
                        },
                        calDebut.get(Calendar.YEAR),
                        calDebut.get(Calendar.MONTH),
                        calDebut.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                LignePicker(Icons.Default.Schedule, "Heure de début", heureDebutTexte) {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            calDebut.set(Calendar.HOUR_OF_DAY, h)
                            calDebut.set(Calendar.MINUTE, m)
                            heureDebutTexte = String.format("%02d:%02d", h, m)
                        },
                        calDebut.get(Calendar.HOUR_OF_DAY),
                        calDebut.get(Calendar.MINUTE),
                        true
                    ).show()
                }
                LignePicker(Icons.Default.Schedule, "Heure de fin", heureFinTexte) {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            calFin.set(Calendar.HOUR_OF_DAY, h)
                            calFin.set(Calendar.MINUTE, m)
                            heureFinTexte = String.format("%02d:%02d", h, m)
                        },
                        calFin.get(Calendar.HOUR_OF_DAY),
                        calFin.get(Calendar.MINUTE),
                        true
                    ).show()
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (tacheExistante != null) {
            TextButton(
                onClick = onTaskDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Supprimer la tâche", color = Color.Red, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (titre.isNotBlank()) onTaskAction(
                        titre,
                        desc,
                        calDebut.timeInMillis,
                        calFin.timeInMillis
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlamePurple),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(if (tacheExistante == null) "Créer" else "Enregistrer")
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, FlamePurple)
            ) {
                Text("Annuler", color = Color.White)
            }
        }
    }
}

@Composable
fun LignePicker(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}