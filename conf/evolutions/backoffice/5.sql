# --- !Ups


CREATE TABLE `auto_deduct_savings` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned DEFAULT NULL,
  `saving_account_id` int(10) unsigned DEFAULT NULL,
  `current_amount` decimal(10,2) DEFAULT NULL,
  `saving_percentage` int(10) unsigned DEFAULT NULL,
  `min_income` int(10) unsigned DEFAULT '0',
  `status_updated_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(4) NOT NULL DEFAULT '0',
  `uuid` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid_autodeduct_savings` (`uuid`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `fk_account_account_id` (`saving_account_id`),
  CONSTRAINT `fk_users_userid` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_account_account_id` FOREIGN KEY (`saving_account_id`) REFERENCES `accounts` (`id`)
);


CREATE TABLE `roundup_savings` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned DEFAULT NULL,
  `saving_account_id` int(10) unsigned DEFAULT NULL,
  `current_amount` decimal(10,2) DEFAULT NULL,
  `rounding_nearest` int(10) unsigned DEFAULT NULL,
  `status_updated_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(4) NOT NULL DEFAULT '0',
  `uuid` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid_roundup_savings` (`uuid`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `fk_accounts_account_id` (`saving_account_id`),
  CONSTRAINT `fk_users_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_accounts_account_id` FOREIGN KEY (`saving_account_id`) REFERENCES `accounts` (`id`)
);


CREATE TABLE `saving_goals` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned DEFAULT NULL,
  `saving_account_id` int(10) unsigned DEFAULT NULL,
  `currency_id` tinyint(3) unsigned NOT NULL,
  `goal_amount` int(10) unsigned DEFAULT NULL,
  `current_amount` int(10) unsigned DEFAULT NULL,
  `initial_amount` int(10) unsigned DEFAULT NULL,
  `emi_amount` int(10) unsigned DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `name` varchar(50) DEFAULT NULL,
  `reason` varchar(50) DEFAULT NULL,
  `payment_type` varchar(15) DEFAULT NULL, --enum('automated','manual')  DEFAULT NULL,
  `status` varchar(15) DEFAULT NULL, --enum('active','frozen','cancelled','reached','achieved')  DEFAULT NULL,
  `status_updated_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `uuid` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid_saving_goals` (`uuid`),
  KEY `fk_currency_id` (`currency_id`),
  KEY `fk_saving_account_id` (`saving_account_id`),
  KEY `fk_user_id` (`user_id`),
  CONSTRAINT `fk_saving_goals_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`),
  CONSTRAINT `fk_saving_goals_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_saving_account_id` FOREIGN KEY (`saving_account_id`) REFERENCES `accounts` (`id`)
);

# --- !Downs

DROP TABLE `auto_deduct_savings`;

DROP TABLE `roundup_savings`;

DROP TABLE `saving_goals`;