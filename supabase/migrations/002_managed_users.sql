-- ============================================================
-- LedgerOS — Managed user access grants  (idempotent — safe to re-run)
-- Run this in: Supabase dashboard → SQL Editor → Run
-- ============================================================
--
-- Allows the app owner to grant Free / Pro Monthly / Pro Yearly
-- access to any email address.  The grantee sees their tier on
-- the Settings screen (with the original price struck through).
-- The owner sees and manages all grants in the Admin panel.

create table if not exists public.managed_users (
    id           text primary key,               -- grantee email (stored lowercase)
    granted_by   uuid references auth.users not null, -- owner's auth user_id
    tier         text not null default 'Free',   -- 'Free' | 'ProMonthly' | 'ProYearly'
    note         text not null default '',        -- optional label (e.g. "Development partner")
    created_at   timestamptz default now()
);

alter table public.managed_users enable row level security;

-- Owner: full read/write/delete access to every grant they created
drop policy if exists "owner manage grants" on public.managed_users;
create policy "owner manage grants" on public.managed_users
    for all
    using  (auth.uid() = granted_by)
    with check (auth.uid() = granted_by);

-- Grantee: read-only access to their own row
drop policy if exists "grantee read grant" on public.managed_users;
create policy "grantee read grant" on public.managed_users
    for select
    using (lower(id) = lower(auth.email()));
