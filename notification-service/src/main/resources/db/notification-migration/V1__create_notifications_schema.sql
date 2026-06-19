create table notification_service.notifications (
    id uuid primary key,
    order_id uuid not null,
    event_type varchar(100) not null,
    channel varchar(50) not null,
    recipient varchar(255) not null,
    message varchar(1000) not null,
    created_at timestamp with time zone not null
);

create index idx_notifications_created_at on notification_service.notifications (created_at desc);

create table notification_service.processed_events (
    event_id uuid primary key,
    processed_at timestamp with time zone not null
);
