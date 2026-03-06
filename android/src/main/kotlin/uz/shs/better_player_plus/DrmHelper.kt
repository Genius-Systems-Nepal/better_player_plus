package uz.shs.better_player_plus

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashChunkSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.drm.ExoMediaDrm
import androidx.media3.exoplayer.drm.MediaDrmCallback
import androidx.media3.exoplayer.drm.MediaDrmCallbackException
import androidx.media3.exoplayer.source.MediaSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

internal class DrmHelper {
    fun buildDrmMediaSource(
        uri: Uri,
        context: Context,
        drmToken: String,
        licenseUrl: String
    ): MediaSource {
        val drmSessionManager = DefaultDrmSessionManager.Builder().build(
            object : MediaDrmCallback {
                override fun executeProvisionRequest(
                    uuid: UUID,
                    request: ExoMediaDrm.ProvisionRequest
                ): ByteArray {
                    return try {
                        val url = request.defaultUrl + "&signedRequest=" + String(request.data)
                        executePost(url)
                    } catch (_: IOException) {
                        ByteArray(0)
                    }
                }

                override fun executeKeyRequest(
                    uuid: UUID,
                    request: ExoMediaDrm.KeyRequest
                ): ByteArray {
                    val requestProperties = mapOf(
                        "kid" to "",
                        "token" to drmToken
                    )
                    return try {
                        executePost(request.data, requestProperties, licenseUrl)
                    } catch (_: IOException) {
                        ByteArray(0)
                    }
                }
            }
        )

        drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, null)
        val drmSessionManagerProvider = DrmSessionManagerProvider { drmSessionManager }

        val dashChunkSourceFactory: DashChunkSource.Factory =
            DefaultDashChunkSource.Factory(DefaultHttpDataSource.Factory())
        val manifestDataSourceFactory = DefaultHttpDataSource.Factory()
        return DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)
            .createMediaSource(MediaItem.Builder().setUri(uri).build())
    }

    @Throws(IOException::class)
    private fun executePost(
        data: ByteArray,
        requestProperties: Map<String, String>,
        licenseUrl: String
    ): ByteArray {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(licenseUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            val drmInfo = JSONArray()
            for (value in data) {
                drmInfo.put(value.toInt() and 0xFF)
            }
            val payload = JSONObject().apply {
                put("token", requestProperties["token"])
                put("drm_info", drmInfo)
                put("kid", requestProperties["kid"])
            }.toString().toByteArray(StandardCharsets.UTF_8)

            connection.outputStream.use { out ->
                out.write(payload)
            }

            if (connection.responseCode >= 400) {
                throw IOException("DRM request failed with ${connection.responseCode}")
            }

            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
                return output.toByteArray()
            }
        } finally {
            connection?.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun executePost(url: String): ByteArray {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doInput = true
            if (connection.responseCode >= 400) {
                throw IOException("Provision request failed with ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
                return output.toByteArray()
            }
        } finally {
            connection?.disconnect()
        }
    }
}
