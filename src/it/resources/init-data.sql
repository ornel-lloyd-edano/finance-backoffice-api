INSERT INTO `currencies` (`id`,`currency_name`,`description`,`created_at`,`created_by`,`updated_at`,`updated_by`,`is_active`, icon) VALUES
('1', 'AED', 'Dirham', '2019-01-20 10:52:40', 'admin', '2019-01-20 10:52:40', 'admin', '1', 'aed_icon'),
('2', 'INR', 'Indian Rupee', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '1', 'inr_icon'),
('3', 'USD', 'US Dollar', '2019-01-20 10:52:40', 'admin', '2019-01-20 10:52:40', 'admin', '1', 'usd_icon'),
('4', 'EUR', 'Euro', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '1', 'eur_icon'),
('5', 'CNY', 'Chinese Yuan', '2019-01-20 10:52:40', 'admin', '2019-01-20 10:52:40', 'admin', '1', 'cny_icon'),
('6', 'CHF', 'Swiss Franc', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '1', 'chf_icon'),
('7', 'PHP', 'Ph Peso', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '0', 'php_icon'),
('8', 'KES', 'Kenyang Shilling', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '0', 'kes_icon');

INSERT INTO `countries`(`id`, `name`, `label`, `icon`, `is_active`, `created_at`, `updated_at`, `currency_id`) VALUES
('1', 'UAE', null, null,   '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00', '1'),
('2', 'India', null, null, '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00', '2'),
('3', 'Philippines', null, null, '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00', '7');


INSERT INTO `account_types` (`id`,`account_type_name`,`description`,`created_at`,`created_by`,`updated_at`,`updated_by`,`is_active`) VALUES
('1', 'standard', 'abcd', '2019-01-20 10:52:55', 'admin', '2019-01-20 10:52:55', NULL, '1'),
('2', 'standard_wallet', NULL, '2019-01-20 10:52:55', 'admin', '2019-01-20 10:52:55', NULL, '1'),
('3', 'standard_saving', NULL, '2019-01-20 10:52:55', 'admin', '2019-01-20 10:52:55', NULL, '1'),
('4', 'collection', NULL, '2019-01-20 10:52:55', 'admin', '2019-01-20 10:52:55', NULL, '1'),
('5', 'distribution', NULL, '2019-01-20 10:52:55', 'admin', '2019-01-20 10:52:55', NULL, '1'),
('6', 'utility', NULL, '2019-01-20 10:52:55', 'admin', '2019-01-20 10:52:55', NULL, '1'),
('7', 'fee_collection', NULL, '2019-01-20 10:52:55', 'admin', '2019-01-20 10:52:55', NULL, '1');

INSERT INTO `users` (`id`,`uuid`,`username`,`password`,`type`,`tier`,`segment`,`subscription`,`email`,`status`,`activated_at`,`password_updated_at`,`created_at`,`created_by`,`updated_at`,`updated_by`) VALUES
('1', '2205409c-1c83-11e9-a2a9-000c297e3e45', '0000000001', 'password', 'individual', 'basic', 'new', 'standard', NULL, 'active', '2019-01-03 06:26:21', '2019-01-03 06:26:21', '2019-01-20 07:22:46', 'admin', '2019-01-03 06:26:21', NULL),
('2', 'efe3b069-476e-4e36-8d22-53176438f55f', '+97123456789', '885B3EDC975842A4D84B9DF5D29E009641A4562259DA2D2D49FBB19CEF56E2E5', 'individual', 'basic', 'new', 'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 09:15:29', 'wallet_api', '2019-01-20 09:15:29', 'wallet_api'),
('3', '910f02a0-48ef-418d-ac0a-06eff7ac9c90', '+971544451674', '5A697C6768FCB4F363874B4D73C517A6E7F8932D23C31B6EA52BEBD2C3F4AA05', 'individual', 'basic', 'new', 'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 09:50:55', 'wallet_api', '2019-01-20 09:50:55', 'wallet_api'),
('4', 'aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d', '+971522106589', '5A697C6768FCB4F363874B4D73C517A6E7F8932D23C31B6EA52BEBD2C3F4AA05', 'individual', 'basic', 'new', 'standard', NULL, 'active', '2019-01-22 11:29:28', NULL, '2019-01-20 09:57:37', 'wallet_api', '2019-01-22 11:29:28', 'UNKNOWN_USER'),
('6', '0b507259-2c3a-48fd-97dc-3760a3756e6d', '+971507472520', '53D9D268DCBE83836BAD147C840EC7027F184D1B6B062197825716D30F472575', 'individual', 'basic', 'new', 'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 10:15:13', 'wallet_api', '2019-01-20 10:15:13', 'wallet_api'),
('7', '0e312dbc-5171-42af-9263-639b8964172e', '+971544451683', '3C504B0EF35FA7704D026B74B058F2FB846CF5AB1F4396F1E0956500D8AFF4B3', 'individual', 'basic', 'new', 'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 11:10:56', 'wallet_api', '2019-01-20 11:10:56', 'wallet_api'),
('11', '3f19fb56-a961-406f-b815-7078efcb6df4', '+971544451680', '3C504B0EF35FA7704D026B74B058F2FB846CF5AB1F4396F1E0956500D8AFF4B3', 'individual', 'basic', 'new', 'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 12:40:58', 'wallet_api', '2019-01-20 12:40:58', 'wallet_api'),
('12', '7f66c98c-9c45-4995-8829-70a62181df86', '+971589721075', '5A697C6768FCB4F363874B4D73C517A6E7F8932D23C31B6EA52BEBD2C3F4AA05', 'individual', 'basic', 'new', 'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 12:43:15', 'wallet_api', '2019-01-20 12:43:15', 'wallet_api'),
('14', 'bdf50f79-0f52-4b0a-86b9-2d30c5e57b8a', '+971544550982', '5A697C6768FCB4F363874B4D73C517A6E7F8932D23C31B6EA52BEBD2C3F4AA05', 'individual', 'basic', 'new', 'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 13:06:14', 'wallet_api', '2019-01-20 13:06:14', 'wallet_api'),
('15', 'bdf50f79-0f52-4b0a-86b9-2d30c5e57c8b', '+971544550983', '5A697C6768FCB4F363874B4D73C517A6E7F8932D23C31B6EA52BEBD2C3F4AA05', 'individual', 'basic', 'new', 'standard', NULL, 'passive', NULL, NULL, '2019-01-20 13:06:14', 'wallet_api', '2019-01-20 13:06:14', 'wallet_api');

INSERT INTO `individual_users` (id, msisdn, user_id, type, name, fullname,  gender,person_id, company, birthdate, birth_place, nationality, occupation, employer, created_at, created_by, updated_at, updated_by) VALUES
('1', '+97123456789', '2', 'standard', 'marcos', 'Test','M', 'A1', NULL, NULL, NULL, NULL, NULL, NULL, '2019-01-20 09:15:30', 'wallet_api', '2019-01-20 09:15:30', 'wallet_api'),
('2', '+971544451674', '3', 'standard', 'marcos', 'Test', 'M','A1', NULL, NULL, NULL, NULL, NULL, NULL, '2019-01-20 09:50:55', 'wallet_api', '2019-01-20 09:50:55', 'wallet_api'),
('3', '+971522106589', '4', 'standard', 'Dave', 'Test', 'M','A1', NULL, NULL, NULL, NULL, NULL, NULL, '2019-01-20 09:57:37', 'wallet_api', '2019-01-20 09:57:37', 'wallet_api'),
('5', '+971507472520', '6', 'standard', 'Red', 'Test', 'F','A1', NULL, NULL, NULL, NULL, NULL, NULL, '2019-01-20 10:15:13', 'wallet_api', '2019-01-20 10:15:13', 'wallet_api'),
('6', '+971544451683', '7', 'standard', 'Vulkan', 'Test','A2', 'M', NULL, NULL, NULL, NULL, NULL, NULL, '2019-01-20 11:10:56', 'wallet_api', '2019-01-20 11:10:56', 'wallet_api'),
('8', '+971544451680', '11', 'standard', 'Alex', NULL, 'M', 'A2', NULL, NULL, NULL, NULL, NULL, NULL, '2019-01-20 12:40:58', 'wallet_api', '2019-01-20 12:40:58', 'wallet_api'),
('9', '+971589721075', '12', 'standard', 'Maria', 'imelda marcos', 'F','A2', NULL, NULL, NULL, NULL, NULL, NULL, '2019-01-20 12:43:15', 'wallet_api', '2019-01-20 12:43:15', 'wallet_api'),
('10', '+971544550982', '14', 'standard', 'Abhishek', 'Demo', 'M','A3', NULL, NULL, NULL, NULL, NULL, NULL, '2019-01-20 13:06:14', 'wallet_api', '2019-01-20 13:06:14', 'wallet_api');

INSERT INTO `accounts` (`id`,`uuid`,`number`,`user_id`,`name`,`account_type_id`,`is_main_account`,`currency_id`,`balance`,`blocked_balance`,`status`,`closed_at`,`last_transaction_at`,`created_at`,          `updated_at`,             `updated_by`,`created_by`,  `main_type`)  VALUES
('1', '273bd341-1c83-11e9-a2a9-000c297e3e45', '1.1', '1', '1_wallet', '1', '1', '1', '0.00', '0.00', 'active', NULL, NULL,                                                             '2019-01-03 06:26:21', '2019-01-03 06:26:21',    'admin',     'admin',       'asset'),
('2', '273bd2d6-1c83-11e9-a2a9-000c297e3e45', 'pegb_vouchers', '1', '1_utility', '1', '0', '1', '0.00', '0.00', 'active', NULL, NULL,                                                  '2019-01-03 06:26:21', '2019-01-03 06:26:21',    'admin',     'admin',       'asset'),
('3', '273be701-1c83-11e9-a2a9-000c297e3e45', 'pegb_fees', '1', '1_fee_collection', '1', '0', '1', '1500.50', '0.00', 'active', NULL, NULL,                                            '2019-01-03 06:26:21', '2019-01-21 15:28:48',    'Analyn',     'admin',      'asset'),
('6', '03668dcd-0e0f-4c3e-8b8b-12847c2ff3f7', '2.1', '2', '+97123456789_standard_wallet', '2', '1', '1', '0.00', '0.00', 'active', NULL, NULL,                                         '2019-01-20 09:15:29', '2019-01-20 09:15:29',    'wallet_api', 'wallet_api', 'asset'),
('7', '4cf577ec-d410-49b1-843d-4ba3509a11b7', '3.1', '3', '+971544451674_standard_wallet', '2', '1', '1', '0.00', '0.00', 'active', NULL, NULL,                                        '2019-01-20 09:50:55', '2019-01-20 09:50:55',    'wallet_api', 'wallet_api', 'asset'),
('8', '734a4d3e-4327-45a4-96f6-e1eca9b4b442', '4.1', '4', '+971522106589_standard_wallet', '2', '1', '1', '0.00', '0.00', 'active', NULL, NULL,                                        '2019-01-20 09:57:37', '2019-01-20 09:57:37',    'wallet_api', 'wallet_api', 'asset'),
('10', 'b3b9785b-4890-489d-8661-7b6813aa8b03', '6.1', '6', '+971507472520_standard_wallet', '2', '1', '1', '135.40', '0.00', 'active', NULL, NULL,                                     '2019-01-20 10:15:13', '2019-01-20 10:15:13',    'wallet_api', 'wallet_api', 'asset'),
('11', '3648be09-8c51-49bb-ab48-a303fe113bb6', '7.1', '7', '+971544451683_standard_wallet', '2', '1', '1', '0.00', '0.00', 'active', NULL, NULL,                                       '2019-01-20 11:10:56', '2019-01-20 11:10:56',    'wallet_api', 'wallet_api', 'asset'),
('14', 'f6065958-0ab9-4297-9176-fdcf06b93972', '11.1', '11', '+971544451680_standard_wallet', '2', '1', '1', '0.00', '0.00', 'active', NULL, NULL,                                     '2019-01-20 12:40:58', '2019-01-20 12:40:58',    'wallet_api', 'wallet_api', 'asset'),
('15', '814fb034-145c-4652-9421-e5008c7aa5f2', '12.1', '12', '+971589721075_standard_wallet', '2', '1', '1', '0.00', '0.00', 'active', NULL, NULL,                                     '2019-01-20 12:43:15', '2019-01-20 12:43:15',    'wallet_api', 'wallet_api', 'asset'),
('17', 'c12ba2fa-129c-4c25-b920-2f85a6428b5f', '14.1', '14', '+971544550982_standard_wallet', '2', '1', '1', '0.00', '0.00', 'active', NULL, NULL,                                     '2019-01-20 13:06:14', '2019-01-20 13:06:14',   'wallet_api', 'wallet_api',  'asset'),
('18', '71e3c129-5cde-437b-8a7d-3791798cea92', '1.2', '14', 'USD Escrow', '6', '0', '3', '17526.5', '0.00', 'active', NULL, NULL,                                                      '2019-01-20 13:06:14', '2019-01-20 13:06:14',   'wallet_api', 'wallet_api',  'liability'),
('19', '6a8ff726-82ed-4571-9421-e76e63ce386d', '1.3', '14', 'EURO Escrow', '6', '0', '4', '10519.0', '0.00', 'active', NULL, NULL,                                                     '2019-01-20 13:06:14', '2019-01-20 13:06:14',   'wallet_api', 'wallet_api',  'liability'),
('20', 'e5e3c613-c3ea-469b-bf20-ab3fed38cd6e', '1.4', '14', 'CNY Escrow', '6', '0', '5', '0.00', '0.00', 'active', NULL, NULL,                                                         '2019-01-20 13:06:14', '2019-01-20 13:06:14',   'wallet_api', 'wallet_api',  'liability'),
('21', 'acfe39ba-9d52-48ee-b4d2-9ef8c11f329f', '1.5', '14', 'CHF Escrow', '6', '0', '6', '0.00', '0.00', 'active', NULL, NULL,                                                         '2019-01-20 13:06:14', '2019-01-20 13:06:14',   'wallet_api', 'wallet_api',  'liability'),
('22', 'bcfe39ba-9d52-48ee-b4d2-9ef8c11f329f', '1.6', '14', 'AED Escrow', '6', '0', '1', '5000000.00', '0.00', 'active', NULL, NULL,                                                   '2019-01-20 13:06:14', '2019-01-20 13:06:14',   'wallet_api', 'wallet_api',  'liability');


INSERT INTO `description_types` (`id`, `type`, `created_at`, `created_by`,  `updated_at`, `updated_by`) VALUES
('1', 'application_stages', '2019-01-01 00:00:00', 'backoffice user', null, null),
('2', 'channels', '2019-01-01 00:00:00', 'backoffice user', null, null),
('3', 'companies', '2019-01-01 00:00:00', 'backoffice user', null, null),
('4', 'customer_types', '2019-01-01 00:00:00', 'backoffice user', null, null),
('5', 'customer_tiers', '2019-01-01 00:00:00', 'backoffice user', null, null),
('6', 'customer_subscriptions', '2019-01-01 00:00:00', 'backoffice user', null, null),
('7', 'application_document_types', '2019-01-01 00:00:00', 'backoffice user', null, null),
('8', 'image_types', '2019-01-01 00:00:00', 'backoffice user', null, null),
('9', 'individual_user_types', '2019-01-01 00:00:00', 'backoffice user', null, null),
('10', 'instruments', '2019-01-01 00:00:00', 'backoffice user', null, null),
('11', 'transaction_types', '2019-01-01 00:00:00', 'backoffice user', null, null),
('12', 'limit_profile_types', '2019-01-01 00:00:00', 'backoffice user', null, null),
('13', 'intervals', '2019-01-01 00:00:00', 'backoffice user', null, null),
('15', 'fee_profile_types', '2019-01-01 00:00:00', 'backoffice user', null, null),
('16', 'fee_calculation_methods', '2019-01-01 00:00:00', 'backoffice user', null, null),
('17', 'platforms', '2019-01-01 00:00:00', 'backoffice user', null, null),
('18', 'locales', '2019-01-01 00:00:00', 'backoffice user', null, null),
('19', 'communication_channels', '2019-01-01 00:00:00', 'backoffice user', null, null),
('20', 'fee_methods', '2019-01-01 00:00:00', 'backoffice user', null, null),
('21', 'business_types', '2019-01-01 00:00:00', 'backoffice user', null, null),
('22', 'user_tiers', '2019-01-01 00:00:00', 'backoffice user', null, null),
('23', 'business_user_tiers', '2019-01-01 00:00:00', 'backoffice user', null, null);



INSERT INTO `descriptions` (`id`, `name`, `description`, `value_id`) VALUES
('1', 'currency_exchange', 'Currency exchange', '11'),
('2', 'international_remittance', 'International remittance', '11'),
('3', 'transaction_based', 'Transaction based', '12'),
('4', 'balance_based', 'Balance based', '12'),
('5', 'mobile_application', 'Mobile Application', '2'),
('6', 'atm', 'ATM', '2'),
('7', 'kiosk', 'KIOSK', '2'),
('8', 'eft', 'EFT', '2'),
('9', 'standard', 'Standard (Basic)', '6'),
('10', 'gold', 'Gold', '6'),
('11', 'platinum', 'Platinum (Full)', '6'),
('12', 'tier_1', 'Tier 1 (Basic)', '5'),
('13', 'tier_2', 'Tier 2 (Standard)', '5'),
('14', 'tier_3', 'Tier 3 (Extended)', '5'),
('15', 'debit_card', 'Debit Card', '10'),
('16', 'credit_card', 'Credit Card', '10'),
('17', 'daily', 'Daily', '13'),
('19', 'monthly', 'Monthly', '13'),
('20', 'yearly', 'Yearly', '13'),
('21', 'individual', 'Individual User', '4'),
('22', 'business', 'Business User', '4'),
('23', 'transaction_based', 'Transaction Based', '15'),
('24', 'subscription_based', 'Subscription Based', '15'),
('25', 'new', 'New Stage', '14'),
('26', 'document_uploaded', 'Document Uploaded', '1'),
('27', 'document_reviewed', 'Document Reviewed', '1'),
('28', 'selfie_uploaded', 'Selfie Uploaded', '1'),
('29', 'liveness_check_passed', 'Liveness Check Passed', '1'),
('30', 'scored', 'Scored', '1'),
('31', 'flat_fee', 'Flat Fee', '16'),
('32', 'flat_percentage', 'Flat Percentage', '16'),
('33', 'staircase_flat_fee', 'Staircase with Flat Fees', '16'),
('34', 'staircase_flat_percentage', 'Staircase with Flat Percentages', '16'),
('35', 'PDF', '', 8),
('36', 'JPG', '', 8),
('37', 'web', 'web', 17),
('38', 'ios', 'ios', 17),
('39', 'android', 'android', 17),
('40', 'en-US', 'English US', 18),
('41', 'en', 'English', 18),
('42', 'fil-PH', 'Filipino PH', 18),
('43', 'es', 'Spanish', 18),
('44', 'de', 'German', 18),
('45', 'email', 'Email', 19),
('46', 'push', 'Push', 19),
('47', 'sms', 'SMS', 19),
('48', 'add', 'Add', 20),
('49', 'deduct', 'Deduct', 20),
('50', 'merchant', 'Merchant', 21),
('51', 'merchant_payment', 'Merchant Payment', 11),
('52', 'standard', 'Standard Tier', 22),
('53', 'basic', 'Basic', '5'),
('54', 'small', 'Small', 23),
('55', 'medium', 'Medium', 23),
('56', 'big', 'Big', 23);

INSERT INTO user_applications(id, uuid, user_id, status, stage, rejection_reason, created_by, created_at,fullname_original, fullname_updated, updated_by, updated_at) VALUES
(1, '852ec331-1b95-4402-97bf-6d2a736da7e8', 1, 'pending', 'new', null, 'pegbuser', '2019-01-01 00:00:00','Test','Test', 'pegbuser', '2019-01-01 00:00:00'),
(2, 'cf057438-17c6-49ad-b0ba-8593c8951364', 2, 'approved', 'ocr', null, 'ujali', '2019-01-30 00:00:00', 'Test','Test','ujali', '2019-01-30 00:00:00'),
(3, '1482c1a0-7e70-4a0f-adef-a07caa3b3acb', 3, 'rejected', 'new', 'test', 'david', '2019-01-30 00:00:00', 'Test','Test','david', '2019-01-30 00:00:00'),
(4, 'ac0718ad-3959-4327-a720-a47b590a2066', 4, 'approved', 'ocr', null, 'ujali', '2019-01-30 00:00:00', 'Test','Test','ujali', '2019-01-30 00:00:00'),
(5, 'c0269418-3dd9-41fe-a756-d4b8ee6ff1c0', 6, 'rejected', 'new', 'test', 'david', '2019-01-30 00:00:00', 'Test','Test','david', '2019-01-30 00:00:00');

INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at, primary_account_previous_balance, secondary_account_previous_balance)VALUES
(1549449579, 1, 8, 2, 'debit', 'p2p_domestic', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '2018-12-25 00:00:00', '2018-12-27 00:00:00', '1000', '500'),
(1549449579, 2, 2, 8, 'credit', 'p2p_domestic', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '2018-12-25 00:00:00','2018-12-27 00:00:00', '1000', '500'),
(1549449579, 3, 8, 7, 'debit', 'fee', 1.00, 1, 'IOS_APP', 'fee', 'success', '2018-12-25 00:00:00', '2018-12-27 00:00:00', '1000', '500'),
(1549446333, 1, 8, 3, 'debit', 'merchant_payment', 500.00, 1, 'IOS_APP', 'some explanation', 'success', '2018-12-26 03:07:30','2018-12-27 00:00:00', '1000', '500'),
(1549446333, 2, 3, 8, 'credit', 'merchant_payment', 500.00, 1, 'IOS_APP', 'some explanation', 'success', '2018-12-26 03:07:30','2018-12-27 00:00:00', '1000', '500'),
(1549446333, 3, 8, 7, 'debit', 'fee', 1.00, 1, 'IOS_APP', 'fee', 'success', '2018-12-26 03:07:30','2018-12-27 00:00:00', '1000', '500'),
(1549446999, 1, 3, 8, 'debit', 'p2p_domestic', 200.00, 1, 'ANDROID_APP', 'some explanation', 'success', '2018-12-25 14:27:30','2018-12-27 00:00:00', '1000', '500'),
(1549446999, 2, 8, 3, 'credit', 'p2p_domestic', 200.00, 1, 'ANDROID_APP', 'some explanation', 'success', '2018-12-25 14:27:30','2018-12-27 00:00:00', '1000', '500');

INSERT INTO currency_exchange_providers(id, user_id, name, is_active, pg_institution_id, created_at, updated_at, created_by, updated_by) VALUES
(1, 14, 'Currency Cloud', 1, 0, '2019-01-01 00:00:00', '2019-01-01 00:00:00', 'pegbuser', 'pegbuser'),
(2, 14, 'Ebury', 1, 0,          '2019-01-01 00:00:00', '2019-01-01 00:00:00', 'pegbuser', 'pegbuser');

INSERT INTO providers
(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
utility_payment_type, utility_min_payment_amount, utility_max_payment_amount,
is_active, created_by, updated_by, created_at, updated_at)
VALUES
('1', '1', null, 'Mashreq', 'txn type 1', 'icon 1', 'label 1', '1', null, '0.1', '1000', '1', 'core', 'core', now(), now()),
('2', '2', null, 'Emirates NBD', 'txn type 2', 'icon 2', 'label 2', '1', null, '0.1', '1000', '1', 'core', 'core', now(), now()),
('3', '14', null, 'Currency Cloud', 'txn type 3', 'icon 3', 'label 3', '1', null, '0.1', '1000', '1', 'pegbuser', 'pegbuser', '2019-01-01 00:00:00', '2019-01-01 00:00:00'),
('4', '14', null, 'Ebury', 'txn type 4', 'icon 4', 'label 4', '1', null, '0.1', '1000', '1', 'pegbuser', 'pegbuser', '2019-01-01 00:00:00', '2019-01-01 00:00:00');


INSERT INTO currency_rates(id, uuid, currency_id, base_currency_id, rate, provider_id, status, updated_by, updated_at) VALUES
(1, 'bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4', 3, 1, 99.9800, 3, 'active', 'pegbuser', '2019-02-25 00:00:00'),
(2, 'b566429b-e166-4cf6-83cb-7d8cf5ec9f09', 4, 1, 112.1020, 3, 'active',  'pegbuser', '2019-02-25 00:00:00'),
(3, '3a01ea86-de7b-414d-8f8a-757f101ccd13', 5, 1, 75.9501, 4, 'active',  'pegbuser', '2019-02-25 00:00:00' ),
(4, '51cd74ee-3a94-45b4-aac2-85856aaffd3f', 6, 1, 152.2014, 4, 'inactive',  'pegbuser', '2019-02-25 00:00:00'),
(5, 'bb01ea86-de7b-414d-8f8a-757f101ccd13', 1, 3, 0.010152, 3, 'active',  'pegbuser', '2019-02-25 00:00:00' ),
(6, 'bbcd74ee-3a94-45b4-aac2-85856aaffd3f', 1, 4, 0.009062, 3, 'active',  'pegbuser', '2019-02-25 00:00:00');

INSERT INTO currency_spreads(id, uuid, currency_rate_id, transaction_type, channel, institution, spread, deleted_at, created_by, created_at,            updated_by, updated_at) VALUES
(1, '747a1077-46ed-43a2-86b9-09817a751a44', 1, 'currency_exchange', null, null, 0.2, null,                           'pegbuser', '2019-02-28 00:00:00', 'pegbuser', '2019-02-28 00:00:00'),
(2, '898ea8be-2c19-4442-ad62-bd7b76a3b8bd', 1, 'currency_exchange', null, null, 0.25, '2019-02-20 00:00:00',         'pegbuser', '2019-02-15 00:00:00', 'pegbuser', '2019-02-20 00:00:00'),
(3, '6c5fffdd-6056-4d81-a348-34b486ce7e6a', 1, 'international_remittance', 'bank', null, 0.25, null,                 'pegbuser', '2019-02-16 00:00:00', 'pegbuser', '2019-02-16 00:00:00'),
(4, 'e5bb6aef-b2b3-4cbf-86ca-e3d858d6209b', 1, 'currency_exchange', null, null, 0.15, null,                          'pegbuser', '2019-01-30 00:00:00', 'pegbuser', '2019-01-30 00:00:00'),
(5, '938bc4b0-6d1e-4fd7-91bf-62e3205ae5b0', 2, 'international_remittance', 'mobile_money', 'mashreq', 0.05, null,    'pegbuser', '2019-02-26 00:00:00', 'pegbuser', '2019-02-26 00:00:00');


INSERT INTO limit_profiles
(id, limit_type, user_type, tier, subscription, transaction_type, channel, provider_id, instrument, max_interval_amount, max_amount,
min_amount, max_count, `interval`, created_at, updated_at, uuid, created_by, updated_by, currency_id)
VALUES
(1, 'balance_based', 'individual_user', 'tier 1', 'standard', 'top-up', 'atm', '1', 'debit_card', null, 10000,
 null, 50000, 'daily', '2019-02-20 00:00:00', '2019-03-31 13:09:39', '33cd967d-5fbe-4af3-9a90-3d37488dc4b5', 'pegbuser', 'pegbuser', 1),
(2, 'balance_based', 'business_user', 'tier 1', 'gold', 'p2p_domestic', 'mobile_application', null, 'debit_card', null, 5000,
 null, 50000, 'daily', '2019-02-20 00:00:00', '2019-02-20 00:00:00', 'c1cdb0af-4940-4fe0-b3f5-320a508c7f8b', 'pegbuser', 'pegbuser',  1),
(3, 'transaction_based', 'individual_user', 'tier 2', 'platinum', 'top-up', 'atm', '1', 'debit_card', 6000, 50000,
 3000, 50000, 'monthly', '2019-02-20 00:00:00', '2019-02-20 00:00:00', 'b03b9a49-135d-4e1d-9c4c-1224667b7edc', 'pegbuser', 'pegbuser', 3),
(4, 'transaction_based', 'business_user', 'tier 1', 'standard', 'withdrawal', 'atm', '1', 'debit_card', null, 100000,
 null, 50000, 'daily', '2019-02-20 00:00:00', '2019-02-20 00:00:00', '4a421917-60fe-44fb-aea2-0d4269796b3f', 'pegbuser', 'pegbuser', 7);

INSERT INTO limit_profiles
(id, limit_type, user_type, tier, subscription, transaction_type, channel, provider_id, instrument, max_interval_amount, max_amount,
min_amount, max_count, `interval`, created_at, updated_at, uuid, created_by, updated_by, currency_id, deleted_at)
VALUES
(5, 'transaction_based', 'business_user', 'tier 1', 'standard', 'withdrawal', 'atm', '1', 'debit_card', null, 100000,
 null, 50000, 'daily', '2019-02-20 00:00:00', '2019-02-20 00:00:00', '5ac9649e-d7bf-4e23-9519-1ad4d41273d3', 'pegbuser', 'pegbuser', 1, '2019-02-28 00:10:10');


INSERT INTO fee_profiles
(id, uuid, fee_type, user_type, tier, subscription_type, transaction_type, channel, provider_id, instrument, calculation_method, max_fee, min_fee,
fee_amount, fee_ratio, fee_method, tax_included, created_at, created_by, updated_at, updated_by, currency_id) VALUES
(1, '78fcb677-d0a6-4074-aac2-c2c7e76e9b25', 'transaction_based', 'individual', 'basic', 'standard', 'p2p_domestic', 'mobile_application', null, 'visa_debit', 'flat_fee', null, null,
'20.00', null, 'add', 'tax_included', '2019-02-20 00:00:00', 'pegbuser', '2019-02-21 00:00:00', 'george', 1),
(2, 'acd39ec4-d76e-41d1-bc07-dca44403059d', 'subscription_based', 'individual', 'basic', 'platinum', 'p2p_international', 'atm', '1', 'visa_debit', 'flat_fee', null, null,
'30.00', null, 'deduct', 'no_tax', '2019-02-20 00:00:00', 'pegbuser', '2019-02-21 00:00:00', 'george', 1),
(3, '95447383-b2a6-4be4-b601-261d235dbb6b', 'transaction_based', 'business', 'small', 'gold', 'p2p_domestic', 'mobile_application', null, 'visa_debit', 'staircase_flat_percentage', '10.00', '5.00',
null, null, 'add', 'tax_not_included', '2019-02-20 00:00:00', 'pegbuser', '2019-02-21 00:00:00', 'george', 3),
(4, '03c5a03d-4397-4fea-9c3f-85ef71ce671c', 'transaction_based', 'individual', 'basic', 'standard', 'p2p_domestic', 'mobile_application', null, 'visa_debit', 'flat_fee', null, null,
'20.00', null, 'add', '1', '2019-02-20 00:00:00', 'pegbuser', '2019-02-21 00:00:00', 'george', 7);

INSERT INTO fee_profiles
(id, uuid, fee_type, user_type, tier, subscription_type, transaction_type, channel, provider_id, instrument, calculation_method, max_fee, min_fee,
fee_amount, fee_ratio, fee_method, tax_included, created_at, created_by, updated_at, updated_by, currency_id, deleted_at) VALUES
(5, 'fb1ac407-0eb5-4721-a0f5-b6b81b3faf71', 'transaction_based', 'individual', 'basic', 'standard', 'p2p_domestic', 'mobile_application', '1', 'visa_debit', 'flat_fee', null, null,
'20.00', null, 'add', 'tax_included', '2019-02-20 00:00:00', 'pegbuser', '2019-02-21 00:00:00', 'george', 1, '2019-02-28 00:10:10');

INSERT INTO fee_profile_ranges
(id, fee_profile_id, min, max, fee_amount, fee_ratio, created_at, updated_at)
VALUES
(1, 3, 0, 1000, null, 0.0005, '2019-02-20 00:00:00', '2019-02-21 00:00:00'),
(2, 3, 1001, 5000, null, 0.0002, '2019-02-20 00:00:00', '2019-02-21 00:00:00'),
(3, 3, 5001, 10000, null, 0.0001, '2019-02-20 00:00:00', '2019-02-21 00:00:00');

INSERT INTO i18n_strings
(id, `key`, `text`, platform, locale, explanation, created_at, updated_at)
VALUES
(1, 'close', 'close', 'web', 'en-US', null, '2019-02-20 00:00:00', '2019-02-20 00:00:00'),
(2, 'close', 'close', 'ios', 'en-US', null, '2019-02-20 00:00:00', '2019-02-20 00:00:00'),
(3, 'close', 'close', 'android', 'en-US', null, '2019-02-20 00:00:00', '2019-02-20 00:00:00'),
(4, 'how_are_you', 'how are you?', 'web', 'en-US', null, '2019-02-20 00:00:00', '2019-02-20 00:00:00'),
(5, 'how_are_you', 'how are you?', 'ios', 'en-US', null, '2019-02-20 00:00:00', '2019-02-20 00:00:00'),
(6, 'how_are_you', 'how are you?', 'android', 'en-US', null, '2019-02-20 00:00:00', '2019-02-20 00:00:00'),
(7, 'close', 'isara', 'web', 'fil-PH', null, '2019-02-20 00:00:00', '2019-02-20 00:00:00'),
(8, 'how_are_you', 'kumusta?', 'web', 'fil-PH', 'tagalog how are you', '2019-02-20 00:00:00', '2019-02-20 00:00:00');

INSERT INTO notification_templates
(id, uuid, `name`, title_resource, default_title, content_resource, default_content, channels, description, created_at, created_by, updated_at, updated_by, is_active)
VALUES
(1, 'b3f3ff13-0613-4d8c-83aa-463414ae3ead', 'template_1', 'template_1_title', 'default title', 'template_1_content', 'this is the default content', '[sms,email]', '', '2019-02-20 00:00:00', 'pegbuser', '2019-02-20 00:00:00', null, 1),
(2, '32d755b2-b332-4850-bb72-406fa5eb95e5', 'template_2', 'template_2_title', 'default title 2', 'template_2_content', 'this is the default content 2', '[sms,push]', '', '2019-02-21 00:00:00', 'pegbuser', '2019-02-20 00:00:00', null, 1),
(3, 'c43dd627-8521-4bcf-aebc-c4c148f2087c', 'template_3', 'template_3_title', 'default title 3', 'template_3_content', 'this is the default content 3', '[push]', '', '2019-02-21 14:30:00', 'pegbuser', '2019-02-20 00:00:00', null, 0),
-- ddc893bc-804c-4992-9674-c77fc0462886
(4, 'ddc893bc-804c-4992-9674-c77fc0462886', 'template_4', 'template_4_title', 'default title 4', 'template_4_content', 'this is the default content 4', '["new_chann1", "new_chann2"]', '', '2019-02-20 00:00:00', 'pegbuser', '2019-02-20 00:00:00', null, 0),
(5, '2d3df534-ae41-41cb-b2af-e045318894f7', 'template_5', 'template_5_title', 'default title 5', 'template_5_content', 'this is the default content 5', '"[\"new_chann1\", \"new_chann2\"]"', '', '2019-02-20 00:00:00', 'pegbuser', '2019-02-20 00:00:00', null, 0);


INSERT INTO system_settings
(id, `key`,                  value,    type,  for_android, for_ios, for_backoffice, explanation, created_at,            updated_at,            created_by, updated_by)
VALUES
(1, 'float_account_numbers', '[4.1]', 'json', 1,           1,       1,              null,       '2019-02-20 00:00:00', '2019-02-20 00:00:00', 'pegbuser',  'pegbuser'),
(2, 'float_user_balance_percentage_institution_map', '[]', 'json', 1,           1,       1,              null,       '2019-02-20 00:00:00', '2019-02-20 00:00:00', 'pegbuser',  'pegbuser'),
(3, 'default_currency', 'AED', 'string', 1,           1,       1,              null,       '2019-02-20 00:00:00', '2019-02-20 00:00:00', 'pegbuser',  'pegbuser');


INSERT INTO tasks
(id, uuid, module, action, verb, url, headers, body, status, maker_level, maker_business_unit, created_at, created_by, checked_at, checked_by, updated_at)
VALUES
(1, '06d18f41-1abf-4507-afab-5f8e1c7a1601', 'strings', 'create i18n string', 'POST', '$backoffice_api_host/api/strings', '\{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"\}', '\{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"\}', 'pending', '3', 'BackOffice', '2019-01-01 00:10:30', 'pegbuser', null, null, '2019-01-01 00:10:30'),
(2, 'e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8', 'strings', 'create i18n string', 'POST', '$backoffice_api_host/api/strings', '\{"X-UserName":"pegbuser","X-RoleLevel":"2","content-type":"application/json","X-BusinessUnit":"BackOffice"\}', '\{"text":"hola","locale":"es","explanation":"text for hello world hahah","key":"hello","platform":"web"\}', 'pending', '2', 'BackOffice', '2019-01-02 00:10:30', 'pegbuser', null, null, '2019-01-02 00:10:30'),
(3, 'ecb907ae-ffaa-45da-abd2-3907fced637f', 'spreads', 'create currency rate spreads', 'POST', '$backoffice_api_host/api/currency_exchanges/fff907ae-ffaa-45da-abd2-3907fced637f/spreads', '\{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"\}', '\{"institution":"Mashreq","channel":"atm","transaction_type":"currency_exchange","spread":0.01\}', 'pending', '3', 'BackOffice', '2019-01-02 05:10:30', 'pegbuser', null, null, '2019-01-02 05:10:30'),
(4, '54403083-e0f1-4e80-bdd6-cfdaefd0646e', 'spreads', 'create currency rate spreads', 'POST', '$backoffice_api_host/api/currency_exchanges/aaa907ae-ffaa-45da-abd2-3907fced637f/spreads', '\{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"\}', '\{"institution":"NBD","channel":"debit_card","transaction_type":"currency_exchange","spread":0.05\}', 'approved', '3', 'BackOffice', '2019-01-02 11:10:30', 'pegbuser', '2019-01-05 15:20:30', 'george', '2019-01-05 15:20:30'),
(5, '2fb15dd8-97b4-4c19-9886-6b5912ccb4d8', 'strings', 'update i18n string', 'PUT', '$backoffice_api_host/api/strings/bb15dd8-97b4-4c19-9886-cfdaefd0646e', '\{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"BackOffice"\}', '\{"updated_at":"2019-07-02T13:10:25.751Z","text":"adios","locale":"es","explanation":"text for bye","key":"bye","platform":"web"\}', 'rejected', '3', 'BackOffice', '2019-01-03 15:50:30', 'pegbuser', '2019-01-05 15:50:30', 'george', '2019-01-05 15:50:30'),
(6, '882baf82-a5c6-47f3-9a9a-14191d14b918', 'strings', 'create i18n string', 'POST', '$backoffice_api_host/api/strings', '\{"X-UserName":"pegbuser","X-RoleLevel":"3","content-type":"application/json","X-BusinessUnit":"Marketing"\}', '\{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"\}', 'pending', '3', 'Marketing', '2019-01-01 00:15:30', 'pegbuser', null, null, '2019-01-01 00:15:30');
