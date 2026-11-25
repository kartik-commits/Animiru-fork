package eu.kanade.tachiyomi.ui.player.utils

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.animesource.model.ChapterType
import eu.kanade.tachiyomi.animesource.model.TimeStamp
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

@Inject
class AniSkipApi(private val json: Json) {
    private val client = OkHttpClient()

    // credits: https://github.com/saikou-app/saikou/blob/main/app/src/main/java/ani/saikou/others/AniSkip.kt
    fun getResult(malId: Int, episodeNumber: Int, episodeLength: Long): List<TimeStamp>? {
        fun query(len: Long): List<TimeStamp>? {
            val url =
                "https://api.aniskip.com/v2/skip-times/$malId/$episodeNumber?types[]=ed" +
                    "&types[]=mixed-ed&types[]=mixed-op&types[]=op&types[]=recap&episodeLength=$len"
            return try {
                val a = client.newCall(GET(url)).execute().body.string()
                val res = json.decodeFromString<AniSkipResponse>(a)
                if (res.found) {
                    res.results?.map {
                        TimeStamp(
                            start = it.interval.startTime,
                            end = it.interval.endTime,
                            name = it.skipType.getString(),
                            type = it.skipType.toChapterType(),
                        )
                    }
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
        return query(episodeLength) ?: if (episodeLength != 0L) query(0L) else null
    }

    // AM (ANISKIP) -->
    fun getMalIdFromTitle(title: String): Long? {
        val cleanTitle = title.replace(Regex("""\s*[\(\[].*?[\)\]]"""), "").trim()
        if (cleanTitle.isBlank()) return null
        val query = """
            query (${'$'}search: String) {
                Media (search: ${'$'}search, type: ANIME) {
                    idMal
                }
            }
        """.trimIndent()
        return fetchMalIdFromAniList(query, buildJsonObject { put("search", cleanTitle) })
    }
    // <-- AM (ANISKIP)

    fun getMalIdFromAL(id: Long): Long {
        val query = """
                query{
                Media(id:$id){idMal}
                }
        """.trimMargin()
        return fetchMalIdFromAniList(query) ?: 0L
    }

    private fun fetchMalIdFromAniList(query: String, variables: JsonObject? = null): Long? {
        val body = buildJsonObject {
            put("query", query)
            if (variables != null) put("variables", variables)
        }.toString()

        return try {
            client.newCall(
                POST(
                    "https://graphql.anilist.co",
                    body = body.toRequestBody(jsonMime),
                ),
            ).execute().use { response ->
                if (!response.isSuccessful) return null
                parseAniListMalId(json, response.body.string())
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        internal fun parseAniListMalId(json: Json, responseBody: String): Long? = try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            root["data"]
                ?.jsonObject
                ?.get("Media")
                ?.jsonObject
                ?.get("idMal")
                ?.jsonPrimitive
                ?.longOrNull
                ?.takeIf { it > 0L }
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
data class AniSkipResponse(
    val found: Boolean,
    val results: List<Stamp>?,
)

@Serializable
data class Stamp(
    val interval: AniSkipInterval,
    val skipType: SkipType,
)

@Serializable
enum class SkipType {
    @SerialName("op")
    OP,

    @SerialName("ed")
    ED,

    @SerialName("recap")
    RECAP,

    @SerialName("mixed-op")
    MIXED_OP, ;

    fun getString(): String {
        return when (this) {
            OP -> "Opening"
            ED -> "Ending"
            RECAP -> "Recap"
            MIXED_OP -> "Mixed-op"
        }
    }

    fun toChapterType(): ChapterType {
        return when (this) {
            OP -> ChapterType.Opening
            ED -> ChapterType.Ending
            RECAP -> ChapterType.Recap
            MIXED_OP -> ChapterType.MixedOp
        }
    }
}

@Serializable
data class AniSkipInterval(
    val startTime: Double,
    val endTime: Double,
)
