package com.ledgeros.app.data

import com.ledgeros.app.model.BasFrequency
import com.ledgeros.app.model.Business
import com.ledgeros.app.model.ComplianceStatus
import com.ledgeros.app.model.ComplianceTask
import com.ledgeros.app.model.ComplianceTaskType
import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.model.Receipt
import java.time.LocalDate

data class LedgerSnapshot(
    val activeBusiness: Business,
    val recentReceipts: List<Receipt>,
    val complianceTasks: List<ComplianceTask>,
    val bankTransactions: List<BankTransaction>,
)

interface LedgerRepository {
    val activeBusiness: Business
    val recentReceipts: List<Receipt>
    val complianceTasks: List<ComplianceTask>

    suspend fun loadSnapshot(): LedgerSnapshot {
        return LedgerSnapshot(activeBusiness, recentReceipts, complianceTasks, emptyList())
    }

    suspend fun saveReceipt(receipt: Receipt) = Unit

    suspend fun updateReceipt(receipt: Receipt) = Unit

    suspend fun saveBankTransactions(transactions: List<BankTransaction>) = Unit

    suspend fun updateBankTransaction(transaction: BankTransaction) = Unit

    suspend fun deleteReceipt(receiptId: String) = Unit

    suspend fun deleteTransaction(transactionId: String) = Unit

    suspend fun updateComplianceTaskStatus(taskId: String, status: ComplianceStatus) = Unit
}

class DemoLedgerRepository : LedgerRepository {
    override val activeBusiness = Business(
        id = "business-demo",
        userId = "user-demo",
        abn = "51824753556",
        businessName = "LedgerOS Operations",
        gstRegistered = true,
        basFrequency = BasFrequency.Quarterly,
    )

    override val recentReceipts = listOf(
        Receipt(
            id = "receipt-officeworks",
            businessId = activeBusiness.id,
            fileUrl = "",
            merchant = "Officeworks",
            receiptDate = LocalDate.of(2026, 5, 12),
            total = 42.50,
            gst = 3.86,
            category = "Office supplies",
            ocrConfidence = 0.91,
            aiConfidence = 0.78,
        ),
        Receipt(
            id = "receipt-canva",
            businessId = activeBusiness.id,
            fileUrl = "",
            merchant = "Canva",
            receiptDate = LocalDate.of(2026, 5, 8),
            total = 19.99,
            gst = 1.82,
            category = "Software",
            ocrConfidence = 0.86,
            aiConfidence = 0.82,
        ),
    )

    override val complianceTasks = listOf(
        ComplianceTask(
            id = "task-bas",
            businessId = activeBusiness.id,
            taskType = ComplianceTaskType.Bas,
            dueDate = LocalDate.of(2026, 7, 28),
            status = ComplianceStatus.Todo,
        ),
        ComplianceTask(
            id = "task-super",
            businessId = activeBusiness.id,
            taskType = ComplianceTaskType.Superannuation,
            dueDate = LocalDate.of(2026, 7, 28),
            status = ComplianceStatus.Pending,
        ),
    )
}
