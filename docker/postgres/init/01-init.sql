create user order_app with password 'order_app';
create user notification_app with password 'notification_app';

create database tenant_a_db owner postgres;
create database tenant_b_db owner postgres;

\connect tenant_a_db

drop schema public cascade;
create schema order_service authorization order_app;
create schema notification_service authorization notification_app;

grant usage, create on schema order_service to order_app;
grant usage, create on schema notification_service to notification_app;

\connect tenant_b_db

drop schema public cascade;
create schema order_service authorization order_app;
create schema notification_service authorization notification_app;

grant usage, create on schema order_service to order_app;
grant usage, create on schema notification_service to notification_app;
