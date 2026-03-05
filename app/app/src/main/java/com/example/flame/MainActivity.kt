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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
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
import com.example.flame.ui.theme.FlameTheme
import java.util.Calendar

// --- COULEURS DU DESIGN FLAME ---
val FlameBackground = Color(0xFF1A0033)
val FlameCard = Color(0xFF2E004B)
val FlamePurple = Color(0xFF9D00FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlameTheme {
                // La Surface garantit que le fond n'est pas noir et occupe tout l'écran
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FlameBackground
                ) {
                    val navController = rememberNavController()
                    // Liste d'état pour simuler les données du diagramme
                    val taches = remember {
                        mutableStateListOf("Morning routine", "Work out", "Reading")
                    }

                    NavHost(navController = navController, startDestination = "liste") {
                        composable("liste") {
                            EcranListeTaches(
                                taches = taches,
                                onNavigateToAdd = { navController.navigate("ajout") }
                            )
                        }
                        composable("ajout") {
                            EcranAjoutTache(
                                onTaskCreated = { nouveauTitre ->
                                    taches.add(nouveauTitre)
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

// --- ÉCRAN 1 : LISTE DES TÂCHES ---
@Composable
fun EcranListeTaches(taches: List<String>, onNavigateToAdd: () -> Unit) {
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
                text = "Bonjour 2pac 👋",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("Vos tâches", color = Color.White, fontSize = 20.sp)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                items(taches) { item ->
                    Button(
                        onClick = { /* Action au clic */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlameCard),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(item, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// --- ÉCRAN 2 : CRÉER UNE TÂCHE ---
@Composable
fun EcranAjoutTache(onTaskCreated: (String) -> Unit, onBack: () -> Unit) {
    // États correspondants aux attributs du diagramme UML
    var titre by remember { mutableStateOf("") }       // nom_tache
    var description by remember { mutableStateOf("") } // description
    var touteLaJournee by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var dateVal by remember { mutableStateOf("11/01/2026") }
    var heureDebut by remember { mutableStateOf("10:00") } // debut
    var heureFin by remember { mutableStateOf("10:20") }   // fin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlameBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Créer une tâche",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Champ Titre (nom_tache)
        OutlinedTextField(
            value = titre,
            onValueChange = { titre = it },
            label = { Text("Titre", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = FlamePurple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Champ Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = FlamePurple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bloc Date et Heures (debut et fin)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FlameCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                LignePicker(Icons.Default.DateRange, "Date", dateVal) {
                    DatePickerDialog(context, { _, y, m, d -> dateVal = "$d/${m+1}/$y" }, 2026, 0, 11).show()
                }
                LignePicker(Icons.Default.Schedule, "Heure de début", heureDebut) {
                    TimePickerDialog(context, { _, h, m -> heureDebut = String.format("%02d:%02d", h, m) }, 10, 0, true).show()
                }
                LignePicker(Icons.Default.Schedule, "Heure de fin", heureFin) {
                    TimePickerDialog(context, { _, h, m -> heureFin = String.format("%02d:%02d", h, m) }, 10, 20, true).show()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Option "Toute la journée"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FlameCard, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Toute la journée", color = Color.White, modifier = Modifier.weight(1f))
            Switch(
                checked = touteLaJournee,
                onCheckedChange = { touteLaJournee = it },
                colors = SwitchDefaults.colors(checkedThumbColor = FlamePurple)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Boutons de validation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { if (titre.isNotBlank()) onTaskCreated(titre) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlamePurple),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Créer la tâche", fontWeight = FontWeight.Bold)
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}