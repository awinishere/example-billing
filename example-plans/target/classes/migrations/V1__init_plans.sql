create table plan
(
    id          uuid           not null,
    code        varchar(64)    not null,
    name        varchar(128)   not null,
    description varchar(2000)  not null,
    active      boolean        not null,
    created_at  timestamptz    not null,
    updated_at  timestamptz    not null,
    constraint pk_plan primary key (id),
    constraint uq_plan_code unique (code)
);

create table feature
(
    id               uuid         not null,
    code             varchar(64)  not null,
    name             varchar(128) not null,
    default_quantity integer,
    active           boolean      not null,
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    constraint pk_feature primary key (id),
    constraint uq_feature_code unique (code)
);

create table plan_price
(
    id             uuid          not null,
    plan_id        uuid          not null,
    amount         numeric(19, 4) not null,
    currency       varchar(3)    not null,
    billing_period varchar(20)   not null,
    active         boolean       not null,
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    constraint pk_plan_price primary key (id),
    constraint fk_plan_price_plan foreign key (plan_id) references plan (id),
    constraint uq_plan_price unique (plan_id, currency, billing_period),
    constraint ck_plan_price_amount check (amount >= 0)
);

create index ix_plan_price_plan on plan_price (plan_id);

create table plan_feature
(
    id         uuid    not null,
    plan_id    uuid    not null,
    feature_id uuid    not null,
    included   boolean not null,
    quantity   integer,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint pk_plan_feature primary key (id),
    constraint fk_plan_feature_plan foreign key (plan_id) references plan (id),
    constraint fk_plan_feature_feature foreign key (feature_id) references feature (id),
    constraint uq_plan_feature unique (plan_id, feature_id),
    constraint ck_plan_feature_quantity check (quantity is null or quantity >= 0)
);

create index ix_plan_feature_plan on plan_feature (plan_id);
create index ix_plan_feature_feature on plan_feature (feature_id);
