package tech.pegb.backoffice.domain.i18n

import java.sql.Connection
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.data.NonEmptyList
import cats.implicits._
import org.scalatest.Matchers._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.i18n
import tech.pegb.backoffice.dao.i18n.abstraction.I18nStringDao
import tech.pegb.backoffice.dao.i18n.dto.I18nStringToInsert
import tech.pegb.backoffice.dao.i18n.entity.{I18nPair, I18nString}
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.i18n.abstraction.I18nStringManagement
import tech.pegb.backoffice.domain.i18n.dto.{I18nStringCriteria, I18nStringToCreate, I18nStringToUpdate}
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform, I18nText}
import tech.pegb.backoffice.domain.i18n.model.I18nBulkInsertResult
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.i18n.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.i18n.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class I18nMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val i18nStringDao = stub[I18nStringDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[I18nStringDao].to(i18nStringDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val i18nStringMgmtService = inject[I18nStringManagement]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "I18nStringMgmtService createString" should {
    "return created i18nString" in {
      implicit val requestId = UUID.randomUUID()

      val dto = I18nStringToCreate(
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)

      val expected = I18nString(
        id = 1,
        key = "hello",
        text = "hola",
        locale = "es",
        platform = "web",
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      (typesDao.getLocales _).when()
        .returns(Right(List((14, "en", "english".some), (15, "es", "espanyol".some), (16, "de", "deutsch".some))))

      (typesDao.getPlatformTypes _).when()
        .returns(Right(List((11, "web", None), (12, "ios", None), (13, "android", None))))

      (i18nStringDao.insertString _).when(dto.asDao)
        .returns(Right(expected))

      val res = i18nStringMgmtService.createI18nString(dto)
      whenReady(res) { actual ⇒
        actual mustBe Right(expected.asDomain)
      }
    }
  }

  "I18nStringMgmtService getString" should {
    "return i18n string in getStringById" in {
      implicit val requestId = UUID.randomUUID()

      val expected = I18nString(
        id = 1,
        key = "hello",
        text = "hola",
        locale = "es",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      (i18nStringDao.getStringById _).when(1)
        .returns(Right(expected.some))

      val res = i18nStringMgmtService.getI18nStringById(1)
      whenReady(res) { actual ⇒
        actual mustBe Right(expected.asDomain)
      }
    }

    "return error in getStringById when dao returns None" in {
      implicit val requestId = UUID.randomUUID()

      (i18nStringDao.getStringById _).when(1)
        .returns(Right(None))

      val res = i18nStringMgmtService.getI18nStringById(1)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.notFoundError(s"I18n String id [1] not found", requestId.toOption))
      }
    }

    "return count by criteria" in {
      val i18nStringCriteria = I18nStringCriteria(
        platform = I18nPlatform("web").some)

      (i18nStringDao.countStringByCriteria _).when(i18nStringCriteria.asDao)
        .returns(Right(2))

      val result = i18nStringMgmtService.countI18nStringByCriteria(i18nStringCriteria)

      whenReady(result) { actual ⇒
        actual mustBe Right(2)
      }
    }

    "return i18nStrings by criteria" in {

      val i1 = I18nString(
        id = 1,
        key = "hello",
        text = "hola",
        locale = "es",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      val i2 = I18nString(
        id = 2,
        key = "bye",
        text = "adios",
        locale = "es",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      val i18nStringCriteria = I18nStringCriteria(
        locale = I18nLocale("es").some,
        platform = I18nPlatform("web").some)

      val ordering = Seq(Ordering("key", Ordering.DESCENDING))

      (i18nStringDao.getStringByCriteria(
        _: i18n.dto.I18nStringCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(i18nStringCriteria.asDao, ordering.asDao, None, None, None)
        .returns(Right(Seq(i1, i2)))

      val result = i18nStringMgmtService.getI18nStringByCriteria(i18nStringCriteria, ordering, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(i1, i2).map(_.asDomain))
      }
    }

    "return i18nPairs in getI18nDictionary" in {

      val i1 = I18nPair(
        key = "hello",
        text = "hola")

      val i2 = I18nPair(
        key = "bye",
        text = "adios")

      val i18nStringCriteria = I18nStringCriteria(
        locale = I18nLocale("es").some,
        platform = I18nPlatform("web").some)

      (i18nStringDao.getI18nPairsByCriteria _).when(i18nStringCriteria.asDao)
        .returns(Right(Seq(i1, i2)))

      val result = i18nStringMgmtService.getI18nDictionary(i18nStringCriteria)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(i1, i2).map(_.asDomain))
      }
    }

  }

  "I18nString Update " should {
    "return updated dto on successful update" in {
      implicit val requestId = UUID.randomUUID()
      val i1 = I18nString(
        id = 1,
        key = "hello",
        text = "HOLA!",
        locale = "es",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      val i18nStringToUpdate = I18nStringToUpdate(
        text = I18nText("HOLA!").some,
        updatedAt = now,
        lastUpdatedAt = now.some)

      (i18nStringDao.updateString(_: Int, _: i18n.dto.I18nStringToUpdate, _: Boolean)(_: Option[Connection]))
        .when(1, i18nStringToUpdate.asDao, false, None)
        .returns(Right(i1.some))

      val result = i18nStringMgmtService.updateI18nString(1, i18nStringToUpdate)
      whenReady(result) { actual ⇒
        actual mustBe Right(i1.asDomain)
      }
    }

    "return error when i18n not found" in {
      implicit val requestId = UUID.randomUUID()

      val i18nStringToUpdate = I18nStringToUpdate(
        text = I18nText("HOLA!").some,
        updatedAt = now,
        lastUpdatedAt = now.some)

      (i18nStringDao.updateString(_: Int, _: i18n.dto.I18nStringToUpdate, _: Boolean)(_: Option[Connection]))
        .when(1, i18nStringToUpdate.asDao, false, None)
        .returns(Right(None))

      val result = i18nStringMgmtService.updateI18nString(1, i18nStringToUpdate)
      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.notFoundError(s"I18n string 1 was not found", requestId.toOption))
      }
    }
  }

  "I18nString Delete" should {
    "return id on successful delete" in {
      implicit val requestId = UUID.randomUUID()
      (i18nStringDao.deleteString _).when(1, now.some)
        .returns(Right(1.some))

      val result = i18nStringMgmtService.deleteI18nString(1, now.some)
      whenReady(result) { actual ⇒
        actual mustBe Right(1)
      }
    }

    "return error when id" in {
      implicit val requestId = UUID.randomUUID()
      (i18nStringDao.deleteString _).when(1, now.some)
        .returns(Right(None))

      val result = i18nStringMgmtService.deleteI18nString(1, now.some)
      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.notFoundError(s"I18n string 1 was not found", requestId.toOption))
      }
    }
  }

  "I18nString Bulk Insert" should {
    "return number of inserted and updated resources" in {
      val dto1 = I18nStringToCreate(
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto2 = I18nStringToCreate(
        key = I18nKey("good_morning"),
        text = I18nText("buen dia"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto3 = I18nStringToCreate(
        key = I18nKey("bye"),
        text = I18nText("adios"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("android"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)

      val i1 = I18nString(
        id = 1,
        key = "hello",
        text = "hola",
        locale = "es",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      val i2 = I18nString(
        id = 2,
        key = "friend",
        text = "parcero",
        locale = "es",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      val i18nStringCriteria = I18nStringCriteria(
        locale = I18nLocale("es").some)

      val mockTxn = mock[java.sql.Connection]
      (i18nStringDao.startTransaction _).when().returns(Right(mockTxn))

      (i18nStringDao.getStringByCriteria(
        _: i18n.dto.I18nStringCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(i18nStringCriteria.asDao, None, None, None, None)
        .returns(Right(Seq(i1, i2)))

      (i18nStringDao.bulkInsertString(_: NonEmptyList[I18nStringToInsert])(_: Option[Connection]))
        .when(NonEmptyList(dto2.asDao, List(dto3.asDao)), mockTxn.some)
        .returns(Right(2))

      (i18nStringDao.updateString(
        _: Int,
        _: i18n.dto.I18nStringToUpdate,
        _: Boolean)(_: Option[Connection]))
        .when(1, dto1.toUpdateWithIdDto(1).dto.asDao, true, mockTxn.some)
        .returns(Right(Some(i1)))

      (i18nStringDao.endTransaction(_: Connection)).when(mockTxn)
        .returns(Right(Unit))

      val result = i18nStringMgmtService.bulkCreateI18nString(I18nLocale("es"), Seq(dto1, dto2, dto3))

      val expected = I18nBulkInsertResult(
        insertedRowCount = 2,
        updatedRowCount = 1)

      whenReady(result) { actual ⇒
        actual mustBe expected.asRight[ServiceError]
      }
    }

    "return number of inserted and updated resources (all update)" in {
      val dto1 = I18nStringToCreate(
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto2 = I18nStringToCreate(
        key = I18nKey("friend"),
        text = I18nText("parcero"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)

      val i1 = I18nString(
        id = 1,
        key = "hello",
        text = "hola",
        locale = "es",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      val i2 = I18nString(
        id = 2,
        key = "friend",
        text = "parcero",
        locale = "es",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      val i18nStringCriteria = I18nStringCriteria(
        locale = I18nLocale("es").some)

      val mockTxn = mock[java.sql.Connection]
      (i18nStringDao.startTransaction _).when().returns(Right(mockTxn))

      (i18nStringDao.getStringByCriteria(
        _: i18n.dto.I18nStringCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(i18nStringCriteria.asDao, None, None, None, None)
        .returns(Right(Seq(i1, i2)))

      (i18nStringDao.updateString(
        _: Int,
        _: i18n.dto.I18nStringToUpdate,
        _: Boolean)(_: Option[Connection]))
        .when(1, dto1.toUpdateWithIdDto(1).dto.asDao, true, mockTxn.some)
        .returns(Right(Some(i1)))

      (i18nStringDao.updateString(
        _: Int,
        _: i18n.dto.I18nStringToUpdate,
        _: Boolean)(_: Option[Connection]))
        .when(2, dto2.toUpdateWithIdDto(2).dto.asDao, true, mockTxn.some)
        .returns(Right(Some(i2)))

      (i18nStringDao.endTransaction(_: Connection)).when(mockTxn)
        .returns(Right(Unit))

      val result = i18nStringMgmtService.bulkCreateI18nString(I18nLocale("es"), Seq(dto1, dto2))

      val expected = I18nBulkInsertResult(
        insertedRowCount = 0,
        updatedRowCount = 2)

      whenReady(result) { actual ⇒
        actual mustBe expected.asRight[ServiceError]
      }
    }

    "return number of inserted and updated resources (all insert)" in {
      val dto1 = I18nStringToCreate(
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto2 = I18nStringToCreate(
        key = I18nKey("good_morning"),
        text = I18nText("buen dia"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto3 = I18nStringToCreate(
        key = I18nKey("bye"),
        text = I18nText("adios"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("android"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)

      val i18nStringCriteria = I18nStringCriteria(
        locale = I18nLocale("es").some)

      val mockTxn = mock[java.sql.Connection]
      (i18nStringDao.startTransaction _).when().returns(Right(mockTxn))

      (i18nStringDao.getStringByCriteria(
        _: i18n.dto.I18nStringCriteria,
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])(_: Option[Connection])).when(i18nStringCriteria.asDao, None, None, None, None)
        .returns(Right(Nil))

      (i18nStringDao.bulkInsertString(_: NonEmptyList[I18nStringToInsert])(_: Option[Connection]))
        .when(NonEmptyList(dto1.asDao, List(dto2.asDao, dto3.asDao)), mockTxn.some)
        .returns(Right(3))

      (i18nStringDao.endTransaction(_: Connection)).when(mockTxn)
        .returns(Right(Unit))

      val result = i18nStringMgmtService.bulkCreateI18nString(I18nLocale("es"), Seq(dto1, dto2, dto3))

      val expected = I18nBulkInsertResult(
        insertedRowCount = 3,
        updatedRowCount = 0)

      whenReady(result) { actual ⇒
        actual mustBe expected.asRight[ServiceError]
      }
    }

    "return error when an item doenst match locale" in {
      val dto1 = I18nStringToCreate(
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("en"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto2 = I18nStringToCreate(
        key = I18nKey("good_morning"),
        text = I18nText("buen dia"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto3 = I18nStringToCreate(
        key = I18nKey("bye"),
        text = I18nText("adios"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("android"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)

      val result = i18nStringMgmtService.bulkCreateI18nString(I18nLocale("es"), Seq(dto1, dto2, dto3))

      whenReady(result) { actual ⇒
        actual.left.get.message should include("Items with different locale from es are found")
      }
    }

    "return error when duplicates found" in {
      val dto1 = I18nStringToCreate(
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto2 = I18nStringToCreate(
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)
      val dto3 = I18nStringToCreate(
        key = I18nKey("bye"),
        text = I18nText("adios"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("android"),
        `type` = "chat_messages".some,
        explanation = None,
        createdAt = now)

      val result = i18nStringMgmtService.bulkCreateI18nString(I18nLocale("es"), Seq(dto1, dto2, dto3))

      whenReady(result) { actual ⇒
        actual.left.get.message should include("Duplicates found in items to create")
      }
    }
  }
}
