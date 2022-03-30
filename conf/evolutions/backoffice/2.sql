# --- !Ups

DROP TABLE IF EXISTS `nationalities`;
CREATE TABLE `nationalities` (
  `nationality_name` VARCHAR(32) NOT NULL,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `created_by` VARCHAR(36) DEFAULT NULL,
  `updated_by` VARCHAR(36) DEFAULT NULL,
  `is_active` INT DEFAULT 1,
  PRIMARY KEY (`nationality_name`)
)  DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `occupations`;
CREATE TABLE `occupations` (
  `occupation_name` VARCHAR(32) NOT NULL,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `created_by` VARCHAR(36) DEFAULT NULL,
  `updated_by` VARCHAR(36) DEFAULT NULL,
  `is_active` INT DEFAULT 1,
  PRIMARY KEY (`occupation_name`)
)  DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `employers`;
CREATE TABLE `employers` (
  `employer_name` VARCHAR(32) NOT NULL,
  `description` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(36) DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(36) DEFAULT NULL,
  `is_active` INT DEFAULT 1,
  PRIMARY KEY (`employer_name`)
)  DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS individual_users;

CREATE TABLE individual_users (
	id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
	msisdn      VARCHAR(50)                 NULL,
	user_id     INT UNSIGNED                NULL,
	type        VARCHAR(30)                 NULL,
	name        VARCHAR(100)                NULL,
	fullname    VARCHAR(100)                NULL,
	gender      VARCHAR(10)                 NULL,
	person_id   VARCHAR(50)                 NULL,
	document_number VARCHAR(50)             NULL,
	document_type   VARCHAR(50)             NULL,
	document_model  VARCHAR(50)             NULL,
	company     VARCHAR(100)                NULL,
	birthdate   DATETIME                    NULL,
	birth_place VARCHAR(30)                 NULL,
	nationality VARCHAR(15)                 NULL,
	occupation  VARCHAR(40)                 NULL,
	employer    VARCHAR(100)                NULL,
	created_at  DATETIME                    NOT NULL NULL,
	created_by  VARCHAR(36)                 NOT NULL NULL,
	updated_at  DATETIME                    NULL,
	updated_by  VARCHAR(36)                 NULL,
	CONSTRAINT msisdn UNIQUE (msisdn),
	CONSTRAINT user_id_UNIQUE UNIQUE (user_id),
	CONSTRAINT `fk_individual_users_users_user_id` FOREIGN KEY(`user_id`) REFERENCES `users`(`id`)
)	charset = utf8;

DROP TABLE IF EXISTS user_applications;
CREATE TABLE user_applications (
	id                   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
	uuid                 VARCHAR(36)                        NOT NULL,
	user_id              INT UNSIGNED                       NOT NULL,
	status               VARCHAR(30)                        NOT NULL,
	stage                VARCHAR(30)                        NOT NULL,
	rejection_reason     VARCHAR(300)                       NULL,
	checked_by           VARCHAR(50)                        NULL,
	checked_at           DATETIME                           NULL,
	total_score          FLOAT                              NULL,

	fullname_score       FLOAT                              NULL,
	fullname_original    VARCHAR(50)                        NULL,
	fullname_updated     VARCHAR(50)                        NULL,

	birthdate_score      FLOAT                              NULL,
	birthdate_original   DATETIME                           NULL,
	birthdate_updated    DATETIME                           NULL,

	birthplace_score     FLOAT                              NULL,
	birthplace_original  VARCHAR(50)                        NULL,
	birthplace_updated   VARCHAR(50)                        NULL,

	gender_score         FLOAT                              NULL,
	gender_original      VARCHAR(50)                        NULL,
	gender_updated       VARCHAR(50)                        NULL,

	nationality_score    FLOAT                              NULL,
	nationality_original VARCHAR(50)                        NULL,
	nationality_updated  VARCHAR(50)                        NULL,

	person_id_score      FLOAT                              NULL,
	person_id_original   VARCHAR(50)                        NULL,
	person_id_updated    VARCHAR(50)                        NULL,

	document_number_score    FLOAT                              NULL,
	document_number_original VARCHAR(50)                        NULL,
	document_number_updated  VARCHAR(50)                        NULL,
	document_type        VARCHAR(50)                        NULL,
	document_model       VARCHAR(50)                        NULL,

	created_by           VARCHAR(50)                        NOT NULL,
	created_at           DATETIME default CURRENT_TIMESTAMP NULL,
	updated_by           VARCHAR(50)                        NULL,
	updated_at           DATETIME default CURRENT_TIMESTAMP NULL,

	CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(id)
) charset = utf8;


CREATE TABLE IF NOT EXISTS `application_documents` (
`id`         INT UNSIGNED NOT NULL AUTO_INCREMENT,
`uuid`       CHAR(36) NOT NULL UNIQUE,
`user_id`    INT DEFAULT NULL,
`application_id` INT UNSIGNED DEFAULT NULL,
`bu_application_id`  INT UNSIGNED DEFAULT NULL,
`status`     VARCHAR(36),
`document_number` VARCHAR(36),
`document_type` VARCHAR(36),
`image_type` VARCHAR(36),
`purpose` VARCHAR(50),
`rejection_reason` VARCHAR(50) DEFAULT NULL,
`properties` VARCHAR(50) DEFAULT NULL,
`created_by` VARCHAR(36) NOT NULL,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`file_name` VARCHAR(100) DEFAULT NULL,
`file_uploaded_by` VARCHAR(50) DEFAULT NULL,
`file_uploaded_at` DATETIME DEFAULT NULL,
`file_persisted_at` DATETIME DEFAULT NULL,
`checked_by` VARCHAR(36) DEFAULT NULL,
`checked_at` DATETIME DEFAULT NULL,
`updated_by` VARCHAR(36),
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


PRIMARY KEY(`id`),
constraint `fk_activation_documents_app_id` FOREIGN KEY(`application_id`) REFERENCES `user_applications`(`id`),
constraint `fk_activation_documents_user_id` FOREIGN KEY(`user_id`) REFERENCES `users`(`id`)
);


CREATE TABLE IF NOT EXISTS `transactions` (
`unique_id` int(11)  AUTO_INCREMENT not null,
  `id` varchar(36) unsigned NOT NULL,
  `sequence` int(10) unsigned NOT NULL,
  `primary_account_id` int(10) unsigned NOT NULL,
  `secondary_account_id` int(10) unsigned NOT NULL,
  `receiver_phone` varchar(50) DEFAULT NULL,
  `direction` varchar(50) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `amount` decimal(10,2) DEFAULT NULL,
  `currency_id` tinyint(3) unsigned DEFAULT NULL,
  `exchange_rate` decimal(10,2) DEFAULT NULL,
  `channel` varchar(50) DEFAULT NULL,
  `provider_id` int(10) unsigned DEFAULT NULL,
  `instrument` varchar(50) DEFAULT NULL,
  `instrument_id` varchar(50) DEFAULT NULL,
  `latitude` decimal(12,10) DEFAULT NULL,
  `longitude` decimal(13,10) DEFAULT NULL,
  `explanation` varchar(256) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `effective_rate` decimal(10,6) DEFAULT NULL,
  `cost_rate` decimal(10,6) DEFAULT NULL,
  `primary_account_previous_balance` decimal(10,2) DEFAULT NULL,
  `secondary_account_previous_balance` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id`,`sequence`,`primary_account_id`,`secondary_account_id`),
  KEY `fk_currencies_id` (`currency_id`),
  KEY `fk_primary_account_id` (`primary_account_id`),
  KEY `fk_seconday_account_id` (`secondary_account_id`),
  CONSTRAINT `fk_primary_account_id` FOREIGN KEY (`primary_account_id`) REFERENCES `accounts` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_seconday_account_id` FOREIGN KEY (`secondary_account_id`) REFERENCES `accounts` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_currencies_id` FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE `currency_exchange_providers` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned NOT NULL,
  `name` varchar(50) NOT NULL,
  `is_active` tinyint(3) NOT NULL,
  `created_by` varchar(36) NOT NULL DEFAULT '',
  `updated_by` varchar(36) NOT NULL DEFAULT '',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `FK_317_683` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE `currency_rates` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` char(36) NOT NULL,
  `currency_id` tinyint(3) unsigned NOT NULL,
  `base_currency_id` tinyint(3) unsigned NOT NULL,
  `rate` decimal(19,6) NOT NULL,
  `provider_id` int(11) unsigned NOT NULL,
  `status` varchar(15) NOT NULL,
  `updated_by` varchar(36) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `currency_exchange_profile` (`currency_id`,`base_currency_id`,`provider_id`),
  KEY `fk_base_currency_id` (`base_currency_id`),
  KEY `fk_providers_id` (`provider_id`),
  CONSTRAINT `fk_base_currency_id` FOREIGN KEY (`base_currency_id`) REFERENCES `currencies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_providers_id` FOREIGN KEY (`provider_id`) REFERENCES `providers` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE `currency_spreads` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` char(36) NOT NULL,
  `currency_rate_id` int(11) unsigned NOT NULL,
  `transaction_type` VARCHAR(30) NOT NULL,
  `channel` VARCHAR(30) DEFAULT NULL,
  `institution` VARCHAR(30) DEFAULT NULL,
  `spread` decimal(19,6) NOT NULL,
  `deleted_at` DATETIME DEFAULT NULL,
  `created_by` varchar(36) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_by` varchar(36) DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fk_currency_exchange_id` FOREIGN KEY (`currency_rate_id`) REFERENCES `currency_rates` (`id`)
);

CREATE TABLE `currency_spreads_history` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `spread_id` int(10) unsigned NOT NULL,
  `new_spread` decimal(19,6) NOT NULL,
  `old_spread` decimal(19,6) NOT NULL,
  `reason` varchar(100) DEFAULT NULL,
  `updated_by` varchar(36) NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_spread_id` (`spread_id`),
  CONSTRAINT `fk_spread_id` FOREIGN KEY (`spread_id`) REFERENCES `currency_spreads` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE `limit_profiles` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `limit_type` varchar(20) DEFAULT NULL,
  `user_type` varchar(20) DEFAULT NULL,
  `tier` varchar(20) DEFAULT NULL,
  `subscription` varchar(20) DEFAULT NULL,
  `transaction_type` varchar(20) DEFAULT NULL,
  `channel` varchar(20) DEFAULT NULL,
  `provider_id` int(10) unsigned DEFAULT NULL,
  `instrument` varchar(20) DEFAULT NULL,
  `max_interval_amount` decimal(10,2) DEFAULT NULL,
  `max_amount` decimal(10,2) DEFAULT NULL,
  `min_amount` decimal(10,2) DEFAULT NULL,
  `max_count` int(11) DEFAULT NULL,
  `interval` varchar(20) DEFAULT NULL,
  `currency_id` int(10) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `uuid` varchar(36) NOT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `created_by` varchar(36) NOT NULL,
  `updated_by` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_limit_profile_currency_id` (`currency_id`),
  CONSTRAINT `fk_limit_profile_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`provider_id`) REFERENCES `providers` (`id`),
  UNIQUE KEY `uuid_UNIQUE` (`uuid`)
);

CREATE TABLE `limit_profile_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `limit_profile_id` int(10) unsigned DEFAULT NULL,
  `old_max_interval_amount` decimal(19,2) DEFAULT NULL,
  `old_max_amount` decimal(19,2) DEFAULT NULL,
  `old_min_amount` decimal(19,2) DEFAULT NULL,
  `old_max_count` decimal(19,2) DEFAULT NULL,
  `new_max_interval_amount` decimal(19,2) DEFAULT NULL,
  `new_max_amount` decimal(19,2) DEFAULT NULL,
  `new_min_amount` decimal(19,2) DEFAULT NULL,
  `new_max_count` decimal(19,2) DEFAULT NULL,
  `updated_by` varchar(36) NOT NULL,
  `updated_at` datetime NOT NULL,
  `reason` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_limit_profile_id_idx` (`limit_profile_id`),
  CONSTRAINT `fk_limit_profile_id` FOREIGN KEY (`limit_profile_id`) REFERENCES `limit_profiles` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE `fee_profiles` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `fee_type` varchar(20) NOT NULL,
  `user_type` varchar(20) NOT NULL,
  `tier` varchar(20) NOT NULL,
  `subscription_type` varchar(20) NOT NULL,
  `transaction_type` varchar(20) NOT NULL,
  `channel` varchar(20) DEFAULT NULL,
  `provider_id` int(10) unsigned DEFAULT NULL,
  `instrument` varchar(20) DEFAULT NULL,
  `calculation_method` varchar(50) NOT NULL,
  `max_fee` decimal(10,2) DEFAULT NULL,
  `min_fee` decimal(10,2) DEFAULT NULL,
  `fee_amount` decimal(10,2) DEFAULT NULL,
  `fee_ratio` decimal(10,4) DEFAULT NULL,
  `fee_method` varchar(20) NOT NULL,
  `tax_included` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `uuid` varchar(36) NOT NULL,
  `created_by` varchar(36) NOT NULL,
  `updated_by` varchar(36) NOT NULL,
  `currency_id` tinyint(3) unsigned NOT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid_UNIQUE` (`uuid`),
  KEY `fk_fee_profile_currency_id` (`currency_id`),
  CONSTRAINT `fk_fee_profile_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`provider_id`) REFERENCES `providers` (`id`)
);

CREATE TABLE `fee_profile_ranges` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `fee_profile_id` int(10) unsigned DEFAULT NULL,
  `min` int(10) unsigned DEFAULT NULL,
  `max` int(10) unsigned DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `fee_ratio` decimal(10,4) DEFAULT NULL,
  `fee_amount` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_fee_profile_id` (`fee_profile_id`),
  CONSTRAINT `fk_fee_profile_id` FOREIGN KEY (`fee_profile_id`) REFERENCES `fee_profiles` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

# --- !Downs
DROP TABLE IF EXISTS `individual_users`;
DROP TABLE IF EXISTS `nationalities`;
DROP TABLE IF EXISTS `companies`;
DROP TABLE IF EXISTS `occupations`;
DROP TABLE IF EXISTS `employers`;
DROP TABLE IF EXISTS `user_applications`;