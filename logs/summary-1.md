# ResolveIT AI Prompt Summary

Task: Brief chronological summary of project-related AI prompts  
Status: completed  
Evidence sources: local Codex session histories and repository history  
Human verification: approved

## Prompt order

1. Refine the initial ResolveIT specification into `PROJECT.md`.
2. Explain the ticket `version` column, add a ticket-edit API contract, and remove Flyway if unnecessary.
3. Create a senior backend Java engineer skill.
4. Initialize the backend Gradle project and implement the user database, REST API, authentication, and user service.
5. Add demo accounts for one manager, three technicians, and thirty employees, with clear login details.
6. Explain how to run the backend, why the Java package used `sg/edu`, then remove those package levels.
7. Explain the SQLite file location and trace data flow from the database to the REST API.
8. Implement ticket and ticket-message services inside the existing monolith, then update the backend README.
9. Explain ticket integration-test warnings, assignee validation, ticket visibility, and optimistic locking for messages.
10. Explain JavaFX refresh behavior and plan SSE message synchronization, then simplify the concurrency explanation and clarify cursor handling.
11. Explain `application.yml` and SQLite’s `foreign_keys=on`.
12. Initialize the JavaFX frontend and implement the login page using the frontend JavaFX skill.
13. Explain how to start the frontend, remove unwanted bottom whitespace, discuss package layout, and locate frontend environment configuration.
14. Create a senior JavaFX frontend skill and store it in the repository’s `.agents` directory.
15. Diagnose why the backend uses `bootRun` instead of `run`.
16. Implement the employee UI, fix white hover text, and remove the “need urgent help” section.
17. Implement the technician UI only, following `PROJECT.md` and the JavaFX skill.
18. Commit the technician UI changes after user confirmation.
19. Implement the manager UI using the completed backend APIs.
20. Commit the manager UI changes after user approval.
21. Explain where manager user-editing APIs are implemented and why no separate manager backend service exists.
22. Refactor manager-service construction and controller injection to match the existing frontend style after questions about inconsistent dependency handling.
23. Remove empty space at the right of the ticket and manager user tables.
