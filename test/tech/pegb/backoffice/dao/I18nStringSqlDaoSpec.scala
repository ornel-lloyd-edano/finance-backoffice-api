package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.data.NonEmptyList
import cats.implicits._
import tech.pegb.backoffice.dao.DaoError.{ConstraintViolationError, PreconditionFailed}
import tech.pegb.backoffice.dao.i18n.abstraction.I18nStringDao
import tech.pegb.backoffice.dao.i18n.dto.{I18nStringCriteria, I18nStringToInsert, I18nStringToUpdate}
import tech.pegb.backoffice.dao.i18n.entity.{I18nPair, I18nString}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBTestApp

class I18nStringSqlDaoSpec extends PegBTestApp {

  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[I18nStringDao]

  override def initSql =
    s"""
       |INSERT INTO i18n_strings
       |(id, `key`, `text`, platform, locale, explanation, created_at, updated_at)
       |VALUES
       |(1, 'close', 'close', 'web', 'en-US', null, '$now', '$now'),
       |(2, 'close', 'close', 'ios', 'en-US', null, '$now', '$now'),
       |(3, 'close', 'close', 'android', 'en-US', null, '$now', '$now'),
       |(4, 'how_are_you', 'how are you?', 'web', 'en-US', null, '$now', '$now'),
       |(5, 'how_are_you', 'how are you?', 'ios', 'en-US', null, '$now', '$now'),
       |(6, 'how_are_you', 'how are you?', 'android', 'en-US', null, '$now', '$now'),
       |(7, 'close', 'isara', 'web', 'fil-PH', null, '$now', '$now'),
       |(8, 'how_are_you', 'kumusta?', 'web', 'fil-PH', 'tagalog how are you', '$now', '$now');
    """.stripMargin

  "I18nStringDao retrieve" should {
    "return i18nString in getStringById if exist" in {

      val resp = dao.getStringById(1)

      resp.map(_.get.id) mustBe Right(1)
      resp.map(_.get.key) mustBe Right("close")
      resp.map(_.get.text) mustBe Right("close")
      resp.map(_.get.locale) mustBe Right("en-US")
      resp.map(_.get.platform) mustBe Right("web")
      resp.map(_.get.createdAt) mustBe Right(now)
    }

    "return None in getStringById if NOT exist" in {

      val resp = dao.getStringById(10000)

      resp mustBe Right(None)
    }

    "return i18nString seq in getStringByCriteria - id filter" in {
      val criteria = I18nStringCriteria(
        id = model.CriteriaField("", 1).some,
        key = none,
        explanation = none,
        locale = none,
        platform = none)

      val resp = dao.getStringByCriteria(criteria, none, none, none)

      val expected = I18nString(
        id = 1,
        key = "close",
        text = "close",
        locale = "en-US",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(Seq(expected))

    }

    "return i18nString seq in getStringByCriteria - key filter" in {
      val criteria = I18nStringCriteria(
        id = none,
        key = CriteriaField("", "close").some,
        explanation = none,
        locale = none,
        platform = none)

      val orderingSet = OrderingSet(Ordering("locale", Ordering.ASC), Ordering("platform", Ordering.DESC)).some
      val resp = dao.getStringByCriteria(criteria, orderingSet, none, none)

      val i1 = I18nString(
        id = 1,
        key = "close",
        text = "close",
        locale = "en-US",
        platform = "web",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)
      val i2 = I18nString(
        id = 2,
        key = "close",
        text = "close",
        locale = "en-US",
        platform = "ios",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)
      val i3 = I18nString(
        id = 3,
        key = "close",
        text = "close",
        locale = "en-US",
        platform = "android",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)
      val i4 = I18nString(
        id = 7,
        key = "close",
        text = "isara",
        locale = "fil-PH",
        platform = "web",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(Seq(i1, i2, i3, i4))

    }

    "return i18nString seq in getStringByCriteria - key partial match" in {
      val criteria = I18nStringCriteria(
        id = none,
        key = CriteriaField("", "cl", MatchTypes.Partial).some,
        explanation = none,
        locale = none,
        platform = none)
      val orderingSet = OrderingSet(Ordering("locale", Ordering.ASC), Ordering("platform", Ordering.DESC)).some
      val resp = dao.getStringByCriteria(criteria, orderingSet, none, none)

      val i1 = I18nString(
        id = 1,
        key = "close",
        text = "close",
        locale = "en-US",
        platform = "web",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)
      val i2 = I18nString(
        id = 2,
        key = "close",
        text = "close",
        locale = "en-US",
        platform = "ios",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)
      val i3 = I18nString(
        id = 3,
        key = "close",
        text = "close",
        locale = "en-US",
        platform = "android",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)
      val i4 = I18nString(
        id = 7,
        key = "close",
        text = "isara",
        locale = "fil-PH",
        platform = "web",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(Seq(i1, i2, i3, i4))

    }

    "return i18nString seq in getStringByCriteria - locale filter" in {
      val criteria = I18nStringCriteria(
        id = None,
        key = None,
        explanation = None,
        locale = model.CriteriaField("", "fil-PH").some,
        platform = None)

      val orderingSet = OrderingSet(Ordering("key", Ordering.ASC)).some
      val resp = dao.getStringByCriteria(criteria, orderingSet, None, None)

      val i1 = I18nString(
        id = 7,
        key = "close",
        text = "isara",
        locale = "fil-PH",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)
      val i2 = I18nString(
        id = 8,
        key = "how_are_you",
        text = "kumusta?",
        locale = "fil-PH",
        platform = "web",
        `type` = None,
        explanation = "tagalog how are you".some,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(Seq(i1, i2))

    }

    "return i18nString seq in getStringByCriteria - platform with limit filter" in {
      val criteria = I18nStringCriteria(
        id = none,
        key = none,
        explanation = none,
        locale = none,
        platform = CriteriaField("", "web").some)
      val orderingSet = OrderingSet(Ordering("locale", Ordering.DESC), Ordering("key", Ordering.ASC)).some
      val resp = dao.getStringByCriteria(criteria, orderingSet, 2.some, none)

      val i1 = I18nString(
        id = 7,
        key = "close",
        text = "isara",
        locale = "fil-PH",
        platform = "web",
        `type` = None,
        explanation = none,
        createdAt = now,
        updatedAt = now.some)
      val i2 = I18nString(
        id = 8,
        key = "how_are_you",
        text = "kumusta?",
        locale = "fil-PH",
        platform = "web",
        `type` = None,
        explanation = "tagalog how are you".some,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(Seq(i1, i2))

    }

    "return i18nString seq in getStringByCriteria - explanation partial" in {
      val criteria = I18nStringCriteria(
        id = None,
        key = None,
        explanation = CriteriaField("explanation", "tagalog", MatchTypes.Partial).some,
        locale = None,
        platform = CriteriaField("", "web").some)

      val orderingSet = OrderingSet(Ordering("locale", Ordering.DESC), Ordering("key", Ordering.ASC)).some
      val resp = dao.getStringByCriteria(criteria, orderingSet, 2.some, None)

      val i2 = I18nString(
        id = 8,
        key = "how_are_you",
        text = "kumusta?",
        locale = "fil-PH",
        platform = "web",
        `type` = None,
        explanation = "tagalog how are you".some,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(Seq(i2))

    }

    "return count in countStringByCriteria - no filter" in {
      val criteria = I18nStringCriteria()

      val resp = dao.countStringByCriteria(criteria)

      resp mustBe Right(8)

    }

    "return count in getStringByCriteria - platform filter" in {
      val criteria = I18nStringCriteria(
        id = none,
        key = none,
        explanation = none,
        locale = none,
        platform = CriteriaField("", "web").some)

      val resp = dao.countStringByCriteria(criteria)

      resp mustBe Right(4)

    }

    "return i18nString seq in getI18nPairsByCriteria" in {
      val criteria = I18nStringCriteria(
        id = none,
        key = none,
        explanation = none,
        locale = CriteriaField("", "fil-PH").some,
        platform = CriteriaField("", "web").some)

      val resp = dao.getI18nPairsByCriteria(criteria)

      val i1 = I18nPair(
        key = "close",
        text = "isara")
      val i2 = I18nPair(
        key = "how_are_you",
        text = "kumusta?")

      resp mustBe Right(Seq(i1, i2))

    }
  }

  "I18nStringDao create" should {
    val dto1 = I18nStringToInsert(
      key = "hello",
      text = "hola",
      locale = "es",
      platform = "web",
      `type` = None,
      explanation = "Hello in Spanish".some,
      createdAt = now)

    "return newly created i18nString" in {

      val resp = dao.insertString(dto1)

      resp.map(_.id) mustBe Right(9)
      resp.map(_.key) mustBe Right(dto1.key)
      resp.map(_.text) mustBe Right(dto1.text)
      resp.map(_.locale) mustBe Right(dto1.locale)
      resp.map(_.platform) mustBe Right(dto1.platform)
      resp.map(_.explanation) mustBe Right(dto1.explanation)
      resp.map(_.createdAt) mustBe Right(dto1.createdAt)
    }

    "return another newly created i18nString" in {
      val dto = I18nStringToInsert(
        key = "hello",
        text = "hello",
        locale = "en",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now)

      val resp = dao.insertString(dto)

      resp.map(_.id) mustBe Right(10)
      resp.map(_.key) mustBe Right(dto.key)
      resp.map(_.text) mustBe Right(dto.text)
      resp.map(_.locale) mustBe Right(dto.locale)
      resp.map(_.platform) mustBe Right(dto.platform)
      resp.map(_.createdAt) mustBe Right(dto.createdAt)

    }

    "return exception when trying to insert same key, locale, platform" in {
      val dto = I18nStringToInsert(
        key = "hello",
        text = "hala",
        locale = "en",
        platform = "web",
        `type` = None,
        explanation = None,
        createdAt = now)

      val resp = dao.insertString(dto)

      resp mustBe ConstraintViolationError(s"Could not create i18n string ${dto.toSmartString}").asLeft[I18nString]

    }

  }

  "I18nString update " should {
    "successfully update 18nString if correct data (text)" in {
      val dto = I18nStringToUpdate(text = "sara!".some, updatedAt = now, lastUpdatedAt = now.some)

      val resp = dao.updateString(7, dto)

      val expected = I18nString(
        id = 7,
        key = "close",
        text = "sara!",
        platform = "web",
        locale = "fil-PH",
        `type` = None,
        explanation = None,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(expected.some)
    }
    "successfully update 18nString if correct data (all data)" in {
      val dto = I18nStringToUpdate(
        key = "bye".some,
        text = "tchuss!".some,
        platform = "ios".some,
        locale = "de".some,
        `type` = "chat_message".some,
        explanation = "Bye in german".some,
        updatedAt = now,
        lastUpdatedAt = now.some)

      val resp = dao.updateString(7, dto)

      val expected = I18nString(
        id = 7,
        key = "bye",
        text = "tchuss!",
        platform = "ios",
        locale = "de",
        `type` = "chat_message".some,
        explanation = "Bye in german".some,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(expected.some)
    }
    "fail on update 18nString when stale data" in {
      val dto = I18nStringToUpdate(
        key = "bye".some,
        text = "tchuss!".some,
        platform = "ios".some,
        locale = "de".some,
        updatedAt = now,
        lastUpdatedAt = LocalDateTime.now().some)

      val resp = dao.updateString(7, dto)

      resp mustBe Left(PreconditionFailed(s"Update failed. I18n String 7 has been modified by another process."))
    }
  }

  "I18n delete" should {
    "return success if record found and non stale" in {
      val resp = dao.deleteString(1, now.some)

      resp mustBe Right(1.some)
    }
    "return fail if record has been modified already" in {
      val resp = dao.deleteString(2, LocalDateTime.now().some)

      resp mustBe Left(PreconditionFailed(s"Delete failed. I18n String 2 has been modified by another process."))
    }
    "return fail if record is not found" in {
      val resp = dao.deleteString(0, LocalDateTime.now().some)

      resp mustBe Right(None)
    }
  }

  "I18n batch insert" should {
    val dto1 = I18nStringToInsert(
      key = "thanks",
      text = "salamat",
      locale = "fil",
      platform = "android",
      `type` = "chat_message".some,
      explanation = "Thanks in Filipino".some,
      createdAt = now)

    val dto2 = I18nStringToInsert(
      key = "bye",
      text = "paalam",
      locale = "fil",
      platform = "android",
      `type` = None,
      explanation = "Bye in Filipino".some,
      createdAt = now)

    "return newly created i18nString" in {

      val dtos = NonEmptyList(dto1, List(dto2))
      val insertResp = dao.bulkInsertString(dtos)

      insertResp mustBe Right(2)

      val criteria = I18nStringCriteria(
        id = none,
        key = none,
        explanation = none,
        locale = CriteriaField("", "fil").some,
        platform = CriteriaField("", "android").some)

      val orderingSet = OrderingSet(Ordering("key", Ordering.ASC)).some
      val resp = dao.getStringByCriteria(criteria, orderingSet, none, none)

      val expected1 = I18nString(
        id = 13,
        key = "bye",
        text = "paalam",
        platform = "android",
        locale = "fil",
        `type` = None,
        explanation = "Bye in Filipino".some,
        createdAt = now,
        updatedAt = now.some)
      val expected2 = I18nString(
        id = 12,
        key = "thanks",
        text = "salamat",
        locale = "fil",
        platform = "android",
        `type` = "chat_message".some,
        explanation = "Thanks in Filipino".some,
        createdAt = now,
        updatedAt = now.some)

      resp mustBe Right(Seq(expected1, expected2))
    }

    "return error when compound key already exists" in {

      val dtos = NonEmptyList(dto1, Nil)
      val insertResp = dao.bulkInsertString(dtos)

      insertResp mustBe Left(ConstraintViolationError("Could not create i18n string, SQLException encountered"))

    }

  }

}
