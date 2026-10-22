# JHENORA

Production Android app for Romblon State University centralizing announcements, events, calendars, organizations, and department communication. Built with Android Studio (Java) and Supabase, using an iOS-inspired "White Paper" design.

## Tech Stack

- **Platform**: Android Studio, Java
- **Architecture**: MVVM + Repository Pattern
- **Backend**: Supabase (PostgreSQL, Auth, Storage, Realtime)
- **Network**: Retrofit + OkHttp, Gson for JSON
- **Images**: Glide
- **UI**: Material Design 3, iOS-inspired
- **Local cache**: Room Database
- **Preferences**: Encrypted SharedPreferences
- **Notifications**: Local Android notifications via WorkManager + AlarmManager
- **Logging**: Timber
- **DI**: Manual repository injection (no Hilt/Dagger)
- **minSdk**: 26, **targetSdk**: 36

## Architecture Layers

Presentation → ViewModel → Repository → Remote Data Source / Local Database → Supabase Backend

### Folder structure
- `activities/` — Activities
- `fragments/` — Fragments
- `viewmodels/` — MVVM ViewModels
- `repositories/` — Business logic
- `models/` — Java models
- `services/` — Notification & background services
- `adapters/` — RecyclerView adapters
- `utils/` — Helper classes
- `database/` — Room database
- `network/` — Supabase API
- `security/` — Encryption & validation
- `notifications/` — Local notification manager

## Design Theme — "White Paper UI"

iOS Human Interface Guidelines inspired.

| Token | Value |
|---|---|
| background | `#FFFFFF` |
| surface | `#F8F8F8` |
| primary | `#0057D9` |
| secondary | `#4A4A4A` |
| text_primary | `#111111` |
| text_secondary | `#6B6B6B` |
| corner_radius | `18dp` |
| shadow | Soft |
| animation | Smooth native motion |
| icons | Outlined |

## User Roles & Permissions

- **admin**: Dashboard, full CRUD, approve/reject posts, manage departments/organizations/users, verify faculty, assign org leaders, manage live display, analytics, send notifications, monitor activities.
- **faculty**: Create/edit/delete own announcements & events, view profile, receive updates, view post history. New faculty accounts start as **pending verification**.
- **organization**: Create/edit/delete own announcements & events, receive updates.
- **student**: Read announcements/events, calendar, search, filter, receive notifications, profile.
- **live_display**: Dedicated read-only account for campus live signage — displays approved announcements/events, auto-refreshes.

## Departments

BSIT, BTLED, EDUC

## Key Pages

Welcome (splash/intro/login/register) · Home (feed + quick access) · Announcements (feed, search, filter, department tabs, bookmarks) · Events (upcoming, search, filter, department view, details) · Calendar (monthly view, indicators, today's activities) · Profile (info, department, role, post history, edit, notification settings)

## Auth & Registration

- Login: email + password
- Faculty registration fields: firstname, middlename, lastname, email, password, department, role (Faculty), profession → status starts as **pending verification**
- Student registration fields: firstname, middlename, lastname, email, password, department, address, block, year

## Content Forms

**Announcement**: Title, Description, Location, Date & Time, Attachment, Priority, Department Scope, General Scope

**Event**: Title, Description, Location, Poster, Start Date, End Date, Registration Link, Department Scope, General Scope

## Admin Dashboard Modules

Analytics, User Verification, Department Management, Organization Management, Faculty Approval, Announcement Approval, Event Approval, Rejected Posts, Live Display, Audit Logs, Notification Manager, Statistics

## Database Tables

`users`, `departments`, `organizations`, `announcements`, `events`, `notifications`, `audit_logs`

## Security

- Supabase Auth for authentication, Role-Based Access Control for authorization
- Row Level Security at the database layer
- Passwords encrypted, all communication over HTTPS, storage in secure buckets
- Server + client-side validation, JWT sessions, audit logs
- SQL injection / XSS protection, rate limiting

## Offline Support

Room database caches announcements and events locally; syncs when back online.

## Performance Practices

Pagination, lazy loading, image caching, background sync, optimized queries — apply these when adding list/feed features.

## Production Standards

- Clean Architecture, full JavaDoc on public APIs
- Centralized error handling, secure logging
- Enterprise-ready security posture; high maintainability and scalability expected
