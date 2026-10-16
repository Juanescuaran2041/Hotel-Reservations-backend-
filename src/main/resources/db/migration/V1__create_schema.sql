-- Core schema. Written with portable SQL so the same file runs on PostgreSQL (Supabase) and on H2 (dev/tests).

create table users (
    id            uuid         primary key,
    full_name     varchar(120) not null,
    email         varchar(160) not null unique,
    password_hash varchar(100) not null,
    role          varchar(20)  not null check (role in ('GUEST', 'ADMIN')),
    created_at    timestamp with time zone not null default current_timestamp
);

create table rooms (
    number varchar(10) primary key,
    type   varchar(10) not null check (type in ('SINGLE', 'DOUBLE', 'SUITE'))
);

create table reservations (
    id                    uuid         primary key,
    version               bigint       not null default 0,
    owner_email           varchar(160) not null references users (email),
    guest_name            varchar(120) not null,
    guest_email           varchar(160) not null,
    guest_phone           varchar(30),
    room_number           varchar(10)  not null references rooms (number),
    check_in_date         date         not null,
    check_out_date        date         not null,
    status                varchar(20)  not null check (status in ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')),
    room_cost             numeric(12, 2) not null,
    digital_key_code      varchar(36),
    digital_key_issued_at timestamp with time zone,
    created_at            timestamp with time zone not null default current_timestamp,
    constraint reservations_dates_valid check (check_out_date > check_in_date)
);

create index idx_reservations_room_dates on reservations (room_number, check_in_date, check_out_date);
create index idx_reservations_owner on reservations (owner_email);
create index idx_reservations_key on reservations (digital_key_code);

-- Price breakdown frozen when the reservation was created (one row per season).
create table reservation_rate_lines (
    reservation_id uuid        not null references reservations (id) on delete cascade,
    line_order     int         not null,
    season         varchar(10) not null check (season in ('HIGH', 'LOW')),
    nights         bigint      not null,
    nightly_rate   numeric(12, 2) not null,
    subtotal       numeric(12, 2) not null,
    primary key (reservation_id, line_order)
);

create table reservation_services (
    reservation_id uuid        not null references reservations (id) on delete cascade,
    line_order     int         not null,
    service_type   varchar(20) not null check (service_type in ('SPA', 'BREAKFAST', 'TRANSPORT')),
    quantity       int         not null,
    unit_price     numeric(12, 2) not null,
    total          numeric(12, 2) not null,
    primary key (reservation_id, line_order)
);

-- One invoice per reservation, so the reservation id is also the invoice key.
create table invoices (
    reservation_id   uuid         primary key references reservations (id),
    number           varchar(20)  not null unique,
    guest_name       varchar(120) not null,
    guest_email      varchar(160) not null,
    guest_phone      varchar(30),
    room_number      varchar(10)  not null,
    check_in_date    date         not null,
    check_out_date   date         not null,
    room_subtotal    numeric(12, 2) not null,
    services_subtotal numeric(12, 2) not null,
    total            numeric(12, 2) not null,
    issued_at        timestamp with time zone not null
);

create table invoice_room_lines (
    invoice_id  uuid         not null references invoices (reservation_id) on delete cascade,
    line_order  int          not null,
    description varchar(200) not null,
    quantity    bigint       not null,
    unit_price  numeric(12, 2) not null,
    amount      numeric(12, 2) not null,
    primary key (invoice_id, line_order)
);

create table invoice_service_lines (
    invoice_id  uuid         not null references invoices (reservation_id) on delete cascade,
    line_order  int          not null,
    description varchar(200) not null,
    quantity    bigint       not null,
    unit_price  numeric(12, 2) not null,
    amount      numeric(12, 2) not null,
    primary key (invoice_id, line_order)
);

-- The hotel's 18 predefined rooms.
insert into rooms (number, type) values
    ('101', 'SINGLE'), ('102', 'SINGLE'), ('103', 'SINGLE'),
    ('104', 'SINGLE'), ('105', 'SINGLE'), ('106', 'SINGLE'),
    ('201', 'DOUBLE'), ('202', 'DOUBLE'), ('203', 'DOUBLE'), ('204', 'DOUBLE'),
    ('205', 'DOUBLE'), ('206', 'DOUBLE'), ('207', 'DOUBLE'), ('208', 'DOUBLE'),
    ('301', 'SUITE'), ('302', 'SUITE'), ('303', 'SUITE'), ('304', 'SUITE');
