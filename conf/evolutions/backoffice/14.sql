# --- !Ups

CREATE TABLE `vp_users` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  `name` varchar(50) NOT NULL,
  `middle_name` varchar(50) DEFAULT NULL,
  `surname` varchar(50) NOT NULL,
  `msisdn` varchar(50) NOT NULL,
  `email` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `created_by` varchar(36) NOT NULL,
  `updated_by` varchar(36) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `password_changed_at` datetime DEFAULT NULL,
  `blocked_at` datetime DEFAULT NULL,
  `failed_logins` int(10) DEFAULT '0',
  `username` varchar(50) NOT NULL,
  `role` varchar(50) NOT NULL DEFAULT 'admin',
  `last_login_at` datetime DEFAULT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'active',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
);

# --- !Downs

DROP TABLE `vp_users`;