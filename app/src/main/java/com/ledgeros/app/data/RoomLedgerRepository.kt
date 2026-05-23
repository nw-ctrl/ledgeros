package com.ledgeros.app.data

import android.content.Context
import com.ledgeros.app.data.local.LedgerDatabase
import com.ledgeros.app.data.local.toEntity
import com.ledgeros.app.data.local.toModel
import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.model.ComplianceStatus
import com.ledgeros.app.model.Receipt

class RoomLedgerRepository(
    context: Context,
    private val seedRepository: LedgerRepository = DemoLedgerRepository(),
) : LedgerRepository {
    private val dao = LedgerDatabase.getInstance(context).ledgerDao()

    override val activeBusiness = seedRepository.activeBusiness
    override val recentReceipts = seedRepository.recentReceipts
    override val complianceTasks = seedRepository.complianceTasks

    override suspend fun loadSnapshot(): LedgerSnapshot {
        dao.seed(seedRepository.loadSnapshot())
        return LedgerSnapshot(
            activeBusiness = requireNotNull(dao.activeBusiness()).toModel(),
            recentReceipts = dao.receipts().map { it.toModel() },
            complianceTasks = dao.complianceTasks().map { it.toModel() },
            bankTransactions = dao.bankTransactions().map { it.toModel() },
        )
    }

    override suspend fun saveReceipt(receipt: Receipt) {
        dao.upsertReceipt(receipt.toEntity())
    }

    override suspend fun updateReceipt(receipt: Receipt) {
        dao.upsertReceipt(receipt.toEntity())
    }

    override suspend fun saveBankTransactions(transactions: List<BankTransaction>) {
        dao.upsertBankTransactions(transactions.map { it.toEntity() })
    }

    override suspend fun updateBankTransaction(transaction: BankTransaction) {
        dao.upsertBankTransaction(transaction.toEntity())
    }

    override suspend fun deleteReceipt(receiptId: String) {
        dao.deleteReceipt(receiptId)
    }

    override suspend fun deleteTransaction(transactionId: String) {
        dao.deleteTransaction(transactionId)
    }

    override suspend fun updateComplianceTaskStatus(taskId: String, status: ComplianceStatus) {
        val existing = dao.complianceTasks().firstOrNull { it.id == taskId } ?: return
        dao.upsertComplianceTask(existing.copy(status = status.name))
    }
}
