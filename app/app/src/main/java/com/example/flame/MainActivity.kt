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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
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

enum class EtatTache { A_FAIRE, EN_RETARD, FAIT }

data class Tache(
    val id: Long = System.currentTimeMillis(),
    val titre: String,
    val description: String = "",
    val dateDebut: Long,
    val dateFin: Long,
    val etat: EtatTache = EtatTache.A_FAIRE
)

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
                            //On trie la liste par date de début avant de l'afficher
                            ListeTaches(
                                taches = taches.sortedBy { it.dateDebut },
                                //Navigation vers AjoutTache avec un index pour la modification
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
                                onTaskAction = { nouveauTitre: String, nouvelleDesc: String, timestamp: Long ->
                                    val tacheMaj = Tache(
                                        titre = nouveauTitre,
                                        description = nouvelleDesc,
                                        dateDebut = timestamp,
                                        dateFin = timestamp + 3600000                                     )
                                    if (index != null) {
                                        taches[index] = tacheMaj
                                    } else {
                                        taches.add(tacheMaj)
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
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToAdd: () -> Unit
) {
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
            Text("Vos tâches", color = Color.White, fontSize = 20.sp)

            // Affichage des tâches
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                taches.forEachIndexed { index, tache ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable { onNavigateToEdit(index) },
                            shape = RoundedCornerShape(25.dp),
                            colors = CardDefaults.cardColors(containerColor = FlameCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    tache.titre,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                //Formatage de l'heure à partir du timestamp
                                val cal =
                                    Calendar.getInstance().apply { timeInMillis = tache.dateDebut }
                                Text(
                                    text = String.format(
                                        "%02d:%02d",
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE)
                                    ),
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AjoutTache(
    tacheExistante: Tache? = null,
    onTaskAction: (String, String, Long) -> Unit,
    onBack: () -> Unit
) {
    //Initialisation des états avec les données de la tâche existante si on est en modification
    var titre by remember { mutableStateOf(tacheExistante?.titre ?: "") }
    var desc by remember { mutableStateOf(tacheExistante?.description ?: "") }

    val calendar = remember {
        Calendar.getInstance().apply {
            if (tacheExistante != null) timeInMillis = tacheExistante.dateDebut
        }
    }
    val context = LocalContext.current
    var dateTexte by remember { mutableStateOf(if (tacheExistante != null) "Date modifiée" else "Choisir la date") }
    var heureDebutTexte by remember { mutableStateOf(if (tacheExistante != null) "Heure modifiée" else "Choisir l'heure de début") }
    var heureFinTexte by remember { mutableStateOf(if (tacheExistante != null) "Heure modifiée" else "Choisir l'heure de fin") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlameBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Changement du titre de la page en fonction du contexte
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
                            calendar.set(y, m, d)
                            dateTexte = "$d/${m + 1}/$y"
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                //Picker pour l'heure de début
                LignePicker(Icons.Default.Schedule, "Heure de début", heureDebutTexte) {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            calendar.set(Calendar.HOUR_OF_DAY, h)
                            calendar.set(Calendar.MINUTE, m)
                            heureDebutTexte = String.format("%02d:%02d", h, m)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                }
                //Picker pour l'heure de fin
                LignePicker(Icons.Default.Schedule, "Heure de fin", heureFinTexte) {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            // Ici vous pouvez gérer la logique pour dateFin si nécessaire
                            heureFinTexte = String.format("%02d:%02d", h, m)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (titre.isNotBlank()) onTaskAction(
                        titre,
                        desc,
                        calendar.timeInMillis
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlamePurple),
                shape = RoundedCornerShape(28.dp)
            ) {
                //Texte du bouton s'adaptant au contexte
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