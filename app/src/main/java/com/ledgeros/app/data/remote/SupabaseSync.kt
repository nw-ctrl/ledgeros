package com.ledgeros.app.data.remote

import android.content.Context
import android.net.Uri
import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.model.BasFrequency
import com.ledgeros.app.model.Business
import com.ledgeros.app.model.ComplianceStatus
import com.ledgeros.app.model.ComplianceTask
import com.ledgeros.app.model.ComplianceTaskType
import com.ledgeros.app.model.GrantedTier
import com.ledgeros.app.model.ManagedUser
import com.ledgeros.app.model.Receipt
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

// ── Serializable DTOs (match Supabase table columns exactly) ─────────────

@Serializable
data class BusinessDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val abn: String,
    @SerialName("business_name") val businessName: String,
    @SerialName("gst_registered") val gstRegistered: Boolean,
    @SerialName("bas_frequency") val basFrequency: String,
)

@Serializable
data class ReceiptDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("user_id") val userId: String,
    val merchant: String,
    @SerialName("receipt_date") val receiptDate: String,
    val total: Double,
    val gst: Double,
    val category: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("ocr_confidence") val ocrConfidence: Double,
    @SerialName("ai_confidence") val aiConfidence: Double,
)

@Serializable
data class BankTransactionDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("transaction_date") val transactionDate: String,
    val description: String,
    val amount: Double,
    val category: String,
    @SerialName("gst_estimate") val gstEstimate: Double,
    @SerialName("source_file") val sourceFile: String,
    @SerialName("matched_receipt_id") val matchedReceiptId: String?,
)

@Serializable
data class ComplianceTaskDto(
    val id: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("task_type") val taskType: String,
    @SerialName("due_date") val dueDate: String,
    val status: String,
)

// ── Sync operations ───────────────────────────────────────────────────────

/**
 * Pulls all data for the authenticated user from Supabase.
 * Returns null if Supabase is not configured or user is not authenticated.
 */
suspend fun pullFromSupabase(): SupabasePullResult? {
    val client = SupabaseClientProvider.client ?: return null
    val userId = client.auth.currentUserOrNull()?.id ?: return null

    return try {
        val businesses = client.from("businesses").select().decodeList<BusinessDto>()
        val receipts = client.from("receipts").select().decodeList<ReceiptDto>()
        val transactions = client.from("bank_transactions").select().decodeList<BankTransactionDto>()
        val tasks = client.from("compliance_tasks").select().decodeList<ComplianceTaskDto>()

        SupabasePullResult(
            business = businesses.firstOrNull()?.toModel(),
            receipts = receipts.map { it.toModel() },
            transactions = transactions.map { it.toModel() },
            complianceTasks = tasks.map { it.toModel() },
        )
    } catch (_: Exception) {
        null
    }
}

data class SupabasePullResult(
    val business: Business?,
    val receipts: List<Receipt>,
    val transactions: List<BankTransaction>,
    val complianceTasks: List<ComplianceTask>,
)

/** Upserts a single receipt — call after every local Room save. */
suspend fun pushReceiptToSupabase(receipt: Receipt) {
    val client = SupabaseClientProvider.client ?: return
    val userId = client.auth.currentUserOrNull()?.id ?: return
    runCatching {
        client.from("receipts").upsert(receipt.toDto(userId))
    }
}

/** Upserts a single bank transaction — call after every local Room save. */
suspend fun pushTransactionToSupabase(transaction: BankTransaction) {
    val client = SupabaseClientProvider.client ?: return
    val userId = client.auth.currentUserOrNull()?.id ?: return
    runCatching {
        client.from("bank_transactions").upsert(transaction.toDto(userId))
    }
}

/** Upserts the business profile — called on onboarding completion. */
suspend fun pushBusinessToSupabase(business: Business) {
    val client = SupabaseClientProvider.client ?: return
    val userId = client.auth.currentUserOrNull()?.id ?: return
    runCatching {
        client.from("businesses").upsert(business.toDto(userId))
    }
}

/** Deletes a receipt from Supabase. */
suspend fun deleteReceiptFromSupabase(receiptId: String) {
    val client = SupabaseClientProvider.client ?: return
    client.auth.currentUserOrNull() ?: return
    runCatching {
        client.from("receipts").delete { filter { eq("id", receiptId) } }
    }
}

/** Deletes a transaction from Supabase. */
suspend fun deleteTransactionFromSupabase(transactionId: String) {
    val client = SupabaseClientProvider.client ?: return
    client.auth.currentUserOrNull() ?: return
    runCatching {
        client.from("bank_transactions").delete { filter { eq("id", transactionId) } }
    }
}

// ── Model ↔ DTO mappers ───────────────────────────────────────────────────

private fun Business.toDto(userId: String) = BusinessDto(
    id = id,
    userId = userId,
    abn = abn,
    businessName = businessName,
    gstRegistered = gstRegistered,
    basFrequency = basFrequency.name,
)

private fun Receipt.toDto(userId: String) = ReceiptDto(
    id = id,
    businessId = businessId,
    userId = userId,
    merchant = merchant,
    receiptDate = receiptDate.toString(),
    total = total,
    gst = gst,
    category = category,
    fileUrl = fileUrl,
    ocrConfidence = ocrConfidence,
    aiConfidence = aiConfidence,
)

private fun BankTransaction.toDto(userId: String) = BankTransactionDto(
    id = id,
    businessId = businessId,
    userId = userId,
    transactionDate = transactionDate.toString(),
    description = description,
    amount = amount,
    category = category,
    gstEstimate = gstEstimate,
    sourceFile = sourceFile,
    matchedReceiptId = matchedReceiptId,
)

private fun BusinessDto.toModel() = Business(
    id = id,
    userId = userId,
    abn = abn,
    businessName = businessName,
    gstRegistered = gstRegistered,
    basFrequency = runCatching { BasFrequency.valueOf(basFrequency) }.getOrDefault(BasFrequency.Quarterly),
)

private fun ReceiptDto.toModel() = Receipt(
    id = id,
    businessId = businessId,
    fileUrl = fileUrl,
    merchant = merchant,
    receiptDate = runCatching { LocalDate.parse(receiptDate) }.getOrDefault(LocalDate.now()),
    total = total,
    gst = gst,
    category = category,
    ocrConfidence = ocrConfidence,
    aiConfidence = aiConfidence,
)

private fun BankTransactionDto.toModel() = BankTransaction(
    id = id,
    businessId = businessId,
    transactionDate = runCatching { LocalDate.parse(transactionDate) }.getOrDefault(LocalDate.now()),
    description = description,
    amount = amount,
    category = category,
    gstEstimate = gstEstimate,
    sourceFile = sourceFile,
    matchedReceiptId = matchedReceiptId,
)

private fun ComplianceTaskDto.toModel() = ComplianceTask(
    id = id,
    businessId = businessId,
    taskType = runCatching { ComplianceTaskType.valueOf(taskType) }.getOrDefault(ComplianceTaskType.Bas),
    dueDate = runCatching { LocalDate.parse(dueDate) }.getOrDefault(LocalDate.now()),
    status = runCatching { ComplianceStatus.valueOf(status) }.getOrDefault(ComplianceStatus.Todo),
)

// ── Managed user access grants ────────────────────────────────────────────

@Serializable
private data class ManagedUserDto(
    val id: String,                            // grantee email (lowercase)
    @SerialName("granted_by") val grantedBy: String,
    val tier: String,
    val note: String,
)

/**
 * Checks whether the current user has been granted a tier by the owner.
 * Called silently after every sign-in for non-owner accounts.
 * Returns null if no grant exists or Supabase is unreachable.
 */
suspend fun fetchGrantedTierForCurrentUser(): GrantedTier? {
    val client = SupabaseClientProvider.client ?: return null
    val email = client.auth.currentUserOrNull()?.email ?: return null
    return try {
        client.from("managed_users")
            .select { filter { eq("id", email.lowercase()) } }
            .decodeSingleOrNull<ManagedUserDto>()
            ?.let { runCatching { GrantedTier.valueOf(it.tier) }.getOrNull() }
    } catch (_: Exception) {
        null
    }
}

/** Owner: fetch all grants they have created. */
suspend fun fetchManagedUsers(): List<ManagedUser> {
    val client = SupabaseClientProvider.client ?: return emptyList()
    client.auth.currentUserOrNull() ?: return emptyList()
    return try {
        client.from("managed_users")
            .select()
            .decodeList<ManagedUserDto>()
            .map {
                ManagedUser(
                    email = it.id,
                    tier = runCatching { GrantedTier.valueOf(it.tier) }.getOrDefault(GrantedTier.Free),
                    note = it.note,
                )
            }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Owner: create or update a grant. */
suspend fun upsertManagedUser(user: ManagedUser) {
    val client = SupabaseClientProvider.client ?: return
    val ownerId = client.auth.currentUserOrNull()?.id ?: return
    runCatching {
        client.from("managed_users").upsert(
            ManagedUserDto(
                id = user.email.lowercase().trim(),
                grantedBy = ownerId,
                tier = user.tier.name,
                note = user.note,
            ),
        )
    }
}

/** Owner: revoke a grant by email. */
suspend fun deleteManagedUser(email: String) {
    val client = SupabaseClientProvider.client ?: return
    client.auth.currentUserOrNull() ?: return
    runCatching {
        client.from("managed_users").delete { filter { eq("id", email.lowercase()) } }
    }
}

// ── Storage ───────────────────────────────────────────────────────────────

/**
 * Uploads a receipt image to Supabase Storage (private "receipts" bucket).
 *
 * Returns the storage path on success (e.g. "{userId}/{receiptId}.jpg"), or null
 * if Supabase is not configured, the user is not authenticated, or the upload fails.
 * The caller should persist the returned path as [Receipt.fileUrl].
 */
suspend fun uploadReceiptImage(receiptId: String, imageUri: String, context: Context): String? {
    val client = SupabaseClientProvider.client ?: return null
    val userId = client.auth.currentUserOrNull()?.id ?: return null
    return try {
        val bytes = context.contentResolver
            .openInputStream(Uri.parse(imageUri))
            ?.use { it.readBytes() } ?: return null
        val path = "$userId/$receiptId.jpg"
        client.storage.from("receipts").upload(path, bytes) { upsert = true }
        path
    } catch (_: Exception) {
        null
    }
}
