package mok.it.tortura.feature

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json

private external fun torturaReadClipboardText(
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
)

private external fun torturaPickRosterFile(
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
)

private object WebTeamCompositionPlatformBridge : TeamCompositionPlatformBridge {
    private val json = Json { ignoreUnknownKeys = true }

    override val supportsClipboardRead: Boolean = true
    override val supportsFileImport: Boolean = true

    override suspend fun readClipboardText(): Result<String> = suspendCancellableCoroutine { continuation ->
        torturaReadClipboardText(
            onSuccess = { text -> continuation.resume(Result.success(text)) },
            onError = { message -> continuation.resume(Result.failure(IllegalStateException(message))) },
        )
    }

    override suspend fun pickRosterFile(): Result<ImportedRosterPayload> = suspendCancellableCoroutine { continuation ->
        torturaPickRosterFile(
            onSuccess = { payload ->
                continuation.resume(runCatching {
                    json.decodeFromString<ImportedRosterPayload>(payload)
                })
            },
            onError = { message -> continuation.resume(Result.failure(IllegalStateException(message))) },
        )
    }
}

actual fun teamCompositionPlatformBridge(): TeamCompositionPlatformBridge =
    WebTeamCompositionPlatformBridge
