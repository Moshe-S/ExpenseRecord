# ExpenseRecord

## Project Overview
A fully offline expense tracking application, designed with an emphasis on simplicity, privacy, and streamlined daily use.

## Key Features
- **Expense Tracking**: Add, edit, and delete records, with undo function for deletions.
- **Date and Time: Automatic stamping with manual selection; confirmation prompt for future dates.**
- **Categories**: Auto-completion mechanism prevents duplicate category creation.
- **Views**: Selectable display views by month or week, dynamically updating the total expenses shown.
- **Scrolling**: Full vertical scrolling in the expense list.
- **Search and Filter**: Text search and category filtering capabilities.
- **Data Export**: CSV export via the Storage Access Framework (SAF).
- **Data Entry UX**: Optimized keyboard navigation and smooth flow between input fields.
- **User Feedback**: Snackbar notification mechanisms for action tracking.
- **Detailed Table View**: Full display with alternate row shading, visual separators, and dynamic sorting.
- **Security**: Encrypted database persistence (SQLCipher).
- **Version Stability**: Active data migration mechanism ensures continuity across versions.

## Technologies
- **Kotlin**
- **Jetpack Compose**
- **Room + SQLCipher (Encrypted Database)**
- **Activity Result API**
- **Logical Modular Architecture (ViewModel, DAO, Entity, UI Composables)**

## Status
The current build is a **stable pre-V1 version**, used daily on physical Android devices and tested across emulators.
**V1 will be published to the Play Store** once the full redesign and structured rebuild are complete.

## Development Insight
The project originated as a personal tool and gradually evolved into a stable version with a defined and clear data structure.
Development progressed in phases, focusing on core flow planning, consistent testing, and continuous adjustments.
AI tools were integrated into the workflow for code snippet generation and development optimization, and every component **went through manual review** and refinement to ensure the application’s stability, consistency, and clear structure.
 

## V1 Roadmap
Version V1 will be written as a **well-structured new project**, including:
- **Detailed documentation** of the logic and the communication structure between application parts.
- **Designing and defining a new architecture** and an **organized file structure**.
- **Transition to multi-screen navigation** instead of a single screen, and a **refined, user-centric UI design.**
- Continued support for existing data migration.
- **Building the code with an organized, modular structure** designed for public release.

## Contact
Technical inquiries: dev.projects.moshe@proton.me
