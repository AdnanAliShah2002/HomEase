-- ============================================================================
-- HOMEEASE: IN-APP MESSAGING & VOICE CALLING SCHEMA & RLS POLICIES
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

-- 2. Call Logs Table (for optional call history and duration records)
create table if not exists public.call_logs (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,
  caller_id uuid not null,
  started_at timestamptz not null default now(),
  ended_at timestamptz,
  duration_seconds int
);

create index if not exists idx_call_logs_job on public.call_logs(job_id);

-- Enable Row Level Security (RLS) on call logs
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
