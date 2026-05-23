package com.ledgeros.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ledgeros.app.model.BankTransaction
import com.ledgeros.app.model.BasFrequency
import com.ledgeros.app.model.Business
import com.ledgeros.app.model.ComplianceStatus
import com.ledgeros.app.model.ComplianceTask
import com.ledgeros.app.model.ComplianceTaskType
import com.ledgeros.app.model.Receipt
import java.time.LocalDate

@Database(
    entities = [
        BusinessEntity::class,
        ReceiptEntity::class,
        BankTransactionEntity::class,
        ComplianceTaskEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var instance: LedgerDatabase? = null

        fun getInstance(context: Context): LedgerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledgeros.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_transactions ADD COLUMN matchedReceiptId TEXT")
            }
        }
    }
}

@Dao
interface LedgerDao {
    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun businessCount(): Int

    @Query("SELECT * FROM businesses LIMIT 1")
    suspend fun activeBusiness(): BusinessEntity?

    @Query("SELECT * FROM receipts ORDER BY receiptDate DESC")
    suspend fun receipts(): List<ReceiptEntity>

    @Query("SELECT * FROM bank_transactions ORDER BY transactionDate DESC")
    suspend fun bankTransactions(): List<BankTransactionEntity>

    @Query("SELECT * FROM compliance_tasks ORDER BY dueDate ASC")
    suspend fun complianceTasks(): List<ComplianceTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBusiness(entity: BusinessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReceipts(entities: List<ReceiptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReceipt(entity: ReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBankTransactions(entities: List<BankTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBankTransaction(entity: BankTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComplianceTasks(entities: List<ComplianceTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComplianceTask(entity: ComplianceTaskEntity)

    @Query("DELETE FROM receipts WHERE id = :receiptId")
    suspend fun deleteReceipt(receiptId: String)

    @Query("DELETE FROM bank_transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    @Transaction
    suspend fun seed(snapshot: com.ledgeros.app.data.LedgerSnapshot) {
        if (businessCount() > 0) return
        upsertBusiness(snapshot.activeBusiness.toEntity())
        upsertReceipts(snapshot.recentReceipts.map { it.toEntity() })
        upsertComplianceTasks(snapshot.complianceTasks.map { it.toEntity() })
        upsertBankTransactions(snapshot.bankTransactions.map { it.toEntity() })
    }
}

@Entity(tableName = "businesses", primaryKeys = ["id"])
data class BusinessEntity(
    val id: String,
    val userId: String,
    val abn: String,
    val businessName: String,
    val gstRegistered: Boolean,
    val basFrequency: String,
)

@Entity(tableName = "receipts", primaryKeys = ["id"])
data class ReceiptEntity(
    val id: String,
    val businessId: String,
    val fileUrl: String,
    val merchant: String,
    val receiptDate: String,
    val total: Double,
    val gst: Double,
    val category: String,
    val ocrConfidence: Double,
    val aiConfidence: Double,
)

@Entity(tableName = "bank_transactions", primaryKeys = ["id"])
data class BankTransactionEntity(
    val id: String,
    val businessId: String,
    val transactionDate: String,
    val description: String,
    val amount: Double,
    val category: String,
    val gstEstimate: Double,
    val sourceFile: String,
    val matchedReceiptId: String?,
)

@Entity(tableName = "compliance_tasks", primaryKeys = ["id"])
data class ComplianceTaskEntity(
    val id: String,
    val businessId: String,
    val taskType: String,
    val dueDate: String,
    val status: String,
)

fun Business.toEntity() = BusinessEntity(
    id = id,
    userId = userId,
    abn = abn,
    businessName = businessName,
    gstRegistered = gstRegistered,
    basFrequency = basFrequency.name,
)

fun BusinessEntity.toModel() = Business(
    id = id,
    userId = userId,
    abn = abn,
    businessName = businessName,
    gstRegistered = gstRegistered,
    basFrequency = BasFrequency.valueOf(basFrequency),
)

fun Receipt.toEntity() = ReceiptEntity(
    id = id,
    businessId = businessId,
    fileUrl = fileUrl,
    merchant = merchant,
    receiptDate = receiptDate.toString(),
    total = total,
    gst = gst,
    category = category,
    ocrConfidence = ocrConfidence,
    aiConfidence = aiConfidence,
)

fun ReceiptEntity.toModel() = Receipt(
    id = id,
    businessId = businessId,
    fileUrl = fileUrl,
    merchant = merchant,
    receiptDate = LocalDate.parse(receiptDate),
    total = total,
    gst = gst,
    category = category,
    ocrConfidence = ocrConfidence,
    aiConfidence = aiConfidence,
)

fun BankTransaction.toEntity() = BankTransactionEntity(
    id = id,
    businessId = businessId,
    transactionDate = transactionDate.toString(),
    description = description,
    amount = amount,
    category = category,
    gstEstimate = gstEstimate,
    sourceFile = sourceFile,
    matchedReceiptId = matchedReceiptId,
)

fun BankTransactionEntity.toModel() = BankTransaction(
    id = id,
    businessId = businessId,
    transactionDate = LocalDate.parse(transactionDate),
    description = description,
    amount = amount,
    category = category,
    gstEstimate = gstEstimate,
    sourceFile = sourceFile,
    matchedReceiptId = matchedReceiptId,
)

fun ComplianceTask.toEntity() = ComplianceTaskEntity(
    id = id,
    businessId = businessId,
    taskType = taskType.name,
    dueDate = dueDate.toString(),
    status = status.name,
)

fun ComplianceTaskEntity.toModel() = ComplianceTask(
    id = id,
    businessId = businessId,
    taskType = ComplianceTaskType.valueOf(taskType),
    dueDate = LocalDate.parse(dueDate),
    status = ComplianceStatus.valueOf(status),
)
