# LedgerOS Status

Last updated: 2026-05-19 Australia/Sydney

## Current State

LedgerOS has been converted from the initial Flutter scaffold to a clean native
Kotlin Android project in this folder.

The app now has first-pass architecture boundaries: repository interfaces, a
Compose ViewModel, and explicit UI state classes shared by Dashboard, Receipts,
Compliance, BAS reports, and Settings.

LedgerOS now has first-pass Room persistence behind the repository boundary.
The database stores the active business, receipts, imported bank transactions,
and compliance tasks, seeded from the demo data on first launch.

The interface has been smoothed with a contextual app header, lightweight
Compose navigation transitions, stable card sizing, and tighter Material 3
shapes. The technical direction is Waze-inspired where it makes sense for this
domain: native mobile UX, realtime-ready state boundaries, and fast operational
feedback rather than a clone of Waze's private internals.

Receipts now have the first real workflow: native Android image selection,
receipt preview, local OCR service boundary, deterministic extraction, editable
review fields, and local save into live dashboard/report totals.

The supplied LedgerOS image has been installed as the Android launcher icon
across standard mipmap densities. The latest icon source is
`C:/Users/aihub/Downloads/ChatGPT Image May 17, 2026, 08_16_28 AM.png`.

The app header now keeps the LedgerOS app name fixed at the top, while the
navigation context underneath changes with the selected tab or drill-down
screen. Nested report and receipt routes keep their correct selected section.

Receipts now support real camera capture through Android's camera intent and a
FileProvider-backed image URI, in addition to gallery upload. Captured invoice
images feed the same OCR/review/save pipeline.

Reports now include a BAS preparation workspace for FY 2025-26. The current
example target is July 2025 through March 2026, divided into Q1, Q2, and Q3,
with Q4 visible as the next section. The workspace supports bank statement
import scaffolding so backlog summaries can be built from statements as well as
receipts.

Reports now automatically select the current Australian financial year and let
the user move backward, forward, or back to current year. The BAS workspace now
shows year-scoped quarter sections, dated due dates, net GST position, export
readiness, and review health signals.

CSV and Excel bank statement parsing is now implemented on Android. The Reports
tab can import CSV, TSV-style Excel exports, and simple `.xlsx` worksheets with
date/description/amount columns or debit/credit columns, infer simple
categories, estimate GST, and feed BAS quarter summaries. A sample import file
is available at `samples/bank-statement-july-march.csv`. Reports also flags
imported transactions that need GST/category review.

Receipt detail and transaction detail screens now include manual edit controls
for correcting missing or incorrect fields. Saved corrections update dashboard
totals, BAS summaries, review readiness signals, and the local Room database.

Reports now includes a manual BAS adjustment entry form. Manual adjustments are
stored as bank transactions, persist through Room, update BAS totals, appear in
recent transactions, and can be opened for later editing like imported rows.

Reports now includes a local AI review brief. It scores the selected financial
year's cleanup risk, explains the current GST posture, and lists next actions
from live review flags, unmatched expenses, possible duplicates, and suggested
receipt matches. The feature is deterministic and auditable today, with a
matching Python `/ai/review-brief` endpoint scaffolded for future provider-backed
summaries.

Reports now has a first smart review layer for the selected financial year:
compact insights, possible duplicate receipt detection, suggested receipt to
bank transaction matches, and top spend category breakdowns with GST estimates.

Receipt-to-bank reconciliation is now actionable. Bank transactions can store a
matched receipt id, suggested matches can be accepted from transaction detail,
matches can be cleared, and Reports shows matched versus unmatched expense
progress. The link is persisted in Room with a version 1 to 2 migration.

Reports now generates a BAS CSV export pack for the selected financial year and
shares it through Android's share sheet. The pack includes quarter totals,
category breakdowns, open review items, possible duplicate receipts, suggested
receipt matches, and a preparation-only disclaimer.

BAS drill-down navigation is now available: Reports overall status opens a
quarter detail screen, quarter transactions open transaction detail, and matched
transactions can open individual receipt detail.

A Python automation service has been scaffolded under `backend/python` for
statement parsing, reconciliation, BAS preparation summaries, and AI
orchestration. Android remains the native mobile client; Python is the heavier
processing layer.

The app currently builds as a Jetpack Compose Android app with a mobile-first
internal operations workflow:

- Dashboard
- Receipts
- Compliance tasks
- BAS reports
- Settings

The Supabase schema from the original brief was preserved at
`supabase/schema.sql`.

## Verified Commands

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Both commands passed after the Kotlin conversion.

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Important Files

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/java/com/ledgeros/app/data/RoomLedgerRepository.kt`
- `app/src/main/java/com/ledgeros/app/data/local/LedgerDatabase.kt`
- `app/src/main/java/com/ledgeros/app/MainActivity.kt`
- `app/src/main/java/com/ledgeros/app/ui/LedgerOsApp.kt`
- `app/src/main/java/com/ledgeros/app/ui/LedgerViewModel.kt`
- `app/src/main/java/com/ledgeros/app/ui/state/LedgerUiState.kt`
- `app/src/main/java/com/ledgeros/app/ui/screens/`
- `app/src/main/java/com/ledgeros/app/ui/screens/BasPeriodDetailScreen.kt`
- `app/src/main/java/com/ledgeros/app/ui/screens/TransactionDetailScreen.kt`
- `app/src/main/java/com/ledgeros/app/ui/screens/ReceiptDetailScreen.kt`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/main/java/com/ledgeros/app/model/`
- `app/src/main/java/com/ledgeros/app/model/BankTransaction.kt`
- `app/src/main/java/com/ledgeros/app/service/`
- `app/src/main/java/com/ledgeros/app/service/LocalReceiptOcrService.kt`
- `app/src/main/java/com/ledgeros/app/service/BankStatementParser.kt`
- `app/src/test/java/com/ledgeros/app/service/RegexReceiptExtractorTest.kt`
- `app/src/test/java/com/ledgeros/app/service/CsvBankStatementParserTest.kt`
- `app/src/main/res/mipmap-*/ic_launcher.png`
- `samples/bank-statement-july-march.csv`
- `supabase/schema.sql`
- `backend/python/`
- `backend/python/app/main.py`
- `.env.example`

## Product Direction

LedgerOS is positioned as an internal operational platform first, with future
SaaS-ready architecture.

The ambition is accountant-grade automation for sole traders, small businesses,
and eventually medium companies. It should replace large amounts of manual
bookkeeping, reconciliation, evidence preparation, and BAS preparation work.
It must not claim to be a registered tax agent, BAS agent, accountant, or
regulated adviser unless the operator is properly registered and the product is
designed around that compliance model.

Use positioning language like:

- Business compliance assistant
- Operational workflow platform
- Record management system

Avoid positioning it as:

- Tax agent
- Accounting advisor
- BAS lodgement provider

## Next Best Steps

1. Replace the demo local OCR implementation with an on-device OCR engine.
2. Add receipt delete/archive actions with confirmation so saved records can be
   corrected end-to-end.
3. Add explicit reject/snooze controls for suggested receipt-bank matches.
4. Add PDF bank statement extraction.
5. Connect the Android app to the Python automation service for BAS preparation
   summaries and reconciliation.
6. Persist settings locally so product controls survive app restarts.
7. Connect the Android AI review brief to the Python automation endpoint when
   authenticated backend calls are available.
8. Add user authentication and a user/business-scoped database.
9. Wire Supabase Auth and business profile persistence behind the repository
   repository interface.
10. Keep deterministic parsing available; use AI for categorization, anomaly
   detection, review explanations, and compact summaries.
11. Add secure environment handling via Gradle/local properties before live
   backend calls.
12. Add Firebase Cloud Messaging later for compliance reminders.

## Full Project Plan

Always keep this section updated with the working plan and status changes.

### Phase 1: Native Foundation

Status: Done.

- Convert from Flutter scaffold to native Kotlin Android.
- Use Jetpack Compose, Material 3, Navigation Compose, and ViewModel state.
- Preserve Supabase schema from the original brief.
- Keep the app positioned as an operational workflow platform, not a tax agent
  or accounting advisor.

### Phase 2: Architecture Boundary

Status: Done for first pass.

- Add `LedgerRepository` interface and demo implementation.
- Route screen data through `LedgerViewModel`.
- Keep explicit UI state classes for dashboard, receipts, compliance, reports,
  and settings.
- Keep services swappable: OCR, deterministic parsing, AI categorization, and
  future persistence should stay behind boundaries.

### Phase 3: Interface Smoothness

Status: In progress, good enough to support feature work.

- Keep mobile-first navigation with bottom tabs.
- Maintain status-bar-safe top header.
- Use lightweight Compose transitions and stable Material 3 components.
- Continue polishing as real workflows are added rather than doing a large
  cosmetic-only pass.

### Phase 4: Receipt Capture And Review

Status: First workflow implemented.

- Native Android image picker is wired.
- Selected image preview is shown.
- Demo local OCR service boundary exists.
- OCR text feeds deterministic extraction.
- Review fields are editable.
- Save adds reviewed receipt to local state, Room persistence, and
  dashboard/report totals.

Remaining:

- Replace demo OCR with real on-device OCR.
- Add camera capture quality checks such as crop, retake, glare/blur warning,
  and document edge detection.
- Add validation for date, total, GST, and category fields.
- Add duplicate detection using receipt hash, merchant, date, and total.
- Add delete actions.

### Phase 5: Durable Local Persistence

Status: First pass implemented.

- Add local database storage for businesses, receipts, imported bank
  transactions, and compliance tasks.
- Room is wired behind the repository boundary.
- Repository interface should expose durable data without screens knowing
  whether data came from demo, local database, or backend.
- Saved receipt reviews and imported bank transactions now persist locally.

Remaining:

- Persist settings.
- Add explicit delete/archive actions with confirmation.
- Add migrations before schema changes ship beyond development.
- Add local persistence tests around seeding and updates.

### Phase 6: BAS Preparation Workspace

Status: In progress with dynamic financial-year selection.

- The BAS workspace detects the current Australian financial year and supports
  previous/current/next year navigation.
- The selected financial year is divided into quarterly sections:
  - Q1: July-September, ordinary due date 28 October.
  - Q2: October-December, ordinary due date 28 February.
  - Q3: January-March, ordinary due date 28 April.
- Q4: April-June, ordinary due date 28 July.
- BAS screen combines receipt totals and imported bank-statement backlog
  transactions.
- BAS screen shows net GST position, export readiness percentage, and review
  health summary for the selected year.
- BAS screen generates a shareable CSV preparation export for the selected
  financial year.
- Keep the app as preparation and record management, not BAS lodgement or tax
  advice.

Remaining:

- Add PDF statement extraction.
- Broaden Excel extraction for multi-sheet workbooks and richer bank formats.
- Replace heuristic receipt matching with a full reconciliation workflow.
- Add exportable BAS preparation summary.

### Phase 7: Python Automation And AI

Status: Scaffolded.

- Python FastAPI service exists under `backend/python`.
- Initial `/health` and `/bas/prepare` endpoints are scaffolded.
- Use Python for statement parsing, reconciliation, report generation, anomaly
  detection, and AI orchestration.
- Keep Android native and call Python/backend APIs for heavier workflows.
- Add clear audit logs for AI classifications, assumptions, confidence, and
  human edits.

Remaining:

- Add PDF parser implementation and backend-grade CSV/XLSX normalization.
- Add reconciliation engine for matching bank transactions to receipts.
- Add AI provider abstraction and prompt/version logging.
- Add evidence-pack generation.
- Add authenticated API calls from Android to the service.

### Phase 8: Backend And Auth

Status: Not started.

- Add secure environment handling via Gradle/local properties before live calls.
- Wire Supabase Auth.
- Persist active business profile.
- Sync receipts and compliance tasks to Supabase tables from
  `supabase/schema.sql`.
- Add conflict handling for offline/local edits.
- Ensure all data is user-scoped and business-scoped before any cloud sync.

### Phase 9: Intelligence Layer

Status: First local intelligence layer implemented.

- Keep deterministic extraction as a transparent baseline.
- Use AI for categorization, compact summaries, and review assistance.
- Keep payloads small and avoid sending full OCR text unless the user enables
  that workflow.
- Innovation mode can unlock more adaptive experiments while preserving user
  control and compliance boundaries.
- Local smart review now identifies possible duplicate receipts, suggested
  receipt-bank matches, top spend categories, and compact BAS readiness
  insights without sending data to an external provider.
- The Reports AI review brief produces an auditable risk score, summary, and
  action list locally. The Python automation service exposes the same
  `/ai/review-brief` contract for future model-backed orchestration.
- Suggested receipt-bank matches can be accepted or cleared and are persisted
  locally as reconciliation evidence.

Remaining:

- Add explicit reject/snooze controls for suggested matches.
- Add anomaly detection for unusual spend, GST ratios, and repeated merchants.
- Connect AI provider-backed explanations through the Python review endpoint
  with prompt/version logging.

### Phase 10: Compliance Operations

Status: Not started.

- Make compliance tasks actionable.
- Add due-date reminders and task state changes.
- Add Firebase Cloud Messaging later for reminders.
- Add BAS report preparation views from reviewed receipt data.
- Keep wording as operational assistance, not lodgement or regulated advice.

### Phase 11: SaaS Readiness

Status: Not started.

- Support multiple businesses per user.
- Add role-aware access patterns.
- Add audit/event history for receipt review and report preparation.
- Add export flows for PDF/CSV.
- Add observability around sync, OCR failures, and review completion.

## Product Controls

Settings now exposes real UI state for deterministic parsing, identifier
masking, OCR fallback, and innovation mode. Innovation mode removes the
hard-coded deterministic-first workflow preference inside the app, while
platform, privacy, and legal compliance constraints still apply.

## Notes For Resume

The project is not currently a git repository. Do not assume git history exists.

The previous Flutter dev server was stopped and Flutter folders were removed.
The active implementation is native Kotlin/Compose.
