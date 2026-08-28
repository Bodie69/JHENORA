-- Fix: registration (and login) failing with "new row violates row-level
-- security policy for table users" (Postgres error 42501).
--
-- schema.sql already declares RLS disabled on these tables (this app uses a
-- custom users table for auth instead of Supabase Auth, so RLS is not used
-- as an access-control layer here), but the live project's RLS state had
-- drifted back to enabled with no policies defined, which makes every anon
-- insert/select return empty or get rejected. This re-applies the intended
-- state. Safe to run multiple times.
--
-- Run this once in the Supabase SQL Editor against your project
-- (fqctgkwgfkdjiamtyevt).

alter table public.users disable row level security;
alter table public.announcements disable row level security;
alter table public.events disable row level security;
alter table public.organizations disable row level security;
alter table public.notifications disable row level security;
alter table public.audit_logs disable row level security;
