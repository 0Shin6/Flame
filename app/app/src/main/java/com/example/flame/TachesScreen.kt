package com.example.flame

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import com.example.flame.ui.theme.FlameBackground
import com.example.flame.ui.theme.FlameCard
import com.example.flame.ui.theme.FlamePurple
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeTachesScreen(
    taches: List<Tache>,
    onToggleEtat: (Tache) -> Unit,
    onDeleteTache: (Tache) -> Unit,
    onNavigateToEdit: (Tache) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    var filtreSelectionne by remember { mutableStateOf<EtatTache?>(null) }
    var menuOuvert by remember { mutableStateOf(false) }

    val tachesFiltrees = if (filtreSelectionne == null) taches
    else taches.filter { obtenirEtat(it) == filtreSelectionne }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = FlamePurple,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = Color.White)
            }
        },
        containerColor = Color.Transparent
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
                        ?.lowercase()
                        ?.replace("_", " ")
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
                        onClick = { filtreSelectionne = null; menuOuvert = false }
                    )
                    EtatTache.entries.forEach { etat ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = etat.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }, 
                                    color = Color.White
                                ) 
                            },
                            onClick = { filtreSelectionne = etat; menuOuvert = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Vos tâches", color = Color.White, fontSize = 20.sp)

            if (tachesFiltrees.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune tâche", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    items(tachesFiltrees, key = { it.id }) { tache ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    onDeleteTache(tache)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Red, RoundedCornerShape(25.dp)).padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            content = {
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(72.dp).clickable { onNavigateToEdit(tache) },
                                    shape = RoundedCornerShape(25.dp),
                                    colors = CardDefaults.cardColors(containerColor = FlameCard)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { onToggleEtat(tache) }) {
                                            Icon(
                                                imageVector = if (tache.etat == EtatTache.FAIT) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                tint = FlamePurple,
                                                contentDescription = null
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tache.titre, color = Color.White, fontWeight = FontWeight.Medium)
                                            if (tache.periodicite != Periodicite.AUCUNE) {
                                                Text(tache.periodicite.name.lowercase().replace("_", " "), color = Color.White.copy(0.6f), fontSize = 10.sp)
                                            }
                                        }
                                        BadgeEtat(tache)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AjoutTacheScreen(
    tacheExistante: Tache? = null,
    objectifs: List<Objectif>,
    onTaskAction: (String, String, Long, Long, Boolean, Int?, Periodicite, Priorite, Long?) -> Unit,
    onTaskDelete: () -> Unit,
    onBack: () -> Unit
) {
    var titre by remember { mutableStateOf(tacheExistante?.titre ?: "") }
    var desc by remember { mutableStateOf(tacheExistante?.description ?: "") }
    var estTouteJournee by remember { mutableStateOf(tacheExistante?.estTouteLaJournee ?: false) }
    var rappelSelectionne by remember { mutableStateOf(tacheExistante?.rappels) }
    var perioSelectionnee by remember { mutableStateOf(tacheExistante?.periodicite ?: Periodicite.AUCUNE) }
    var prioriteSelectionnee by remember { mutableStateOf(tacheExistante?.priorite ?: Priorite.FAIBLE) }
    var objectifIdSelectionne by remember { mutableStateOf(tacheExistante?.objectifId) }
    
    var menuRappelOuvert by remember { mutableStateOf(false) }
    var menuObjOuvert by remember { mutableStateOf(false) }

    val calDebut = remember { Calendar.getInstance().apply { if (tacheExistante != null) timeInMillis = tacheExistante.dateDebut } }
    val calFin = remember { Calendar.getInstance().apply { if (tacheExistante != null) timeInMillis = tacheExistante.dateFin } }
    val context = LocalContext.current

    var dateTexte by remember { mutableStateOf(tacheExistante?.let { "${calDebut.get(Calendar.DAY_OF_MONTH)}/${calDebut.get(Calendar.MONTH) + 1}" } ?: "Choisir") }
    var heureDebutTexte by remember { mutableStateOf(tacheExistante?.let { String.format("%02d:%02d", calDebut.get(Calendar.HOUR_OF_DAY), calDebut.get(Calendar.MINUTE)) } ?: "Choisir") }
    var heureFinTexte by remember { mutableStateOf(tacheExistante?.let { String.format("%02d:%02d", calFin.get(Calendar.HOUR_OF_DAY), calFin.get(Calendar.MINUTE)) } ?: "Choisir") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlameBackground)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (tacheExistante == null) "Créer une tâche" else "Modifier la tâche", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = titre, onValueChange = { titre = it }, label = { Text("Titre", color = Color.White.copy(0.7f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = FlamePurple, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description", color = Color.White.copy(0.7f)) }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = FlamePurple, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = FlameCard)) {
            Column(modifier = Modifier.padding(16.dp)) {
                LignePicker(Icons.Default.DateRange, "Date", dateTexte) {
                    DatePickerDialog(context, { _, y, m, d ->
                        calDebut.set(y, m, d); calFin.set(y, m, d)
                        dateTexte = "$d/${m + 1}"
                    }, calDebut.get(Calendar.YEAR), calDebut.get(Calendar.MONTH), calDebut.get(Calendar.DAY_OF_MONTH)).show()
                }
                LignePicker(Icons.Default.AccessTime, "Heure de début", if (estTouteJournee) "Toute la journée" else heureDebutTexte, enabled = !estTouteJournee) {
                    TimePickerDialog(context, { _, h, m ->
                        calDebut.set(Calendar.HOUR_OF_DAY, h); calDebut.set(Calendar.MINUTE, m)
                        heureDebutTexte = String.format("%02d:%02d", h, m)
                    }, calDebut.get(Calendar.HOUR_OF_DAY), calDebut.get(Calendar.MINUTE), true).show()
                }
                LignePicker(Icons.Default.AccessTime, "Heure de fin", if (estTouteJournee) "Toute la journée" else heureFinTexte, enabled = !estTouteJournee) {
                    TimePickerDialog(context, { _, h, m ->
                        calFin.set(Calendar.HOUR_OF_DAY, h); calFin.set(Calendar.MINUTE, m)
                        heureFinTexte = String.format("%02d:%02d", h, m)
                    }, calFin.get(Calendar.HOUR_OF_DAY), calFin.get(Calendar.MINUTE), true).show()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Toute la journée", color = Color.White)
            }
            Switch(checked = estTouteJournee, onCheckedChange = { estTouteJournee = it }, colors = SwitchDefaults.colors(checkedThumbColor = FlamePurple))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = FlameCard)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box {
                    LignePicker(Icons.Default.Notifications, "Rappels", rappelSelectionne?.let { "$it mn avant" } ?: "Aucun") { menuRappelOuvert = true }
                    DropdownMenu(expanded = menuRappelOuvert, onDismissRequest = { menuRappelOuvert = false }, modifier = Modifier.background(FlameCard)) {
                        listOf(null, 5, 15, 30, 60).forEach { mins ->
                            DropdownMenuItem(text = { Text(mins?.let { "$it mn avant" } ?: "Aucun", color = Color.White) }, onClick = { rappelSelectionne = mins; menuRappelOuvert = false })
                        }
                    }
                }
                Box {
                    val currentObj = objectifs.find { it.id == objectifIdSelectionne }
                    LignePicker(Icons.AutoMirrored.Filled.List, "Objectif", currentObj?.titre ?: "Ajouté à aucun objectif") { menuObjOuvert = true }
                    DropdownMenu(expanded = menuObjOuvert, onDismissRequest = { menuObjOuvert = false }, modifier = Modifier.background(FlameCard)) {
                        DropdownMenuItem(text = { Text("Aucun", color = Color.White) }, onClick = { objectifIdSelectionne = null; menuObjOuvert = false })
                        objectifs.forEach { obj ->
                            DropdownMenuItem(text = { Text(obj.titre, color = Color.White) }, onClick = { objectifIdSelectionne = obj.id; menuObjOuvert = false })
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Priorité de la tâche", color = Color.White, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        PrioriteSelector(selected = prioriteSelectionnee, onSelect = { prioriteSelectionnee = it })

        Spacer(modifier = Modifier.height(24.dp))
        
        if (tacheExistante != null) {
            TextButton(onClick = onTaskDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Supprimer la tâche", color = Color.Red, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { if (titre.isNotBlank()) onTaskAction(titre, desc, calDebut.timeInMillis, calFin.timeInMillis, estTouteJournee, rappelSelectionne, perioSelectionnee, prioriteSelectionnee, objectifIdSelectionne) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlamePurple),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(if (tacheExistante == null) "Créer la tâche" else "Enregistrer")
            }
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, FlamePurple)
            ) {
                Text("Annuler", color = Color.White)
            }
        }
    }
}
