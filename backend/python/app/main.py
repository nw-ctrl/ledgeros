from datetime import date
from typing import Literal

from fastapi import FastAPI
from pydantic import BaseModel, Field

app = FastAPI(
    title="LedgerOS Automation Service",
    version="0.1.0",
    description="Python automation layer for ledger normalization and BAS preparation.",
)


class Transaction(BaseModel):
    transaction_date: date
    description: str
    amount: float
    category: str = "Uncategorised"
    gst_estimate: float = 0.0
    source: Literal["bank", "receipt", "manual"] = "bank"


class BasQuarterSummary(BaseModel):
    label: str
    date_range: str
    sales: float
    expenses: float
    gst_on_sales: float
    gst_on_purchases: float
    net_gst_estimate: float
    review_flags: list[str] = Field(default_factory=list)


class BasPreparationRequest(BaseModel):
    business_id: str
    financial_year: str = "2025-26"
    transactions: list[Transaction]


class BasPreparationResponse(BaseModel):
    business_id: str
    financial_year: str
    quarters: list[BasQuarterSummary]
    disclaimer: str


class AiReviewRequest(BaseModel):
    business_id: str
    financial_year: str = "2025-26"
    transactions: list[Transaction]
    unmatched_expense_count: int = 0
    duplicate_receipt_count: int = 0
    suggested_match_count: int = 0


class AiReviewResponse(BaseModel):
    risk_score: int
    summary: str
    actions: list[str]
    audit_note: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/bas/prepare", response_model=BasPreparationResponse)
def prepare_bas(request: BasPreparationRequest) -> BasPreparationResponse:
    quarter_defs = [
        ("Q1", "July-September", date(2025, 7, 1), date(2025, 9, 30)),
        ("Q2", "October-December", date(2025, 10, 1), date(2025, 12, 31)),
        ("Q3", "January-March", date(2026, 1, 1), date(2026, 3, 31)),
        ("Q4", "April-June", date(2026, 4, 1), date(2026, 6, 30)),
    ]

    quarters: list[BasQuarterSummary] = []
    for label, date_range, start, end in quarter_defs:
        transactions = [
            transaction
            for transaction in request.transactions
            if start <= transaction.transaction_date <= end
        ]
        sales = sum(transaction.amount for transaction in transactions if transaction.amount > 0)
        expenses = sum(abs(transaction.amount) for transaction in transactions if transaction.amount < 0)
        gst_on_sales = sum(
            transaction.gst_estimate for transaction in transactions if transaction.amount > 0
        )
        gst_on_purchases = sum(
            transaction.gst_estimate for transaction in transactions if transaction.amount < 0
        )
        review_flags = [
            f"Review GST/category for {transaction.description}"
            for transaction in transactions
            if transaction.category == "Uncategorised" or transaction.gst_estimate == 0.0
        ]
        quarters.append(
            BasQuarterSummary(
                label=label,
                date_range=date_range,
                sales=sales,
                expenses=expenses,
                gst_on_sales=gst_on_sales,
                gst_on_purchases=gst_on_purchases,
                net_gst_estimate=gst_on_sales - gst_on_purchases,
                review_flags=review_flags,
            )
        )

    return BasPreparationResponse(
        business_id=request.business_id,
        financial_year=request.financial_year,
        quarters=quarters,
        disclaimer=(
            "Preparation estimate only. Review records and lodge through official "
            "channels or a registered adviser where required."
        ),
    )


@app.post("/ai/review-brief", response_model=AiReviewResponse)
def ai_review_brief(request: AiReviewRequest) -> AiReviewResponse:
    transactions_needing_review = [
        transaction
        for transaction in request.transactions
        if transaction.category == "Uncategorised" or transaction.gst_estimate == 0.0
    ]
    risk_score = min(
        100,
        len(transactions_needing_review) * 12
        + request.unmatched_expense_count * 8
        + request.duplicate_receipt_count * 10
        + request.suggested_match_count * 4,
    )
    net_gst = sum(
        transaction.gst_estimate if transaction.amount > 0 else -transaction.gst_estimate
        for transaction in request.transactions
    )
    posture = (
        "High review load"
        if risk_score >= 70
        else "Moderate review load"
        if risk_score >= 35
        else "Light review load"
        if risk_score > 0
        else "Clean preparation set"
    )
    position = "payable" if net_gst >= 0 else "credit"
    actions: list[str] = []
    if transactions_needing_review:
        actions.append(
            f"Review {len(transactions_needing_review)} transactions with missing GST or category."
        )
    if request.suggested_match_count:
        actions.append(f"Accept or clear {request.suggested_match_count} suggested receipt matches.")
    if request.unmatched_expense_count:
        actions.append(f"Attach evidence for {request.unmatched_expense_count} unmatched expenses.")
    if request.duplicate_receipt_count:
        actions.append(f"Check {request.duplicate_receipt_count} possible duplicate receipts.")
    if not actions:
        actions.append("No urgent cleanup detected. Export is ready for human review.")

    return AiReviewResponse(
        risk_score=risk_score,
        summary=f"{posture}. Current BAS workspace shows ${abs(net_gst):,.2f} estimated {position}.",
        actions=actions[:4],
        audit_note=(
            "Deterministic local heuristic. Future AI provider calls should log model, "
            "prompt version, inputs, and human edits."
        ),
    )
