-- RecipeCatelog schema v1.
-- Rewritten pre-release to the full recipe model: ordered steps with per-step
-- tools; catalog entries with quantity / optional / replaceable and their
-- substitutes; and name/creator/version variations. Matches the JPA mappings;
-- Hibernate runs in validate mode against this.
--
-- If you already applied an earlier V1 to a persistent dev DB, reset it
-- (docker compose down -v) before running this one - Flyway checksums migrations.

create table tag (
    id          bigint       not null,
    name        varchar(100) not null,
    namespace   varchar(60),
    description varchar(500),
    primary key (id),
    constraint uk_tag_name unique (name)
) engine = InnoDB;

create index ix_tag_namespace on tag (namespace);

create table recipe (
    id      bigint       not null,
    name    varchar(255) not null,
    creator varchar(255) not null,
    version integer      not null,
    primary key (id),
    constraint uk_recipe_name_creator_version unique (name, creator, version)
) engine = InnoDB;

create table recipe_tag (
    recipe_id bigint not null,
    tag_id    bigint not null,
    primary key (recipe_id, tag_id),
    constraint fk_recipe_tag__recipe foreign key (recipe_id) references recipe (id),
    constraint fk_recipe_tag__tag    foreign key (tag_id)    references tag (id)
) engine = InnoDB;

create table recipe_step (
    id        bigint  not null,
    recipe_id bigint  not null,
    position  integer,
    text      text    not null,
    primary key (id),
    constraint fk_recipe_step__recipe foreign key (recipe_id) references recipe (id)
) engine = InnoDB;

create table recipe_step_tool (
    recipe_step_id bigint       not null,
    tool           varchar(100) not null,
    primary key (recipe_step_id, tool),
    constraint fk_recipe_step_tool__step foreign key (recipe_step_id) references recipe_step (id)
) engine = InnoDB;

-- ingredient_id references the Ingredient catalog (another service): no FK.
create table recipe_ingredient (
    id            bigint       not null,
    recipe_id     bigint       not null,
    ingredient_id bigint       not null,
    quantity      varchar(100),
    optional      bit          not null,
    replaceable   bit          not null,
    primary key (id),
    constraint fk_recipe_ingredient__recipe foreign key (recipe_id) references recipe (id)
) engine = InnoDB;

-- ingredient_id -> Ingredient catalog (no FK).
-- recipe_id -> a recipe here that makes the substitute (soft link, nullable, no FK).
create table ingredient_replacement (
    id                   bigint not null,
    recipe_ingredient_id bigint not null,
    ingredient_id        bigint not null,
    recipe_id            bigint,
    primary key (id),
    constraint fk_ingredient_replacement__recipe_ingredient
        foreign key (recipe_ingredient_id) references recipe_ingredient (id)
) engine = InnoDB;

-- @GeneratedValue (AUTO) on MySQL: one id table per entity.
create table recipe_seq (next_val bigint) engine = InnoDB;
insert into recipe_seq (next_val) values (1);
create table tag_seq (next_val bigint) engine = InnoDB;
insert into tag_seq (next_val) values (1);
create table recipe_step_seq (next_val bigint) engine = InnoDB;
insert into recipe_step_seq (next_val) values (1);
create table recipe_ingredient_seq (next_val bigint) engine = InnoDB;
insert into recipe_ingredient_seq (next_val) values (1);
create table ingredient_replacement_seq (next_val bigint) engine = InnoDB;
insert into ingredient_replacement_seq (next_val) values (1);
