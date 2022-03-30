SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM `descriptions`; -- description_types
DELETE FROM `description_types`;
DELETE FROM `transactions`; -- currencies, accounts
DELETE FROM `emis`; -- saving goals
DELETE FROM `individual_users`; -- users
DELETE FROM `vouchers`; -- currencies
DELETE FROM `user_limit_activities`; -- users
DELETE FROM `application_documents`; -- users, user_applications
DELETE FROM `user_applications`; -- users
DELETE FROM `saving_goals`; -- users, currencies, accounts
DELETE FROM `currency_spreads`; -- currency_rates
DELETE FROM `currency_rates`; -- currency_exchange_providers, accounts
DELETE FROM `accounts`; -- users, currencies, account_types
DELETE FROM `currency_exchange_providers`; -- users
DELETE FROM `account_types`;
DELETE FROM `currencies`;
DELETE FROM `users`;
DELETE FROM `currency_exchange_providers`;
DELETE FROM `currency_rates`;
DELETE FROM `currency_spreads`;
DELETE FROM `limit_profile_history`;
DELETE FROM `limit_profiles`;
DELETE FROM `fee_profiles`;
DELETE FROM `fee_profile_ranges`;
DELETE FROM `i18n_strings`;
DELETE FROM `notification_templates`;
DELETE FROM `system_settings`;
DELETE FROM `tasks`;
DELETE FROM `permissions`;
DELETE FROM `back_office_users`;
DELETE FROM `business_units`;
DELETE FROM `roles`;
DELETE FROM `report_definitions`;
DELETE FROM `scopes`;
DELETE FROM `settlements`;
DELETE FROM `settlement_lines`;
DELETE FROM `countries`;
DELETE FROM `providers`;
DELETE FROM business_user_application_primary_addresses;
DELETE FROM business_user_application_primary_contacts;
DELETE FROM business_user_application_account_configs;
DELETE FROM business_user_application_external_accounts;
DELETE FROM business_user_applications;
DELETE FROM business_users;
DELETE FROM vp_users;
SET FOREIGN_KEY_CHECKS = 1;