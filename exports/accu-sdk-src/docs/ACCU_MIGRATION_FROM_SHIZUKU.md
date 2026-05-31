# Migrating from Shizuku to ACCU

If your app already integrates Shizuku, this guide shows the exact
mapping between Shizuku concepts and ACCU equivalents.

---

## Key Differences

| Aspect | Shizuku | ACCU |
|---|---|---|
| Service binding action | `moe.shizuku.privilege.api.ShizukuService` | `com.accu.api.AccuSystemService` |
| Package | `moe.shizuku.privileged.api` | `com.accu.controlcenter` |
| Permission check | `Shizuku.checkSelfPermission()` | `IAccuService.checkPermission()` |
| Permission request | `Shizuku.requestPermission(int code)` → `onRequestPermissionsResult` | `IAccuService.requestPermission(callback)` |
| Shell execution | `Shizuku.newProcess(arrayOf("sh"), null, null)` | `IAccuService.exec("command")` |
| Async shell | Manual thread + streams | `IAccuService.execAsync(cmd, callback)` |
| Binder retrieval | `Shizuku.bindUserService()` | `bindService()` to AccuSystemService |
| User service | Must deploy your own UserService.aidl | No user service needed — ACCU IS the service |

---

## Concept Mapping

### 1. Initialization

**Shizuku:**
```kotlin
// Add Shizuku listener
Shizuku.addBinderReceivedListener(binderReceived)
Shizuku.addBinderDeadListener(binderDead)
// Check if Shizuku is running
if (Shizuku.pingBinder()) { ... }
```

**ACCU:**
```kotlin
val accu = AccuClient(context)
accu.connect()  // Handles binding, reconnection, and state automatically
// Observe state
accu.state.collect { state -> ... }
```

---

### 2. Permission Check

**Shizuku:**
```kotlin
if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
    // has Shizuku
}
```

**ACCU:**
```kotlin
val code = accu.checkPermission()
if (code == AccuConstants.PERMISSION_GRANTED) {
    // has ACCU
}
```

---

### 3. Permission Request

**Shizuku:**
```kotlin
// Request (code-based, uses onRequestPermissionsResult)
if (Shizuku.shouldShowRequestPermissionRationale()) { ... }
Shizuku.requestPermission(REQUEST_CODE)

// In Activity:
override fun onRequestPermissionsResult(requestCode: Int, ...) {
    if (requestCode == REQUEST_CODE) { /* handle */ }
}
```

**ACCU:**
```kotlin
// Coroutine-based, no request codes needed
viewModelScope.launch {
    val result = accu.requestPermission()  // suspends until user responds
    if (result == AccuConstants.PERMISSION_GRANTED) { /* proceed */ }
}
```

---

### 4. Shell Execution

**Shizuku:**
```kotlin
// Shizuku shell (manual process management)
val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
val stdout = process.inputStream.bufferedReader().readText()
val stderr = process.errorStream.bufferedReader().readText()
val exit = process.waitFor()
```

**ACCU:**
```kotlin
// Sync (same thread)
val result = accu.exec(command)
val stdout = result.stdout
val stderr = result.stderr
val exit = result.exitCode

// Async (streaming)
accu.execAsync(command,
    onStdout = { line -> /* handle */ },
    onStderr = { line -> /* handle */ },
    onExit   = { code -> /* done */ },
)
```

---

### 5. User Service (Shizuku's "bind user service")

**Shizuku approach:** You define your own AIDL service, deploy it to Shizuku's
user service host, bind to it, and manage its lifecycle.

**ACCU approach:** No user service needed. ACCU IS the service. You bind directly
to `AccuSystemService` and call its 25 built-in privileged APIs.

If you need a custom privileged operation that ACCU doesn't expose, use:
```kotlin
// ACCU's exec() runs arbitrary shell — covers most use cases
val result = accu.exec("your-custom-pm-or-am-command")
```

---

### 6. Dependency Changes

**Remove from build.gradle.kts:**
```kotlin
// REMOVE:
implementation("dev.rikka.shizuku:api:13.1.5")
implementation("dev.rikka.shizuku:provider:13.1.5")
```

**Remove from AndroidManifest.xml:**
```xml
<!-- REMOVE: -->
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:exported="true"
    android:grantUriPermissions="true"
    android:multiprocess="true"
    tools:ignore="ExportedContentProvider" />
```

**Add to build.gradle.kts:**
```kotlin
// ADD:
buildFeatures { aidl = true }
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
```

**Add to AndroidManifest.xml:**
```xml
<!-- ADD: -->
<queries>
    <package android:name="com.accu.controlcenter" />
</queries>
```

---

### 7. Full Migration Example

**Before (Shizuku):**
```kotlin
class MyViewModel : ViewModel() {
    init {
        Shizuku.addBinderReceivedListenerSticky(::onBinderReceived)
    }

    private fun onBinderReceived() {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(1001)
            return
        }
        doPrivilegedWork()
    }

    fun runShell(cmd: String) {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        // use output
    }
}
```

**After (ACCU):**
```kotlin
class MyViewModel(app: Application) : AndroidViewModel(app) {
    private val accu = AccuClient(app)
    val state = accu.state

    init { accu.connect() }
    override fun onCleared() { accu.disconnect() }

    fun requestPermission() {
        viewModelScope.launch {
            accu.requestPermission()
        }
    }

    fun runShell(cmd: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { accu.exec(cmd) }
            // use result.stdout
        }
    }
}
```

---

## Feature Parity Matrix

| Shizuku Feature | ACCU Equivalent | Notes |
|---|---|---|
| `newProcess()` shell | `exec()` / `execAsync()` | ACCU handles process management |
| `checkSelfPermission()` | `checkPermission()` | Returns typed int code |
| `requestPermission()` | `requestPermission()` | Coroutine-based, no request codes |
| Binder ping | `ping()` | Same concept |
| User service | Not needed | ACCU's 25 built-in APIs cover most use cases |
| `addBinderReceivedListener` | Observe `accuState` | StateFlow-based |
| `addBinderDeadListener` | `AccuConnectionState.Disconnected` | Auto-reconnects |
| `getUid()` | `getUid()` | Same |
| `getPid()` | `getPid()` | Same |
