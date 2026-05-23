create table if not exists public.users (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null,
  created_at timestamptz not null default now()
);

create table if not exists public.businesses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users(id) on delete cascade,
  abn text,
  business_name text not null,
  gst_registered boolean not null default false,
  bas_frequency text not null default 'quarterly',
  created_at timestamptz not null default now()
);

create table if not exists public.receipts (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,
  file_url text not null,
  merchant text,
  receipt_date date,
  total numeric(12, 2),
  gst numeric(12, 2),
  category text,
  ocr_confidence numeric(4, 3),
  ai_confidence numeric(4, 3),
  created_at timestamptz not null default now()
);

create table if not exists public.compliance_tasks (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,
  task_type text not null,
  due_date date not null,
  status text not null default 'todo',
  created_at timestamptz not null default now()
);

create table if not exists public.reminders (
  id uuid primary key default gen_random_uuid(),
  task_id uuid not null references public.compliance_tasks(id) on delete cascade,
  reminder_date date not null,
  sent boolean not null default false,
  created_at timestamptz not null default now()
);

alter table public.users enable row level security;
alter table public.businesses enable row level security;
alter table public.receipts enable row level security;
alter table public.compliance_tasks enable row level security;
alter table public.reminders enable row level security;

create policy "Users can read own profile"
on public.users for select
using (auth.uid() = id);

create policy "Users can manage own businesses"
on public.businesses for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "Users can manage receipts for own businesses"
on public.receipts for all
using (
  exists (
    select 1 from public.businesses
    where businesses.id = receipts.business_id
    and businesses.user_id = auth.uid()
  )
)
with check (
  exists (
    select 1 from public.businesses
    where businesses.id = receipts.business_id
    and businesses.user_id = auth.uid()
  )
);

create policy "Users can manage own compliance tasks"
on public.compliance_tasks for all
using (
  exists (
    select 1 from public.businesses
    where businesses.id = compliance_tasks.business_id
    and businesses.user_id = auth.uid()
  )
)
with check (
  exists (
    select 1 from public.businesses
    where businesses.id = compliance_tasks.business_id
    and businesses.user_id = auth.uid()
  )
);

create policy "Users can manage own reminders"
on public.reminders for all
using (
  exists (
    select 1
    from public.compliance_tasks
    join public.businesses on businesses.id = compliance_tasks.business_id
    where compliance_tasks.id = reminders.task_id
    and businesses.user_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.compliance_tasks
    join public.businesses on businesses.id = compliance_tasks.business_id
    where compliance_tasks.id = reminders.task_id
    and businesses.user_id = auth.uid()
  )
);
