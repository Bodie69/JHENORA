-- Makes profile photos owner-only in the "campusmate" storage bucket.
--
-- Previously any signed-in user could upload, overwrite or delete any object outside
-- departments/, and profile photos lived at a predictable path (profiles/profile_<uid>.jpg)
-- uploaded with x-upsert, so one user could replace another user's photo.
--
-- The app now stores each photo at profiles/<uid>/<random>.jpg. Writes under profiles/ are
-- allowed only inside the caller's own <uid> folder (admins may manage any). The caller may
-- also delete their own legacy profiles/profile_<uid>.jpg so the app can clean it up.
-- departments/ stays admin-only (see restrict_department_logo_storage.sql); other folders
-- (announcements/, events/, organizations/) keep their existing behaviour. Public read is
-- unchanged.
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
                and (name not like 'departments/%' or public.is_admin())
                and (name not like 'profiles/%'
                     or (storage.foldername(name))[2] = auth.uid()::text
                     or public.is_admin()));

create policy "campusmate_authenticated_update"
    on storage.objects for update
    to authenticated
    using (bucket_id = 'campusmate'
           and (name not like 'departments/%' or public.is_admin())
           and (name not like 'profiles/%'
                or (storage.foldername(name))[2] = auth.uid()::text
                or public.is_admin()))
    with check (bucket_id = 'campusmate'
                and (name not like 'departments/%' or public.is_admin())
                and (name not like 'profiles/%'
                     or (storage.foldername(name))[2] = auth.uid()::text
                     or public.is_admin()));

create policy "campusmate_authenticated_delete"
    on storage.objects for delete
    to authenticated
    using (bucket_id = 'campusmate'
           and (name not like 'departments/%' or public.is_admin())
           and (name not like 'profiles/%'
                or (storage.foldername(name))[2] = auth.uid()::text
                or name = 'profiles/profile_' || auth.uid()::text || '.jpg'
                or public.is_admin()));
