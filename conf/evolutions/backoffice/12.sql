# --- !Ups

CREATE TABLE `third_party_fee_profiles` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `transaction_type` varchar(20) DEFAULT NULL,
  `provider_id` int(10) unsigned DEFAULT NULL,
  `calculation_method` varchar(50) NOT NULL,
  `max_fee` decimal(10,2) DEFAULT NULL,
  `min_fee` decimal(10,2) DEFAULT NULL,
  `fee_amount` decimal(10,2) DEFAULT NULL,
  `fee_ratio` decimal(10,4) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `created_by` varchar(36) NOT NULL,
  `updated_by` varchar(36) NOT NULL,
  `currency_id` tinyint(3) unsigned NOT NULL,
  `is_active` tinyint default 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_third_party_fee_profile` (`transaction_type`, `provider_id`, `currency_id`),
  KEY `fk_third_party_fee_profile_currency_id` (`currency_id`),
  CONSTRAINT `fk_third_party_fee_profile_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (`provider_id`) REFERENCES `providers` (`id`)
);

CREATE TABLE `third_party_fee_profile_ranges` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `third_party_fee_profile_id` int(10) unsigned DEFAULT NULL,
  `min` int(10) unsigned DEFAULT NULL,
  `max` int(10) unsigned DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `fee_ratio` decimal(10,4) DEFAULT NULL,
  `fee_amount` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_third_party_fee_profile_id` (`third_party_fee_profile_id`),
  CONSTRAINT `fk_third_party_fee_profile_id` FOREIGN KEY (`third_party_fee_profile_id`) REFERENCES `third_party_fee_profiles` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

# --- !Downs

DROP TABLE `third_party_fee_profiles`;
DROP TABLE `third_party_fee_profile_ranges`;