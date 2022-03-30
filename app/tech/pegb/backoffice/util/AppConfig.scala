package tech.pegb.backoffice.util

import java.util.UUID

import com.google.inject._
import com.typesafe.config.ConfigFactory
import tech.pegb.backoffice.util.time.DateTimeRangeConfig
import scala.concurrent.duration._

@Singleton
class AppConfig @Inject() (config: play.api.Configuration) {

  val BackOfficeHost: String = config.get[String]("app.host")

  val ActivatedBusinessUserStatus: String = config.getOptional[String]("business-user.activated-status").get

  val NotYetActivatedBusinessUserStatus: String = config.getOptional[String]("business-user.not-yet-activated-status").get

  val BusinessUserStatusWhereUpdateIsAllowed: String = config.getOptional[String]("business-user.allow-update-status").get

  val EmailAttributeName: String = config.getOptional[String]("business-user.email-attribute-name").get

  val AddressAttributeName: String = config.getOptional[String]("business-user.address-attribute-name").get

  val PhoneNumberAttributeName: String = config.getOptional[String]("business-user.phone-number-attribute-name").get

  val UserTypeForBusinessUserRegistration: String = config.getOptional[String]("business-user.registration.user-type").get

  val DefaultSubscriptionForBusinessUserRegistration: String = config.getOptional[String]("business-user.registration.default-subscription").get

  val DefaultSegmentForBusinessUserRegistration: String = config.getOptional[String]("business-user.registration.default-segment").get

  val DefaultTierForBusinessUserRegistration: String = config.getOptional[String]("business-user.registration.default-tier").get

  val DefaultStatusForBusinessUserRegistration: String = config.getOptional[String]("business-user.registration.default-status").get

  val ActivationEmailTemplate: String = config.getOptional[String]("business-user.registration.activation-email-template").get

  val DefaultBusinessUserTypeForRegistration: String = config.getOptional[String]("business-user.registration.default-business-user-type").get

  val BusinessUserAccountTypeOnRegistration = config.getOptional[String]("business-user.registration.default-account-type").get

  val NewlyCreatedAccountStatus: String = config.getOptional[String]("account.newly-created-status").get

  val DefaultCurrency = config.getOptional[String]("wallet.default-currency").get

  val DefaultNewCardApplicationStatus = config.getOptional[String]("card-application.default-status").get

  val OperationTypeForNewCardApplication = config.getOptional[String]("card-application.new-application-type").get

  val WaitingForActivationUserStatus = config.get[String]("user.waiting-for-activation-status")

  val ActiveUserStatus = config.get[String]("user.active-status")

  val PassiveUserStatus = config.get[String]("user.passive-status")

  val AccountCloseStatus = config.get[String]("account.close-status")

  val IndividualUserType = config.get[String]("user.type.individual-user")

  val BusinessUserType = config.get[String]("user.type.business-user")

  val WalletAccountType = config.get[String]("account.type.wallet-account")

  val NationalIdDocumentType = config.get[String]("document.type.national-id")

  val SelfieDocumentType = config.get[String]("document.type.selfie")

  val LivenessDocumentType = config.get[String]("document.type.liveness")

  val DocumentApprovedStatus = config.get[String]("document.status.approved")

  val PendingWalletApplicationStatus = config.get[String]("wallet-application.status.pending")

  val ApprovedWalletApplicationStatus = config.get[String]("wallet-application.status.approved")

  val RejectedWalletApplicationStatus = config.get[String]("wallet-application.status.rejected")

  val CoreWalletApplicationActivationUrl = config.get[String]("hosts.url.core-wallet-activation")
  val CoreWalletApplicationActivationUrlVerb = config.get[String]("hosts.url.core-wallet-activation-verb")

  val CoreCurrencyExchangeActivationUrl: String = config.get[String]("hosts.url.core-fx-activation")
  val CoreCurrencyExchangeActivationUrlVerb: String = config.get[String]("hosts.url.core-fx-activation-verb")

  val CreateManualTxnUrl: String = config.get[String]("hosts.url.core-create-manual-transaction")
  val CreateManualTxnFxUrl: String = config.get[String]("hosts.url.core-create-manual-transaction-fx")
  val CreateManualTxnUrlVerb: String = config.get[String]("hosts.url.core-create-manual-transaction-verb")

  val CancelTransactionUrl: String = config.get[String]("hosts.url.core-cancel-transaction")
  val CancelTransactionUrlVerb: String = config.get[String]("hosts.url.core-cancel-transaction-verb")

  val ReverseTransactionUrl: String = config.get[String]("hosts.url.core-reverse-transaction")
  val ReverseTransactionUrlVerb: String = config.get[String]("hosts.url.core-reverse-transaction-verb")

  val CoreDeactivateSavingGoalUrl: String = config.get[String]("hosts.url.core-deactivate-saving-goals")
  val CoreDeactivateSavingGoalUrlVerb: String = config.get[String]("hosts.url.core-deactivate-saving-goals-verb")

  val CoreDeactivateAutoDeductUrl: String = config.get[String]("hosts.url.core-deactivate-auto-deduct-savings")
  val CoreDeactivateAutoDeductUrlVerb: String = config.get[String]("hosts.url.core-deactivate-auto-deduct-savings-verb")

  val CoreDeactivateRoundUpUrl: String = config.get[String]("hosts.url.core-deactivate-roundup-savings")
  val CoreDeactivateRoundUpUrlVerb: String = config.get[String]("hosts.url.core-deactivate-roundup-savings-verb")

  val BackOfficeEventsNotificationUrl: String = config.get[String]("hosts.url.backoffice-events-notification")
  val BackOfficeEventsNotificationUrlVerb: String = config.get[String]("hosts.url.backoffice-events-notification-verb")

  val CreateBusinessUserUrl: String = config.get[String]("hosts.url.core-create-business-user")
  val CreateBusinessUserUrlVerb: String = config.get[String]("hosts.url.core-create-business-user-verb")

  val ResetVelocityPortalUserPinUrl: String = config.get[String]("hosts.url.vp-reset-pin")
  val ResetVelocityPortalUserPinUrlVerb: String = config.get[String]("hosts.url.vp-reset-pin-verb")

  val InactiveStatuses = config.get[String]("user.inactive-statuses").split(",").map(_.trim).toSet

  val TransientFileExpiryTime: FiniteDuration = config.get[FiniteDuration]("document.expiration")

  val PaginationLimit = config.get[Int]("pagination.defaultLimit")

  val PaginationOffset = config.get[Int]("pagination.defaultOffset")

  val PaginationMaxLimit = config.get[Int]("pagination.max-cap")

  val FlatFee = config.get[String]("fee-profile.calculation-method.flat-fee")

  val FlatPercentage = config.get[String]("fee-profile.calculation-method.flat-percentage")

  val StaircaseFlatFee = config.get[String]("fee-profile.calculation-method.staircase-with-flat-fees")

  val StaircaseFlatPercentage = config.get[String]("fee-profile.calculation-method.staircase-with-flat-percentages")

  val FloatAccountNumbersKey = config.get[String]("float.account.numbers.key")

  val FutureTimeout = config.get[FiniteDuration]("future-timeout")

  val SchemaName = config.get[String]("db.reports.schema")

  val DefaultLocale = config.get[String]("default.locale")

  val DefaultPlatform = config.get[String]("default.platform")

  val LogMaxLength = config.getOptional[Int]("log.max-len").getOrElse(512)

/***
    * MakerChecker config
    **/

  val RemoveFromRequest: Array[String] = config.get[String]("remove.from.url").split(",")

  object Hosts {
    private val root = "hosts"
    val TimeoutSeconds = config.get[FiniteDuration](s"$root.timeout.seconds")
    val MainBackofficeApi = config.get[String](s"$root.main-backoffice-api")
    val BackofficeAuthApi = config.get[String](s"$root.backoffice-auth-api")
    val MainBackofficeApiPlaceholder = config.get[String](s"$root.placeholder.main-backoffice-api")
  }

  object HeaderKeys {
    private val root = "http-header-keys"

    val StrictDeserializationKey = config.get[String](s"$root.strict-deserialization")
    val RequestIdKey = config.get[String](s"$root.request-id")
    val RequestDateKey = config.get[String](s"$root.request-date")
    val RequestFromKey = config.get[String](s"$root.request-from")
    val RoleLevelKey = config.get[String](s"$root.role-level")
    val BusinessUnitKey = config.get[String](s"$root.business-unit-name")

    val latestVersion = config.get[String](s"$root.latest-version")
    val accessControlExposeHeaders: String = config.get[String](s"$root.access-control-expose-headers")

    val ApiKey = config.get[String](s"$root.api-key")
  }

  object ApiKeys {
    private val root = "api-keys"
    val MakerChecker = config.get[String](s"$root.maker-checker-api")
    val BackofficeAuth = config.get[String](s"$root.backoffice-auth-api")
    val MainBackofficeApi = config.get[String](s"$root.backoffice-api")
  }

  object Mailer {
    private val root = "mailer"
    val Enabled = config.get[Boolean](s"$root.enabled")
    val Host = config.get[String](s"$root.host")
    val Port = config.get[Int](s"$root.port")
    val Sender = config.get[String](s"$root.sender")
  }

  object I18N {
    val i18nLocale: String = config.get[String]("default.locale")
    val i18nPlatform: String = config.get[String]("default.platform")
  }

  object Document {
    val FormKeyForFileUpload: String = config.get[String]("document.multipart-form-data.doc-key")
    val FormKeyForJson: String = config.get[String]("document.multipart-form-data.json-key")
  }

  object Authentication {
    private val auth = "authentication"

    object PasswordConfig {
      private val root = s"$auth.password-generation"
      val default: String = config.get[String](s"$root.default")
      val nDigits: Int = config.get[Int](s"$root.n-digits")
      val nLowercase: Int = config.get[Int](s"$root.n-lowercase")
      val nUppercase: Int = config.get[Int](s"$root.n-uppercase")
      val nSpecialChars: Int = config.get[Int](s"$root.n-special-chars")
      val length: Int = config.get[Int](s"$root.length")
      val duplicateCharsAllowed: Boolean = config.get[Boolean](s"$root.duplicate-chars-allowed ")
    }

    val secret = config.get[String](s"$auth.secret")
    val recaptchaSecret = config.get[String](s"$auth.recaptcha-secret")
    val recaptchaUrl = config.get[String](s"$auth.recaptcha-url")

    val tokenExpirationOffsetMinutes = config.get[Int](s"$auth.token-expiration-offset-minutes")

    val accountLockTimeout = config.get[FiniteDuration](s"$auth.account-lock-timeout")
    val maxBadLoginAttempts = config.get[Int](s"$auth.max-bad-login-attempts")
    val requireCaptcha = config.get[Boolean](s"$auth.require-captcha")
  }

  object MakerChecker {
    private val root = "maker-checker"

    val exemptedModules = config.get[String](s"$root.exempted-modules")

    object Notification {
      private val notification = "notification"

      val i18nSubjectKey = config.get[String](s"$root.$notification.i18n-subject-key")
      val i18nBodyKey = config.get[String](s"$root.$notification.i18n-body-key")

      val defaultSubject = config.get[String](s"$root.$notification.default-subject")
      val defaultBody = config.get[String](s"$root.$notification.default-body")
    }
  }

  object Aggregations {
    private val root = "aggregations"
    val defaultNumDaysWhenToSwitchDataSource = config.get[Int](s"$root.default-num-days-datasource-switch")
    val isOnTheFly = config.get[Boolean](s"$root.is-on-the-fly")
    val timeout = config.get[FiniteDuration](s"$root.future-timeout")
    val floatUserBalancePercentageKey = config.get[String](s"$root.float-user-balance-percentage-institution-map-key")
    val closingUserBalanceUserType = config.get[String](s"$root.closing-user-balance-user-type")
  }

  //TODO put under Hosts
  object AggregationEndpoints {
    private val root = "hosts.url"
    val host = s"${Hosts.MainBackofficeApi}"

    val aggregation = s"$host${config.get[String](s"$root.backoffice-aggregation")}"
    val aggregationMargin = s"$host${config.get[String](s"$root.backoffice-aggregation-margin")}"
  }

  //TODO put under Aggregations
  object AggregationConstants {
    private val root = "aggregations"
    val step = config.get[Int](s"$root.revenue-step")
  }

  object SettlementConstants {
    private val root = "settlements"
    val fxRecentCount = config.get[Int](s"$root.default-fx-recent-count")
  }

  object Reports {
    val dbSchema = config.get[String]("db.reports.schema")
    val cashflowReportUuid = UUID.fromString(config.get[String]("dashboardReports.cashflowUuid"))
  }

  object DateTimeRangeLimits {
    private val root = "date-time-range"
    private val startDateLimitFromCurrentInDays = config.get[Int](s"$root.start-limit-in-days").days
    private val endDateLimitFromCurrentInDays = config.get[Int](s"$root.end-limit-in-days").days
    private val startEndDateRangeLimitInDays = config.get[Int](s"$root.start-end-limit-in-days").days

    val dateTimeRangeConfig = DateTimeRangeConfig(
      startDateLimitFromCurrent = Some(startDateLimitFromCurrentInDays),
      endDateLimitFromCurrent = Some(endDateLimitFromCurrentInDays),
      rangeLimit = Some(startEndDateRangeLimitInDays))
  }

}

object AppConfig {
  def apply(arg: String) = new AppConfig(play.api.Configuration(ConfigFactory.load(arg)))
}
