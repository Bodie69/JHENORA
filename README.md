# JHENORA

An Android app for **Romblon State University** that brings campus announcements, events, calendars, organizations and department communication into one place.

Built in Java with Android Studio, backed by [Supabase](https://supabase.com), with a clean iOS-inspired "White Paper" design.

## Features

- **Announcements**: a searchable, filterable feed with department tabs, priorities, attachments and bookmarks.
- **Events**: upcoming events with posters, start/end dates, registration links and a department view.
- **Calendar**: a monthly view with event indicators and an agenda of the day's activities.
- **Profile**: personal info, department, role, post history and notification settings.
- **Approval workflow**: faculty and organization posts go to admins, who approve or reject them.
- **Admin dashboard**: analytics and statistics, user and faculty verification, department and organization management, announcement and event approval, rejected posts, audit logs, a notification manager and Live Display management.
- **Live Display**: a read-only kiosk mode for campus signage that rotates approved content and refreshes automatically.
- **Offline support**: announcements and events are cached in Room and synced when the device is back online.
- **Notifications**: local notifications scheduled with WorkManager and AlarmManager, plus Supabase Realtime updates.

## User roles

| Role | What they can do |
|---|---|
| `admin` | Full access: dashboard, CRUD on everything, approvals, user/department/organization management, Live Display, analytics, notifications |
| `faculty` | Create, edit and delete their own announcements and events; new accounts start as **pending verification** |
| `organization` | Create, edit and delete their own announcements and events |
| `student` | Read announcements and events, use the calendar, search, filter and receive notifications |
| `live_display` | A read-only signage account that shows approved announcements and events |

Departments: **BSIT**, **BTLED**, **EDUC**.

## Tech stack

- **Language / platform**: Java, Android SDK (minSdk 26, targetSdk 36)
- **Architecture**: MVVM + Repository pattern, with manual dependency injection
- **Backend**: Supabase (PostgreSQL, Auth, Storage, Realtime, Edge Functions)
- **Networking**: Retrofit, OkHttp and Gson
- **Images**: Glide
- **Local storage**: Room and EncryptedSharedPreferences
- **Background work**: WorkManager and AlarmManager
- **Logging**: Timber
- **UI**: Material Design 3

## Project structure

```
app/src/main/java/com/campus/campusmate/
├── activities/      Screens
├── fragments/       Tab and admin dashboard fragments
├── viewmodels/      MVVM ViewModels
├── repositories/    Data and business logic
├── models/          Data models
├── api/             Retrofit Supabase API and DTOs
├── database/        Room cache
├── services/        Sync and realtime services
├── notifications/   Local notification helpers
├── receivers/       Broadcast receivers (e.g. boot)
├── adapters/        RecyclerView adapters
├── security/        Encryption and validation
└── utils/           Helpers and constants

supabase/
├── schema.sql       Full database schema, RLS policies and storage bucket
├── *.sql            Incremental migrations for existing projects
└── functions/       Edge Functions (create-display-account, delete-user)
```

## Getting started

### Prerequisites

- Android Studio (latest stable) with JDK 17+
- A Supabase project
- The [Supabase CLI](https://supabase.com/docs/guides/cli) for deploying Edge Functions (optional; you can also paste them into the dashboard)

### 1. Set up Supabase

1. In your Supabase project, open **SQL Editor**, paste `supabase/schema.sql` and run it. This creates the tables, Row Level Security policies and the public `campusmate` storage bucket.
2. Under **Authentication → Providers → Email**, turn **off** "Confirm email". Registration creates the user's profile row with the session returned by sign-up, so it needs that session right away.
3. Deploy the Edge Functions:
   ```sh
   supabase functions deploy create-display-account
   supabase functions deploy delete-user
   ```
   Keep the default JWT verification. Supabase provides the service-role key to the functions automatically, so it never ships in the app.
4. Create the first admin account. Add a user under **Authentication → Users**, copy its UID, then run:
   ```sql
   insert into public.users (uid, email, role, first_name, last_name, status, email_verified, created_at)
   values ('<ADMIN_AUTH_UID>', 'admin@example.edu', 'admin', 'Admin', 'User', 'approved', true,
           extract(epoch from now())::bigint * 1000);
   ```
   After that, the admin can create Live Display accounts from the dashboard.

The other `.sql` files in `supabase/` are migrations for projects created from an older schema. A fresh project only needs `schema.sql`.

### 2. Point the app at your project

Set your project URL and anon key in `app/src/main/java/com/campus/campusmate/utils/Constants.java`:

```java
public static final String SUPABASE_URL = "https://<your-project>.supabase.co";
public static final String SUPABASE_ANON_KEY = "<your-anon-key>";
```

The anon key is safe to ship in the app because access is enforced by Row Level Security. Never put the service-role key in the app.

### 3. Build and run

Open the project in Android Studio and run the `app` configuration, or build from the command line:

```sh
./gradlew assembleDebug
```

Gradle advances the app version in `app/version.properties` after every successful build.

### Tests

```sh
./gradlew test                  # unit tests
./gradlew connectedAndroidTest  # instrumented tests (needs a device or emulator)
```

## Security

- Supabase Auth with JWT sessions, plus role-based access control
- Row Level Security on every table, and folder-level storage policies (for example, profile photos are owner-only and department logos are admin-only)
- Privileged operations, such as creating or deleting accounts, run in server-side Edge Functions
- Encrypted local preferences and HTTPS-only communication
- Audit logs for admin actions
