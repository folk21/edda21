# Use Cases

## Login

### Primary actor
Instructor or Student

### Status
Implemented baseline

### Main flow

1. The actor submits username and password.
2. The system loads the user from the relational store.
3. The system verifies the password hash.
4. The system issues a JWT token with role claims.
5. The actor uses the token on subsequent requests.

## Request question generation session

### Primary actor
Instructor

### Status
Implemented baseline

### Main flow

1. The instructor opens an assignment context.
2. The instructor submits a generation request with count, mode, and filters.
3. The system creates a `question_generation_session`.
4. The system selects reusable questions from the database when allowed by the mode.
5. The system publishes a Kafka request if more questions are still needed.
6. The system returns the current session snapshot.

## Poll generation status

### Primary actor
Instructor

### Status
Implemented baseline

### Main flow

1. The instructor requests a session by identifier or by assignment.
2. The system loads the latest session state.
3. The system returns counters, status, result code, and timestamps.

## Read assignment with questions

### Primary actor
Instructor

### Status
Implemented baseline

### Main flow

1. The instructor requests an assignment by identifier.
2. The backend delegates to the question-provider service.
3. The system returns the assignment together with linked questions when found.

## Register instructor

### Primary actor
Instructor

### Status
Planned

### Main flow

1. The instructor submits registration data.
2. The system creates a `user` record.
3. The system creates the matching `instructor` record.
4. The system returns the created identity or a login-ready confirmation.

## Register student

### Primary actor
Student

### Status
Planned

### Main flow

1. The student submits registration data.
2. The system creates a `user` record.
3. The system creates the matching `student` record.
4. The system returns the created identity or a login-ready confirmation.

## Create course

### Primary actor
Instructor

### Status
Planned

### Main flow

1. The instructor authenticates.
2. The instructor submits course data.
3. The system validates ownership and required fields.
4. The system creates the course.

## Add students to course

### Primary actor
Instructor

### Status
Planned

### Main flow

1. The instructor selects a course.
2. The instructor selects one or more students.
3. The system validates the relationship.
4. The system stores the course-student assignment.

## Start work on an assignment

### Primary actor
Student

### Status
Planned

### Main flow

1. The student opens a course.
2. The student selects an assignment.
3. The system creates or updates the student-assignment relation.
4. The student begins answering questions.

## Save answer

### Primary actor
Student

### Status
Planned

### Main flow

1. The student enters an answer for a question.
2. The system validates the payload.
3. The system stores the answer state.

## Submit assignment

### Primary actor
Student

### Status
Planned

### Main flow

1. The student submits completed work.
2. The system validates that the assignment can be submitted.
3. The system marks the student-assignment record as finished.

## Grade assignment

### Primary actor
Instructor

### Status
Planned

### Main flow

1. The instructor opens completed student work.
2. The instructor reviews answers.
3. The instructor stores per-question and overall grades.
4. The student can later read the resulting grade state.
