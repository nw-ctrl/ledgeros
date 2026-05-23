package com.ledgeros.app.service

interface LocalReceiptOcrService {
    suspend fun extractText(imageUri: String): String
}

class DemoLocalReceiptOcrService : LocalReceiptOcrService {
    override suspend fun extractText(imageUri: String): String {
        return "Officeworks ABN 36 004 763 526 Receipt OW-2048 Date 12/05/2026 " +
            "GST $3.86 Total $42.50 printer paper"
    }
}
