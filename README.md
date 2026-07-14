# Temperature Demo (RGB SDK 溫度監控)

一個精簡的 Android 溫度資料展示應用程式，主要用於串接底層的硬體 RGB SDK (`com.rgbsdk.temperaturedemo`)，作為硬體溫度感測與燈效控制的測試與概念驗證 (PoC) 介面。

---

## 程式功能分析 (Program Feature Analysis)

1. **硬體狀態展示**
   * 透過 `MainActivity` 單一畫面即時展示從底層接收到的硬體溫度數據（例如 CPU/GPU 或周邊設備溫度）。
2. **SDK 介接與除錯**
   * 作為 RGB SDK 的宿主 (Host App)，提供基礎的 API 呼叫入口，用以驗證溫度數值是否正確回傳，並可能連帶觸發硬體的 RGB 燈效變更。

---

## 檔案與目錄結構 (Directory Structure)

專案刻意保持極度精簡，以利於硬體驅動層的除錯：

```text
TemperatureDemo/
├── app/src/main/
│   ├── AndroidManifest.xml          # 宣告 App 進入點
│   └── java/com/rgbsdk/temperaturedemo/
│       └── MainActivity.java        # 主程式與 SDK 介接邏輯
└── build.gradle                     # 基礎建置配置
```

---

## 架構決策與亮點 (Highlights)

* 🔬 **輕量化的測試沙盒 (Test Sandbox)**
  * **決策**：無複雜的背景服務 (Service) 或網路請求架構。
  * **優勢**：在開發底層硬體通訊 SDK 時，保持 App 端的絕對單純，能確保在問題排查時，快速釐清 Bug 是出在 App 層還是 SDK/底層硬體層，是標準的驅動程式測試架構。
