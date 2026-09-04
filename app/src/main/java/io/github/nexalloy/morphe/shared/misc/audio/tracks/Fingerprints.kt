package io.github.nexalloy.morphe.shared.misc.audio.tracks

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.fingerprint
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string

internal val formatStreamModelToStringFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/String;")
    name("toString")
    strings(
        "isDefaultAudioTrack=",
        "audioTrackId="
    )
}

val formatStringModelClass = findClassDirect {
    formatStreamModelToStringFingerprint().declaredClass!!
}

val getFormatStreamModelGetter = findMethodListDirect {
    val formatStringModelClass = formatStringModelClass().name
    formatStreamModelToStringFingerprint().invokes.windowed(3).first {
        it[0].returnTypeName == "boolean" &&
            it[1].returnTypeName == "java.lang.String" &&
            it[2].returnTypeName == "java.lang.String"
    }.also {
        it.forEach { method ->
            require(method.paramCount == 0) { "Expected no parameters for FormatStreamModel getter methods" }
            require(method.declaredClassName == formatStringModelClass) { "Expected FormatStreamModel instance method" }
        }
    }
}

internal object AudioTrackRecordToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;",
            location = MatchAfterWithin(5)
        ),
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = "this",
            type = "Z",
            location = MatchAfterWithin(5)
        ),
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = "this",
            type = "Z",
            location = MatchAfterWithin(5)
        ),
        string("id;displayName;isAutoDubbed;isDefault")
    )
)

private object AudioTrackItemOnClickParentFingerprint : Fingerprint(
    parameters = emptyList(),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "audio_tracks_title"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/content/res/Resources;->getString(I)Ljava/lang/String;",
            location = MatchAfterWithin(3)
        )
    )
)

internal fun getAudioTrackItemOnClickFingerprint(audioTrackRecordClass: String) = Fingerprint(
    classFingerprint = AudioTrackItemOnClickParentFingerprint,
    name = "onItemClick",
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = audioTrackRecordClass,
            type = "Ljava/lang/String;"
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("Ljava/lang/String;"),
            location = MatchAfterWithin(3)
        )
    )
)

private object CurrentAudioVideoFormatToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("currentVideoFormat=")
)

internal fun getCurrentAudioFormatConstructorFingerprint(audioTrackRecordClass: String) = Fingerprint(
    classFingerprint = CurrentAudioVideoFormatToStringFingerprint,
    name = "<init>",
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "[$audioTrackRecordClass"
        )
    )
)

internal fun getSetVideoQualityListFingerprint(
    audioVideoFormatClass: String,
    playerControllerClass: String
) = Fingerprint(
    parameters = listOf(audioVideoFormatClass),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = audioVideoFormatClass,
            type = "[L"
        ),
        resourceLiteral(ResourceType.STRING, "quality_auto")
    ),
    custom = {
        declaredClass {
            fields {
                addForType(playerControllerClass)
            }
        }
    }
)

internal const val AUDIO_STREAM_IGNORE_DEFAULT_FEATURE_FLAG = 45666189L
