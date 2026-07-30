# CodeFlow 🚀
> **Modern Mobile Text Editor & Incremental Delta Version Control System**

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-brightgreen.svg)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-orange.svg)
![Database](https://img.shields.io/badge/Database-Room%20SQLite-red.svg)
![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)

---

## 📌 Project Overview

**CodeFlow** is a lightweight, high-performance mobile text editor and version control application engineered for Android. Built using **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Room SQLite**, and **Clean Architecture**, CodeFlow provides developers and technical writers with a desktop-grade editing experience directly on Android smartphones and tablets.

Key architectural pillars include real-time syntax highlighting for **Kotlin** and **Markdown**, dual-tier local storage persistence, a **10-second background auto-save crash recovery engine**, and a **zero-duplication delta version control system** powered by `java-diff-utils`.

---

## ✨ Key Features

### 🛠️ 1. Editor Engine & User Interface
- **Complete Text Manipulation**: Full support for `Open`, `New`, `Recent Files`, `Save`, and `Save As` operations.
- **Character Encoding Selection**: Toggle between `UTF-8`, `ASCII`, and `UTF-16` encodings.
- **Word Wrapping & Line Numbers**: Persistent word wrap toggle and synchronized line-numbering gutter.
- **Font Scaling**: Dynamic font size scaling (12sp – 24sp) with persistent preferences.
- **Undo & Redo Manager**: Dedicated dual-stack memory engine (`UndoRedoManager.kt`) tracking up to 50 granular edit steps.
- **Text Search & Replace**: Real-time query search, match counter (`X matches`), live yellow text background highlighting (`Color(0xFFFBC02D)`), **Replace Next**, and **Replace All**.
- **Automated Kotlin Formatter**: Built-in code formatter (`formatCode()`) adjusting scope bracket indentation (`{}`), trimming trailing spaces, and normalizing line breaks.
- **Navigation Drawer**: Clean sidebar drawer with theme-consistent branding, recent files list, and quick actions.

### 🎨 2. Advanced Syntax Highlighting & Rendering
- **Kotlin Syntax Styling**: Real-time tokenization in `CodeSyntaxHighlighter.kt`:
  - **Keywords**: `fun`, `val`, `var`, `class`, `interface`, `override`, `suspend`, `import`, `package`, `return`, `if`, `else`, `for`, `while`, `data`, `object` (Bold Pink `#FF79C6`).
  - **Strings**: Single & multi-line string literals (Light Green `#50FA7B`).
  - **Comments**: `//` and `/* */` (Slate Blue `#6272A4`).
  - **Annotations**: `@Composable`, `@JvmStatic` (Orange `#FFB86C`).
  - **Numbers**: Hex, binary, integers, floats (Cyan `#8BE9FD`).
- **Markdown Editor Highlighting**: Live styling for headers (`#`), code blocks (``` & ``), bold (`**`), italic (`*`), links, and blockquotes (`>`).
- **Toggleable Markdown Preview Panel**: Built-in renderer (`MarkdownPreview.kt`) displaying formatted headers, dark code blocks (`#0D1117`), quotes, and styled text.

### 🛡️ 3. Fault Tolerance & Crash Recovery
- **10-Second Background Auto-Save**: Asynchronous coroutine loop running every 10 seconds (`delay(10_000)`). Automatically caches active modified buffers to `.crash_recovery_backup.tmp`.
- **Crash Recovery Handshake**: On app launch, detects unsaved crash backups and prompts the user with a 1-tap restoration handshake dialog.

### 📜 4. Incremental Delta-Based Version Control System
- **Zero-Duplication Storage**: Base files are **never duplicated** across version records.
- **Diff Patch Storage**: Calculates unified text patch strings (`deltaPatch`) using `java-diff-utils` (`DiffUtils.diff` & `UnifiedDiffUtils.generateUnifiedDiff`) and stores them in Room DB (`FileSnapshot` table).
- **Line-by-Line Visual Diff Viewer**: `DiffViewerScreen.kt` displays a structural comparison highlighting additions in soft green (`+`) and deletions in soft red (`-`).
- **Version Rollback Engine**: Reconstructs any historical version $N$ by starting at an empty baseline text state ($\text{Text}_0 = \emptyset$) and sequentially applying delta patches:
  $$\text{Text}_N = \text{Patch}_N \Big( \dots \text{Patch}_2 \big( \text{Patch}_1(\emptyset) \big) \Big)$$
- **Read-Only File Locking**: DB-backed file lock flag (`isReadOnly`) preventing accidental edits in the editor and file repository.

---

## 🏛️ System Architecture

```
                                  +---------------------------------------+
                                  |         Jetpack Compose UI            |
                                  | (EditorScreen, TopBar, SidebarDrawer) |
                                  +---------------------------------------+
                                                      |
                                                      v
                                  +---------------------------------------+
                                  |            EditorViewModel            |
                                  |   (StateFlow, UndoRedo, Auto-Save)    |
                                  +---------------------------------------+
                                                      |
                                                      v
                                  +---------------------------------------+
                                  |            FileRepository             |
                                  | (SAF Storage, Mirroring, Settings)    |
                                  +---------------------------------------+
                                          /                       \
                                         v                         v
                       +---------------------------+     +-------------------+
                       |    DeltaSnapshotEngine    |     | CodeFlowDatabase  |
                       |    (java-diff-utils)      |     |   (Room SQLite)   |
                       +---------------------------+     +-------------------+
```

---

## 📁 Directory Structure

```text
CodeFlow/
├── app/
│   ├── src/main/java/com/example/codeflow/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── dao/             # Room DAOs (FileDao.kt, SnapshotDao.kt)
│   │   │   │   ├── database/        # CodeFlowDatabase.kt
│   │   │   │   └── entity/          # Room Entities (ProjectFile.kt, FileSnapshot.kt)
│   │   │   └── repository/          # FileRepository.kt & DeltaSnapshotEngine.kt
│   │   ├── ui/
│   │   │   ├── components/          # UI Components (CodeEditorEngine.kt, TopBar.kt, SidebarDrawer.kt, SearchBar.kt, MarkdownPreview.kt)
│   │   │   ├── dialogs/             # SettingsDialog.kt, SaveAsDialog.kt, OpenFileDialog.kt
│   │   │   ├── editor/              # EditorScreen.kt, EditorViewModel.kt, CodeSyntaxHighlighter.kt, UndoRedoManager.kt
│   │   │   ├── theme/               # Color.kt, Theme.kt, Type.kt
│   │   │   └── versioning/          # VersionHistoryDialog.kt, DiffViewerScreen.kt, DiffLineItem.kt
│   │   └── MainActivity.kt          # Main Application Entry Point
├── gradle/
│   └── libs.versions.toml           # Version Catalog Dependencies
└── build.gradle.kts
```

---

## 🔧 Prerequisites & Tech Stack

- **Min SDK**: `26` (Android 8.0 Oreo)
- **Target SDK**: `34` (Android 14)
- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material3)
- **Database**: SQLite via Room 2.6+
- **Diff Library**: `com.github.difflib:java-diff-utils:4.12`
- **Build Tool**: Gradle 8.x (Kotlin DSL)

---

## 🚀 Building & Running

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/CodeFlow.git
cd CodeFlow
```

### 2. Build Debug APK
```bash
# On Windows
.\gradlew.bat assembleDebug

# On Linux/macOS
./gradlew assembleDebug
```

### 3. Install on Connected Device or Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Launch Application
```bash
adb shell am start -n com.example.codeflow/.MainActivity
```

---

## 📄 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
