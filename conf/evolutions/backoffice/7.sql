# --- !Ups
CREATE TABLE `notification_templates` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `uuid` char(36) UNIQUE not null,
    `name` varchar(36) not null,
    `title_resource` varchar(128) not null,
    `default_title`    VARCHAR(128) NOT NULL,
    `content_resource` varchar(128) not null,
    `default_content`  VARCHAR(512) NOT NULL,
    `channels` varchar(32) not null,
    `description`      VARCHAR(128) NOT NULL,
    `created_at` datetime not null,
    `created_by` varchar(36) not null,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` varchar(36) default null,
    `is_active` tinyint default 1,

  PRIMARY KEY (`id`)
);


CREATE TABLE `notifications` (
`id` int(10) unsigned NOT NULL AUTO_INCREMENT,
`uuid`      VARCHAR(36) NOT NULL,
`template_id` int(10) unsigned NOT NULL,
`channel` varchar(32) not null,
`title` varchar(128) not null,
`content` text not null,
`operation_id`   VARCHAR(36) NOT NULL DEFAULT '',
`address` varchar(128) not null,
`user_id` int default null,
`status` varchar(15) not null,

`sent_at` datetime default null,
`error_message` varchar(128) default null,
`retries` int default 0,

`created_at` datetime not null,
`created_by` varchar(36) not null,
`updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`updated_by` varchar(36) default null,

PRIMARY KEY (`id`),
FOREIGN KEY (`template_id`) REFERENCES notification_templates(`id`),
FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
);







# --- !Downs
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `notification_templates`;