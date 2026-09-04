package io.github.nexalloy.morphe.youtube.misc.imageurlhook

import io.github.nexalloy.patch
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.chromium.net.impl.CronetUrlRequest
import java.io.IOException
import java.lang.reflect.Field

val cronetImageUrlHookPatch = patch(
    description = "Hooks Cronet image urls.",
) {
    MessageDigestImageUrlFingerprint.hookMethod {
        before { param ->
            var url = param.args[0] as String? ?: return@before
            imageUrlHooks.forEach {
                url = it(url)
            }
            param.args[0] = url
        }
    }

    OnSucceededFingerprint.hookMethod {
        before { param ->
            imageUrlSuccessCallbackHook.forEach {
                it(param.args[0] as UrlRequest, param.args[1] as UrlResponseInfo)
            }
        }
    }

    OnFailureFingerprint.hookMethod {
        before { param ->
            val request = param.args[0] as UrlRequest
            val responseInfo = param.args[1] as? UrlResponseInfo
            val exception = param.args[2] as IOException
            imageUrlErrorCallbackHook.forEach {
                it(request, responseInfo, exception)
            }
        }
    }

    urlJField = ::urlField.field
}

private lateinit var urlJField: Field

fun getHookedUrl(o: CronetUrlRequest) = urlJField.get(o) as String

private var imageUrlHooks = listOf<(String) -> String>()
private var imageUrlSuccessCallbackHook = listOf<(UrlRequest, UrlResponseInfo) -> Unit>()
private var imageUrlErrorCallbackHook = listOf<(UrlRequest, UrlResponseInfo?, IOException) -> Unit>()

fun addImageUrlHook(f: (String) -> String, highPriority: Boolean = false) {
    imageUrlHooks = if (highPriority) listOf(f) + imageUrlHooks else imageUrlHooks + listOf(f)
}

fun addImageUrlSuccessCallbackHook(f: (UrlRequest, UrlResponseInfo) -> Unit) {
    imageUrlSuccessCallbackHook += listOf(f)
}

fun addImageUrlErrorCallbackHook(f: (UrlRequest, UrlResponseInfo?, IOException) -> Unit) {
    imageUrlErrorCallbackHook += listOf(f)
}
