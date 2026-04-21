# SayIt Case Study

## 1. Project Summary

SayIt is a voice-first Android task management prototype built to reduce friction in everyday task capture and review.

Instead of starting from a traditional list-and-form interface, the product starts from a simple assumption: a daily task app should be fast enough to use at the speed of thought.

The current prototype focuses on:

- capturing tasks through short spoken commands
- centering the experience on today's work
- making date navigation lightweight through a monthly calendar
- using visual task states to reduce scanning effort

## 2. Problem Statement

Many task apps work well for heavy planning, but feel too slow for lightweight daily use. The common pain points are:

- too many taps to add a simple task
- poor support for hands-busy or walking scenarios
- limited focus on "today" as the default working context
- calendar views that are visually disconnected from actual task state

This creates a gap between the user's intention and the product's interaction cost.

## 3. Target Users

This concept is best suited for users who:

- think in daily tasks rather than long project boards
- want fast task capture on mobile
- prefer speaking over typing in low-friction moments
- need a simple visual overview of daily completion status

Example scenarios:

- capturing a task while commuting
- checking unfinished tasks before leaving work
- reviewing task completion across recent days
- quickly cleaning up today's task list by voice

## 4. Product Goals

The product goals for this prototype were:

1. Make task capture nearly immediate
2. Make today's tasks the default focus
3. Make date-based browsing visually clear
4. Reduce interaction overhead for repeated daily actions
5. Keep the experience simple enough for casual, frequent use

## 5. Functional Requirements

### Primary Requirements

- Open directly to today's task list
- Let the user start speech recognition with one tap
- Support quick voice commands for add, complete, query, and delete
- Support manual completion toggling from the list
- Provide a monthly calendar view
- Let users tap a day to open that day's task list
- Show task status visually on the calendar

### Interaction Requirements

- Back from task list should open calendar
- Back from calendar should return to today's task list
- Double-tapping blank space on the main page should start listening
- The quick-command hint card should be collapsible
- The listening flow should expose clear in-progress states

### Visual Requirements

- Completed tasks should read as clearly done
- Overdue unfinished tasks should be easy to spot
- Active and pending states should remain visually simple
- Branding should feel more intentional than a plain utility app

## 6. UX Decisions

### Why Start On Today's Task List

The product is designed around execution, not long-form planning. Opening directly on today's list keeps the app aligned with the most likely user intent.

### Why Keep Calendar As A Secondary View

Calendar is useful for context and review, but not the highest-frequency action. It works better as a browse layer behind the daily list.

### Why Use Color-Coded Day States

Users should not need to open each date just to understand whether the day is complete, overdue, or still active.

### Why Add Listening And Processing States

Speech interfaces often feel broken when they are merely waiting on recognition. Explicit status messaging improves perceived responsiveness and user trust.

### Why Support Double-Tap To Listen

For a voice-first app, starting speech should not require precision targeting every time. A background gesture lowers friction for repeat use.

## 7. Technical Decisions

### Architecture

- Kotlin
- Jetpack Compose for UI
- Room for local persistence
- Android TextToSpeech for spoken feedback

### Why Local Task Storage

This prototype does not require user accounts or cloud sync to validate the core interaction model. Local persistence keeps the build simple and fast.

### Why System Speech Recognition First

Using Android system speech recognition was the fastest way to validate the voice-first workflow in an early prototype.

### Known Technical Limitation

The current speech layer depends on Android system recognition and may rely on Google-backed services. This is a weak fit for users in mainland China and creates latency and availability risk.

## 8. Constraints And Tradeoffs

### Constraint: Fast Prototype Delivery

The project prioritized speed of product validation over final speech infrastructure.

Tradeoff:

- faster MVP delivery
- weaker production readiness in China

### Constraint: Mobile-First Simplicity

The prototype avoids feature creep such as projects, tags, accounts, and collaboration.

Tradeoff:

- clearer focused experience
- narrower scope

### Constraint: Daily Use Case

The UX is optimized for short, repetitive daily interactions.

Tradeoff:

- excellent for daily capture and review
- less suitable for complex project management

## 9. What Was Implemented

- Voice-triggered task creation
- Voice-triggered task completion
- Voice-triggered completed and unfinished task queries
- Voice-triggered deletion of all tasks for today
- Today's task list as home
- Monthly calendar browsing
- Date-based task detail view
- Completed / overdue / pending color states
- Collapsible hint card
- Double-tap blank area to start listening
- Listening and processing status feedback
- Custom icon and lightweight brand treatment

## 10. What Still Needs To Be Improved

- Replace the current speech recognition layer with a China-friendly solution
- Add screenshot assets for store or portfolio use
- Add release packaging and signed builds
- Add editing and finer-grained deletion flows
- Improve README visuals and product storytelling

## 11. Recommended Next Product Step

The most important next step is replacing the current voice recognition layer with either:

- a domestic cloud SDK such as Baidu or Aliyun, or
- an offline engine such as `sherpa-onnx`

This is the clearest path from prototype to production viability for China-based users.

## 12. LinkedIn Post Angle

This project works well as a case study around:

- voice-first mobile UX
- reducing friction in daily task management
- designing around real-world mobile contexts
- balancing prototype speed vs production constraints

## 13. Slide Outline

### Slide 1: Title

- SayIt
- A voice-first daily task management app for Android

### Slide 2: Problem

- Daily task apps are often too slow for lightweight use
- Voice interaction can reduce friction, but only if the workflow is tightly scoped

### Slide 3: Product Opportunity

- Build a task app centered on today's work
- Let users capture and manage tasks in short spoken commands

### Slide 4: User Needs

- Quick capture
- Minimal typing
- Simple date navigation
- Clear completion status

### Slide 5: Core Requirements

- Today's task list by default
- Voice add / complete / query / delete
- Monthly calendar with task state visibility

### Slide 6: UX Decisions

- Daily list first, calendar second
- Color-coded day states
- Listening and processing feedback
- Double-tap gesture for faster voice access

### Slide 7: Technical Stack

- Kotlin
- Jetpack Compose
- Room
- Android speech recognition

### Slide 8: Constraint

- Current speech layer is fast for prototyping but not ideal for mainland China

### Slide 9: Next Step

- Replace speech recognition with Baidu / Aliyun / offline ASR

### Slide 10: Outcome

- A working prototype that validates the voice-first task flow
- A clear path toward a production-ready version

## 14. Short Portfolio Summary

SayIt is an Android prototype that rethinks task management around voice-first interaction. Instead of optimizing for complex planning, it optimizes for low-friction daily execution: capture tasks fast, focus on today, and understand progress visually across the calendar.
