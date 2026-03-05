package com.example.appcliente

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appcliente.ui.theme.AppClienteTheme
import java.text.SimpleDateFormat
import java.util.*

// Modelos de datos ajustados
data class Materia(val nombre: String, val docente: String, val dias: String, val grupo: String)
data class KardexItem(val materia: String, val calificacion: String, val periodo: String)

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
            // val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            // debugLogs.add("[$time] $msg")
            Log.d("SICENET_CLIENT", msg)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Sicenet Client - Consulta") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Carga") },
                        label = { Text("Carga") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.List, contentDescription = "Kardex") },
                        label = { Text("Kardex") }
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)) {

                /* 
                Text("Log de Operaciones:", style = MaterialTheme.typography.labelSmall)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(vertical = 4.dp),
                    color = Color.Black, shape = MaterialTheme.shapes.extraSmall
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(debugLogs) { log ->
                            Text(log, color = if(log.contains("❌")) Color.Red else Color.Cyan,
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                */

                Spacer(Modifier.height(8.dp))

                when (selectedTab) {
                    0 -> CargaTab(onQuery = {
                        addLog("Consultando Carga Académica...")
                        listaMaterias = queryMaterias(::addLog)
                    }, items = listaMaterias)

                    1 -> KardexTab(onQuery = {
                        addLog("Consultando Kardex...")
                        listaKardex = queryKardex(::addLog)
                    }, items = listaKardex)
                }
            }
        }
    }

    private fun queryMaterias(log: (String) -> Unit): List<Materia> {
        val list = mutableListOf<Materia>()
        val uri = Uri.parse("content://com.example.sicenet.provider/materias")
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val iNom = c.getColumnIndex("nombre")
                val iDoc = c.getColumnIndex("docente")
                val iGru = c.getColumnIndex("grupo")

                // Columnas de días
                val iLun = c.getColumnIndex("lunes")
                val iMar = c.getColumnIndex("martes")
                val iMie = c.getColumnIndex("muercoles") // Nota: mantengo el nombre del log
                val iJue = c.getColumnIndex("jueves")
                val iVie = c.getColumnIndex("viernes")

                while (c.moveToNext()) {
                    val nombre = if(iNom != -1) c.getString(iNom) ?: "N/A" else "N/A"
                    val docente = if(iDoc != -1) c.getString(iDoc) ?: "Desconocido" else "Desconocido"
                    val grupo = if(iGru != -1) c.getString(iGru) ?: "-" else "-"

                    val diasList = mutableListOf<String>()
                    if (iLun != -1 && !c.getString(iLun).isNullOrBlank()) diasList.add("Lun")
                    if (iMar != -1 && !c.getString(iMar).isNullOrBlank()) diasList.add("Mar")
                    if (iMie != -1 && !c.getString(iMie).isNullOrBlank()) diasList.add("Mié")
                    if (iJue != -1 && !c.getString(iJue).isNullOrBlank()) diasList.add("Jue")
                    if (iVie != -1 && !c.getString(iVie).isNullOrBlank()) diasList.add("Vie")

                    val dias = if (diasList.isEmpty()) "Sin días" else diasList.joinToString(", ")

                    list.add(Materia(nombre, docente, dias, grupo))
                }
                log("✅ Éxito: ${list.size} materias obtenidas.")
            } ?: log("❌ Error: Cursor nulo")
        } catch (e: Exception) { log("❌ Excepción: ${e.message}") }
        return list
    }

    private fun queryKardex(log: (String) -> Unit): List<KardexItem> {
        val list = mutableListOf<KardexItem>()
        val uri = Uri.parse("content://com.example.sicenet.provider/kardex")
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val iMat = c.getColumnIndex("materia")
                val iCal = c.getColumnIndex("calificacion")
                val iPer = c.getColumnIndex("periodo")

                while (c.moveToNext()) {
                    val mat = if(iMat != -1) c.getString(iMat) ?: "N/A" else "N/A"
                    val cal = if(iCal != -1) c.getString(iCal) ?: "S/C" else "S/C"
                    val per = if(iPer != -1) c.getString(iPer) ?: "Sin Periodo" else "Sin Periodo"
                    list.add(KardexItem(mat, cal, per))
                }
                log("✅ Éxito: ${list.size} registros en Kardex.")
            } ?: log("❌ Error: Cursor Kardex nulo")
        } catch (e: Exception) { log("❌ Excepción Kardex: ${e.message}") }
        return list
    }
}

@Composable
fun CargaTab(onQuery: () -> Unit, items: List<Materia>) {
    Column {
        Button(
            onClick = onQuery,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("CONSULTAR CARGA")
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay datos cargados", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { materia ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = materia.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Badge { Text("Gpo: ${materia.grupo}") }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                Text("Docente: ${materia.docente}", style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(Modifier.height(4.dp))

                            Row {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Días: ${materia.dias}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KardexTab(onQuery: () -> Unit, items: List<KardexItem>) {
    val groupedItems = items.groupBy { it.periodo }

    Column {
        Button(onClick = onQuery, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Icon(Icons.Default.Info, null)
            Spacer(Modifier.width(8.dp))
            Text("CONSULTAR KARDEX")
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay datos cargados", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groupedItems.forEach { (periodo, kardexList) ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(
                                text = periodo,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(kardexList) { item ->
                        ListItem(
                            headlineContent = { Text(item.materia, fontWeight = FontWeight.Medium) },
                            trailingContent = {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = " ${item.calificacion} ",
                                        modifier = Modifier.padding(4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
