# Flux

Flux is a modern, high-performance, and visually polished native Android SSH client, SFTP file explorer, and Linux server performance monitor. Engineered with Jetpack Compose, Material Design 3, and Kotlin Coroutines, Flux delivers a seamless terminal experience, beautiful real-time performance dashboards, and robust server management.

---

## 🚀 Key Features

*   📊 **Real-time Performance Monitoring**: Tracks CPU utilization, RAM usage, storage systems, and network bandwidth in real-time. Allows you to monitor Linux processes and terminate them safely via SSH.
*   💻 **Interactive SSH Terminal**: Run standard commands with built-in ANSI stripping, and configure customizable command shortcuts for fast execution.
*   📁 **SFTP File Explorer**: Full-featured remote file system access to download, edit, rename, create, and delete remote files/directories securely.
*   💾 **Local Backup & Import**: Easily export server configurations and terminal shortcuts into a structured JSON backup file, and import them back on any device to restore your profile instantly.
*   🌐 **Bilingual Support**: Fully localized in English and 简体中文.
*   🎨 **Material 3 Design**: Supports Adaptive layouts, dynamic color palettes, and Light/Dark themes.

---

## 📂 Project Structure

```
├── app
│   ├── src
│   │   ├── main
│   │   │   ├── AndroidManifest.xml (App Manifest)
│   │   │   ├── java/com/example
│   │   │   │   ├── data (Database & Model persistence layer via Room)
│   │   │   │   │   ├── db (AppDatabase, ServerDao, ShortcutDao)
│   │   │   │   │   ├── model (Server and Shortcut Entities)
│   │   │   │   │   └── repository (ServerRepository)
│   │   │   │   ├── ssh (Core SSH engine, parser, and monitoring helper)
│   │   │   │   └── ui (Compose screen layers, theme engines, localization)
│   │   │   └── res (Adaptive icons, localized strings, layout values)
```

---

## 🛠️ Build & Development

### Requirements
*   **Android Studio Jellyfish** (or newer)
*   **Java Development Kit (JDK) 17+**
*   **Android SDK API 26** (Minimum SDK)

### Quick Commands
To compile the debug version of the application:
```bash
./gradlew assembleDebug
```

To build a release-ready APK:
```bash
./gradlew assembleRelease
```

---

## 💾 How to Use Backup & Restore

### Exporting configurations
1. Open the **Settings** panel (bottom-sheet sheet from the main screen).
2. Scroll to the **Backup & Restore** / **备份与恢复** section.
3. Tap **Export Backup** / **导出备份**.
4. Select a location in the Android SAF (Storage Access Framework) file picker and save your `.json` backup file.

### Importing configurations
1. Open the **Settings** panel.
2. Tap **Import Backup** / **导入恢复** under the Backup & Restore section.
3. Choose your exported `.json` file.
4. The application will instantly load and reconstruct your servers list and custom shortcuts safely.

---

## 🎨 Design Theme & Identity
Flux features a highly custom dark visual theme reminiscent of a sci-fi cyberpunk terminal, centered around spacious negative margins, sharp high-contrast typography, and beautiful cyan/purple neon accents.
