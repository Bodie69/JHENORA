-- Migrate CampusMate from custom table-based "auth" (plaintext password column,
-- RLS disabled everywhere) to real Supabase Auth + Row Level Security.
--
-- Context: previously the app checked a plaintext public.users.password column
-- client-side using only the public anon key, with RLS disabled on every table.
-- That meant anyone holding the anon key (shipped in the APK) could read every
-- user's plaintext password and write/delete any row in any table. From now on,
-- Supabase Auth (auth.users) is the real, sole login mechanism, and RLS is the
-- actual access-control layer, enforced by Postgres regardless of what the
-- client sends.
--
-- Run this ONCE in the Supabase SQL Editor against the live project
-- (fqctgkwgfkdjiamtyevt). This is a clean-cutover migration: existing rows are
-- treated as disposable test/dev data and wiped (see step 0). Do NOT run step 0
-- against a project with real user accounts you need to keep.
--
-- Prerequisite: Authentication -> Providers -> Email -> "Confirm email" must be
-- OFF (schema.sql already calls this out), otherwise sign-up won't return a
-- session/access-token immediately and the registration insert below will fail.

-- ============================================================
-- 0. CLEAN CUTOVER: wipe existing test/dev data (irreversible)
-- ============================================================
truncate table
    public.audit_logs,
    public.notifications,
    public.organizations,
    public.events,
    public.announcements,
    public.users
    cascade;

delete from auth.users;

-- ============================================================
-- 1. Drop the plaintext password column; tie uid to auth.users
-- ============================================================
alter table public.users drop column if exists password;

alter table public.users alter column uid drop default;

alter table public.users drop constraint if exists users_uid_fkey;
alter table public.users
    add constraint users_uid_fkey foreign key (uid) references auth.users(id) on delete cascade;

-- ============================================================
-- 2. Helper functions (security definer + stable: bypass RLS
--    internally so they can be safely used inside policies on
--    public.users itself without recursing into RLS).
-- ============================================================
create or replace function public.is_admin()
returns boolean
language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from public.users where uid = auth.uid() and role = 'admin'
    );
$$;

-- Supersedes the definition in migration_org_leader_can_post.sql: also
-- requires faculty/organization accounts to be approved (matches
-- RoleManager.canCreateAnnouncement / canCreateEvent on the client).
create or replace function public.can_post()
returns boolean
language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from public.users
        where uid = auth.uid()
          and (role = 'admin' or (role in ('faculty','organization') and status = 'approved'))
    );
$$;

create or replace function public.current_department()
returns text
language sql stable security definer set search_path = public
as $$
    select department from public.users where uid = auth.uid();
$$;

-- ============================================================
-- 3. Triggers: block non-admins from changing privileged columns
--    even on rows their UPDATE policy otherwise allows.
-- ============================================================
create or replace function public.prevent_privilege_escalation()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
    if not public.is_admin() then
        if new.role is distinct from old.role or new.status is distinct from old.status then
            raise exception 'Only admins can change role or status';
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists trg_users_prevent_privilege_escalation on public.users;
create trigger trg_users_prevent_privilege_escalation
    before update on public.users
    for each row execute function public.prevent_privilege_escalation();

create or replace function public.prevent_status_change_by_non_admin()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
    if not public.is_admin() then
        if new.status is distinct from old.status
           or new.approved_by is distinct from old.approved_by
           or new.approved_at is distinct from old.approved_at then
            raise exception 'Only admins can change approval status';
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists trg_announcements_prevent_status_change on public.announcements;
create trigger trg_announcements_prevent_status_change
    before update on public.announcements
    for each row execute function public.prevent_status_change_by_non_admin();

drop trigger if exists trg_events_prevent_status_change on public.events;
create trigger trg_events_prevent_status_change
    before update on public.events
    for each row execute function public.prevent_status_change_by_non_admin();

-- ============================================================
-- 4. Enable RLS and define policies
-- ============================================================
alter table public.users enable row level security;
alter table public.announcements enable row level security;
alter table public.events enable row level security;
alter table public.organizations enable row level security;
alter table public.notifications enable row level security;
alter table public.audit_logs enable row level security;

-- Drop existing policies if re-running this script
drop policy if exists "users_select_self_or_admin" on public.users;
drop policy if exists "users_insert_self" on public.users;
drop policy if exists "users_update_self_or_admin" on public.users;

drop policy if exists "announcements_select" on public.announcements;
drop policy if exists "announcements_insert_own" on public.announcements;
drop policy if exists "announcements_update_own_or_admin" on public.announcements;
drop policy if exists "announcements_delete_own_or_admin" on public.announcements;

drop policy if exists "events_select" on public.events;
drop policy if exists "events_insert_own" on public.events;
drop policy if exists "events_update_own_or_admin" on public.events;
drop policy if exists "events_delete_own_or_admin" on public.events;

drop policy if exists "organizations_select" on public.organizations;
drop policy if exists "organizations_admin_insert" on public.organizations;
drop policy if exists "organizations_admin_update" on public.organizations;
drop policy if exists "organizations_admin_delete" on public.organizations;

drop policy if exists "notifications_select_own" on public.notifications;
drop policy if exists "notifications_update_own" on public.notifications;

drop policy if exists "audit_logs_insert_own" on public.audit_logs;
drop policy if exists "audit_logs_select_admin" on public.audit_logs;

-- users
create policy "users_select_self_or_admin"
    on public.users for select
    to authenticated
    using (auth.uid() = uid or public.is_admin());

create policy "users_insert_self"
    on public.users for insert
    to authenticated
    with check (auth.uid() = uid);

create policy "users_update_self_or_admin"
    on public.users for update
    to authenticated
    using (auth.uid() = uid or public.is_admin())
    with check (auth.uid() = uid or public.is_admin());

-- announcements
create policy "announcements_select"
    on public.announcements for select
    to authenticated
    using (
        public.is_admin()
        or author_id = auth.uid()
        or (status = 'approved' and (visibility = 'general' or department = public.current_department()))
    );

create policy "announcements_insert_own"
    on public.announcements for insert
    to authenticated
    with check (author_id = auth.uid() and public.can_post());

create policy "announcements_update_own_or_admin"
    on public.announcements for update
    to authenticated
    using (public.is_admin() or author_id = auth.uid())
    with check (public.is_admin() or author_id = auth.uid());

create policy "announcements_delete_own_or_admin"
    on public.announcements for delete
    to authenticated
    using (public.is_admin() or author_id = auth.uid());

-- events
create policy "events_select"
    on public.events for select
    to authenticated
    using (
        public.is_admin()
        or author_id = auth.uid()
        or (status = 'approved' and (visibility = 'general' or department = public.current_department()))
    );

create policy "events_insert_own"
    on public.events for insert
    to authenticated
    with check (author_id = auth.uid() and public.can_post());

create policy "events_update_own_or_admin"
    on public.events for update
    to authenticated
    using (public.is_admin() or author_id = auth.uid())
    with check (public.is_admin() or author_id = auth.uid());

create policy "events_delete_own_or_admin"
    on public.events for delete
    to authenticated
    using (public.is_admin() or author_id = auth.uid());

-- organizations (CRUD is admin-only today; leader_id is just a field admins set)
create policy "organizations_select"
    on public.organizations for select
    to authenticated
    using (public.is_admin() or status = 'approved');

create policy "organizations_admin_insert"
    on public.organizations for insert
    to authenticated
    with check (public.is_admin());

create policy "organizations_admin_update"
    on public.organizations for update
    to authenticated
    using (public.is_admin())
    with check (public.is_admin());

create policy "organizations_admin_delete"
    on public.organizations for delete
    to authenticated
    using (public.is_admin());

-- notifications (table exists but is not queried anywhere in the app today;
-- closing the hole rather than enabling a feature)
create policy "notifications_select_own"
    on public.notifications for select
    to authenticated
    using (user_id = auth.uid() or public.is_admin());

create policy "notifications_update_own"
    on public.notifications for update
    to authenticated
    using (user_id = auth.uid() or public.is_admin())
    with check (user_id = auth.uid() or public.is_admin());

-- audit_logs (every logAudit() call site logs the acting user, never a target)
create policy "audit_logs_insert_own"
    on public.audit_logs for insert
    to authenticated
    with check (user_id = auth.uid());

create policy "audit_logs_select_admin"
    on public.audit_logs for select
    to authenticated
    using (public.is_admin());

-- ============================================================
-- 5. Manual step: create the admin and live_display accounts
-- ============================================================
-- The app has no in-app flow for creating admin/display accounts (they're
-- operational accounts, not self-registered). After running the above:
--
--   1. Supabase Dashboard -> Authentication -> Users -> Add user
--      Create the admin account (email + password), copy its UID.
--   2. Run, with that UID substituted in:
--
--      insert into public.users (uid, email, role, first_name, last_name, status, email_verified, created_at)
--      values ('<ADMIN_AUTH_UID>', 'admin@example.edu', 'admin', 'Admin', 'User', 'approved', true,
--              extract(epoch from now())::bigint * 1000);
--
--   3. Repeat for the live-display signage account with role 'live_display':
--
--      insert into public.users (uid, email, role, first_name, last_name, status, email_verified, created_at)
--      values ('<DISPLAY_AUTH_UID>', 'display@example.edu', 'live_display', 'Live', 'Display', 'approved', true,
--              extract(epoch from now())::bigint * 1000);
