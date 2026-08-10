package ng.leafsolar.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBG = Color(0xFFFFFFFF)
private val CardLine = Color(0xFFE1E7E1)

@Composable
fun BatterySizingPage(dailyWh: Int, onNext: (Int) -> Unit) {
  var whInput by remember { mutableStateOf(if (dailyWh > 0) dailyWh.toString() else "") }
  var autonomy by remember { mutableStateOf("1") }
  var voltage by remember { mutableStateOf("12") }
  var dod by remember { mutableStateOf("0.5") }
  var dodPct by remember { mutableStateOf("50") }
  var unitAh by remember { mutableStateOf("200") }
  val wh = whInput.toIntOrNull() ?: 0
  val aut = autonomy.toIntOrNull() ?: 1
  val v = voltage.toIntOrNull() ?: 12
  val dp = dodPct.toIntOrNull() ?: 50
  val d = (dp / 100.0).coerceIn(0.1, 0.95)
  val r = SizingEngine.sizeBattery(wh, v, d, aut)
  val unit = unitAh.toIntOrNull() ?: 200
  val plan = SizingEngine.planBank(r.bankAh, v, unit, 12)
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    InputCard {
      NumField("Total watt-hours / day (Wh)", whInput, { whInput = it.filter { c -> c.isDigit() } }, if (dailyWh>0) dailyWh.toString() else "e.g. 2400")
      Spacer(Modifier.height(6.dp))
      Text(if (dailyWh>0) "Pre-filled from inverter sizing — edit if you have your own figure." else "Enter your total daily energy use in watt-hours.", color = Muted, fontSize = 10.sp)
    }
    InputCard {
      NumField("Autonomy (days)", autonomy, { autonomy = it.filter { c -> c.isDigit() } }, "1")
      Spacer(Modifier.height(8.dp))
      Text("System voltage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("12","24","48").forEach { sv ->
          FilterChip(selected = voltage == sv, onClick = { voltage = sv }, label = { Text("${sv}V") })
        }
      }
      Spacer(Modifier.height(8.dp))
      Text("Depth of discharge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("50" to "Flooded", "60" to "AGM/Gel", "80" to "LiFePO4").forEach { (k,lab) ->
          FilterChip(selected = dodPct == k, onClick = { dodPct = k; dod = (k.toInt()/100.0).toString() }, label = { Text(lab) })
        }
      }
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(dodPct, { v2 -> val n = v2.filter{it.isDigit()}.take(2); dodPct=n; if(n.isNotEmpty()) dod=(n.toInt()/100.0).toString() },
        label = { Text("Custom DoD (%)", fontSize = 10.sp) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(),
        trailingIcon = { Text("%", color=Muted, modifier=Modifier.padding(end=10.dp)) })
      Spacer(Modifier.height(4.dp))
      Text("Using ${dp}% depth of discharge.", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
    Stat("Required usable capacity", "${r.usableAh} Ah", sub = "@${v}V")
    Stat("Bank size (${dp}% DoD)", "${r.bankAh} Ah", sub = "≈ ${r.bankKwh} kWh")
    if (plan.units > 0) {
      Card("Suggested bank (${unit}Ah 12V units)", "${plan.units} units = ${plan.series}S × ${plan.parallel}P  (≈ ${plan.kwh} kWh)")
    }
    Spacer(Modifier.height(4.dp))
    Button(onClick = { onNext(v) }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(containerColor = GreenDark)) {
      Text("CONTINUE TO PANEL SIZING →", fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
    Note("Flooded lead-acid: ~50% DoD. AGM/Gel: ~60%. LiFePO4: 80–90%. Shallower discharge extends battery life.")
  }
}

@Composable
fun PanelSizingPage(dailyWh: Int, systemVoltage: Int, onNext: () -> Unit) {
  var sun by remember { mutableStateOf("5") }
  var panelW by remember { mutableStateOf("350") }
  val sh = sun.toDoubleOrNull() ?: 5.0
  val pw = panelW.toIntOrNull() ?: 350
  val r = SizingEngine.sizePanels(dailyWh, sh, pw, 0.78, systemVoltage)
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Card("Daily energy use", "$dailyWh Wh")
    InputCard {
      NumField("Peak sun hours/day", sun, { sun = it.filter { c -> c.isDigit() || c=='.' } }, "5")
      Spacer(Modifier.height(8.dp))
      NumField("Panel wattage (W)", panelW, { panelW = it.filter { c -> c.isDigit() } }, "350")
    }
    Stat("Array size needed", "${r.arrayWatts} W")
    Stat("Panels required", "${r.panelCount} × ${r.panelWatts}W", big = true)
    Card("Suggested layout", "${r.strings} string${if(r.strings>1)"s" else ""} of ${r.perString} panels")
    Stat("Approx charge current", "${r.chargeCurrentA} A", sub = "@${systemVoltage}V")
    Spacer(Modifier.height(4.dp))
    Button(onClick = onNext, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(containerColor = GreenDark)) {
      Text("CONTINUE TO CHARGE CONTROLLER →", fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
    Note("Derate factor ~0.78 accounts for heat, dust, wiring and charge-controller losses. Use local peak-sun hours.")
  }
}

@Composable
fun ControllerPage(arrayW: Int, panelW: Int, count: Int, batteryV: Int, onNext: () -> Unit) {
  val r = SizingEngine.sizeController(panelW, count, batteryV)
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Card("Solar array", "${r.arrayWatts} W (${count} × ${panelW}W)")
    Stat("Recommended controller", r.stdRating, big = true)
    Stat("Minimum current", "${r.minCurrentA} A", sub = "with 25% safety")
    Card("Type", r.type)
    Spacer(Modifier.height(4.dp))
    Button(onClick = onNext, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(containerColor = GreenDark)) {
      Text("CONTINUE TO CABLE SIZING →", fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
    Note("MPPT is strongly recommended for arrays over ~400W and for 24/48V banks. Ensure the controller's PV Voc rating exceeds your panels' open-circuit voltage in series.")
  }
}

@Composable
fun CablePage(arrayW: Int, systemVoltage: Int, panelW: Int, count: Int) {
  var length by remember { mutableStateOf("10") }
  var dc by remember { mutableStateOf("20") }
  var acLoad by remember { mutableStateOf("3000") }
  val len = length.toDoubleOrNull() ?: 10.0
  val dci = dc.toDoubleOrNull() ?: 20.0
  val dcRes = SizingEngine.sizeDcCable(len, dci, systemVoltage, 3.0)
  val ac = acLoad.toIntOrNull() ?: 3000
  val acRes = SizingEngine.sizeAcCable(ac, 230, 15.0)
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("DC CABLE (battery/inverter/array)", color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    InputCard {
      NumField("One-way cable length (m)", length, { length = it.filter { c -> c.isDigit() || c=='.' } }, "10")
      Spacer(Modifier.height(8.dp))
      NumField("Max DC current (A)", dc, { dc = it.filter { c -> c.isDigit() || c=='.' } }, "20")
    }
    Stat("Min conductor size", "${dcRes.mm2} mm²", sub = "≈ ${dcRes.awg}")
    Card("Use standard cable", "${dcRes.stdMm2} mm²  (${dcRes.awg})")
    Text("AC CABLE (loads)", color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    InputCard { NumField("Largest AC load (W)", acLoad, { acLoad = it.filter { c -> c.isDigit() } }, "3000") }
    Stat("Load current", "${acRes.currentA} A @ 230V")
    Stat("Breaker rating", "${acRes.breakerA} A")
    Card("AC cable (≈15m run)", "${acRes.mm2} mm²  (${acRes.awg})")
    Note("Voltage drop kept within 3%. Use larger cable for longer runs or higher current. Fuse/breaker each circuit and use properly rated DC cable for solar.")
  }
}

// ---- shared UI ----
@Composable
fun Stat(label: String, value: String, sub: String? = null, big: Boolean = false) {
  Surface(shape = RoundedCornerShape(10.dp), color = CardBG, border = androidx.compose.foundation.BorderStroke(1.5.dp, CardLine)) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = if (big) 12.dp else 9.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(label, color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
      Text(value, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = if (big) 20.sp else 14.sp)
      if (sub != null) { Spacer(Modifier.width(4.dp)); Text(sub, color = Muted, fontSize = 10.sp) }
    }
  }
}
@Composable
fun Card(title: String, value: String) {
  Surface(shape = RoundedCornerShape(10.dp), color = GreenDark) {
    Column(Modifier.padding(12.dp)) {
      Text(title.uppercase(), color = Lime, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
      Spacer(Modifier.height(3.dp)); Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
  }
}
@Composable
fun InputCard(content: @Composable ColumnScope.() -> Unit) {
  Surface(shape = RoundedCornerShape(10.dp), color = CardBG, border = androidx.compose.foundation.BorderStroke(1.5.dp, CardLine)) {
    Column(Modifier.padding(12.dp), content = content)
  }
}
@Composable
fun NumField(label: String, value: String, onChange: (String)->Unit, placeholder: String) {
  OutlinedTextField(value, onChange, label = { Text(label, fontSize = 10.sp) }, singleLine = true,
    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
}
@Composable
fun Note(text: String) {
  Surface(shape = RoundedCornerShape(10.dp), color = Bg) { Text(text, color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(10.dp)) }
}

@Composable
fun PanelSizingPageFull(dailyWh: Int, systemVoltage: Int, sunStr: String, onSun: (String)->Unit, panelStr: String, onPanel: (String)->Unit, onNext: () -> Unit) {
  val sh = sunStr.toDoubleOrNull() ?: 5.0
  val pw = panelStr.toIntOrNull() ?: 350
  val r = SizingEngine.sizePanels(dailyWh, sh, pw, 0.78, systemVoltage)
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Card("Daily energy use", "$dailyWh Wh")
    InputCard {
      NumField("Peak sun hours/day", sunStr, onSun, "5")
      Spacer(Modifier.height(8.dp))
      NumField("Panel wattage (W)", panelStr, onPanel, "350")
    }
    Stat("Array size needed", "${r.arrayWatts} W")
    Stat("Panels required", "${r.panelCount} x ${r.panelWatts}W", big = true)
    Card("Suggested layout", "${r.strings} string${if(r.strings>1)"s" else ""} of ${r.perString} panels")
    Stat("Approx charge current", "${r.chargeCurrentA} A", sub = "@${systemVoltage}V")
    Spacer(Modifier.height(4.dp))
    Button(onClick = onNext, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GreenDark)) { Text("CONTINUE TO CHARGE CONTROLLER →", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
    Note("Derate ~0.78 for heat, dust, wiring and controller losses.")
  }
}
