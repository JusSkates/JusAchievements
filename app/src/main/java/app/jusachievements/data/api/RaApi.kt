package app.jusachievements.data.api

import app.jusachievements.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import android.util.Log
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** RetroAchievements Web API. The `y` (API key) parameter is added by [ApiFactory]. */
interface RaApi {
    /** Same as [profile], but with an explicit key — used to validate credentials at sign-in. */
    @GET("API_GetUserProfile.php")
    suspend fun validate(@Query("u") user: String, @Query("y") apiKey: String): UserProfile

    @GET("API_GetUserProfile.php")
    suspend fun profile(@Query("u") user: String): UserProfile

    @GET("API_GetUserRecentlyPlayedGames.php")
    suspend fun recentlyPlayed(
        @Query("u") user: String,
        @Query("c") count: Int = 50,
        @Query("o") offset: Int = 0,
    ): List<RecentGame>

    @GET("API_GetUserCompletionProgress.php")
    suspend fun completionProgress(
        @Query("u") user: String,
        @Query("c") count: Int = 500,
        @Query("o") offset: Int = 0,
    ): CompletionPage

    @GET("API_GetUserWantToPlayList.php")
    suspend fun wantToPlay(
        @Query("u") user: String,
        @Query("c") count: Int = 500,
        @Query("o") offset: Int = 0,
    ): WantToPlayPage

    @GET("API_GetGameInfoAndUserProgress.php")
    suspend fun gameProgress(
        @Query("g") gameId: Int,
        @Query("u") user: String,
        @Query("a") includeAwardMetadata: Int = 1,
    ): GameProgress

    /** Comments on an achievement (`t=2`), newest first. Often contains tips on how to get it. */
    @GET("API_GetComments.php")
    suspend fun achievementComments(
        @Query("i") achievementId: Int,
        @Query("t") type: Int = 2,
        @Query("c") count: Int = 50,
        @Query("sort") sort: String = "-submitted",
    ): CommentsPage

    /** Users the signed-in account follows. */
    @GET("API_GetUsersIFollow.php")
    suspend fun usersIFollow(@Query("c") count: Int = 100, @Query("o") offset: Int = 0): FollowPage

    /** A user's unlocks in the last `m` minutes. */
    @GET("API_GetUserRecentAchievements.php")
    suspend fun recentUnlocks(@Query("u") user: String, @Query("m") minutes: Int): List<RecentUnlock>

    /** Site rank and related summary (no recent games / achievements). */
    @GET("API_GetUserSummary.php")
    suspend fun userSummary(
        @Query("u") user: String,
        @Query("g") recentGames: Int = 0,
        @Query("a") recentAchievements: Int = 0,
    ): UserSummary

    /** Everything a user unlocked between two unix timestamps. */
    @GET("API_GetAchievementsEarnedBetween.php")
    suspend fun earnedBetween(@Query("u") user: String, @Query("f") from: Long, @Query("t") to: Long): List<RecentUnlock>

    @GET("API_GetAchievementOfTheWeek.php")
    suspend fun achievementOfTheWeek(): AotwResponse

    /** All games for a console. `f=1` keeps only games that have achievements. */
    @GET("API_GetGameList.php")
    suspend fun gameList(
        @Query("i") consoleId: Int,
        @Query("f") onlyWithAchievements: Int = 1,
        @Query("h") includeHashes: Int = 0,
    ): List<GameListEntry>
}

/**
 * Keeps JA inside RetroAchievements' rate limits, which are counted per endpoint:
 *  - calls are spaced [SPACING_MS] apart;
 *  - when an endpoint answers 429 with Retry-After, that endpoint is left alone until the time
 *    is up (calls to it fail fast with the remaining wait, without touching the network);
 *  - only very short waits are retried in place.
 */
private class Throttle : Interceptor {
    private val lock = Any()
    private var nextSlot = 0L
    private val blockedUntil = HashMap<String, Long>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val endpoint = request.url.encodedPath.substringAfterLast('/') // never the query: it has the key
        var attempt = 0
        while (true) {
            val now = System.currentTimeMillis()
            val blockedFor = synchronized(lock) { (blockedUntil[endpoint] ?: 0L) - now }
            if (blockedFor > 0) return tooManyRequests(request, (blockedFor + 999) / 1000)

            val wait = synchronized(lock) {
                val at = maxOf(now, nextSlot)
                nextSlot = at + SPACING_MS
                at - now
            }
            if (wait > 0) Thread.sleep(wait)

            val response = chain.proceed(request)
            if (response.code != 429) return response

            val seconds = response.header("Retry-After")?.toLongOrNull()?.coerceIn(1, 600) ?: 30L
            Log.w("JaApi", "429 $endpoint: asked to wait ${seconds}s")
            if (seconds > SHORT_WAIT_S || attempt >= 1) {
                synchronized(lock) { blockedUntil[endpoint] = System.currentTimeMillis() + seconds * 1000 }
                return response
            }
            response.close()
            attempt++
            Thread.sleep(seconds * 1000)
        }
    }

    private fun tooManyRequests(request: Request, seconds: Long): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(429)
        .message("Too Many Requests")
        .header("Retry-After", seconds.toString())
        .body("".toResponseBody(null))
        .build()

    private companion object {
        const val SPACING_MS = 350L
        const val SHORT_WAIT_S = 4L
    }
}

object ApiFactory {
    private const val BASE_URL = "https://retroachievements.org/API/"
    private const val MEDIA_URL = "https://media.retroachievements.org"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    /** Shared client (also used by the image loader). Never carries the API key. */
    fun baseClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "JusAchievements/${BuildConfig.VERSION_NAME} (Android)")
                    .build()
            )
        }
        .build()

    fun api(base: OkHttpClient, apiKey: () -> String?): RaApi {
        val client = base.newBuilder()
            // RetroAchievements rate-limits bursts: few requests at a time, spaced out, with retry.
            .dispatcher(Dispatcher().apply { maxRequests = 3; maxRequestsPerHost = 3 })
            .addInterceptor(Throttle())
            .addInterceptor { chain ->
                val request = chain.request()
                val key = apiKey()
                val url = if (request.url.queryParameter("y") == null && key != null) {
                    request.url.newBuilder().addQueryParameter("y", key).build()
                } else {
                    request.url
                }
                chain.proceed(request.newBuilder().url(url).build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RaApi::class.java)
    }

    /** Turns a relative media path like `/Images/000001.png` into a full URL. */
    fun media(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path
        return MEDIA_URL + if (path.startsWith("/")) path else "/$path"
    }

    fun badge(badgeName: String, locked: Boolean = false): String =
        "$MEDIA_URL/Badge/$badgeName${if (locked) "_lock" else ""}.png"

    fun userPic(user: String): String = "$MEDIA_URL/UserPic/$user.png"
}
