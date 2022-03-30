# --- !Ups

CREATE TABLE IF NOT EXISTS `internal_recon_tracker`(
  `id` varchar(36) NOT NULL PRIMARY KEY,
  `recon_date` datetime NOT NULL,
  `start_account_id` INT DEFAULT NULL,
  `end_account_id` INT DEFAULT NULL,
  `status` varchar(15) NOT NULL,
  `last_successful_account_id` INT DEFAULT NULL,
  `total_num_accounts` INT NOT NULL
);

CREATE TABLE IF NOT EXISTS `internal_recon_daily_summary` (
  `id` varchar(36) NOT NULL PRIMARY KEY,
  `recon_date` datetime NOT NULL,
  `account_id` varchar(36) NOT NULL,
  `account_number` varchar(50) NOT NULL,
  `account_type` varchar(50) NOT NULL,
  `main_account_type` varchar(50) NOT NULL,
  `user_uuid` varchar(36) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `end_of_day_balance` decimal(20,4) NOT NULL,
  `value_change` decimal(20,4) NOT NULL,
  `transaction_total_amount` decimal(20,4) NOT NULL,
  `transaction_total_count` INT NOT NULL,
  `problematic_transaction_count` INT NOT NULL,
  `status` varchar(15) NOT NULL,
  `comments` varchar(256) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` varchar(36) DEFAULT NULL);

CREATE TABLE IF NOT EXISTS `internal_recon_daily_details` (
  `id` varchar(36) NOT NULL PRIMARY KEY,
  `internal_reconciliation_summary_id` varchar(36) NOT NULL,
  `recon_date` datetime NOT NULL,
  `account_id` varchar(36) NOT NULL,
  `account_number` varchar(50) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `current_txn_id` bigint NOT NULL,
  `current_txn_sequence` bigint NOT NULL,
  `current_txn_direction` varchar(10) NOT NULL,
  `current_txn_timestamp` datetime NOT NULL,
  `current_txn_amount` decimal(20,4) NOT NULL,
  `current_txn_previous_balance` decimal(20,4) NULL,

  `previous_txn_id` bigint DEFAULT NULL,
  `previous_txn_sequence` bigint DEFAULT NULL,
  `previous_txn_direction` varchar(10) DEFAULT NULL,
  `previous_txn_timestamp` datetime DEFAULT NULL,
  `previous_txn_amount` decimal(20,4) DEFAULT NULL,
  `previous_txn_previous_balance` decimal(20,4) DEFAULT NULL,

  `recon_status` varchar(15) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP);
  

# --- !Downs

DROP TABLE `internal_recon_daily_summary`;
DROP TABLE `internal_recon_daily_details`;
DROP TABLE `internal_recon_tracker`;