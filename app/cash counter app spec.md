# Wallet Denomination Counter — Android App Spec

## 0. Domain Background (read this first — no outside context assumed)

This section exists because whoever implements this app has no memory of any
prior conversation and no built-in Korean cash-handling context. Nothing below
is optional color — naming and structural choices later in this spec depend on
it.

- **Who this is for**: one individual in South Korea who personally handles a
  meaningful amount of physical cash day-to-day and wants to log it with
  denomination-level precision, replacing a paper notebook currently kept by
  hand.
- **Scope**: this app tracks only the cash a person actively carries and
  spends/receives while out and about. Throughout this spec that is called
  "Wallet" — a plain descriptive word chosen for this document, not the name
  of any other product or account elsewhere. Any larger cash reserve kept at
  home is explicitly out of scope and must not be modeled.
- **This app is fully standalone.** It has no relationship to, and does not
  need to import from, export to, or match the format of, any other software
  the user may separately use for personal records. Treat this spec as the
  complete and only source of requirements.
- **Why per-denomination tracking, not just "coins vs. bills"**: two coins
  share the same 10-won face value but are physically different objects — an
  older design (minted before a 2006 redesign, larger/heavier) and the current
  smaller design. The user actively seeks out and keeps the older 10-won coins
  because some legacy equipment (older public payphones) only accepts the old
  coin and rejects the current one. This is why the app needs **nine** distinct
  counters, not eight: two of them share a 10-won face value but must never be
  merged into one counter.
- **Naming warning — do not translate literally**: the two 10-won denominations
  are labeled with the user's own shorthand, `구십` and `신십`. `구십` looks
  like it could be read as the Korean number "90" (구=9, 십=10, 9×10=90) —
  **it is not**. Here `구` means "old/former" and `십` means "10 won," so
  `구십` means "the old 10-won coin," not ninety won. Likewise `신십` means
  "the new 10-won coin" (신 = new), not a numeric value either. Use the
  neutral internal keys (`C10_OLD` / `C10_NEW`) for all logic, and reserve the
  Korean labels purely for on-screen display text — never derive a numeric
  value from the label text itself.

## 1. Purpose

Replace a paper cash notebook (which today only distinguishes coins from
bills, not individual denominations) with a native Android app for on-the-go
recording of cash transactions at full per-denomination granularity. The app
must work fully offline: no network dependency for any core operation
(recording, editing, or viewing transactions).

## 2. Denominations (fixed set, fixed order)

| # | Face value (KRW) | Internal key | Display label | Note |
|---|---|---|---|---|
| 1 | 50000 | `W50000` | 오만원 |  |
| 2 | 10000 | `W10000` | 일만원 |  |
| 3 | 5000  | `W5000`  | 오천원 |  |
| 4 | 1000  | `W1000`  | 천원   |  |
| 5 | 500   | `C500`   | 오백원 |  |
| 6 | 100   | `C100`   | 백원   |  |
| 7 | 10 (old) | `C10_OLD` | 구십 (구 10원) | Visually distinct old-design 10-won coin |
| 8 | 50    | `C50`    | 오십원 |  |
| 9 | 10 (new) | `C10_NEW` | 신십 (현 10원) | Current-design 10-won coin |

The user distinguishes old vs. new 10-won coins visually when handling cash — the
app must never try to auto-detect this; it simply needs two separate counter
buttons for the two 10-won variants.

## 3. Interaction Model — "Coin Dispenser" Metaphor

Inspired by a bus fare change machine: each denomination has one button. Tapping
it increments a **pending count** for that denomination by exactly 1 (as if one
coin/note were dispensed). This is the *only* required input gesture for entering
amounts — no numeric keypad.

- Each button tap: pending_count[denom] += 1, with a small tactile/haptic tick.
- Each denomination line displays: pending_count, and subtotal = pending_count × face_value.
- A running **pending total** = sum of all line subtotals is shown prominently.
- Because dispensing is one-directional, each line needs a small decrement
  affordance (long-press, or a small "–1" tap target) to correct an accidental
  extra tap, without wiping the whole entry.
- A single "Clear" control resets all pending counts to 0 (abandon this entry).

## 4. Screen: Main Entry (default screen on launch)

```
┌─────────────────────────────┐
│ Wallet total: ₩xxx,xxx      │ <- running balance, derived, read-only
├─────────────────────────────┤
│ [50000]  x2   ₩100,000   -  │
│ [10000]  x1   ₩10,000    -  │
│ [5000]   x0   ₩0         -  │
│ [1000]   x3   ₩3,000     -  │
│ [500]    x0   ₩0         -  │
│ [100]    x2   ₩200       -  │
│ [구10]   x0   ₩0         -  │
│ [50]     x1   ₩50        -  │
│ [신10]   x3   ₩30        -  │
├─────────────────────────────┤
│ Pending total: ₩113,280     │
├─────────────────────────────┤
│ Memo: [___________________] │ <- always visible, optional, plain text
│                              │    persists while entering counts,
│                              │    cleared only after a successful save
├─────────────────────────────┤
│   [ 지출 Expense ]  [ 수입 Income ]   │
│           [ Clear ]          │
└─────────────────────────────┘
```

Rationale for memo placement: the memo field must be part of this same screen,
not a follow-up dialog after pressing 지출/수입 — a second screen risks losing
state (app killed, focus lost, back-button, etc.). Typing is optional; leaving
it blank is a valid, common case.

### Save action (지출 or 수입 pressed)

On tap:
1. Reject if pending total == 0 (both buttons disabled/no-op until at least one
   denomination is nonzero).
2. Capture `timestamp = now()`.
3. Capture location asynchronously, best-effort, non-blocking (see §7). Do not
   delay the save waiting on a GPS fix.
4. Persist a new transaction record (see §5) with direction = EXPENSE or INCOME,
   the pending denomination counts, computed total, memo text (may be empty),
   and location (nullable).
5. Update the derived wallet running total.
6. Reset all pending counts to 0 and clear the memo field.
7. Show a brief confirmation (snackbar/toast) with a short-window "Undo" action
   (e.g. 5 seconds) that deletes the just-created record directly, for
   fat-finger recovery without navigating to history.

## 5. Data Model

```kotlin
enum class Denomination(val faceValue: Int, val label: String) {
    W50000(50000, "오만원"),
    W10000(10000, "일만원"),
    W5000(5000, "오천원"),
    W1000(1000, "천원"),
    C500(500, "오백원"),
    C100(100, "백원"),
    C10_OLD(10, "구십"),
    C50(50, "오십원"),
    C10_NEW(10, "신십")
}

enum class TransactionType { INITIAL, EXPENSE, INCOME, ADJUSTMENT }

data class CashTransaction(
    val id: String,                 // UUID
    val timestamp: Long,            // epoch millis
    val type: TransactionType,
    val counts: Map<Denomination, Int>,  // magnitude only, always >= 0
    val total: Long,                // derived, cached for convenience: sum(count * faceValue)
    val memo: String?,              // nullable/blank allowed
    val lat: Double?,               // nullable if location unavailable/denied
    val lon: Double?,
    val createdAt: Long,
    val updatedAt: Long
)
```

Sign convention: `counts` always stores unsigned magnitudes ("how many of this
denomination changed hands"). The record's `type` (EXPENSE/INCOME) determines
whether those counts add to or subtract from the running wallet inventory.
Do not store signed counts — keep magnitude and direction separate, it's easier
to display and to edit later.

### Mixed-direction transactions (pay + receive change)

The current flow only supports one direction per saved transaction. A real
"paid ₩10,000, got ₩2,500 change" event should be entered as two consecutive
transactions (one EXPENSE, one INCOME). Don't build combined pay/change
handling into v1 — note it as a possible v2 quick-flow if it turns out to be
annoying in practice.

### Wallet aggregate (derived state)

Do not maintain wallet-per-denomination totals as hand-patched running counters
that drift out of sync. Compute the current wallet state by replaying, in
chronological order: `INITIAL` (starting counts) → all `EXPENSE`/`INCOME`/
`ADJUSTMENT` records to date. Given personal-use data volumes (low thousands of
rows at most over years), a full replay on each read is cheap — don't add a
cached/incremental aggregate table unless profiling shows it's actually needed.

## 6. Storage

- Room (SQLite) — single `transactions` table matching the model in §5.
- No network dependency for reads/writes. This is simply how native Android
  local storage works; no special "offline mode" is required.
- `INITIAL` is stored as a transaction row like any other (type = INITIAL,
  direction is implicitly "set," timestamp = setup time) rather than a special
  separate table — keeps the replay logic in §5 uniform.

## 7. Location Capture

- Use `FusedLocationProviderClient.getLastLocation()` (Google Play services) for
  low latency — do not block the save action waiting for a fresh GPS fix.
- Request `ACCESS_FINE_LOCATION` at runtime; if denied, or if last-known
  location is null, save the transaction with `lat`/`lon` = null. Location must
  never block or fail the core save operation.
- Foreground-only; no background location access needed, since entry only
  happens while the app is open and in use.

## 8. Screen: History / Ledger

- Reverse-chronological list: date/time, direction (color/icon-coded), total,
  memo preview, tap to expand per-denomination breakdown.
- Tap → detail/edit screen: edit counts, memo, or direction; delete with
  confirmation. Any edit re-derives the wallet aggregate via the same replay
  logic in §5 (no manual patching of a cached total).
- Show captured lat/lon on the detail screen (raw coordinates are fine;
  reverse-geocoding to an address is optional/non-blocking and only if network
  happens to be available — never required).

## 9. Setup Screen (first run, and re-accessible later)

- Reuses the same denomination counter UI from §3/§4 to enter the starting
  on-hand counts.
- Saved as a `type = INITIAL` transaction.
- Re-running setup later (e.g. to correct drift after a manual recount) creates
  a new `type = ADJUSTMENT` transaction rather than overwriting history — keep
  a full audit trail.
- **Reference figure for building/testing this screen** (do not hardcode this
  as a real default — the actual starting count must always be entered by the
  user at first run): 오만2·일만5·오천6·천20·오백30·백20·구십20·오십14·신십10,
  totaling ₩218,000. This is a real figure the user has relied on before as a
  fixed cash-reconciliation reference point, so its shape (few large notes,
  many small coins) is realistic for sanity-checking the layout and the total
  calculation — treat it purely as sample data, never as an auto-filled value.

## 10. Validation

- Disable 지출/수입 buttons while pending total == 0.
- For EXPENSE entries (and for edits/deletes anywhere in history), optionally
  warn — non-blocking caution banner, not a hard block — if the resulting
  replayed wallet state would make any single denomination's running count go
  negative at any point in the timeline. This catches "you don't actually have
  that many 500-won coins" mistakes at entry time rather than after the fact.
  This can be deferred to a v2 pass if it adds too much complexity up front.

## 11. Export (low priority, nice-to-have — not core to v1)

- CSV or JSON export of all transactions via Android's Storage Access
  Framework (`ACTION_CREATE_DOCUMENT`), so the user picks the save location
  manually (e.g. to copy off-device later). No cloud/API integration.
- Columns: timestamp, type, 9 denomination counts, total, memo, lat, lon.

## 12. Suggested Stack

- Kotlin, Jetpack Compose (reactive state binding suits the frequently-updating
  counter grid better than a classic View/XML layout).
- Room for persistence.
- FusedLocationProviderClient for location, with graceful degradation if Play
  services / permission is unavailable.
- minSdk 26+ is a reasonable default; adjust to match the target device.

## 13. Explicit Non-Goals

- No integration with, import from, export-format matching against, or any
  other dependency on separate personal-finance software the user may keep
  elsewhere — this app is fully standalone (see §0).
- No cloud sync; no network dependency for any core (non-export) feature.
- No tracking of any cash reserve other than what's actively carried
  day-to-day (see §0 scope note) — do not model a second, larger cash
  account.
