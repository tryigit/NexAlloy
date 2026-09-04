package io.github.nexalloy.morphe.youtube.misc.contexthook

import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.patch

internal enum class ClientContextEndpoint {
    BROWSE,
    GET_WATCH,
    GUIDE,
    NEXT,
    PLAYER,
    REEL,
    SEARCH;

    val osNameHooks = mutableListOf<(String) -> String>()
}

internal fun addOSNameHook(
    endpoint: ClientContextEndpoint,
    hook: (String) -> String
) {
    endpoint.osNameHooks += hook
}

internal val ClientContextHook = patch {
    dependsOn(VersionCheck)

    val builderField = ::messageLiteBuilderField.field.apply { isAccessible = true }
    val clientField = ::clientInfoField.field.apply { isAccessible = true }
    val osField = ::osNameField.field.apply { isAccessible = true }
    val builderMethod = ::messageLiteBuilderMethod.method.apply { isAccessible = true }

    fun applyHooks(target: Any, endpoint: ClientContextEndpoint) {
        if (endpoint.osNameHooks.isEmpty()) return
        val builder = builderMethod.invoke(target) ?: return
        val contextBody = builderField.get(builder) ?: return
        val clientInfo = clientField.get(contextBody) ?: return
        var osName = osField.get(clientInfo) as? String ?: return
        endpoint.osNameHooks.forEach { hook ->
            osName = hook(osName)
        }
        osField.set(clientInfo, osName)
    }

    ::browseRequestBodyMethod.hookMethod {
        after { applyHooks(it.thisObject, ClientContextEndpoint.BROWSE) }
    }
    ::guideRequestBodyMethod.hookMethod {
        after { applyHooks(it.thisObject, ClientContextEndpoint.GUIDE) }
    }
    ::nextRequestBodyMethod.hookMethod {
        after { applyHooks(it.thisObject, ClientContextEndpoint.NEXT) }
    }
    ::playerRequestBodyMethod.hookMethod {
        after { applyHooks(it.thisObject, ClientContextEndpoint.PLAYER) }
    }
    ::searchRequestBodyMethod.hookMethod {
        after { applyHooks(it.thisObject, ClientContextEndpoint.SEARCH) }
    }
    ::getWatchRequestBodyMethods.dexMethodList.forEach { method ->
        method.hookMethod {
            after { applyHooks(it.thisObject, ClientContextEndpoint.GET_WATCH) }
        }
    }
    ::reelRequestBodyMethods.dexMethodList.forEach { method ->
        method.hookMethod {
            after { applyHooks(it.thisObject, ClientContextEndpoint.REEL) }
        }
    }
}
