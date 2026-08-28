-- CampusMate database schema for Supabase (Postgres)
-- Generated to match the JSON field names produced by the Android app's Gson
-- FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES (see SupabaseManager.java) and
-- the @SerializedName overrides on the model classes.
--
-- How to use:
--   1. Open your Supabase project -> SQL Editor -> New query.
--   2. Paste this whole file and run it once, top to bottom.
--   3. In Authentication -> Providers -> Email, turn OFF "Confirm email"
--      (registration inserts the profile row immediately using the session
--      returned by sign-up; if email confirmation is required, sign-up
--      returns no session/access-token yet and the insert below would be
--      rejected by RLS).
--   4. In Storage, this script creates a public bucket named "campusmate".

-- ============================================================
-- Extensions
-- ============================================================
create extension if not exists pgcrypto; -- gen_random_uuid()

-- ============================================================
-- departments
-- ============================================================
create table if not exists public.departments (
    id          uuid primary key default gen_random_uuid(),
    name        text not null unique,
    code        text not null unique,
    description text,
    color_hex   text,
    created_at  bigint not null
);

create index if not exists idx_departments_name on public.departments(name);
create index if not exists idx_departments_code on public.departments(code);

-- Seed departments
insert into public.departments (name, code, color_hex, created_at)
values ('BSIT', 'BSIT', '#007AFF', extract(epoch from now())::bigint * 1000),
       ('BTLED', 'BTLED', '#34C759', extract(epoch from now())::bigint * 1000),
       ('EDUC', 'EDUC', '#FF9500', extract(epoch from now())::bigint * 1000),
       ('General', 'GEN', '#8E8E93', extract(epoch from now())::bigint * 1000)
on conflict (name) do update set
    code = excluded.code,
    color_hex = excluded.color_hex;

-- ============================================================
-- users
-- Mirrors com.campus.campusmate.models.User
-- uid is the same id as the matching auth.users row (Supabase Auth is the
-- real login mechanism; this table holds the profile RLS reads role/status
-- from).
-- ============================================================
create table if not exists public.users (
    uid                       uuid primary key references auth.users(id) on delete cascade,
    email                     text not null unique,
    role                      text not null check (role in ('admin','faculty','student','organization','live_display')),
    first_name                text,
    middle_name               text,
    last_name                 text,
    department                text references public.departments(name),
    profile_image_url         text,
    status                    text not null default 'pending' check (status in ('pending','approved','rejected')),
    email_verified            boolean not null default false,
    created_at                bigint not null,
    updated_at                bigint,

    -- student-only
    block                     text,
    year_level                text check (year_level in ('1st Year','2nd Year','3rd Year','4th Year')),
    address                   text,

    -- faculty-only
    profession                text,

    -- organization-only
    organization_name         text,
    organization_description  text,
    organization_id           text
);

create index if not exists idx_users_role on public.users(role);
create index if not exists idx_users_status on public.users(status);

-- ============================================================
-- announcements
-- Mirrors com.campus.campusmate.models.Announcement
-- ============================================================
create table if not exists public.announcements (
    id                uuid primary key default gen_random_uuid(),
    title             text not null,
    what              text,
    who               text,
    when_info         text,
    where_info        text,
    why               text,
    additional_info   text,
    image_url         text,
    author_id         uuid not null references public.users(uid) on delete cascade,
    author_name       text,
    author_role       text,
    department        text references public.departments(name),
    visibility        text not null default 'general' check (visibility in ('general','department')),
    status            text not null default 'pending' check (status in ('pending','approved','rejected','archived')),
    priority          text not null default 'normal' check (priority in ('normal','high','urgent')),
    category          text,
    rejection_reason  text,
    approved_by       text,
    created_at        bigint not null,
    updated_at        bigint,
    approved_at       bigint
);

create index if not exists idx_announcements_status on public.announcements(status);
create index if not exists idx_announcements_visibility on public.announcements(visibility);
create index if not exists idx_announcements_department on public.announcements(department);
create index if not exists idx_announcements_author on public.announcements(author_id);
create index if not exists idx_announcements_created_at on public.announcements(created_at);

-- ============================================================
-- events
-- Mirrors com.campus.campusmate.models.Event
-- ============================================================
create table if not exists public.events (
    id                  uuid primary key default gen_random_uuid(),
    title               text not null,
    what                text,
    who                 text,
    when_info           text,
    where_info          text,
    why                 text,
    registration_link   text,
    image_url           text,
    author_id           uuid not null references public.users(uid) on delete cascade,
    author_name         text,
    author_role         text,
    department          text references public.departments(name),
    visibility          text not null default 'general' check (visibility in ('general','department')),
    status              text not null default 'pending' check (status in ('pending','approved','rejected','archived')),
    category            text,
    rejection_reason    text,
    approved_by         text,
    start_date          bigint,
    end_date            bigint,
    created_at          bigint not null,
    updated_at          bigint,
    approved_at         bigint
);

create index if not exists idx_events_status on public.events(status);
create index if not exists idx_events_visibility on public.events(visibility);
create index if not exists idx_events_department on public.events(department);
create index if not exists idx_events_author on public.events(author_id);
create index if not exists idx_events_created_at on public.events(created_at);
create index if not exists idx_events_start_date on public.events(start_date);

-- ============================================================
-- organizations
-- Mirrors com.campus.campusmate.models.Organization
-- ============================================================
create table if not exists public.organizations (
    id            uuid primary key default gen_random_uuid(),
    name          text not null,
    description   text,
    leader_id     uuid references public.users(uid) on delete set null,
    leader_name   text,
    department    text references public.departments(name),
    image_url     text,
    status        text not null default 'pending' check (status in ('pending','approved','rejected')),
    created_at    bigint not null
);

create index if not exists idx_organizations_status on public.organizations(status);

-- ============================================================
-- notifications
-- Mirrors com.campus.campusmate.models.AppNotification
-- ============================================================
create table if not exists public.notifications (
    id            uuid primary key default gen_random_uuid(),
    title         text not null,
    message       text,
    type          text check (type in ('announcement','event','approval','registration')),
    reference_id  text,
    user_id       uuid not null references public.users(uid) on delete cascade,
    is_read       boolean not null default false,
    created_at    bigint not null
);

create index if not exists idx_notifications_user on public.notifications(user_id);
create index if not exists idx_notifications_created_at on public.notifications(created_at);

-- ============================================================
-- audit_logs
-- Mirrors com.campus.campusmate.models.AuditLog
-- ============================================================
create table if not exists public.audit_logs (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid,
    user_email   text,
    action       text not null check (action in ('create','update','delete','approve','reject','login','logout')),
    target_type  text not null check (target_type in ('announcement','event','user','organization')),
    target_id    text,
    details      text,
    timestamp    bigint not null
);

create index if not exists idx_audit_logs_user on public.audit_logs(user_id);
create index if not exists idx_audit_logs_timestamp on public.audit_logs(timestamp);

-- ============================================================
-- Security: Row Level Security, enforced via Supabase Auth (auth.uid())
-- ============================================================

-- Helper functions: security definer + stable so they bypass RLS internally
-- (needed to safely query public.users from inside policies on that same
-- table without recursing into RLS).
create or replace function public.is_admin()
returns boolean
language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from public.users where uid = auth.uid() and role = 'admin'
    );
$$;

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

-- Triggers: block non-admins from changing privileged columns even on rows
-- their UPDATE policy otherwise allows.
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

alter table public.users enable row level security;
alter table public.announcements enable row level security;
alter table public.events enable row level security;
alter table public.organizations enable row level security;
alter table public.notifications enable row level security;
alter table public.audit_logs enable row level security;

-- Drop existing policies if they exist (to avoid errors on re-run)
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

-- organizations (CRUD is admin-only; leader_id is just a field admins set)
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

-- notifications (own row only)
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
-- Storage: "campusmate" bucket for post/profile/organization images
-- ============================================================
insert into storage.buckets (id, name, public)
values ('campusmate', 'campusmate', true)
on conflict (id) do update set public = true;

-- Drop existing storage policies if they exist
drop policy if exists "campusmate_public_read" on storage.objects;
drop policy if exists "campusmate_authenticated_upload" on storage.objects;
drop policy if exists "campusmate_authenticated_update" on storage.objects;
drop policy if exists "campusmate_authenticated_delete" on storage.objects;

create policy "campusmate_public_read"
    on storage.objects for select
    using (bucket_id = 'campusmate');

create policy "campusmate_authenticated_upload"
    on storage.objects for insert
    to authenticated
    with check (bucket_id = 'campusmate');

create policy "campusmate_authenticated_update"
    on storage.objects for update
    to authenticated
    using (bucket_id = 'campusmate')
    with check (bucket_id = 'campusmate');

create policy "campusmate_authenticated_delete"
    on storage.objects for delete
    to authenticated
    using (bucket_id = 'campusmate');
