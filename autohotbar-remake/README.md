# AutoHotbar Remake (Fabric, Minecraft 26.1.2)

Highlights which item should go in which hotbar slot, using YOUR remapped key
as the label (falls back to the slot number). Never swaps for you (anticheat safe).

## Build on GitHub (no local setup)
Push this folder to a GitHub repo -> Actions tab -> "build" run -> download the artifact `autohotbar-remake-jar`. Red run = open it and copy the first error lines.

## Build (Windows PC)
Needs JDK 25 installed.

This zip has NO gradle wrapper (I had no internet to generate one). Easiest:
1. Go to https://fabricmc.net/develop/template and generate a 26.1.2 template
   (any mod id, e.g. "temp"). Unzip it.
2. Delete the template's `src` folder, `build.gradle`, `gradle.properties`, `settings.gradle`.
3. Copy this project's `src`, `build.gradle`, `gradle.properties`, `settings.gradle`, `LICENSE` into it.
4. Open a terminal there and run:  gradlew build
5. Jar = build/libs/autohotbar-remake-0.1.0.jar  (NOT the -sources jar). Put it in .minecraft/mods with Fabric API.

If gradle.properties versions (loom_version / loader_version / fabric_api_version)
are rejected, copy the ones from the template's gradle.properties instead.

## Keys
\  open config    ]  toggle debug overlay (both rebindable, category "AutoHotbar Remake")

## If the build fails
Paste me the first ~20 error lines. Likely spots (all flagged in comments):
- util/ItemInspector.java (enchantment / block hardness accessors)
- client/AutoHotbarRemakeClient.java (VanillaHudElements.HOTBAR import)
- client/highlight/InventorySlotHighlighter.java (ScreenEvents.afterExtract vs afterRender)
