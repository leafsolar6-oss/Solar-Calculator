package ng.leafsolar.calculator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

internal val Lime = Color(0xFF76D50B)
internal val Green = Color(0xFF3CA506)
internal val GreenDark = Color(0xFF102417)
internal val Ink = Color(0xFF14201A)
internal val Muted = Color(0xFF5E6B62)
internal val Line = Color(0xFFE1E7E1)
internal val Bg = Color(0xFFF5F9F3)
private val Warn = Color(0xFFB26A00)
private val WarnBg = Color(0xFFFFF4D6)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { CalculatorApp() }
  }
}

private class RowState(val def: CalcEngine.ApplianceDef) {
  var qty by mutableStateOf(0)
  var watts by mutableStateOf("")
  var inverter by mutableStateOf(false)
  var hours by mutableStateOf("")
}
private class CustomRow(name: String, watts: String, surge: Int) {
  var name by mutableStateOf(name)
  var watts by mutableStateOf(watts)
  var surge by mutableStateOf(surge)
  var inverter by mutableStateOf(false)
  var hours by mutableStateOf("")
}
private data class StoredRow(val qty: Int, val watts: String, val inverter: Boolean, val hours: String)

private object Store {
  private const val PREF = "leafcalc"
  fun save(ctx: Context, rows: List<RowState>, custom: List<CustomRow>) {
    val arr = JSONArray()
    CalcEngine.APPS.forEachIndexed { i, _ ->
      val r = rows[i]
      arr.put(JSONObject().put("qty", r.qty).put("watts", r.watts).put("inverter", r.inverter).put("hours", r.hours))
    }
    val carr = JSONArray()
    custom.forEach { c -> carr.put(JSONObject().put("name", c.name).put("watts", c.watts).put("surge", c.surge).put("inverter", c.inverter).put("hours", c.hours)) }
    ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("rows", arr.toString()).putString("custom", carr.toString()).apply()
  }
  fun loadRows(ctx: Context): List<StoredRow> {
    val s = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("rows", null) ?: return List(CalcEngine.APPS.size) { StoredRow(0, "", false, "") }
    return try { val a = JSONArray(s); List(a.length()) { i -> val o = a.getJSONObject(i); StoredRow(o.optInt("qty",0), o.optString("watts",""), o.optBoolean("inverter",false), o.optString("hours","")) } }
    catch (e: Exception) { List(CalcEngine.APPS.size) { StoredRow(0, "", false, "") } }
  }
  fun loadCustom(ctx: Context): MutableList<CustomRow> {
    val s = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("custom", null) ?: return mutableListOf()
    return try { val a = JSONArray(s); val out = mutableListOf<CustomRow>(); for (i in 0 until a.length()) { val o = a.getJSONObject(i); out.add(CustomRow(o.optString("name"), o.optString("watts"), o.optInt("surge",1)).apply { inverter=o.optBoolean("inverter",false); hours=o.optString("hours","") }) }; out }
    catch (e: Exception) { mutableListOf() }
  }
}

@Composable
fun CalculatorApp() {
  val context = LocalContext.current
  val rows = remember { val st = Store.loadRows(context); CalcEngine.APPS.mapIndexed { i, def -> RowState(def).apply { qty=st[i].qty; watts=st[i].watts; inverter=st[i].inverter; hours=st[i].hours } } }
  val custom = remember { Store.loadCustom(context) }
  var showHow by remember { mutableStateOf(false) }
  var customName by remember { mutableStateOf("") }
  var customWatts by remember { mutableStateOf("") }
  var splash by remember { mutableStateOf(true) }
  var page by remember { mutableStateOf(0) }
  var showMenu by remember { mutableStateOf(false) }
  var batteryVoltage by remember { mutableStateOf(12) }
  var sunHours by remember { mutableStateOf("5") }
  var panelW by remember { mutableStateOf("350") }

  LaunchedEffect(rows.map { it.qty }.hashCode() + rows.map { it.watts }.hashCode() + rows.map { it.hours }.hashCode() + custom.size + custom.map { it.watts }.hashCode() + custom.map { it.hours }.hashCode()) { Store.save(context, rows, custom) }
  LaunchedEffect(Unit) { kotlinx.coroutines.delay(1200); splash = false }
  if (splash) { SplashScreen(); return }

  val items = buildList {
    rows.forEach { r -> if (r.qty > 0) add(CalcEngine.Item(r.def.name, r.def.name, r.watts.toIntOrNull() ?: 0, r.qty, if (r.inverter) 1 else r.def.surge, hours = r.hours.toIntOrNull() ?: 0)) }
    custom.forEach { c -> if (c.name.isNotBlank()) add(CalcEngine.Item(c.name, c.name, c.watts.toIntOrNull() ?: 0, 1, if (c.inverter) 1 else c.surge, hours = c.hours.toIntOrNull() ?: 0, custom = true)) }
  }
  val result = CalcEngine.calculate(items, 6)
  val totalWatts = items.filter { it.watts > 0 }.sumOf { it.watts * it.qty }
  val totalPeak = items.filter { it.watts > 0 }.sumOf { it.watts * it.qty * it.surge }
  val totalWh = items.filter { it.watts > 0 && it.hours > 0 }.sumOf { it.watts * it.qty * it.hours }
  val shareText = buildString {
    appendLine("LEAF SOLAR CALCULATOR — INVERTER SIZING")
    appendLine("Running load: ${result.runningW} W (${result.itemCount} items)")
    appendLine("Startup (peak) load: ${result.peakW} W")
    appendLine("Daily energy: ${result.dailyWh} Wh")
    appendLine("Inverter required: ${result.recommendedKva} KVA")
    appendLine("Battery capacity: ~ ${result.batteryAh12} Ah / 12V")
    appendLine("Solar panels: ~ ${result.panels350} x 350W")
  }

  MaterialTheme(colorScheme = lightColorScheme(primary = Green, surface = Color.White, background = Bg)) {
    Scaffold(topBar = {
      Surface(shadowElevation = 3.dp, color = GreenDark) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Lime, Green))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
          Spacer(Modifier.width(8.dp))
          Column { Text("Leaf Solar Calculator", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp); Text("Inverter Sizing", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
          Spacer(Modifier.weight(1f))
          if (result.runningW > 0) IconButton(onClick = { val i = Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }; context.startActivity(Intent.createChooser(i,"Share sizing")) }, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
          Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Menu, "Menu", tint = Color.White, modifier = Modifier.size(20.dp)) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              listOf("Inverter Sizing","Battery Sizing","Panel Sizing","Charge Controller","Cable Sizing").forEachIndexed { i,t ->
                DropdownMenuItem(text = { Text(t, fontWeight = if (page==i) FontWeight.ExtraBold else FontWeight.Normal) }, onClick = { page=i; showMenu=false })
              }
            }
          }
          TextButton(onClick = {
            rows.forEach { it.qty = 0; it.watts = ""; it.inverter = false; it.hours = "" }
            custom.clear()
          }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("CLEAR", color = Lime, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp) }
        }
      }
    }) { pad ->
      when (page) {
        0 -> InverterScreen(pad, result, shareText, context, showHow, { showHow = it }, rows, custom, customName, { customName = it }, customWatts, { customWatts = it }, { n -> custom.add(CustomRow(n, customWatts, CalcEngine.detectSurge(n))); customName=""; customWatts="" }, totalWatts, totalPeak, totalWh)
        1 -> Box(Modifier.padding(pad).fillMaxSize()) { BatterySizingPage(result.dailyWh) { v -> batteryVoltage = v; page = 2 } }
        2 -> Box(Modifier.padding(pad).fillMaxSize()) { PanelSizingPageFull(result.dailyWh, batteryVoltage, sunHours, { sunHours = it }, panelW, { panelW = it }) { page = 3 } }
        3 -> Box(Modifier.padding(pad).fillMaxSize()) {
               val pr = SizingEngine.sizePanels(result.dailyWh, sunHours.toDoubleOrNull() ?: 5.0, panelW.toIntOrNull() ?: 350, 0.78, batteryVoltage)
               ControllerPage(pr.arrayWatts, pr.panelWatts, pr.panelCount, batteryVoltage) { page = 4 }
             }
        4 -> Box(Modifier.padding(pad).fillMaxSize()) {
               val pr = SizingEngine.sizePanels(result.dailyWh, sunHours.toDoubleOrNull() ?: 5.0, panelW.toIntOrNull() ?: 350, 0.78, batteryVoltage)
               CablePage(pr.arrayWatts, batteryVoltage, pr.panelWatts, pr.panelCount)
             }
      }
    }
  }
}

@Composable
private fun InverterScreen(
  pad: PaddingValues,
  result: CalcEngine.Result,
  shareText: String,
  context: android.content.Context,
  showHow: Boolean,
  setShowHow: (Boolean)->Unit,
  rows: List<RowState>,
  custom: MutableList<CustomRow>,
  customName: String,
  setCustomName: (String)->Unit,
  customWatts: String,
  setCustomWatts: (String)->Unit,
  onAddCustom: (String)->Unit,
  totalWatts: Int,
  totalPeak: Int,
  totalWh: Int
) {
  LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    item {
      Surface(shape = RoundedCornerShape(14.dp), color = GreenDark) {
        Column(Modifier.padding(12.dp)) {
          Text("Size your solar inverter", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
          Spacer(Modifier.height(4.dp))
          Surface(color = Color(0x14FFFFFF), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { setShowHow(!showHow) }) { Text(if (showHow) "How do I find the watt?" else "How do I find the watt? ▾", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
          AnimatedVisibility(showHow) { Surface(color = Color(0xFF0E1B12), shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(top = 6.dp)) { Text("Check the nameplate for W/Watts. For motors/ACs use input watts, not HP. If only amps show, multiply by voltage (1.0A x 230V = 230W).", color = Color(0xFFD6E9D8), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(10.dp)) } }
        }
      }
    }
    item { SectionHeader(1, "Add your appliances") }
    items(rows) { ApplianceRow(it) }
    item {
      Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
        Column(Modifier.padding(10.dp)) {
          Text("+ Add a custom appliance", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(customName, setCustomName, label = { Text("Name", fontSize = 8.sp) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(52.dp))
            OutlinedTextField(customWatts, setCustomWatts, label = { Text("W", fontSize = 8.sp) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), modifier = Modifier.width(80.dp).height(52.dp))
          }
          Spacer(Modifier.height(6.dp))
          Button(onClick = { if (customName.isNotBlank()) onAddCustom(customName.trim()) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Text("ADD", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        }
      }
    }
    items(custom) { CustomRowView(it, onDelete = { custom.remove(it) }) }
    item {
      Surface(shape = RoundedCornerShape(12.dp), color = GreenDark) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("TOTAL WATTS", color = Lime, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold); Text("$totalWatts W", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
            Column(horizontalAlignment = Alignment.End) { Text("STARTUP PEAK", color = Color(0xFFB9C7BC), fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold); Text("$totalPeak W", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
          }
          Divider(color = Color(0xFF2A4A33))
          Row(verticalAlignment = Alignment.CenterVertically) { Text("TOTAL WATT-HOURS / DAY", color = Lime, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)); Text("$totalWh Wh", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
        }
      }
    }
    item { SectionHeader(2, "Inverter Sizing") }
    item { InlineResults(result, shareText, context) }
    item { Spacer(Modifier.height(20.dp)) }
  }
}

@Composable
private fun SplashScreen() {
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GreenDark, Green))), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Lime, Green))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
      Spacer(Modifier.height(14.dp))
      Text("LEAF SOLAR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
      Text("CALCULATOR", color = Lime, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 4.sp)
    }
  }
}

@Composable
private fun SectionHeader(n: Int, title: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(20.dp).clip(CircleShape).background(Lime), contentAlignment = Alignment.Center) { Text("$n", color = Color(0xFF08110A), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp) }
    Spacer(Modifier.width(6.dp)); Text(title, color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
  }
}

@Composable
private fun ApplianceRow(r: RowState) {
  val on = r.qty > 0
  val w = r.watts.toIntOrNull() ?: 0
  val total = w * r.qty
  val surge = if (r.inverter) 1 else r.def.surge
  val showSurge = on && surge > 1
  val hrs = r.hours.toIntOrNull()
  val showWh = on && w > 0 && hrs != null && hrs > 0
  Surface(shape = RoundedCornerShape(10.dp), color = if (on) Color(0xFFF1FAE8) else Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, if (on) Green else Line)) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(if (on) Lime else Color(0xFFE3F2D9)), contentAlignment = Alignment.Center) { Text(r.def.badge, color = if (on) Color(0xFF08110A) else Color(0xFF2E6B12), fontWeight = FontWeight.ExtraBold, fontSize = 8.5.sp) }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) { Text(r.def.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Ink, modifier = Modifier.weight(1f, fill = false)); if (showSurge) { Spacer(Modifier.width(4.dp)); Surface(color = WarnBg, shape = RoundedCornerShape(5.dp)) { Text("x$surge", color = Warn, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) } } }
          if (r.def.invToggle) Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = r.inverter, onCheckedChange = { r.inverter = it; if (r.qty == 0) r.qty = 1 }, modifier = Modifier.height(20.dp), colors = SwitchDefaults.colors(checkedTrackColor = Green)); Text("Inverter", fontSize = 9.sp, color = Green, fontWeight = FontWeight.Bold) }
        }
        QtyStepper(r.qty, onMinus = { if (r.qty > 0) r.qty-- }, onPlus = { r.qty++ }, onSet = { r.qty = it.coerceAtLeast(0) })
      }
      Spacer(Modifier.height(5.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(r.watts, { v -> r.watts = v.filter { it.isDigit() }; if (r.qty == 0 && v.isNotBlank()) r.qty = 1 }, label = { Text("W", fontSize = 8.sp) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(7.dp), modifier = Modifier.weight(1f).height(52.dp))
        OutlinedTextField(r.hours, { v -> r.hours = v.filter { it.isDigit() } }, label = { Text("Usage hrs/day", fontSize = 6.5.sp, maxLines = 1) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(7.dp), modifier = Modifier.width(96.dp).height(52.dp))
        if (on && w > 0) Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(78.dp)) {
          Text("W", color = Muted, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold); Text("$total", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
          if (showWh) { Text("Wh/day", color = Green, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold); Text("${total * hrs!!}", color = GreenDark, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
        }
      }
    }
  }
}

@Composable
private fun QtyStepper(qty: Int, onMinus: () -> Unit, onPlus: () -> Unit, onSet: (Int) -> Unit) {
  var edit by remember { mutableStateOf<String?>(null) }
  Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(shape = RoundedCornerShape(6.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line), modifier = Modifier.size(26.dp).clickable(onClick = onMinus)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp)) } }
    if (edit == null) Text("$qty", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.width(28.dp).clickable { edit = qty.toString() }, textAlign = TextAlign.Center)
    else OutlinedTextField(edit ?: "", { v -> edit = v.filter { it.isDigit() }; v.toIntOrNull()?.let(onSet) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(6.dp), modifier = Modifier.width(46.dp).height(42.dp))
    Surface(shape = RoundedCornerShape(6.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line), modifier = Modifier.size(26.dp).clickable(onClick = onPlus)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) } }
  }
}

@Composable
private fun CustomRowView(c: CustomRow, onDelete: () -> Unit) {
  var watts by remember { mutableStateOf(c.watts) }
  var checked by remember { mutableStateOf(c.surge > 1) }
  val tw = c.watts.toIntOrNull() ?: 0
  val hrs = c.hours.toIntOrNull()
  Surface(shape = RoundedCornerShape(10.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
    Column(Modifier.padding(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(c.name, { c.name = it }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp), label = { Text("Appliance name", fontSize = 8.sp) }, shape = RoundedCornerShape(7.dp), modifier = Modifier.weight(1f).height(52.dp))
        Spacer(Modifier.width(6.dp)); TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("x", color = Muted, fontWeight = FontWeight.Bold) }
      }
      Spacer(Modifier.height(5.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(watts, { v -> watts = v.filter { it.isDigit() }; c.watts = watts }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), label = { Text("W", fontSize = 8.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(7.dp), modifier = Modifier.weight(1f).height(52.dp))
        OutlinedTextField(c.hours, { v -> c.hours = v.filter { it.isDigit() } }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), label = { Text("Usage hrs", fontSize = 6.5.sp, maxLines = 1) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(7.dp), modifier = Modifier.width(86.dp).height(52.dp))
        if (tw > 0) Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(64.dp)) { Text("W", color = Muted, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold); Text("$tw", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold); if (hrs != null && hrs > 0) { Text("Wh/day", color = Green, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold); Text("${tw * hrs}", color = GreenDark, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) } }
        Spacer(Modifier.width(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (c.inverter) Color(0xFFE3F2D9) else WarnBg).clickable { c.inverter = !c.inverter; checked = c.surge > 1 && !c.inverter }.padding(horizontal = 4.dp, vertical = 3.dp)) {
          Checkbox(checked = c.inverter || checked, onCheckedChange = { if (it) c.inverter = true else { c.inverter = false; c.surge = CalcEngine.detectSurge(c.name) }; checked = c.surge > 1 && !c.inverter }, modifier = Modifier.size(20.dp))
          Text(if (c.inverter) "Inv" else "x${c.surge}", color = if (c.inverter) Green else Warn, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
        }
      }
    }
  }
}

@Composable
private fun InlineResults(r: CalcEngine.Result, shareText: String, context: android.content.Context) {
  if (r.runningW == 0) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
      Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Power, null, tint = Muted, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text("Add appliances, their wattage and daily hours to see inverter sizing.", color = Muted, fontSize = 12.sp)
      }
    }
    return
  }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    if (r.missing.isNotEmpty()) Surface(shape = RoundedCornerShape(8.dp), color = WarnBg) { Text("Add wattage for: ${r.missing.joinToString(", ")} — not included.", color = Warn, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
    Stat("Running load", "${r.runningW} W", sub = "${r.itemCount} items")
    Stat("Startup (peak) load", "${r.peakW} W", accent = true)
    if (r.surgeItems.isNotEmpty()) Surface(shape = RoundedCornerShape(8.dp), color = Bg) { Text("Surge applied to: ${r.surgeItems.take(4).joinToString(", ")}", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(8.dp)) }
    Stat("Daily energy use", "${r.dailyWh} Wh")
    Stat("Inverter required", "${r.recommendedKva} KVA", big = true)
    Stat("Battery capacity", "~ ${r.batteryAh12} Ah", sub = "12V")
    Stat("Solar panels", "~ ${r.panels350} x 350W")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
      Button(onClick = { val i = Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }; context.startActivity(Intent.createChooser(i,"Share sizing")) }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = GreenDark)) { Icon(Icons.Default.Share, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text("SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
    Surface(shape = RoundedCornerShape(10.dp), color = Bg) { Text("Motors/compressors draw extra startup current, so the inverter is sized for peak. Inverter models use soft-start (no surge). Figures are indicative; a site assessment finalises the design.", color = Muted, fontSize = 9.5.sp, lineHeight = 13.sp, modifier = Modifier.padding(10.dp)) }
  }
}

@Composable
private fun Stat(label: String, value: String, sub: String? = null, accent: Boolean = false, big: Boolean = false) {
  Surface(shape = RoundedCornerShape(10.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = if (big) 12.dp else 9.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(label, color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
      Text(value, color = if (accent) Warn else Ink, fontWeight = FontWeight.ExtraBold, fontSize = if (big) 20.sp else 14.sp)
      if (sub != null) { Spacer(Modifier.width(4.dp)); Text(sub, color = Muted, fontSize = 10.sp) }
    }
  }
}
