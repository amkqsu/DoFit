package com.dofit.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private val Bg = Color(0xFF0D0D0E)
private val Surface = Color(0xFF19191B)
private val Raised = Color(0xFF272729)
private val Line = Color(0xFF303034)
private val Text = Color(0xFFF5F5F7)
private val Muted = Color(0xFF99999F)
private val Accent = Color(0xFFC7C7DB)

class DoFitViewModel(private val dao: DoFitDao) : ViewModel() {
    private val day = LocalDate.now().toString()
    val state = combine(dao.habits(), dao.checks(day), dao.health()) { habits, checks, health -> Triple(habits, checks.associateBy { it.habitId }, health) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), emptyMap(), emptyList()))
    fun addHabit(name: String, hour: Int?, minute: Int?) = viewModelScope.launch {
        val id = dao.addHabit(Habit(name = name, reminderHour = hour, reminderMinute = minute))
        if (hour != null && minute != null) dao.allHabits().firstOrNull { it.id == id }?.let { ReminderScheduler.schedule(App.context, it) }
    }
    fun toggle(habit: Habit, done: Boolean) = viewModelScope.launch { dao.setCheck(HabitCheck(habit.id, day, done)) }
    fun delete(habit: Habit) = viewModelScope.launch { dao.deleteChecks(habit.id); dao.deleteHabit(habit.id) }
    fun addHealth(type: String, value: Double, unit: String) = viewModelScope.launch { dao.addHealth(HealthEntry(type = type, value = value, unit = unit)) }
}

object App { lateinit var context: android.content.Context }

class MainActivity : ComponentActivity() {
    private val notifyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.context = applicationContext
        if (Build.VERSION.SDK_INT >= 33) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        val dao = DoFitDatabase.get(this).dao()
        val vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DoFitViewModel(dao) as T
        })[DoFitViewModel::class.java]
        setContent { DoFitTheme { DoFitApp(vm) } }
    }
}

@Composable
fun DoFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Surface, onPrimary = Color(0xFF20202B), onBackground = Text, onSurface = Text), content = content)
}

@Composable
fun DoFitApp(vm: DoFitViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = Bg, bottomBar = {
        NavigationBar(containerColor = Color(0xFF111113)) {
            listOf("Today", "Habits", "Health", "Stats").forEachIndexed { i, label ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(label) })
            }
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("DoFit", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Text)
            Spacer(Modifier.height(18.dp))
            when (tab) {
                0 -> Today(state.first, state.second, vm)
                1 -> Habits(state.first, vm)
                2 -> Health(state.third, vm)
                else -> Stats(state.first, state.second)
            }
        }
    }
}

@Composable
fun Today(habits: List<Habit>, checks: Map<Long, HabitCheck>, vm: DoFitViewModel) {
    val done = habits.count { checks[it.id]?.done == true }
    Text("$done / ${habits.size} completed", color = Muted)
    Spacer(Modifier.height(14.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(habits) { habit ->
            val checked = checks[habit.id]?.done == true
            Row(Modifier.fillMaxWidth().background(Surface).clickable { vm.toggle(habit, !checked) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { vm.toggle(habit, it) })
                Spacer(Modifier.width(10.dp))
                Text(habit.name, color = Text, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun Habits(habits: List<Habit>, vm: DoFitViewModel) {
    var name by remember { mutableStateOf("") }
    OutlinedTextField(name, { name = it }, label = { Text("Habit name") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    Button(onClick = { val n = name.trim(); if (n.isNotEmpty()) { vm.addHabit(n, null, null); name = "" } }) { Text("Add habit") }
    Spacer(Modifier.height(16.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(habits) { h ->
            Row(Modifier.fillMaxWidth().background(Surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(h.name, modifier = Modifier.weight(1f), color = Text)
                Text("Delete", color = Accent, modifier = Modifier.clickable { vm.delete(h) })
            }
        }
    }
}

@Composable
fun Health(entries: List<HealthEntry>, vm: DoFitViewModel) {
    var value by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Water") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Water", "Sleep", "Weight").forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) }) }
    }
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(value, { value = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Value") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    Button(onClick = { value.toDoubleOrNull()?.let { vm.addHealth(type, it, when (type) { "Water" -> "ml"; "Sleep" -> "h"; else -> "kg" }); value = "" } }) { Text("Save") }
    Spacer(Modifier.height(16.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(entries) { e -> Text("${e.type}: ${e.value} ${e.unit}", color = Text, modifier = Modifier.fillMaxWidth().background(Surface).padding(14.dp)) }
    }
}

@Composable
fun Stats(habits: List<Habit>, checks: Map<Long, HabitCheck>) {
    val completed = habits.count { checks[it.id]?.done == true }
    val rate = if (habits.isEmpty()) 0 else completed * 100 / habits.size
    Text("Today", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Text)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("Habits", habits.size.toString(), Modifier.weight(1f))
        StatCard("Done", completed.toString(), Modifier.weight(1f))
        StatCard("Rate", "$rate%", Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Surface).padding(16.dp)) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Text)
        Text(label, color = Muted)
    }
}
