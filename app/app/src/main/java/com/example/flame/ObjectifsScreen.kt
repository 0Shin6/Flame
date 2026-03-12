package com.example.flame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flame.ui.theme.FlameCard
import com.example.flame.ui.theme.FlamePurple

@Composable
fun ObjectifsScreen(
    objectifs: List<Objectif>,
    taches: List<Tache>,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Objectif) -> Unit
) {
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
                "Tes objectifs,",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(objectifs) { obj ->
                    val objTasks = taches.filter { it.objectifId == obj.id }
                    val progress = if (objTasks.isEmpty()) 0f 
                    else objTasks.count { it.etat == EtatTache.FAIT }.toFloat() / objTasks.size

                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onNavigateToDetail(obj) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = FlameCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(obj.emoji, fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = obj.titre,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = FlamePurple,
                                trackColor = Color.White.copy(0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailObjectifScreen(
    objectif: Objectif,
    taches: List<Tache>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleEtat: (Tache) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = objectif.titre,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(Color.Red.copy(0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = objectif.description,
            color = Color.White.copy(0.7f),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 48.dp)
        )
        Spacer(Modifier.height(24.dp))

        val progress = if (taches.isEmpty()) 0f 
        else taches.count { it.etat == EtatTache.FAIT }.toFloat() / taches.size

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = FlamePurple,
            trackColor = Color.White.copy(0.1f)
        )
        
        Spacer(Modifier.height(24.dp))
        Text(
            "Tâches de l'objectif",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            items(taches) { tache ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
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
                        }
                        BadgeEtat(tache)
                    }
                }
            }
        }
    }
}

@Composable
fun AjoutObjectifScreen(
    objectifExistant: Objectif? = null,
    onSave: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var titre by remember { mutableStateOf(objectifExistant?.titre ?: "") }
    var description by remember { mutableStateOf(objectifExistant?.description ?: "") }
    var emoji by remember { mutableStateOf(objectifExistant?.emoji ?: "🎯") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (objectifExistant == null) "Créer un objectif" else "Modifier l'objectif",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = emoji,
            onValueChange = { emoji = it },
            label = { Text("Sélectionne un émoji", color = Color.White.copy(0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = FlamePurple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = titre,
            onValueChange = { titre = it },
            label = { Text("Titre", color = Color.White.copy(0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = FlamePurple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description", color = Color.White.copy(0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = FlamePurple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { if (titre.isNotBlank()) onSave(titre, description, emoji) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlamePurple),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(if (objectifExistant == null) "Créer l'objectif" else "Enregistrer")
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
