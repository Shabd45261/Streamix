package com.streamix.core.plugin

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class RepoConfig(
    val id   : String,
    val name : String,
    val url  : String   // points to index.json
)

@Singleton
class RepoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "streamix_repos"
        private const val KEY_REPOS  = "repos_json"
        // Default built-in repo
        private const val DEFAULT_REPO_URL = "https://raw.githubusercontent.com/YOURNAME/streamix-extensions/main/index.json"
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    fun getRepos(): List<RepoConfig> {
        val json = prefs.getString(KEY_REPOS, null) ?: return listOf(
            RepoConfig("default", "Streamix Official", DEFAULT_REPO_URL)
        )
        return runCatching { parseReposJson(json) }.getOrDefault(emptyList())
    }

    fun addRepo(repo: RepoConfig) {
        val updated = getRepos().filterNot { it.id == repo.id } + repo
        prefs.edit().putString(KEY_REPOS, serializeRepos(updated)).apply()
    }

    fun removeRepo(id: String) {
        val updated = getRepos().filterNot { it.id == id }
        prefs.edit().putString(KEY_REPOS, serializeRepos(updated)).apply()
    }

    private fun parseReposJson(json: String): List<RepoConfig> {
        val arr = org.json.JSONArray(json)
        return (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            RepoConfig(obj.getString("id"), obj.getString("name"), obj.getString("url"))
        }
    }

    private fun serializeRepos(repos: List<RepoConfig>): String {
        val arr = org.json.JSONArray()
        repos.forEach { r ->
            arr.put(org.json.JSONObject().apply {
                put("id", r.id); put("name", r.name); put("url", r.url)
            })
        }
        return arr.toString()
    }
}
