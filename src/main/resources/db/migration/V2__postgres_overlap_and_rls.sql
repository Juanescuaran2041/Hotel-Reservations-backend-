-- PostgreSQL / Supabase only (the dev and test profiles stop at V1, see application-dev.yml).

set local search_path to public, extensions;

-- Race-proof overbooking protection: two active reservations of the same room can never overlap.
-- A reservation stops blocking its room once it is CHECKED_OUT. The check-out date is exclusive.
create extension if not exists btree_gist with schema extensions;

alter table reservations
    add constraint reservations_no_overlap
    exclude using gist (
        room_number with =,
        daterange(check_in_date, check_out_date) with &&
    ) where (status <> 'CHECKED_OUT');

-- Supabase exposes every table in the public schema through its REST API. The backend connects with the
-- database owner role (which bypasses RLS), so enabling RLS without policies closes the public API completely.
alter table users                  enable row level security;
alter table rooms                  enable row level security;
alter table reservations           enable row level security;
alter table reservation_rate_lines enable row level security;
alter table reservation_services   enable row level security;
alter table invoices               enable row level security;
alter table invoice_room_lines     enable row level security;
alter table invoice_service_lines  enable row level security;

-- flyway_schema_history cannot be altered here: Flyway holds a lock on it while this migration runs.
-- Enable RLS on it once, from the Supabase SQL editor (see README).
