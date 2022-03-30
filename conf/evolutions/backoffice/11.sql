# --- !Ups

CREATE TABLE `business_user_applications` (
  id int(10) unsigned NOT NULL AUTO_INCREMENT,
  uuid varchar(36) NOT NULL,
  business_name varchar(36) NOT NULL,
  brand_name varchar(36) NOT NULL,
  business_category varchar(36) NOT NULL,
  stage varchar(30) NOT NULL,
  status varchar(30) NOT NULL,
  user_tier varchar(30) NOT NULL,
  business_type varchar(50) NOT NULL,
  registration_number varchar(50) NOT NULL,
  tax_number varchar(50) DEFAULT NULL,
  registration_date date DEFAULT NULL,
  explanation varchar(256) NULL,
  user_id int(10) unsigned DEFAULT NULL,
  submitted_by varchar(50) NULL,
  submitted_at datetime NULL,
  checked_by varchar(50) NULL,
  checked_at datetime NULL,
  created_by varchar(50) NOT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_by varchar(50) DEFAULT NULL,
  updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (id));

ALTER TABLE `application_documents`
ADD FOREIGN KEY (bu_application_id) REFERENCES business_user_applications(id);

CREATE TABLE business_user_application_txn_configs (
 id int unsigned NOT NULL AUTO_INCREMENT,
 uuid varchar(36) NOT NULL,
 application_id int unsigned NOT NULL,
 txn_type varchar(30) NOT NULL,
 currency_id tinyint(3) unsigned NOT NULL,
 created_by varchar(50) NOT NULL,
 created_at datetime DEFAULT CURRENT_TIMESTAMP,
 updated_by varchar(50) DEFAULT NULL,
 updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (id),
 FOREIGN KEY(application_id) REFERENCES business_user_applications(id),
 FOREIGN KEY(currency_id) REFERENCES currencies(id)
);

CREATE TABLE business_user_application_account_configs (
 id int unsigned NOT NULL AUTO_INCREMENT,
 uuid varchar(36) NOT NULL,
 application_id int unsigned NOT NULL,
 account_type varchar(50) NOT NULL,
 account_name varchar(50) NOT NULL,
 currency_id tinyint(3) unsigned NOT NULL,
 is_default tinyint(1) NOT NULL DEFAULT 0,
 created_by varchar(50) NOT NULL,
 created_at datetime DEFAULT CURRENT_TIMESTAMP,
 updated_by varchar(50) DEFAULT NULL,
 updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (id),
 FOREIGN KEY(application_id) REFERENCES business_user_applications(id),
 FOREIGN KEY(currency_id) REFERENCES currencies(id)
);

CREATE TABLE business_user_application_external_accounts (
 id int unsigned NOT NULL AUTO_INCREMENT,
 uuid varchar(36) NOT NULL,
 application_id int unsigned NOT NULL,
 provider varchar(100) NOT NULL,
 account_number varchar(100) NOT NULL,
 account_holder varchar(100) NOT NULL,
 currency_id tinyint(3) unsigned NOT NULL,
 created_by varchar(50) NOT NULL,
 created_at datetime DEFAULT CURRENT_TIMESTAMP,
 updated_by varchar(50) DEFAULT NULL,
 updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (id),
 FOREIGN KEY(application_id) REFERENCES business_user_applications(id),
 FOREIGN KEY(currency_id) REFERENCES currencies(id)
);

CREATE TABLE description_types(
id INT AUTO_INCREMENT,
`type` varchar(20) NOT NULL UNIQUE,
created_at DATETIME NOT NULL,
created_by varchar(36) NOT NULL,
updated_at DATETIME DEFAULT NULL,
updated_by varchar(36) DEFAULT NULL,
PRIMARY KEY(id)
);

CREATE TABLE descriptions(
id INT AUTO_INCREMENT,
name varchar(36) NOT NULL,
description varchar(128) DEFAULT NULL,
value_id INT NOT NULL,
PRIMARY KEY(id),
CONSTRAINT `fk_description_types_id` FOREIGN KEY (`value_id`) REFERENCES `description_types` (`id`)
);

# --- !Downs

DROP TABLE `business_user_applications`
DROP TABLE `business_user_application_txn_configs`
DROP TABLE `business_user_application_account_configs`
DROP TABLE `business_user_application_external_accounts`
DROP TABLE `descriptions`;
DROP TABLE `description_types`;
