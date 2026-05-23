package com.ledgeros.app.model

import java.time.LocalDate

data class ComplianceTask(
    val id: String,
    val businessId: String,
    val taskType: ComplianceTaskType,
    val dueDate: LocalDate,
    val status: ComplianceStatus,
) {
    val isOverdue: Boolean
        get() = status != ComplianceStatus.Done && dueDate.isBefore(LocalDate.now())
}

enum class ComplianceTaskType(val label: String) {
    Bas("BAS"),
    GstReview("GST review"),
    Payroll("Payroll"),
    Superannuation("Super"),
}

enum class ComplianceStatus(val label: String) {
    Todo("To do"),
    Pending("Pending"),
    Done("Done"),
}
