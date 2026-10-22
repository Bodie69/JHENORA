-- Lets admins upload a logo for each department (Admin Dashboard > Depts).
--
-- * Adds departments.logo_url (public URL of an image in the "campusmate" bucket under
--   departments/). An empty string means "no logo" — the app writes '' to clear it.
-- * Fills in the full program names as descriptions for the seeded departments.
-- * Allows 'department' as an audit_logs.target_type.
-- * Turns on Row Level Security for departments, which previously had none (so any signed-in
--   user could edit the table): everyone may read, only admins may insert/update/delete.
--   Reads stay open to anon too, since department names are public and the
--   registration screen may list them before sign-in.
--
-- Run this ONCE in the Supabase SQL Editor against the live project. Safe to re-run:
-- idempotent (add-column-if-not-exists / drop-if-exists-then-create).
alter table public.departments add column if not exists logo_url text;

update public.departments set description = 'Bachelor of Science in Information Technology'
    where code = 'BSIT' and description is null;
update public.departments set description = 'Bachelor of Technology and Livelihood Education'
    where code = 'BTLED' and description is null;
update public.departments set description = 'Bachelor of Education'
    where code = 'EDUC' and description is null;
update public.departments set description = 'Campus-wide (all departments)'
    where code = 'GEN' and description is null;

-- Allow 'department' as an audit-log target (logo changes are audited).
alter table public.audit_logs drop constraint if exists audit_logs_target_type_check;
alter table public.audit_logs add constraint audit_logs_target_type_check
    check (target_type in ('announcement','event','user','organization','department'));

alter table public.departments enable row level security;

drop policy if exists "departments_select" on public.departments;
drop policy if exists "departments_admin_insert" on public.departments;
drop policy if exists "departments_admin_update" on public.departments;
drop policy if exists "departments_admin_delete" on public.departments;

create policy "departments_select"
    on public.departments for select
    to anon, authenticated
    using (true);

create policy "departments_admin_insert"
    on public.departments for insert
    to authenticated
    with check (public.is_admin());

create policy "departments_admin_update"
    on public.departments for update
    to authenticated
    using (public.is_admin())
    with check (public.is_admin());

create policy "departments_admin_delete"
    on public.departments for delete
    to authenticated
    using (public.is_admin());
