# --- !Ups
CREATE TABLE `transaction_reversals` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `reversed_transaction_id` varchar(36) NOT NULL,
  `reversal_transaction_id` varchar(36) NOT NULL,
  `reason` varchar(100) NOT NULL,
  `status` varchar(50) NOT NULL,
  `created_by` varchar(36) NOT NULL DEFAULT '',
  `updated_by` varchar(36) NOT NULL DEFAULT '',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

# --- !Downs
DROP TABLE IF EXISTS `transaction_reversals`;