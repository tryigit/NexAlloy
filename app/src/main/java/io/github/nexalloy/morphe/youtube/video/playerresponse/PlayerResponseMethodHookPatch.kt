package io.github.nexalloy.morphe.youtube.video.playerresponse

import io.github.nexalloy.patch

private val beforeVideoIdHooks = linkedSetOf<Hook.ProtoBufferParameterBeforeVideoId>()
private val videoIdHooks = linkedSetOf<Hook.VideoId>()
private val playlistIdHooks = linkedSetOf<Hook.PlaylistId>()
private val afterVideoIdHooks = linkedSetOf<Hook.ProtoBufferParameter>()

fun addPlayerResponseMethodHook(hook: Hook<*>) {
    when (hook) {
        is Hook.ProtoBufferParameterBeforeVideoId -> beforeVideoIdHooks += hook
        is Hook.VideoId -> videoIdHooks += hook
        is Hook.PlaylistId -> playlistIdHooks += hook
        is Hook.ProtoBufferParameter -> afterVideoIdHooks += hook
    }
}

val PlayerResponseMethodHook = patch {
    val PARAMETER_VIDEO_ID = 0
    val PARAMETER_PROTO_BUFFER = 2
    val PARAMETER_PLAYLIST_ID = 3
    val parameterIsShortAndOpeningOrPlaying =
        ::playerParameterBuilderFingerprint.dexMethod.paramTypeNames
            .indexOfFirst { type -> type == "boolean" && ::playerParameterBuilderFingerprint.dexMethod.paramTypeNames.indexOf(type) >= 10 }

    require(parameterIsShortAndOpeningOrPlaying >= 0) {
        "Player parameter builder is missing the Shorts state boolean parameter"
    }

    ::playerParameterBuilderFingerprint.hookMethod {
        before { param ->
            val videoId = param.args[PARAMETER_VIDEO_ID] as String
            var protobuf = param.args[PARAMETER_PROTO_BUFFER] as String
            val playlistId = param.args[PARAMETER_PLAYLIST_ID] as String?
            val isShortAndOpeningOrPlaying =
                param.args[parameterIsShortAndOpeningOrPlaying] as Boolean

            beforeVideoIdHooks.forEach {
                protobuf = it(protobuf, videoId, isShortAndOpeningOrPlaying)
            }
            videoIdHooks.forEach {
                it(videoId, isShortAndOpeningOrPlaying)
            }
            playlistIdHooks.forEach {
                it(playlistId, isShortAndOpeningOrPlaying)
            }
            afterVideoIdHooks.forEach {
                protobuf = it(protobuf, videoId, isShortAndOpeningOrPlaying)
            }
            param.args[PARAMETER_PROTO_BUFFER] = protobuf
        }
    }
}

sealed class Hook<T> {
    class PlaylistId(val hook: (String?, Boolean) -> Unit) : Hook<(String?, Boolean) -> Unit>() {
        operator fun invoke(p1: String?, p2: Boolean) = hook(p1, p2)
    }

    class VideoId(val hook: (String, Boolean) -> Unit) : Hook<(String, Boolean) -> Unit>() {
        operator fun invoke(p1: String, p2: Boolean) = hook(p1, p2)
    }

    class ProtoBufferParameter(val hook: (String, String, Boolean) -> String) :
        Hook<(String, String, Boolean) -> String>() {
        operator fun invoke(p1: String, p2: String, p3: Boolean): String = hook(p1, p2, p3)
    }

    class ProtoBufferParameterBeforeVideoId(val hook: (String, String, Boolean) -> String) :
        Hook<(String, String, Boolean) -> String>() {
        operator fun invoke(p1: String, p2: String, p3: Boolean): String = hook(p1, p2, p3)
    }
}
