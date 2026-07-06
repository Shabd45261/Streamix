package com.streamix.scraper.cloudstream.providers.gizlikeyif

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element
import com.streamix.scraper.cloudstream.utils.AppUtils.parseJson

class NetFapXProvider : MainAPI() {
    override var mainUrl = "https://netfapx.com"
    override var name = "NetFapX"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "${mainUrl}" to "All Videos",
        "${mainUrl}/tag/step-mom" to "Step Mom",
        "${mainUrl}/tag/step-sister" to "Step Sister",
        "${mainUrl}/category/milf" to "Milf",
        "${mainUrl}/category/big-ass" to "Big Ass",
        "${mainUrl}/category/big-tits" to "Big Tits",
        "${mainUrl}/category/asian" to "Asian"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("article").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val href = fixUrl(aTag.attr("href"))

        val img = aTag.selectFirst("img")
        val title = img?.attr("alt")?.trim() ?: return null
        val posterUrl = fixUrl(img?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select("article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null

        val poster = fixUrl(document.selectFirst("meta[property='og:image']")?.attr("content") ?: "")

        val plot = document.selectFirst("div.textbox h2:contains(Description:) + div")?.selectFirst("p")?.text()?.trim()

        val actors = document.select("div.infovideo h2:contains(Pornstars:) + p a").mapNotNull {
            val actorName = it.text().trim()
            if (actorName.isEmpty()) null else ActorData(Actor(actorName))
        }

        val categories = document.select("div.infovideo h2:contains(Categories:) + p a").map { it.text().trim() }
        val tagsFromHtml = document.select("div.infovideo h2:contains(Tags:) + p a").map { it.text().trim() }
        val tags = (categories + tagsFromHtml).distinct()
        val recommendations = document.select("article").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = plot
            this.tags = tags
            this.actors = actors
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        val scriptElement = document.selectFirst("script#wp-postviews-cache-js-extra") ?: return false
        val scriptContent = scriptElement.data()

        val postIdPattern = "\"post_id\":\"(\\d+)\"".toRegex()
        val postIdMatch = postIdPattern.find(scriptContent)
        val postId = postIdMatch?.groupValues?.get(1) ?: return false

        val ajaxUrl = "https://netfapx.com/wp-admin/admin-ajax.php"

        val ajaxResponse = app.post(
            url = ajaxUrl,
            data = mapOf(
                "action" to "get_video_url",
                "idpost" to postId
            ),
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to data,
                "Origin" to "https://netfapx.com"
            )
        )

        if (ajaxResponse.code != 200) return false

        val responseText = ajaxResponse.text
        val videoUrlPattern = "https://videos\\.netfapx\\.com/[^\"'\\s]+\\.mp4".toRegex()
        val urlMatch = videoUrlPattern.find(responseText)

        if (urlMatch != null) {
            val videoUrl = urlMatch.value
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = videoUrl,
                    referer = data,
                    quality = 0,
                    type = ExtractorLinkType.VIDEO
                )
            )
            return true
        }

        return false
    }
}
