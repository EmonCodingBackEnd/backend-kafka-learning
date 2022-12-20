-- -------------------------------------------------
--   _____       _      _
--  |_   _|__ _ | |__  | |  ___
--    | | / _` || '_ \ | | / _ \
--    | || (_| || |_) || ||  __/
--    |_| \__,_||_.__/ |_| \___|
--
-- 表
-- -------------------------------------------------

drop table if exists users;

/*==============================================================*/
/* Table: t_user                                                */
/*==============================================================*/
create table users
(
    id       int not null auto_increment,
    username varchar(20),
    age      int,
    primary key (id)
);

drop table if exists users_bak;

/*==============================================================*/
/* Table: t_user                                                */
/*==============================================================*/
create table users_bak
(
    id       int not null auto_increment,
    username varchar(20),
    age      int,
    primary key (id)
);