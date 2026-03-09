package com.example.flame

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flame.ui.theme.FlameTheme
import kotlinx.coroutines.launch
import java.util.*

// Les différents états d'une tâche
enum class EtatTache { A_FAIRE, EN_RETARD, FAIT, EN_COURS }

// Périodicité des tâches
enum class Periodicite { AUCUNE, QUOTIDIEN, HEBDOMADAIRE, MENSUEL }

data class Tache(
    val id: Long = System.currentTimeMillis(),
    val titre: String,
    val description: String = "",
    val dateDebut: Long,
    val dateFin: Long,
    val etat: EtatTache = EtatTache.A_FAIRE,
    val estTouteLaJournee: Boolean = false,
    val rappels: Int? = null,
    val periodicite: Periodicite = Periodicite.AUCUNE
)

// Design global du projet
val FlameBackground = Color(0xFF1A0033)
val FlameCard = Color(0xFF2E004B)
val FlamePurple = Color(0xFF9D00FF)

fun obtenirEtat(tache: Tache): EtatTache {
    val maintenant = System.currentTimeMillis()

    //Si la tache dure toute la journée, on ne garde de fait ou à faire
    if (tache.estTouteLaJournee) {
        return if (tache.etat == EtatTache.FAIT) EtatTache.FAIT else EtatTache.A_FAIRE
    }
    return when {
        tache.etat == EtatTache.FAIT -> EtatTache.FAIT
        maintenant > tache.dateFin -> EtatTache.EN_RETARD
        maintenant in tache.dateDebut..tache.dateFin -> EtatTache.EN_COURS
        else -> EtatTache.A_FAIRE
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlameTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FlameBackground) {
                    val navController = rememberNavController()
                    //Mets à jour l'interface quand une tâche est ajouté / modifié
                    val taches = remember { mutableStateListOf<Tache>() }
                    var showFlameEffect by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(navController = navController, startDestination = "liste") {
                            composable("liste") {
                                ListeTaches(
                                    taches = taches.toList().sortedBy { it.dateDebut },
                                    onToggleEtat = { tache ->
                                        val index = taches.indexOfFirst { it.id == tache.id }
                                        if (index != -1) {
                                            val t = taches[index]
                                            val nouvelEtat = if (t.etat == EtatTache.FAIT) EtatTache.A_FAIRE else EtatTache.FAIT
                                            taches[index] = t.copy(etat = nouvelEtat)
                                            
                                            if (nouvelEtat == EtatTache.FAIT) {
                                                showFlameEffect = true
                                                vibrer(this@MainActivity)
                                            }
                                        }
                                    },
                                    onDeleteTache = { tache ->
                                        annulerNotifications(this@MainActivity, tache)
                                        taches.removeIf { it.id == tache.id }
                                    },
                                    onNavigateToEdit = { tache ->
                                        val index = taches.indexOfFirst { it.id == tache.id }
                                        navController.navigate("ajout?index=$index")
                                    },
                                    onNavigateToAdd = { navController.navigate("ajout") }
                                )
                            }

                            composable(
                                route = "ajout?index={index}",
                                arguments = listOf(navArgument("index") { nullable = true })
                            ) { backStackEntry ->
                                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                                val tacheExistante = if (index != null && index in taches.indices) taches[index] else null

                                AjoutTache(
                                    tacheExistante = tacheExistante,
                                    onTaskAction = { titre, desc, debut, fin, touteJournee, rappel, perio ->
                                        val tacheMaj = Tache(
                                            id = tacheExistante?.id ?: System.currentTimeMillis(),
                                            titre = titre,
                                            description = desc,
                                            dateDebut = debut,
                                            dateFin = fin,
                                            etat = tacheExistante?.etat ?: EtatTache.A_FAIRE,
                                            estTouteLaJournee = touteJournee,
                                            rappels = rappel,
                                            periodicite = perio
                                        )
                                        if (index != null && index in taches.indices) {
                                            taches[index] = tacheMaj
                                        } else {
                                            taches.add(tacheMaj)
                                        }
                                        
                                        programmerNotifications(this@MainActivity, tacheMaj)
                                        navController.popBackStack()
                                    },
                                    onTaskDelete = {
                                        if (index != null && index in taches.indices) {
                                            annulerNotifications(this@MainActivity, taches[index])
                                            taches.removeAt(index)
                                        }
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        if (showFlameEffect) {
                            EffetFlammes(onAnimationFinished = { showFlameEffect = false })
                        }
                    }
                }
            }
        }
    }

    private fun vibrer(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    //Cette fonction gère les notifications (rappels et retards)
    private fun programmerNotifications(context: Context, tache: Tache) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        tache.rappels?.let { mins ->
            val tempsRappel = tache.dateDebut - (mins * 60 * 1000)
            if (tempsRappel > System.currentTimeMillis()) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("TYPE", "RAPPEL")
                    putExtra("TITRE", tache.titre)
                    putExtra("DESC", tache.description)
                    putExtra("ID", tache.id)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, tache.id.toInt(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, tempsRappel, pendingIntent)
            }
        }

        if (tache.dateFin > System.currentTimeMillis()) {
            val intentRetard = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("TYPE", "RETARD")
                putExtra("TITRE", tache.titre)
                putExtra("ID", tache.id)
            }
            val pendingIntentRetard = PendingIntent.getBroadcast(
                context, (tache.id + 1).toInt(), intentRetard,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(alarmManager, tache.dateFin, pendingIntentRetard)
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, time: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
    }

    private fun annulerNotifications(context: Context, tache: Tache) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intentRappel = Intent(context, ReminderReceiver::class.java)
        PendingIntent.getBroadcast(context, tache.id.toInt(), intentRappel, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            alarmManager.cancel(it)
        }

        val intentRetard = Intent(context, ReminderReceiver::class.java)
        PendingIntent.getBroadcast(context, (tache.id + 1).toInt(), intentRetard, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            alarmManager.cancel(it)
        }
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
    
    val animatableY = remember { Animatable(1.1f) }
    val animatableAlpha = remember { Animatable(1f) }
    val animatableScale = remember { Animatable(0.2f) }

    val painter = painterResource(id = R.drawable.ic_flame_custom)

    LaunchedEffect(Unit) {
        launch {
            animatableY.animateTo(
                targetValue = -0.3f,
                animationSpec = tween(duration, delay, LinearEasing)
            )
            onFinished()
        }
        launch {
            animatableAlpha.animateTo(0f, tween(duration, delay, LinearEasing))
        }
        launch {
            animatableScale.animateTo(1.2f, tween(duration, delay, LinearEasing))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        val x = (startX / 1000f) * canvasWidth
        val y = animatableY.value * canvasHeight
        
        translate(left = x, top = y) {
            scale(animatableScale.value) {
                drawIntoCanvas {
                    with(painter) {
                        draw(
                            size = androidx.compose.ui.geometry.Size(100.dp.toPx(), 120.dp.toPx()),
                            alpha = animatableAlpha.value
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeTaches(
    taches: List<Tache>,
    onToggleEtat: (Tache) -> Unit,
    onDeleteTache: (Tache) -> Unit,
    onNavigateToEdit: (Tache) -> Unit,
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
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(25.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.White)
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            content = {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clickable { onNavigateToEdit(tache) },
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
                                            Text(
                                                tache.titre,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (tache.periodicite != Periodicite.AUCUNE) {
                                                Text(
                                                    tache.periodicite.name.lowercase().replaceFirstChar { it.uppercase() },
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 10.sp
                                                )
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
fun BadgeEtat(tache: Tache) {
    val maintenant = System.currentTimeMillis()
    val etatReel = obtenirEtat(tache)

    val (label, color) = when {
        etatReel == EtatTache.FAIT -> {
            if (!tache.estTouteLaJournee && maintenant < tache.dateDebut) "En avance" to Color(0xFFE91E63)
            else "Fait" to Color(0xFFB39DDB)
        }
        tache.estTouteLaJournee -> "Aujourd'hui" to FlamePurple
        etatReel == EtatTache.EN_RETARD -> "En retard" to Color(0xFF9C27B0)
        etatReel == EtatTache.EN_COURS -> "En cours" to Color(0xFFFF9800)
        else -> {
            val differenceMn = (tache.dateDebut - maintenant) / 60000
            if (differenceMn < 60) {
                //Débute dans moins d'1h
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
    onTaskAction: (String, String, Long, Long, Boolean, Int?, Periodicite) -> Unit,
    onTaskDelete: () -> Unit,
    onBack: () -> Unit
) {
    var titre by remember { mutableStateOf(tacheExistante?.titre ?: "") }
    var desc by remember { mutableStateOf(tacheExistante?.description ?: "") }
    var estTouteJournee by remember { mutableStateOf(tacheExistante?.estTouteLaJournee ?: false) }
    var rappelSelectionne by remember { mutableStateOf(tacheExistante?.rappels) }
    var perioSelectionnee by remember { mutableStateOf(tacheExistante?.periodicite ?: Periodicite.AUCUNE) }
    var menuRappelOuvert by remember { mutableStateOf(false) }
    var menuPerioOuvert by remember { mutableStateOf(false) }

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

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Toute la journée", color = Color.White)
            Switch(checked = estTouteJournee, onCheckedChange = { estTouteJournee = it }, colors = SwitchDefaults.colors(checkedThumbColor = FlamePurple))
        }

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
                LignePicker(
                    icon = Icons.Default.AccessTime,
                    label = "Heure de début",
                    value = if (estTouteJournee) "Toute la journée" else heureDebutTexte,
                    enabled = !estTouteJournee
                ) {
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
                LignePicker(
                    icon = Icons.Default.AccessTime,
                    label = "Heure de fin",
                    value = if (estTouteJournee) "Toute la journée" else heureFinTexte,
                    enabled = !estTouteJournee
                ) {
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

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                Box {
                    LignePicker(
                        icon = Icons.Default.NotificationsActive,
                        label = "Rappel",
                        value = rappelSelectionne?.let { if (it >= 60) "${it/60}h avant" else "${it}mn avant" } ?: "Aucun"
                    ) { menuRappelOuvert = true }

                    DropdownMenu(expanded = menuRappelOuvert, onDismissRequest = { menuRappelOuvert = false }, modifier = Modifier.background(FlameCard)) {
                        listOf(null, 5, 15, 30, 60, 120).forEach { mins ->
                            DropdownMenuItem(
                                text = { Text(mins?.let { if (it >= 60) "${it/60}h avant" else "${it}mn avant" } ?: "Aucun", color = Color.White) },
                                onClick = { rappelSelectionne = mins; menuRappelOuvert = false }
                            )
                        }
                    }
                }

                Box {
                    LignePicker(
                        icon = Icons.Default.Repeat,
                        label = "Répéter",
                        value = when(perioSelectionnee) {
                            Periodicite.AUCUNE -> "Jamais"
                            Periodicite.QUOTIDIEN -> "Chaque jour"
                            Periodicite.HEBDOMADAIRE -> "Chaque semaine"
                            Periodicite.MENSUEL -> "Chaque mois"
                        }
                    ) { menuPerioOuvert = true }

                    DropdownMenu(expanded = menuPerioOuvert, onDismissRequest = { menuPerioOuvert = false }, modifier = Modifier.background(FlameCard)) {
                        Periodicite.entries.forEach { perio ->
                            DropdownMenuItem(
                                text = { Text(when(perio) {
                                    Periodicite.AUCUNE -> "Jamais"
                                    Periodicite.QUOTIDIEN -> "Chaque jour"
                                    Periodicite.HEBDOMADAIRE -> "Chaque semaine"
                                    Periodicite.MENSUEL -> "Chaque mois"
                                }, color = Color.White) },
                                onClick = { perioSelectionnee = perio; menuPerioOuvert = false }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (tacheExistante != null) {
            TextButton(onClick = onTaskDelete, modifier = Modifier.fillMaxWidth()) {
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
                        calFin.timeInMillis,
                        estTouteJournee,
                        rappelSelectionne,
                        perioSelectionnee
                    )
                },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlamePurple),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(if (tacheExistante == null) "Créer" else "Enregistrer")
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
        Icon(icon, contentDescription = null, tint = if (enabled) Color.White else Color.Gray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = if (enabled) Color.White else Color.Gray, modifier = Modifier.weight(1f))
        Text(value, color = if (enabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}
