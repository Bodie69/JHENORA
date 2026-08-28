# JHENORA Maintenance Plan

This document outlines the maintenance procedures for the JHENORA Android application and its Supabase backend.

## 1. Backend Maintenance (Supabase)
### Database Management
- **Backups**: Supabase performs daily automatic backups. Monthly manual exports of the `announcements` and `events` tables are recommended for redundancy.
- **Scaling**: Monitor database usage through the Supabase Dashboard. If the number of concurrent users exceeds 500, consider upgrading to a Pro plan.
- **Row Level Security (RLS)**: Audit RLS policies quarterly to ensure student accounts cannot modify announcements or events.

### Storage Management
- **Cleanup**: Periodically remove images associated with rejected or archived posts from the `JHENORA` bucket.
- **Optimization**: Ensure images are compressed before upload (handled by `ImageUtils` in the app).

## 2. App Maintenance
### Bug Tracking & Reporting
- Use **Firebase Crashlytics** (once integrated in production) to monitor app stability.
- Maintain a feedback loop for students and faculty to report UI issues.

### Regular Updates
- **Dependencies**: Check for updates to Retrofit, Glide, and AndroidX libraries every 3 months.
- **Security**: Regularly rotate Supabase API keys if any breach is suspected.
- **API Versioning**: Ensure the `SupabaseApi` interface remains compatible with any changes to the PostgreSQL schema.

## 3. Deployment & Release Cycle
### Versioning
- Follow Semantic Versioning (MAJOR.MINOR.PATCH).
- Major: Significant UI overhaul or backend migration.
- Minor: New features (e.g., Organization chat, analytics).
- Patch: Bug fixes and security patches.

### Quality Assurance (QA)
- Run all unit tests (`./gradlew test`) and instrumented tests (`./gradlew connectedCheck`) before every release.
- Perform User Acceptance Testing (UAT) with a small group of faculty members for new features.

## 4. Support
- **Admin Training**: Provide training for designated university administrators on using the Admin Dashboard.
- **Faculty Verification**: Designate one admin specifically for verifying new faculty accounts to prevent unauthorized posts.
