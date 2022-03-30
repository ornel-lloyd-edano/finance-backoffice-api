package tech.pegb.backoffice.api.parameter

import java.time._
import java.util.UUID

import cats.implicits._
import org.coursera.autoschema.AutoSchema
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.parameter.dto.{ParameterToCreate, ParameterToUpdate}
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountType
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.settings.entity.SystemSetting
import tech.pegb.backoffice.dao.types.entity.Description
import tech.pegb.backoffice.domain.parameter.abstraction.ParameterManagement
import tech.pegb.backoffice.domain.parameter.implementation.ParameterMgmtService.{AccountTypes, Currencies, SystemSettings, Types}
import tech.pegb.backoffice.domain.parameter.model.{MetadataSchema, Parameter}
import tech.pegb.backoffice.mapping.api.domain.parameter.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

//TODO add integration test
class ParameterControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val parameterManagement = stub[ParameterManagement]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[ParameterManagement].to(parameterManagement),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(1551830400000L), ZoneId.of("UTC"))
  private val id = UUID.randomUUID()
  private val createdAt = mockRequestDate

  "ParameterController" should {

    val jsonRequest =
      """
        |{
        |"key": "auto_deduct_saving_instruments",
        |"value":{
        |"id": 20,
        |"key": "auto_deduct_saving_instruments",
        |"value":  "[\"VISA_DEBIT\"]",
        |"type": "json",
        |"forAndroid": false,
        |"forIOS": false,
        |"forBackoffice": false
        |},
        |"explanation": null,
        |"metadata_id": "system_settings",
        |"platforms": []
        |}
      """.stripMargin

    val jsonRequestForUpdate =
      """
         |{
         |"key": "auto_deduct_saving_instruments",
         |"value":{
         |"id": 20,
         |"key": "auto_deduct_saving_instruments",
         |"value":  "[\"VISA_DEBIT\"]",
         |"type": "json",
         |"forAndroid": false,
         |"forIOS": false,
         |"forBackoffice": false
         |},
         |"explanation": null,
         |"metadata_id": "system_settings",
         |"platforms": [],
         |"updated_at":null
         |}
      """.stripMargin

    val jsonValue =
      """
        |{
        |"id": 20,
        |"key": "auto_deduct_saving_instruments",
        |"value":  "[\"VISA_DEBIT\"]",
        |"type": "json",
        |"forAndroid": false,
        |"forIOS": false,
        |"forBackoffice": false
        |}
      """.stripMargin

    val parameterToCreate = ParameterToCreate(
      key = "auto_deduct_saving_instruments",
      value = Json.parse(jsonValue),
      explanation = none,
      metadataId = "system_settings",
      platforms = Vector.empty)

    val parameterToUpdate = ParameterToUpdate(
      value = Json.parse(jsonValue),
      explanation = none,
      metadataId = "system_settings".some,
      platforms = Vector.empty.some,
      updatedAt = none)

    val p1 = Parameter(
      id = id,
      key = "auto_deduct_saving_instruments",
      value = Json.parse(jsonValue),
      explanation = none,
      metadataId = "system_settings",
      platforms = Vector.empty,
      createdAt = createdAt.toLocalDateTimeUTC.some,
      createdBy = "pegbuser".some,
      updatedAt = createdAt.toLocalDateTimeUTC.some,
      updatedBy = "ujali".some)
    val p2 = Parameter(
      id = id,
      key = "auto_deduct_saving_instruments",
      value = Json.parse(jsonValue),
      explanation = none,
      metadataId = "system_settings",
      platforms = Vector.empty,
      createdAt = createdAt.toLocalDateTimeUTC.some,
      createdBy = "pegbuser".some,
      updatedAt = none,
      updatedBy = none)

    "insert system parameter" in {
      (parameterManagement.createParameter _).when(parameterToCreate.asDomain(createdAt, "pegbuser").get)
        .returns(Future.successful(Right(p2)))

      val expectedJson =
        s"""
           |{"id":"$id",
           |"key":"auto_deduct_saving_instruments",
           |"value":{
           |"id":20,
           |"key":"auto_deduct_saving_instruments",
           |"value":"[\\"VISA_DEBIT\\"]",
           |"type":"json",
           |"forAndroid":false,
           |"forIOS":false,
           |"forBackoffice":false
           |},
           |"platforms":[],
           |"metadata_id":"system_settings",
           |"explanation":null,
           |"created_at":${createdAt.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":null,
           |"updated_by":null
           |}
        """.stripMargin.replaceAll("\n", "")
          .trim

      val fakeRequest = FakeRequest(POST, s"/parameters",
        jsonHeaders.replace((requestDateHeaderKey, createdAt.toString)),
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "update system parameter" in {
      val expectedJson =
        s"""
           |"{"id":"$id",
           |"key":"auto_deduct_saving_instruments",
           |"value":{
           |"id":20,
           |"key":"auto_deduct_saving_instruments",
           |"value":"[\"VISA_DEBIT\"]",
           |"type":"json",
           |"forAndroid":false,
           |"forIOS":false,
           |"forBackoffice":false
           |},
           |"platforms":[],
           |"metadata_id":"system_settings",
           |"explanation":null,
           |"created_at":${createdAt.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":${createdAt.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"ujali"
           |}"
        """.stripMargin.replaceAll("\n", "")
          .trim

      (parameterManagement.updateParameter _)
        .when(id, parameterToUpdate.asDomain(createdAt, "pegbuser").get)
        .returns(Future.successful(Right(p1)))

      val fakeRequest = FakeRequest(PUT, s"/parameters/$id",
        jsonHeaders.replace((requestDateHeaderKey, createdAt.toString)),
        jsonRequestForUpdate)

      val resp = route(app, fakeRequest).get
      status(resp) mustBe OK
      contentAsString(resp) contains expectedJson
    }

    "get metadata by id" in {
      val jsonObject = Json.obj(
        "title" → "Currency",
        "type" → "object",
        "required" → "[\"isActive\",\"name\"]")

      val metadataSchema = MetadataSchema(
        metadataId = "currencies",
        schema = jsonObject,
        readOnlyFields = Seq("id", "name"),
        isCreationAllowed = false,
        isDeletionAllowed = true,
        isArray = true)
      val expectedJson =
        s"""
           |{"metadata_id":"currencies",
           |"schema":{
           |"title":"Currency",
           |"type":"object",
           |"required":"["isActive","name"]"
           |},
           |"read_only_fields":["id","name"],
           |"is_creation_allowed":false,
           |"is_deletion_allowed":true,
           |"is_array":true
           |}
         """.stripMargin.replaceAll("\n", "")
          .replaceAll(" ", "")

      (parameterManagement.getMetadataSchemaById _).when(id.toString)
        .returns(Future.successful(Right(metadataSchema)))

      val fakeRequest = FakeRequest(GET, s"/metadata/$id")
      val resp = route(app, fakeRequest).get
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "get all metadata" in {
      val metadataSchema = Seq(
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
          isArray = true)))
      val expectedJson =
        s"""
           |{"metadata_id":"account_types",
           |"schema":{
           |"title":"AccountType",
           |"type":"object",
           |"required":["isActive","accountTypeName"],
           |"properties":{"accountTypeName":{"type":"string"},
           |"description":{"type":"string"},
           |"isActive":{"type":"boolean"}}},
           |"is_array":true},
           |{"metadata_id":"currencies",
           |"schema":{
           |"title":"Currency",
           |"type":"object",
           |"required":["isActive","name"],
           |"properties":{"description":{"type":"string"},
           |"icon":{"type":"string"},
           |"isActive":{"type":"boolean"},
           |"name":{"type":"string"}}},
           |"is_array":true},
           |{"metadata_id":"system_settings",
           |"schema":{
           |"title":"SystemSetting",
           |"type":"object",
           |"required":
           |["forBackoffice",
           |"forIOS",
           |"forAndroid",
           |"type",
           |"value","key"],
           |"properties":{
           |"explanation":{"type":"string"},
           |"forAndroid":{"type":"boolean"},
           |"forBackoffice":{"type":"boolean"},
           |"forIOS":{"type":"boolean"},
           |"key":{"type":"string"},
           |"type":{"type":"string"},
           |"value":{"type":"string"}}},
           |"is_array":false},
           |{"metadata_id":"types",
           |"schema":
           |{"title":"Description",
           |"type":"object","
           |required":["name"],
           |"properties":{"description":{"type":"string"},
           |"name":{"type":"string"}}},
           |"is_array":true}
         """.stripMargin.replaceAll("\n", "")
          .replaceAll(" ", "")

      (parameterManagement.getMetadataSchema _).when()
        .returns(Future.successful(Right(metadataSchema)))

      val fakeRequest = FakeRequest(GET, s"/metadata")
      val resp = route(app, fakeRequest).get
      status(resp) mustBe OK
      contentAsString(resp) contains expectedJson.toJsonStr
    }

    "get system parameter by id" in {
      val expectedJson =
        s"""
           |{"id":"$id",
           |"key":"auto_deduct_saving_instruments",
           |"value":{
           |"id":20,
           |"key":"auto_deduct_saving_instruments",
           |"value":"[\\"VISA_DEBIT\\"]",
           |"type":"json",
           |"forAndroid":false,
           |"forIOS":false,
           |"forBackoffice":false
           |},
           |"platforms":[],
           |"metadata_id":"system_settings",
           |"explanation":null,
           |"created_at":${createdAt.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":null,
           |"updated_by":null
           |}
        """.stripMargin.replaceAll("\n", "")
          .trim

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(p2))))

      (parameterManagement.filterParametersByCriteria _).when(Seq(p2), id.asDomain, Nil, None, None)
        .returns(Future.successful(Right(Seq(p2))))

      val fakeRequest = FakeRequest(GET, s"/parameters/$id")

      val resp = route(app, fakeRequest).get
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "get system parameters by criteria" in {
      val expectedJson =
        s"""
           |{"total":1,
           |"results":[{
           |"id":"$id",
           |"key":"auto_deduct_saving_instruments",
           |"value":{
           |"id":20,
           |"key":"auto_deduct_saving_instruments",
           |"value":"[\\"VISA_DEBIT\\"]",
           |"type":"json",
           |"forAndroid":false,
           |"forIOS":false,
           |"forBackoffice":false
           |},
           |"platforms":[],
           |"metadata_id":"system_settings",
           |"explanation":null,
           |"created_at":${createdAt.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":${createdAt.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"ujali"
           |}],"limit":null,"offset":null}
        """.stripMargin.replaceAll("\n", "")
          .trim

      (parameterManagement.getParameters _).when()
        .returns(Future.successful(Right(Seq(p1))))

      (parameterManagement.countParametersByCriteria _).when(Seq(p1), ("auto_deduct_saving_instruments".some, "system_settings".some, none).asDomain).returns(Future.successful(Right(1)))

      (parameterManagement.filterParametersByCriteria _).when(Seq(p1), ("auto_deduct_saving_instruments".some, "system_settings".some, none).asDomain, Nil, None, None)
        .returns(Future.successful(Right(Seq(p1))))

      (parameterManagement.getLatestVersion _).when(Seq(p1))
        .returns(Future.successful(Right(none)))

      val fakeRequest = FakeRequest(GET, s"/parameters?key=auto_deduct_saving_instruments&metadata_id=system_settings")

      val resp = route(app, fakeRequest).get
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

  }

}
