<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif">

# Markdown Editor

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Markdown Editor Icon" width="120" height="120"/>
</p>

<p align="center">
  <strong>A modern, offline, and feature-rich Markdown Editor for Android.</strong><br/>
  Write, preview, and organize your Markdown notes seamlessly.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-blue" />
  <img src="https://img.shields.io/badge/Language-Java-orange?logo=java" />
  <img src="https://img.shields.io/badge/License-MIT-lightgrey" />
</p>

![GitHub last commit](https://img.shields.io/github/last-commit/abdurrahman101bd/markdown-editor)
![GitHub issues](https://img.shields.io/github/issues-raw/abdurrahman101bd/markdown-editor)
![GitHub repo size](https://img.shields.io/github/repo-size/abdurrahman101bd/markdown-editor)
![GitHub stars](https://img.shields.io/github/stars/abdurrahman101bd/markdown-editor?style=social)
![GitHub forks](https://img.shields.io/github/forks/abdurrahman101bd/markdown-editor?style=social)

---

## ✨ Features

- **Real-time Markdown Preview:** Split-screen mode to write and see the rendered output simultaneously.
- **WebView Preview Mode:** Full-screen rendered preview using `marked.js` with custom CSS themes.
- **Auto Save & Undo/Redo:** Never lose your work with customizable auto-save intervals and a robust undo/redo manager.
- **Organize with Trash & Archive:** Move notes to trash (with auto-delete after a specified time) or archive them.
- **Local Backups:** Export and import your entire database (notes + settings) as a `.zip` file. Includes an auto-backup feature using `WorkManager`.
- **Biometric App Lock:** Secure your notes using fingerprint or device PIN/pattern.
- **Themes & UI:** Beautifully crafted Material 3 UI with System, Light, and Dark theme support. Switch between List and Grid views.
- **Quick Insert Toolbar:** Easily insert bold, italic, headings, code blocks, tables, and links.
- **Print & Share:** Export notes as PDF, Markdown files, or share as plain text.
- **Privacy First:** Completely offline. Disallow screenshots with a single toggle.

---

## 🚀 Getting Started

### Requirements
- **Android 8.0+ (API 26)**

---

## 🏗️ Tech Stack

| Component | Technology |
| --- | --- |
| Language | Java |
| IDE | AndroidIDE / Android Studio |
| Min SDK | API 26 (Android 8.0) |
| Database | Room |
| Markdown Renderer | Markwon (Preview) & Marked.js (WebView) |
| Background Tasks | WorkManager |
| Security | AndroidX Biometric |
| UI Components | Material 3 Components, RecyclerView |
| Image Loading | Glide |

---

## 🔐 Permissions

The app requires the following permissions to function fully:

- **INTERNET:** Required for loading remote images in Markdown.
- **POST_NOTIFICATIONS:** For auto-backup success/failure notifications (Android 13+).
-  **USE_BIOMETRIC:** For the App Lock feature.
- **WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE:** Required only for Android 8.0 - 9.0 (API 26-28) to write auto-backups to the storage.

---

## 🚀 Installation

1. Download the APK from [Releases](https://github.com/abdurrahman101bd/markdown-editor)
2. Enable **"Install from unknown sources"** in your device settings
3. Open the APK and install
4. Launch **Markdown Editor**

---

## 📝 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 📞 Contact

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/abdurrahman101bd)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/abdurrahman101bd)
[![Twitter](https://img.shields.io/badge/Twitter-1DA1F2?style=for-the-badge&logo=twitter&logoColor=white)](https://x.com/abdurrahman101b)
[![Facebook](https://img.shields.io/badge/Facebook-1877F2?style=for-the-badge&logo=facebook&logoColor=white)](https://www.facebook.com/abdurrahman101bd)

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 🐛 Bug Reports

Found a bug? Please [open an issue](https://github.com/abdurrahman101bd/markdown-editor/issues) with:
- Android version
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if applicable

<p align="center">
  Built with ❤️ on Android, for Android
</p>

---

<p align="center">
  <a href="#-markdown-editor">Back to Top ⬆️</a>
</p>

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif">