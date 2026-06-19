create table order_service.orders (
    id uuid primary key,
    customer_id varchar(100) not null,
    customer_email varchar(255) not null,
    description varchar(500) not null,
    total_amount numeric(19,2) not null,
    currency varchar(3) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_orders_created_at on order_service.orders (created_at desc);

create table order_service.order_outbox (
    id uuid primary key,
    aggregate_type varchar(100) not null,
    aggregate_id uuid not null,
    event_type varchar(100) not null,
    topic varchar(100) not null,
    event_key varchar(200) not null,
    payload text not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone null,
    publish_attempts int not null default 0,
    last_error varchar(1000) null
);

create index idx_order_outbox_unpublished on order_service.order_outbox (published_at, created_at);
