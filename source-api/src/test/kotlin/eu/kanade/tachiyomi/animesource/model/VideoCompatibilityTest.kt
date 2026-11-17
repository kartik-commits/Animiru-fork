package eu.kanade.tachiyomi.animesource.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Headers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// AM (MEDIA3_DRM_CONTRACT) -->
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class VideoCompatibilityTest {
    private val drm = VideoDrm(
        scheme = VideoDrmScheme.WIDEVINE,
        licenseUrl = "https://license.invalid/secret-token",
        licenseHeaders = Headers.Builder()
            .add("Authorization", "secret-bearer")
            .add("X-Duplicate", "first")
            .add("X-Duplicate", "second")
            .build(),
    )
    private val memo = JsonObject(mapOf("fixture" to JsonPrimitive("preserve")))

    @Test
    fun `v14 v16 and v17 constructor and copy descriptors remain available`() {
        val videoClass = Video::class.java
        val marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")
        val jsonObject = JsonObject::class.java
        val headers = Headers::class.java
        val list = List::class.java
        val string = String::class.java
        val integer = Integer::class.java

        listOf(
            arrayOf(string, string, string, headers, list, list),
            arrayOf(string, string, string, headers, list, list, Int::class.javaPrimitiveType, marker),
            arrayOf(string, string, string, android.net.Uri::class.java, headers),
            arrayOf(string, string, string, android.net.Uri::class.java, headers, Int::class.javaPrimitiveType, marker),
            arrayOf(
                string, string, integer, integer, headers, Boolean::class.javaPrimitiveType,
                list, list, list, list, list, list, string, Boolean::class.javaPrimitiveType,
            ),
            arrayOf(
                string, string, integer, integer, headers, Boolean::class.javaPrimitiveType,
                list, list, list, list, list, list, string, Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, marker,
            ),
            arrayOf(
                string, string, integer, integer, headers, Boolean::class.javaPrimitiveType,
                list, list, list, list, list, list, string, Boolean::class.javaPrimitiveType, jsonObject,
            ),
            arrayOf(
                string, string, integer, integer, headers, Boolean::class.javaPrimitiveType,
                list, list, list, list, list, list, string, Boolean::class.javaPrimitiveType, jsonObject,
                Int::class.javaPrimitiveType, marker,
            ),
        ).forEach { parameters ->
            assertNotNull(videoClass.getDeclaredConstructor(*parameters))
        }

        val copyDefaultSignatures = listOf(
            arrayOf(
                videoClass, string, string, string, headers, list, list,
                Int::class.javaPrimitiveType, Any::class.java,
            ),
            arrayOf(
                videoClass, string, string, integer, integer, headers, Boolean::class.javaPrimitiveType,
                list, list, list, list, list, list, string, Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, Any::class.java,
            ),
            arrayOf(
                videoClass, string, string, integer, integer, headers, Boolean::class.javaPrimitiveType,
                list, list, list, list, list, list, string, Boolean::class.javaPrimitiveType, jsonObject,
                Int::class.javaPrimitiveType, Any::class.java,
            ),
        )
        copyDefaultSignatures.forEach { parameters ->
            assertNotNull(videoClass.getDeclaredMethod("copy\$default", *parameters))
        }
    }

    @Test
    fun `old copy default bridges preserve memo and drm`() {
        val original = Video(videoUrl = "https://media.invalid/video.mpd", memo = memo, drm = drm)
        val methods = Video::class.java.declaredMethods.filter { it.name == "copy\$default" }

        val copiedV14 = methods.single { it.parameterCount == 9 }.invoke(
            null,
            original,
            null,
            null,
            null,
            null,
            null,
            null,
            0x3f,
            null,
        ) as Video
        val copiedV16 = methods.single { it.parameterCount == 17 }.invoke(
            null,
            original,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            0x3fff,
            null,
        ) as Video
        val copiedV17 = methods.single { it.parameterCount == 18 }.invoke(
            null,
            original,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            0x7fff,
            null,
        ) as Video

        listOf(copiedV14, copiedV16, copiedV17).forEach { copied ->
            assertEquals(memo, copied.memo)
            assertSame(drm, copied.drm)
        }
    }

    @Test
    fun `DRM survives video JSON and copy paths`() {
        val video = Video(
            videoUrl = "https://media.invalid/video.mpd",
            subtitleTracks = listOf(Track("http://localhost:1/subtitle.vtt", "en")),
            memo = memo,
            drm = drm,
        )
        val encoded = with(SerializableVideo) { listOf(video).serialize() }
        val decoded = with(SerializableVideo) { encoded.toVideoList().single() }

        assertEquals(VideoDrmScheme.WIDEVINE, decoded.drm?.scheme)
        assertEquals("", decoded.drm?.licenseUrl)
        assertNull(decoded.drm?.licenseHeaders)
        assertFalse(encoded.contains("secret-token"))
        assertFalse(encoded.contains("secret-bearer"))
        assertFalse(decoded.initialized)
        assertEquals(memo, decoded.memo)
        assertSame(drm, video.copy(videoUrl = "https://media.invalid/other.mpd").drm)
        assertSame(drm, video.copyHttpServer(1234).drm)
    }

    @Test
    fun `old JSON defaults DRM to null and diagnostics redact secrets`() {
        val oldJson = """[{"videoUrl":"https://media.invalid/clear.mpd","videoTitle":"Clear"}]"""
        val decoded = with(SerializableVideo) { oldJson.toVideoList().single() }
        assertNull(decoded.drm)

        val diagnostic = drm.toString()
        assertTrue(diagnostic.contains("WIDEVINE"))
        assertFalse(diagnostic.contains("secret-token"))
        assertFalse(diagnostic.contains("secret-bearer"))
    }
}
// <-- AM (MEDIA3_DRM_CONTRACT)
