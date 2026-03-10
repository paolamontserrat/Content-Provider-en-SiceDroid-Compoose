package com.example.appcliente

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appcliente.ui.theme.AppClienteTheme
import java.text.SimpleDateFormat
import java.util.*

// Modelos de datos
data class Materia(val id: String, val nombre: String, val docente: String, val dias: String, val grupo: String)
data class KardexItem(val id: String, val materia: String, val calificacion: String, val periodo: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppClienteTheme {
                SicenetCompleteApp()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SicenetCompleteApp() {
        var selectedTab by remember { mutableIntStateOf(0) }
        // val debugLogs = remember { mutableStateListOf<String>() }

        // Estados para los datos
        var listaMaterias by remember { mutableStateOf(listOf<Materia>()) }
        var listaKardex by remember { mutableStateOf(listOf<KardexItem>()) }

        fun addLog(msg: String) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            // debugLogs.add("[$time] $msg")
            Log.d("SICENET_CLIENT", msg)
        }

        // Función para refrescar todo
        val refreshAll = {
            listaMaterias = queryMaterias(::addLog)
            listaKardex = queryKardex(::addLog)
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Sicenet Client Pro") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Carga") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Kardex") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Lock, null) }, label = { Text("Seguridad") })
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                /*
                Surface(
                    modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 4.dp),
                    color = Color.Black, shape = MaterialTheme.shapes.extraSmall
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(debugLogs) { log -> Text(log, color = Color.Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                    }
                }
                */
                Spacer(Modifier.height(8.dp))
                when (selectedTab) {
                    0 -> CargaTab(onQuery = refreshAll, items = listaMaterias, onDelete = { id -> deleteRow("materias", id, ::addLog, refreshAll) })
                    1 -> KardexTab(onQuery = refreshAll, items = listaKardex, onDelete = { id -> deleteRow("kardex", id, ::addLog, refreshAll) })
                    2 -> SeguridadTab(log = ::addLog, onDataChanged = refreshAll)
                }
            }
        }
    }

    private fun queryMaterias(log: (String) -> Unit): List<Materia> {
        val list = mutableListOf<Materia>()
        try {
            contentResolver.query(Uri.parse("content://com.example.sicenet.provider/materias"), null, null, null, null)?.use { c ->
                val iId = c.getColumnIndex("id")
                val iNom = c.getColumnIndex("nombre")
                val iDoc = c.getColumnIndex("docente")
                val iGru = c.getColumnIndex("grupo")
                val iLun = c.getColumnIndex("lunes")
                val iMar = c.getColumnIndex("martes")
                val iMie = c.getColumnIndex("miercoles")
                val iJue = c.getColumnIndex("jueves")
                val iVie = c.getColumnIndex("viernes")
                val iSab = c.getColumnIndex("sabado")
                val iDom = c.getColumnIndex("domingo")

                while (c.moveToNext()) {
                    val id = if(iId != -1) c.getString(iId) else "0"
                    val nom = if(iNom != -1) c.getString(iNom) else "N/A"
                    val doc = if(iDoc != -1) c.getString(iDoc) else "-"
                    val gru = if(iGru != -1) c.getString(iGru) ?: "-" else "-"
                    val diasList = mutableListOf<String>()
                    if (iLun != -1 && !c.getString(iLun).isNullOrBlank()) diasList.add("Lun")
                    if (iMar != -1 && !c.getString(iMar).isNullOrBlank()) diasList.add("Mar")
                    if (iMie != -1 && !c.getString(iMie).isNullOrBlank()) diasList.add("Mié")
                    if (iJue != -1 && !c.getString(iJue).isNullOrBlank()) diasList.add("Jue")
                    if (iVie != -1 && !c.getString(iVie).isNullOrBlank()) diasList.add("Vie")
                    if (iSab != -1 && !c.getString(iSab).isNullOrBlank()) diasList.add("Sab")
                    if (iDom != -1 && !c.getString(iDom).isNullOrBlank()) diasList.add("Dom")
                    list.add(Materia(id, nom, doc, diasList.joinToString(", "), gru))
                }
                log("Carga: ${list.size} materias.")
            } ?: log("Sesión cerrada o error.")
        } catch (e: Exception) { log("Error query: ${e.message}") }
        return list
    }

    private fun queryKardex(log: (String) -> Unit): List<KardexItem> {
        val list = mutableListOf<KardexItem>()
        try {
            contentResolver.query(Uri.parse("content://com.example.sicenet.provider/kardex"), null, null, null, null)?.use { c ->
                val iId = c.getColumnIndex("id")
                val iMat = c.getColumnIndex("materia")
                val iCal = c.getColumnIndex("calificacion")
                val iPer = c.getColumnIndex("periodo")
                while (c.moveToNext()) {
                    val id = if(iId != -1) c.getString(iId) else "0"
                    val mat = if(iMat != -1) c.getString(iMat) else "N/A"
                    val cal = if(iCal != -1) c.getString(iCal) else "0"
                    val per = if(iPer != -1) c.getString(iPer) else "2024"
                    list.add(KardexItem(id, mat, cal, per))
                }
                log("Kardex: ${list.size} registros.")
            }
        } catch (e: Exception) { log("Error Kardex: ${e.message}") }
        return list
    }

    private fun deleteRow(table: String, id: String, log: (String) -> Unit, onDone: () -> Unit) {
        try {
            val deleted = contentResolver.delete(Uri.parse("content://com.example.sicenet.provider/$table"), "id = ?", arrayOf(id))
            log("🗑️ Borrado ID $id: $deleted filas")
            onDone()
        } catch (e: Exception) { log("Error al borrar: ${e.message}") }
    }
}

@Composable
fun CargaTab(onQuery: () -> Unit, items: List<Materia>, onDelete: (String) -> Unit) {
    Column {
        Button(onClick = onQuery, modifier = Modifier.fillMaxWidth()) { Text("REFRESCAR CARGA") }
        LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(items) { m ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(m.nombre, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                Badge { Text("Gpo: ${m.grupo}") }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                Text(m.docente, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text("Días: ${m.dias}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        IconButton(onClick = { onDelete(m.id) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                }
            }
        }
    }
}

@Composable
fun KardexTab(onQuery: () -> Unit, items: List<KardexItem>, onDelete: (String) -> Unit) {
    val grouped = items.groupBy { it.periodo }
    Column {
        Button(onClick = onQuery, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("REFRESCAR KARDEX")
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            grouped.forEach { (periodo, list) ->
                item {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(periodo, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
                items(list) { k ->
                    ListItem(headlineContent = { Text(k.materia) }, trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(k.calificacion, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDelete(k.id) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        }
                    })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun SeguridadTab(log: (String) -> Unit, onDataChanged: () -> Unit) {
    val context = LocalContext.current
    var mClave by remember { mutableStateOf("") }
    var mNombre by remember { mutableStateOf("") }
    var mDocente by remember { mutableStateOf("") }
    var mHora by remember { mutableStateOf("07:00-09:00") }

    val diasLabels = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
    val diasSeleccionados = remember { mutableStateMapOf<String, Boolean>().apply {
        diasLabels.forEach { put(it, false) }
    } }

    var kClave by remember { mutableStateOf("") }
    var kMateria by remember { mutableStateOf("") }
    var kCalificacion by remember { mutableStateOf("") }
    var kAcreditacion by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("CRUD Materias", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(value = mClave, onValueChange = { mClave = it }, label = { Text("Clave Materia") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = mNombre, onValueChange = { mNombre = it }, label = { Text("Nombre Materia") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = mDocente, onValueChange = { mDocente = it }, label = { Text("Docente") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = mHora, onValueChange = { mHora = it }, label = { Text("Horario (ej: 07:00-09:00)") }, modifier = Modifier.fillMaxWidth())

        Text("Seleccionar días:", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
        diasLabels.chunked(3).forEach { fila ->
            Row(Modifier.fillMaxWidth()) {
                fila.forEach { dia ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(checked = diasSeleccionados[dia] ?: false, onCheckedChange = { diasSeleccionados[dia] = it })
                        Text(dia.take(3).replaceFirstChar { it.uppercase() }, fontSize = 12.sp)
                    }
                }
            }
        }

        Button(onClick = {
            try {
                val v = ContentValues().apply {
                    put("clave", mClave); put("nombre", mNombre); put("docente", mDocente)
                    put("grupo", "A"); put("creditos", 5); put("aula", "S/A")
                    diasLabels.forEach { dia -> put(dia, if (diasSeleccionados[dia] == true) mHora else "") }
                    put("lastUpdate", System.currentTimeMillis())
                }
                context.contentResolver.insert(Uri.parse("content://com.example.sicenet.provider/materias"), v)
                log("Materia OK"); onDataChanged()
            } catch (e: Exception) { log("Error Materias: ${e.message}") }
        }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Insertar Materia") }

        Spacer(Modifier.height(20.dp))
        Text("CRUD Kardex", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        OutlinedTextField(value = kClave, onValueChange = { kClave = it }, label = { Text("Clave Kardex") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = kMateria, onValueChange = { kMateria = it }, label = { Text("Nombre Materia") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = kCalificacion, onValueChange = { kCalificacion = it }, label = { Text("Calificación") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = kAcreditacion, onValueChange = { kAcreditacion = it }, label = { Text("Acreditación") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            try {
                val v = ContentValues().apply {
                    put("clave", kClave); put("materia", kMateria)
                    put("calificacion", kCalificacion); put("acreditacion", kAcreditacion)
                    put("periodo", "2024"); put("lastUpdate", System.currentTimeMillis())
                }
                context.contentResolver.insert(Uri.parse("content://com.example.sicenet.provider/kardex"), v)
                log("Kardex OK"); onDataChanged()
            } catch (e: Exception) { log("Error Kardex: ${e.message}") }
        }, Modifier.fillMaxWidth()) { Text("Insertar Kardex") }
    }
}