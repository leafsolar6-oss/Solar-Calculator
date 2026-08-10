package ng.leafsolar.calculator

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.math.roundToInt

/**
 * Standard solar system sizing formulas (off-grid / hybrid).
 * References: industry practice documented in HeatSpring Magazine solar
 * sizing guides, NAZ Solar Electric, Victron/Watts24 wiring guides.
 */
object SizingEngine {

  // ---------- BATTERY SIZING ----------
  // Days of autonomy (default 1), battery DoD by chemistry, system voltage,
  // inverter/round-trip efficiency (~0.9), temperature derate.
  data class BatteryResult(
    val dailyWh: Int,
    val usableAh: Double,        // raw Ah before DoD
    val bankAh: Double,          // Ah after DoD
    val bankKwh: Double,
    val series: Int,             // batteries in series
    val parallel: Int,
    val totalUnits: Int,
    val systemVoltage: Int,
    val chemistry: String,
    val dod: Double,
    val autonomyDays: Int
  )

  fun sizeBattery(dailyWh: Int, systemVoltage: Int = 12, dod: Double = 0.5, autonomyDays: Int = 1, efficiency: Double = 0.9): BatteryResult {
    val days = autonomyDays.coerceAtLeast(1)
    // required usable energy with autonomy and inverter losses
    val requiredWh = dailyWh * days / efficiency
    val usableAh = requiredWh.toDouble() / systemVoltage
    val bankAh = usableAh / dod
    val bankKwh = bankAh * systemVoltage / 1000.0
    return BatteryResult(dailyWh, Math.round(usableAh * 10) / 10.0, Math.round(bankAh * 10) / 10.0,
      Math.round(bankKwh * 100) / 100.0, 0, 0, 0, systemVoltage,
      if (dod >= 0.8) "LiFePO4" else if (dod >= 0.6) "AGM/Gel" else "Flooded lead-acid", dod, days)
  }

  /** Build a concrete bank from a chosen unit (e.g. 200Ah 12V battery). */
  data class BankPlan(val units: Int, val series: Int, val parallel: Int, val ah: Int, val kwh: Double)
  fun planBank(bankAh: Double, systemVoltage: Int, unitAh: Int = 200, unitVoltage: Int = 12): BankPlan {
    if (bankAh <= 0 || unitAh <= 0) return BankPlan(0,0,0,0,0.0)
    val series = if (unitVoltage > 0) (systemVoltage / unitVoltage).coerceAtLeast(1) else 1
    val strings = ceil(bankAh / unitAh).toInt().coerceAtLeast(1)
    val total = series * strings
    val kwh = (total * unitAh * unitVoltage) / 1000.0
    return BankPlan(total, series, strings, unitAh, Math.round(kwh * 100) / 100.0)
  }

  // ---------- PANEL (ARRAY) SIZING ----------
  data class PanelResult(
    val dailyWh: Int,
    val sunHours: Double,
    val derate: Double,            // system derate (0.7-0.85)
    val arrayWatts: Int,
    val panelWatts: Int,
    val panelCount: Int,
    val chargeCurrentA: Double,
    val strings: Int,
    val perString: Int
  )

  fun sizePanels(dailyWh: Int, sunHours: Double = 5.0, panelW: Int = 350, derate: Double = 0.78, chargeV: Int = 12): PanelResult {
    val sh = sunHours.coerceAtLeast(1.0)
    val arrayW = ceil((dailyWh / derate) / sh).toInt()
    val count = (ceil(arrayW.toDouble() / panelW)).toInt().coerceAtLeast(1)
    val chargeA = (panelW * count) / chargeV.toDouble() / 0.9  // rough PV current for controller sizing
    // Simple string layout: keep strings ≤15 panels (Mppt practical); otherwise parallel strings
    val perString = minOf(15, count)
    val strings = ceil(count / perString.toDouble()).toInt().coerceAtLeast(1)
    return PanelResult(dailyWh, sh, derate, arrayW, panelW, count, Math.round(chargeA * 10) / 10.0, strings, perString)
  }

  // ---------- CHARGE CONTROLLER ----------
  data class ControllerResult(
    val type: String,             // MPPT recommended
    val arrayWatts: Int,
    val pvVoltage: Int,
    val minCurrentA: Double,
    val recommendedA: Int,
    val stdRating: String         // e.g. "30A MPPT"
  )

  fun sizeController(panelW: Int, count: Int, batteryV: Int = 12, pvVoc: Int = 45): ControllerResult {
    val watts = panelW * count
    val iscPer = panelW / pvVoc.toDouble()  // rough Isc per panel (Voc ~ 38-46 for 350W)
    // MPPT: I = W / Vbatt, plus 25% safety. PWM would need higher current & waste.
    val raw = (watts / batteryV.toDouble()) * 1.25
    val rec = listOf(10, 15, 20, 30, 40, 60, 80, 100).firstOrNull { it >= raw } ?: 100
    val type = if (watts >= 400) "MPPT" else "MPPT (PWM ok for tiny systems)"
    return ControllerResult(type, watts, pvVoc, Math.round(raw * 10) / 10.0, rec, "${rec}A $type")
  }

  // ---------- CABLE SIZING (AC & DC) ----------
  // 3% max voltage drop. DC cables use Vd = 2*L*I*rho/A; copper rho=0.0175 ohm.mm2/m.
  data class CableResult(
    val lengthM: Double,
    val currentA: Double,
    val voltage: Int,
    val maxDropV: Double,
    val mm2: Double,
    val awg: String,
    val stdMm2: Double         // nearest standard cable size
  )

  private val STD_MM2 = listOf(1.0, 1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0, 120.0)
  fun sizeDcCable(lengthOneWayM: Double, currentA: Double, voltage: Int = 12, dropPct: Double = 3.0): CableResult {
    val drop = voltage * dropPct / 100.0
    val area = if (drop > 0 && lengthOneWayM > 0 && currentA > 0) (2 * lengthOneWayM * currentA * 0.0183) / drop else 0.0
    val std = STD_MM2.firstOrNull { it >= area } ?: area
    return CableResult(lengthOneWayM, currentA, voltage, Math.round(drop * 100) / 100.0,
      Math.round(area * 100) / 100.0, mm2ToAwg(std), std)
  }

  // AC cable: per-circuit current, use standard 3% drop with AC 230V.
  // For a typical circuit, breaker rating = current * 1.25 rounded to std.
  data class AcCableResult(val loadW: Int, val voltage: Int, val currentA: Double, val breakerA: Int, val mm2: Double, val awg: String)
  fun sizeAcCable(loadW: Int, voltage: Int = 230, lengthM: Double = 15.0): AcCableResult {
    val i = loadW.toDouble() / voltage
    val breaker = listOf(6, 10, 16, 20, 25, 32, 40, 50, 63).firstOrNull { it >= i * 1.25 } ?: 63
    val drop = voltage * 0.03
    val area = if (drop > 0) (2 * lengthM * i * 0.0183) / drop else 0.0
    val std = STD_MM2.firstOrNull { it >= area } ?: area
    return AcCableResult(loadW, voltage, Math.round(i * 100) / 100.0, breaker, std, mm2ToAwg(std))
  }

  // AWG approximation (mm² -> AWG) for reference
  fun mm2ToAwg(mm2: Double): String {
    if (mm2 <= 0) return "-"
    val awg = when {
      mm2 >= 120 -> "4/0"
      mm2 >= 95 -> "3/0"
      mm2 >= 70 -> "2/0"
      mm2 >= 50 -> "1/0"
      mm2 >= 35 -> "2 AWG"
      mm2 >= 25 -> "4 AWG"
      mm2 >= 16 -> "6 AWG"
      mm2 >= 10 -> "8 AWG"
      mm2 >= 6 -> "10 AWG"
      mm2 >= 4 -> "12 AWG"
      mm2 >= 2.5 -> "14 AWG"
      mm2 >= 1.5 -> "16 AWG"
      else -> "18 AWG"
    }
    return "$awg"
  }
}
