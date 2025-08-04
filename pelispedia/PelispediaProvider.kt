package com.muruffin.pelispedia

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class PelispediaProvider : MainAPI() {
    override var mainUrl = "https://pelispedia.mov"
    override var name = "Pelispedia"
    override val hasMainPage = true
    override val lang = "es"
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/peliculas/page/" to "Películas"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val doc = app.get(request.data + page).document
        val movies = doc.select("article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, movies)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("article").mapNotNull {
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): MovieSearchResponse? {
        val title = selectFirst("h2")?.text()?.trim() ?: return null
        val link = selectFirst("a")?.attr("href") ?: return null
        val poster = selectFirst("img")?.attr("src")
        return newMovieSearchResponse(title, link, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text() ?: "Película"
        val poster = doc.selectFirst("img.wp-post-image")?.attr("src")
        val iframe = doc.selectFirst("iframe")?.attr("src") ?: throw ErrorLoadingException("No se encontró reproductor")

        val sources = mutableListOf<ExtractorLink>()
        loadExtractor(iframe, mainUrl, sources)

        return newMovieLoadResponse(title, url, TvType.Movie, sources) {
            this.posterUrl = poster
        }
    }
}
