package com.streamix.scraper.cloudstream

import com.streamix.scraper.cloudstream.providers.*
import com.streamix.scraper.cloudstream.providers.gizlikeyif.*
import com.streamix.scraper.cloudstream.providers.cxxx.*

object ProviderRegistry {
    val providers = listOf(
        PornHatProvider(),
        OkxxxProvider(),
        ChaturbateProvider(),
        CollectionOfBestPornProvider(),
        FapixProvider(),
        HanimeTVProvider(),
        InfluencerChicksProvider(),
        LiveCamRipsProvider(),
        NetFapXProvider(),
        PerfectGirlsProvider(),
        HahoMoeProvider(),
        HStreamProvider(),
        ChatrubateProvider(),
        HentaiCityProvider(),
        FreePornVideosProvider(),
        XvideosProvider(),
        XnxxProvider(),
        XhamsterProvider(),
        YesPornPleaseProvider(),
        NoodleMagazineProvider(),
        InternetchicksProvider(),
        OnlyjerkProvider(),
        ParadisehillProvider(),
        PornobaeProvider(),
        PornhoarderProvider()
    )

    fun getProviderByName(name: String): MainAPI? {
        return providers.find { it.name.equals(name, ignoreCase = true) }
    }
}
