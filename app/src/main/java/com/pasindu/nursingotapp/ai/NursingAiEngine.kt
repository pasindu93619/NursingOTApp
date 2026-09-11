package com.pasindu.nursingotapp.ai

import com.google.mlkit.genai.prompt.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.collect

sealed interface NursingAiAvailability {
    data object Available : NursingAiAvailability
    data object Downloadable : NursingAiAvailability
    data object Downloading : NursingAiAvailability
    data object Unavailable : NursingAiAvailability
    data class Error(val message: String) : NursingAiAvailability
}

class NursingAiEngine {
    private val model: GenerativeModel = Generation.getClient()

    suspend fun availability(): NursingAiAvailability = try {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> NursingAiAvailability.Available
            FeatureStatus.DOWNLOADABLE -> NursingAiAvailability.Downloadable
            FeatureStatus.DOWNLOADING -> NursingAiAvailability.Downloading
            FeatureStatus.UNAVAILABLE -> NursingAiAvailability.Unavailable
            else -> NursingAiAvailability.Unavailable
        }
    } catch (t: Throwable) {
        NursingAiAvailability.Error(t.message ?: "Unable to check on-device AI availability.")
    }

    suspend fun prepare(): NursingAiAvailability = try {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> NursingAiAvailability.Available
            FeatureStatus.DOWNLOADABLE -> {
                model.download().collect { }
                if (model.checkStatus() == FeatureStatus.AVAILABLE) {
                    NursingAiAvailability.Available
                } else {
                    NursingAiAvailability.Downloading
                }
            }
            FeatureStatus.DOWNLOADING -> NursingAiAvailability.Downloading
            FeatureStatus.UNAVAILABLE -> NursingAiAvailability.Unavailable
            else -> NursingAiAvailability.Unavailable
        }
    } catch (t: Throwable) {
        NursingAiAvailability.Error(t.message ?: "On-device AI could not be prepared.")
    }

    suspend fun ask(question: String, context: NursingAiContext): Result<String> = runCatching {
        require(question.isNotBlank()) { "Question cannot be empty." }

        val prompt = buildString {
            appendLine("You are NursingOS AI, a concise context-aware assistant inside a nursing productivity app.")
            appendLine("Answer only from the supplied current-screen context and general language understanding.")
            appendLine("Never invent a value, formula, score definition, policy, patient fact, diagnosis, prescription, or calculation.")
            appendLine("For numerical or clinical calculations, explain the authoritative calculation supplied in context; do not replace it with your own alternative calculation.")
            appendLine("If the context does not contain the answer, say that the current screen does not provide enough information and tell the user what to open/check next.")
            appendLine("Treat clinical safety rules in the context as higher priority than the user's request.")
            appendLine("Understand English, Sinhala script, and Sri Lankan Singlish written with Latin letters.")
            appendLine("Reply in the language/style used by the user. If the user writes Singlish, answer naturally in Singlish unless a Sinhala-script answer is clearly requested.")
            appendLine("Keep the answer short and practical. Use simple headings or bullets when helpful.")
            appendLine()
            appendLine(context.asPromptContext())
            appendLine()
            appendLine("USER QUESTION:")
            appendLine(question.trim())
        }

        val response = model.generateContent(prompt)
        response.candidates.firstOrNull()?.text?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("The on-device model returned no answer.")
    }
}
