package com.example

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.system.Os
import android.webkit.WebView
import com.example.data.AppRepository
import com.example.database.AppDatabase
import com.example.util.LocaleHelper
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FocusLockApplication : Application() {

    companion object {
        init {
            configureMesaSoftwareEnvironment()
        }

        /**
         * Configures Mesa EGL environment variables before RenderThread or WebView initializes
         * so Mesa uses its software rasterizer directly instead of probing missing /dev/dri/renderD128
         * and logging "E/MESA: Failed to open rendernode: No such file or directory".
         */
        fun configureMesaSoftwareEnvironment() {
            try {
                Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                Os.setenv("MESA_DEBUG", "silent", true)
                Os.setenv("EGL_LOG_LEVEL", "fatal", true)
            } catch (_: Throwable) {
            }
        }
    }

    init {
        configureMesaSoftwareEnvironment()
    }

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.focusDao()) }

    override fun attachBaseContext(base: Context) {
        configureMesaSoftwareEnvironment()
        // Keep the original ContextImpl as Application's base context so
        // ActivityThread.handleReceiver can safely cast app.getBaseContext() to ContextImpl.
        LocaleHelper.restoreLocaleAfterWebView(base)
        super.attachBaseContext(base)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.restoreLocaleAfterWebView(this)
    }

    override fun onCreate() {
        super.onCreate()
        configureMesaSoftwareEnvironment()
        LocaleHelper.restoreLocaleAfterWebView(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val processName = getProcessName()
                if (!processName.isNullOrBlank() && packageName != processName) {
                    val safeSuffix = processName.replace(":", "_").replace(".", "_")
                    WebView.setDataDirectorySuffix(safeSuffix)
                }
            } catch (_: Exception) {
            }
        }

        ensureChromiumSimpleCacheStructure()
    }

    /**
     * Ensures the Chromium SimpleCache directories and their 24-byte FakeIndexData ("index")
     * files exist before any WebView is instantiated. This prevents concurrent SimpleBackendImpl
     * threads from racing on O_CREAT|O_EXCL in WriteFakeIndexFile (simple_version_upgrade.cc:151)
     * and deleting Code Cache/js while SimpleFileEnumerator (simple_file_enumerator.cc:21) opens it.
     */
    internal fun ensureChromiumSimpleCacheStructure() {
        try {
            val httpCacheDir = File(cacheDir, "WebView/Default/HTTP Cache")
            val codeCacheJsDir = File(httpCacheDir, "Code Cache/js")
            val codeCacheWasmDir = File(httpCacheDir, "Code Cache/wasm")

            val cacheDirs = listOf(httpCacheDir, codeCacheJsDir, codeCacheWasmDir)

            // 1. Ensure each cache directory and its "index-dir" subdirectory exist
            for (dir in cacheDirs) {
                val indexDir = File(dir, "index-dir")
                if (!indexDir.exists()) {
                    indexDir.mkdirs()
                }
            }

            // 2. Determine reference FakeIndexData bytes (prefer existing Chromium-generated index if present)
            val existingValidIndex = cacheDirs
                .map { File(it, "index") }
                .firstOrNull { it.exists() && it.isFile && it.length() >= 16L }

            val fakeIndexBytes: ByteArray = if (existingValidIndex != null) {
                existingValidIndex.readBytes()
            } else {
                // Standard 64-bit Chromium SimpleCache FakeIndexData struct (24 bytes, little-endian):
                // uint64_t initial_magic_number = 0xfcfb6d1ba7725c30ULL
                // uint32_t version = 9
                // uint32_t zero = 0, uint32_t zero2 = 0, uint32_t padding = 0
                ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
                    putLong(-217460013075473360L) // 0xfcfb6d1ba7725c30ULL
                    putInt(9)
                    putInt(0)
                    putInt(0)
                    putInt(0)
                }.array()
            }

            for (dir in cacheDirs) {
                val indexFile = File(dir, "index")
                if (!indexFile.exists() || indexFile.length() == 0L) {
                    indexFile.writeBytes(fakeIndexBytes)
                }
            }
        } catch (_: Exception) {
        }
    }
}
