package ng.leafsolar.calculator

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CloudSync {
  private const val BASE = "https://leafsolar.ng/wp-json/lfx/v1"

  fun save(code: String, rows: List<RowState>, custom: List<CustomRow>, dailyWh: Int): Boolean {
    return try {
      val arr = JSONArray()
      rows.forEach { r -> arr.put(JSONObject().put("qty", r.qty).put("watts", r.watts).put("inverter", r.inverter).put("hours", r.hours)) }
      val carr = JSONArray()
      custom.forEach { c -> carr.put(JSONObject().put("name", c.name).put("watts", c.watts).put("surge", c.surge).put("inverter", c.inverter).put("hours", c.hours)) }
      val payload = JSONObject().put("rows", arr).put("custom", carr).put("dailyWh", dailyWh)
      val body = JSONObject().put("code", code.uppercase()).put("data", payload).toString()
      post("$BASE/calc-save", body)
      true
    } catch (e: Exception) { false }
  }

  fun load(code: String): Triple<List<Map<String,String>>, List<Map<String,String>>, Int>? {
    return try {
      val raw = get("$BASE/calc-load/${code.uppercase()}") ?: return null
      val o = JSONObject(raw).optJSONObject("data") ?: return null
      val rs = mutableListOf<Map<String,String>>()
      val ra = o.optJSONArray("rows")
      if (ra != null) for (i in 0 until ra.length()) {
        val x=ra.getJSONObject(i); rs.add(mapOf("qty" to x.optString("qty"),"watts" to x.optString("watts"),"inverter" to x.optString("inverter"),"hours" to x.optString("hours")))
      }
      val cs = mutableListOf<Map<String,String>>()
      val ca = o.optJSONArray("custom")
      if (ca != null) for (i in 0 until ca.length()) {
        val x=ca.getJSONObject(i); cs.add(mapOf("name" to x.optString("name"),"watts" to x.optString("watts"),"surge" to x.optString("surge"),"inverter" to x.optString("inverter"),"hours" to x.optString("hours")))
      }
      Triple(rs, cs, o.optInt("dailyWh",0))
    } catch (e: Exception) { null }
  }

  private fun post(url: String, body: String): String? {
    val c = URL(url).openConnection() as HttpURLConnection
    c.requestMethod = "POST"; c.doOutput = true; c.setRequestProperty("Content-Type","application/json"); c.connectTimeout=15000; c.readTimeout=15000
    c.outputStream.use { it.write(body.toByteArray()) }
    return if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null
  }
  private fun get(url: String): String? {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout=15000; c.readTimeout=15000
    return if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null
  }
}
