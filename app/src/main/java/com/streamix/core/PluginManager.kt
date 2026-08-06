package com.streamix.core

import android.content.Context
import android.os.Build
import dalvik.system.DexClassLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginManager @Inject constructor(
    private val context: Context
) {
    private val pluginsDir = File(context.filesDir, "plugins")
    private val loadedPlugins = mutableMapOf<String, MainAPI>()

    init {
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
        }
    }

    /**
     * Loads a plugin from a .dex file.
     * @param pluginFile The .dex file to load.
     * @param entryPoint The fully qualified class name of the MainAPI implementation.
     */
    fun loadPlugin(pluginFile: File, entryPoint: String): MainAPI? {
        try {
            // Android 14+ security requirement: file must be read-only
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                pluginFile.setReadOnly()
            }

            val classLoader = DexClassLoader(
                pluginFile.absolutePath,
                context.cacheDir.absolutePath,
                null,
                context.classLoader
            )

            val pluginClass = classLoader.loadClass(entryPoint)
            val provider = pluginClass.getDeclaredConstructor().newInstance() as? MainAPI
            
            if (provider != null) {
                loadedPlugins[provider.name] = provider
                return provider
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getPlugins(): List<MainAPI> = loadedPlugins.values.toList()

    fun getPlugin(name: String): MainAPI? = loadedPlugins[name]
}
