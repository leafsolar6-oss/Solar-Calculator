package ng.leafsolar.calculator

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Solar load calculator for engineers, students and homeowners.
 * Ports the website's calculation rules exactly, without any sales content.
 */
object CalcEngine {

  data class ApplianceDef(
    val name: String, val badge: String,
    val surge: Int, val invToggle: Boolean
  )

  val APPS = listOf(
    ApplianceDef("Light Bulbs", "LB", 1, false),
    ApplianceDef("Ceiling Fan", "CF", 3, true),
    ApplianceDef("Television", "TV", 1, false),
    ApplianceDef("Fridge / Freezer", "FR", 3, true),
    ApplianceDef("Air Conditioner", "AC", 3, true),
    ApplianceDef("Decoder / Router", "DR", 1, false),
    ApplianceDef("Laptop / Phone", "LP", 1, false),
    ApplianceDef("Sound System", "SS", 1, false),
    ApplianceDef("Standing Fan", "SF", 3, true),
    ApplianceDef("Microwave", "MW", 2, false),
    ApplianceDef("Water Pump", "WP", 3, false),
    ApplianceDef("Washing Machine", "WM", 3, false)
  )

  // Researched startup/surge multipliers (locked-rotor vs running watts):
  //  x3 = compressor & induction-motor loads (fridges, non-inverter ACs, fans, pumps,
  //        washing machines, compressors, power tools)
  //  x2 = heating appliances with transformers/motors & many universal-motor tools
  //  x1 = electronics, LED lighting and pure resistive loads (no inrush)
  private val SURGE_MAP = listOf(
    Regex("\\b(fan|fridge|freezer|refrigerator|air.?cond|a/?c|pump|washing|washer|compressor|motor|blender|grinder|mixer|drill|saw|sander|grinder|sewing|vacuum|dryer|dishwasher|extractor|generator|airer|cooler|dispenser|ice.?maker|deep.?fryer|air.?compressor)\\b", RegexOption.IGNORE_CASE) to 3,
    Regex("\\b(microwave|oven|heater|kettle|toaster|iron|press|geyser|boiler|induction|hot\\s?plate|coffee|espresso|rice.?cooker|slow.?cooker|pressure.?cooker|fryer|grill|waffle|steam|hair.?dry|curling|heat.?gun|welder|planer|router|jigsaw|sander|circular)\\b", RegexOption.IGNORE_CASE) to 2
  )
  fun detectSurge(name: String): Int = SURGE_MAP.firstOrNull { it.first.containsMatchIn(name) }?.second ?: 1

  data class Item(
    val id: String, val name: String, val watts: Int, val qty: Int,
    val surge: Int, val custom: Boolean = false, val isInverter: Boolean = false
  )

  data class Result(
    val runningW: Int,
    val peakW: Int,
    val dailyWh: Int,
    val exactKva: Double,
    val recommendedKva: Double,
    val batteryAh12: Int,
    val batteryAh24: Int,
    val batteryAh48: Int,
    val batteryKwh: Double,
    val panels350: Int,
    val panels450: Int,
    val chargeCurrentA: Int,
    val itemCount: Int,
    val missing: List<String>,
    val surgeItems: List<String>
  ) {
    val hasInput get() = runningW > 0 || missing.isNotEmpty()
  }

  private fun roundKva(kw: Double): Double {
    var e = ceil(kw * 2) / 2.0
    if (e < 0.5) e = 0.5
    return e
  }

  fun calculate(items: List<Item>, hours: Int): Result {
    var running = 0; var peak = 0; var count = 0
    val missing = mutableListOf<String>(); val surgeItems = mutableListOf<String>()
    items.forEach { it ->
      if (it.qty > 0) {
        if (it.watts < 1) {
          if (missing.size < 6) missing += it.name
        } else {
          count += it.qty
          val unitW = it.watts * it.qty
          running += unitW
          peak += unitW * it.surge
          if (it.surge > 1) surgeItems += "${it.qty}x ${it.name}"
        }
      }
    }
    val exactKva = if (peak > 0) roundKva((peak * 1.2) / 1000.0) else 0.5
    val recommendedKva = exactKva  // inverter must cover peak with 20% headroom
    val dailyWh = running * hours
    // Battery sizing (DoD ~0.8, losses ~1.3 -> same factor the site uses)
    val ah12 = ((dailyWh * 1.3) / 12.0).roundToInt()
    val ah24 = ((dailyWh * 1.3) / 24.0).roundToInt()
    val ah48 = ((dailyWh * 1.3) / 48.0).roundToInt()
    val batteryKwh = Math.round(dailyWh * 1.3 / 1000.0 * 100) / 100.0
    val panels350 = maxOf(2, ceil((running * hours * 0.7) / 350.0).toInt())
    val panels450 = maxOf(2, ceil((running * hours * 0.7) / 450.0).toInt())
    // PWM/MPPT charge controller rating: battery charging current for ~5 peak sun hours
    val chargeCurrent = maxOf(10, ceil((panels350 * 350.0) / 60.0).toInt())
    return Result(running, peak, dailyWh, exactKva, recommendedKva, ah12, ah24, ah48, batteryKwh,
      panels350, panels450, chargeCurrent, count, missing, surgeItems)
  }
}
