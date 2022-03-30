# --- !Ups

CREATE TABLE `i18n_strings` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `key` varchar(100) NOT NULL,
  `text` varchar(300) NOT NULL,
  `locale` varchar(10) NOT NULL,
  `platform` varchar(50) NOT NULL,
  `type` varchar(20) NULL,
  `explanation` varchar(128) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_key_text_locale_platform` (`key`,`locale`,`platform`)
);


# --- !Downs
DROP TABLE IF EXISTS `i18n_strings`;
