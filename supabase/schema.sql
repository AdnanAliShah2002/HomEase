-- ========================================================================
-- Supabase Schema for HomEase with Row Level Security (RLS)
-- ========================================================================

-- 1. Users Table (Customers & base user profiles)
create table if not exists public.users (
  id uuid primary key default gen_random_uuid(),
  phone text unique not null,
  full_name text not null,
  city text,
  role text not null default 'customer', -- 'customer', 'provider'
  notification_preference text default 'WhatsApp',
  saved_addresses jsonb default '[]'::jsonb,
  is_verified boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_users_phone on public.users(phone);

-- 2. Service Providers Table
-- Stores personal, professional, and manual verification details
create table if not exists public.service_providers (
  id uuid primary key default gen_random_uuid(),
  phone text unique not null,
  full_name text not null,
  profile_photo_url text,
  cnic_number text not null,
  cnic_front_url text,
  cnic_back_url text,
  date_of_birth text,
  home_address text,
  city_area text,
  service_categories text[], -- e.g. ARRAY['plumbing', 'electrical']
  years_of_experience text,
  service_area text,
  service_radius_km int default 10,
  business_name text,
  business_photo_url text,
  bio text,
  reference_name text,
  reference_phone text,
  payout_method text default 'Cash', -- 'Cash', 'JazzCash', 'EasyPaisa'
  payout_account_number text,
  status text not null default 'pending', -- 'pending', 'approved', 'rejected'
  consent_agreed boolean not null default true,
  reviewed_by text,
  reviewed_at timestamptz,
  avg_rating numeric(2,1) default 0,
  total_jobs int default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_service_providers_phone on public.service_providers(phone);
create index if not exists idx_service_providers_status on public.service_providers(status);

-- 3. Jobs Table
-- statuses: searching | accepted | in_progress | awaiting_customer_confirmation | completed | cancelled
create table if not exists public.jobs (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid references public.users(id),
  customer_phone text not null,
  customer_name text not null,
  category text not null,
  category_id text not null,
  service_title text not null,
  description text,
  city_area text not null,
  full_address text not null,
  budget_rs int not null,
  agreed_price_rs int default 0,
  provider_id uuid references public.service_providers(id),
  provider_phone text,
  provider_name text,
  status text not null default 'searching',
  created_at timestamptz not null default now(),
  completed_at timestamptz
);

create index if not exists idx_jobs_customer on public.jobs(customer_id);
create index if not exists idx_jobs_provider on public.jobs(provider_id);
create index if not exists idx_jobs_status on public.jobs(status);

-- 4. Job Ratings Table
create table if not exists public.job_ratings (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id),
  provider_id uuid not null references public.service_providers(id),
  customer_id uuid not null references public.users(id),
  provider_phone text,
  customer_phone text,
  rating int not null check (rating between 1 and 5),
  comment text,
  created_at timestamptz not null default now()
);

create index if not exists idx_job_ratings_provider on public.job_ratings(provider_id);
create index if not exists idx_job_ratings_customer on public.job_ratings(customer_id);

-- Trigger to automatically recalculate average rating and total jobs count
create or replace function public.update_provider_rating() returns trigger as $$
begin
  update public.service_providers
  set avg_rating = coalesce((select round(avg(rating)::numeric, 1) from public.job_ratings where provider_id = new.provider_id), 0),
      total_jobs = coalesce((select count(*) from public.job_ratings where provider_id = new.provider_id), 0)
  where id = new.provider_id;
  return new;
end;
$$ language plpgsql;

drop trigger if exists on_job_rating_insert on public.job_ratings;
create trigger on_job_rating_insert
after insert on public.job_ratings
for each row execute function public.update_provider_rating();

-- 5. OTP verification codes table
-- Handled strictly server-side by Edge Functions via service role key
create table if not exists public.otp_codes (
  id uuid primary key default gen_random_uuid(),
  phone text not null,
  code_hash text not null,
  expires_at timestamptz not null,
  attempts int not null default 0,
  created_at timestamptz not null default now()
);

create index if not exists idx_otp_phone on public.otp_codes(phone);

-- 6. Private Storage Bucket for provider documents
insert into storage.buckets (id, name, public)
values ('provider-documents', 'provider-documents', false)
on conflict (id) do nothing;

create policy "Allow upload to provider-documents"
  on storage.objects for insert
  with check (bucket_id = 'provider-documents');

create policy "Allow read provider-documents"
  on storage.objects for select
  using (bucket_id = 'provider-documents');

-- ========================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ========================================================================

-- Enable Row Level Security on every table
alter table public.users enable row level security;
alter table public.service_providers enable row level security;
alter table public.jobs enable row level security;
alter table public.job_ratings enable row level security;
alter table public.otp_codes enable row level security;

-- ------------------------------------------------------------------------
-- 1. `users` (customers) Policies
-- ------------------------------------------------------------------------
drop policy if exists "Users can view own profile" on public.users;
create policy "Users can view own profile"
on public.users for select
using (auth.uid() = id);

drop policy if exists "Users can update own profile" on public.users;
create policy "Users can update own profile"
on public.users for update
using (auth.uid() = id);

drop policy if exists "Users can insert own profile" on public.users;
create policy "Users can insert own profile"
on public.users for insert
with check (auth.uid() = id);

-- ------------------------------------------------------------------------
-- 2. `service_providers` Policies & Safe Public View
-- ------------------------------------------------------------------------
drop policy if exists "Providers can view own profile" on public.service_providers;
create policy "Providers can view own profile"
on public.service_providers for select
using (auth.uid() = id);

drop policy if exists "Providers can update own profile" on public.service_providers;
create policy "Providers can update own profile"
on public.service_providers for update
using (auth.uid() = id)
with check (auth.uid() = id);

-- Prevent providers from altering their own status directly via trigger instead of recursive RLS
create or replace function public.prevent_provider_self_approval()
returns trigger as $$
begin
  if new.status is distinct from old.status and (auth.jwt() ->> 'role') != 'service_role' then
    new.status := old.status;
  end if;
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists trg_prevent_provider_self_approval on public.service_providers;
create trigger trg_prevent_provider_self_approval
before update on public.service_providers
for each row execute function public.prevent_provider_self_approval();

drop policy if exists "Providers can insert own profile" on public.service_providers;
create policy "Providers can insert own profile"
on public.service_providers for insert
with check (auth.uid() = id);

drop policy if exists "Customers can view approved provider public info" on public.service_providers;
create policy "Customers can view approved provider public info"
on public.service_providers for select
using (status = 'approved');

-- Public view exposing only safe public fields for customers browsing providers
-- Sensitive fields (CNIC number, CNIC photos, reference info, payout details) are hidden
create or replace view public.public_provider_profiles as
select id, full_name, profile_photo_url, service_categories, avg_rating, total_jobs, bio, business_name
from public.service_providers
where status = 'approved';

-- ------------------------------------------------------------------------
-- 3. `jobs` Policies
-- ------------------------------------------------------------------------
drop policy if exists "Customers can view own jobs" on public.jobs;
create policy "Customers can view own jobs"
on public.jobs for select
using (auth.uid() = customer_id);

drop policy if exists "Customers can insert own jobs" on public.jobs;
create policy "Customers can insert own jobs"
on public.jobs for insert
with check (auth.uid() = customer_id);

drop policy if exists "Customers can update own jobs" on public.jobs;
create policy "Customers can update own jobs"
on public.jobs for update
using (auth.uid() = customer_id);

drop policy if exists "Providers can view assigned jobs" on public.jobs;
create policy "Providers can view assigned jobs"
on public.jobs for select
using (auth.uid() = provider_id);

drop policy if exists "Providers can view open jobs in their category" on public.jobs;
create policy "Providers can view open jobs in their category"
on public.jobs for select
using (
  status = 'searching'
  and exists (
    select 1 from public.service_providers
    where id = auth.uid()
    and status = 'approved'
    and category = any(service_categories)
  )
);

drop policy if exists "Providers can accept open jobs" on public.jobs;
create policy "Providers can accept open jobs"
on public.jobs for update
using (status = 'searching')
with check (auth.uid() = provider_id);

-- ------------------------------------------------------------------------
-- 4. `job_ratings` Policies
-- ------------------------------------------------------------------------
drop policy if exists "Customers can rate own jobs" on public.job_ratings;
create policy "Customers can rate own jobs"
on public.job_ratings for insert
with check (
  auth.uid() = customer_id
  and exists (select 1 from public.jobs where id = job_id and customer_id = auth.uid())
);

drop policy if exists "Providers can view own ratings" on public.job_ratings;
create policy "Providers can view own ratings"
on public.job_ratings for select
using (auth.uid() = provider_id);

drop policy if exists "Customers can view own given ratings" on public.job_ratings;
create policy "Customers can view own given ratings"
on public.job_ratings for select
using (auth.uid() = customer_id);

-- ------------------------------------------------------------------------
-- 5. `otp_codes` Policies
-- ------------------------------------------------------------------------
-- No client-facing policies! Enabling RLS with zero policies ensures that
-- anon and authenticated client roles have ZERO read/write access.
-- Only Edge Functions using the service role key (which bypasses RLS by design)
-- can insert, select, or delete from this table.

-- ============================================================================
-- APP THEMES TABLE (Dynamic Remote Theming)
-- ============================================================================
create table if not exists public.app_themes (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  primary_color text not null,
  accent_color text not null,
  background_color text not null,
  text_color text not null,
  is_active boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_app_themes_is_active on public.app_themes(is_active);

-- Enable RLS
alter table public.app_themes enable row level security;

-- Allow anonymous and authenticated read access for active themes
drop policy if exists "Allow public read of active themes" on public.app_themes;
create policy "Allow public read of active themes"
  on public.app_themes for select
  using (true);

-- Insert default Option D "Coral Sunset" theme
insert into public.app_themes (name, primary_color, accent_color, background_color, text_color, is_active)
values ('Coral Sunset', '#DC5F45', '#2A9D8F', '#FFFBF7', '#292524', true)
on conflict do nothing;

-- ============================================================================
-- REAL-TIME PROVIDER LOCATIONS (InDrive-style Live Tracking)
-- ============================================================================

-- Extend jobs table with status timestamp tracking
alter table public.jobs add column if not exists status_updated_at timestamptz;

-- provider_locations table: one row per active job, continuously overwritten
create table if not exists public.provider_locations (
  job_id uuid primary key references public.jobs(id) on delete cascade,
  provider_id uuid not null references public.service_providers(id),
  lat numeric not null,
  lng numeric not null,
  heading numeric, -- direction of travel in degrees
  updated_at timestamptz not null default now()
);

create index if not exists idx_provider_locations_provider on public.provider_locations(provider_id);

-- Enable Row Level Security (RLS)
alter table public.provider_locations enable row level security;

-- Provider can update own location
drop policy if exists "Provider can update own location" on public.provider_locations;
create policy "Provider can update own location"
on public.provider_locations for all
using (auth.uid() = provider_id)
with check (auth.uid() = provider_id);

-- Customer can view location for their own job
drop policy if exists "Customer can view location for their own job" on public.provider_locations;
create policy "Customer can view location for their own job"
on public.provider_locations for select
using (
  exists (select 1 from public.jobs where id = job_id and customer_id = auth.uid())
);

-- Enable Supabase Realtime replication on provider_locations
alter publication supabase_realtime add table public.provider_locations;

-- ============================================================================
-- IN-APP MESSAGING & VOICE CALLING (Supabase Realtime & Agora Audio)
-- ============================================================================

-- 1. Job Messages Table
create table if not exists public.job_messages (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,
  sender_id uuid not null,
  sender_type text not null, -- 'customer' | 'provider'
  message text not null,
  created_at timestamptz not null default now(),
  read_at timestamptz
);

create index if not exists idx_messages_job on public.job_messages(job_id, created_at);

-- Enable Row Level Security (RLS)
alter table public.job_messages enable row level security;

-- Policy 1: Participants can view job messages
drop policy if exists "Participants can view job messages" on public.job_messages;
create policy "Participants can view job messages"
on public.job_messages for select
using (
  exists (select 1 from public.jobs where id = job_id and (customer_id = auth.uid() or provider_id = auth.uid()))
);

-- Policy 2: Participants can send job messages
drop policy if exists "Participants can send job messages" on public.job_messages;
create policy "Participants can send job messages"
on public.job_messages for insert
with check (
  sender_id = auth.uid()
  and exists (select 1 from public.jobs where id = job_id and (customer_id = auth.uid() or provider_id = auth.uid()))
);

-- Policy 3: Participants can update read_at timestamp
drop policy if exists "Participants can update read_at on job messages" on public.job_messages;
create policy "Participants can update read_at on job messages"
on public.job_messages for update
using (
  exists (select 1 from public.jobs where id = job_id and (customer_id = auth.uid() or provider_id = auth.uid()))
)
with check (
  exists (select 1 from public.jobs where id = job_id and (customer_id = auth.uid() or provider_id = auth.uid()))
);

-- Enable Realtime replication for instant messaging delivery
alter publication supabase_realtime add table public.job_messages;

-- 2. Call Logs Table
create table if not exists public.call_logs (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,
  caller_id uuid not null,
  started_at timestamptz not null default now(),
  ended_at timestamptz,
  duration_seconds int
);

create index if not exists idx_call_logs_job on public.call_logs(job_id);

alter table public.call_logs enable row level security;

drop policy if exists "Participants can view call logs" on public.call_logs;
create policy "Participants can view call logs"
on public.call_logs for select
using (
  exists (select 1 from public.jobs where id = job_id and (customer_id = auth.uid() or provider_id = auth.uid()))
);

drop policy if exists "Participants can insert call logs" on public.call_logs;
create policy "Participants can insert call logs"
on public.call_logs for insert
with check (
  caller_id = auth.uid()
  and exists (select 1 from public.jobs where id = job_id and (customer_id = auth.uid() or provider_id = auth.uid()))
);

drop policy if exists "Participants can update call logs" on public.call_logs;
create policy "Participants can update call logs"
on public.call_logs for update
using (
  exists (select 1 from public.jobs where id = job_id and (customer_id = auth.uid() or provider_id = auth.uid()))
);

-- ========================================================================
-- FCM PUSH NOTIFICATIONS & DEVICE TOKENS
-- ========================================================================

create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  phone text not null,
  role text not null default 'customer', -- 'customer' | 'provider'
  fcm_token text unique not null,
  categories text[], -- for providers: e.g. ARRAY['plumbing', 'electrical']
  is_online boolean not null default true,
  updated_at timestamptz not null default now()
);

create index if not exists idx_device_tokens_phone on public.device_tokens(phone);
create index if not exists idx_device_tokens_role on public.device_tokens(role);
create index if not exists idx_device_tokens_online on public.device_tokens(is_online);

alter table public.users add column if not exists fcm_token text;
alter table public.service_providers add column if not exists fcm_token text;

alter table public.device_tokens enable row level security;

drop policy if exists "Allow public upsert own device token" on public.device_tokens;
create policy "Allow public upsert own device token"
  on public.device_tokens for all
  using (true)
  with check (true);

-- Enable Realtime for instant jobs & offers notifications
alter publication supabase_realtime add table public.jobs;
alter publication supabase_realtime add table public.job_offers;

-- Trigger Function for notify-job-event Edge Function
create or replace function public.dispatch_job_fcm_notification()
returns trigger as $$
declare
  payload jsonb;
  target_url text;
begin
  payload := jsonb_build_object(
    'type', TG_OP,
    'table', TG_TABLE_NAME,
    'record', row_to_json(NEW),
    'old_record', case when TG_OP = 'UPDATE' then row_to_json(OLD) else null end
  );

  target_url := current_setting('app.settings.supabase_url', true);
  if target_url is null or target_url = '' then
    target_url := 'https://nsqrfagylbqrwlbvnsug.supabase.co';
  end if;

  if exists (select 1 from pg_extension where extname = 'pg_net') then
    perform net.http_post(
      url := target_url || '/functions/v1/notify-job-event',
      headers := jsonb_build_object(
        'Content-Type', 'application/json'
      ),
      body := payload
    );
  end if;

  return NEW;
exception when others then
  return NEW;
end;
$$ language plpgsql security definer;

drop trigger if exists on_job_changed_notify on public.jobs;
create trigger on_job_changed_notify
  after insert or update on public.jobs
  for each row execute function public.dispatch_job_fcm_notification();

drop trigger if exists on_job_offer_changed_notify on public.job_offers;
create trigger on_job_offer_changed_notify
  after insert or update on public.job_offers
  for each row execute function public.dispatch_job_fcm_notification();



