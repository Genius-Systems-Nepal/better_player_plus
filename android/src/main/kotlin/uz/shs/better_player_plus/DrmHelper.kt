package uz.shs.better_player_plus

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSpec
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap
import java.util.UUID

// Legacy betterplayer VuDRM/Widevine license flow for DASH streams.
internal class DrmHelper {
    fun buildDrmMediaSource(
        uri: Uri,
        context: Context,
        drmToken: String?,
        drmHeaders: Map<String, String>?,
        licenseUrl: String
    ): MediaSource {
        val drmSessionManager = DefaultDrmSessionManager.Builder().build(
            object : MediaDrmCallback {
                @Throws(MediaDrmCallbackException::class)
                override fun executeProvisionRequest(
                    uuid: UUID,
                    request: ExoMediaDrm.ProvisionRequest
                ): ByteArray {
                    val url = request.defaultUrl + "&signedRequest=" + String(request.data)
                    try {
                        return executePost(url)
                    } catch (e: IOException) {
                        throw drmCallbackException(url, e)
                    }
                }

                @Throws(MediaDrmCallbackException::class)
                override fun executeKeyRequest(
                    uuid: UUID,
                    request: ExoMediaDrm.KeyRequest
                ): ByteArray {
                    try {
                        return executePost(
                            request.data,
                            buildRequestHeaders(drmToken, drmHeaders),
                            licenseUrl
                        )
                    } catch (e: IOException) {
                        throw drmCallbackException(licenseUrl, e)
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
        requestHeaders: Map<String, String>,
        licenseUrl: String
    ): ByteArray {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(licenseUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            requestHeaders.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            connection.outputStream.use { out ->
                out.write(data)
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

    private fun buildRequestHeaders(
        drmToken: String?,
        drmHeaders: Map<String, String>?
    ): Map<String, String> {
        val requestHeaders = HashMap<String, String>()
        drmHeaders?.let { requestHeaders.putAll(it) }
        val normalizedToken = drmToken?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals("null", ignoreCase = true)
        }
        normalizedToken?.let { requestHeaders["nv-authorizations"] = it }
        return requestHeaders
    }

    private fun drmCallbackException(url: String, cause: Throwable): MediaDrmCallbackException {
        val dataSpec = DataSpec.Builder().setUri(url).build()
        return MediaDrmCallbackException(
            dataSpec,
            dataSpec.uri,
            emptyMap(),
            0,
            cause
        )
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
