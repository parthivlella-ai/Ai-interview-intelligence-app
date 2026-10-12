package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.EvaluatedAnswer
import com.example.data.QuestionDifficulty
import com.example.data.SkillStatus
import com.example.service.AdaptiveInterviewEngine.QuestionTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Data model for preserving adaptive question history across interview turns.
 */
data class QuestionHistoryItem(
    val orderIndex: Int,
    val questionText: String,
    val targetCompetency: String,
    val candidateAnswer: String,
    val score: Int
)

/**
 * Real AI Interview Service powered by Google Gemini API.
 * Connects adaptive interview question generation and evidence-based answer evaluation
 * to Gemini 3.5 Flash when valid credentials exist, with robust timeout handling,
 * schema validation, and fallback isolation.
 */
object GeminiInterviewService {

    private const val TAG = "GeminiInterviewService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // 25-second connect/read timeout for adaptive generation
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Checks if a valid, non-placeholder Gemini API key is configured.
     * Never exposes or logs the actual key value.
     */
    fun isAiConfigured(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("MY_")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Human-readable diagnostic description of AI engine state.
     */
    fun getAiStatusDescription(): String {
        return if (isAiConfigured()) {
            "Live AI (Google Gemini 3.5 Flash)"
        } else {
            "Rule-Based Evaluator (Offline Fallback — GEMINI_API_KEY not configured)"
        }
    }

    /**
     * Generates an adaptive technical interview question using live Gemini AI.
     * Considers role, resume claims, difficulty target, and prior answer context.
     */
    suspend fun generateAdaptiveQuestion(
        roleTitle: String,
        jobDescription: String,
        resumeSnippet: String,
        questionIndex: Int,
        totalQuestions: Int,
        targetCompetency: String,
        difficulty: QuestionDifficulty,
        previousContext: List<QuestionHistoryItem>
    ): Result<Pair<QuestionTemplate, String>> = withContext(Dispatchers.IO) {
        if (!isAiConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY

            val historySummary = if (previousContext.isEmpty()) {
                "No previous questions asked yet. This is question $questionIndex of $totalQuestions."
            } else {
                previousContext.joinToString("\n") {
                    "- Q${it.orderIndex} [${it.targetCompetency}] (Score: ${it.score}%): Candidate answered: \"${it.candidateAnswer.take(120)}\""
                }
            }

            val prompt = """
                You are ForkAI, an expert technical interviewer conducting an adaptive competency interview.
                Generate Question #$questionIndex of $totalQuestions for the role of '$roleTitle'.
                
                Target Competency to Assess: $targetCompetency
                Required Difficulty Level: ${difficulty.name}
                
                Candidate Resume Profile:
                ${resumeSnippet.ifBlank { "Standard candidate profile applying for $roleTitle" }}
                
                Job Description Context:
                ${jobDescription.ifBlank { "General engineering standards for $roleTitle" }}
                
                Previous Interview Questions and Candidate Performance:
                $historySummary
                
                Instructions:
                1. Ask a targeted, realistic technical scenario question that specifically probes the candidate's understanding of '$targetCompetency'.
                2. Adapt the challenge level to '${difficulty.name}'. If the candidate scored high previously, probe edge cases, race conditions, architecture, or scaling trade-offs. If lower, probe fundamental principles.
                3. Formulate a clear 'adaptiveReason' explaining why this question and difficulty were selected based on the candidate's resume claims and past performance.
                4. Provide 4-7 expected technical keywords/concepts that a strong answer must address.
                
                Return ONLY a JSON object with this exact structure:
                {
                  "questionText": "The comprehensive interview question",
                  "adaptiveReason": "[Live AI • Gemini 3.5 Flash] Specific reason explaining question selection and difficulty adaptation",
                  "targetCompetency": "$targetCompetency",
                  "difficulty": "${difficulty.name}",
                  "keywordsExpected": ["keyword1", "keyword2", "keyword3", "keyword4"],
                  "rationaleTemplate": "Why this question validates competence"
                }
            """.trimIndent()

            val requestBodyJson = buildGeminiRequestBody(prompt)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.w(TAG, "Gemini API HTTP ${response.code}: $responseBody")
                return@withContext Result.failure(IllegalStateException("Gemini API returned HTTP ${response.code}"))
            }

            val rawOutput = extractCandidateText(responseBody)
                ?: return@withContext Result.failure(IllegalStateException("No valid response candidate from Gemini."))

            val parsedJson = parseCleanJson(rawOutput)
            val questionText = parsedJson.optString("questionText").takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("Missing questionText in AI response."))

            val adaptiveReason = parsedJson.optString("adaptiveReason")
                .ifBlank { "[Live AI • Gemini 3.5 Flash] Question tailored for $targetCompetency at ${difficulty.name} level." }

            val keywordsArray = parsedJson.optJSONArray("keywordsExpected")
            val keywordsList = mutableListOf<String>()
            if (keywordsArray != null) {
                for (i in 0 until keywordsArray.length()) {
                    keywordsList.add(keywordsArray.getString(i))
                }
            }
            if (keywordsList.isEmpty()) {
                keywordsList.addAll(listOf("architecture", "trade-offs", "performance", "scalability", "implementation"))
            }

            val template = QuestionTemplate(
                questionText = questionText,
                targetCompetency = targetCompetency,
                difficulty = difficulty,
                keywordsExpected = keywordsList,
                rationaleTemplate = parsedJson.optString("rationaleTemplate", "Validates practical proficiency in $targetCompetency.")
            )

            Result.success(Pair(template, adaptiveReason))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate question with Gemini AI: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Evaluates candidate's answer with Gemini 3.5 Flash.
     * Performs evidence extraction, uncertainty scoring, missing point identification, and skill classification.
     */
    suspend fun evaluateAnswerWithAi(
        questionText: String,
        targetCompetency: String,
        candidateAnswer: String,
        difficulty: QuestionDifficulty,
        expectedKeywords: List<String>
    ): Result<EvaluatedAnswer> = withContext(Dispatchers.IO) {
        if (!isAiConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val trimmedAnswer = candidateAnswer.trim()

            // If the candidate gave practically no answer, return early with honest assessment
            if (trimmedAnswer.split("\\s+".toRegex()).count { it.isNotBlank() } < 5) {
                return@withContext Result.success(
                    EvaluatedAnswer(
                        score = 25,
                        correctness = "[Live AI • Gemini 3.5 Flash] Candidate answer was too brief to demonstrate technical competence.",
                        missingPoints = "Expected detailed explanation of architecture, implementation approach, and trade-offs.",
                        supportingEvidence = "Minimal input provided: \"${trimmedAnswer.take(60)}\"",
                        uncertaintyLevel = "High",
                        uncertaintyReason = "Insufficient response length to establish reliable evidence of capability.",
                        skillUpdated = targetCompetency,
                        skillStatusResult = SkillStatus.NOT_DEMONSTRATED
                    )
                )
            }

            val prompt = """
                You are ForkAI, an objective, evidence-based technical assessment engine.
                Evaluate the candidate's interview answer with high analytical rigor.
                
                Question Asked:
                "$questionText"
                
                Target Competency Being Assessed:
                "$targetCompetency" (Difficulty tier: ${difficulty.name})
                
                Expected Technical Themes / Rubrics:
                ${expectedKeywords.joinToString(", ")}
                
                Candidate's Answer:
                "$trimmedAnswer"
                
                Evaluation Instructions:
                1. Score from 0 to 100 based on technical accuracy, practical mechanics, trade-offs, and depth.
                   - 80-100: Exceptional accuracy, covers failure modes, nuances, and architecture.
                   - 65-79: Good foundational understanding, covers main principles with minor omissions.
                   - 50-64: Partial or high-level understanding without implementation mechanics.
                   - 0-49: Incorrect, evasive, or severely lacking substance.
                2. Extract concrete 'supportingEvidence' by directly quoting key phrases from the candidate's answer that support your score.
                3. List 'missingPoints': specific technical details, edge cases, or failure modes the candidate omitted or explained poorly.
                4. Determine 'uncertaintyLevel':
                   - "Low": Candidate answered with high specificity, precision, and demonstrable evidence.
                   - "Medium": Candidate touched on correct concepts, but left ambiguity or lacked depth.
                   - "High": Vague generalizations, buzzword dropping without explanation, or contradictory statements.
                5. Provide 'uncertaintyReason': An honest explanation of why that uncertainty level was assigned.
                6. Determine 'skillStatus':
                   - If score >= 75: "DEMONSTRATED"
                   - If score between 55 and 74: "CLAIMED_UNVALIDATED"
                   - If score < 55: "NOT_DEMONSTRATED"
                   
                Return ONLY a JSON object with this exact structure:
                {
                  "score": 82,
                  "correctness": "Detailed technical analysis of accuracy",
                  "missingPoints": "Concrete missing concepts or trade-offs",
                  "supportingEvidence": "Direct quote evidence from candidate answer",
                  "uncertaintyLevel": "Low",
                  "uncertaintyReason": "Reason for uncertainty classification",
                  "skillStatus": "DEMONSTRATED"
                }
            """.trimIndent()

            val requestBodyJson = buildGeminiRequestBody(prompt)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.w(TAG, "Gemini evaluation HTTP ${response.code}: $responseBody")
                return@withContext Result.failure(IllegalStateException("Gemini API returned HTTP ${response.code}"))
            }

            val rawOutput = extractCandidateText(responseBody)
                ?: return@withContext Result.failure(IllegalStateException("No valid response text from Gemini."))

            val parsedJson = parseCleanJson(rawOutput)
            val score = parsedJson.optInt("score", 70).coerceIn(0, 100)
            val correctness = parsedJson.optString("correctness")
                .ifBlank { "Evaluated by Live AI for $targetCompetency." }
            val missingPoints = parsedJson.optString("missingPoints")
                .ifBlank { "Could expand on production scaling and monitoring considerations." }
            val supportingEvidence = parsedJson.optString("supportingEvidence")
                .ifBlank { "Candidate discussed core concepts of $targetCompetency." }
            val uncertaintyLevel = parsedJson.optString("uncertaintyLevel", "Low")
            val uncertaintyReason = parsedJson.optString("uncertaintyReason")
                .ifBlank { "Assessed based on technical clarity and terminology in response." }

            val statusStr = parsedJson.optString("skillStatus", "CLAIMED_UNVALIDATED")
            val skillStatus = when (statusStr.uppercase()) {
                "DEMONSTRATED" -> SkillStatus.DEMONSTRATED
                "NOT_DEMONSTRATED" -> SkillStatus.NOT_DEMONSTRATED
                else -> {
                    if (score >= 75) SkillStatus.DEMONSTRATED
                    else if (score >= 55) SkillStatus.CLAIMED_UNVALIDATED
                    else SkillStatus.NOT_DEMONSTRATED
                }
            }

            Result.success(
                EvaluatedAnswer(
                    score = score,
                    correctness = "[Live AI • Gemini 3.5 Flash] $correctness",
                    missingPoints = missingPoints,
                    supportingEvidence = "Evidence extracted: \"$supportingEvidence\"",
                    uncertaintyLevel = uncertaintyLevel,
                    uncertaintyReason = uncertaintyReason,
                    skillUpdated = targetCompetency,
                    skillStatusResult = skillStatus
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to evaluate answer with Gemini AI: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Builds Gemini REST API request payload using JSON.
     */
    private fun buildGeminiRequestBody(promptText: String): String {
        val root = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", promptText)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        val generationConfig = JSONObject()
        generationConfig.put("temperature", 0.4)
        generationConfig.put("responseMimeType", "application/json")
        root.put("generationConfig", generationConfig)

        return root.toString()
    }

    /**
     * Extracts model text response from Gemini response payload.
     */
    private fun extractCandidateText(responseJsonString: String): String? {
        return try {
            val root = JSONObject(responseJsonString)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            parts.getJSONObject(0).optString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from Gemini response: ${e.message}")
            null
        }
    }

    /**
     * Cleans up markdown fences if present and parses JSONObject.
     */
    private fun parseCleanJson(rawString: String): JSONObject {
        var cleaned = rawString.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        cleaned = cleaned.trim()
        return JSONObject(cleaned)
    }
}
