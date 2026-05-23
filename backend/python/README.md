# LedgerOS Python Automation Service

This service is the planned automation layer for heavier bookkeeping,
statement parsing, reconciliation, BAS preparation summaries, and AI-assisted
review.

The Android app remains native Kotlin/Compose. Python is used where it is
stronger: document parsing, bank-statement normalization, reconciliation
algorithms, report generation, and AI orchestration.

## Intended Responsibilities

- Parse bank statements from CSV, XLSX, and PDF.
- Normalize transactions into a common ledger format.
- Match receipts to bank transactions.
- Flag GST and category uncertainty for human review.
- Generate BAS preparation summaries by quarter.
- Produce evidence packs and review notes.
- Call AI providers only through explicit, auditable service boundaries.

## Regulatory Boundary

LedgerOS can automate preparation, record management, review workflows, and
owner-facing summaries. It must not claim to be a registered tax agent, BAS
agent, accountant, or regulated adviser unless the operating entity is properly
registered and the feature is designed around that compliance model.

Official Australian guidance says registered BAS/tax agent rules can apply when
providing BAS or tax agent services for fee or reward. Keep product language and
workflows focused on preparation, estimates, evidence, and review-ready outputs.

## Local Run

```powershell
cd backend/python
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Health check:

```text
GET http://127.0.0.1:8000/health
```

AI review brief:

```text
POST http://127.0.0.1:8000/ai/review-brief
```

The first implementation is deterministic and auditable. It returns a cleanup
risk score, a compact BAS preparation summary, next actions, and an audit note.
Future AI provider calls should keep the same response shape while logging the
model, prompt version, inputs, and human edits.
