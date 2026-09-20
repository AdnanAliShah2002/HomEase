-- ========================================================================
-- HomEase Job Matching, FCM Token, Online Status & RLS Migration
-- Migration: 20260920_job_matching_fixes.sql
-- ========================================================================

-- 1. Ensure `is_online` column exists on public.service_providers
alter table public.service_providers 
add column if not exists is_online boolean not null default true;

-- Ensure index on is_online
create index if not exists idx_service_providers_online on public.service_providers(is_online);

-- 2. Ensure `fcm_token` column exists on public.service_providers and public.users
alter table public.service_providers 
add column if not exists fcm_token text;

alter table public.users 
add column if not exists fcm_token text;

-- 3. Fix RLS Policy for open jobs matching provider categories
-- Providers can view jobs in 'searching' status if their registered service_categories
-- contains either the canonical category_id OR the category display text.
drop policy if exists "Providers can view open jobs in their category" on public.jobs;

create policy "Providers can view open jobs in their category" on public.jobs
for select
using (
  status = 'searching'
  and exists (
    select 1 from public.service_providers
    where id = auth.uid()
    and status = 'approved'
    and (
      category_id = any(service_categories)
      or category = any(service_categories)
      or exists (
        select 1 from unnest(service_categories) sc
        where lower(sc) = lower(jobs.category_id)
           or lower(sc) = lower(jobs.category)
      )
    )
  )
);

-- 4. Ensure Providers can update their own profile fields (bio, experience, categories, is_online, fcm_token)
drop policy if exists "Providers can update own profile" on public.service_providers;

create policy "Providers can update own profile" on public.service_providers
for update
using (auth.uid() = id or phone = (select phone from public.users where id = auth.uid()))
with check (
  -- Prevent changing status to approved/active by self (must be handled by admin or trigger)
  status = (select sp.status from public.service_providers sp where sp.id = service_providers.id)
  or (select sp.status from public.service_providers sp where sp.id = service_providers.id) = 'approved'
);
