package com.example.core.voice

import android.util.Log
import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
data class GeminiGenerateContentRequest(val contents: List<GeminiContent>)

@Serializable
data class GeminiCandidate(val content: GeminiContent)

@Serializable
data class GeminiGenerateContentResponse(val candidates: List<GeminiCandidate>)

@Serializable
data class GeminiBookingResult(
    val clientName: String,
    val date: String,            // YYYY-MM-DD
    val time: String,            // HH:MM (24-hour style)
    val durationMinutes: Int,    // defaults to 60 if not specified
    val serviceId: String? = null,      // matched service ID if found in inventory
    val staffId: String? = null,        // matched staff ID if found in staff list
    val matchedServiceName: String? = null,
    val matchedStaffName: String? = null
)

class GeminiParser {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun parseBookingVoiceCommand(
        rawText: String,
        currentDateStr: String, // e.g. "2026-06-11"
        servicesJson: String,   // serialized list of available services
        staffJson: String       // serialized list of available staff and roles
    ): GeminiBookingResult? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiParser", "API Key is empty or placeholder!")
            return null
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val prompt = """
            You are an assistant parsing speech-to-text booking commands for a Service-Based Business app.
            
            Current Date is: $currentDateStr.
            
            User's Speech Input: "$rawText"
            The user says their booking details using this structure: [client name], [date/day], appointment, [time], [appointment length].
            Example: "rahul, 25 July 2026, 5 PM, two hours" or "rahul, tomorrow, 10 AM, one hour"
            If any parameter is missing or not provided, leave it empty or use intelligent defaults:
            - ClientName: Extract the client name (e.g. rahul)
            - Date: Parse it relative to Current Date ($currentDateStr) into YYYY-MM-DD format. E.g. "today" -> $currentDateStr, "tomorrow" -> day after $currentDateStr, "25 July 2026" -> "2026-07-25".
            - Time: Convert it to 24-hour HH:MM style format. E.g. "5 PM" -> "17:00", "10 AM" -> "10:00".
            - DurationMinutes: Extract appointment length (e.g., "two hours" -> 120, "one hour" -> 60, "3 hours" -> 180). Default value MUST be 60 if not provided.
            
            Based on the following Services database: 
            $servicesJson
            
            And the following Staff database:
            $staffJson
            
            Task:
            1. Extract the client name.
            2. Compute the correct date in YYYY-MM-DD.
            3. Compute the correct time in HH:MM.
            4. Compute the duration in minutes (integer, default 60).
            5. Search the Services database to find the closest match. Return the correct matched serviceId (String) and matchedServiceName (String). If no services match closely, return null.
            6. Search the Staff database to find the closest matched staff member name. Return matched staffId (String) and matchedStaffName (String). If no staff match closely, return null.
            
            Respond STRICTLY with a SINGLE valid JSON object. No markdown wrappers, no backticks, no other text.
            JSON Format Schema:
            {
              "clientName": "...",
              "date": "YYYY-MM-DD",
              "time": "HH:MM",
              "durationMinutes": 60,
              "serviceId": "...",
              "staffId": "...",
              "matchedServiceName": "...",
              "matchedStaffName": "..."
            }
        """.trimIndent()

        val requestPayload = GeminiGenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        val requestBodyString = json.encodeToString(GeminiGenerateContentRequest.serializer(), requestPayload)
        val body = requestBodyString.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiParser", "Request failed: ${response.code} ($respStr)")
                return null
            }

            val genResponse = json.decodeFromString(GeminiGenerateContentResponse.serializer(), respStr)
            val rawResponseText = genResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Clean up any potential markdown backticks
            val cleanJson = rawResponseText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            json.decodeFromString(GeminiBookingResult.serializer(), cleanJson)
        } catch (e: Exception) {
            Log.e("GeminiParser", "Error calling Gemini", e)
            null
        }
    }
}
