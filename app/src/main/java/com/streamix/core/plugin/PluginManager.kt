package com.streamix.core.plugin

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class LoadedPlugin(
    val manifest : ExtensionManifest,
    val source   : StreamSource,
    val apkPath  : String
)

@Singleton
class PluginManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _plugins = MutableStateFlow<Map<String, LoadedPlugin>>(emptyMap())
    val plugins: StateFlow<Map<String, LoadedPlugin>> = _plugins.asStateFlow()

    private val pluginsDir: File
        get() = File(context.filesDir, "plugins").also { it.mkdirs() }

    private val mockFile: File
        get() = File(pluginsDir, "mock_plugins.json")

    init {
        loadInstalledPlugins()
    }

    // ── Load all plugins on startup ──────────────────────────────────────────

    fun loadInstalledPlugins() {
        val loaded = mutableMapOf<String, LoadedPlugin>()
        
        // Load APK plugins
        pluginsDir.listFiles { f -> f.extension == "apk" }?.forEach { apkFile ->
            runCatching { loadApk(apkFile) }
                .onSuccess { plugin -> loaded[plugin.manifest.id] = plugin }
                .onFailure { Log.e("PluginManager", "Failed to load ${apkFile.name}", it) }
        }

        // Scan Local Stash Scrapers as plugins
        val localDir = File("D:/Android/Streamix/Scrapers/stashapp_communityscrapers/CommunityScrapers")
        if (localDir.exists() && localDir.isDirectory) {
            scanLocalPluginsRecursive(localDir, loaded)
        }
        
        _plugins.value = loaded
    }

    private fun scanLocalPluginsRecursive(dir: File, loaded: MutableMap<String, LoadedPlugin>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (file.name != ".git" && file.name != "node_modules") {
                    scanLocalPluginsRecursive(file, loaded)
                }
            } else if (file.extension == "yml") {
                try {
                    val category = file.parentFile?.name ?: "General"
                    val id = "local-${file.nameWithoutExtension.lowercase()}"
                    val manifest = ExtensionManifest(
                        id = id,
                        name = file.nameWithoutExtension,
                        version = "1.0.0",
                        mainClass = "StashYamlScraper",
                        categories = listOf(ContentCategory.ADULT),
                        author = "Community",
                        iconUrl = ""
                    )
                    
                    // Note: StashScraperAdapter needs to be compatible with StashYamlScraper
                    // Or we just use a simplified mock for now to satisfy the UI
                } catch (e: Exception) {
                    Log.e("PluginManager", "Failed to load local plugin: ${file.name}", e)
                }
            }
        }
    }

    // ── Query ────────────────────────────────────────────────────────────────

    fun getPlugin(sourceId: String): LoadedPlugin? = _plugins.value[sourceId]

    fun isInstalled(id: String): Boolean = _plugins.value.containsKey(id)

    fun getPluginsForCategory(cat: ContentCategory): List<LoadedPlugin> =
        _plugins.value.values.filter { cat in it.manifest.categories }

    // ── Install ──────────────────────────────────────────────────────────────

    fun installFromUrl(apkUrl: String, manifestUrl: String): Flow<InstallState> = flow {
        emit(InstallState.Progress(0))

        val client = OkHttpClient.Builder().build()

        // 1. Fetch and parse manifest JSON
        val manifestJson = client.newCall(Request.Builder().url(manifestUrl).build())
            .execute().body?.string()
            ?: run { emit(InstallState.Error("Failed to fetch manifest")); return@flow }

        val manifest = runCatching { parseManifestJson(manifestJson) }.getOrElse {
            emit(InstallState.Error("Invalid manifest: ${it.message}")); return@flow
        }

        // 2. Download APK with progress
        emit(InstallState.Progress(10))
        val apkFile = File(pluginsDir, "${manifest.id}.apk")
        val response = client.newCall(Request.Builder().url(apkUrl).build()).execute()
        val body = response.body ?: run { emit(InstallState.Error("Empty APK response")); return@flow }
        val totalBytes = body.contentLength().takeIf { it > 0 } ?: 1L
        var downloadedBytes = 0L

        apkFile.outputStream().use { out ->
            body.byteStream().use { input ->
                val buf = ByteArray(8 * 1024)
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                    downloadedBytes += read
                    val pct = (10 + (downloadedBytes.toFloat() / totalBytes * 80)).toInt()
                    emit(InstallState.Progress(pct.coerceAtMost(90)))
                }
            }
        }

        // 3. Load the plugin into memory
        emit(InstallState.Progress(95))
        runCatching { loadApk(apkFile) }
            .onSuccess { plugin ->
                _plugins.update { it + (plugin.manifest.id to plugin) }
                emit(InstallState.Progress(100))
                emit(InstallState.Success)
            }
            .onFailure {
                apkFile.delete()
                emit(InstallState.Error("Failed to load plugin: ${it.message}"))
            }
    }.flowOn(Dispatchers.IO)

    // ── Uninstall ────────────────────────────────────────────────────────────

    fun uninstall(id: String) {
        val plugin = _plugins.value[id]
        if (plugin?.apkPath == "mock") {
            _plugins.update { it - id }
            saveMockPlugins()
        } else {
            File(pluginsDir, "$id.apk").delete()
            _plugins.update { it - id }
        }
    }

    private fun saveMockPlugins() {
        val mocks = _plugins.value.values.filter { it.apkPath == "mock" }
        val arr = org.json.JSONArray()
        mocks.forEach { 
            val obj = org.json.JSONObject().apply {
                put("id", it.manifest.id)
                put("name", it.manifest.name)
                put("version", it.manifest.version)
                put("mainClass", it.manifest.mainClass)
                put("categories", org.json.JSONArray().apply { 
                    it.manifest.categories.forEach { cat -> put(cat.name) } 
                })
                put("author", it.manifest.author)
            }
            arr.put(obj.toString())
        }
        mockFile.writeText(arr.toString())
    }

    // ── Internal APK Loading via DexClassLoader ───────────────────────────────

    private fun loadApk(apkFile: File): LoadedPlugin {
        val optimizedDir = File(context.codeCacheDir, "dex").also { it.mkdirs() }
        val classLoader = DexClassLoader(
            apkFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            context.classLoader
        )
        // Read manifest JSON from APK assets
        val manifestJson = readAssetFromApk(apkFile, "extension.json")
            ?: throw IllegalStateException("extension.json missing from APK")
        val manifest = parseManifestJson(manifestJson)

        // Instantiate the StreamSource implementation
        val clazz = classLoader.loadClass(manifest.mainClass)
        val source = clazz.getDeclaredConstructor().newInstance() as StreamSource

        return LoadedPlugin(manifest, source, apkFile.absolutePath)
    }

    private fun readAssetFromApk(apkFile: File, assetName: String): String? = runCatching {
        val zip = java.util.zip.ZipFile(apkFile)
        val entry = zip.getEntry("assets/$assetName") ?: return null
        zip.getInputStream(entry).bufferedReader().use { it.readText() }
    }.getOrNull()

    private fun parseManifestJson(json: String): ExtensionManifest {
        val obj = org.json.JSONObject(json)
        return ExtensionManifest(
            id            = obj.getString("id"),
            name          = obj.getString("name"),
            version       = obj.getString("version"),
            mainClass     = obj.getString("mainClass"),
            categories    = obj.getJSONArray("categories").let { arr ->
                (0 until arr.length()).map { ContentCategory.valueOf(arr.getString(it)) }
            },
            author        = obj.optString("author", "unknown"),
            iconUrl       = obj.optString("iconUrl", ""),
            requiresLogin = obj.optBoolean("requiresLogin", false),
            changelog     = obj.optString("changelog", "")
        )
    }
}
