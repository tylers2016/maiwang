# 脈望 (Maiwang)

一個木刻古籍風格的第三方 Readwise Android 客戶端，基於 Jetpack Compose 構建。

## 核心功能

- **藝文（檢索與錄入）**
  - 支持 ISBN 條碼掃描（CameraX + Bundled ML Kit）與手動輸入檢索。
  - 支持中國 Z39.50、英國 Z39.50 與全球 SRU 多源文獻元數據查詢，以及異步封面獲取。
  - 支持拍照裁剪與百度高精度位置版 OCR 圖片點選識別，快速創建書摘與批註並同步至 Readwise。
- **校讎（瀏覽與修訂）**
  - 基於 Readwise Export API 與 REST API 的離線優先增量同步。
  - 提供書籍網格、類別篩選、模糊搜索、書摘詳情瀑布流，以及雲端雙向編輯與刪除。
- **每日回顧**
  - 生成古籍木刻風格的書摘回顧卡片，支持高清保存至相冊與系統分享。

## 從源碼構建

### 環境要求
- JDK 17
- Android SDK（`compileSdk = 36`，`minSdk = 24`，`targetSdk = 36`）

### 構建步驟

1. 克隆本倉庫：
   ```bash
   git clone https://github.com/tylers2016/maiwang.git
   cd maiwang
   ```

2. 在項目根目錄創建 `local.properties` 文件，填入本機 Android SDK 路徑：
   - **Windows**：
     ```properties
     sdk.dir=C\:\\Android\\Sdk
     ```
   - **macOS / Linux**：
     ```properties
     sdk.dir=/path/to/Android/sdk
     ```

3. 編譯調試版 APK：
   - **Windows**：
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   - **macOS / Linux**：
     ```bash
     ./gradlew assembleDebug
     ```

4. 編譯完成後，APK 位於：
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

### 應用內配置
- 打開應用後進入**設置**頁面，填寫個人的 **Readwise Access Token** 即可同步與上傳書摘。
- 如需使用拍照識別錄入功能，可在設置中配置百度智能雲高精度 OCR 的 API Key 與 Secret Key。

## 倉庫目錄說明

- `app/`、`gradle/`、`build.gradle.kts`、`settings.gradle.kts`、`gradle.properties`、`gradlew`、`gradlew.bat`：正式 Android 工程源碼與 Gradle 構建配置。
- `daily_review/`、`metadata_covers/`、`ui_reference/`、`Z39.EXE`、`Z39.ini`：開發初期的腳本原型、測試工具與設計參考資料，**不參與** Android APK 的編譯與打包。
