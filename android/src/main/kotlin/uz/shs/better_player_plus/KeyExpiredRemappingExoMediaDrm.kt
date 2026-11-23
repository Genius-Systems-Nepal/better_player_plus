package uz.shs.better_player_plus

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.ExoMediaDrm
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.UnsupportedDrmException
import java.util.UUID

/**
 * Wraps [FrameworkMediaDrm] to handle OEM [ExoMediaDrm.EVENT_KEY_EXPIRED].
 *
 * Media3 renews on KEY_REQUIRED. Remapping KEY_EXPIRED → KEY_REQUIRED fails on some OEM CDMs
 * (`getKeyRequest` → ERROR_DRM_UNKNOWN) once the session is already expired. For those devices
 * we invoke [onKeyExpired] so the player can open a fresh DRM session silently.
 */
@OptIn(UnstableApi::class)
class KeyExpiredRemappingExoMediaDrm(
    private val delegate: FrameworkMediaDrm,
    private val onKeyExpired: (() -> Unit)?
) : ExoMediaDrm by delegate {

    override fun setOnEventListener(listener: ExoMediaDrm.OnEventListener?) {
        if (listener == null) {
            delegate.setOnEventListener(null)
            return
        }
        delegate.setOnEventListener { _, sessionId, event, extra, data ->
            if (event == ExoMediaDrm.EVENT_KEY_EXPIRED) {
                Log.i(TAG, "EVENT_KEY_EXPIRED — requesting silent DRM session recreate")
                onKeyExpired?.invoke()
                // Do not forward KEY_REQUIRED: expired OEM sessions often reject getKeyRequest.
                return@setOnEventListener
            }
            listener.onEvent(this, sessionId, event, extra, data)
        }
    }

    companion object {
        private const val TAG = "KeyExpiredRemapDrm"

        @Throws(UnsupportedDrmException::class)
        fun createWidevineL3(uuid: UUID, onKeyExpired: (() -> Unit)? = null): ExoMediaDrm {
            val framework = FrameworkMediaDrm.newInstance(uuid)
            framework.setPropertyString("securityLevel", "L3")
            return KeyExpiredRemappingExoMediaDrm(framework, onKeyExpired)
        }
    }
}
