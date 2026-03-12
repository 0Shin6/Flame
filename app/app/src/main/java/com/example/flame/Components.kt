// Ce fichier regroupe l'ensemble des composants de l'application

package com.example.flame

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flame.ui.theme.FlameBackground
import com.example.flame.ui.theme.FlamePurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun LoadingScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlameBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_flame_custom),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Flame",
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Quel est ton objectif du jour ?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BadgeEtat(tache: Tache) {
    val etat = obtenirEtat(tache)
    val (label, color) = when {
        etat == EtatTache.FAIT -> "Fait" to Color(0xFFB39DDB)
        tache.estTouteLaJournee -> "Aujourd'hui" to FlamePurple
        etat == EtatTache.EN_RETARD -> "En retard" to Color(0xFF9C27B0)
        etat == EtatTache.EN_COURS -> "En cours" to Color(0xFFFF9800)
        else -> {
            val diffMn = (tache.dateDebut - System.currentTimeMillis()) / 60000
            if (diffMn < 60) "Dans ${diffMn}mn" to Color(0xFF03A9F4)
            else {
                val cal = Calendar.getInstance().apply { timeInMillis = tache.dateDebut }
                String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) to Color(0xFF9E9E9E)
            }
        }
    }

    Surface(color = color, shape = RoundedCornerShape(12.dp)) {
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
fun PrioriteSelector(selected: Priorite, onSelect: (Priorite) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFF2E004B), RoundedCornerShape(22.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Priorite.entries.forEach { p ->
            val isS = selected == p
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isS) {
                            if (p == Priorite.FAIBLE) Color(0xFFD1A3FF)
                            else if (p == Priorite.MODEREE) Color(0xFFA53DFF)
                            else Color(0xFF8A00FF)
                        } else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(p) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = p.label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isS) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun LignePicker(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (enabled) Color.White else Color.Gray)
        Spacer(Modifier.width(16.dp))
        Text(label, color = if (enabled) Color.White else Color.Gray, modifier = Modifier.weight(1f))
        Text(value, color = if (enabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EffetFlammes(onAnimationFinished: () -> Unit) {
    val flameCount = 20
    val iterations = remember { List(flameCount) { it } }
    var finishedCount by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        iterations.forEach { _ ->
            PetiteFlamme(onFinished = {
                finishedCount++
                if (finishedCount >= flameCount) onAnimationFinished()
            })
        }
    }
}

@Composable
fun PetiteFlamme(onFinished: () -> Unit) {
    val startX = remember { (0..1000).random().toFloat() }
    val duration = remember { (1200..2500).random() }
    val delay = remember { (0..800).random() }

    val animY = remember { Animatable(1.1f) }
    val animAlpha = remember { Animatable(1f) }
    val animScale = remember { Animatable(0.2f) }

    val painter = painterResource(id = R.drawable.ic_flame_custom)

    LaunchedEffect(Unit) {
        launch {
            animY.animateTo(-0.3f, tween(duration, delay, LinearEasing))
            onFinished()
        }
        launch {
            animAlpha.animateTo(0f, tween(duration, delay, LinearEasing))
        }
        launch {
            animScale.animateTo(1.2f, tween(duration, delay, LinearEasing))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val x = (startX / 1000f) * size.width
        val y = animY.value * size.height

        translate(left = x, top = y) {
            scale(animScale.value) {
                with(painter) {
                    draw(
                        size = Size(80.dp.toPx(), 100.dp.toPx()),
                        alpha = animAlpha.value
                    )
                }
            }
        }
    }
}
