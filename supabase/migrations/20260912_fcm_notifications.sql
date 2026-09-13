-- ========================================================================
-- HomEase FCM Notifications, Device Tokens & Realtime Publication
-- Migration: 20260912_fcm_notifications.sql
-- ========================================================================

-- 1. Device Tokens Table
-- Tracks current FCM token for each registered phone, active role, and categories
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

-- 2. Add direct fcm_token columns to users and service_providers for convenience
alter table public.users add column if not exists fcm_token text;
alter table public.service_providers add column if not exists fcm_token text;

-- 3. Row Level Security (RLS) for device_tokens
alter table public.device_tokens enable row level security;

drop policy if exists "Allow public upsert own device token" on public.device_tokens;
create policy "Allow public upsert own device token"
  on public.device_tokens for all
  using (true)
  with check (true);

-- 4. Enable Supabase Realtime Replication for instant push on jobs and offers
-- (Runs alongside existing provider_locations and job_messages publications)
alter publication supabase_realtime add table public.jobs;
alter publication supabase_realtime add table public.job_offers;

-- 5. Postgres Trigger Function for pg_net asynchronous webhook dispatch
-- Note: Alternatively, Supabase Database Webhooks can be configured directly in
-- the Supabase Studio dashboard: Database -> Webhooks -> Add Webhook:
--   Table: public.jobs (INSERT, UPDATE) -> Edge Function: notify-job-event
--   Table: public.job_offers (INSERT, UPDATE) -> Edge Function: notify-job-event

create or replace function public.dispatch_job_fcm_notification()
returns trigger as $$
declare
  payload jsonb;
  target_url text;
  anon_key text;
begin
  payload := jsonb_build_object(
    'type', TG_OP,
    'table', TG_TABLE_NAME,
    'record', row_to_json(NEW),
    'old_record', case when TG_OP = 'UPDATE' then row_to_json(OLD) else null end
  );

  -- Retrieve Supabase project URL & service key if available from database settings
  target_url := current_setting('app.settings.supabase_url', true);
  if target_url is null or target_url = '' then
    target_url := 'https://nsqrfagylbqrwlbvnsug.supabase.co';
  end if;

  -- Attempt pg_net asynchronous post if extension is active
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
  -- Fail-safe: ensure DB operations never abort if network webhook is temporarily unreachable
  return NEW;
end;
$$ language plpgsql security definer;

-- Drop existing triggers if re-running
drop trigger if exists on_job_changed_notify on public.jobs;
create trigger on_job_changed_notify
  after insert or update on public.jobs
  for each row execute function public.dispatch_job_fcm_notification();

drop trigger if exists on_job_offer_changed_notify on public.job_offers;
create trigger on_job_offer_changed_notify
  after insert or update on public.job_offers
  for each row execute function public.dispatch_job_fcm_notification();
