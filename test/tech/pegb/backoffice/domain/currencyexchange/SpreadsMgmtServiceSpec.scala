package tech.pegb.backoffice.domain.currencyexchange

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.{Currency, UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.DaoError.{EntityNotFoundError, GenericDbError}
import tech.pegb.backoffice.dao.currencyexchange.abstraction.{CurrencyExchangeDao, SpreadsDao}
import tech.pegb.backoffice.dao.currencyexchange.dto.{SpreadCriteria ⇒ DaoSpreadCriteria}
import tech.pegb.backoffice.dao.currencyexchange.entity
import tech.pegb.backoffice.dao.currencyexchange.entity.Spread
import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.currencyexchange.abstraction.{CurrencyExchangeManagement, SpreadsManagement}
import tech.pegb.backoffice.domain.currencyexchange.dto.{SpreadCriteria, SpreadToCreate}
import tech.pegb.backoffice.domain.currencyexchange.model.CurrencyExchange
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.mapping.dao.domain.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.currencyexchange.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class SpreadsMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val currencyExchangeManagement = stub[CurrencyExchangeManagement]
  val currencyExchangeDao = stub[CurrencyExchangeDao]
  val spreadsDao = stub[SpreadsDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[CurrencyExchangeManagement].to(currencyExchangeManagement),
      bind[CurrencyExchangeDao].to(currencyExchangeDao),
      bind[SpreadsDao].to(spreadsDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val config = inject[AppConfig]
  val spreadsMgmtService = inject[SpreadsManagement]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  "SpreadsMgmtService" should {
    "return Future[Some[Spread]] in getSpread(id: UUID) if id was found" in {

      val mockSpreadId = UUID.randomUUID()
      val mockCurrencyExchangeId = UUID.randomUUID()
      val mockSpreadEntity = Spread.empty.copy(uuid = mockSpreadId, currencyExchangeUuid = mockCurrencyExchangeId)
      (spreadsDao.getSpread _).when(mockSpreadId).returns(Right(Option(mockSpreadEntity)))

      val mockCurrencyExchange = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId)
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(mockCurrencyExchange)))

      val result = spreadsMgmtService.getSpread(mockSpreadId)

      whenReady(result)(result ⇒ {
        result.right.get.id mustBe mockSpreadId
        result.right.get.currencyExchange mustBe mockCurrencyExchange
      })
    }

    "return Future[Left[ServiceError.NotFound]] in getSpread(id: UUID) if id was not found" in {
      val mockSpreadId = UUID.randomUUID()
      (spreadsDao.getSpread _).when(mockSpreadId).returns(Right(None))

      val result = spreadsMgmtService.getSpread(mockSpreadId)

      whenReady(result)(result ⇒ {
        result.left.get mustBe ServiceError.notFoundError(s"spread with id [$mockSpreadId] not found", UUID.randomUUID().toOption)
      })
    }

    "return Future[Left[ServiceError.NotFound]] in getSpread(id: UUID) if id was found but isDeleted is true" in {
      val mockSpreadId = UUID.randomUUID()

      val mockSpreadEntity = Spread.empty.copy(deletedAt = Option(LocalDateTime.now))
      (spreadsDao.getSpread _).when(mockSpreadId).returns(Right(Option(mockSpreadEntity)))

      val result = spreadsMgmtService.getSpread(mockSpreadId)

      whenReady(result)(result ⇒ {
        result.left.get mustBe ServiceError.notFoundError(s"spread with id [$mockSpreadId] not found", UUID.randomUUID().toOption)
      })
    }

    "return Future[Left[ServiceError.CorruptRead]] in getSpread(id: UUID) if id was found but model validation failed on transaction type: currency_exchange" in {
      val mockSpreadId = UUID.randomUUID()
      val mockCurrencyExchangeId = UUID.randomUUID()
      val mockSpreadEntity = Spread.empty.copy(uuid = mockSpreadId, currencyExchangeUuid = mockCurrencyExchangeId, transactionType = "currency_exchange", channel = Option("some channel"), recipientInstitution = Option("some institution"))

      (spreadsDao.getSpread _).when(mockSpreadId).returns(Right(Option(mockSpreadEntity)))

      val mockCurrencyExchange = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId)
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(mockCurrencyExchange)))

      val result = spreadsMgmtService.getSpread(mockSpreadId)

      whenReady(result)(result ⇒ {
        result.left.get mustBe ServiceError.validationError(
          s"Corrupt data for spread id [$mockSpreadId]. " +
            s"Reason: channel or recipient institution cannot " +
            s"have value if transaction type is currency_exchange",
          UUID.randomUUID().toOption)
      })
    }

    "return Future[Left[ServiceError.CorruptRead]] in getSpread(id: UUID) if id was found but model validation failed on buy and/or sell cost (must be 0 to 1)" in {
      val mockSpreadId = UUID.randomUUID()
      val mockCurrencyExchangeId = UUID.randomUUID()
      val mockSpreadEntity = Spread.empty.copy(uuid = mockSpreadId, currencyExchangeUuid = mockCurrencyExchangeId, transactionType = "international_remittance", channel = Option("some channel"), recipientInstitution = Option("some institution"), spread = BigDecimal(-2))

      (spreadsDao.getSpread _).when(mockSpreadId).returns(Right(Option(mockSpreadEntity)))

      val mockCurrencyExchange = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId)
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(mockCurrencyExchange)))

      val result = spreadsMgmtService.getSpread(mockSpreadId)

      whenReady(result)(result ⇒ {
        result.left.get mustBe ServiceError.validationError(
          s"Corrupt data for spread id [$mockSpreadId]. " +
            s"Reason: spread must be from 0 to 1",
          UUID.randomUUID().toOption)
      })
    }

    "return Future[Left[ServiceError.CorruptRead]] in getSpread(id: UUID) if id was found but model validation failed on buy and/or sell cost (must not be greater than 6 decimal places)" in {
      val mockSpreadId = UUID.randomUUID()
      val mockCurrencyExchangeId = UUID.randomUUID()
      val mockSpreadEntity = Spread.empty.copy(uuid = mockSpreadId, currencyExchangeUuid = mockCurrencyExchangeId, transactionType = "international_remittance", channel = Option("some channel"), recipientInstitution = Option("some institution"), spread = BigDecimal(0.12345611))

      (spreadsDao.getSpread _).when(mockSpreadId).returns(Right(Option(mockSpreadEntity)))

      val mockCurrencyExchange = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId)
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(mockCurrencyExchange)))

      val result = spreadsMgmtService.getSpread(mockSpreadId)

      whenReady(result)(result ⇒ {
        result.left.get mustBe ServiceError.validationError(
          s"Corrupt data for spread id [$mockSpreadId]. " +
            s"Reason: spread value right significant digits must not be over 6",
          UUID.randomUUID().toOption)
      })
    }

    "return Future[Right[Spread]] in getSpread(id: UUID) if id was found and passed model validation" in {
      val mockSpreadId = UUID.randomUUID()
      val mockCurrencyExchangeId = UUID.randomUUID()
      val mockSpreadEntity = Spread.empty.copy(uuid = mockSpreadId, currencyExchangeUuid = mockCurrencyExchangeId, transactionType = "international_remittance", channel = Option("some channel"), recipientInstitution = Option("some institution"), spread = BigDecimal(0.1234))

      (spreadsDao.getSpread _).when(mockSpreadId).returns(Right(Option(mockSpreadEntity)))

      val mockCurrencyExchange = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId)
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId, *)
        .returns(Future.successful(Right(mockCurrencyExchange)))

      val result = spreadsMgmtService.getSpread(mockSpreadId)

      whenReady(result)(result ⇒ {
        result.isRight mustBe true
      })
    }

    "return Future[Seq[Spread]] in getSpreadsByCriteria for all Spread which isDeleted = false" in {
      val criteria = SpreadCriteria(isDeleted = Option(false))

      val mockCurrencyExchangeId1 = UUID.randomUUID()
      val mockCurrencyExchangeId2 = UUID.randomUUID()
      val mockCurrencyExchangeId3 = UUID.randomUUID()
      val mockCurrencyExchangeId4 = UUID.randomUUID()

      val mockSpreadEntities = Seq(
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId1),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId2),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId3),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId4, deletedAt = Option(LocalDateTime.now)))
      (spreadsDao.getSpreadsByCriteria _).when(criteria.asDao, None, None, None).returns(Right(mockSpreadEntities.filter(_.deletedAt.isEmpty)))

      val mockCurrencyExchange1 = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId1)
      val mockCurrencyExchange2 = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId2)
      val mockCurrencyExchange3 = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId3)
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId1, *)
        .returns(Future.successful(Right(mockCurrencyExchange1)))
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId2, *)
        .returns(Future.successful(Right(mockCurrencyExchange2)))
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId3, *)
        .returns(Future.successful(Right(mockCurrencyExchange3)))

      val result = spreadsMgmtService.getSpreadByCriteria(criteria, Nil, None, None)

      whenReady(result)(result ⇒ {
        result.right.get.size mustBe 3
        result.right.get.find(_.isDeleted == true) mustBe None
      })
    }

    "return Future[Seq[Spread]] in getSpreadsByCriteria for all Spread meeting the given exact match criteria" in {
      val criteria = SpreadCriteria(
        transactionType = Option(TransactionType("currency_exchange")),
        currencyCode = Option(Currency.getInstance("USD")),
        isDeleted = Option(false))

      val mockCurrencyExchangeId1 = UUID.randomUUID()
      val mockCurrencyExchangeId2 = UUID.randomUUID()
      val mockCurrencyExchangeId3 = UUID.randomUUID()
      val mockCurrencyExchangeId4 = UUID.randomUUID()

      val mockSpreadEntities = Seq(
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId1, transactionType = "international_remittance"),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId2, transactionType = "international_remittance"),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId3, transactionType = "currency_exchange", deletedAt = None),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId4, transactionType = "currency_exchange", deletedAt = Option(LocalDateTime.now)))

      (spreadsDao.getSpreadsByCriteria _)
        .when(criteria.asDao, None, None, None)
        .returns(Right(mockSpreadEntities.filter(_.deletedAt.isEmpty)
          .filter(_.transactionType == "currency_exchange")))

      val mockCurrencyExchange3 = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId3)
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId3, *)
        .returns(Future.successful(Right(mockCurrencyExchange3)))

      val result = spreadsMgmtService.getSpreadByCriteria(criteria, Nil, None, None)

      import tech.pegb.backoffice.mapping.dao.domain.currencyexchange.Implicits._

      whenReady(result)(result ⇒ {
        result.right.get.size mustBe 1
        result.right.get mustBe Seq(mockSpreadEntities(2).asDomain(mockCurrencyExchange3))
      })
    }

    "return Future[Seq[Spread]] in getSpreadsByCriteria paginated by limit and offset" in {
      val criteria = SpreadCriteria(isDeleted = Option(false))

      val mockCurrencyExchangeId1 = UUID.randomUUID()
      val mockCurrencyExchangeId2 = UUID.randomUUID()
      val mockCurrencyExchangeId3 = UUID.randomUUID()
      val mockCurrencyExchangeId4 = UUID.randomUUID()
      val mockCurrencyExchangeId5 = UUID.randomUUID()
      val mockCurrencyExchangeId6 = UUID.randomUUID()

      val mockSpreadEntities = Seq(
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId1),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId2),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId3),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId4),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId5),
        Spread.empty.copy(currencyExchangeUuid = mockCurrencyExchangeId6))
      (spreadsDao.getSpreadsByCriteria _).when(criteria.asDao, None, Option(2), Option(2))
        .returns(Right(mockSpreadEntities.tail.tail.take(2))) //simulates limit = 2, offset = 2

      val mockCurrencyExchange1 = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId3)
      val mockCurrencyExchange2 = CurrencyExchange.empty.copy(id = mockCurrencyExchangeId4)

      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId3, *)
        .returns(Future.successful(Right(mockCurrencyExchange1)))
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(mockCurrencyExchangeId4, *)
        .returns(Future.successful(Right(mockCurrencyExchange2)))

      val result = spreadsMgmtService.getSpreadByCriteria(criteria, Nil, Option(2), Option(2))

      whenReady(result)(result ⇒ {
        result.right.get.size mustBe 2
        result.right.get.map(_.currencyExchange.id).toSet mustBe Set(mockCurrencyExchangeId3, mockCurrencyExchangeId4)
      })
    }

    "return Future[Int] in countSpreadsByCriteria" in {
      val criteria = SpreadCriteria(isDeleted = Option(false))
      (spreadsDao.countSpreadsByCriteria _).when(criteria.asDao).returns(Right(4))

      val result = spreadsMgmtService.countSpreadByCriteria(criteria)
      whenReady(result)(result ⇒ {
        result mustBe Right(4)
      })

    }

    "return Future[Right[Spread]] on successful createSpread " in {
      val fxUUID = UUID.randomUUID()

      val spreadUUID = UUID.randomUUID()
      val daoSpread = Spread.empty.copy(
        uuid = spreadUUID,
        transactionType = "international_remittance",
        channel = Some("bank"),
        recipientInstitution = Some("Mashreq"),
        spread = BigDecimal(0.01),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser")

      val currencyExchangeDomainModel = CurrencyExchange(
        id = fxUUID,
        currency = Currency.getInstance("USD"),
        baseCurrency = Currency.getInstance("KES"),
        rate = BigDecimal(99.9800),
        provider = "Currency Cloud",
        balance = BigDecimal(17526.5),
        dailyAmount = None,
        status = "active",
        lastUpdated = None)

      val domainSpreadToCreateDto = SpreadToCreate(
        currencyExchangeId = fxUUID,
        transactionType = TransactionType("international_remittance"),
        channel = Some(Channel("bank")),
        institution = Some("Mashreq"),
        spread = BigDecimal(0.01),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser")

      val spreadsCriteria = DaoSpreadCriteria(
        currencyExchangeId = Some(CriteriaField("currency_exchange_id", fxUUID.toString)),
        transactionType = Some("international_remittance"),
        channel = Some("bank"),
        recipientInstitution = Some("Mashreq"),
        isDeletedAtNotNull = Some(false))

      (spreadsDao.getSpreadsByCriteria _).when(spreadsCriteria, None, None, None)
        .returns(Right(Nil))
      (spreadsDao.createSpread _).when(domainSpreadToCreateDto.asDao)
        .returns(Right(daoSpread))
      (currencyExchangeManagement.getCurrencyExchangeByUUID(_: UUID)(_: UUID)).when(fxUUID, *)
        .returns(Future.successful(Right(currencyExchangeDomainModel)))

      implicit val mockRequestId = UUID.randomUUID()

      val result = spreadsMgmtService.createSpread(domainSpreadToCreateDto)(mockRequestId)

      val expectedSpread = daoSpread.asDomain(currencyExchangeDomainModel)

      whenReady(result)(actual ⇒ {
        actual mustBe Right(expectedSpread)
      })
    }

    "return Future[Left[DaoError] on createSpread when currencyExchange does not exist" in {

      val fxUUID = UUID.randomUUID()

      val spreadUUID = UUID.randomUUID()

      val domainSpreadToCreateDto = SpreadToCreate(
        currencyExchangeId = fxUUID,
        transactionType = TransactionType("international_remittance"),
        channel = Some(Channel("bank")),
        institution = Some("Mashreq"),
        spread = BigDecimal(0.01),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser")

      val spreadsCriteria = DaoSpreadCriteria(
        currencyExchangeId = Some(CriteriaField("currency_exchange_id", fxUUID.toString)),
        transactionType = Some("international_remittance"),
        channel = Some("bank"),
        recipientInstitution = Some("Mashreq"),
        isDeletedAtNotNull = Some(false))

      val expectedError = EntityNotFoundError(s"Couldn't find newly created account $spreadUUID")

      (spreadsDao.getSpreadsByCriteria _).when(spreadsCriteria, None, None, None)
        .returns(Right(Nil))
      (spreadsDao.createSpread _).when(domainSpreadToCreateDto.asDao)
        .returns(Left(expectedError))

      implicit val mockRequestId = UUID.randomUUID()

      val result = spreadsMgmtService.createSpread(domainSpreadToCreateDto)(mockRequestId)

      whenReady(result)(actual ⇒ {
        actual mustBe expectedError.asDomainError.toLeft
      })
    }

    "return Future[Left[DaoError]] when dao.createSpread throw DaoError in createSpread" in {
      val fxUUID = UUID.randomUUID()

      val usdId = 1
      val kesId = 2
      val cloudProviderId = 1
      val usdEscrowUUID = UUID.randomUUID()
      val baseEscrowId = 2
      val baseEscrowUUID = UUID.randomUUID()
      val currencyExchangeDaoModel = entity.CurrencyExchange(
        id = 1,
        uuid = fxUUID.toString,
        currencyId = usdId,
        currencyCode = "USD",
        baseCurrencyId = kesId,
        baseCurrency = "KES",
        rate = BigDecimal(99.9800),
        providerId = cloudProviderId,
        provider = "Currency Cloud",
        targetCurrencyAccountId = usdId,
        targetCurrencyAccountUuid = usdEscrowUUID.toString,
        baseCurrencyAccountId = baseEscrowId,
        baseCurrencyAccountUuid = baseEscrowUUID.toString,
        balance = BigDecimal(17526.5),
        status = "active",
        updatedAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("pegbuser"))

      val domainSpreadToCreateDto = SpreadToCreate(
        currencyExchangeId = fxUUID,
        transactionType = TransactionType("international_remittance"),
        channel = Some(Channel("bank")),
        institution = Some("Mashreq"),
        spread = BigDecimal(0.01),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser")

      val spreadsCriteria = DaoSpreadCriteria(
        currencyExchangeId = Some(CriteriaField("currency_exchange_id", fxUUID.toString)),
        transactionType = Some("international_remittance"),
        channel = Some("bank"),
        recipientInstitution = Some("Mashreq"),
        isDeletedAtNotNull = Some(false))

      val expectedError = GenericDbError("Error saving in database")

      (spreadsDao.getSpreadsByCriteria _).when(spreadsCriteria, None, None, None)
        .returns(Right(Nil))
      (spreadsDao.createSpread _).when(domainSpreadToCreateDto.asDao)
        .returns(Left(expectedError))

      implicit val mockRequestId = UUID.randomUUID()

      val result = spreadsMgmtService.createSpread(domainSpreadToCreateDto)(mockRequestId)

      whenReady(result)(actual ⇒ {
        actual mustBe expectedError.asDomainError.toLeft
      })
    }

    "return Future[Left[DuplicateError]] when spread with same values exists in currency_exchange" in {
      val fxUUID = UUID.randomUUID()

      val spreadUUID = UUID.randomUUID()
      val daoSpread = Spread.empty.copy(
        uuid = spreadUUID,
        transactionType = "international_remittance",
        channel = Some("bank"),
        recipientInstitution = Some("Mashreq"),
        spread = BigDecimal(0.01))

      val currencyExchangeDomainModel = CurrencyExchange(
        id = fxUUID,
        currency = Currency.getInstance("USD"),
        baseCurrency = Currency.getInstance("KES"),
        rate = BigDecimal(99.9800),
        provider = "Currency Cloud",
        balance = BigDecimal(17526.5),
        dailyAmount = None,
        status = "active",
        lastUpdated = None)

      val domainSpreadToCreateDto = SpreadToCreate(
        currencyExchangeId = fxUUID,
        transactionType = TransactionType("international_remittance"),
        channel = Some(Channel("bank")),
        institution = Some("Mashreq"),
        spread = BigDecimal(0.01),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser")

      val spreadsCriteria = DaoSpreadCriteria(
        currencyExchangeId = Some(CriteriaField("currency_exchange_id", fxUUID.toString)),
        transactionType = Some("international_remittance"),
        channel = Some("bank"),
        recipientInstitution = Some("Mashreq"),
        isDeletedAtNotNull = Some(false))

      val mockDuplicateSpread = Spread.empty.copy(
        currencyExchangeUuid = fxUUID,
        transactionType = "internation_remittance",
        channel = Some("bank"),
        recipientInstitution = Some("Mashreq"))

      (spreadsDao.getSpreadsByCriteria _).when(spreadsCriteria, None, None, None)
        .returns(Right(List(mockDuplicateSpread)))

      implicit val mockRequestId = UUID.randomUUID()

      val result = spreadsMgmtService.createSpread(domainSpreadToCreateDto)(mockRequestId)

      val expectedSpread = daoSpread.asDomain(currencyExchangeDomainModel)

      whenReady(result)(actual ⇒ {
        actual mustBe Left(ServiceError.duplicateError(s"Spread: (transaction_type: international_remittance, channel: Some(Channel(bank)), institution: Some(Mashreq)) already exists for currency_exchange: $fxUUID", mockRequestId.toOption))
      })
    }

  }

}
