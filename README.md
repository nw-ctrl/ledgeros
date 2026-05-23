# LedgerOS

Native Kotlin Android app for an internal Australian business compliance
workflow platform.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room local database persistence
- Supabase-ready schema in `supabase/schema.sql`
- Python automation service for statement parsing, reconciliation, BAS
  preparation summaries, and AI orchestration

## Waze-Inspired Technical Direction

Waze's exact private app stack is not public, so LedgerOS follows the same
publicly visible product pattern rather than pretending to clone internals:
native mobile UX, fast state updates, realtime-ready data boundaries, and
low-friction navigation.

- Keep Android native: Kotlin, Jetpack Compose, Android lifecycle ViewModels.
- Keep UI motion lightweight: short Compose transitions and stable list/card
  sizing.
- Keep data realtime-ready: repositories hide Supabase/Firebase/local sources
  from screens.
- Add live operational signals next: background sync, push reminders, and OCR
  status events.

## Receipt Workflow

The current receipt flow supports native Android image selection, selected-image
preview, local OCR abstraction, deterministic extraction, editable review
fields, local save into dashboard/report totals, and durable Room persistence.

## BAS Preparation Workspace

Reports now includes an Australian financial-year BAS preparation workspace
divided into quarterly sections. It automatically opens on the current
financial year and lets users navigate backward or forward by year, with bank
statement import scaffolding for backlog summaries beyond receipt scanning.

CSV and Excel bank statement import is available in the Reports tab. CSV,
tab-delimited Excel exports, and simple `.xlsx` worksheets are supported when
they expose date/description/amount or debit/credit columns. A test file lives
at `samples/bank-statement-july-march.csv`.

Receipt details and bank transaction details include manual edit controls so
missing dates, amounts, GST estimates, categories, merchants, and descriptions
can be corrected after import or OCR review.

Reports also includes a manual BAS adjustment form for adding one-off sales,
expense, GST, or correction rows without importing a statement file.

Reports also includes a first smart review layer: compact BAS insights,
possible duplicate receipts, suggested receipt-to-bank-transaction matches, and
top spend categories for the selected financial year.

Reports includes a local AI review brief that turns the current BAS workspace
into a cleanup risk score, short summary, and next-action list. The Python
automation service also exposes `/ai/review-brief` so the same contract can be
upgraded to provider-backed AI later with audit logging.

Suggested receipt matches are actionable from transaction detail: users can
accept or clear a receipt link, and reconciliation progress is persisted in the
local Room database.

Reports can prepare and share a BAS CSV export pack for the selected financial
year. The export includes quarter totals, category breakdowns, open review
items, duplicate receipt candidates, and suggested receipt matches.

This app prepares records and estimates only. Figures should be reviewed before
lodging through official channels or a registered adviser.

## Python And AI Direction

LedgerOS should become accountant-grade automation for sole traders and small
businesses: receipt capture, bank statement ingestion, reconciliation,
categorization, quarterly BAS preparation, evidence packs, and owner-facing
summaries.

Python powers the heavier automation layer under `backend/python`. AI should be
used through explicit service boundaries for classification, anomaly detection,
compact explanations, and review assistance. The app should keep an audit trail
of AI outputs and preserve human review before any regulated or official action.

## Local Build

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

## Manual Test Targets

- Install `app/build/outputs/apk/debug/app-debug.apk` and confirm the supplied
  LedgerOS launcher icon appears.
- Confirm the app header shows a compact icon, title, and business subtitle
  without overlapping the mobile status bar.
- Open Receipts, tap `Camera`, capture an invoice/receipt, confirm preview
  appears, then review and save the extracted fields.
- Open Receipts, tap `Gallery`, select an existing receipt image, confirm the
  same review flow works.
- Open Reports, tap `Import CSV or Excel statement`, and choose
  `samples/bank-statement-july-march.csv`.
- Add a manual adjustment in Reports, then open it from recent transactions and
  confirm the transaction detail edit flow still works.
- Confirm Reports opens on the current Australian financial year and the
  previous/current/next year controls rebuild the quarterly BAS sections.
- Confirm the backlog card changes to one imported statement with five
  transactions.
- Confirm Q1, Q2, and Q3 show bank transaction counts and GST estimates.
- Confirm the review queue shows one transaction needing GST/category checking.
- Confirm Smart insights and Top spend categories appear after receipt or bank
  statement data exists for the selected financial year.
- Confirm the AI review brief shows a risk score and action list that changes
  when transactions are reviewed or matched.
- Tap a quarter card to open quarter detail.
- Tap a transaction to open transaction detail.
- Edit a transaction category or GST estimate, save, and confirm the review
  queue/readiness values update.
- Open a suggested transaction match, accept the receipt link, and confirm the
  reconciliation progress card updates.
- Tap `Share BAS CSV export` and confirm Android opens a share sheet with the
  generated preparation summary.
- If a matching receipt exists, tap the linked receipt to open receipt detail.
- Edit a receipt total, GST, category, date, or merchant, save, and confirm
  dashboard/report totals update.
- Return to Dashboard and Reports to confirm totals update.

Environment placeholders live in `.env.example`. Runtime secret injection should
move into local Gradle properties or a secure build config path before backend
connections are enabled.
