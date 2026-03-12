package com.example.flame

import android.app.AlarmManager
import android.app.PendingIntent
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flame.ui.theme.FlameBackground
import com.example.flame.ui.theme.FlameCard
import com.example.flame.ui.theme.FlamePurple
import com.example.flame.ui.theme.FlameTheme
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.util.*

class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("flame_prefs", Context.MODE_PRIVATE)
    private val gson = GsonBuilder().create()

    fun saveTaches(taches: List<Tache>) {
        prefs.edit().putString("taches", gson.toJson(taches)).apply()
    }

    fun loadTaches(): List<Tache> {
        val json = prefs.getString("taches", null) ?: return emptyList()
        val type = object : TypeToken<List<Tache>>() {}.type
        val rawList: List<Tache> = gson.fromJson(json, type)
        return rawList.map { it.copy(priorite = it.priorite ?: Priorite.FAIBLE, periodicite = it.periodicite ?: Periodicite.AUCUNE) }
    }

    fun saveObjectifs(objectifs: List<Objectif>) {
        prefs.edit().putString("objectifs", gson.toJson(objectifs)).apply()
    }

    fun loadObjectifs(): List<Objectif> {
        val json = prefs.getString("objectifs", null) ?: return emptyList()
        val type = object : TypeToken<List<Objectif>>() {}.type
        return gson.fromJson(json, type)
    }
}

@Composable
fun NavBar(navController: NavController) {
    val items = listOf(
        Triple("liste", "Tâches", Icons.Default.List),
        Triple("calendrier", "Calendrier", Icons.Default.DateRange),
        Triple("objectifs", "Objectifs", Icons.Default.Star)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val routesSansNavbar = listOf("ajout", "ajout_objectif", "detail_objectif/{id}")
    if (currentRoute?.split("?")?.get(0) in routesSansNavbar || currentRoute in routesSansNavbar) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        NavigationBar(
            containerColor = FlameCard,
            modifier = Modifier
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp)),
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            items.forEach { (route, label, icon) ->
                val isSelected = currentRoute == route
                NavigationBarItem(
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    selected = isSelected,
                    alwaysShowLabel = true,
                    onClick = {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlamePurple,
                        selectedTextColor = FlamePurple,
                        unselectedIconColor = Color.White.copy(0.5f),
                        unselectedTextColor = Color.White.copy(0.5f),
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
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
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    LoadingScreen(onFinished = { showSplash = false })
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = FlameBackground) {
                        val navController = rememberNavController()
                        val taches = remember { mutableStateListOf<Tache>().apply { addAll(storage.loadTaches()) } }
                        val objectifs = remember { mutableStateListOf<Objectif>().apply { addAll(storage.loadObjectifs()) } }

                        LaunchedEffect(taches.toList()) { storage.saveTaches(taches.toList()) }
                        LaunchedEffect(objectifs.toList()) { storage.saveObjectifs(objectifs.toList()) }

                        var ticker by remember { mutableLongStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(Unit) {
                            while(true) { delay(60000); ticker = System.currentTimeMillis() }
                        }

                        var showFlameEffect by remember { mutableStateOf(false) }

                        val onToggleEtat: (Tache) -> Unit = { t ->
                            val idx = taches.indexOfFirst { it.id == t.id }
                            if (idx != -1) {
                                val oldT = taches[idx]
                                val next = if (oldT.etat == EtatTache.FAIT) EtatTache.A_FAIRE else EtatTache.FAIT
                                taches[idx] = oldT.copy(etat = next)
                                if (next == EtatTache.FAIT) {
                                    showFlameEffect = true
                                    vibrer(this@MainActivity)
                                    if (oldT.periodicite != Periodicite.AUCUNE) {
                                        val nT = oldT.copy(
                                            id = System.currentTimeMillis(),
                                            dateDebut = getNextDate(oldT.dateDebut, oldT.periodicite),
                                            dateFin = getNextDate(oldT.dateFin, oldT.periodicite),
                                            etat = EtatTache.A_FAIRE
                                        )
                                        taches.add(nT)
                                        programmerNotifications(this@MainActivity, nT)
                                    }
                                }
                            }
                        }

                        Scaffold(
                            bottomBar = { NavBar(navController) },
                            containerColor = FlameBackground
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                                NavHost(navController = navController, startDestination = "liste") {
                                    composable("liste") {
                                        key(ticker) {
                                            ListeTachesScreen(
                                                taches = taches.toList().sortedWith(compareBy<Tache> { it.dateDebut }.thenByDescending { it.priorite.valeur }),
                                                onToggleEtat = onToggleEtat,
                                                onDeleteTache = { t -> annulerNotifications(this@MainActivity, t); taches.removeIf { it.id == t.id } },
                                                onNavigateToEdit = { t -> navController.navigate("ajout?index=${taches.indexOfFirst { it.id == t.id }}") },
                                                onNavigateToAdd = { navController.navigate("ajout") }
                                            )
                                        }
                                    }
                                    composable("calendrier") {
                                        CalendrierScreen(
                                            taches = taches.toList(),
                                            onToggleEtat = onToggleEtat,
                                            onNavigateToEdit = { t -> navController.navigate("ajout?index=${taches.indexOfFirst { it.id == t.id }}") }
                                        )
                                    }
                                    composable("objectifs") {
                                        ObjectifsScreen(
                                            objectifs = objectifs.toList(),
                                            taches = taches.toList(),
                                            onNavigateToAdd = { navController.navigate("ajout_objectif") },
                                            onNavigateToDetail = { obj -> navController.navigate("detail_objectif/${obj.id}") }
                                        )
                                    }
                                    composable("ajout_objectif?id={id}", arguments = listOf(navArgument("id") { nullable = true })) { bse ->
                                        val idStr = bse.arguments?.getString("id")
                                        val id = idStr?.toLongOrNull()
                                        val exist = if (id != null) objectifs.find { it.id == id } else null
                                        
                                        AjoutObjectifScreen(
                                            objectifExistant = exist,
                                            onSave = { tit, desc, emj -> 
                                                if (exist != null) {
                                                    val idx = objectifs.indexOfFirst { it.id == exist.id }
                                                    if (idx != -1) objectifs[idx] = exist.copy(titre = tit, description = desc, emoji = emj)
                                                } else {
                                                    objectifs.add(Objectif(titre = tit, description = desc, emoji = emj))
                                                }
                                                navController.popBackStack()
                                            },
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                    composable(
                                        route = "detail_objectif/{id}",
                                        arguments = listOf(navArgument("id") { type = androidx.navigation.NavType.LongType })
                                    ) { bse ->
                                        val id = bse.arguments?.getLong("id")
                                        val obj = objectifs.find { it.id == id }
                                        if (obj != null) {
                                            DetailObjectifScreen(
                                                objectif = obj,
                                                taches = taches.filter { it.objectifId == obj.id }.sortedWith(compareBy<Tache> { it.dateDebut }.thenByDescending { it.priorite.valeur }),
                                                onBack = { navController.popBackStack() },
                                                onDelete = {
                                                    objectifs.removeIf { it.id == obj.id }
                                                    taches.indices.forEach { i -> 
                                                        if (taches[i].objectifId == obj.id) taches[i] = taches[i].copy(objectifId = null)
                                                    }
                                                    navController.popBackStack()
                                                },
                                                onEdit = { navController.navigate("ajout_objectif?id=${obj.id}") },
                                                onToggleEtat = onToggleEtat
                                            )
                                        }
                                    }
                                    composable("ajout?index={index}", arguments = listOf(navArgument("index") { nullable = true })) { bse ->
                                        val idx = bse.arguments?.getString("index")?.toIntOrNull()
                                        val exist = if (idx != null && idx in taches.indices) taches[idx] else null
                                        AjoutTacheScreen(
                                            tacheExistante = exist,
                                            objectifs = objectifs.toList(),
                                            onTaskAction = { tit, desc, img, deb, fin, tj, rap, per, prio, objId ->
                                                val tMaj = Tache(id = exist?.id ?: System.currentTimeMillis(), titre = tit, description = desc, imageUri = img, dateDebut = deb, dateFin = fin, etat = exist?.etat ?: EtatTache.A_FAIRE, estTouteLaJournee = tj, rappels = rap, periodicite = per, priorite = prio, objectifId = objId)
                                                if (idx != null && idx in taches.indices) taches[idx] = tMaj else taches.add(tMaj)
                                                programmerNotifications(this@MainActivity, tMaj)
                                                navController.popBackStack()
                                            },
                                            onTaskDelete = { if (idx != null && idx in taches.indices) { annulerNotifications(this@MainActivity, taches[idx]); taches.removeAt(idx) }; navController.popBackStack() },
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                                if (showFlameEffect) EffetFlammes(onAnimationFinished = { showFlameEffect = false })
                            }
                        }
                    }
                }
            }
        }
    }
}

fun vibrer(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(100)
    }
}

fun programmerNotifications(context: Context, tache: Tache) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
            return
        }
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    tache.rappels?.let { mins ->
        val time = tache.dateDebut - mins * 60000
        if (time > System.currentTimeMillis()) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("titre", tache.titre)
                putExtra("msg", "Commence dans $mins min")
            }
            val pi = PendingIntent.getBroadcast(context, tache.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        }
    }
}

fun annulerNotifications(context: Context, tache: Tache) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(context, tache.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarmManager.cancel(pi)
}

fun getNextDate(ts: Long, p: Periodicite): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    when(p) {
        Periodicite.QUOTIDIEN -> c.add(Calendar.DAY_OF_YEAR, 1)
        Periodicite.HEBDOMADAIRE -> c.add(Calendar.WEEK_OF_YEAR, 1)
        Periodicite.MENSUEL -> c.add(Calendar.MONTH, 1)
        else -> {}
    }
    return c.timeInMillis
}
