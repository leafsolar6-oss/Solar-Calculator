package ng.leafsolar.calculator

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object CalcEngine {
  data class ApplianceDef(val name: String, val badge: String, val surge: Int, val invToggle: Boolean)

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

  private val SURGE_MAP = listOf(
    Regex("\\b(fan|fridge|freezer|refrigerator|air.?cond|a/?c|pump|washing|washer|compressor|motor|blender|grinder|mixer|drill|saw|sander|sewing|vacuum|dryer|dishwasher|extractor|generator|cooler|dispenser|ice.?maker|deep.?fryer|air.?compressor)\\b", RegexOption.IGNORE_CASE) to 3,
    Regex("\\b(microwave|oven|heater|kettle|toaster|iron|press|geyser|boiler|induction|hot\\s?plate|coffee|espresso|rice.?cooker|slow.?cooker|pressure.?cooker|fryer|grill|waffle|steam|hair.?dry|curling|heat.?gun|welder|planer|router|jigsaw|circular)\\b", RegexOption.IGNORE_CASE) to 2
  )
  fun detectSurge(name: String): Int = SURGE_MAP.firstOrNull { it.first.containsMatchIn(name) }?.second ?: 1

  data class Item(
    val id: String, val name: String, val watts: Int, val qty: Int,
    val surge: Int, val hours: Int = 6,
    val custom: Boolean = false, val isInverter: Boolean = false
  )

  data class Result(
    val runningW: Int, val peakW: Int, val dailyWh: Int,
    val exactKva: Double, val recommendedKva: Double,
    val batteryAh12: Int, val panels350: Int,
    val itemCount: Int, val missing: List<String>, val surgeItems: List<String>
  ) {
    val hasInput get() = runningW > 0 || missing.isNotEmpty()
  }

  private fun roundKva(kw: Double): Double {
    var e = ceil(kw * 2) / 2.0
    if (e < 0.5) e = 0.5
    return e
  }

  fun calculate(items: List<Item>, defaultHours: Int): Result {
    var running = 0; var peak = 0; var count = 0; var daily = 0
    val missing = mutableListOf<String>(); val surgeItems = mutableListOf<String>()
    items.forEach {
      if (it.qty > 0) {
        if (it.watts < 1) { if (missing.size < 6) missing += it.name }
        else {
          count += it.qty
          val unitW = it.watts * it.qty
          running += unitW
          peak += unitW * it.surge
          daily += unitW * it.hours.coerceIn(1, 24)
          if (it.surge > 1) surgeItems += "${it.qty}x ${it.name}"
        }
      }
    }
    val exactKva = if (peak > 0) roundKva((peak * 1.2) / 1000.0) else 0.5
    val ah12 = ((daily * 1.3) / 12.0).roundToInt()
    val panels350 = maxOf(2, ceil((daily * 0.7) / 350.0).toInt())
    return Result(running, peak, daily, exactKva, exactKva, ah12, panels350, count, missing, surgeItems)
  }
}
