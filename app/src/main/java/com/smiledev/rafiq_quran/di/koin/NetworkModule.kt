package com.smiledev.rafiq_quran.di.koin

import com.smiledev.rafiq_quran.data.remote.AladhanApi
import com.smiledev.rafiq_quran.data.remote.AladhanApiService
import com.smiledev.rafiq_quran.data.remote.EQuranApiService
import com.smiledev.rafiq_quran.data.remote.GithubReleaseApiService
import com.smiledev.rafiq_quran.data.remote.IslamicAppApiService
import com.smiledev.rafiq_quran.data.remote.MetalPriceApi
import com.smiledev.rafiq_quran.data.remote.MetalPriceApiService
import com.smiledev.rafiq_quran.data.remote.OverpassApi
import com.smiledev.rafiq_quran.data.remote.OverpassApiService
import com.smiledev.rafiq_quran.data.remote.ktor.KtorAladhanApiService
import com.smiledev.rafiq_quran.data.remote.ktor.KtorClientFactory
import com.smiledev.rafiq_quran.data.remote.ktor.KtorEQuranApiService
import com.smiledev.rafiq_quran.data.remote.ktor.KtorGithubReleaseApiService
import com.smiledev.rafiq_quran.data.remote.ktor.KtorIslamicAppApiService
import com.smiledev.rafiq_quran.data.remote.ktor.KtorMetalPriceApiService
import com.smiledev.rafiq_quran.data.remote.ktor.KtorOverpassApiService
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {
    single<Json> { KtorClientFactory.defaultJson }

    single<HttpClient> {
        KtorClientFactory.createDefaultHttpClient(timeoutMs = 30_000L)
    }

    single<HttpClient>(named("metalpriceClient")) {
        KtorClientFactory.createDefaultHttpClient(timeoutMs = 5_000L)
    }

    single<AladhanApiService> {
        KtorAladhanApiService(client = get())
    }

    single { AladhanApi(service = get()) }

    single<EQuranApiService> {
        KtorEQuranApiService(client = get())
    }

    single<GithubReleaseApiService> {
        KtorGithubReleaseApiService(client = get())
    }

    single<IslamicAppApiService> {
        KtorIslamicAppApiService(client = get())
    }

    single<MetalPriceApiService> {
        KtorMetalPriceApiService(client = get(named("metalpriceClient")))
    }

    single { MetalPriceApi(service = get()) }

    single<OverpassApi> {
        val mirrors = listOf(
            "https://overpass-api.de/api/",
            "https://overpass.kumi.systems/api/",
            "https://maps.mail.ru/osm/tools/overpass/api/"
        )
        val services: List<OverpassApiService> = mirrors.map { url ->
            KtorOverpassApiService(client = get(), baseUrl = url)
        }
        OverpassApi(services = services)
    }
}
