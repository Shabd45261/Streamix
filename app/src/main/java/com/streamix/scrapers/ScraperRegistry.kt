package com.streamix.scrapers

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.adult.*
import com.streamix.scrapers.movies.*
import com.streamix.scrapers.music.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperRegistry @Inject constructor() {

    // ── ALL SCRAPERS — register every scraper here ───────────────────────────
    private val allScrapers: List<BaseScraper> = listOf(
        // Adult
        PornhubScraper(),
        XVideosScraper(),
        XHamsterScraper(),
        SpankBangScraper(),
        XNXXScraper(),
        EpornerScraper(),
        EromeScraper(),
        MissAVScraper(),
        RedgifsScraper(),
        MotherlessScraper(),
        BeegScraper(),
        JavGuruScraper(),
        HanimeScraper(),
        Rule34VideoScraper(),
        VsexScraper(),
        PornHatScraper(),
        FypttScraper(),
        HqpornerScraper(),
        JavseenScraper(),
        JavMostScraper(),
        AnimeIdHentaiScraper(),
        HentaiHavenScraper(),
        JavtifulScraper(),
        DesiXFlixScraper(),
        FikFapScraper(),
        RedtubeScraper(),

        // Backup Scrapers (Old versions)
        PornhubBackupScraper(),
        XVideosBackupScraper(),
        XHamsterBackupScraper(),
        SpankBangBackupScraper(),
        XNXXBackupScraper(),
        EpornerBackupScraper(),
        MissAVBackupScraper(),
        RedgifsBackupScraper(),
        MotherlessBackupScraper(),
        JavGuruBackupScraper(),
        HanimeBackupScraper(),
        AllPornStreamBackupScraper(),
        
        // Batch 1 (Universal Scrapers)
        UniversalAdultScraper("asianpinay", "Asian Pinay", "https://asianpinay.com"),
        UniversalAdultScraper("javx", "JavX", "https://javx.tv"),
        UniversalAdultScraper("vlxx", "Vlxx", "https://vlxx.tv"),
        UniversalAdultScraper("superjav", "SuperJav", "https://superjav.com"),
        UniversalAdultScraper("sxyprn", "SxyPrn", "https://sxyprn.com"),
        UniversalAdultScraper("xtapes", "Xtapes", "https://xtapes.to"),
        UniversalAdultScraper("allpornstream", "AllPornStream", "https://allpornstream.com"),
        UniversalAdultScraper("freepornvideos", "FreePornVideos", "https://freepornvideos.com"),
        UniversalAdultScraper("fullporner", "FullPorner", "https://fullporner.com"),
        UniversalAdultScraper("hentaicity", "HentaiCity", "https://hentaicity.com"),
        UniversalAdultScraper("hentaipro", "HentaiPro", "https://hentaipro.com"),
        UniversalAdultScraper("incestflix", "IncestFlix", "https://incestflix.com"),
        UniversalAdultScraper("internetchicks", "InternetChicks", "https://internetchicks.com"),
        UniversalAdultScraper("javfree", "JavFree", "https://javfree.me"),
        UniversalAdultScraper("javhd", "JAVHd", "https://javhd.com"),
        UniversalAdultScraper("javpoint", "JAVPoint", "https://javpoint.me"),
        UniversalAdultScraper("mangoporn", "Mangoporn", "https://mangoporn.net"),
        UniversalAdultScraper("netflav", "Netflav", "https://netflav.com"),
        UniversalAdultScraper("pornhits", "PornHits", "https://pornhits.com"),
        UniversalAdultScraper("pornhoarder", "Pornhoarder", "https://pornhoarder.tv"),

        // Batch 2 (Universal Scrapers)
        UniversalAdultScraper("pornken", "Pornken", "https://pornken.com"),
        UniversalAdultScraper("pornmz", "Pornmz", "https://pornmz.com"),
        UniversalAdultScraper("pornobae", "Pornobae", "https://pornobae.com"),
        UniversalAdultScraper("pornone", "PornOne", "https://pornone.com"),
        UniversalAdultScraper("porntrex", "Porntrex", "https://porntrex.com"),
        UniversalAdultScraper("sextb", "Sextb", "https://sextb.net"),
        UniversalAdultScraper("tollypro", "TollyPro", "https://tollypro.com"),
        UniversalAdultScraper("trendyporn", "TrendyPorn", "https://trendyporn.com"),
        UniversalAdultScraper("uncut99", "Uncut99", "https://uncut99.com"),
        UniversalAdultScraper("uncutmaza", "UncutMaza", "https://uncutmaza.com"),
        UniversalAdultScraper("whoreshub", "Whoreshub", "https://whoreshub.com"),
        UniversalAdultScraper("xmaza", "Xmaza", "https://xmaza.com"),
        UniversalAdultScraper("xmovies4u", "Xmovies4u", "https://xmovies4u.com"),
        UniversalAdultScraper("xnhau", "Xnhau", "https://xnhau.com"),
        UniversalAdultScraper("xprimehub", "XPrimeHub", "https://xprimehub.com"),
        UniversalAdultScraper("yespornplease", "YesPornPlease", "https://yespornplease.com"),
        UniversalAdultScraper("ymaal", "YMaal", "https://ymaal.com"),
        UniversalAdultScraper("adultdeepfakes", "AdultDeepFakes", "https://adultdeepfakes.com"),
        UniversalAdultScraper("aznude", "AZNude", "https://aznude.com"),
        UniversalAdultScraper("coomer", "Coomer", "https://coomer.su"),
        UniversalAdultScraper("cosxplay", "CosXPlay", "https://cosxplay.com"),

        // Batch 3 (Universal Scrapers)
        UniversalAdultScraper("cumlouder", "Cumlouder", "https://cumlouder.com"),
        UniversalAdultScraper("dirtyship", "DirtyShip", "https://dirtyship.com"),
        UniversalAdultScraper("doeda", "Doeda", "https://doeda.com"),
        UniversalAdultScraper("domlepen", "Domlepen", "https://domlepen.com"),
        UniversalAdultScraper("efukt", "EFukt", "https://efukt.com"),
        UniversalAdultScraper("epawg", "ePawg", "https://epawg.com"),
        UniversalAdultScraper("erothots", "EroThots", "https://erothots.com"),
        UniversalAdultScraper("evooli", "Evooli", "https://evooli.com"),
        UniversalAdultScraper("familyporn", "FamilyPorn", "https://familyporn.tv"),
        UniversalAdultScraper("fapix", "Fapix", "https://fapix.com"),
        UniversalAdultScraper("fitnakedgirls", "FitNakedGirls", "https://fitnakedgirls.com"),
        UniversalAdultScraper("freeuseporn", "FreeUsePorn", "https://freeuseporn.com"),
        UniversalAdultScraper("girlsswallowed", "GirlsSwallowed", "https://girlsswallowed.com"),
        UniversalAdultScraper("hdabla", "HdAbla", "https://hdabla.com"),
        UniversalAdultScraper("hdstreaming", "HDStreaming", "https://hdstreaming.tv"),
        UniversalAdultScraper("heavyr", "HeavyR", "https://heavyr.com"),
        UniversalAdultScraper("hentaila", "Hentaila", "https://hentaila.com"),
        UniversalAdultScraper("hentaiworld", "HentaiWorld", "https://hentaiworld.com"),
        UniversalAdultScraper("hentaizm", "Hentaizm", "https://hentaizm.com"),

        // Batch 4 (Universal Scrapers)
        UniversalAdultScraper("hotleak", "HotLeak", "https://hotleak.vip"),
        UniversalAdultScraper("hqcollect", "HQCollect", "https://hqcollect.net"),
        UniversalAdultScraper("kalite18", "Kalite18", "https://kalite18.com"),
        UniversalAdultScraper("kopeda", "Kopeda", "https://kopeda.com"),
        UniversalAdultScraper("koreanpornmovie", "KoreanPornMovie", "https://koreanpornmovie.com"),
        UniversalAdultScraper("koreaye", "Koreaye", "https://koreaye.com"),
        UniversalAdultScraper("livecamrips", "LiveCamRips", "https://livecamrips.com"),
        UniversalAdultScraper("maheir", "Maheir", "https://maheir.com"),
        UniversalAdultScraper("milfnut", "MilfNut", "https://milfnut.com"),
        UniversalAdultScraper("netfapx", "NetFapX", "https://netfapx.net"),
        UniversalAdultScraper("noodlemagazine", "NoodleMagazine", "https://noodlemagazine.com"),
        UniversalAdultScraper("notmik", "Notmik", "https://notmik.com"),
        UniversalAdultScraper("onscreens", "OnScreens", "https://onscreens.com"),
        UniversalAdultScraper("oppaistream", "OppaiStream", "https://oppaistream.com"),
        UniversalAdultScraper("perfectgirls", "PerfectGirls", "https://perfectgirls.net"),
        UniversalAdultScraper("pimpbunny", "Pimpbunny", "https://pimpbunny.com"),
        UniversalAdultScraper("pinkueiga", "Pinkueiga", "https://pinkueiga.com"),
        UniversalAdultScraper("porn00", "Porn00", "https://porn00.com"),
        UniversalAdultScraper("porn36", "Porn36", "https://porn36.com"),
        UniversalAdultScraper("pornocarioca", "Pornocarioca", "https://pornocarioca.com"),
        UniversalAdultScraper("pornslash", "Pornslash", "https://pornslash.com"),
        UniversalAdultScraper("pornwatch", "PornWatch", "https://pornwatch.ws"),
        UniversalAdultScraper("roshytv", "RoshyTv", "https://roshytv.com"),
        UniversalAdultScraper("rouvideo", "RouVideo", "https://rouvideo.com"),
        UniversalAdultScraper("rusporn", "Rusporn", "https://rusporn.net"),
        UniversalAdultScraper("sexalarab", "SexAlArab", "https://sexalarab.com"),
        UniversalAdultScraper("sexfilm", "Sexfilm", "https://sexfilm.com"),
        UniversalAdultScraper("thotdeep", "ThotDeep", "https://thotdeep.com"),
        UniversalAdultScraper("tubepornclassic", "Tubepornclassic", "https://tubepornclassic.com"),
        UniversalAdultScraper("turkhub", "TurkHub", "https://turkhub.com"),
        UniversalAdultScraper("turkifsahub", "Turkifsahub", "https://turkifsahub.com"),
        UniversalAdultScraper("turkifsalar", "TurkIfsalar", "https://turkifsalar.com"),
        UniversalAdultScraper("turkporno", "TurkPorno", "https://turkporno.com"),
        UniversalAdultScraper("videocelebs", "VideoCelebs", "https://videocelebs.net"),
        UniversalAdultScraper("watchporn", "WatchPorn", "https://watchporn.to"),
        UniversalAdultScraper("wumaobi", "Wumaobi", "https://wumaobi.com"),
        UniversalAdultScraper("xchina", "XChina", "https://xchina.co"),
        UniversalAdultScraper("xmoviesforyou", "XMoviesForYou", "https://xmoviesforyou.com"),
        UniversalAdultScraper("xnalgas", "XNalgas", "https://xnalgas.com"),
        UniversalAdultScraper("xpaja", "Xpaja", "https://xpaja.com"),
        UniversalAdultScraper("xrares", "XRares", "https://xrares.com"),
        UniversalAdultScraper("xxxparodyhd", "XXXParodyHD", "https://xxxparodyhd.com"),
        UniversalAdultScraper("youperv", "Youperv", "https://youperv.com"),
        UniversalAdultScraper("ytboob", "YTBoob", "https://ytboob.com"),

        // Movies & Shows
        HDTodayScraper(),
        VegaMoviesScraper(),
        MovieBoxScraper(),
        UniversalMovieScraper("moviesdrive", "MoviesDrive", "https://moviesdrive.forum"),
        UniversalMovieScraper("moviesmod", "Moviesmod", "https://moviesmod.info"),
        UniversalMovieScraper("onlinemovieshindi", "OnlineMoviesHindi", "https://onlinemovieshindi.com"),
        UniversalMovieScraper("9kmovies", "9kMovies", "https://9kmovies.ren/m"),
        UniversalMovieScraper("10hitmovies", "TenHitMovies", "https://10hitmovies.com"),
        UniversalMovieScraper("dflix", "Dflix", "https://dflix.discoveryftp.net"),
        UniversalMovieScraper("themoviesflix", "TheMoviesFlix", "https://themoviesflix.email"),
        UniversalMovieScraper("watchmoviespk", "WatchMoviesPk", "https://www.watch-movies.com.pk"),
        UniversalMovieScraper("movies4u", "Movies4u", "https://movies4u.co"),
        UniversalMovieScraper("multimovies", "MultiMovies", "https://multimovies.me"),
        UniversalMovieScraper("uhdmovies", "UHDmovies", "https://uhdmovies.io"),
        UniversalMovieScraper("zinkmovies", "Zinkmovies", "https://zinkmovies.com"),
        UniversalMovieScraper("bollyflix", "BollyFlix", "https://bollyflix.frl"),
        UniversalMovieScraper("cinestream", "CineStream", "https://cinemeta-catalogs.strem.io"),
        UniversalMovieScraper("emwbd", "EmwBD", "https://www.emwbd1.fun/"),
        UniversalMovieScraper("fullymaza", "FullyMaza", "https://fullymaza.qpon"),
        UniversalMovieScraper("mp4moviez", "Mp4Moviez", "https://www.mp4moviez.capital"),
        UniversalMovieScraper("bolly4u", "Bolly4u", "https://bolly4u.org"),
        UniversalMovieScraper("katmoviehd", "KatMovieHD", "https://katmoviehd.nl"),
        UniversalMovieScraper("extramovies", "ExtraMovies", "https://extramovies.click"),
        UniversalMovieScraper("hdhub4u", "HDHub4u", "https://hdhub4u.run"),
        UniversalMovieScraper("yts", "YTS", "https://yts.mx"),
        
        // Discovered Movie Scrapers
        UniversalMovieScraper("allmovieland", "AllMovieLand", "https://allmovieland.you"),
        UniversalMovieScraper("allwish", "AllWish", "https://all-wish.me"),
        UniversalMovieScraper("ani-db", "AniDb", "https://anidb.app"),
        UniversalMovieScraper("animeav1", "Animeav1", "https://animeav1.com"),
        UniversalMovieScraper("animecloud", "AnimeCloud", "https://fireani.me"),
        UniversalMovieScraper("animedekho", "AnimeDekho", "https://animedekho.app"),
        UniversalMovieScraper("animedubhindi", "Animedubhindi", "https://www.animedubhindi.cc"),
        UniversalMovieScraper("animekhor", "Animekhor", "https://animekhor.p2pstream.vip"),
        UniversalMovieScraper("animenosub", "Animenosub", "https://vtbe.to"),
        UniversalMovieScraper("animepahe", "AnimePahe", "https://pahe.win"),
        UniversalMovieScraper("animesalt", "Animesalt", "https://animesalt.ac"),
        UniversalMovieScraper("bilibili", "Bilibili", "https://www.bilibili.tv"),
        UniversalMovieScraper("castletv", "CastleTv", "https://api.hlowb.com"),
        UniversalMovieScraper("cinetv", "CineTv", "https://filmin.ajfysu.com"),
        UniversalMovieScraper("disneystudio", "DisneyStudio", "https://net52.cc"),
        UniversalMovieScraper("hotstarmirror", "HotStarMirror", "https://net52.cc"),
        UniversalMovieScraper("netflixmirror", "NetflixMirror", "https://net52.cc"),
        UniversalMovieScraper("primevideomirror", "PrimeVideoMirror", "https://net52.cc"),
        UniversalMovieScraper("cricify", "Cricify", "https://cfyhljddgbkkufh82.top"),
        UniversalMovieScraper("desiserials", "DesiSerials", "https://www.desi-serials.to"),
        UniversalMovieScraper("doflix", "DoFlix", "https://panel.watchkaroabhi.com"),
        UniversalMovieScraper("einthusan", "Einthusan", "https://einthusan.tv"),
        UniversalMovieScraper("hdrezka", "HDrezka", "https://rezka.ag"),
        UniversalMovieScraper("mlsbd", "MLSBD", "https://mlsbd.co"),
        UniversalMovieScraper("movieboxapi", "MovieBoxAPI", "https://api3.aoneroom.com"),
        UniversalMovieScraper("moviezwap", "Moviezwap", "https://www.moviezwap.surf"),
        UniversalMovieScraper("pikashow", "Pikashow", "https://manoda.co"),
        UniversalMovieScraper("radioindia", "RadioIndia", "https://www.radioindia.in"),
        UniversalMovieScraper("rtally", "Rtally", "https://www.rtally.shop"),
        UniversalMovieScraper("streamflix", "StreamFlix", "https://api.streamflix.app"),
        UniversalMovieScraper("tamildhool", "TamilDhool", "https://www.tamildhool.tech"),
        UniversalMovieScraper("tamilultra", "TamilUltra", "https://tamilultra.top"),
        UniversalMovieScraper("watch32", "Watch32", "https://watch32.sx"),
        UniversalMovieScraper("xon", "Xon", "https://xon-avens.xyz/apis"),

        // Music
        YTMusicScraper()
    )

    // ── Public API ────────────────────────────────────────────────────────────

    fun getAdultScrapers(): List<BaseScraper> =
        allScrapers.filter { it.info.categories.contains(ContentCategory.ADULT) }
            .sortedBy { it.info.displayName.lowercase() }

    fun getMovieScrapers(): List<BaseScraper> =
        allScrapers.filter { it.info.categories.any { c -> c == ContentCategory.MOVIES || c == ContentCategory.SHOWS } }
            .sortedBy { it.info.displayName.lowercase() }

    fun getMusicScrapers(): List<BaseScraper> =
        allScrapers.filter { it.info.categories.contains(ContentCategory.MUSIC) }
            .sortedBy { it.info.displayName.lowercase() }

    fun getScraperById(id: String): BaseScraper? =
        allScrapers.firstOrNull { it.info.id == id }

    fun getAllScrapers(): List<BaseScraper> = allScrapers

    fun getScrapersForProfile(profile: String): List<BaseScraper> = when (profile) {
        "ADULT"  -> getAdultScrapers()
        "MOVIES" -> getMovieScrapers()
        "SONGS"  -> getMusicScrapers()
        else     -> emptyList()
    }
}
