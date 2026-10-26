create table credentials
(
    id                 uuid          not null,
    email              varchar(320)  not null,
    password           varchar(255)  not null,
    role               varchar(16)   not null,
    email_verification boolean       not null,
    account_status     varchar(16)   not null,
    created_at         timestamptz   not null,
    updated_at         timestamptz   not null,
    constraint pk_credentials primary key (id),
    constraint ck_credentials_role check (role in ('ADMIN', 'USER', 'SUPPORT')),
    constraint ck_credentials_account_status check (account_status in ('ACTIVE', 'PENDING', 'INACTIVE', 'SUSPENDED', 'DELETED')),
    constraint uq_credentials_email unique (email)
);

create table oauth_credentials
(
    id               uuid         not null,
    user_id          uuid         not null,
    provider         varchar(32)  not null,
    provider_user_id varchar(255) not null,
    email            varchar(255) not null,
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    constraint pk_oauth_credentials primary key (id),
    constraint uq_oauth_credentials_provider_identity unique (provider, provider_user_id)
);

create index idx_oauth_credentials_user_id on oauth_credentials (user_id);

create table otp_code
(
    id         uuid         not null,
    user_id    uuid         not null,
    purpose    varchar(32)  not null,
    code_hash  varchar(512) not null,
    expires_at timestamptz  not null,
    consumed   boolean      not null,
    attempts   integer      not null,
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    constraint pk_otp_code primary key (id),
    constraint ck_otp_code_purpose check (purpose in ('LOGIN_VERIFICATION', 'PASSWORD_RESET'))
);

create index idx_otp_code_user_purpose on otp_code (user_id, purpose);

create table refresh_secret
(
    id          uuid         not null,
    user_id     uuid         not null,
    family_id   uuid         not null,
    secret_hash varchar(512) not null,
    expires_at  timestamptz  not null,
    revoked     boolean      not null,
    user_agent  varchar(512),
    ip_address  varchar(64),
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null,
    constraint pk_refresh_secret primary key (id)
);

create index idx_refresh_secret_user_id on refresh_secret (user_id);

create index idx_refresh_secret_family_id on refresh_secret (family_id);
