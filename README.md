# 🌿 Leaf Solar Calculator (Android app)

Standalone Android app that is an **exact replica** of the Solar Load Calculator
on the Leaf Solar website: https://leafsolar.ng/solar-calculator/

The calculator runs **fully offline** — the HTML/CSS/JS is bundled inside the APK
and loaded in a WebView, so it is a pixel-perfect copy with no logic drift and no
internet required.

## What it does (same as the website)
- Pick common appliances (lights, fans, TV, fridge, AC, pump, etc.) with +/− qty
- Inverter-model toggle for fridge/AC/fans (disables startup surge)
- Add custom appliances with automatic surge detection by name
- Hours-of-use slider (1–18h)
- Outputs: running load, startup/peak load, daily Wh, inverter KVA, battery Ah,
  number of 350W panels, and a recommended Leaf Solar package with price + links
- "Get a custom quote" opens WhatsApp

## Project layout
```
app/src/main/
  assets/www/index.html   ← the exact calculator (HTML + CSS + JS, self-contained)
  java/.../MainActivity.kt ← WebView host
  res/                    ← icon, colors, theme
reference/                ← source material extracted from the live site
  website-page.html       (full page as captured)
  calculator-logic.js     (decoded lcData script)
  calculator-styles.css   (the .lc styles)
.github/workflows/build-apk.yml  ← builds APK on every push
```

## Build
- Uses Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24, compileSdk 34, minSdk 24.
- **GitHub Actions:** push to `main` → artifacts `SolarCalculator-debug` and
  `SolarCalculator-release` (release is signed with the debug key for sideloading).
- **Local:** `./gradlew assembleDebug` (output in `app/build/outputs/apk/debug/`).

## Updating the calculator to match the website
If the website calculator changes, re-extract and rebuild `assets/www/index.html`:
1. Save the live page HTML.
2. The calculator markup is `<div class="lc" id="leafCalc">…</div>`.
3. Its logic is base64 in `<script type="text/plain" id="lcData">…</script>`
   (`base64 -d` to decode).
4. Its CSS is the `<style>` block immediately before that div (selectors start `.lc`).
5. Reassemble into `assets/www/index.html` (head + CSS + calculator div + script).

## Package / identity
- applicationId: `ng.leafsolar.calculator`
- app label: "Solar Calculator"
- For Play Store: switch `release` signing to an upload keystore and build an AAB
  (`./gradlew bundleRelease`).
