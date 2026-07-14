<h1 align="center">Temperature Demo (JNI & RGB SDK)</h1>

<p align="center">
  <a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://img.shields.io/badge/NDK-C%2FC%2B%2B-orange.svg"><img alt="NDK" src="https://img.shields.io/badge/NDK-C%2FC%2B%2B-orange.svg"/></a>
</p>

An industrial-grade Android application demonstrating direct hardware communication. It bypasses the standard Android Framework SDK by utilizing the Java Native Interface (JNI) and NDK to communicate directly with hardware sensors via Linux Serial Ports (`/dev/ttyS*`).

## 🚀 Features

- **Low-Level Hardware I/O**: Direct read/write access to serial ports for fetching real-time CPU/GPU thermal sensor data.
- **JNI Bridge**: Seamless communication between the Android Dalvik/ART virtual machine and native C/C++ POSIX API calls.
- **Asynchronous Data Polling**: Dedicated background threads for continuous hardware polling without blocking the Main UI Thread.
- **RGB SDK Integration**: Acts as the host PoC for testing hardware-driven RGB lighting effects based on temperature curves.

## 🛠 Tech Stack & Architecture

- **Languages**: Java (UI & Logic) + C (Native Serial Port Driver).
- **JNI Architecture**: 
  - `SerialPort.c`: Implements POSIX `open()`, `read()`, `write()`, and `ioctl()` to configure baud rates and parity bits.
  - `SerialPort.java`: Uses reflection to map the returned native File Descriptor (`fd`) into a Java `FileDescriptor` for `FileInputStream` access.
- **Concurrency**: Background Reader Threads with synchronized Handlers for thread-safe UI updates.

## 📦 Getting Started

### Prerequisites
- Android Studio with **NDK (Native Development Kit)** installed.
- A rooted Android device or an engineering build with `0666` permissions and SELinux policies granted for `/dev/ttyS*`.

### Build & Run
```bash
git clone https://github.com/boochlin06/temperature-demo.git
```
Sync project with Gradle and ensure CMake/NDK paths are configured correctly in `local.properties`.

## 📄 License
This project is licensed under the MIT License.
