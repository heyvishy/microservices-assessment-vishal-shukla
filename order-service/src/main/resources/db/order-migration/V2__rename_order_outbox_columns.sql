alter table order_service.order_outbox
    rename column aggregate_type to object_type;

alter table order_service.order_outbox
    rename column aggregate_id to object_id;
