# --- !Ups

CREATE TABLE `tasks` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL UNIQUE,

  `module` varchar(36) NOT NULL,
  `action` varchar(64) NOT NULL,
  `verb` varchar(10) NOT NULL,
  `url` varchar(128) NOT NULL,
  `headers` text NOT NULL,
  `body` text DEFAULT NULL,
  `status` varchar(10) NOT NULL,
  `maker_level` int UNSIGNED NOT NULL,
  `maker_business_unit` varchar(36) NOT NULL,
  `value_to_update` text NULL,

  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(36) NOT NULL,
  `checked_by` varchar(36) DEFAULT NULL,
  `checked_at` datetime DEFAULT NULL,
  `reason`     varchar(128) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

# --- !Downs

DROP TABLE `tasks`