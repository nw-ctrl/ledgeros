-- ============================================================
-- LedgerOS — Supabase initial schema  (idempotent — safe to re-run)
-- Run this in: Supabase dashboard → SQL Editor → Run
-- ============================================================

-- ── Businesses ────────────────────────────────────────────────
create table if not exists public.businesses (
    id                text primary key,
    user_id           uuid references auth.users not null,
    abn               text not null default '',
    business_name     text not null,
    gst_registered    boolean not null default false,
    bas_frequency     text not null default 'Quarterly',
    created_at        timestamptz default now()
);

alter table public.businesses enable row level security;
drop policy if exists "owner only" on public.businesses;
create policy "owner only" on public.businesses
    for all using (auth.uid() = user_id);

-- ── Receipts ──────────────────────────────────────────────────
create table if not exists public.receipts (
    id                text primary key,
    business_id       text references public.businesses(id) on delete cascade,
    user_id           uuid references auth.users not null,
    merchant          text not null default '',
    receipt_date      date,
    total             numeric not null default 0,
    gst               numeric not null default 0,
    category          text not null default 'Uncategorised',
    file_url          text not null default '',
    ocr_confidence    numeric not null default 0,
    ai_confidence     numeric not null default 0,
    created_at        timestamptz default now(),
    updated_at        timestamptz default now()
);

alter table public.receipts enable row level security;
drop policy if exists "owner only" on public.receipts;
create policy "owner only" on public.receipts
    for all using (auth.uid() = user_id);

-- ── Bank transactions ─────────────────────────────────────────
create table if not exists public.bank_transactions (
    id                  text primary key,
    business_id         text references public.businesses(id) on delete cascade,
    user_id             uuid references auth.users not null,
    transaction_date    date,
    description         text not null default '',
    amount              numeric not null default 0,
    category            text not null default 'Uncategorised',
    gst_estimate        numeric not null default 0,
    source_file         text not null default '',
    matched_receipt_id  text,
    created_at          timestamptz default now(),
    updated_at          timestamptz default now()
);

alter table public.bank_transactions enable row level security;
drop policy if exists "owner only" on public.bank_transactions;
create policy "owner only" on public.bank_transactions
    for all using (auth.uid() = user_id);

-- ── Compliance tasks ──────────────────────────────────────────
create table if not exists public.compliance_tasks (
    id            text primary key,
    business_id   text references public.businesses(id) on delete cascade,
    user_id       uuid references auth.users not null,
    task_type     text not null,
    due_date      date,
    status        text not null default 'Todo',
    created_at    timestamptz default now(),
    updated_at    timestamptz default now()
);

alter table public.compliance_tasks enable row level security;
drop policy if exists "owner only" on public.compliance_tasks;
create policy "owner only" on public.compliance_tasks
    for all using (auth.uid() = user_id);

-- ── Receipt image storage ─────────────────────────────────────
insert into storage.buckets (id, name, public)
    values ('receipts', 'receipts', false)
    on conflict do nothing;

drop policy if exists "upload own receipts" on storage.objects;
create policy "upload own receipts" on storage.objects
    for insert with check (
        bucket_id = 'receipts'
        and auth.uid()::text = (storage.foldername(name))[1]
    );

drop policy if exists "view own receipts" on storage.objects;
create policy "view own receipts" on storage.objects
    for select using (
        bucket_id = 'receipts'
        and auth.uid()::text = (storage.foldername(name))[1]
    );

drop policy if exists "delete own receipts" on storage.objects;
create policy "delete own receipts" on storage.objects
    for delete using (
        bucket_id = 'receipts'
        and auth.uid()::text = (storage.foldername(name))[1]
    );
