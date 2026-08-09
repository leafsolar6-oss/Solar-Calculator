# 🌿 Leaf Solar Calculator (Android)

A native Android app that is an exact replica of the Leaf Solar Load Calculator
(https://leafsolar.ng/solar-calculator/) — built with Jetpack Compose. **Fully
offline**, no website chrome, no WebView.

## Features (same rules as the website)
- 12 common appliances with +/− quantity and watts
- Inverter-model toggle for fridge/AC/fans (disables startup surge)
- Add custom appliances with automatic surge detection by name (motor/compressor/heating words)
- 1–18 hours/day slider
- Outputs: running load, startup/peak load, daily Wh, inverter KVA, battery Ah (12V), number of 350W panels
- Recommended Leaf Solar package with price + "View package" / "Get quote" (WhatsApp)

## Calculation engine
`CalcEngine.kt` ports the website JS verbatim — same appliance list, surge map,
KVA rounding (`ceil(peak*1.2/1000)` rounded up to nearest 0.5), battery Ah
`round((Wh*1.3)/12)`, panels `max(2, ceil(running*hours*0.7/350))`, and the same
package recommendation table.

## Build
- AGP 8.5.2, Kotlin 1.9.24, compileSdk 34, minSdk 24, Compose BOM 2024.06
- GitHub Actions builds on push: artifacts `SolarCalculator-debug` and `SolarCalculator-release`
- Local: `gradle assembleDebug` (JDK 17 + Android SDK 34). CI uses the runner's `gradle`.
- Package: `ng.leafsolar.calculator`
