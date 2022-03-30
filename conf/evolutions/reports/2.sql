CREATE TABLE IF NOT EXISTS transactions (
  unique_id int(10) unsigned NOT NULL,
  id varchar(36)  NOT NULL,
  sequence int(10) unsigned NOT NULL,
  primary_account_id int(10) unsigned NOT NULL,
  primary_account_uuid varchar(50) NOT NULL,
  primary_account_number varchar(50) NOT NULL,
  primary_account_type varchar(50) NOT NULL,
  primary_account_main_type varchar(50) NOT NULL,
  primary_account_user_id INT NOT NULL,
  primary_account_user_uuid varchar(36) NOT NULL,
  secondary_account_id int(10) unsigned NOT NULL,
  secondary_account_uuid varchar(50) NOT NULL,
  secondary_account_number varchar(50) NOT NULL,
  receiver_phone varchar(50) DEFAULT NULL,
  direction varchar(50) DEFAULT NULL,
  type varchar(50) DEFAULT NULL,
  amount decimal(10,2) DEFAULT NULL,
  currency varchar(10) DEFAULT NULL,
  exchange_rate decimal(10,2) DEFAULT NULL,
  channel varchar(50) DEFAULT NULL,
  other_party varchar(50) DEFAULT NULL,
  instrument varchar(50) DEFAULT NULL,
  instrument_id varchar(50) DEFAULT NULL,
  latitude decimal(12,10) DEFAULT NULL,
  longitude decimal(13,10) DEFAULT NULL,
  explanation varchar(256) DEFAULT NULL,
  status varchar(50) DEFAULT NULL,
  created_at datetime DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  effective_rate decimal(10,2) DEFAULT NULL,
  cost_rate decimal(10,2) DEFAULT NULL,
  previous_balance decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (unique_id)
  );


CREATE TABLE IF NOT EXISTS accounts (
id int(10) unsigned NOT NULL PRIMARY KEY,
uuid varchar default 'missing',
number varchar default 'missing',
name varchar default 'missing',
account_type varchar default 'missing',
is_main_account boolean default null,
user_id int default null,
user_uuid varchar default 'missing',
currency varchar default 'missing',
balance decimal(30,4) default null,
blocked_balance decimal(30,4) default null,
status varchar default 'missing',
closed_at datetime DEFAULT NULL,
last_transaction_at datetime DEFAULT NULL,
created_at datetime DEFAULT NULL,
updated_at datetime DEFAULT NULL,
updated_by varchar default null,
created_by varchar default 'missing',
main_type varchar default 'missing'
);

create table users
(
  id                  int 															 NOT NULL ,
  uuid                varchar(36)                        NOT NULL UNIQUE,
  username            varchar(100)                       null,
  fullname            varchar(200)                       null,
  password            varchar(100)                       null,
  type                varchar(30)                        null,
  tier                varchar(100)                       null,
  segment             varchar(100)                       null,
  subscription        varchar(100)                       null,
  email               varchar(100)                       null,
  user_status         varchar(30)                        null,
  activated_at        timestamp                          null,
  password_updated_at timestamp                          null,
  created_at          timestamp                          NOT NULL,
  created_by          varchar(36)                        NOT NULL,
  updated_at          timestamp    							default  null,
  updated_by          varchar(36)                        null
 
 );

DROP TABLE transactions;
DROP TABLE accounts;
DROP TABLE users;