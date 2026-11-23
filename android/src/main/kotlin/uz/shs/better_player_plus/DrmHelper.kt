package uz.shs.better_player_plus

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashChunkSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.drm.DummyExoMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.drm.UnsupportedDrmException
import androidx.media3.exoplayer.source.MediaSource
import java.util.UUID

@OptIn(UnstableApi::class)
internal class DrmHelper {

    @Suppress("UNUSED_PARAMETER")
    fun buildDrmMediaSource(
        uri: Uri,
        context: Context,
        drmToken: String?,
        drmHeaders: Map<String, String>?,
        licenseUrl: String,
        mediaDataSourceFactory: DataSource.Factory? = null,
        onKeyExpired: (() -> Unit)? = null
    ): MediaSource {
        // Force every key/renewal request onto the app-supplied licenseUrl so
        // CustomData (and other query auth) is never dropped for PSSH default URLs.
        // License HTTP stays unwrapped (no CMCD on key fetches).
        val httpMediaDrmCallback =
            HttpMediaDrmCallback(
                licenseUrl,
                /* forceDefaultLicenseUrl= */ true,
                DefaultHttpDataSource.Factory()
            )
        drmHeaders?.forEach { (drmKey, drmValue) ->
            httpMediaDrmCallback.setKeyRequestProperty(drmKey, drmValue)
        }
        val normalizedToken = drmToken?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals("null", ignoreCase = true)
        }
        normalizedToken?.let {
            httpMediaDrmCallback.setKeyRequestProperty("nv-authorizations", it)
        }

        val drmSchemeUuid = Util.getDrmUuid("widevine")
        val defaultDrmSessionManager = if (drmSchemeUuid != null && Build.VERSION.SDK_INT >= 18) {
            DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(drmSchemeUuid) { uuid: UUID? ->
                    try {
                        // L3 + KEY_EXPIRED → silent session recreate (OEM cannot renew in-place).
                        KeyExpiredRemappingExoMediaDrm.createWidevineL3(uuid!!, onKeyExpired)
                    } catch (e: UnsupportedDrmException) {
                        Log.e(TAG, "Widevine unsupported", e)
                        DummyExoMediaDrm()
                    }
                }
                .setMultiSession(true)
                .build(httpMediaDrmCallback)
        } else {
            DefaultDrmSessionManager.Builder().build(httpMediaDrmCallback)
        }

        defaultDrmSessionManager.setMode(
            DefaultDrmSessionManager.MODE_PLAYBACK,
            null
        )

        val drmSessionManagerProvider = DrmSessionManagerProvider {
            defaultDrmSessionManager
        }

        return buildDashMediaSource(drmSessionManagerProvider, uri, mediaDataSourceFactory)
    }

    private fun buildDashMediaSource(
        drmSessionManager: DrmSessionManagerProvider,
        uri: Uri,
        mediaDataSourceFactory: DataSource.Factory?,
    ): DashMediaSource {
        // Optional CMCD-wrapped factory for manifest/segments; never used for license.
        val mediaFactory = mediaDataSourceFactory ?: DefaultHttpDataSource.Factory()
        val dashChunkSourceFactory: DashChunkSource.Factory =
            DefaultDashChunkSource.Factory(mediaFactory)
        return DashMediaSource.Factory(dashChunkSourceFactory, mediaFactory)
            .setDrmSessionManagerProvider(drmSessionManager)
            .createMediaSource(
                MediaItem.Builder()
                    .setUri(uri).build()
            )
    }

    companion object {
        private const val TAG = "DrmHelper"
    }
}
