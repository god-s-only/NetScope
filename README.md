# NetScope

<p align="center">
  <img src="docs/assets/banner.png" alt="NetScope Banner" width="100%"/>
</p>

<p align="center">
  <strong>A professional Android network inspector — capture, inspect and replay HTTP/HTTPS traffic from any app on your device.</strong>
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
    <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"/>
  </a>
  <a href="https://github.com/netscope/netscope/releases">
    <img src="https://img.shields.io/github/v/release/netscope/netscope?color=00BFA5" alt="Release"/>
  </a>
</p>

---

## What is NetScope?

NetScope is a developer tool for Android that lets you inspect, capture, and analyze HTTP/HTTPS network traffic from any app on your device — without root access.

Think **Charles Proxy** or **Proxyman** — but running directly on your Android device. No laptop required.

It works as a local MITM proxy. Set it up once, install the CA certificate, point your WiFi proxy to `127.0.0.1:8888`, and every API call made by any app on your device flows through NetScope. You see the full request and response — URL, method, headers, body, status code, timing — in real time.

---

## Screenshots

<p align="center">
  <img src="docs/assets/dashboard.png" width="19%" alt="Dashboard"/>
  <img src="docs/assets/traffic.png" width="19%" alt="HTTP Traffic"/>
  <img src="docs/assets/detail.png" width="19%" alt="Request Detail"/>
  <img src="docs/assets/dns.png" width="19%" alt="DNS Log"/>
  <img src="docs/assets/timeline.png" width="19%" alt="Timeline"/>
</p>

---

## How it works

```
Any app makes a network request
        ↓
Android routes it through the system WiFi proxy (127.0.0.1:8888)
        ↓
NetScope's LocalProxyServer receives the connection
        ↓
For HTTPS: TLS handshake using a per-host certificate signed by our CA
        ↓
Full request and response read in plaintext
        ↓
Request forwarded to the real server — app gets its response normally
        ↓
HttpTransaction saved to Room database
        ↓
All screens update live via Kotlin Flow
```

NetScope is completely transparent to the target app. The app receives exactly the same response it would get without a proxy. The server sees a normal request. NetScope reads everything as it passes through.

---

## Features

### HTTP/HTTPS Traffic Capture
- Full request and response — URL, method, headers, body, status code, timing
- Works on any app that respects the Android system proxy — no code changes needed in the target app
- HTTPS decryption via local MITM proxy with a custom CA certificate
- Gzip response body decompression
- Body capture up to 500KB per request

### HTTP Traffic List
- Live list of all captured requests updating in real time
- Full-text search by URL, host, path, or response body
- Filter by HTTP method — GET, POST, PUT, DELETE, PATCH
- Filter by status category — 2xx, 3xx, 4xx, 5xx
- Filter by response time — greater than 500ms, 1s, or 2s
- Errors only and slow requests only quick presets
- Active filter chips with individual remove buttons
- Filter count badge on the filter icon

### Request Detail
- Four-tab layout — Overview, Request, Response, Headers
- Full URL, method, request headers and body
- Full status code, response headers and body
- Monospace body rendering for JSON and XML
- One-tap replay shortcut

### Request Replay
- Resend any captured request directly from the app
- Edit URL, headers, and request body before sending
- Response displayed inline with status code and full body
- Replayed requests tagged separately in the traffic list
- Reset to original with one tap

### DNS Log
- Every domain lookup made by any proxied app extracted from the Host header
- Deduplicated — same domain logged once per 60 seconds
- Search by domain name
- Response time per entry
- Clear DNS log with confirmation dialog

### Connections Tracker
- Every unique host and port that received proxied traffic
- Bytes sent and received per connection
- All connections and flagged connections tabs
- Protocol and port with human-readable labels (HTTPS, HTTP)
- Clear connections with confirmation dialog

### Timeline Waterfall
- Gantt-style chart of all captured requests plotted over time
- Greedy lane-packing algorithm places concurrent requests in parallel swim lanes
- Color coded by status category — green 2xx, yellow 3xx, red 4xx/5xx
- Tap any bar to open the full request detail screen
- Auto-scaling so the chart always fits within screen bounds
- Horizontal and vertical scroll

### Statistics
- Total requests, unique hosts, DNS domains, and connections
- Total bytes sent and received
- Success, redirect, and error counts with error rate percentage
- Average, fastest, and slowest response times
- Most active host and most used HTTP method

### Settings
- Max stored requests — 50, 100, 500, 1000, or unlimited
- Auto-clear oldest requests automatically when limit is reached
- Show replayed requests toggle
- Clear all data with confirmation dialog
- CA certificate install status and reinstall shortcut
- App version and package name

### Persistent Notification
- Shows while the proxy is running
- Live request count updates as traffic is captured
- Tap notification to open the app
- Stop button directly in the notification

### Export to HAR
- Export all captured traffic as HAR 1.2 format
- Opens in Chrome DevTools, Postman, Charles Proxy, or any HAR viewer
- Share via any installed app — email, Google Drive, Slack
- Full headers, bodies, and timing included

### Proxy Setup Guidance
- Step-by-step setup screen with clear instructions
- Copyable proxy hostname and port values
- Live proxy detection — green checkmark appears automatically when the proxy is correctly configured
- Direct shortcut to Android WiFi settings
- CA certificate install status indicator

---

## Tech Stack

### Language and Core
| Technology | Purpose |
|---|---|
| **Kotlin 1.9.22** | 100% Kotlin — no Java anywhere |
| **Kotlin Coroutines** | All async operations throughout the app |
| **Kotlin Flow** | Live data streams from database to UI |
| **KSP 1.9.22** | Symbol processing — replaces KAPT, 2x faster incremental builds |

### Architecture
| Pattern | Implementation |
|---|---|
| **Clean Architecture** | Strict Data / Domain / Presentation layer separation |
| **MVVM** | ViewModels expose `StateFlow<UiState>` consumed by Compose screens |
| **Repository pattern** | Domain layer depends only on interfaces, never implementations |
| **Use cases** | One class, one responsibility, single `operator fun invoke()` |
| **Single Activity** | One `MainActivity`, full Compose Navigation |

### UI
| Technology | Purpose |
|---|---|
| **Jetpack Compose** | 100% declarative UI — zero XML layouts |
| **Material 3** | Design system, theming, components |
| **Navigation Compose** | Type-safe screen navigation, arguments via `SavedStateHandle` |
| **Compose Canvas** | Custom Gantt timeline chart drawn with `drawRoundRect` |
| **Core SplashScreen** | Branded teal splash screen on launch |
| **Material Icons Extended** | Icon set throughout the app |

### Dependency Injection
| Technology | Purpose |
|---|---|
| **Hilt** | Compile-time dependency injection — `@HiltAndroidApp`, `@HiltViewModel`, `@AndroidEntryPoint` |

### Database and Storage
| Technology | Purpose |
|---|---|
| **Room** | Local SQLite persistence for `HttpTransactionEntity`, `DnsEntryEntity`, `ConnectionEntryEntity` |
| **Room Flow DAOs** | Live database queries that emit on every change |
| **TypeConverters** | Serializes `Map<String,String>`, `List<String>`, and enums to JSON |
| **DataStore Preferences** | App settings — max requests, toggles |
| **SharedPreferences** | Lightweight certificate install flag |

### Networking and Proxy
| Technology | Purpose |
|---|---|
| **LocalProxyServer** | Custom HTTP/HTTPS proxy built entirely with raw Java sockets |
| **TLS MITM** | Per-host X.509 certificate generation for HTTPS body decryption |
| **OkHttp 4** | HTTP client used in `ReplayRequestUseCase` |
| **Retrofit 2** | Available for future API integrations |
| **BouncyCastle** | RSA 2048-bit key generation and X.509 certificate signing |
| **Conscrypt** | Modern TLS provider for Android — replaces legacy SSLContext |

### Background and System
| Technology | Purpose |
|---|---|
| **Foreground Service** | `ProxyForegroundService` keeps proxy alive when app is backgrounded |
| **NotificationManager** | Persistent notification with live request count |
| **FileProvider** | Secure HAR file sharing via `content://` URIs — no exposed file paths |
| **ProxyDetector** | Reads `http.proxyHost` and `http.proxyPort` system properties to verify setup |

### Export and Serialization
| Technology | Purpose |
|---|---|
| **HAR 1.2** | Industry standard HTTP archive format |
| **Gson** | JSON serialization for HAR export and Room TypeConverters |
| **Intent.ACTION_SEND** | Android system share sheet |

---

## Project Structure

```
com.netscope.app/
├── data/
│   ├── export/
│   │   └── ExportManager              # Saves HAR to cache, builds share Intent
│   ├── local/
│   │   ├── converter/                 # Room TypeConverters (enums, maps, lists)
│   │   ├── dao/                       # HttpTransactionDao, DnsEntryDao, ConnectionEntryDao
│   │   ├── database/                  # NetScopeDatabase
│   │   ├── entity/                    # Room entities
│   │   └── mapper/                    # Entity ↔ domain model extension functions
│   ├── proxy/
│   │   ├── cert/
│   │   │   └── CertificateManager     # Generates CA + per-host certs, BKS keystore
│   │   ├── HttpTransactionEmitter     # SharedFlow — broadcasts captured transactions
│   │   ├── LocalProxyServer           # Raw socket HTTP/HTTPS proxy, MITM, body capture
│   │   ├── ProxyDetector              # Reads system proxy properties, returns ProxyStatus
│   │   ├── ProxyForegroundService     # Foreground service, notification, live count
│   │   └── ProxyManager               # Clean start/stop API for the UI layer
│   └── repository/
│       ├── BandwidthRepositoryImpl
│       ├── ConnectionRepositoryImpl   # Extracts connections from transactions
│       ├── DnsRepositoryImpl          # Extracts domains from transaction Host headers
│       ├── SettingsRepositoryImpl     # DataStore backed settings
│       └── TrafficRepositoryImpl      # Persists transactions, enforces max limit
│
├── di/
│   ├── DatabaseModule                 # Room, DAOs
│   ├── NetworkModule                  # OkHttpClient
│   ├── ProxyModule                    # Proxy singletons
│   └── RepositoryModule               # Interface → implementation bindings
│
├── domain/
│   ├── model/
│   │   ├── AppInfo
│   │   ├── AppSettings
│   │   ├── BandwidthSnapshot
│   │   ├── ConnectionEntry
│   │   ├── DnsEntry
│   │   ├── HttpTransaction            # Core model — isSuccess, isSlow, statusCategory
│   │   ├── MaxRequestsOption
│   │   ├── PacketInfo
│   │   └── TrafficFilter              # Filter state + matches() predicate
│   ├── repository/                    # Pure Kotlin interfaces
│   └── usecase/
│       ├── ClearAllTrafficUseCase
│       ├── ExportTrafficUseCase       # Builds HAR 1.2 JSON
│       ├── GetHttpTransactionDetailUseCase
│       ├── ObserveBandwidthUseCase
│       ├── ObserveConnectionsUseCase
│       ├── ObserveDnsEntriesUseCase
│       ├── ObserveHttpTransactionsUseCase
│       ├── ObserveSettingsUseCase
│       └── ReplayRequestUseCase       # Raw socket replay through local proxy
│
└── presentation/
    ├── base/
    │   └── BaseViewModel              # Generic StateFlow ViewModel with updateState
    ├── components/                    # NetScopeTopBar, StatusChip, MethodChip, StatCard
    ├── navigation/
    │   ├── NavArgs
    │   ├── NavRoutes
    │   └── NetScopeNavGraph
    ├── screens/
    │   ├── connections/               # ConnectionsScreen + ConnectionsViewModel
    │   ├── dashboard/                 # DashboardScreen + DashboardViewModel
    │   ├── detail/                    # TrafficDetailScreen + TrafficDetailViewModel
    │   ├── dns/                       # DnsScreen + DnsViewModel
    │   ├── replay/                    # ReplayScreen + ReplayViewModel
    │   ├── settings/                  # SettingsScreen + SettingsViewModel
    │   ├── setup/                     # ProxySetupScreen + ProxySetupViewModel
    │   ├── stats/                     # StatsScreen + StatsViewModel
    │   ├── timeline/                  # TimelineScreen + TimelineViewModel
    │   └── traffic/                   # TrafficListScreen + TrafficListViewModel
    ├── theme/
    │   ├── Color                      # Brand and semantic color tokens
    │   ├── Theme                      # Material 3 dark color scheme
    │   └── Type                       # Typography scale
    └── MainActivity
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android device or emulator running API 26+

### Clone and build

```bash
git clone https://github.com/god-s-only/netscope.git
cd netscope
```

Open in Android Studio, let Gradle sync, then run on a physical device or emulator.

### First launch

1. Open NetScope — tap the **settings icon** on the dashboard
2. On the Setup screen tap **Install** to install the CA certificate and follow the system prompt
3. Tap **Start** to start the local proxy server
4. Tap **Open WiFi Settings** → long press your connected network → Modify → Advanced → Proxy → Manual
5. Enter `127.0.0.1` as the hostname and `8888` as the port → Save
6. Return to NetScope — the setup screen shows a green checkmark when the proxy is detected correctly
7. Open any app, browse a site, and watch requests appear live in the HTTP Traffic screen

---

## Using NetScope with your own app

Add `network_security_config.xml` to your debug app so it trusts the NetScope CA certificate for HTTPS decryption:

**Step 1 — Create `res/xml/network_security_config.xml` in your app**

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <debug-overrides>
        <trust-anchors>
            <certificates src="user"/>
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

**Step 2 — Reference it in your app's `AndroidManifest.xml`**

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

Now every HTTPS request your app makes is captured by NetScope with full plaintext request and response bodies. No interceptors required. No changes to your networking code. Just a config file in debug builds.

---

## Permissions

| Permission | Why it is needed |
|---|---|
| `INTERNET` | Forwarding proxied requests to real servers |
| `ACCESS_NETWORK_STATE` | Checking network availability |
| `FOREGROUND_SERVICE` | Keeping the proxy alive when the app is backgrounded |
| `POST_NOTIFICATIONS` | Persistent proxy status notification on API 33+ |

NetScope does not request location, contacts, phone state, camera, microphone, or any permission unrelated to network inspection.

---

## Privacy

- No data ever leaves your device. NetScope has no backend and makes no outbound connections of its own.
- All captured traffic is stored in a local Room database at `/data/data/com.netscope.app/databases/netscope.db`
- The database is private to the app and inaccessible to other apps
- Clear all data at any time from Settings or directly from each screen
- The CA certificate is generated fresh on first install and is unique per device — never shared

---

## Building a release APK

```bash
./gradlew assembleRelease
```

Create `keystore.properties` at the project root (never commit this file):

```properties
storeFile=../keystore/netscope.keystore
storePassword=your_store_password
keyAlias=netscope
keyPassword=your_key_password
```

Reference it in `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            val props = Properties().apply {
                load(rootProject.file("keystore.properties").inputStream())
            }
            storeFile = file(props["storeFile"] as String)
            storePassword = props["storePassword"] as String
            keyAlias = props["keyAlias"] as String
            keyPassword = props["keyPassword"] as String
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

## Contributing

Contributions are welcome. Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature-name`
3. Commit using conventional commits: `feat:`, `fix:`, `chore:`, `refactor:`
4. Open a pull request with a clear description of what changed and why

### Code style

- All features go through the domain layer — ViewModels never call repositories directly
- Use cases are single-responsibility with one public `operator fun invoke()`
- All Room queries return `Flow` — no blocking calls on the main thread
- New screens require a corresponding `UiState` data class and `BaseViewModel` subclass
- Standard Kotlin formatting — single space around operators, no aligned equals signs

---

## License

```
Copyright 2024 NetScope

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## Acknowledgements

- [OkHttp](https://square.github.io/okhttp/) — HTTP client used in request replay
- [Retrofit](https://square.github.io/retrofit/) — Type-safe HTTP client
- [Hilt](https://dagger.dev/hilt/) — Dependency injection for Android
- [Room](https://developer.android.com/jetpack/androidx/releases/room) — SQLite abstraction layer
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Modern Android UI toolkit
- [BouncyCastle](https://www.bouncycastle.org/) — Certificate generation and signing
- [Conscrypt](https://github.com/google/conscrypt) — Modern TLS provider
- [Charles Proxy](https://www.charlesproxy.com/) — Inspiration for the overall tool concept

---

<p align="center">
  Built for Android developers who want to know exactly what their apps are doing on the network.
</p>
