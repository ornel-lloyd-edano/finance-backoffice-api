# --- !Ups

CREATE TABLE `business_units` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `name`                VARCHAR(32) NOT NULL,
  `is_active`           INT DEFAULT 0,
  `created_by`          VARCHAR(128) NOT NULL,
  `updated_by`          VARCHAR(128) DEFAULT NULL,
  `created_at`          DATETIME NOT NULL,
  `updated_at`          DATETIME DEFAULT NULL,
  UNIQUE KEY unique_bu_name(`name`));

CREATE TABLE `roles` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `name`                VARCHAR(128) NOT NULL,
  `is_active`           TINYINT NOT NULL DEFAULT 0,
  `created_by`          VARCHAR(128) NOT NULL,
  `updated_by`          VARCHAR(128) DEFAULT NULL,
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME DEFAULT NULL,
  `level`               TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY unique_role_name(`name`));

CREATE TABLE `back_office_users` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `userName`            VARCHAR(128) NOT NULL,
  `password`            VARCHAR(128) NOT NULL,
  `roleId`              CHAR(36) NOT NULL,
  `businessUnitId`      CHAR(36) NOT NULL,
  `email`               VARCHAR(128) NOT NULL,
  `phoneNumber`         VARCHAR(50)  DEFAULT NULL,
  `firstName`           VARCHAR(128) NOT NULL,
  `middleName`          VARCHAR(128) DEFAULT NULL,
  `lastName`            VARCHAR(128) NOT NULL,
  `description`         VARCHAR(128) DEFAULT NULL,
  `homePage`            VARCHAR(128) DEFAULT NULL,
  `is_active`           TINYINT NOT NULL DEFAULT 0,
  `activeLanguage`      VARCHAR(50) DEFAULT NULL,
  `customData`          VARCHAR(512) DEFAULT NULL,
  `lastLoginTimestamp`  BIGINT(20) UNSIGNED DEFAULT NULL,
  `created_by`          VARCHAR(36) DEFAULT NULL,
  `updated_by`          VARCHAR(36) DEFAULT NULL,
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`roleId`) REFERENCES roles(`id`),
  FOREIGN KEY (`businessUnitId`) REFERENCES business_units(`id`),
  UNIQUE KEY unique_phoneNumber(`phoneNumber`),
  UNIQUE KEY unique_email(`email`),
  UNIQUE KEY unique_userName (`userName`));

CREATE TABLE `scopes` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `parentId`            CHAR(36) DEFAULT NULL,
  `name`                VARCHAR(32) NOT NULL,
  `description`         VARCHAR(255) DEFAULT NULL,
  `is_active`           BIT NOT NULL DEFAULT 1,
  `created_by`          CHAR(36) NOT NULL,
  `updated_by`          CHAR(36) DEFAULT NULL,
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  `status`              BIT NOT NULL DEFAULT 1,
  `cBy`                 CHAR(36) NOT NULL,
  `uBy`                 CHAR(36) DEFAULT NULL,
  `cDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uDate`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (`parentId`) REFERENCES scopes(`id`),
  UNIQUE KEY unique_scope_name (`name`));

CREATE TABLE `permissions` (
  `id`                  CHAR(36) NOT NULL PRIMARY KEY,
  `buId`                CHAR(36) DEFAULT NULL,
  `userId`              CHAR(36) DEFAULT NULL,
  `roleId`              CHAR(36) DEFAULT NULL,
  `scopeId`             CHAR(36) NOT NULL,
  `canWrite`            BOOLEAN NOT NULL,
  `is_active`           TINYINT NOT NULL DEFAULT 1,
  `created_by`          CHAR(36) NOT NULL,
  `updated_by`          CHAR(36) DEFAULT NULL,
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`buId`) REFERENCES business_units(`id`),
  FOREIGN KEY (`userId`) REFERENCES `back_office_users`(`id`),
  FOREIGN KEY (`roleId`) REFERENCES roles(`id`),
  FOREIGN KEY (`scopeId`) REFERENCES scopes(`id`),
  UNIQUE KEY unique_business_unit_scope (`buId`, `scopeId`),
  UNIQUE KEY unique_user_scope (`userId`, `scopeId`),
  UNIQUE KEY unique_role_scope (`roleId`, `scopeId`));

CREATE TABLE `user_types` (
  `type_name` VARCHAR(32) NOT NULL,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(36) NOT NULL,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(36) DEFAULT NULL,
  `is_active` BIT(1) DEFAULT 1,
  PRIMARY KEY (`type_name`)
)  DEFAULT CHARSET=utf8;

CREATE TABLE `customer_tiers` (
  `tier_name` VARCHAR(32) NOT NULL,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_active` BIT(1) DEFAULT true,
  PRIMARY KEY (`tier_name`)
)  DEFAULT CHARSET=utf8;

CREATE TABLE `user_status` (
  `status_name` VARCHAR(32) NOT NULL,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME NULL,
  `created_by` CHAR(36) DEFAULT NULL,
  `updated_at` DATETIME  NULL,
  `updated_by` VARCHAR(20) DEFAULT NULL,
  `is_active`   BIT(1) DEFAULT true,
  PRIMARY KEY (`status_name`)
)  DEFAULT CHARSET=utf8;

CREATE TABLE `subscriptions` (
  `subscription_name` VARCHAR(32) NOT NULL,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(36) NOT NULL,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(36) DEFAULT NULL,
  `is_active` BIT(1) DEFAULT true,
  PRIMARY KEY (`subscription_name`)
)  DEFAULT CHARSET=utf8;

CREATE TABLE `customer_segments` (
  `segment_name` VARCHAR(32) NOT NULL,
  `description`  VARCHAR(100) DEFAULT NULL,
  `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by`   VARCHAR(36) NOT NULL,
  `updated_at`   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`   VARCHAR(36) DEFAULT NULL,
  `is_active`    BIT(1) DEFAULT true,
  PRIMARY KEY (`segment_name`)
)  DEFAULT CHARSET=utf8;

CREATE TABLE users (
	id                  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
	uuid                VARCHAR(36)  NOT NULL,
	username            VARCHAR(100) NULL,
	password            VARCHAR(100) NULL,
	type                VARCHAR(30)  NULL,
	tier                VARCHAR(100) NULL,
	segment             VARCHAR(100) NULL,
	subscription        VARCHAR(100) NULL,
	email               VARCHAR(100) NULL,
	status              VARCHAR(30)  NULL,
	activated_at        DATETIME     NULL,
	password_updated_at DATETIME     NULL,
	created_at          DATETIME     NOT NULL,
	created_by          VARCHAR(36)  NOT NULL,
	updated_at          DATETIME     NULL,
	updated_by          VARCHAR(36)  NULL,
	CONSTRAINT username UNIQUE (username),
	CONSTRAINT uuid UNIQUE (uuid)
)	charset = utf8;

CREATE TABLE `companies` (
  `company_name` VARCHAR(32) NOT NULL,
  `company_full_name` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `created_by` VARCHAR(36) DEFAULT NULL,
  `updated_by` VARCHAR(36) DEFAULT NULL,
  `is_active` BIT(1) DEFAULT true,
  PRIMARY KEY (`company_name`)
)  DEFAULT CHARSET=utf8;

create table `business_users`
(
	id                      int unsigned     auto_increment primary key,
	uuid                    varchar(36)      not null,
	user_id                 int unsigned     not null,
	business_name           varchar(36)      not null,
	brand_name              varchar(36)      not null,
	business_category       varchar(36)      not null,
	business_type           varchar(50)      not null,
	registration_number     varchar(50)      not null,
	tax_number              varchar(50)      not null,
	registration_date       date             not null,
	currency_id             tinyint unsigned not null,
	collection_account_id   int unsigned     null,
	distribution_account_id int unsigned     null,
	default_contact_id      int unsigned     null,
	created_by              varchar(50)      not null,
	updated_by              varchar(50)      null,
	created_at              datetime         not null,
	updated_at              datetime         not null,
	total_transactions_amount decimal(10,2) NOT NULL DEFAULT '0.00',
	transaction_count int(11) NOT NULL DEFAULT '0'
) DEFAULT CHARSET=utf8;

CREATE TABLE `extra_attribute_types` (
 `attribute_type_name` CHAR(32) NOT NULL,
 `description`  VARCHAR(100) DEFAULT NULL,
 `created_at` DATETIME DEFAULT NULL,
 `created_by` CHAR(36) NOT NULL,
 `updated_at` DATETIME DEFAULT NULL,
 `updated_by` CHAR(36) DEFAULT NULL,
 `is_active`  BOOLEAN DEFAULT 1,

 PRIMARY KEY (`attribute_type_name`)
)  DEFAULT CHARSET=utf8;

CREATE TABLE `business_users_has_extra_attributes` (
 `id` INT NOT NULL AUTO_INCREMENT,
 `business_user_id` INT NOT NULL,
 `extra_attribute_type` CHAR(36) NOT NULL,
 `attribute_value` VARCHAR(100) DEFAULT NULL,
 `created_at` DATETIME DEFAULT NULL,
 `created_by` CHAR(36) NOT NULL,
 `updated_at` DATETIME DEFAULT NULL,
 `updated_by` CHAR(36) DEFAULT NULL,


 PRIMARY KEY (`id`),

 CONSTRAINT `fk_busi_users_extra_attrib_business_user_id` FOREIGN KEY (`business_user_id`) REFERENCES `business_users` (`user_id`),
 CONSTRAINT `fk_busi_users_extra_attrib_extra_attribute_type` FOREIGN KEY (`extra_attribute_type`) REFERENCES `extra_attribute_types` (`attribute_type_name`)
)  DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `currencies`;
CREATE TABLE `currencies` (
  `id` TINYINT(3) UNSIGNED NOT NULL AUTO_INCREMENT,
  `currency_name` char(3) NOT NULL,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by`  VARCHAR(100) NOT NULL,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
   `updated_by`  VARCHAR(100) DEFAULT NULL,
  `is_active` TINYINT(4) NOT NULL DEFAULT '1',
  `icon`  VARCHAR(20) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `currency_name` (`currency_name`)
) DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `account_types`;
CREATE TABLE `account_types` (
  `id` INT UNSIGNED AUTO_INCREMENT,
  `account_type_name` VARCHAR(32) NOT NULL UNIQUE,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `created_by` VARCHAR(36) DEFAULT NULL,
  `updated_by` VARCHAR(36) DEFAULT NULL,
  `is_active` TINYINT DEFAULT 1,
  PRIMARY KEY (`id`)
)  DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `accounts`;
CREATE TABLE accounts (
	id                  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
	uuid                VARCHAR(36)      NOT NULL UNIQUE ,
	number              VARCHAR(100)     NULL,
	user_id             INT UNSIGNED     NULL,
	name                VARCHAR(100)     NULL,
	account_type_id     TINYINT UNSIGNED NULL,
	is_main_account     TINYINT          NULL,
	currency_id         TINYINT UNSIGNED NOT NULL,
	balance             DECIMAL(10, 2)   NULL,
	blocked_balance     DECIMAL(10, 2)   NULL,
	status              VARCHAR(15)      NULL,
	closed_at           DATETIME         NULL,
	last_transaction_at DATETIME         NULL,
	created_at          DATETIME         NOT NULL,
	updated_at          DATETIME         NULL,
	updated_by          VARCHAR(36)      NULL,
	created_by          VARCHAR(36)      NOT NULL,
  main_type           VARCHAR(36)      NOT NULL, --h2 converts enum to int, main_type enum('liability','asset') NOT NULL DEFAULT 'liability',
	CONSTRAINT NUMBER UNIQUE (number),
	CONSTRAINT fk_accounts_account_type_id FOREIGN KEY (account_type_id) REFERENCES account_types (id),
	CONSTRAINT fk_accounts_currency_id FOREIGN KEY (currency_id) REFERENCES currencies (id),
	CONSTRAINT fk_accounts_user_id FOREIGN KEY (user_id) REFERENCES users (id)
)charset = utf8;


CREATE TABLE `user_status_has_requirements` (
 `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
 `user_status` VARCHAR(32) NOT NULL,
 `requirement_type` VARCHAR(32) NOT NULL,
 `created_at` DATETIME DEFAULT NULL,
 `created_by` CHAR(36) NOT NULL,
 `updated_at` DATETIME DEFAULT NULL,
 `updated_by` CHAR(36) DEFAULT NULL,

 PRIMARY KEY (`id`),

 CONSTRAINT `fk_user_status_has_requirements_user_status` FOREIGN KEY (`user_status`) REFERENCES `user_status` (`status_name`),
 CONSTRAINT `fk_user_status_has_requirements_requirement_type` FOREIGN KEY (`requirement_type`) REFERENCES `extra_attribute_types` (`attribute_type_name`)
)  DEFAULT CHARSET=utf8;


CREATE TABLE `providers` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned NOT NULL,
  `service_id` int(10) unsigned DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `transaction_type` varchar(50) NOT NULL,
  `icon` varchar(50)  NOT NULL,
  `label` varchar(50)  NOT NULL,
  `pg_institution_id` int(10) unsigned NOT NULL,
  `utility_payment_type` varchar(20)  DEFAULT NULL,
  `utility_min_payment_amount` decimal(10,2) DEFAULT '0.01',
  `utility_max_payment_amount` decimal(10,2) DEFAULT NULL,
  `is_active` tinyint(3) NOT NULL,
  `created_by` varchar(36) NOT NULL DEFAULT 'core',
  `updated_by` varchar(36) NOT NULL DEFAULT 'core',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `providers_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);

# --- !Downs
DROP TABLE `providers`;
DROP TABLE `permissions`;
DROP TABLE `scopes`;
DROP TABLE `back_office_users`;
DROP TABLE `roles`;
DROP TABLE `business_units`;
DROP TABLE `users`;
DROP TABLE `business_users`;
DROP TABLE `companies`;
DROP TABLE `customer_segments`;