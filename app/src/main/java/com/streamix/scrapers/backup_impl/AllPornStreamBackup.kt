package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class AllPornStreamBackupScraper : UniversalAdultScraper(
    id = "allpornstream_backup",
    displayName = "AllPornStream (Backup)",
    baseUrl = "https://allpornstream.com"
)
