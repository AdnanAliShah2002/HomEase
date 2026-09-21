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

-- 5. Ensure `public.job_offers` table exists with complete columns and RLS
create table if not exists public.job_offers (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,
  provider_id uuid references public.service_providers(id),
  provider_phone text not null,
  provider_name text not null,
  offer_price_rs int not null,
  counter_price_rs int,
  distance_km double precision default 1.5,
  provider_rating numeric(2,1) default 4.8,
  offer_note text,
  status text not null default 'pending', -- pending, accepted, rejected, withdrawn
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_job_offers_job_id on public.job_offers(job_id);
create index if not exists idx_job_offers_provider_phone on public.job_offers(provider_phone);
create index if not exists idx_job_offers_status on public.job_offers(status);

alter table public.job_offers enable row level security;

drop policy if exists "Allow reading job offers" on public.job_offers;
create policy "Allow reading job offers" on public.job_offers
  for select using (true);

drop policy if exists "Allow inserting job offers" on public.job_offers;
create policy "Allow inserting job offers" on public.job_offers
  for insert with check (true);

drop policy if exists "Allow updating job offers" on public.job_offers;
create policy "Allow updating job offers" on public.job_offers
  for update using (true) with check (true);

do $$
begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime'
    and schemaname = 'public'
    and tablename = 'job_offers'
  ) then
    alter publication supabase_realtime add table public.job_offers;
  end if;
end $$;
