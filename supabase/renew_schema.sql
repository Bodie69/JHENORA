-- CampusMate Database Renewal Schema
-- This script will drop existing tables and recreate them from scratch.
-- Execute this in your Supabase SQL Editor.

-- 1. Drop existing tables if they exist
DROP TABLE IF EXISTS public.audit_logs;
DROP TABLE IF EXISTS public.announcements;
DROP TABLE IF EXISTS public.events;
DROP TABLE IF EXISTS public.organizations;
DROP TABLE IF EXISTS public.users;

-- 2. Create Users Table
CREATE TABLE public.users (
    uid uuid REFERENCES auth.users NOT NULL PRIMARY KEY,
    email text UNIQUE NOT NULL,
    role text DEFAULT 'student' CHECK (role IN ('admin', 'faculty', 'student', 'organization', 'live_display')),
    first_name text NOT NULL,
    middle_name text,
    last_name text NOT NULL,
    department text,
    profile_image_url text,
    status text DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    email_verified boolean DEFAULT false,
    created_at bigint DEFAULT (extract(epoch from now()) * 1000),
    block text,
    year_level text,
    address text,
    profession text,
    organization_name text,
    organization_description text,
    organization_id uuid
);

-- 3. Create Announcements Table
CREATE TABLE public.announcements (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    title text NOT NULL,
    what text NOT NULL,
    who text,
    "when" text,
    "where" text,
    why text,
    additional_info text,
    department text,
    visibility text DEFAULT 'General' CHECK (visibility IN ('General', 'Department Only')),
    priority text DEFAULT 'NORMAL' CHECK (priority IN ('NORMAL', 'HIGH', 'URGENT')),
    status text DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    author_id uuid REFERENCES public.users(uid) ON DELETE CASCADE,
    author_name text,
    created_at bigint DEFAULT (extract(epoch from now()) * 1000),
    updated_at bigint DEFAULT (extract(epoch from now()) * 1000),
    image_url text
);

-- 4. Create Events Table
CREATE TABLE public.events (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    title text NOT NULL,
    description text,
    start_date bigint NOT NULL,
    end_date bigint,
    location text,
    department text,
    visibility text DEFAULT 'General' CHECK (visibility IN ('General', 'Department Only')),
    status text DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    author_id uuid REFERENCES public.users(uid) ON DELETE CASCADE,
    author_name text,
    created_at bigint DEFAULT (extract(epoch from now()) * 1000),
    updated_at bigint DEFAULT (extract(epoch from now()) * 1000),
    image_url text,
    registration_link text
);

-- 5. Create Organizations Table
CREATE TABLE public.organizations (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    name text NOT NULL,
    description text,
    department text,
    leader_id uuid REFERENCES public.users(uid) ON DELETE SET NULL,
    leader_name text,
    status text DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    created_at bigint DEFAULT (extract(epoch from now()) * 1000),
    logo_url text
);

-- 6. Create Audit Logs Table
CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id uuid REFERENCES public.users(uid) ON DELETE SET NULL,
    user_email text,
    action text NOT NULL,
    target_type text NOT NULL,
    target_id text NOT NULL,
    details text,
    created_at bigint DEFAULT (extract(epoch from now()) * 1000)
);

-- 7. Enable Realtime
-- This allows the app to listen for real-time changes
alter publication supabase_realtime add table public.users;
alter publication supabase_realtime add table public.announcements;
alter publication supabase_realtime add table public.events;
alter publication supabase_realtime add table public.organizations;

-- 8. Storage Buckets (Note: These must be created manually in Supabase Dashboard UI under 'Storage')
-- Buckets to create: 'profiles', 'announcements', 'events'

-- 9. Basic RLS (Row Level Security) - Optional but recommended
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public profiles are viewable by everyone" ON public.users FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON public.users FOR UPDATE USING (auth.uid() = uid);

-- 10. Helper trigger to auto-create public.users row on Auth signup
-- Note: You need to pass metadata from the app during signup for this to fill names
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.users (uid, email, first_name, last_name)
  VALUES (NEW.id, NEW.email, '', ''); -- Names are usually updated by the app repository right after signup
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger should be enabled ONLY if you don't manually insert the row from the Android app logic.
-- Since the current Android app logic (UserRepository.java) handles the insert,
-- we skip the automatic trigger to avoid conflicts.
