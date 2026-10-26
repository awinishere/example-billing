create table login_attempt
(
    id           uuid         not null,
    email        varchar(320) not null,
    ip_address   varchar(64),
    succeeded    boolean      not null,
    attempted_at timestamptz  not null,
    constraint pk_login_attempt primary key (id)
);

create index idx_login_attempt_email_time on login_attempt (email, attempted_at);

create index idx_login_attempt_ip_time on login_attempt (ip_address, attempted_at);
