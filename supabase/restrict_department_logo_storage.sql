-- Makes the departments/ folder of the "campusmate" storage bucket admin-only.
--
-- Previously any signed-in user could upload, overwrite or delete any object in the bucket,
-- so a student could replace or delete a department logo file even though only admins may
-- change public.departments.logo_url. Other folders (announcements/, events/, profiles/,
-- organizations/) keep their existing behaviour. Public read is unchanged.
--
-- Requires public.is_admin() (created by schema.sql). Run this ONCE in the Supabase SQL
-- Editor against the live project. Safe to re-run: drop-if-exists-then-create.
drop policy if exists "campusmate_authenticated_upload" on storage.objects;
drop policy if exists "campusmate_authenticated_update" on storage.objects;
drop policy if exists "campusmate_authenticated_delete" on storage.objects;

create policy "campusmate_authenticated_upload"
    on storage.objects for insert
    to authenticated
    with check (bucket_id = 'campusmate'
                and (name not like 'departments/%' or public.is_admin()));

create policy "campusmate_authenticated_update"
    on storage.objects for update
    to authenticated
    using (bucket_id = 'campusmate'
           and (name not like 'departments/%' or public.is_admin()))
    with check (bucket_id = 'campusmate'
                and (name not like 'departments/%' or public.is_admin()));

create policy "campusmate_authenticated_delete"
    on storage.objects for delete
    to authenticated
    using (bucket_id = 'campusmate'
           and (name not like 'departments/%' or public.is_admin()));
