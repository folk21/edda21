# Vision

## Problem

Instructors need a reliable way to prepare assignments without manually authoring every question from scratch.
Students need a structured workflow for accessing assignments, answering questions, and eventually seeing results.

A plain synchronous CRUD backend is not enough for this product because question generation may depend on slower and more variable LLM-backed work.

## Product idea

edda21 is an educational platform that combines reusable question-bank content with LLM-generated questions.

The platform is designed around three source strategies:
- `DB_ONLY`
- `DB_THEN_LLM`
- `LLM_ONLY`

This gives instructors control over cost, speed, and novelty of generated content while keeping question generation observable through session status endpoints.

## Product direction

The repository already demonstrates the technical core of the generation scenario:
- login
- instructor identity resolution
- generation session tracking
- question reuse from PostgreSQL
- LLM-backed question creation through a dedicated bridge service

The broader product target adds:
- registration for instructors and students
- course creation and enrollment
- student work on assignments
- grading and grade visibility
- analytics and reporting

## Success criteria for the first practical version

- Instructors can authenticate and request question generation for an assignment.
- The system can reuse existing database questions before calling the LLM.
- The generation status can be polled through a dedicated session API.
- Generated or selected questions are linked to assignments in PostgreSQL.
- Local development works without paid LLM access through a stub-capable bridge.

## Success criteria for the broader product

- Instructors can manage courses and enroll students.
- Students can open assignments, save answers, and submit work.
- Instructors can grade completed assignments.
- Analytics can be produced without overloading the transactional database.
