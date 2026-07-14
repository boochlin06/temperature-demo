# RGB SDK 溫度監控系統 (Temperature Demo) - 底層硬體通訊與 JNI 架構技術深度解析白皮書

## 第一章：專案總覽與工業級應用情境 (Executive Summary)

TemperatureDemo 看似為一個簡單的 Android 應用程式，實際上卻是一個深入 Android 系統底層 (Kernel/HAL)，並涉及硬體通訊的工業級展示框架 (PoC)。它作為 `com.rgbsdk.temperaturedemo` 的宿主 (Host)，其核心任務是透過底層的串列埠 (Serial Port) 或是 I2C/SPI 等硬體匯流排，即時抓取硬體感測器 (如 CPU 熱敏電阻、外部感測晶片) 的溫度數據，並據此驅動外部的 RGB 燈效控制器。

在工業控制、物聯網 (IoT)、電競設備 (如帶有 RGB 燈條與水冷監控的手機或筆電) 領域，此類底層通訊應用是極其關鍵的。此專案展示了如何突破 Android 虛擬機 (Dalvik/ART) 的限制，直接透過 C/C++ (JNI) 呼叫 Linux POSIX API 來操作硬體節點 (`/dev/ttyS*`)。本白皮書將為您深入解析這套系統的底層通訊架構與 JNI 實作細節。

## 第二章：系統架構與 JNI 通訊層 (System Architecture & JNI Layer)

### 2.1 突破 Java 邊界的 JNI (Java Native Interface) 架構
Android 預設的 Java API (`android.hardware.*`) 通常只能存取標準的感測器 (如陀螺儀、加速度計)。對於特規的溫度控制器或串列埠設備，Java 是無能為力的。
- **底層 C 語言實作 (`app/src/jni/SerialPort.c`)**: 這是整個專案的心臟。它透過標準的 Linux 系統呼叫 (System Calls) 如 `open()`, `read()`, `write()`, `ioctl()` 來開啟硬體節點。它需要精確地設定串列埠的波特率 (Baud Rate)、資料位元 (Data Bits)、停止位元 (Stop Bits) 與同位檢查 (Parity)。
- **Java Native 方法宣告 (`android_serialport_api/SerialPort.java`)**: 這是一個標準的 JNI 橋接類別。它宣告了 `native` 方法，如 `private native static FileDescriptor open(String path, int baudrate, int flags);`。當 Android App 呼叫這個方法時，Dalvik/ART 虛擬機會透過 JNI 映射表，將呼叫轉送至 `SerialPort.c` 中編譯好的 `.so` (Shared Object) 函式庫。

### 2.2 檔案描述符 (File Descriptor) 的生命週期管理
在 C 語言中成功開啟串列埠後，會獲得一個檔案描述符 (`fd`)。JNI 的厲害之處在於，它能透過反射機制 (Reflection) 實例化一個 Java 的 `FileDescriptor` 物件，並將這個整數 `fd` 注入其中。隨後，Java 端就能利用標準的 `FileInputStream` 與 `FileOutputStream`，像讀寫一般檔案一樣，對硬體進行非同步的資料流讀寫。

## 第三章：檔案與目錄結構解析 (Directory & Build Structure)

```text
TemperatureDemo/
├── app/
│   ├── build.gradle                 # 包含 NDK (Native Development Kit) 的編譯設定
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml  # 宣告必要的硬體存取權限
│       │   ├── java/com/rgbsdk/temperaturedemo/
│       │   │   ├── MainActivity.java    # UI 呈現與狀態更新
│       │   │   ├── BaseActivity.java    # 生命週期與錯誤處理基底類別
│       │   │   └── TemperatureInfo.java # 溫度資料封裝模型 (Data Class)
│       │   └── java/android_serialport_api/
│       │       └── SerialPort.java      # JNI 橋接類別
│       └── jni/
│           ├── Android.mk & Application.mk # ndk-build 專用的編譯腳本
│           ├── SerialPort.c         # 核心底層硬體通訊實作
│           └── SerialPort.h         # JNI 標頭檔
```

## 第四章：硬體通訊的痛點與解決方案 (Hardware Communication Challenges)

### 4.1 權限與 SELinux 阻擋
在現代 Android 系統中，即使你的 C 程式碼寫得再好，只要嘗試存取 `/dev/ttyS0`，通常都會遇到 `Permission Denied` (權限不足) 或 `SELinux AVC Denial`。
- **解決方案**: 這種專案通常需要裝置已經 Root，或者本身就是由系統廠開發並擁有系統簽名 (`System Signature`)，然後在 ROM 階段修改 `init.rc` 賦予 `/dev/ttyS0` `0666` (可讀寫) 的權限，並在 SELinux 策略 (`sepolicy`) 中開啟對應的存取規則。

### 4.2 執行緒阻塞 (Thread Blocking) 與 ANR 防護
硬體通訊是極度不可靠的。從串列埠讀取資料時 (`read`)，如果感測器沒有回傳資料，該操作預設是阻塞的 (Blocking)。
- **架構設計**: 在 `MainActivity.java` 中，必須開啟專屬的背景執行緒 (Reader Thread) 來監聽資料流。當接收到完整的溫度封包 (通常需要自定義協定，如起始位元組、資料長度、Checksum 校驗和) 後，再透過 `Handler` 或 `runOnUiThread()` 將數值拋回主執行緒更新 UI。

## 第五章：專案未來擴展與技術價值 (Future Extensibility)

1. **Protocol Buffer 或輕量級序列化整合**: 未來可將 `TemperatureInfo.java` 的資料傳輸改用更高效的序列化框架，便於在多個程序 (IPC) 間共享溫度資料。
2. **AIDL 背景服務化**: 目前的架構緊綁在 Activity 上，理想的工業級架構應將硬體通訊層下沉至背景的遠端服務 (Remote Service)，並透過 AIDL (Android Interface Definition Language) 提供介面供上層多個不同的 App 同時訂閱溫度變化。
3. **RGB 燈效聯動演算法**: 結合溫度曲線 (Fan Curve / RGB Curve)，撰寫平滑的 PID 控制演算法，當 CPU 溫度達到 80 度時，自動發送指令將 RGB 燈條漸變為紅色警告燈。

本專案完美展示了 Android 作為一個基於 Linux Kernel 的作業系統，其強大且靈活的軟硬體整合能力。從最上層的 XML UI 佈局，到中間層的 Java 業務邏輯，再穿透到 JNI 的 C 語言系統呼叫，是全端 (Full-Stack) 嵌入式系統開發工程師的絕佳參考範例。
