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
import android.provider.Settings
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
import androidx.compose.ui.geometry.Size
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
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

// Les différents états d'une tâche
enum class EtatTache { A_FAIRE, EN_RETARD, FAIT, EN_COURS }

// Périodicité des tâches
enum class Periodicite { AUCUNE, QUOTIDIEN, HEBDOMADAIRE, MENSUEL }

// Priorité des tâches
enum class Priorite(val valeur: Int, val label: String) {
    FAIBLE(0, "Faible"),
    MODEREE(1, "Modérée"),
    FORTE(2, "Forte")
}

//Structure globale d'une tâche
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
    val priorite: Priorite = Priorite.FAIBLE
)

class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("flame_prefs", Context.MODE_PRIVATE)
    private val gson = GsonBuilder().create()

    // Sérialise la liste de tâches en JSON pour la sauvegarder localement
    fun saveTaches(taches: List<Tache>) {
        val json = gson.toJson(taches)
        prefs.edit().putString("taches", json).apply()
    }

    // Désérialise le JSON pour récupérer les tâches
    fun loadTaches(): List<Tache> {
        val json = prefs.getString("taches", null) ?: return emptyList()
        val type = object : TypeToken<List<Tache>>() {}.type
        val rawList: List<Tache> = gson.fromJson(json, type)

        return rawList.map { tache ->
            tache.copy(
                priorite = tache.priorite ?: Priorite.FAIBLE,
                periodicite = tache.periodicite ?: Periodicite.AUCUNE
            )
        }
    }
}

// Design global de l'application
val FlameBackground = Color(0xFF1A0033)
val FlameCard = Color(0xFF2E004B)
val FlamePurple = Color(0xFF9D00FF)

//Fonction de calcule pour modifier l'état en temps réel
fun obtenirEtat(tache: Tache): EtatTache {
    val maintenant = System.currentTimeMillis()

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
    private lateinit var storage: Storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = Storage(this)
        enableEdgeToEdge()
        setContent {
            FlameTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FlameBackground) {
                    val navController = rememberNavController()

                    // Liste qui déclenche une sauvegarde automatique à chaque modification
                    val taches = remember {
                        mutableStateListOf<Tache>().apply {
                            addAll(storage.loadTaches())
                        }
                    }

                    LaunchedEffect(taches.toList()) {
                        storage.saveTaches(taches.toList())
                    }

                    // timer de 1 minute pour forcer la recomposition et mettre à jour les états "En cours/En retard"
                    var ticker by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while(true) {
                            delay(60000)
                            ticker = System.currentTimeMillis()
                        }
                    }

                    var showFlameEffect by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(navController = navController, startDestination = "liste") {
                            composable("liste") {
                                key(ticker) {
                                    ListeTaches(
                                        // Tri avec Priorité d'abord puis chronologie
                                        taches = taches.toList().sortedWith(
                                            compareByDescending<Tache> { it.priorite.valeur }.thenBy { it.dateDebut }
                                        ),
                                        onToggleEtat = { tache ->
                                            val index = taches.indexOfFirst { it.id == tache.id }
                                            if (index != -1) {
                                                val t = taches[index]
                                                val nouvelEtat = if (t.etat == EtatTache.FAIT) EtatTache.A_FAIRE else EtatTache.FAIT

                                                val tacheMaj = t.copy(
                                                    etat = nouvelEtat,
                                                    priorite = t.priorite ?: Priorite.FAIBLE
                                                )
                                                taches[index] = tacheMaj

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
                            }

                            composable(
                                route = "ajout?index={index}",
                                arguments = listOf(navArgument("index") { nullable = true })
                            ) { backStackEntry ->
                                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                                val tacheExistante = if (index != null && index in taches.indices) taches[index] else null

                                AjoutTache(
                                    tacheExistante = tacheExistante,
                                    onTaskAction = { titre, desc, debut, fin, touteJournee, rappel, perio, prio ->
                                        val tacheMaj = Tache(
                                            id = tacheExistante?.id ?: System.currentTimeMillis(),
                                            titre = titre,
                                            description = desc,
                                            dateDebut = debut,
                                            dateFin = fin,
                                            etat = tacheExistante?.etat ?: EtatTache.A_FAIRE,
                                            estTouteLaJournee = touteJournee,
                                            rappels = rappel,
                                            periodicite = perio,
                                            priorite = prio
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

    // Vibrations lors de la validation d'une tâche
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

    // Fonction gérant les notifications
    private fun programmerNotifications(context: Context, tache: Tache) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
                return
            }
        }

        // Planification l'alarme de rappel
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

        // Met en place l'alarme de retard
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
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
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

// Effet lorsqu'une tâche est réalisée
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

    // Valeurs animées pour le mouvement ascendant, la transparence et la taille
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
        val x = (startX / 1000f) * size.width
        val y = animatableY.value * size.height

        translate(left = x, top = y) {
            scale(animatableScale.value) {
                with(painter) {
                    draw(
                        size = Size(80.dp.toPx(), 100.dp.toPx()),
                        alpha = animatableAlpha.value
                    )
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

            // Menu de filtrage par état
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
                // Liste scrollable avec support du Swipe-to-Dismiss pour la suppression
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

// Badge d'état
@Composable
fun BadgeEtat(tache: Tache) {
    val maintenant = System.currentTimeMillis()
    val etatReel = obtenirEtat(tache)

    val (label, color) = when {
        etatReel == EtatTache.FAIT -> "Fait" to Color(0xFFB39DDB)
        tache.estTouteLaJournee -> "Aujourd'hui" to FlamePurple
        etatReel == EtatTache.EN_RETARD -> "En retard" to Color(0xFF9C27B0)
        etatReel == EtatTache.EN_COURS -> "En cours" to Color(0xFFFF9800)
        else -> {
            val diffMn = (tache.dateDebut - maintenant) / 60000
            if (diffMn < 60) "Dans ${diffMn}mn" to Color(0xFF03A9F4)
            else {
                val cal = Calendar.getInstance().apply { timeInMillis = tache.dateDebut }
                String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) to Color(0xFF9E9E9E)
            }
        }
    }

    Surface(color = color, shape = RoundedCornerShape(12.dp)) {
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

// Formulaire de création/édition
@Composable
fun AjoutTache(
    tacheExistante: Tache? = null,
    onTaskAction: (String, String, Long, Long, Boolean, Int?, Periodicite, Priorite) -> Unit,
    onTaskDelete: () -> Unit,
    onBack: () -> Unit
) {
    var titre by remember { mutableStateOf(tacheExistante?.titre ?: "") }
    var desc by remember { mutableStateOf(tacheExistante?.description ?: "") }
    var estTouteJournee by remember { mutableStateOf(tacheExistante?.estTouteLaJournee ?: false) }
    var rappelSelectionne by remember { mutableStateOf(tacheExistante?.rappels) }
    var perioSelectionnee by remember { mutableStateOf(tacheExistante?.periodicite ?: Periodicite.AUCUNE) }
    var prioriteSelectionnee by remember { mutableStateOf(tacheExistante?.priorite ?: Priorite.FAIBLE) }
    var menuRappelOuvert by remember { mutableStateOf(false) }
    var menuPerioOuvert by remember { mutableStateOf(false) }

    val calDebut = remember { Calendar.getInstance().apply { if (tacheExistante != null) timeInMillis = tacheExistante.dateDebut } }
    val calFin = remember { Calendar.getInstance().apply { if (tacheExistante != null) timeInMillis = tacheExistante.dateFin } }
    val context = LocalContext.current

    var dateTexte by remember { mutableStateOf(tacheExistante?.let { "${calDebut.get(Calendar.DAY_OF_MONTH)}/${calDebut.get(Calendar.MONTH) + 1}" } ?: "Choisir") }
    var heureDebutTexte by remember { mutableStateOf(tacheExistante?.let { String.format("%02d:%02d", calDebut.get(Calendar.HOUR_OF_DAY), calDebut.get(Calendar.MINUTE)) } ?: "Choisir") }
    var heureFinTexte by remember { mutableStateOf(tacheExistante?.let { String.format("%02d:%02d", calFin.get(Calendar.HOUR_OF_DAY), calFin.get(Calendar.MINUTE)) } ?: "Choisir") }

    Column(modifier = Modifier.fillMaxSize().background(FlameBackground).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (tacheExistante == null) "Créer une tâche" else "Modifier la tâche", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = titre, onValueChange = { titre = it }, label = { Text("Titre", color = Color.White.copy(0.7f)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = FlamePurple, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description", color = Color.White.copy(0.7f)) }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = FlamePurple, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Toute la journée", color = Color.White)
            Switch(checked = estTouteJournee, onCheckedChange = { estTouteJournee = it }, colors = SwitchDefaults.colors(checkedThumbColor = FlamePurple))
        }
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
                HorizontalDivider(color = Color.White.copy(0.1f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                Box {
                    LignePicker(Icons.Default.NotificationsActive, "Rappel", rappelSelectionne?.let { if (it >= 60) "${it/60}h avant" else "${it}mn avant" } ?: "Aucun") { menuRappelOuvert = true }
                    DropdownMenu(expanded = menuRappelOuvert, onDismissRequest = { menuRappelOuvert = false }, modifier = Modifier.background(FlameCard)) {
                        listOf(null, 5, 15, 30, 60, 120).forEach { mins ->
                            DropdownMenuItem(text = { Text(mins?.let { if (it >= 60) "${it/60}h avant" else "${it}mn avant" } ?: "Aucun", color = Color.White) }, onClick = { rappelSelectionne = mins; menuRappelOuvert = false })
                        }
                    }
                }
                Box {
                    LignePicker(Icons.Default.Repeat, "Répéter", perioSelectionnee.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) { menuPerioOuvert = true }
                    DropdownMenu(expanded = menuPerioOuvert, onDismissRequest = { menuPerioOuvert = false }, modifier = Modifier.background(FlameCard)) {
                        Periodicite.entries.forEach { perio ->
                            DropdownMenuItem(text = { Text(perio.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }, color = Color.White) }, onClick = { perioSelectionnee = perio; menuPerioOuvert = false })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Priorité de la tâche", color = Color.White, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        PrioriteSelector(selected = prioriteSelectionnee, onSelect = { prioriteSelectionnee = it })

        Spacer(modifier = Modifier.weight(1f))
        if (tacheExistante != null) {
            TextButton(onClick = onTaskDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Supprimer la tâche", color = Color.Red, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { if (titre.isNotBlank()) onTaskAction(titre, desc, calDebut.timeInMillis, calFin.timeInMillis, estTouteJournee, rappelSelectionne, perioSelectionnee, prioriteSelectionnee) }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = FlamePurple), shape = RoundedCornerShape(28.dp)) { Text(if (tacheExistante == null) "Créer" else "Enregistrer") }
            Button(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = RoundedCornerShape(28.dp), border = androidx.compose.foundation.BorderStroke(2.dp, FlamePurple)) { Text("Annuler", color = Color.White) }
        }
    }
}

// Sélecteur pour la priorité
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
        Priorite.entries.forEach { priorite ->
            val isSelected = selected == priorite
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) {
                            if (priorite == Priorite.FAIBLE) Color(0xFFD1A3FF)
                            else if (priorite == Priorite.MODEREE) Color(0xFFA53DFF)
                            else Color(0xFF8A00FF)
                        } else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(priorite) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = priorite.label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun LignePicker(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = if (enabled) Color.White else Color.Gray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = if (enabled) Color.White else Color.Gray, modifier = Modifier.weight(1f))
        Text(value, color = if (enabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}