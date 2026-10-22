# Gameplay acceptance scenarios

These operator stories describe expected behavior and map it to automated business-logic tests.
They also provide a manual checklist for browser testing.

| User story | Expected outcome | Automated coverage |
| --- | --- | --- |
| As a location operator, when a team arrives, I want to see its current progress and money. | Show successful attempts plus signed manual corrections, minus purchases. Count each solved task once; bonuses increase points without increasing solved-task counts. | `TeamProgressSummaryTest`, existing `ShopViewModelTest` |
| As a location operator, when I enter an answer with different case or spacing, I want equivalent answers to match. | Trim and normalize case/whitespace, record success, refresh progress and clear the submitted draft. | `LocationTasksViewModelTest.correctAnswerIgnoresCaseAndWhitespaceAndRefreshesMoney` |
| As a location operator, when an answer is wrong, I want the failed attempt recorded without awarding a point. | Record failure and show that outcome; leave points unchanged. | `LocationTasksViewModelTest.incorrectAnswerRecordsFailureWithoutAwardingPoints` |
| As a location operator, when I switch teams, I want the next team's own state. | Refresh its progress and discard the previous team's drafts and submission indicators. Unknown team IDs cannot change selection. | `LocationTasksViewModelTest` |
| As an operator, when an input is incomplete or a write fails, I want to avoid a misleading success. | Reject missing team/task/answer before writing. Keep an unsaved answer after a write failure and show an error. | `LocationTasksViewModelTest` |
| As an operator, when I click submit or heal twice during a save, I want one transaction. | Ignore repeated actions until the outstanding action finishes. | Task/healing repeated-click tests |
| As a healer, when I complete a healing task for a failed attempt, I want the available choices refreshed. | Forward the selected team/task/attempt IDs, reload healing history, remove the healed attempt and clear selection. | `HealerTasksViewModelTest.completedHealingRefreshesFailedAttemptsAndTaskHistory` |
| As a healer, when healing fails or a selected task disappears, I want a recoverable screen. | Report errors without claiming success or removing the failed attempt; stop loading. Retain only selections that still exist after reload. | `HealerTasksViewModelTest` |
| As a shop operator, when buying a targeted item, I want a valid target and an updated budget. | Require a target, record the purchase and reload the derived balance. | Existing `ShopViewModelTest` |
| As a game organizer, when creating a game and team setup, I want invalid drafts rejected. | Validate setup before persistence and scope saved records to the selected game. | Existing `GameSelectionViewModelTest`, `SetupViewModelTest` |

## Running the tests

Use the existing desktop test task. These tests use in-memory data sources and need no running database:

```sh
./gradlew :composeApp:desktopTest \
  -Psupabase.env=local \
  -Psupabase.url=http://127.0.0.1:54321 \
  -Psupabase.key=test-only \
  -Pbatkabank.apiBaseUrl=
```

The generated build configuration uses placeholder local values. Keep `RUN_SUPABASE_CONNECTION_TEST` unset;
the separately opt-in live smoke test is not part of this isolated check.

These tests cover ViewModel decisions and calculations, not rendered layout, Supabase permissions, concurrency
between separate clients, or database triggers. Exercise those separately against a test deployment, including
shop affordability, miniboss unlock/rewind rules and healing an already healed attempt.
