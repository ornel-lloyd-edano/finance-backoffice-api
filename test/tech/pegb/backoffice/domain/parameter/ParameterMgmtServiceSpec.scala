package tech.pegb.backoffice.domain.parameter

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import java.time._

import org.coursera.autoschema.AutoSchema
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import play.api.libs.json._
import tech.pegb.backoffice.dao.account.abstraction.AccountTypesDao
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountType
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.CountryDao
import tech.pegb.backoffice.dao.businessuserapplication.entity.Country
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.settings.abstraction.SystemSettingsDao
import tech.pegb.backoffice.dao.settings.entity.SystemSetting
import tech.pegb.backoffice.dao.types.entity.{Description, DescriptionType}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.parameter.abstraction.ParameterManagement
import tech.pegb.backoffice.domain.parameter.dto.{ParameterCriteria, ParameterToCreate}
import tech.pegb.backoffice.domain.parameter.implementation.ParameterMgmtService
import tech.pegb.backoffice.domain.parameter.implementation.ParameterMgmtService.{AccountTypes, Currencies, SystemSettings, Types, Countries}
import tech.pegb.backoffice.domain.parameter.model.{MetadataSchema, Parameter, Platforms}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class ParameterMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val localDateOrdering: scala.Ordering[LocalDateTime] = _ compareTo _
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  implicit val systemSettingFormat: OWrites[SystemSetting] = Json.writes[SystemSetting]

  private val systemSettingsDao = stub[SystemSettingsDao]
  private val currencyDao = stub[CurrencyDao]
  private val accountTypesDao = stub[AccountTypesDao]
  private val countryDao = stub[CountryDao]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(1563288490000L), ZoneId.of("UTC"))
  val now = LocalDateTime.now(mockClock)

  val parameterMgmt = inject[ParameterManagement]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[SystemSettingsDao].to(systemSettingsDao),
      bind[CountryDao].to(countryDao),
      bind[CurrencyDao].to(currencyDao),
      bind[AccountTypesDao].to(accountTypesDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val s1 = SystemSetting(
    id = 2,
    key = "saving_goal_reasons",
    value = """["vacation","marriage","birthday","gift","new_car"]""",
    `type` = "json",
    explanation = None,
    forAndroid = true,
    forIOS = true,
    forBackoffice = false,
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = now.some,
    updatedBy = none)

  val s2 = SystemSetting(
    id = 4,
    key = "roundup_saving_tx_types",
    value = """["p2p_domestic"]""",
    `type` = "json",
    explanation = None,
    forAndroid = false,
    forIOS = true,
    forBackoffice = false,
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = now.some,
    updatedBy = none)

  val s3 = SystemSetting(
    id = 5,
    key = "roundup_saving_nearest",
    value = "10",
    `type` = "integer",
    explanation = None,
    forAndroid = true,
    forIOS = true,
    forBackoffice = false,
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = now.some,
    updatedBy = none)

  val a1 = AccountType(
    id = 1,
    accountTypeName = "WALLET",
    description = "standard account type for individual users".some,
    isActive = true,
    createdAt = now,
    createdBy = "dima",
    updatedAt = none,
    updatedBy = none)
  val a2 = AccountType(
    id = 2,
    accountTypeName = "STANDARD",
    description = "standard account type for individual users".some,
    isActive = true,
    createdAt = now,
    createdBy = "david",
    updatedAt = none,
    updatedBy = none)
  val a3 = AccountType(
    id = 3,
    accountTypeName = "NEW",
    description = "standard account type for individual users".some,
    isActive = true,
    createdAt = now,
    createdBy = "lloyd",
    updatedAt = none,
    updatedBy = none)

  val c1 = Currency(
    id = 1,
    name = "KES",
    description = "kenya shillings".some,
    isActive = true,
    icon = "icon_one".some,
    createdAt = now,
    createdBy = "dima",
    updatedAt = none,
    updatedBy = none)
  val c2 = Currency(
    id = 2,
    name = "USD",
    description = "randomly updated".some,
    isActive = true,
    icon = "test".some,
    createdAt = now,
    createdBy = "ujali",
    updatedAt = now.some,
    updatedBy = "SuperUser".some)
  val c3 = Currency(
    id = 3,
    name = "INR",
    description = "indian currency".some,
    isActive = true,
    icon = none,
    createdAt = now,
    createdBy = "SuperUser",
    updatedAt = none,
    updatedBy = none)

  val dt1 = DescriptionType(
    id = 1,
    `type` = "limit_profile_types",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = none,
    updatedBy = none)
  val dt2 = DescriptionType(
    id = 2,
    `type` = "task_statuses",
    createdAt = now,
    createdBy = "pegbuser",
    updatedAt = none,
    updatedBy = none)

  val d1 = Description(
    id = 1,
    name = "transaction_based",
    description = "transactionbased limit profile".some)
  val d2 = Description(
    id = 2,
    name = "balance_based",
    description = "transactionbased limit profile".some)

  val d3 = Description(
    id = 3,
    name = "pending",
    description = "task is pending approval".some)
  val d4 = Description(
    id = 4,
    name = "approved",
    description = "task was approved".some)
  val d5 = Description(
    id = 5,
    name = "rejected",
    description = "task was rejected".some)

  val p_accountTypes = Parameter(
    id = UUID.fromString("30303031-3a30-3030-3030-303030303031"),
    key = "account_types",
    value = Json.parse(
      """[{
        |"id":1,
        |"accountTypeName":"WALLET",
        |"description":"standard account type for individual users",
        |"isActive":true,
        |"createdAt":"2019-07-16T14:48:10",
        |"createdBy":"dima"
        |},
        |{
        |"id":2,
        |"accountTypeName":"STANDARD",
        |"description":"standard account type for individual users",
        |"isActive":true,
        |"createdAt":"2019-07-16T14:48:10",
        |"createdBy":"david"
        |},
        |{
        |"id":3,
        |"accountTypeName":"NEW",
        |"description":"standard account type for individual users",
        |"isActive":true,
        |"createdAt":"2019-07-16T14:48:10",
        |"createdBy":"lloyd"}]""".stripMargin),
    explanation = None,
    metadataId = "account_types",
    platforms = Platforms.AllValidPlatforms,
    createdAt = now.some,
    createdBy = "dima".some,
    updatedAt = None,
    updatedBy = None)

  val country1 = Country(
    id = 1,
    name = "India",
    label = Some("IN"),
    currencyId = Some(1),
    isActive = Some(true),
    createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
    createdBy = "test_user")
  val country2 = Country(
    id = 2,
    name = "Pakistan",
    label = Some("PAK"),
    currencyId = Some(2),
    isActive = Some(true),
    createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
    createdBy = "test_user")
  val country3 = Country(
    id = 3,
    name = "Philippines",
    label = Some("PH"),
    currencyId = Some(3),
    isActive = Some(true),
    createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
    createdBy = "test_user")

  val p_countries = Parameter(
    id = UUID.fromString("30303035-3a30-3030-3030-303030303031"),
    key = "countries",
    value = Json.parse(
      """[{
        |"id":1,
        |"name":"India",
        |"label":"IN",
        |"icon":null,
        |"currency":1,
        |"isActive":true,
        |"createdAt":"2019-01-01T00:00:00",
        |"createdBy":"test_user"
        |},
        |{
        |"id":2,
        |"name":"Pakistan",
        |"label":"PAK",
        |"icon":null,
        |"currency":2,
        |"isActive":true,
        |"createdAt":"2019-01-01T00:00:00",
        |"createdBy":"test_user"
        |},
        |{
        |"id":3,
        |"name":"Philippines",
        |"label":"PH",
        |"icon":null,
        |"currency":3,
        |"isActive":true,
        |"createdAt":"2019-01-01T00:00:00",
        |"createdBy":"test_user"
        |}]""".stripMargin),
    explanation = None,
    metadataId = "countries",
    platforms = Platforms.AllValidPlatforms,
    createdAt = now.some,
    createdBy = "test_user".some,
    updatedAt = None,
    updatedBy = None)

  val p_currencies = Parameter(
    id = UUID.fromString("30303032-3a30-3030-3030-303030303031"),
    key = "currencies",
    value = Json.parse(
      """[
        |{"id":1,
        |"name":"KES",
        |"description":"kenya shillings",
        |"isActive":true,
        |"icon":"icon_one",
        |"createdAt":"2019-07-16T14:48:10",
        |"createdBy":"dima"
        |},
        |{"id":2,
        |"name":"USD",
        |"description":"randomly updated",
        |"isActive":true,
        |"icon":"test",
        |"createdAt":"2019-07-16T14:48:10",
        |"createdBy":"ujali",
        |"updatedAt":"2019-07-16T14:48:10",
        |"updatedBy":"SuperUser"
        |},
        |{"id":3,
        |"name":"INR",
        |"description":"indian currency",
        |"isActive":true,
        |"createdAt":"2019-07-16T14:48:10",
        |"createdBy":"SuperUser"
        |}]""".stripMargin),
    explanation = none,
    metadataId = "currencies",
    platforms = Platforms.AllValidPlatforms,
    createdAt = now.some,
    createdBy = "dima".some,
    updatedAt = now.some,
    updatedBy = "SuperUser".some)

  val p_limitProfileTypes = Parameter(
    id = UUID.fromString("30303034-3a30-3030-3030-303030303031"),
    key = "limit_profile_types",
    value = Json.parse(
      """[{
        |"id":1,
        |"name":"transaction_based",
        |"description":"transactionbased limit profile"
        |},
        |{"id":2,
        |"name":"balance_based",
        |"description":"transactionbased limit profile"
        |}]""".stripMargin),
    explanation = None,
    metadataId = "types",
    platforms = Platforms.AllValidPlatforms,
    createdAt = now.some,
    createdBy = "pegbuser".some,
    updatedAt = None,
    updatedBy = None)

  val p_taskStatuses = Parameter(
    id = UUID.fromString("30303034-3a30-3030-3030-303030303032"),
    key = "task_statuses",
    value = Json.parse(
      """[{
        |"id":3,
        |"name":"pending",
        |"description":"task is pending approval"
        |},
        |{"id":4,
        |"name":"approved",
        |"description":"task was approved"
        |},
        |{"id":5,
        |"name":"rejected",
        |"description":"task was rejected"}]""".stripMargin),
    explanation = None,
    metadataId = "types",
    platforms = Platforms.AllValidPlatforms,
    createdAt = now.some,
    createdBy = "pegbuser".some,
    updatedAt = None,
    updatedBy = None)

  val p_s1 = Parameter(
    id = UUID.fromString("30303033-3a30-3030-3030-303030303032"),
    key = "saving_goal_reasons",
    value = Json.toJson(s1),
    explanation = None,
    metadataId = "system_settings",
    platforms = Seq(Platforms.MobileAndroid, Platforms.MobileIOS),
    createdAt = now.some,
    createdBy = "pegbuser".some,
    updatedAt = now.some,
    updatedBy = None)

  val p_s2 = Parameter(
    id = UUID.fromString("30303033-3a30-3030-3030-303030303034"),
    key = "roundup_saving_tx_types",
    value = Json.toJson(s2),
    explanation = None,
    metadataId = "system_settings",
    platforms = Seq(Platforms.MobileIOS),
    createdAt = now.some,
    createdBy = "pegbuser".some,
    updatedAt = now.some,
    updatedBy = None)

  val p_s3 = Parameter(
    id = UUID.fromString("30303033-3a30-3030-3030-303030303035"),
    key = "roundup_saving_nearest",
    value = Json.toJson(s3),
    explanation = None,
    metadataId = "system_settings",
    platforms = Seq(Platforms.MobileAndroid, Platforms.MobileIOS),
    createdAt = now.some,
    createdBy = "pegbuser".some,
    updatedAt = now.some,
    updatedBy = None)

  "ParameterMgmtService getParameters" should {
    "return all parameters from 5 tables when no criteria" in {
      val criteria = ParameterCriteria()

      val ordering = Seq(Ordering("metadata_id", Ordering.ASCENDING))

      (accountTypesDao.getAll _).when().returns(Right(Set(a1, a2, a3)))
      (currencyDao.getAll _).when().returns(Right(Set(c1, c2, c3)))
      (countryDao.getCountries _).when().returns(Right(Seq(country1, country2, country3)))
      (typesDao.fetchAllTypes _).when().returns(Right(Map(dt1 → Seq(d1, d2), dt2 → Seq(d3, d4, d5))))
      (systemSettingsDao.getSystemSettingsByCriteria _).when(*, *, *, *)
        .returns(Right(Seq(s1, s2, s3)))

      val allParameters = Seq(p_accountTypes, p_countries, p_currencies, p_s1, p_s2, p_s3, p_limitProfileTypes, p_taskStatuses)

      val result = parameterMgmt.filterParametersByCriteria(allParameters, criteria, ordering, None, None)

      whenReady(result)(actual ⇒ actual mustBe Right(allParameters))
    }

    "return all account types only when filtered by metadata_id" in {
      val criteria = ParameterCriteria(metadataId = "account_types".some)

      val ordering = Seq(Ordering("metadata_id", Ordering.ASCENDING))

      (accountTypesDao.getAll _).when().returns(Right(Set(a1, a2, a3)))
      (currencyDao.getAll _).when().returns(Right(Set(c1, c2, c3)))
      (typesDao.fetchAllTypes _).when().returns(Right(Map(dt1 → Seq(d1, d2), dt2 → Seq(d3, d4, d5))))
      (systemSettingsDao.getSystemSettingsByCriteria _).when(*, *, *, *)
        .returns(Right(Seq(s1, s2, s3)))

      val allParameters = Seq(p_accountTypes, p_currencies, p_s1, p_s2, p_s3, p_limitProfileTypes, p_taskStatuses)

      val result = parameterMgmt.filterParametersByCriteria(allParameters, criteria, ordering, None, None)

      whenReady(result)(actual ⇒ actual mustBe Right(Seq(p_accountTypes)))

    }

    "return all account types only when filtered by platforms" in {
      val criteria = ParameterCriteria(platforms = Seq(Platforms.MobileAndroid, Platforms.MobileIOS).some)

      val ordering = Seq(Ordering("metadata_id", Ordering.ASCENDING))

      (accountTypesDao.getAll _).when().returns(Right(Set(a1, a2, a3)))
      (currencyDao.getAll _).when().returns(Right(Set(c1, c2, c3)))
      (typesDao.fetchAllTypes _).when().returns(Right(Map(dt1 → Seq(d1, d2), dt2 → Seq(d3, d4, d5))))
      (systemSettingsDao.getSystemSettingsByCriteria _).when(*, *, *, *)
        .returns(Right(Seq(s1, s2, s3)))

      val expected = Seq(p_accountTypes, p_currencies, p_s1, p_s3, p_limitProfileTypes, p_taskStatuses)

      val result = parameterMgmt.filterParametersByCriteria(expected, criteria, ordering, None, None)

      whenReady(result)(actual ⇒ actual mustBe Right(expected))

    }

    "return all account types only when filtered by key" in {
      val criteria = ParameterCriteria(key = "limit_profile_types".some)

      val ordering = Seq(Ordering("metadata_id", Ordering.ASCENDING))

      (accountTypesDao.getAll _).when().returns(Right(Set(a1, a2, a3)))
      (currencyDao.getAll _).when().returns(Right(Set(c1, c2, c3)))
      (typesDao.fetchAllTypes _).when().returns(Right(Map(dt1 → Seq(d1, d2), dt2 → Seq(d3, d4, d5))))
      (systemSettingsDao.getSystemSettingsByCriteria _).when(*, *, *, *)
        .returns(Right(Seq(s1, s2, s3)))

      val expected = Seq(p_limitProfileTypes)

      val result = parameterMgmt.filterParametersByCriteria(expected, criteria, ordering, None, None)

      whenReady(result)(actual ⇒ actual mustBe Right(expected))
    }

    "return correct number of elements list when limit and offset are applied" in {
      val criteria = ParameterCriteria(metadataId = "system_settings".some)

      val ordering = Seq(Ordering("metadata_id", Ordering.ASCENDING))

      (accountTypesDao.getAll _).when().returns(Right(Set(a1, a2, a3)))
      (currencyDao.getAll _).when().returns(Right(Set(c1, c2, c3)))
      (typesDao.fetchAllTypes _).when().returns(Right(Map(dt1 → Seq(d1, d2), dt2 → Seq(d3, d4, d5))))
      (systemSettingsDao.getSystemSettingsByCriteria _).when(*, *, *, *)
        .returns(Right(Seq(s1, s2, s3)))

      val all = Seq(p_accountTypes, p_currencies, p_s1, p_s3, p_limitProfileTypes, p_taskStatuses)
      val expected = Seq(p_s3)

      val result = parameterMgmt.filterParametersByCriteria(all, criteria, ordering, 1.some, 1.some)

      whenReady(result)(actual ⇒ actual mustBe Right(expected))
    }

    "return count from 4 tables when no criteria" in {
      val criteria = ParameterCriteria()

      (accountTypesDao.getAll _).when().returns(Right(Set(a1, a2, a3)))
      (currencyDao.getAll _).when().returns(Right(Set(c1, c2, c3)))
      (typesDao.fetchAllTypes _).when().returns(Right(Map(dt1 → Seq(d1, d2), dt2 → Seq(d3, d4, d5))))
      (systemSettingsDao.getSystemSettingsByCriteria _).when(*, *, *, *)
        .returns(Right(Seq(s1, s2, s3)))

      val all = Seq(p_accountTypes, p_currencies, p_s1, p_s3, p_limitProfileTypes, p_taskStatuses)

      val result = parameterMgmt.countParametersByCriteria(all, criteria)

      whenReady(result)(actual ⇒ actual mustBe Right(6))

    }

  }

  "ParamterMgmtService getMetadataSchemaById(uuid: UUID)" should {
    "return metadata for AccountTypes" in {
      val result = parameterMgmt.getMetadataSchemaById("account_types")

      val expected = MetadataSchema("account_types", AutoSchema.createSchema[AccountType], readOnlyFields = Seq("id"), isCreationAllowed = false, isDeletionAllowed = false, true)

      whenReady(result)(actual ⇒ actual mustBe Right(expected))
    }

    "return metadata for Currencies" in {
      val result = parameterMgmt.getMetadataSchemaById("currencies")

      val expected = MetadataSchema("currencies", AutoSchema.createSchema[Currency], readOnlyFields = Seq("id"), isCreationAllowed = false, isDeletionAllowed = false, true)

      whenReady(result)(actual ⇒ actual mustBe Right(expected))
    }

    "return metadata for Types" in {
      val result = parameterMgmt.getMetadataSchemaById("types")

      val expected = MetadataSchema("types", AutoSchema.createSchema[Description], readOnlyFields = Seq("id"), isCreationAllowed = true, isDeletionAllowed = false, true)

      whenReady(result)(actual ⇒ actual mustBe Right(expected))
    }

    "return metadata for System settings" in {
      (systemSettingsDao.getSystemSettingById _).when(5L)
        .returns(Right(s3.some))

      val result = parameterMgmt.getMetadataSchemaById("system_settings")

      val expected = MetadataSchema("system_settings", AutoSchema.createSchema[SystemSetting], readOnlyFields = Seq("id"), isCreationAllowed = true, isDeletionAllowed = false, false)

      whenReady(result)(actual ⇒ actual mustBe Right(expected))
    }

  }

  "ParameterMgmtService getParameterUUIDPrefix" should {
    "return metadata id " in {
      whenReady(parameterMgmt.getParameterUUIDPrefix("account_types"))(actual ⇒ actual mustBe Right(1))
      whenReady(parameterMgmt.getParameterUUIDPrefix("currencies"))(actual ⇒ actual mustBe Right(2))
      whenReady(parameterMgmt.getParameterUUIDPrefix("system_settings"))(actual ⇒ actual mustBe Right(3))
      whenReady(parameterMgmt.getParameterUUIDPrefix("types"))(actual ⇒ actual mustBe Right(4))

    }
  }

  "ParameterMgmtService getMetadataSchema" should {
    "return metadata id " in {
      whenReady(parameterMgmt.getMetadataSchema)(actual ⇒ actual mustBe Right(Seq(
        (AccountTypes, MetadataSchema(
          metadataId = AccountTypes,
          schema = AutoSchema.createSchema[AccountType],
          readOnlyFields = Seq("id"),
          isCreationAllowed = false,
          isDeletionAllowed = false,
          isArray = true)),
        (Currencies, MetadataSchema(
          metadataId = Currencies,
          schema = AutoSchema.createSchema[Currency],
          readOnlyFields = Seq("id"),
          isCreationAllowed = false,
          isDeletionAllowed = false,
          isArray = true)),
        (SystemSettings, MetadataSchema(
          metadataId = SystemSettings,
          schema = AutoSchema.createSchema[SystemSetting],
          readOnlyFields = Seq("id"),
          isCreationAllowed = true,
          isDeletionAllowed = false,
          isArray = false)),
        (Types, MetadataSchema(
          metadataId = Types,
          schema = AutoSchema.createSchema[Description],
          readOnlyFields = Seq("id"),
          isCreationAllowed = true,
          isDeletionAllowed = false,
          isArray = true)),
        (Countries, MetadataSchema(
          metadataId = Countries,
          schema = AutoSchema.createSchema[Country],
          readOnlyFields = Seq("id"),
          isCreationAllowed = false,
          isDeletionAllowed = false,
          isArray = true)))))
    }
  }

  "ParamterMgmtService createParameter(createDto: ParameterToCreate)" should {
    import ParameterMgmtService._
    "return message if try to create parameter for AccountType" in {
      val jsValue =
        """
          |[{"name":"test_one"},{"name":"test_two"}]
        """.stripMargin
      val createDto = ParameterToCreate(
        key = "testone",
        value = Json.parse(jsValue),
        explanation = None,
        metadataId = AccountTypes,
        platforms = Seq.empty,
        createdAt = now,
        createdBy = "ujali")
      val resultF = parameterMgmt.createParameter(createDto)

      whenReady(resultF) { result ⇒

        assert(result.isLeft)
        result.contains("operation not allowed by design")

      }
    }

    "return message if try to create parameter for currencies" in {
      val jsValue =
        """
          |[{"name":"test_one"},{"name":"test_two"}]
        """.stripMargin
      val createDto = ParameterToCreate(
        key = "testone",
        value = Json.parse(jsValue),
        explanation = None,
        metadataId = Currencies,
        platforms = Seq.empty,
        createdAt = now,
        createdBy = "ujali")
      val resultF = parameterMgmt.createParameter(createDto)

      whenReady(resultF) { result ⇒

        assert(result.isLeft)
        result.contains("operation not allowed by design")

      }
    }

    "return parameter when system settings created" in {
      val jsValue =
        """{
          |      "id": 20,
          |      "key": "auto_deduct_saving_instruments",
          |      "value": "[\"VISA_DEBIT\"]",
          |      "type": "json",
          |      "forAndroid": true,
          |      "forIOS": true,
          |      "forBackoffice": false
          |    }
        """.stripMargin
      val createDto = ParameterToCreate(
        key = "auto_deduct_saving_instruments",
        value = Json.parse(jsValue),
        explanation = None,
        metadataId = SystemSettings,
        platforms = Seq.empty,
        createdAt = now,
        createdBy = "ujali")
      val s1 = SystemSetting(
        id = 20,
        key = "auto_deduct_saving_instruments",
        value = """[\"VISA_DEBIT\"]""",
        `type` = "json",
        explanation = None,
        forAndroid = true,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "ujali",
        updatedAt = now.some,
        updatedBy = none)
      val expectedResult = Parameter(
        id = UUID.fromString("30303033-3a30-3030-3030-303030303032"),
        key = "auto_deduct_saving_instruments",
        value = Json.toJson(s1),
        explanation = None,
        metadataId = SystemSettings,
        platforms = List(Platforms.MobileAndroid, Platforms.MobileIOS),
        createdAt = s1.createdAt.some,
        createdBy = s1.createdBy.some,
        updatedAt = s1.updatedAt,
        updatedBy = s1.updatedBy)

      (systemSettingsDao.insertSystemSetting _).when(*)
        .returns(Right(s1))
      val resultF = parameterMgmt.createParameter(createDto)

      whenReady(resultF) { result ⇒
        assert(result.isRight)
        result.map(_.copy(id = expectedResult.id)) mustBe Right(expectedResult)

      }
    }
  }
}
