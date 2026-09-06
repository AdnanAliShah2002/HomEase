# HomEase — Row Level Security (RLS) Policies

RLS is configured in Supabase directly (Dashboard → Table Editor → or SQL Editor), not something AI Studio's frontend code controls — but AI Studio's app code needs to call Supabase using the authenticated user's session (not the service role key) for these policies to actually take effect. See note at the end.

## Principle for each table
- A user can read/write their own rows, nothing else.
- Providers can only see job data relevant to jobs they're matched to or have accepted — never another provider's jobs or another customer's private info.
- Only edge functions (using the service role key, which bypasses RLS by design) can write to sensitive tables like `otp_codes` or change a provider's `status` from pending to approved.

---

## 1. Enable RLS on every table first

```sql
alter table users enable row level security;
alter table service_providers enable row level security;
alter table jobs enable row level security;
alter table job_ratings enable row level security;
alter table otp_codes enable row level security;
```

## 2. `users` (customers)

```sql
-- customers can read and update only their own row
create policy "Users can view own profile"
on users for select
using (auth.uid() = id);

create policy "Users can update own profile"
on users for update
using (auth.uid() = id);

-- inserts only happen via your registration edge function / signed-in session matching the new id
create policy "Users can insert own profile"
on users for insert
with check (auth.uid() = id);
```

## 3. `service_providers`

```sql
-- providers can view and edit their own profile
create policy "Providers can view own profile"
on service_providers for select
using (auth.uid() = id);

create policy "Providers can update own profile"
on service_providers for update
using (auth.uid() = id)
with check (
  -- prevent providers from approving themselves by editing status
  status = (select status from service_providers where id = auth.uid())
);

create policy "Providers can insert own profile"
on service_providers for insert
with check (auth.uid() = id);

-- customers need to see limited public info of APPROVED providers only (name, rating, category) when browsing offers
create policy "Customers can view approved provider public info"
on service_providers for select
using (status = 'approved');
```

Note: Supabase RLS doesn't do column-level restriction natively — if customers should only see name/rating/category and not CNIC numbers, handle that by exposing a separate `public_provider_view` (a Postgres view with just the safe columns) rather than relying on RLS alone to hide sensitive columns from a full-row select.

```sql
create view public_provider_profiles as
select id, full_name, profile_photo_url, service_categories, avg_rating, total_jobs, bio, business_name
from service_providers
where status = 'approved';
```

## 4. `jobs`

```sql
-- customers can see and manage their own jobs
create policy "Customers can view own jobs"
on jobs for select
using (auth.uid() = customer_id);

create policy "Customers can insert own jobs"
on jobs for insert
with check (auth.uid() = customer_id);

create policy "Customers can update own jobs"
on jobs for update
using (auth.uid() = customer_id);

-- providers can see jobs assigned to them, or unassigned jobs matching their category/area (adjust the matching logic to however your app queries "nearby open jobs")
create policy "Providers can view assigned jobs"
on jobs for select
using (auth.uid() = provider_id);

create policy "Providers can view open jobs in their category"
on jobs for select
using (
  status = 'searching'
  and exists (
    select 1 from service_providers
    where id = auth.uid()
    and status = 'approved'
    and category = any(service_categories)
  )
);

-- a provider can only update a job (accept it) if it's currently unassigned
create policy "Providers can accept open jobs"
on jobs for update
using (status = 'searching')
with check (auth.uid() = provider_id);
```

## 5. `job_ratings`

```sql
-- customers can insert a rating only for their own completed job
create policy "Customers can rate own jobs"
on job_ratings for insert
with check (
  auth.uid() = customer_id
  and exists (select 1 from jobs where id = job_id and customer_id = auth.uid())
);

-- providers can view ratings left for them
create policy "Providers can view own ratings"
on job_ratings for select
using (auth.uid() = provider_id);

-- customers can view ratings they gave
create policy "Customers can view own given ratings"
on job_ratings for select
using (auth.uid() = customer_id);
```

## 6. `otp_codes`

```sql
-- no client access at all — only edge functions using the service role key should ever touch this table
-- (service role bypasses RLS automatically, so no policy needs to grant access;
--  simply enabling RLS with zero policies means the anon/authenticated roles get nothing)
```

---

## Important note for AI Studio

For any of this to actually restrict access, the app's Supabase client calls must use the **user's logged-in session** (the anon key + their auth token after login), not the service role key. The service role key should only ever be used inside edge functions (like `send-otp`/`verify-otp`), never shipped in the mobile app itself. Confirm the app's Supabase client initialization uses the anon key, and that all direct table reads/writes from the app (not through an edge function) are done as the authenticated user, so these policies apply.

After applying these, test manually: try querying another user's job or provider row from the app while logged in as someone else — it should return empty/denied rather than the data.
