package io.github.nexalloy.morphe.youtube.video.codecs

import android.view.Display
import app.morphe.extension.youtube.patches.DisableVideoCodecsPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

val DisableVideoCodecs = patch(
    name = "Disable video codecs",
    description = "Adds options to disable or force HDR, and to disable VP9 codecs.",
) {
    PreferenceScreen.VIDEO.addPreferences(
        SwitchPreference("morphe_disable_hdr_video"),
        SwitchPreference("morphe_force_hdr_video", summary = true),
        SwitchPreference(
            key = "morphe_force_avc_codec",
            tag = app.morphe.extension.youtube.settings.preference.ForceAVCSwitchPreference::class.java
        )
    )

    val supportedHdrTypesMember =
        DexMethod($$"Landroid/view/Display$HdrCapabilities;->getSupportedHdrTypes()[I").toMember()

    // Match upstream's call-site replacement rather than globally overriding every platform
    // getSupportedHdrTypes() call in the process. The nested call made by
    // overrideSupportedHdrTypes() itself is left untouched so the extension can read the
    // original platform value.
    ::HDRCapabilityFingerprint.dexMethodList.forEach { outerMethod ->
        outerMethod.hookMethod(scopedHook(supportedHdrTypesMember) {
            after { innerParam ->
                if (innerDepth != 0) return@after
                innerParam.result = DisableVideoCodecsPatch.overrideSupportedHdrTypes(
                    innerParam.thisObject as Display.HdrCapabilities
                )
            }
        })
    }

    Vp9CapabilityFingerprint.hookMethod {
        before {
            if (!DisableVideoCodecsPatch.allowVP9()) {
                it.result = false
            }
        }
    }
}
