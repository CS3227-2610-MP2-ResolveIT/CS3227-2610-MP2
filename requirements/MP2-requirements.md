# CS3227 MP2 Requirements

## Project Description

Develop a production-level Java desktop application for a formal setting with
multiple user roles while using basic Agentic Software Engineering (Agentic SE)
features as part of the development process.

The exact application domain, roles, and features are defined by the team. For
a team of **N = 2 or 3 students**, the application should provide **N distinct
user roles** with relevant features. The interface should be simple and
appropriately separated for each role. Shared components such as data storage
should follow sound design principles including SRP and DRY.

The application should be robust and reliable for public users and demonstrate
production-level practices such as CI/CD, automated testing, and monitoring.
Each student should complete the features for a specific role as well as a good
portion of team-level work. The estimated individual workload is 1.5 times the
level of MP1.

The team must also create a reflection document about using basic Agentic SE.
Possible reflection questions include:

- What tasks was the AI agent customised to perform, and how was the skill set
  for each task determined?
- How was a specific skill defined and verified?
- Which tasks did the agent handle effectively, and how did this improve
  productivity, code quality, or testing efficiency?
- Where did the agent need additional guidance or correction?
- What would be changed in the agent instructions or skill set next time?
- What was learned about designing an effective single AI agent for software
  engineering tasks?

## Restrictions

- This is a team project; individual submissions are not allowed.
- The team must contain 2 or 3 students.
- Focus on basic Agentic SE features, such as customising a single AI agent and
  verifying that its skill set works for implementation and testing.
- Codex must be used; Claude may also be used.
- The MP1 application cannot be reused.
- The application must remain a Java desktop application.
- The default Java version is Java SE 25.
- Violations may require the project to be redone. Use the course forum for
  clarification when necessary.

## Submission Requirements

The repository must:

- Be named `CS3227-2610-MP2`.
- Be public and hosted in a team GitHub organisation.
- Use an organisation name in the format
  `CS3227-2610-MP2-[your-project-name]`.
- Have the `master` branch up to date at the submission deadline.

It should contain:

- Source code in the repository, formatted clearly for code review.
- The latest application release as a formal GitHub production release. The
  Gradle-generated JAR should include JavaFX third-party libraries and work on
  different operating systems.
- `docs/UserGuide.md`, describing current features and setup/testing steps
  accurately.
- `docs/DeveloperGuide.md`, describing the current design and software
  engineering process, with acknowledgements for reused ideas, code, and
  documentation.
- A product website hosted using GitHub Pages.
- `docs/Reflections.md`, containing reflections on basic Agentic SE and at least
  three interesting skills explained in detail.
- `logs/…`, containing verified summaries of prompts and AI-agent interactions
  during development.

One team member must submit the GitHub organisation name and the GitHub
usernames of all team members through the relevant Canvas quiz.

- Organisation name and team-member usernames: **4 September, 2:00 pm SGT**
- Repository submission: **29 September, 2:00 pm SGT**

There will be no extensions. Refer to the
[Canvas Grading page](https://canvas.nus.edu.sg/courses/99226/pages/grading)
for late-submission information. Keep the repository accessible and make no
further changes after submission.

## Grading Criteria

- **20% Features**
- **25% Code Quality**
- **10% Documentation Quality**
- **20% Software Engineering Practices**, such as project management, design,
  and testing
- **25% Reflections on Agentic SE**

Peer evaluators and tutors will assess the criteria. Students will also be
required to formally evaluate other submissions. Further evaluation details
will be released later.
