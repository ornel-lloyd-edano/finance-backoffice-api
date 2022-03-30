# --- !Ups

CREATE TABLE `settlements` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL UNIQUE,
  `reason` varchar(128) NOT NULL,
  `status` varchar(15) NOT NULL,
  `fx_provider` varchar(50) DEFAULT NULL,
  `from_currency_id` tinyint(3) unsigned DEFAULT NULL,
  `to_currency_id` tinyint(3) unsigned DEFAULT NULL,
  `fx_rate` decimal(19,6) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(36) NOT NULL,
  `checked_by` varchar(36) DEFAULT NULL,
  `checked_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_settlement_from_currency_id` FOREIGN KEY (`from_currency_id`) REFERENCES `currencies` (`id`),
  CONSTRAINT `fk_settlement_to_currency_id` FOREIGN KEY (`to_currency_id`) REFERENCES `currencies` (`id`)
);

CREATE TABLE `settlement_lines` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `settlement_id` int(10) unsigned NOT NULL,
  `account_id` int(10) unsigned NOT NULL,
  `direction` varchar(10) NOT NULL,
  `currency_id` tinyint(3) unsigned DEFAULT NULL,
  `amount` decimal(19,4) NOT NULL,
  `explanation` varchar(128) NOT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_settlement_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`),
  CONSTRAINT `fk_settlement_id` FOREIGN KEY (`settlement_id`) REFERENCES `settlements` (`id`),
  CONSTRAINT `fk_account_id` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
);

# --- !Downs

DROP TABLE `settlement_lines`;
DROP TABLE `settlements`;