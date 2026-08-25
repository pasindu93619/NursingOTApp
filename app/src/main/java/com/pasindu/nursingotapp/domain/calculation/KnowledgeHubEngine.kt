package com.pasindu.nursingotapp.domain.calculation

import com.pasindu.nursingotapp.data.local.entity.CpdLogEntity

object KnowledgeHubEngine {

    /**
     * Tallies the total CPD points accumulated for the year.
     */
    fun calculateTotalCpdPoints(logs: List<CpdLogEntity>): Int {
        return logs.sumOf { it.earnedPoints }
    }

    /**
     * Evaluates a trivia mini-game score.
     */
    fun evaluateTriviaScore(correctAnswers: Int, totalQuestions: Int): String {
        if (totalQuestions <= 0) return "N/A"
        val percentage = (correctAnswers.toDouble() / totalQuestions.toDouble()) * 100
        return when {
            percentage >= 90 -> "Expert"
            percentage >= 75 -> "Proficient"
            percentage >= 50 -> "Competent"
            else -> "Needs Review"
        }
    }

    /**
     * Formats a raw MoH circular document string into a prompt optimized for the Gemini AI
     * to generate bite-sized clinical flashcards.
     */
    fun createFlashcardPrompt(circularText: String): String {
        return """
            You are a clinical nursing educator in Sri Lanka.
            Analyze the following Ministry of Health (MoH) circular/guideline and extract the most critical actionable points for ward nurses.
            Convert these points into 3-5 bite-sized interactive flashcards.
            Format each flashcard with a clear 'Question/Scenario' and an 'Answer/Action'.
            
            Document Text:
            $circularText
        """.trimIndent()
    }
}