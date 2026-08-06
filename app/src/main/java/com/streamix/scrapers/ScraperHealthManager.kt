package com.streamix.scrapers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperHealthManager @Inject constructor(
    private val registry: ScraperRegistry
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var healthCheckJob: Job? = null

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking

    // Map of scraper ID to its health status (true = working, false = not working)
    private val _healthStatusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val healthStatusMap: StateFlow<Map<String, Boolean>> = _healthStatusMap

    /**
     * Starts checking the health of all registered scrapers.
     * This runs in the background and updates the ScraperInfo.isWorking property
     * as well as a centralized health status map.
     */
    fun startHealthChecks(force: Boolean = false) {
        if (_isChecking.value && !force) return
        
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            _isChecking.value = true
            val scrapers = registry.getAllScrapers()
            
            // Initialize map with current values
            val currentMap = scrapers.associate { it.info.id to it.info.isWorking }.toMutableMap()
            _healthStatusMap.value = currentMap

            // Check in chunks to avoid overwhelming the network or device
            val chunks = scrapers.chunked(4)
            for (chunk in chunks) {
                chunk.map { scraper ->
                    async {
                        val isWorking = try {
                            // Quick test: Try to get trending content
                            val results = withTimeoutOrNull(10000) {
                                scraper.getTrending(1)
                            }
                            !results.isNullOrEmpty()
                        } catch (e: Exception) {
                            false
                        }
                        
                        // Update the scraper's internal state
                        scraper.info.isWorking = isWorking
                        
                        // Update the centralized map for UI observation
                        synchronized(currentMap) {
                            currentMap[scraper.info.id] = isWorking
                            _healthStatusMap.value = currentMap.toMap()
                        }
                    }
                }.awaitAll()
                
                // Small delay between chunks to be polite
                delay(500)
            }
            
            _isChecking.value = false
        }
    }

    /**
     * Checks health for a specific scraper.
     */
    fun checkSingleScraper(scraperId: String) {
        val scraper = registry.getScraperById(scraperId) ?: return
        scope.launch {
            val isWorking = try {
                val results = withTimeoutOrNull(10000) { scraper.getTrending(1) }
                !results.isNullOrEmpty()
            } catch (e: Exception) {
                false
            }
            scraper.info.isWorking = isWorking
            
            val currentMap = _healthStatusMap.value.toMutableMap()
            currentMap[scraperId] = isWorking
            _healthStatusMap.value = currentMap
        }
    }

    fun stopHealthChecks() {
        healthCheckJob?.cancel()
        _isChecking.value = false
    }
}
