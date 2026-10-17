# Roboto RS Clock – OnePlus 15 OxygenOS 16 (Quellcode)

**Status:** Auf die tatsächlichen Klassen aus der hochgeladenen `SystemUI.apk` angepasst. **Noch keine auf dem OnePlus getestete APK.** Der Download enthält den vollständig konfigurierten Gradle-Quellcode samt deiner eigenen `Roboto-RSf.ttf` und GitHub Actions zur APK-Erstellung.

## Konkrete Firmware

- OnePlus 15 (CPH2747), OxygenOS 16, `CPH2747_11.A.48_0480_202609111034`
- SystemUI: `com.android.systemui`, Version `16.99.12`
- Modern LSPosed `2.2.0 (7854)`, API 102
- Ziel: `com.oplus.systemui.statusbar.widget.StatClock` (geprüft in `classes4.dex`)
- `StatClock` erweitert `com.android.systemui.statusbar.policy.Clock` (geprüft in `classes2.dex`), diese erweitert `TextView`.
- Die Uhr im Sperrbildschirm ist **eine andere Klasse**, `com.oplus.systemui.statusbar.widget.OplusKeyguardStatusBarClock` – diese wird **nicht** gehookt.

## Was der Hook tut

- Er registriert sich **ausschließlich für** `com.android.systemui`.
- Er hookt `StatClock.onDraw(Canvas)` – nicht `TextView.onDraw()` systemweit.
- Er setzt einmalig bzw. nach einem System-Reset `setTypeface()` auf die geladene TTF.
- Die Schrift wird über Java-Resources aus der Modul-APK gelesen (`fonts/Roboto-RSf.ttf`, ca. 2,3 MB). Kein Systempartition-Mount, keine Speicherberechtigungen und keine externen Datenzugriffe erforderlich.
- Bei einem Fehler wird **nichts** überschrieben. Fehler stehen unter dem Log-Tag `RSClock`.

## Android APK erzeugen

**Option A – GitHub Actions (ohne lokale Android-SDK-Installation):**

1. Inhalt dieses ZIP in ein neues privates GitHub-Repository hochladen (Gradle-Dateien müssen im Repo-Stamm liegen).
2. Auf GitHub **Actions** öffnen, **Build signed debug APK** auswählen und **Run workflow** ausführen.
3. Bei erfolgreicher Action unter **Artifacts** `Roboto-RSf-Clock-LSPosed-APK` herunterladen und entpacken.
4. Die `app-debug.apk` aufs Handy übertragen und installieren. Der Workflow erstellt einen eigenen Debugsignaturschlüssel.

**Option B – Android Studio:** Quellordner öffnen, Gradle Sync und `:app:assembleDebug` ausführen.

## Installation

1. Vorherige Testversion dieses Moduls deaktivieren oder deinstallieren. Lost Xposed kann installiert bleiben; zum Testen den konkurrierenden Clock-Studio-Hook möglichst deaktivieren.
2. APK installieren. Das Modul erscheint in der LSPosed-Modulliste (keine eigenständige Einstellungs-App).
3. Modul einschalten, im Geltungsbereich **nur System-UI (`com.android.systemui`)** wählen.
4. SystemUI bei Bedarf neu starten: `su -c 'killall com.android.systemui'`. Dabei verschwindet die Leiste kurz und lädt neu.
5. Die kleine Statusleisten-Uhr überprüfen. Bei Problemen Modul deaktivieren und das Handy regulär neu starten.

## Logs

```sh
su -c 'logcat -d -s RSClock:I RSClock:W RSClock:E | tail -n 80'
```

Erwartet: `StatClock.onDraw hook installed on OxygenOS 16 (Modern API 102)` und später `Applied custom Roboto-RSf font...`.

Falls nur der erste Eintrag erscheint, aber keiner mit `Applied`, wird eventuell ein anderer Statusleisten-Clock-View genutzt. Dafür die Logs zurücksenden. Wenn gar kein Eintrag kommt, LSPosed-Modulaktivierung/Scope prüfen.

## Einschränkungen

Diese Arbeitsumgebung enthält kein Android-SDK/aapt2/d8/Gradle und kann daher **keine überprüft installierbare APK** bauen. Auch ist nicht bewiesen, dass `StatClock` in jeder OnePlus-15-Konfiguration die sichtbare Uhr rendert; der DEX-Nachweis zeigt Existenz, Superklasse und die `onDraw`-Methode, ersetzt aber keinen Gerätetest.

Die originale SystemUI-APK bleibt unangetastet. Keine Garantie für SystemUI-Stabilität beim LSPosed-Hooking.
