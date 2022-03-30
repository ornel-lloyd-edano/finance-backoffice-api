# --- !Ups

CREATE TABLE `system_settings` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `key` varchar(100) DEFAULT NULL,
  `value` varchar(100) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(10 ) NOT NULL,
  `for_android` tinyint(3) NOT NULL DEFAULT 0,
  `for_ios` tinyint(3) NOT NULL DEFAULT 0,
  `for_backoffice` tinyint(3) NOT NULL DEFAULT 0,
  `created_by` varchar(36) NOT NULL,
  `updated_by` varchar(36) DEFAULT NULL,
  `explanation` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `key` (`key`)
);


# --- !Downs
DROP TABLE IF EXISTS `system_settings`;
