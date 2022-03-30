# --- !Ups

CREATE TABLE `report_definitions` (
  `id` CHAR(36) NOT NULL PRIMARY KEY,
  `report_name` VARCHAR(32) NOT NULL,
  `report_title` varchar(500) DEFAULT NULL,
  `report_description` varchar(500) DEFAULT NULL,
  `report_columns` text,
  `parameters` text,
  `joins` text,
  `grouping_columns` text,
  `ordering` text,
  `raw_sql` varchar(1000) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_at` datetime NOT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  `paginated` tinyint(1) DEFAULT NULL,
  FOREIGN KEY (`report_name`) REFERENCES scopes(`name`),
  UNIQUE KEY unique_report_name (`report_name`)
);

# --- !Downs

DROP TABLE `report_definitions`
