# NetScope

<p align="center">
  <img src="docs/assets/banner.png" alt="NetScope Banner" width="100%"/>
</p>

<p align="center">
  <strong>A professional Android network inspector — see exactly what your device is doing on the internet.</strong>
</p>

<p align="center">
  <a href="https://android-arsenal.com/api?level=26">
    <img src="https://img.shields.io/badge/API-26%2B-brightgreen.svg" alt="API Level"/>
  </a>
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin" alt="Kotlin"/>
  </a>
  <a href="https://developer.android.com/jetpack/compose">
    <img src="https://img.shields.io/badge/Jetpack%20Compose-2024.02-4285F4?logo=jetpackcompose" alt="Compose"/>
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License"/>
  </a>
  <a href="https://github.com/netscope/netscope/releases">
    <img src="https://img.shields.io/github/v/release/netscope/netscope?color=00BFA5" alt="Release"/>
  </a>
</p>

---

## What is NetScope?

NetScope is a standalone Android network inspector that gives you real-time visibility into every network connection your device makes — across every installed app, without root access.

Think **Little Snitch** or **GlassWire** — but for Android.

It operates in two complementary modes:

| Mode | How it works | What you see |
|---|---|---|
| **VPN Tunnel** | Creates a local VPN that routes all device traffic through NetScope | Every app's connections, DNS queries, bandwidth, raw packet metadata |
| **OkHttp Interceptor** | Embedded inside your own app's OkHttp stack | Full HTTP request/response bodies, headers, timing, status codes |

No external server. No data leaves your device. Everything stays local.

---

## Screenshots

<p align="center">
  <img src="docs/assets/dashboard.png" width="24%" alt="Dashboard"/>
  <img src="docs/assets/traffic_list.png" width="24%" alt="Traffic List"/>
  <img src="docs/assets/detail.png" width="24%" alt="Request Detail"/>
  <img src="docs/assets/dns.png" width="24%" alt="DNS Log"/>
</p>

---

## Features

### 🛡️ System-wide VPN Capture
- Routes all device traffic through a local TUN interface using `VpnService`
- Works across **every installed app** — no code changes needed in target apps
- No external VPN server — everything stays on-device
- Foreground service with persistent notification and one-tap stop

### 📡 Live Dashboard
- Real-time upload/download bandwidth per app in bytes/second
- Active connection count with live updates
- Top bandwidth consumer highlighted
- Anomaly detection with severity levels (LOW / MEDIUM / HIGH)

### 🌐 HTTP Traffic Inspector
- Full request and response capture (URL, method, headers, body)
- Status code with colour-coded category (2xx / 3xx / 4xx / 5xx)
- Request and response body up to 250KB with gzip decompression
- Timing — duration in ms, flagged if > 2 seconds
- Filter by method, status category, slow requests, errors
- Full-text search across URL, host, and response body

### 🔁 Request Replay
- Resend any captured HTTP request directly from the app
- Edit headers, body, and URL before sending
- View the replay result inline with full response detail
- All replays tagged separately so they don't pollute original traffic

### 🔍 DNS Log
- Parses raw UDP port-53 packets off the TUN interface
- Shows every domain lookup made by any app
- Resolves DNS responses to IP addresses (A, AAAA, CNAME, MX, TXT)
- Search by domain name
- App attribution — see which app triggered each lookup

### 🔗 Connection Tracker
- Tracks active, historical, and flagged connections
- Per-connection byte counts (sent and received)
- Protocol and port detection with human-readable labels (HTTPS, DNS, SSH)
- Hostname resolution for known IPs
- Suspicious connections flagged automatically

### ⏱️ Timeline Waterfall
- Gantt-style chart of all HTTP requests over time
- Greedy lane-packing algorithm shows concurrent requests side by side
- Colour-coded by status category
- Tap any bar to jump to the full request detail
- Horizontal and vertical scroll for dense sessions

### 🚨 Anomaly Detection
- Bandwidth spikes above 5 MB/s flagged automatically
- Connections to suspicious ports (4444, 6666, 31337, etc.)
- Connections to raw IP addresses with no hostname resolution
- Flagged connection log with custom reasons

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture (Data / Domain / Presentation) |
| DI | Hilt |
| Database | Room with KSP |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| Networking | OkHttp 4 + Retrofit 2 |
| Preferences | DataStore |
| Logging | Timber |
| Build | Gradle Kotlin DSL + Version Catalogs |
| Symbol Processing | KSP (replaces KAPT) |

---

## Architecture

NetScope follows **Clean Architecture** with strict layer separation. No Android framework classes leak into the domain layer.

```
com.netscope.app/
├── data/
│   ├── interceptor/          # OkHttp interceptor (HTTP capture)
│   ├── local/
│   │   ├── converter/        # Room TypeConverters (enums, maps, lists)
│   │   ├── dao/              # Room DAOs
│   │   ├── database/         # NetScopeDatabase
│   │   ├── entity/           # Room entities
│   │   └── mapper/           # Entity ↔ domain model mappers
│   ├── repository/           # Repository implementations
│   └── vpn/
│       ├── DnsPacketParser   # Parses raw UDP/53 payloads
│       ├── NetScopeVpnService# TUN interface, capture loop
│       ├── PacketEventBus    # SharedFlow broadcast hub
│       ├── PacketParser      # IPv4 header parser
│       ├── UidResolver       # /proc/net/tcp → UID → AppInfo
│       └── VpnController     # Clean start/stop API for UI
│
├── di/
│   ├── DatabaseModule        # Room + DAO providers
│   ├── NetworkModule         # OkHttpClient provider
│   ├── RepositoryModule      # Interface → implementation bindings
│   └── VpnModule             # VPN singleton marker
│
├── domain/
│   ├── model/                # Pure Kotlin data models
│   │   ├── AppInfo
│   │   ├── BandwidthSnapshot
│   │   ├── ConnectionEntry
│   │   ├── DnsEntry
│   │   ├── HttpTransaction
│   │   ├── PacketInfo
│   │   └── TrafficFilter
│   ├── repository/           # Repository interfaces (contracts)
│   └── usecase/              # One class, one responsibility
│       ├── ClearAllTrafficUseCase
│       ├── DetectAnomaliesUseCase
│       ├── ExportTrafficUseCase
│       ├── GetAppListUseCase
│       ├── GetHttpTransactionDetailUseCase
│       ├── ObserveBandwidthUseCase
│       ├── ObserveConnectionsUseCase
│       ├── ObserveDnsEntriesUseCase
│       ├── ObserveHttpTransactionsUseCase
│       └── ReplayRequestUseCase
│
└── presentation/
    ├── base/
    │   ├── BaseViewModel     # Generic StateFlow ViewModel base
    │   └── UiEvent           # One-shot navigation/snackbar events
    ├── components/           # Shared Composables
    │   └── Components        # TopBar, StatusChip, MethodChip, StatCard, etc.
    ├── navigation/
    │   ├── NavArgs
    │   ├── NavRoutes
    │   └── NetScopeNavGraph
    ├── screens/
    │   ├── connections/      # ConnectionsScreen + ViewModel
    │   ├── dashboard/        # DashboardScreen + ViewModel
    │   ├── detail/           # TrafficDetailScreen + ViewModel
    │   ├── dns/              # DnsScreen + ViewModel
    │   ├── replay/           # ReplayScreen + ViewModel
    │   ├── timeline/         # TimelineScreen + ViewModel
    │   └── traffic/          # TrafficListScreen + ViewModel
    ├── theme/
    │   ├── Color             # Brand and semantic color tokens
    │   ├── Theme             # MaterialTheme dark scheme
    │   └── Type              # Typography scale
    └── MainActivity
```

### Data flow

```
VpnService (TUN interface)
    │
    ├── PacketParser ──────────────────────────────────────┐
    │   (IPv4 headers, TCP/UDP ports, direction, size)     │
    │                                                       ▼
    ├── DnsPacketParser                             PacketEventBus
    │   (UDP/53 → domain, IPs, query type)          SharedFlow hub
    │                                                       │
    └── UidResolver                        ┌───────────────┼────────────────┐
        (/proc/net/tcp → UID → AppInfo)    │               │                │
                                           ▼               ▼                ▼
OkHttp Interceptor ──────────► TrafficRepository  DnsRepository  ConnectionRepository
(full HTTP bodies)                        │               │                │
                                          └───────────────┼────────────────┘
                                                          │
                                                    Use Cases
                                                          │
                                                    ViewModels
                                                    (StateFlow)
                                                          │
                                                  Compose UI
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android device or emulator running API 26+

### Clone and build

```bash
git clone https://github.com/netscope/netscope.git
cd netscope
```

Open in Android Studio, let Gradle sync, then run on a physical device or emulator.

> **Note:** VPN capture requires a real Android device for full functionality. The emulator supports the app but `VpnService` behaviour may differ.

### First launch

1. Tap the **WiFi icon** in the top-right corner of the Dashboard
2. Android will show a system VPN permission dialog — tap **OK**
3. NetScope starts a foreground service and begins capturing all device traffic
4. Navigate between tabs to explore HTTP traffic, DNS queries, connections, and the timeline
5. Tap **Stop** in the persistent notification or dashboard banner to end capture

---

## Embedding the interceptor in your own app

If you want full HTTP body capture (not just packet metadata) for your own app, add the `NetScopeInterceptor` to your `OkHttpClient`. This is optional — the VPN tunnel works without any changes to target apps.

### Step 1 — Copy the interceptor

Copy `NetScopeInterceptor.kt` from `data/interceptor/` into your project.

### Step 2 — Add to your OkHttpClient

```kotlin
// In your app's NetworkModule or wherever you build OkHttpClient
val client = OkHttpClient.Builder()
    .apply {
        // Only attach in debug builds — never ship to production
        if (BuildConfig.DEBUG) {
            addInterceptor(NetScopeInterceptor())
        }
    }
    .build()
```

### Step 3 — Install NetScope alongside your app

Install NetScope on the same device. Captured transactions from your app appear live in the HTTP Traffic screen.

> The interceptor captures request and response bodies **before** TLS encryption, so you get full plaintext content even over HTTPS.

---

## Permissions

| Permission | Why it is needed |
|---|---|
| `INTERNET` | Forwarding captured packets to the real internet |
| `ACCESS_NETWORK_STATE` | Checking network availability |
| `BIND_VPN_SERVICE` | Declaring the VpnService component |
| `FOREGROUND_SERVICE` | Keeping capture alive while the app is in the background |
| `POST_NOTIFICATIONS` | Showing the persistent capture notification (API 33+) |

NetScope does **not** request `READ_PHONE_STATE`, `CONTACTS`, `LOCATION`, or any other sensitive permission unrelated to network inspection.

---

## Privacy

- **No data leaves your device.** NetScope never connects to any external server.
- All captured traffic is stored in a local Room database at `/data/data/com.netscope.app/databases/netscope.db`
- The database is private to the app and not accessible to other apps
- You can wipe all captured data at any time using the **Delete** button in the HTTP Traffic screen
- NetScope does not decrypt HTTPS traffic in VPN mode — it only sees metadata (IP, port, size, timing). Full body capture requires the OkHttp interceptor embedded in the target app

---

## Roadmap

- [ ] IPv6 packet parsing
- [ ] HTTPS interception via local proxy + custom CA certificate
- [ ] Cross-app interceptor via `ContentProvider` (no copy-paste required)
- [ ] Export to HAR — open in Chrome DevTools or Charles Proxy
- [ ] Per-app traffic history charts
- [ ] Notification alerts for anomalies detected in background
- [ ] Publish interceptor as a Maven/JitPack library
- [ ] Tablet and foldable layout support
- [ ] Dark/light theme toggle

---

## Contributing

Contributions are welcome. Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature-name`
3. Commit using conventional commits: `feat:`, `fix:`, `chore:`, `refactor:`
4. Open a pull request with a clear description of what changed and why

### Code style

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- All new features must go through the domain layer — no repository calls directly from ViewModels
- Use cases must be single-responsibility — one public `operator fun invoke()`
- All Room queries must use `Flow` — no blocking calls on the main thread
- New screens must have a corresponding `UiState` data class and `BaseViewModel` subclass

---

## Building a release APK

```bash
./gradlew assembleRelease
```

The signed APK will be at `app/build/outputs/apk/release/app-release.apk`.

Make sure you have a `keystore.properties` file at the project root:

```properties
storeFile=../keystore/netscope.keystore
storePassword=your_store_password
keyAlias=netscope
keyPassword=your_key_password
```

And reference it in your `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            val props = Properties().apply {
                load(rootProject.file("keystore.properties").inputStream())
            }
            storeFile     = file(props["storeFile"] as String)
            storePassword = props["storePassword"] as String
            keyAlias      = props["keyAlias"] as String
            keyPassword   = props["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## License

```
MIT License

Copyright (c) 2024 NetScope

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Acknowledgements

- [OkHttp](https://square.github.io/okhttp/) — HTTP client and interceptor API
- [Retrofit](https://square.github.io/retrofit/) — Type-safe HTTP client
- [Hilt](https://dagger.dev/hilt/) — Dependency injection for Android
- [Room](https://developer.android.com/jetpack/androidx/releases/room) — SQLite abstraction layer
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Modern Android UI toolkit
- [Timber](https://github.com/JakeWharton/timber) — Logging utility
- [Chucker](https://github.com/ChuckerTeam/chucker) — Inspiration for the OkHttp interceptor approach
- [NetBare](https://github.com/MegatronKing/NetBare) — Reference for VpnService packet interception

---

<p align="center">
  Built with ❤️ for Android developers who want to know exactly what their apps are doing.
</p>
