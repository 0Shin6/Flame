package com.example.flame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flame.ui.theme.FlameBackground
import com.example.flame.ui.theme.FlameCard
import com.example.flame.ui.theme.FlamePurple
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendrierScreen(
    taches: List<Tache>,
    onToggleEtat: (Tache) -> Unit,
    onNavigateToEdit: (Tache) -> Unit
) {
    var dateSelectionnee by remember { mutableStateOf(Calendar.getInstance()) }
    
    val tachesDuJour = taches.filter { tache ->
        val calTache = Calendar.getInstance().apply { timeInMillis = tache.dateDebut }
        calTache.get(Calendar.YEAR) == dateSelectionnee.get(Calendar.YEAR) &&
        calTache.get(Calendar.DAY_OF_YEAR) == dateSelectionnee.get(Calendar.DAY_OF_YEAR)
    }

    Column(modifier = Modifier.fillMaxSize().background(FlameBackground).padding(24.dp)) {
        Text("Calendrier", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FlameCard)
        ) {
            CalendarWidget(dateSelectionnee) { nouvelleDate ->
                dateSelectionnee = nouvelleDate
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Tâches du jour", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        if (tachesDuJour.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Aucune tâche pour ce jour", color = Color.White.copy(0.5f))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp).weight(1f)
            ) {
                items(tachesDuJour) { tache ->
                    TacheMiniCard(tache, onToggleEtat, onNavigateToEdit)
                }
            }
        }
    }
}

@Composable
fun CalendarWidget(dateSelectionnee: Calendar, onDateSelected: (Calendar) -> Unit) {
    val currentMonth = remember(dateSelectionnee) { dateSelectionnee.clone() as Calendar }
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time).replaceFirstChar { it.uppercase() }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newDate = dateSelectionnee.clone() as Calendar
                newDate.add(Calendar.MONTH, -1)
                onDateSelected(newDate)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
            }
            Text(monthName, color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                val newDate = dateSelectionnee.clone() as Calendar
                newDate.add(Calendar.MONTH, 1)
                onDateSelected(newDate)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(0.5f),
                    fontSize = 12.sp
                )
            }
        }

        val firstDayOfMonth = dateSelectionnee.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val dayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Ajustement pour commencer par Lundi
        val daysInMonth = dateSelectionnee.getActualMaximum(Calendar.DAY_OF_MONTH)

        var dayCounter = 1
        for (i in 0..5) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0..6) {
                    val currentDayIndex = i * 7 + j
                    if (currentDayIndex >= dayOfWeek && dayCounter <= daysInMonth) {
                        val day = dayCounter
                        val isSelected = dateSelectionnee.get(Calendar.DAY_OF_MONTH) == day
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(if (isSelected) FlamePurple else Color.Transparent, CircleShape)
                                .clickable {
                                    val newDate = dateSelectionnee.clone() as Calendar
                                    newDate.set(Calendar.DAY_OF_MONTH, day)
                                    onDateSelected(newDate)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), color = Color.White, fontSize = 14.sp)
                        }
                        dayCounter++
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
            if (dayCounter > daysInMonth) break
        }
    }
}

@Composable
fun TacheMiniCard(tache: Tache, onToggleEtat: (Tache) -> Unit, onNavigateToEdit: (Tache) -> Unit) {
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
