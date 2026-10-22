-- Adds a real, structured date/time to announcements.
--
-- Context: announcements only ever had `when_info` (free text, e.g. "next Monday" or
-- nothing at all), so the Live Display kiosk's "time bomb" couldn't expire an
-- announcement based on what it's actually about — it could only guess, using a fixed
-- 7-day age cutoff from created_at. Events already have this (start_date/end_date);
-- this brings announcements to parity so CreateAnnouncementActivity can offer a real
-- date/time picker, and LiveDisplayActivity can expire an announcement once its own
-- event_date has passed, falling back to the old age-based cutoff when event_date is
-- unset (0) — e.g. general FYI announcements with no specific date.
--
-- Safe to run against an existing project: idempotent, additive only.
alter table public.announcements
    add column if not exists event_date bigint;
