# HomEase Job Completion, History & Profile Specification

## 1. Job Completion + Rating Flow
- Provider taps "Mark Job as Completed" -> Confirmation Dialog ("Mark this job as complete? The customer will be asked to confirm and rate.")
- Provider job status set to `awaiting_customer_confirmation`.
- Active job card clears from provider's live screen and moves to history with status "Completed — awaiting rating".
- Customer receives notification / banner and "How did it go?" screen:
  - Job summary (Service title, provider name, price)
  - Required 1–5 star rating
  - Optional comment box ("Tell others about your experience")
  - "Submit rating" button -> finalizes job as `completed`
  - Secondary option: "Something wrong? Report an issue" (Category dropdown + free text stub form)
  - 48-hour auto-complete fallback without rating if customer does not respond.
- Recalculate and update provider running average rating and total completed jobs count.

## 2. Bookings / Job History Screens
- Bookings tab with "Active" / "Past" filters for both customer and provider roles.
- Customer:
  - Active: Non-completed jobs (searching, accepted, in-progress, awaiting confirmation)
  - Past: Completed jobs with service, provider name, date, price, rating given. Tap for read-only details.
- Provider:
  - Active: Currently active or awaiting-rating jobs
  - Past: Completed jobs with service, customer name, date, price, rating received.
  - "X jobs completed" counter badge at top of Past tab. Tap for read-only details.

## 3. Profile Screens
- Customer profile:
  - Profile photo, Name (editable), Phone (read-only, verified via OTP), City/Area (editable), Notification preference (WhatsApp/SMS/Both), Language switch, Saved addresses list with Add/Delete, Logout, Help/Support.
- Provider profile:
  - Profile photo, Name, Phone (read-only), Rating average & total completed jobs prominently displayed, Verification status badge ("Pending review" / "Verified"), Editable service categories, service area & radius, experience, bio, shop name, Payout method (Cash, JazzCash, EasyPaisa with account number), Language switch, Logout, Help/Support.

## 4. Supabase Schema Additions
```sql
alter table jobs add column status text not null default 'searching';
-- statuses: searching | accepted | in_progress | awaiting_customer_confirmation | completed | cancelled

create table job_ratings (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references jobs(id),
  provider_id uuid not null references service_providers(id),
  customer_id uuid not null,
  rating int not null check (rating between 1 and 5),
  comment text,
  created_at timestamptz not null default now()
);

-- add these to service_providers if not already present
alter table service_providers add column avg_rating numeric(2,1) default 0;
alter table service_providers add column total_jobs int default 0;

create or replace function update_provider_rating() returns trigger as $$
begin
  update service_providers
  set avg_rating = (select avg(rating) from job_ratings where provider_id = new.provider_id),
      total_jobs = (select count(*) from job_ratings where provider_id = new.provider_id)
  where id = new.provider_id;
  return new;
end;
$$ language plpgsql;

create trigger on_job_rating_insert
after insert on job_ratings
for each row execute function update_provider_rating();
```
