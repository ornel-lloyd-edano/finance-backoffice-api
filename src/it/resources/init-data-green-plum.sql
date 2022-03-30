DELETE FROM pegb_wallet_dwh_it.currencies;
DELETE FROM pegb_wallet_dwh_it.providers;
DELETE FROM pegb_wallet_dwh_it.transactions;
DELETE FROM pegb_wallet_dwh_it.users;
DELETE FROM pegb_wallet_dwh_it.accounts;
DELETE FROM pegb_wallet_dwh_it.account_types;
DELETE FROM pegb_wallet_dwh_it.internal_recon_daily_summary;


INSERT INTO pegb_wallet_dwh_it.currencies (id,currency_name,description,created_at,created_by,updated_at,updated_by,is_active) VALUES
('1', 'KES', 'Kenyan Shillings', '2019-01-20 10:52:40', 'admin', '2019-01-20 10:52:40', 'admin', '1'),
('2', 'INR', 'Indian Rupee', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '1'),
('3', 'USD', 'US Dollar', '2019-01-20 10:52:40', 'admin', '2019-01-20 10:52:40', 'admin', '1'),
('4', 'EUR', 'Euro', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '1'),
('5', 'CNY', 'Chinese Yuan', '2019-01-20 10:52:40', 'admin', '2019-01-20 10:52:40', 'admin', '1'),
('6', 'CHF', 'Swiss Franc', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '1'),
('7', 'PHP', 'Ph Peso', '2019-01-21 11:31:53', 'admin', '2019-01-21 11:31:53', 'admin', '0');

INSERT INTO pegb_wallet_dwh_it.providers
(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
utility_payment_type, utility_min_payment_amount, utility_max_payment_amount,
is_active, created_by, updated_by, created_at, updated_at)
VALUES
('1', '45', null, 'SBM', 'txn type 1', 'icon 1', 'label 1', '1', null, '0.1', '1000', '1', 'core', 'core', now(), now());
--433   001-1  mpesa.2, 148  001-1  66.25,  520  002-1  343.2, 429  004-1   airtel.1
INSERT INTO pegb_wallet_dwh_it.transactions (unique_id, id, sequence, primary_account_id, primary_account_uuid, primary_account_number, secondary_account_id, secondary_account_uuid, secondary_account_number, receiver_phone, direction, type, amount, currency, exchange_rate, channel, provider_id, instrument, instrument_id, latitude, longitude, explanation, status, created_at, updated_at, cost_rate, previous_balance, gp_insert_timestamp, primary_account_type, primary_account_main_type, primary_account_user_id, primary_account_user_uuid, effective_rate, dashboard_revenue, currency_id) VALUES
(1549446112, '1549446111', 1, 148, '1971e279-cecd-44bc-8731-0945ce53f84e', '66.25', 148, '2e2a439c-fb85-431f-b2f5-5ba2bb5d2b6e', '66.25', null, 'debit', 'p2p_domestic', 1250.0000, 'KES', null, 'IOS_APP', '1', null, null, null, null, 'some explanation', 'success', '2019-06-17 00:00:00.000000', null, null, null, '2020-01-19 11:46:12.225843', 'standard wallet', 'liability', '251', 'b8a41f80-6db0-4af3-9e2e-ce77043653e0',  null, null, 1),
 (154944611264, '1549446334', 1, 427, '8d82d0bd-4955-49f5-be41-fc0f48f94a4a', 'pesalink.1', 427, '1971e279-cecd-44bc-8731-0945ce53f84e', 'pesalink.1', null, 'credit', 'bank_transfer', 500.0000, 'KES', null, 'IOS_APP', '1', null, null, null, null, 'some explanation', 'success', '2019-06-17 03:07:30.000000', null, null , null, '2020-01-19 11:46:12.575519', 'standard wallet', 'liability', '256', '9ef1e29a-8554-4640-8668-9933543dbe4d', null, null, 1),
(154944611276, '1549446995', 1, 429, 'db0efa3d-65b7-4293-b91d-7288ee0b1a4e', 'airtel.1', 429, '1971e279-cecd-44bc-8731-0945ce53f84e', 'airtel.1', null, 'debit', 'cashout', 200.0000, 'KES', null, 'ANDROID_APP', '1', null, null, null, null, 'some explanation', 'success', '2019-06-17 14:27:30.000000', null, null, null, '2020-01-19 11:46:12.646386', 'standard wallet', 'liability', '266', '19c3b53d-488d-4b97-9c30-6db8e9c5584e', null, null, 1),
(154944611278, '1549446996', 1, 433, '1971e279-cecd-44bc-8731-0945ce53f84e', 'mpesa.2', 433, 'db0efa3d-65b7-4293-b91d-7288ee0b1a4e', 'mpesa.2', null, 'credit', 'cashin', 200.0000, 'KES', null, 'ANDROID_APP', '1', null, null, null, null, 'some explanation', 'success', '2019-06-17 14:27:30.000000', null, null, null, '2020-01-19 11:46:12.720682', 'standard wallet', 'liability', '270', 'fc142f12-f06e-4064-9e36-755fbad1e42f', null, null, 1),
(154944611290, '1549446998', 1, 497, '1971e279-cecd-44bc-8731-0945ce53f84e', '342.1', 497, 'db0efa3d-65b7-4293-b91d-7288ee0b1a4e', '342.1', null, 'credit', 'bank_transfer', 200.0000, 'KES', null, 'ANDROID_APP', '1', null, null, null, null, 'some explanation', 'success', '2019-06-15 14:27:30.000000', null, null, null, '2020-01-19 11:46:12.907124', 'standard wallet', 'liability', '274', '8219e212-d8fb-48fa-b39a-4f728fb65d3b', null, null, 1),
(1549446123, '1549446112', 1, 520, '2e2a439c-fb85-431f-b2f5-5ba2bb5d2b6e', '343.2', 520, '1971e279-cecd-44bc-8731-0945ce53f84e', '343.2', null, 'credit', 'cashout', 1250.0000, 'KES', null, 'IOS_APP', '1', null, null, null, null, 'some explanation', 'success', '2019-06-17 00:00:00.000000', null, null, null, '2020-01-19 11:46:12.385539', 'standard wallet', 'liability', '294', '4b3aa425-5af8-4161-8584-527f7413e734', null, null, 1),
(154944611265, '1549446333', 1, 148, '1971e279-cecd-44bc-8731-0945ce53f84e', '66.25', 148, '8d82d0bd-4955-49f5-be41-fc0f48f94a4a', '66.25', null, 'debit', 'cashin', 500.0000, 'KES', null, 'IOS_APP', '1', null, null, null, null, 'some explanation', 'success', '2019-06-17 03:07:30.000000', null, null, null, '2020-01-19 11:46:12.476576', 'standard wallet', 'liability', '251', 'b8a41f80-6db0-4af3-9e2e-ce77043653e0', null, null, 1),
(154944611279, '1549446997', 1, 427, 'db0efa3d-65b7-4293-b91d-7288ee0b1a4e', 'pesalink.1', 427, '1971e279-cecd-44bc-8731-0945ce53f84e', 'pesalink.1', null, 'debit', 'p2p_domestic', 200.0000, 'KES', null, 'ANDROID_APP', '1', null, null, null, null, 'some explanation', 'success', '2019-06-15 14:27:30.000000', null, null, null, '2020-01-19 11:46:12.822208', 'standard wallet', 'liability', '256', '9ef1e29a-8554-4640-8668-9933543dbe4d', null, null, 1);

INSERT INTO pegb_wallet_dwh_it.accounts (id, uuid, number, "name", account_type, account_type_id, is_main_account, user_id, user_uuid, currency, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, updated_at, updated_by, created_by, main_type, gp_insert_timestamp) VALUES
(6, '057715a3-fbdf-11e9-a5ee-fa163ed3b165', 'pegb_fees.4', '1_fee_collection_USD', '7', 7, false, 1, '2205409c-1c83-11e9-a2a9-000c297e3e45', '4', 4, 10000.0700, 0.0000, 'active', null, null, '2019-10-31 13:04:49.000000', '2020-01-14 20:22:56.000000', '1', 'wallet_api', 'liability', '2020-01-15 06:28:32.871622'),
(148, '2038a52b-49bf-48c6-8510-4f93c1185a38', '66.25', '66.25_utility', '4', 4, false, 251, '76775a33-3fe3-4edf-8263-6ae2cc879bee', '11', 11, 52530.3600, 0.0000, 'active', null, '2020-01-14 10:57:45.000000', '2019-03-26 12:31:59.000000', '2020-01-14 10:57:45.000000', 'core', 'core', 'asset', '2020-01-15 06:28:32.871622'),
(427, '5080b8e5-c709-11e9-973e-000c297e3e45', 'pesalink.1', 'pesalink_distribution', '5', 5, true, 256, '261b023b-c709-11e9-973e-000c297e3e45', '1', 1, 103720.0000, 0.0000, 'active', null, '2020-01-05 08:36:42.000000', '2019-08-25 08:24:03.000000', '2020-01-05 08:36:42.000000', 'core', 'core', 'liability', '2020-01-15 06:28:32.871622'),
(429, '527b7852-c70a-11e9-973e-000c297e3e45', 'airtel.1', 'airtel_distribution', '5', 5, true, 266, '4e69026c-c70a-11e9-973e-000c297e3e45', '1', 1, 101513.0000, 0.0000, 'active', null, '2020-01-14 09:06:46.000000', '2019-08-25 08:31:15.000000', '2020-01-14 09:06:46.000000', 'core', 'core', 'liability', '2020-01-15 06:28:32.871622'),
(433, '58868a55-c97f-11e9-973e-000c297e3e45', 'mpesa.2', 'mpesa_collection', '4', 4, false, 270, 'a481c5e3-c709-11e9-973e-000c297e3e45', '1', 1, 654476.9500, 0.0000, 'active', null, '2020-01-14 20:01:20.000000', '2019-08-28 11:33:59.000000', '2020-01-14 20:01:20.000000', 'core', 'core', 'liability', '2020-01-15 06:28:32.871622'),
(497, '789f5dff-0436-418d-936e-12f42d40ad5c', '342.1', 'pesalink_int_remittance_liability', '2', 2, false, 274, 'e90ee532-d55c-11e9-973e-000c297e3e45', '1', 1, 496146.2500, 0.0000, 'active', null, '2020-01-14 20:28:51.000000', '2019-06-10 11:30:57.000000', '2020-01-14 20:28:51.000000', 'wallet_api', 'wallet_api', 'liability', '2020-01-15 06:28:32.871622'),
(520, '789f5dff-0436-418d-936e-12f42d40addf', '343.2', 'mpesa int_remittance_asset', '2', 2, false, 294, '0577f952-d55d-11e9-973e-000c297e3e45', '1', 1, 484370.7200, 0.0000, 'active', null, '2020-01-14 15:10:04.000000', '2019-06-10 11:30:57.000000', '2020-01-14 15:10:04.000000', 'wallet_api', 'wallet_api', 'asset', '2020-01-15 06:28:32.871622'),
(522, '789f5dff-0436-418d-936e-12f42d40adda', '344.2', 'airtel_int_remittance_asset', '2', 2, false, 298, '05a443a0-d55d-11e9-973e-000c297e3e45', '1', 1, 484513.0800, 0.0000, 'active', null, '2020-01-14 20:22:56.000000', '2019-06-10 11:30:57.000000', '2020-01-14 20:22:56.000000', 'wallet_api', 'wallet_api', 'asset', '2020-01-15 06:28:32.871622'),
(3, '057713ee-fbdf-11e9-a5ee-fa163ed3b165', 'pegb_fees', '1_fee_collection_KES', '7', 7, false, 1, '2205409c-1c83-11e9-a2a9-000c297e3e45', '1', 1, 11450.6000, 0.0000, 'active', null, null, '2019-10-31 13:04:49.000000', '2020-01-14 20:28:51.000000', '1', 'wallet_api', 'liability', '2020-01-15 06:28:32.871622'),
(71, '6bf89c2f-96a6-4469-b663-eb0e2fb62125', '66.1', '66_utility', '5', 5, true, 66, '76775a33-3fe3-4edf-8263-6ae2cc879bee', '1', 1, 1194036.9300, 0.0000, 'active', null, '2020-01-14 20:22:56.000000', '2019-02-18 13:08:05.000000', '2020-01-14 20:22:56.000000', 'wallet_api', 'wallet_api', 'liability', '2020-01-15 06:28:32.871622'),
(105, '2038a52b-49bf-48c6-8510-4f93c1185a04', '66.4', '66.4_utility', '5', 5, false, 66, '76775a33-3fe3-4edf-8263-6ae2cc879bee', '4', 4, 52357.5000, 0.0000, 'active', null, '2020-01-14 20:22:56.000000', '2019-03-11 12:30:26.000000', '2020-01-14 20:22:56.000000', 'core', 'core', 'liability', '2020-01-15 06:28:32.871622'),
(129, '2038a52b-49bf-48c6-8510-4f93c1185a77', '66.14', '66.14_utility', '4', 4, false, 66, '76775a33-3fe3-4edf-8263-6ae2cc879bee', '1', 1, 156449.3700, 0.0000, 'active', null, '2020-01-14 20:22:56.000000', '2019-03-26 12:31:59.000000', '2020-01-14 20:22:56.000000', 'core', 'core', 'asset', '2020-01-15 06:28:32.871622'),
(133, '2038a52b-49bf-48c6-8510-4f93c1185a79', '66.17', '66.17_utility', '4', 4, false, 66, '76775a33-3fe3-4edf-8263-6ae2cc879bee', '4', 4, 57909.1400, 0.0000, 'active', null, '2020-01-14 12:37:59.000000', '2019-03-26 12:31:59.000000', '2020-01-14 12:37:59.000000', 'core', 'core', 'asset', '2020-01-15 06:28:32.871622'),
(462, '1ec0aed0-cd71-11e9-973e-000c297e3e33', 'cybersource.2', 'cybersource_collection_usd', '4', 4, false, 322, 'f16317f9-cd70-11e9-973e-000c297e3e45', '4', 4, 98359.7000, 0.0000, 'active', null, '2020-01-14 10:32:15.000000', '2019-09-02 12:02:14.000000', '2020-01-14 10:32:15.000000', 'core', 'core', 'liability', '2020-01-15 06:28:32.871622'),
(498, '789f5dff-0436-418d-936e-12f42d40ad43', '342.2', 'pesalink_int_remittance_asset', '2', 2, false, 342, 'e90ee532-d55c-11e9-973e-000c297e3e45', '1', 1, 495222.2800, 0.0000, 'active', null, '2020-01-14 20:28:51.000000', '2019-06-10 11:30:57.000000', '2020-01-14 20:28:51.000000', 'wallet_api', 'wallet_api', 'asset', '2020-01-15 06:28:32.871622'),
(521, '789f5dff-0436-418d-936e-12f42d40aff', '343.1', 'mpesa_int_remittance_liability', '2', 2, false, 343, '0577f952-d55d-11e9-973e-000c297e3e45', '1', 1, 485450.3700, 0.0000, 'active', null, '2020-01-14 15:10:04.000000', '2019-06-10 11:30:57.000000', '2020-01-14 15:10:04.000000', 'wallet_api', 'wallet_api', 'liability', '2020-01-15 06:28:32.871622'),
(523, '789f5dff-0436-418d-936e-12f42d40afa', '344.1', 'airtel_int_remittance_liability', '2', 2, false, 344, '05a443a0-d55d-11e9-973e-000c297e3e45', '1', 1, 485174.3500, 0.0000, 'active', null, '2020-01-14 20:22:56.000000', '2019-06-10 11:30:57.000000', '2020-01-14 20:22:56.000000', 'wallet_api', 'wallet_api', 'liability', '2020-01-15 06:28:32.871622');

--251  acc_id 148,  266  acc_id 429,  270   acc_id 433,  294   acc_id 520
INSERT INTO pegb_wallet_dwh_it.users (id, uuid, username, password, type, tier, segment, subscription, email, status, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by, fullname, name) VALUES
(45, '15c27deb-4fec-4cfa-8a53-f9d2aa49c5c4', '+639985647529', '5A697C6768FCB4F363874B4D73C517A6E7F8932D23C31B6EA52BEBD2C3F4AA05', 'individual', 'basic', 'new', 'standard', null, 'waiting_for_activation', null, null, '2019-02-04 10:29:24.000000', 'wallet_api', '2019-02-04 10:29:24.000000', 'wallet_api', 'missing', null),
(251, 'b8a41f80-6db0-4af3-9e2e-ce77043653e0', '+971521111111', '3C504B0EF35FA7704D026B74B058F2FB846CF5AB1F4396F1E0956500D8AFF4B3', 'provider', 'basic', 'new', 'standard', null, 'waiting_for_activation', null, null, '2019-05-30 13:08:45.000000', 'wallet_api', '2019-05-30 13:08:45.000000', 'wallet_api', 'missing', null),
 (256, '9ef1e29a-8554-4640-8668-9933543dbe4d', '+971558563214', '3C504B0EF35FA7704D026B74B058F2FB846CF5AB1F4396F1E0956500D8AFF4B3', 'provider', 'basic', 'new', 'standard', null, 'waiting_for_activation', null, null, '2019-06-10 11:30:57.000000', 'wallet_api', '2019-06-10 11:30:57.000000', 'wallet_api', 'missing', null),
(266, '19c3b53d-488d-4b97-9c30-6db8e9c5584e', '+971551111111', '3C504B0EF35FA7704D026B74B058F2FB846CF5AB1F4396F1E0956500D8AFF4B3', 'provider', 'basic', 'new', 'standard', null, 'waiting_for_activation', null, null, '2019-06-12 12:39:04.000000', 'wallet_api', '2019-06-12 12:39:04.000000', 'wallet_api', 'missing', null),
 (270, 'fc142f12-f06e-4064-9e36-755fbad1e42f', '+9715000011111', '7BCCB038346B28CF2331986ED523500D6CC7184680FDE4A21C92437867CEC0CF', 'provider', 'basic', 'new', 'standard', null, 'waiting_for_activation', null, null, '2019-06-13 09:35:42.000000', 'wallet_api', '2019-06-13 09:35:42.000000', 'wallet_api', 'missing', null),
(274, '8219e212-d8fb-48fa-b39a-4f728fb65d3b', '+9715555555555', '7BCCB038346B28CF2331986ED523500D6CC7184680FDE4A21C92437867CEC0CF', 'provider', 'basic', 'new', 'standard', null, 'active', '2019-06-18 11:04:23.000000', null, '2019-06-13 13:07:53.000000', 'wallet_api', '2019-06-18 11:04:23.000000', 'wallet_api', 'missing', null),
(294, '4b3aa425-5af8-4161-8584-527f7413e734', '+971981234456', '7BCCB038346B28CF2331986ED523500D6CC7184680FDE4A21C92437867CEC0CF', 'provider', 'basic', 'new', 'standard', null, 'waiting_for_activation', null, null, '2019-08-19 16:20:58.000000', 'wallet_api', '2019-08-19 16:20:58.000000', 'wallet_api', 'missing', null),
(298, 'fa9b0e13-de79-4625-b71a-9d1bf76d6033', '+9719812321424', '7BCCB038346B28CF2331986ED523500D6CC7184680FDE4A21C92437867CEC0CF', 'provider', 'basic', 'new', 'standard', null, 'waiting_for_activation', null, null, '2019-08-22 10:24:16.000000', 'wallet_api', '2019-08-22 10:24:16.000000', 'wallet_api', 'missing', null),
(302, '4e69026c-c70a-11e9-973e-000c297e3e45', 'airtel', 'password', 'individual', 'basic', 'new', 'standard', null, 'active', '2019-08-25 08:31:09.000000', '2019-08-25 08:31:09.000000', '2019-08-25 08:31:09.000000', 'core', '2019-08-25 08:31:09.000000', 'core', 'missing', null);

INSERT INTO pegb_wallet_dwh_it.account_types (id, account_type_name, description, created_at, created_by, updated_at, updated_by, is_active) VALUES
(3, 'standard_saving', null, '2019-01-01 00:00:00', 'SYSTEM', '2019-01-01 00:00:00', null, 1),
(6, 'utility', null, '2019-01-01 00:00:00', 'SYSTEM', '2019-01-01 00:00:00', null, 1),
(5, 'distribution', null, '2019-01-01 00:00:00', 'SYSTEM', '2019-01-01 00:00:00', null, 1),
(4, 'collection', null, '2019-01-01 00:00:00', 'SYSTEM', '2019-01-01 00:00:00', null, 1),
(2, 'std_dwh_it', null, '2019-01-01 00:00:00', 'SYSTEM', '2019-01-01 00:00:00', null, 1),
(7, 'fee_collection', null, '2019-01-01 00:00:00', 'SYSTEM', '2019-01-01 00:00:00', null, 1),
(1, 'standard', 'abcd1', '2019-01-01 00:00:00', 'SYSTEM', '2019-01-01 00:00:00', null, 1);

INSERT INTO pegb_wallet_dwh_it.internal_recon_daily_summary(
		id, recon_date, account_id, account_number,
		account_type, main_account_type,user_id,
		user_uuid, currency, end_of_day_balance,
		value_change, transaction_total_amount,
		transaction_total_count, problematic_transaction_count,
		status, created_at, comments, updated_at,
		updated_by
		)
VALUES
			 
			 (
					 '123', '2019-05-15', 'ujali account',
					 'some account num', 'distribution',
					 'liability',1, '2205409c-1c83-11e9-a2a9-000c297e3e45',
					 'KES', '3200.50', '300', '9999.00',
					 '100', '1', 'FAIL', now(), null,
					 null, null
					 ),
			 (
					 '124', '2019-05-20', 'loyd account',
					 'some account num', 'distribution',
					 'liability',1, 'efe3b069-476e-4e36-8d22-53176438f55f',
					 'KES', '1600.50', '400', '1000.00',
					 '30', '1', 'FAIL', now(), null,
					 null, null
					 ),
			 (
					 '8a77e520-d427-4205-a397-3a5e95ed5b5f', '2019-06-16', '5',
					 '1234-jack-4321', 'distribution',
					 'liability',1, 'efe3b069-476e-4e36-8d22-53176438f55f',
					 'KES', '1000.00', '1000.00', '1000.00',
					 '30', '0', 'SUCCESS', now(), null,
					 null, null
					 ),
			 (
					 '8a77e520-d427-4205-a397-3a5e95ed5b56', '2019-06-16', '6',
					 '1234-jill-4321', 'distribution',
					 'asset',1, 'efe3b069-476e-4e36-8d22-53176438f55f',
					 'KES', '1000.00', '1000.00', '1000.00',
					 '30', '0', 'SUCCESS', now(), null,
					 null, null
					 );




