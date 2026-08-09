package ng.leafsolar.calculator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLEncoder

// Brand palette (matches website .lc variables)
private val Lime = Color(0xFF76D50B)
private val Green = Color(0xFF3CA506)
private val GreenDark = Color(0xFF102417)
private val Ink = Color(0xFF14201A)
private val Muted = Color(0xFF5E6B62)
private val Line = Color(0xFFE1E7E1)
private val Bg = Color(0xFFF5F9F3)
private val Warn = Color(0xFFB26A00)
private val WarnBg = Color(0xFFFFF4D6)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { CalculatorApp() }
  }
}

private data class RowState(
  val def: CalcEngine.ApplianceDef,
  var qty: Int = 0,
  var watts: String = "",
  var inverter: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorApp() {
  val context = LocalContext.current
  val rows = remember { CalcEngine.APPS.map { RowState(it) } }
  val custom = remember { mutableStateListOf<CustomRow>() }
  var hours by remember { mutableStateOf(6f) }
  var showHow by remember { mutableStateOf(false) }

  val items = remember(rows, custom, hours) {
    val list = mutableListOf<CalcEngine.Item>()
    rows.forEach { r ->
      if (r.qty > 0) {
        val surge = if (r.inverter) 1 else r.def.surge
        list += CalcEngine.Item(r.def.name, r.def.name, r.watts.toIntOrNull() ?: 0, r.qty, surge, isInverter = r.inverter)
      }
    }
    custom.forEach { c ->
      if (c.name.isNotBlank()) list += CalcEngine.Item(c.name, c.name, c.watts.toIntOrNull() ?: 0, 1, c.surge, custom = true)
    }
    list
  }
  val result = remember(items, hours) { CalcEngine.calculate(items, hours.toInt()) }
  var customName by remember { mutableStateOf("") }
  var customWatts by remember { mutableStateOf("") }

  MaterialTheme(colorScheme = lightColorScheme(primary = Green, surface = Color.White, background = Bg)) {
    Scaffold(
      topBar = {
        Surface(shadowElevation = 2.dp, color = Color.White) {
          Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Lime, Green))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bolt, null, tint = Color.White)
              }
              Spacer(Modifier.width(10.dp))
              Column {
                Text("Solar Load Calculator", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ink)
                Text("Estimate your system size", fontSize = 12.sp, color = Muted)
              }
            }
          }
        }
      }
    ) { pad ->
      LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header card
        item {
          Surface(shape = RoundedCornerShape(18.dp), color = GreenDark) {
            Column(Modifier.padding(18.dp)) {
              Text("Size your solar system", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
              Spacer(Modifier.height(4.dp))
              Text("Add the appliances you want to power, then enter how many and the watts of each.", color = Color(0xFFB9C7BC), fontSize = 13.sp, lineHeight = 18.sp)
              Spacer(Modifier.height(6.dp))
              Surface(color = Color(0x14FFFFFF), shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { showHow = !showHow }) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                  Text(if (showHow) "How do I find the watt?" else "How do I find the watt? ▾", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
              AnimatedVisibility(showHow) {
                Surface(color = Color(0xFF0E1B12), shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                  Text("Check the sticker/nameplate on the appliance — it shows a number followed by \"W\" or \"Watts\". For ACs and motors use rated input watts, not HP. If it only shows amps, multiply amps × voltage (e.g. 1.0A × 230V ≈ 230W).",
                    color = Color(0xFFD6E9D8), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(12.dp))
                }
              }
            }
          }
        }

        // Section 1: appliances
        item { SectionHeader(1, "Choose your appliances") }
        items(rows) { r -> ApplianceRow(r) }

        // Custom appliance
        item {
          Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
            Column(Modifier.padding(12.dp)) {
              Text("+ Add a custom appliance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Spacer(Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(customName, { customName = it },
                  label = { Text("Name e.g. Decoder") }, singleLine = true,
                  shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
                OutlinedTextField(customWatts, { customWatts = it.filter { c -> c.isDigit() } },
                  label = { Text("Watts") }, singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  shape = RoundedCornerShape(10.dp), modifier = Modifier.width(96.dp))
              }
              Spacer(Modifier.height(8.dp))
              Button(onClick = {
                if (customName.isNotBlank()) {
                  custom.add(CustomRow(customName.trim(), customWatts, CalcEngine.detectSurge(customName)))
                  customName = ""; customWatts = ""
                }
              }, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink)) {
                Text("ADD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
        items(custom) { c -> CustomRowView(c, onDelete = { custom.remove(c) }) }

        // Hours slider
        item {
          Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
            Column(Modifier.padding(14.dp)) {
              Text("Average hours of use per day", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Spacer(Modifier.height(2.dp))
              Text("— ${hours.toInt()} hrs", color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
              Slider(value = hours, onValueChange = { hours = it }, valueRange = 1f..18f, steps = 16)
              Row { Text("1h", fontSize = 10.sp, color = Muted); Spacer(Modifier.weight(1f)); Text("18h+", fontSize = 10.sp, color = Muted) }
            }
          }
        }

        // Section 2: results
        item { SectionHeader(2, "Your estimate") }
        item { ResultCard(result, onQuote = {
          val msg = "Hello Leaf Solar! I used the load calculator. Running load: ${result.runningW}W; startup (peak) load: ${result.peakW}W; about ${hours.toInt()} hrs/day. Recommended: ${result.rec.name} (${CalcEngine.formatNaira(result.rec.price)}). Please send a quote."
          val url = "https://wa.me/2347037561216?text=" + URLEncoder.encode(msg, "UTF-8")
          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }, onPackage = {
          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }) }
        item { Spacer(Modifier.height(40.dp)) }
      }
    }
  }
}

@Composable
private fun SectionHeader(n: Int, title: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(24.dp).clip(CircleShape).background(Lime), contentAlignment = Alignment.Center) {
      Text("$n", color = Color(0xFF08110A), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    }
    Spacer(Modifier.width(8.dp))
    Text(title, color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
  }
}

@Composable
private fun ApplianceRow(r: RowState) {
  val on = r.qty > 0
  Surface(shape = RoundedCornerShape(12.dp), color = if (on) Color(0xFFF1FAE8) else Color.White,
    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (on) Green else Line)) {
    Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (on) Brush.linearGradient(listOf(Lime, Lime)) else Color(0xFFE3F2D9)), contentAlignment = Alignment.Center) {
        Text(r.def.badge, color = if (on) Color(0xFF08110A) else Color(0xFF2E6B12), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
      }
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)) {
        Text(r.def.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Ink)
        if (r.def.invToggle) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = r.inverter, onCheckedChange = { r.inverter = it; r.qty = if (r.qty == 0) 1 else r.qty },
              modifier = Modifier.height(24.dp), colors = SwitchDefaults.colors(checkedTrackColor = Green))
            Text("Inverter model", fontSize = 10.sp, color = Green, fontWeight = FontWeight.Bold)
          }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(r.watts, { v -> r.watts = v.filter { it.isDigit() }; if (r.qty == 0 && v.isNotBlank()) r.qty = 1 },
          label = { Text("W", fontSize = 10.sp) }, singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(8.dp), modifier = Modifier.width(90.dp).height(52.dp))
      }
      QtyStepper(r.qty, onMinus = { if (r.qty > 0) r.qty-- }, onPlus = { r.qty++ })
    }
  }
}

@Composable
private fun QtyStepper(qty: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line),
      modifier = Modifier.size(28.dp).clickable(onClick = onMinus)) {
      Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
    }
    Text("$qty", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.width(28.dp).padding(top = 2.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Surface(shape = RoundedCornerShape(8.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line),
      modifier = Modifier.size(28.dp).clickable(onClick = onPlus)) {
      Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
    }
  }
}

private data class CustomRow(var name: String, var watts: String, var surge: Int)

@Composable
private fun CustomRowView(c: CustomRow, onDelete: () -> Unit) {
  var watts by remember { mutableStateOf(c.watts) }
  var checked by remember { mutableStateOf(c.surge > 1) }
  Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        OutlinedTextField(c.name, { c.name = it }, singleLine = true,
          label = { Text("Appliance name") }, shape = RoundedCornerShape(9.dp), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(watts, { v -> watts = v.filter { it.isDigit() }; c.watts = watts }, singleLine = true,
            label = { Text("Watts") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(9.dp), modifier = Modifier.width(110.dp))
          Spacer(Modifier.width(10.dp))
          Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(WarnBg).clickable {
              checked = !checked; c.surge = if (checked) 3 else 1
            }.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Checkbox(checked = checked, onCheckedChange = { checked = it; c.surge = if (it) 3 else 1 }, modifier = Modifier.size(28.dp))
            Text("×${if (checked) 3 else 1} surge", color = Warn, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
          }
        }
      }
      Spacer(Modifier.width(8.dp))
      IconButton(onClick = onDelete) { Text("✕", color = Muted, fontWeight = FontWeight.Bold) }
    }
  }
}

@Composable
private fun ResultCard(r: CalcEngine.Result, onQuote: () -> Unit, onPackage: (String) -> Unit) {
  if (!r.hasInput) {
    EmptyState("Tap + next to an appliance and type its wattage. Your estimate appears here.")
    return
  }
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (r.missing.isNotEmpty()) {
      Surface(shape = RoundedCornerShape(10.dp), color = WarnBg) {
        Text("Add the wattage for: ${r.missing.joinToString(", ")} — not included.",
          color = Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
      }
    }
    if (r.runningW > 0) {
      Stat("Running load", "${r.runningW} W", sub = "/ ${r.itemCount} item${if (r.itemCount == 1) "" else "s"}")
      Stat("Startup (peak) load", "${r.peakW} W", accent = true)
      if (r.surgeItems.isNotEmpty()) {
        Surface(shape = RoundedCornerShape(10.dp), color = Bg) {
          Text("Startup surge applied to: ${r.surgeItems.take(5).joinToString(", ")}",
            color = Muted, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
        }
      }
      Stat("Daily energy use", "${r.dailyWh} Wh")
      Stat("Inverter required", "${r.exactKva} KVA")
      Stat("Battery capacity", "≈ ${r.batteryAh} Ah", sub = "/ 12V")
      Stat("Solar panels", "≈ ${r.panels} x 350W")

      Surface(shape = RoundedCornerShape(16.dp), color = GreenDark) {
        Column(Modifier.padding(16.dp)) {
          Surface(color = Lime, shape = RoundedCornerShape(99.dp)) {
            Text("RECOMMENDED PACKAGE", color = Color(0xFF08110A), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
          }
          Spacer(Modifier.height(8.dp))
          Text(r.rec.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
          Spacer(Modifier.height(2.dp))
          Text(CalcEngine.formatNaira(r.rec.price), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
          Spacer(Modifier.height(6.dp))
          Text(r.rec.note + " Includes " + r.rec.battery + ", panels, inverter, delivery and installation.",
            color = Color(0xFFDCEFDE), fontSize = 12.sp, lineHeight = 17.sp)
          Spacer(Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onPackage(r.rec.url) }, shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Color(0xFF08110A)),
              modifier = Modifier.weight(1f)) { Text("VIEW PACKAGE", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            OutlinedButton(onClick = onQuote, shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3C5A44)),
              modifier = Modifier.weight(1f)) { Text("GET QUOTE", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
          }
        }
      }
      Surface(shape = RoundedCornerShape(12.dp), color = Bg) {
        Text("Motors and compressors (fans, fridges, pumps, non-inverter ACs, microwaves) draw extra current at startup, so the inverter is sized for the peak. Inverter fridges and inverter ACs use soft-start and have no surge. These figures are indicative; a free site assessment finalises the design. Prices include installation within Ibadan.",
          color = Muted, fontSize = 10.5.sp, lineHeight = 15.sp, modifier = Modifier.padding(12.dp))
      }
    }
  }
}

@Composable
private fun Stat(label: String, value: String, sub: String? = null, accent: Boolean = false) {
  Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
      Text(value, color = if (accent) Warn else Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
      if (sub != null) { Spacer(Modifier.width(4.dp)); Text(sub, color = Muted, fontSize = 11.sp) }
    }
  }
}

@Composable
private fun EmptyState(msg: String) {
  Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Power, null, tint = Muted)
      Spacer(Modifier.width(10.dp))
      Text(msg, color = Muted, fontSize = 13.sp)
    }
  }
}
