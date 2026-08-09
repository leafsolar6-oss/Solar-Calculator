package ng.leafsolar.calculator

import kotlin.math.ceil
import kotlin.math.roundToInt

/** Exact replica of the website solar load calculator rules. */
object CalcEngine {

  data class ApplianceDef(
    val name: String, val badge: String,
    val surge: Int, val invToggle: Boolean
  )

  /** The 12 common appliances from the website, in the same order. */
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

  data class Package(
    val kva: Double, val name: String, val price: Long,
    val url: String, val battery: String, val note: String
  )

  val PACKAGES = listOf(
    Package(1.5, "1.5KVA Tubular Package", 1_200_000L,
      "https://leafsolar.ng/product-category/solar-inverters/solar-packages/tubular-packages/",
      "1 x 220AH tubular battery",
      "Best for lights, fan, TV, phone charging and a small decoder."),
    Package(3.5, "3.5KVA Tubular Package", 2_300_000L,
      "https://leafsolar.ng/product-category/solar-inverters/solar-packages/tubular-packages/",
      "2 x 220AH tubular batteries",
      "Adds a fridge, more fans and several outlets on a budget."),
    Package(3.5, "3.5KVA Lithium Package", 4_000_000L,
      "https://leafsolar.ng/product-category/solar-inverters/solar-packages/lithium-packages/",
      "5kWh+ LiFePO4 battery",
      "Maintenance-free, 8-12 year battery life, faster charging."),
    Package(5.0, "5KVA Tubular Package", 3_800_000L,
      "https://leafsolar.ng/product-category/solar-inverters/solar-packages/tubular-packages/",
      "4 x 220AH tubular batteries",
      "Handles most homes: fridge, TV, fans, lights, pumping."),
    Package(5.0, "5KVA Lithium Package", 5_200_000L,
      "https://leafsolar.ng/product-category/solar-inverters/solar-packages/lithium-packages/",
      "10kWh+ LiFePO4 battery",
      "Quiet, long-lasting power for heavier household use."),
    Package(7.5, "7.5KVA Package", 8_500_000L,
      "https://leafsolar.ng/product-category/solar-inverters/solar-packages/lithium-packages/",
      "15kWh lithium bank",
      "For larger homes with multiple ACs or heavy appliances."),
    Package(10.0, "10KVA Commercial Package", 14_800_000L,
      "https://leafsolar.ng/product-category/solar-inverters/solar-packages/commercial-packages/",
      "Lithium bank + hybrid inverter",
      "For offices, shops, clinics and small businesses."),
    Package(20.0, "20KVA+ Industrial Package", 24_800_000L,
      "https://leafsolar.ng/product-category/solar-inverters/solar-packages/industrial-packages/",
      "High-capacity lithium bank",
      "For factories, hotels and large complexes. Contact us for a custom design.")
  )

  // Motor/compressor/heating words -> default surge multiplier (same regexes as site)
  private val SURGE_MAP = listOf(
    Regex("\\b(fan|fridge|freezer|refrigerator|ac|air.?cond|pump|washing|washer|compressor|motor|blender|grinder|drill|saw|sewing|vacuum|dryer|dishwasher|extractor|generator|sewing machine)\\b", RegexOption.IGNORE_CASE) to 3,
    Regex("\\b(microwave|oven|heater|kettle|toaster|iron|press|geyser|boiler|induction|hot\\s?plate)\\b", RegexOption.IGNORE_CASE) to 2
  )
  fun detectSurge(name: String): Int = SURGE_MAP.firstOrNull { it.first.containsMatchIn(name) }?.second ?: 1

  data class Item(
    val id: String,
    val name: String,
    val watts: Int,
    val qty: Int,
    val surge: Int,
    val custom: Boolean = false,
    val isInverter: Boolean = false
  )

  data class Result(
    val runningW: Int,
    val peakW: Int,
    val dailyWh: Int,
    val exactKva: Double,
    val batteryAh: Int,
    val panels: Int,
    val itemCount: Int,
    val missing: List<String>,
    val surgeItems: List<String>,
    val rec: Package
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
          val mult = it.surge
          peak += unitW * mult
          if (mult > 1) surgeItems += "${it.qty}x ${it.name}"
        }
      }
    }
    val exactKva = if (peak > 0) roundKva((peak * 1.2) / 1000.0) else 0.5
    val rec = PACKAGES.firstOrNull { it.kva >= exactKva } ?: PACKAGES.last()
    val dailyWh = running * hours
    val ah = ((dailyWh * 1.3) / 12.0).roundToInt()
    val panels = maxOf(2, ceil((running * hours * 0.7) / 350.0).toInt())
    return Result(running, peak, dailyWh, exactKva, ah, panels, count, missing, surgeItems, rec)
  }

  fun formatNaira(v: Long): String = "₦" + "%,d".format(v)
}
