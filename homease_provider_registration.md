# HomEase - Service Provider Registration Specification

## Overview
This specification details the provider registration flow, collecting personal, professional, and verification information to support manual review and approval before any provider can receive job matches or dispatch notifications.

## 1. Database Schema (`service_providers` Table)

```sql
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
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
```

## 2. Storage Bucket (`provider-documents`)
- **Bucket ID**: `provider-documents`
- **Visibility**: Private (`public: false`)
- **Purpose**: Holds sensitive identity documents (CNIC front/back), profile photos, and optional business shop photos.
- **File Hierarchy**:
  - `avatars/{phone}_{timestamp}.jpg`
  - `cnic_front/{phone}_{timestamp}.jpg`
  - `cnic_back/{phone}_{timestamp}.jpg`
  - `business/{phone}_{timestamp}.jpg`
- The database table stores the storage object path or signed/canonical URL, never the binary file content directly.

## 3. Account Lifecycle & Status Transitions
- **`pending`**: Default status set upon registration submission.
  - Live job matching is **disabled**.
  - Pings and notifications are **suppressed**.
  - Provider sees "Under Review (Within 24 Hours)" dashboard banner.
- **`approved`**: Provider has been vetted by admin. Live job matching and dispatching activated.
- **`rejected`**: Admin rejected application (e.g., blurry CNIC). Provider is prompted to resubmit.

## 4. Verification Policy
- Police character certificate and formal trade licenses are not required at initial registration; they may be added as optional trust badge upgrades post-approval.
