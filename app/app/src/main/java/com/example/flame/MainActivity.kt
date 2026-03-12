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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Triple("objectifs", "Objectifs", Icons.Default.Star)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val routesSansNavbar = listOf("ajout", "ajout_objectif", "detail_objectif/{id}")
    if (currentRoute?.split("?")?.get(0) in routesSansNavbar || currentRoute in routesSansNavbar) return

    NavigationBar(
        containerColor = FlameCard,
        modifier = Modifier.height(64.dp), // Réduction de la taille
        tonalElevation = 0.dp
    ) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label, fontSize = 10.sp) },
                selected = currentRoute == route,
                alwaysShowLabel = true, // Force l'affichage du texte
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
                    val taches = remember { mutableStateListOf<Tache>().apply { addAll(storage.loadTaches()) } }
                    val objectifs = remember { mutableStateListOf<Objectif>().apply { addAll(storage.loadObjectifs()) } }

                    LaunchedEffect(taches.toList()) { storage.saveTaches(taches.toList()) }
                    LaunchedEffect(objectifs.toList()) { storage.saveObjectifs(objectifs.toList()) }

                    var ticker by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while(true) { delay(60000); ticker = System.currentTimeMillis() }
                    }

                    var showFlameEffect by remember { mutableStateOf(false) }

                    Scaffold(
                        bottomBar = { BottomBar(navController) },
                        containerColor = FlameBackground
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                            NavHost(navController = navController, startDestination = "liste") {
                                composable("liste") {
                                    key(ticker) {
                                        ListeTachesScreen(
                                            taches = taches.toList().sortedWith(compareByDescending<Tache> { it.priorite.valeur }.thenBy { it.dateDebut }),
                                            onToggleEtat = { t ->
                                                val idx = taches.indexOfFirst { it.id == t.id }
                                                if (idx != -1) {
                                                    val next = if (taches[idx].etat == EtatTache.FAIT) EtatTache.A_FAIRE else EtatTache.FAIT
                                                    taches[idx] = taches[idx].copy(etat = next)
                                                    if (next == EtatTache.FAIT) { showFlameEffect = true; vibrer(this@MainActivity) }
                                                }
                                            },
                                            onDeleteTache = { t -> annulerNotifications(this@MainActivity, t); taches.removeIf { it.id == t.id } },
                                            onNavigateToEdit = { t -> navController.navigate("ajout?index=${taches.indexOfFirst { it.id == t.id }}") },
                                            onNavigateToAdd = { navController.navigate("ajout") }
                                        )
                                    }
                                }
                                composable("objectifs") {
                                    ObjectifsScreen(
                                        objectifs = objectifs.toList(),
                                        taches = taches.toList(),
                                        onNavigateToAdd = { navController.navigate("ajout_objectif") },
                                        onNavigateToDetail = { obj -> navController.navigate("detail_objectif/${obj.id}") }
                                    )
                                }
                                composable("ajout_objectif") {
                                    AjoutObjectifScreen(
                                        onSave = { tit, desc, emj -> objectifs.add(Objectif(titre = tit, description = desc, emoji = emj)); navController.popBackStack() },
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
                                            taches = taches.filter { it.objectifId == obj.id }.sortedWith(compareByDescending<Tache> { it.priorite.valeur }.thenBy { it.dateDebut }),
                                            onBack = { navController.popBackStack() },
                                            onToggleEtat = { t ->
                                                val idx = taches.indexOfFirst { it.id == t.id }
                                                if (idx != -1) {
                                                    val next = if (taches[idx].etat == EtatTache.FAIT) EtatTache.A_FAIRE else EtatTache.FAIT
                                                    taches[idx] = taches[idx].copy(etat = next)
                                                    if (next == EtatTache.FAIT) { showFlameEffect = true; vibrer(this@MainActivity) }
                                                }
                                            }
                                        )
                                    }
                                }
                                composable("ajout?index={index}", arguments = listOf(navArgument("index") { nullable = true })) { bse ->
                                    val idx = bse.arguments?.getString("index")?.toIntOrNull()
                                    val exist = if (idx != null && idx in taches.indices) taches[idx] else null
                                    AjoutTacheScreen(
                                        tacheExistante = exist,
                                        objectifs = objectifs.toList(),
                                        onTaskAction = { tit, desc, deb, fin, tj, rap, per, prio, objId ->
                                            val tMaj = Tache(id = exist?.id ?: System.currentTimeMillis(), titre = tit, description = desc, dateDebut = deb, dateFin = fin, etat = exist?.etat ?: EtatTache.A_FAIRE, estTouteLaJournee = tj, rappels = rap, periodicite = per, priorite = prio, objectifId = objId)
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

    private fun vibrer(context: Context) {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") v.vibrate(300)
    }

    private fun programmerNotifications(context: Context, t: Tache) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)); return }
        t.rappels?.let { mins ->
            val time = t.dateDebut - (mins * 60 * 1000)
            if (time > System.currentTimeMillis()) {
                val intent = Intent(context, ReminderReceiver::class.java).apply { putExtra("TYPE", "RAPPEL"); putExtra("TITRE", t.titre); putExtra("DESC", t.description); putExtra("ID", t.id) }
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, PendingIntent.getBroadcast(context, t.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
        }
        if (t.dateFin > System.currentTimeMillis()) {
            val intentRetard = Intent(context, ReminderReceiver::class.java).apply { putExtra("TYPE", "RETARD"); putExtra("TITRE", t.titre); putExtra("ID", t.id) }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.dateFin, PendingIntent.getBroadcast(context, (t.id + 1).toInt(), intentRetard, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
    }

    private fun annulerNotifications(context: Context, t: Tache) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        PendingIntent.getBroadcast(context, t.id.toInt(), Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { am.cancel(it) }
        PendingIntent.getBroadcast(context, (t.id + 1).toInt(), Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { am.cancel(it) }
    }
}
