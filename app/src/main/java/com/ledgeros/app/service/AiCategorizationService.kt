package com.ledgeros.app.service

data class AiCategorizationPayload(
    val merchant: String,
    val amount: Double,
    val description: String,
)

data class AiCategorizationResult(
    val category: String,
    val confidence: Double,
    val summary: String,
)

class AiCategorizationService {
    fun categorize(payload: AiCategorizationPayload): AiCategorizationResult {
        val description = payload.description.lowercase()
        val category = when {
            "paper" in description || "office" in description -> "Office supplies"
            "fuel" in description || "uber" in description -> "Motor vehicle"
            "software" in description || "saas" in description -> "Software"
            else -> "Uncategorised"
        }

        return AiCategorizationResult(
            category = category,
            confidence = if (category == "Uncategorised") 0.35 else 0.72,
            summary = "Tiny payload categorized without resending OCR text.",
        )
    }

    companion object {
        const val SYSTEM_PROMPT =
            "You are an Australian business compliance assistant. " +
                "Return compact JSON only. Use Australian GST and BAS concepts. " +
                "Do not explain unless requested."
    }
}
