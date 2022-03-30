# --- !Ups

CREATE TABLE IF NOT EXISTS `commission_profiles` (
  `id`                 INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36)   NOT NULL,
  `business_type`      VARCHAR(50) NOT NULL,
  `tier`               VARCHAR(20) NOT NULL,
  `subscription_type`  VARCHAR(20) NOT NULL,
  `transaction_type`   VARCHAR(50) NOT NULL,
  `currency_id`        TINYINT(3) unsigned NOT NULL,
  `channel`            VARCHAR(20),
  `instrument`         VARCHAR(20),
  `calculation_method` VARCHAR(50) NOT NULL,
  `max_commission`            DECIMAL(10, 2),
  `min_commission`            DECIMAL(10, 2),
  `commission_amount`         DECIMAL(10, 2),
  `commission_ratio`          DECIMAL(10, 4),
  `created_by` varchar(36) NOT NULL,
  `updated_by` varchar(36) NOT NULL,
  `created_at`         DATETIME NOT NULL,
  `updated_at`         DATETIME NOT NULL,
  `deleted_at`         DATETIME,
  PRIMARY KEY(`id`),
  FOREIGN KEY(`currency_id`) REFERENCES `currencies`(`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `commission_profile_ranges` (
  `id`             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `commission_profile_id` INT UNSIGNED,
  `min`            INT UNSIGNED,
  `max`            INT UNSIGNED,
  `commission_amount`     DECIMAL(10, 2),
  `commission_ratio`      DECIMAL(10, 4),
  `created_at`     DATETIME NOT NULL,
  `updated_at`     DATETIME NOT NULL,
  PRIMARY KEY(`id`),
  FOREIGN KEY(`commission_profile_id`) REFERENCES `commission_profiles`(`id`)
);

# --- !Downs

DROP TABLE `commission_profile_ranges`;