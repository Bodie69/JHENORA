-- Migration: revert "student org leader can post without role change".
-- Restores can_post() to faculty/organization/admin only, and fixes up any
-- student rows that were assigned as an org leader while that feature was
-- active (they keep organization_id/organization_name, but their role is
-- promoted to 'organization' so they match the rest of the app's role model).
-- Run this once in the Supabase SQL Editor against your existing project.

create or replace function public.can_post()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select role in ('faculty','organization','admin')
  from public.users where uid = auth.uid();
$$;

update public.users
set role = 'organization'
where role = 'student'
  and organization_id is not null
  and organization_id <> '';
