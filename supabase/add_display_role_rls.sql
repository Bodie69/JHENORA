-- Lets the Live Display kiosk account (role 'live_display') read every approved
-- announcement/event, not just General-visibility ones.
--
-- Context: the kiosk is campus-wide, not tied to one department, so its account has no
-- department of its own. The existing announcements_select/events_select policies fall
-- back to `department = current_department()` for non-admin, non-author reads — but
-- current_department() is null for a department-less account, and `department = null` is
-- never true in SQL, so a real (non-admin) kiosk account could only ever see
-- General-visibility posts no matter what the client asked for. This adds an is_display()
-- bypass (read-only: still gated on status = 'approved', unlike is_admin()'s full access)
-- so department-scoped posts show up too, labeled by their own department badge in the app.
--
-- Run this ONCE in the Supabase SQL Editor against the live project. Safe to run against
-- an existing project: idempotent (create-or-replace / drop-if-exists-then-create).
create or replace function public.is_display()
returns boolean
language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from public.users where uid = auth.uid() and role = 'live_display'
    );
$$;

drop policy if exists "announcements_select" on public.announcements;
create policy "announcements_select"
    on public.announcements for select
    to authenticated
    using (
        public.is_admin()
        or (public.is_display() and status = 'approved')
        or author_id = auth.uid()
        or (status = 'approved' and (visibility = 'general' or department = public.current_department()))
    );

drop policy if exists "events_select" on public.events;
create policy "events_select"
    on public.events for select
    to authenticated
    using (
        public.is_admin()
        or (public.is_display() and status = 'approved')
        or author_id = auth.uid()
        or (status = 'approved' and (visibility = 'general' or department = public.current_department()))
    );
