package tech.pegb.backoffice.domain.graphql

import java.time.{LocalDate, LocalDateTime}
import java.util.{Currency, UUID}

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import sangria.ast
import sangria.macros.derive.{Interfaces, deriveObjectType}
import sangria.marshalling.DateSupport
import sangria.schema._
import sangria.validation.ValueCoercionViolation
import tech.pegb.backoffice.dao.application.sql.WalletApplicationSqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.{UserSqlDao}
import tech.pegb.backoffice.dao.model.GroupOperationTypes.{Date, DateHour, Day, Hour, IsNotNull, Minute, Month, Year}
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.domain.Identifiable
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerSegment, CustomerStatus, CustomerSubscription, CustomerTier, Msisdn, MsisdnLike}
import tech.pegb.backoffice.domain.model.TransactionAggregatation
import tech.pegb.backoffice.domain.transaction.model.{Transaction, TransactionStatus}
import tech.pegb.backoffice.domain.model.CustomerAggregation

import scala.util.{Failure, Success, Try}

/**
 * Defines a GraphQL schema for the current project
 */
object SchemaDefinition {

  case object DateCoercionViolation extends ValueCoercionViolation("Date value expected")
  case object UUIDCoercionViolation extends ValueCoercionViolation("UUID value expected")
  case object CurrencyCoercionViolation extends ValueCoercionViolation("Valid currency code expected")
  case object TrxStatusCoercionViolation extends ValueCoercionViolation("Valid trx status expected")
  case object MsisdnCoercionViolation extends ValueCoercionViolation("Valid msisdn expected")
  case object TierCoercionViolation extends ValueCoercionViolation("Valid customer tier expected")
  case object SegmentCoercionViolation extends ValueCoercionViolation("Valid customer segment expected")
  case object SubscriptionCoercionViolation extends ValueCoercionViolation("Valid customer subscription level expected")
  case object CustomerStatusCoercionViolation extends ValueCoercionViolation("Valid customer status expected")

  def parseDate(s: String) = Try(new DateTime(s, DateTimeZone.UTC)) match {
    case Success(date) ⇒ Right(date)
    case Failure(_) ⇒ Left(DateCoercionViolation)
  }

  def parseDateTime(s: String) = Try(LocalDateTime.parse(s)) match {
    case Success(date) ⇒ Right(date)
    case Failure(_) ⇒ Left(DateCoercionViolation)
  }

  def parseLocalDate(s: String) = Try(LocalDate.parse(s)) match {
    case Success(date) ⇒ Right(date)
    case Failure(_) ⇒ Left(DateCoercionViolation)
  }

  def parseUUID(s: String) = Try(UUID.fromString(s)) match {
    case Success(uuid) ⇒ Right(uuid)
    case Failure(_) ⇒ Left(UUIDCoercionViolation)
  }

  def parseCurrency(s: String) = Try(Currency.getInstance(s)) match {
    case Success(uuid) ⇒ Right(uuid)
    case Failure(_) ⇒ Left(UUIDCoercionViolation)
  }

  def parseTransactionStatus(s: String) = Try(TransactionStatus(s)) match {
    case Success(status) ⇒ Right(status)
    case Failure(_) ⇒ Left(TrxStatusCoercionViolation)
  }

  def parseMsisdnLike(s: String) = Try(MsisdnLike(s)) match {
    case Success(status) ⇒ Right(status)
    case Failure(_) ⇒ Left(MsisdnCoercionViolation)
  }

  def parseMsisdn(s: String) = Try(Msisdn(s)) match {
    case Success(status) ⇒ Right(status)
    case Failure(_) ⇒ Left(MsisdnCoercionViolation)
  }

  def parseCustomerTier(s: String) = Try(CustomerTier(s)) match {
    case Success(status) ⇒ Right(status)
    case Failure(_) ⇒ Left(TierCoercionViolation)
  }

  def parseCustomerSegment(s: String) = Try(CustomerSegment(s)) match {
    case Success(status) ⇒ Right(status)
    case Failure(_) ⇒ Left(SegmentCoercionViolation)
  }

  def parseCustomerSubscription(s: String) = Try(CustomerSubscription(s)) match {
    case Success(status) ⇒ Right(status)
    case Failure(_) ⇒ Left(SubscriptionCoercionViolation)
  }

  def parseCustomerStatus(s: String) = Try(CustomerStatus(s)) match {
    case Success(status) ⇒ Right(status)
    case Failure(_) ⇒ Left(CustomerStatusCoercionViolation)
  }

  implicit val UUIDType = ScalarType[UUID](
    "UUID",
    coerceOutput = (d, _) ⇒ d.toString,
    coerceUserInput = {
      case s: String ⇒ parseUUID(s)
      case _ ⇒ Left(UUIDCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseUUID(s)
      case _ ⇒ Left(UUIDCoercionViolation)
    })

  implicit val DateTimeType = ScalarType[DateTime](
    "DateTime",
    coerceOutput = (d, caps) ⇒
      if (caps.contains(DateSupport)) d.toDate
      else ISODateTimeFormat.dateTime().print(d),
    coerceUserInput = {
      case s: String ⇒ parseDate(s)
      case _ ⇒ Left(DateCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseDate(s)
      case _ ⇒ Left(DateCoercionViolation)
    })

  implicit val CurrencyType = ScalarType[Currency](
    "Currency",
    coerceOutput = (d, _) ⇒ d.getCurrencyCode,
    coerceUserInput = {
      case s: String ⇒ parseCurrency(s)
      case _ ⇒ Left(CurrencyCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseCurrency(s)
      case _ ⇒ Left(CurrencyCoercionViolation)
    })

  implicit val LocalDateTimeType = ScalarType[LocalDateTime](
    "LocalDateTime",
    coerceOutput = (d, caps) ⇒
      if (caps.contains(DateSupport)) d.toString
      else d.toString(),
    coerceUserInput = {
      case s: String ⇒ parseDateTime(s)
      case _ ⇒ Left(DateCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseDateTime(s)
      case _ ⇒ Left(DateCoercionViolation)
    })

  implicit val LocalDateType = ScalarType[LocalDate](
    "LocalDate",
    coerceOutput = (d, caps) ⇒
      if (caps.contains(DateSupport)) d.toString
      else d.toString(),
    coerceUserInput = {
      case s: String ⇒ parseLocalDate(s)
      case _ ⇒ Left(DateCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseLocalDate(s)
      case _ ⇒ Left(DateCoercionViolation)
    })

  //  val transactions = Fetcher.caching(
  //    (ctx: TransactionRepo, ids: Seq[String]) ⇒
  //      Future.successful(
  //        ids.map(id ⇒ ctx.transaction(id).map(t => t))
  //      )
  //  )(HasId(_.map(_.uniqueId)))

  val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified",

    fields[Unit, Identifiable](
      Field("id", StringType, resolve = _.value.uniqueId)))

  implicit val TransactionDirectionEnum = EnumType(
    "TransactionDirection",
    Some("A list of valid transaction statuses"),
    List(
      EnumValue(
        "credit",
        value = TransactionDirection.credit),
      EnumValue(
        "debit",
        value = TransactionDirection.debit)))

  //  implicit val AccountType = deriveObjectType[Unit, Account](
  //    Interfaces(IdentifiableType))

  implicit val TransactionStatusType = ScalarType[TransactionStatus](
    "TransactionStatus",
    coerceOutput = (d, _) ⇒ d.underlying,
    coerceUserInput = {
      case s: String ⇒ parseTransactionStatus(s)
      case _ ⇒ Left(TrxStatusCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseTransactionStatus(s)
      case _ ⇒ Left(TrxStatusCoercionViolation)
    })

  implicit val MsidnType = ScalarType[Msisdn](
    "Msisdn",
    coerceOutput = (d, _) ⇒ d.underlying,
    coerceUserInput = {
      case s: String ⇒ parseMsisdn(s)
      case _ ⇒ Left(MsisdnCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseMsisdn(s)
      case _ ⇒ Left(MsisdnCoercionViolation)
    })

  implicit val MsidnLikeType = ScalarType[MsisdnLike](
    "MsisdnLike",
    coerceOutput = (d, _) ⇒ d.underlying,
    coerceUserInput = {
      case s: String ⇒ parseMsisdnLike(s)
      case _ ⇒ Left(MsisdnCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseMsisdnLike(s)
      case _ ⇒ Left(MsisdnCoercionViolation)
    })

  implicit val TierType = ScalarType[CustomerTier](
    "CustomerTier",
    coerceOutput = (d, _) ⇒ d.underlying,
    coerceUserInput = {
      case s: String ⇒ parseCustomerTier(s)
      case _ ⇒ Left(TierCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseCustomerTier(s)
      case _ ⇒ Left(TierCoercionViolation)
    })

  implicit val SegmentType = ScalarType[CustomerSegment](
    "CustomerSegment",
    coerceOutput = (d, _) ⇒ d.underlying,
    coerceUserInput = {
      case s: String ⇒ parseCustomerSegment(s)
      case _ ⇒ Left(SegmentCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseCustomerSegment(s)
      case _ ⇒ Left(SegmentCoercionViolation)
    })

  implicit val SubscriptionType = ScalarType[CustomerSubscription](
    "CustomerSubscription",
    coerceOutput = (d, _) ⇒ d.underlying,
    coerceUserInput = {
      case s: String ⇒ parseCustomerSubscription(s)
      case _ ⇒ Left(SubscriptionCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseCustomerSubscription(s)
      case _ ⇒ Left(SubscriptionCoercionViolation)
    })

  implicit val CustomerStatusType = ScalarType[CustomerStatus](
    "CustomerStatus",
    coerceOutput = (d, _) ⇒ d.underlying,
    coerceUserInput = {
      case s: String ⇒ parseCustomerStatus(s)
      case _ ⇒ Left(CustomerStatusCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) ⇒ parseCustomerStatus(s)
      case _ ⇒ Left(CustomerStatusCoercionViolation)
    })

  val TransactionType = deriveObjectType[Unit, Transaction](
    Interfaces(IdentifiableType))

  val TransactionAggregateType = deriveObjectType[Unit, TransactionAggregatation](
    Interfaces(IdentifiableType))

  val CustomerAggregationType = deriveObjectType[Unit, CustomerAggregation](
    Interfaces(IdentifiableType))

  val Id = Argument("id", StringType)

  val StatusArg = Argument("status", OptionInputType(StringType),
    description = "If omitted, returns all transactions. If provided, returns tranasactions only in that status.")

  val TransactionTypeArg = Argument("type", OptionInputType(StringType),
    description = "If omitted, returns all transactions. If provided, returns tranasactions only of the type.")

  val TransactionDirectionArg = Argument("direction", OptionInputType(StringType),
    description = "If omitted, returns all transactions. If provided, returns tranasactions only on that direction.")

  val LimitArg = Argument("limit", OptionInputType(IntType))
  val OffsetArg = Argument("offset", OptionInputType(IntType))

  val FromDateTimeArg = Argument("date_time_from", OptionInputType(StringType),
    description = "If omitted set will not have a lower bound in time")

  val ToDateTimeArg = Argument("date_time_to", OptionInputType(StringType),
    description = "If omitted set will not have a upper bound in time")

  val FromDateArg = Argument("date_from", OptionInputType(StringType),
    description = "If omitted set will not have a lower bound in time")

  val ToDateArg = Argument("date_to", OptionInputType(StringType),
    description = "If omitted set will not have a upper bound in time")

  val CustomerIdArg = Argument("customer_id", OptionInputType(UUIDType),
    description = "Filter by owner of the transaction")

  val AccountIdArg = Argument("account_id", OptionInputType(UUIDType),
    description = "Filter by account of owner of the transaction")

  val ChannelArg = Argument("channel", OptionInputType(StringType),
    description = "If omitted, returns all transactions. If provided, returns tranasactions only on that channel.")

  val CurrencyArg = Argument("currency", OptionInputType(StringType),
    description = "If omitted, returns all transactions. If provided, returns tranasactions only for this currency.")

  val GroupingByArg = Argument("groupBy", ListInputType(StringType),
    description = "Specifies how to group aggregated data")

  val MsisdnArg = Argument("msisdn", OptionInputType(MsidnLikeType),
    description = "A valid phone number used to filter customers")

  val TierArg = Argument("tier", OptionInputType(TierType),
    description = "A valid tier for customer filtering")

  val SegmentArg = Argument("segment", OptionInputType(SegmentType),
    description = "A valid segment for customer filtering")

  val SubscriptionArg = Argument("subscription", OptionInputType(SubscriptionType),
    description = "A valid subscription level for customer filtering")

  val CustomerStatusArg = Argument("customerStatus", OptionInputType(CustomerStatusType),
    description = "A valid status for customer filtering")

  val GenderArg = Argument("gender", OptionInputType(StringType), "Any of these values: male, female")

  val DocumentTypeArg = Argument("documentType", OptionInputType(StringType), "Any of valid document type")

  val BirthPlaceArg = Argument("birthPlace", OptionInputType(StringType), "Any of valid city or country")

  val NationalityArg = Argument("nationality", OptionInputType(StringType), "Any valid country")

  val OnlyFloatAccountsArg = Argument("onlyFloatAccounts", OptionInputType(BooleanType), "Include only data related to " +
    "accounts included in system setting 'float_account_numbers'")

  val AccountTypeArg = Argument("accountType", OptionInputType(StringType), "Only search for accounts of this type. In the" +
    " case of transactions this is only going to be matched against the primary account")

  val buildTrxArgsFormContext = (ctx: Context[GraphQLRepo, Unit]) ⇒
    TransactionQueryArgs(
      limit = ctx.arg(LimitArg),
      offset = ctx.arg(OffsetArg),
      status = ctx.arg(StatusArg),
      `type` = ctx.arg(TransactionTypeArg),
      direction = ctx.arg(TransactionDirectionArg),
      from_date = ctx.arg(FromDateArg),
      to_date = ctx.arg(ToDateArg),
      channel = ctx.arg(ChannelArg),
      customerId = ctx.arg(CustomerIdArg),
      accountId = ctx.arg(AccountIdArg),
      currency = ctx.arg(CurrencyArg),
      onlyFloatAccounts = ctx.arg(OnlyFloatAccountsArg).getOrElse(false),
      accountType = ctx.arg(AccountTypeArg))

  val buildCustomerArgsFormContext = (ctx: Context[GraphQLRepo, Unit]) ⇒
    CustomerQueryArgs(
      limit = ctx.arg(LimitArg),
      offset = ctx.arg(OffsetArg),
      msisdn = ctx.arg(MsisdnArg),
      tier = ctx.arg(TierArg),
      segment = ctx.arg(SegmentArg),
      subscription = ctx.arg(SubscriptionArg),
      status = ctx.arg(CustomerStatusArg),
      gender = ctx.arg(GenderArg),
      documentType = ctx.arg(DocumentTypeArg),
      birthPlace = ctx.arg(BirthPlaceArg),
      nationality = ctx.arg(NationalityArg),
      dateFrom = ctx.arg(FromDateArg),
      dateTo = ctx.arg(ToDateArg))

  val buildGroupByFormContext = (ctx: Context[GraphQLRepo, Unit]) ⇒
    ctx.arg(GroupingByArg).map({ value ⇒
      {
        value.toString match {
          case "day" ⇒
            GroupingField("created_at", Option(Day), projectionAlias = Option("day"))
          case "month" ⇒
            GroupingField("created_at", Option(Month), projectionAlias = Option("month"))
          case "year" ⇒
            GroupingField("created_at", Option(Year), projectionAlias = Option("year"))
          case "hour" ⇒
            GroupingField("created_at", Option(Hour), projectionAlias = Option("hour"))
          case "minute" ⇒
            GroupingField("created_at", Option(Minute), projectionAlias = Option("minute"))
          case "date" ⇒
            GroupingField("created_at", Option(Date), projectionAlias = Option("date"))
          case "date-hour" ⇒
            GroupingField("created_at", Option(DateHour), projectionAlias = Option("date"))
          case "updatedDate" ⇒
            GroupingField("updated_at", Option(Date), projectionAlias = Option("date"))
          case "updatedMonth" ⇒
            GroupingField("updated_at", Option(Month), projectionAlias = Option("month"))
          case "currency" ⇒
            GroupingField(
              TransactionSqlDao.caCurrency,
              tableAlias = Option(CurrencySqlDao.TableAlias),
              columnAlias = Option(CurrencySqlDao.cName))
          case "activated" ⇒
            GroupingField(
              UserSqlDao.activatedAt,
              Option(IsNotNull),
              projectionAlias = Option("is_activated"))
          case "active" ⇒
            GroupingField(
              TransactionSqlDao.cUniqueId,
              Option(IsNotNull),
              tableAlias = Option(TransactionSqlDao.TableAlias),
              projectionAlias = Option("is_active"))
          case "applicationStatus" ⇒
            GroupingField(
              TransactionSqlDao.cStatus,
              tableAlias = Option(WalletApplicationSqlDao.TableAlias),
              projectionAlias = Option("applicationStatus"))
          case "exchangedCurrency" ⇒
            GroupingField(
              TransactionSqlDao.caExCurrency,
              tableAlias = Option(s"${TransactionSqlDao.TableAlias}1"))
          case other ⇒
            GroupingField(other)
        }
      }
    }): Seq[GroupingField]

  val Query = ObjectType(
    "Query", fields[GraphQLRepo, Unit](
      Field("transaction", OptionType(TransactionType),
        description = Some("Returns a transaction with specific `id`."),
        arguments = Id :: Nil,
        resolve = ctx ⇒ ctx.ctx.transaction(ctx arg Id)),
      Field("transactions", ListType(TransactionType),
        description = Some("Returns a list of transactions by query."),
        arguments = StatusArg :: LimitArg :: OffsetArg :: TransactionTypeArg :: CurrencyArg
          :: TransactionDirectionArg :: FromDateArg :: ToDateArg :: ChannelArg
          :: CustomerIdArg :: AccountIdArg :: OnlyFloatAccountsArg :: AccountTypeArg :: Nil,
        resolve = ctx ⇒ ctx.ctx.transactions(buildTrxArgsFormContext(ctx))),
      Field("sum", OptionType(BigDecimalType),
        description = Some("Returns a sum over a field for a set of parameters."),
        arguments = StatusArg :: LimitArg :: OffsetArg :: TransactionTypeArg :: CurrencyArg
          :: TransactionDirectionArg :: FromDateArg :: ToDateArg :: ChannelArg
          :: CustomerIdArg :: AccountIdArg :: OnlyFloatAccountsArg :: AccountTypeArg :: Nil,
        resolve = ctx ⇒ ctx.ctx.sum(buildTrxArgsFormContext(ctx))),
      Field("count", OptionType(IntType),
        description = Some("Returns a count of transactions for a set of parameters."),
        arguments = StatusArg :: LimitArg :: OffsetArg :: TransactionTypeArg :: CurrencyArg
          :: TransactionDirectionArg :: FromDateArg :: ToDateArg :: ChannelArg
          :: CustomerIdArg :: AccountIdArg :: OnlyFloatAccountsArg :: AccountTypeArg :: Nil,
        resolve = ctx ⇒ ctx.ctx.count(buildTrxArgsFormContext(ctx))),
      Field("transactionAggregations", OptionType(ListType(TransactionAggregateType)),
        description = Some("Aggregations over a variety of resources."),
        arguments = StatusArg :: LimitArg :: OffsetArg :: TransactionTypeArg :: CurrencyArg
          :: TransactionDirectionArg :: FromDateArg :: ToDateArg :: ChannelArg
          :: CustomerIdArg :: AccountIdArg :: GroupingByArg :: OnlyFloatAccountsArg :: AccountTypeArg :: Nil,
        resolve = ctx ⇒ ctx.ctx.aggregateTransactions(buildTrxArgsFormContext(ctx), buildGroupByFormContext(ctx))),
      Field("customerAggregations", OptionType(ListType(CustomerAggregationType)),
        description = Some("Returns a list of individual users by query."),
        arguments = CustomerStatusArg :: StatusArg :: LimitArg :: OffsetArg :: TransactionTypeArg
          :: TransactionDirectionArg :: FromDateArg :: ToDateArg :: ChannelArg
          :: CurrencyArg :: AccountTypeArg :: CustomerIdArg :: AccountIdArg :: MsisdnArg :: TierArg :: SegmentArg :: SubscriptionArg
          :: GenderArg :: DocumentTypeArg :: BirthPlaceArg :: NationalityArg :: GroupingByArg :: OnlyFloatAccountsArg :: Nil,
        resolve = ctx ⇒ ctx.ctx.aggregateCustomers(buildCustomerArgsFormContext(ctx), buildTrxArgsFormContext(ctx),
          buildGroupByFormContext(ctx)))))

  val Value = Schema(Query)
}
