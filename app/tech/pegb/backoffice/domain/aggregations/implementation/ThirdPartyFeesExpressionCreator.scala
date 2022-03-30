package tech.pegb.backoffice.domain.aggregations.implementation

import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.dao.aggregations.dto.Entity
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.fee.abstraction.ThirdPartyFeeProfileDao
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionGroupDao
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.domain.{BaseService, ErrorCodes}
import tech.pegb.backoffice.domain.aggregations.abstraction.ThirdPartyFeesCalculationExpressionCreator
import tech.pegb.backoffice.domain.aggregations.dto.{FeeRange, TransactionGrouping}
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.mapping.dao.domain.aggregation.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.aggregation.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.Future

@Singleton
class ThirdPartyFeesExpressionCreator @Inject() (
    executionContexts: WithExecutionContexts,
    txnGroupingDao: TransactionGroupDao,
    tpFeeProfileDao: ThirdPartyFeeProfileDao) extends ThirdPartyFeesCalculationExpressionCreator with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  private def getFlatFeeExpression(feeAmount: BigDecimal, alias: Option[String]): String = {
    s"${feeAmount.setScale(2, BigDecimal.RoundingMode.HALF_EVEN).toString()}${alias.map(a ⇒ s" as $a").getOrElse("")}"
  }

  private def getFlatPercentageExpression(
    entity: Entity,
    amountColumn: String,
    feeRatio: BigDecimal,
    alias: Option[String]): String = {
    s"${entity.alias.getOrElse(entity.name)}.$amountColumn * ${feeRatio.setScale(4, BigDecimal.RoundingMode.HALF_EVEN)}${alias.map(a ⇒ s" as $a").getOrElse("")}"
  }

  private def getStaircaseFlatFeeExpression(
    entity: Entity,
    amountColumn: String,
    feeAmountRanges: Seq[FeeRange],
    alias: Option[String]): String = {

    val rangesExpression = feeAmountRanges.headOption.map(range ⇒
      (range.min, range.max) match {
        case (Some(min), Some(max)) ⇒
          s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn >= ${min.toString()} AND ${entity.alias.getOrElse(entity.name)}.$amountColumn <= ${max.toString()} THEN " +
            s"${range.value.setScale(2, BigDecimal.RoundingMode.HALF_EVEN).toString()}"
        case (None, Some(max)) ⇒
          s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn <= ${max.toString()} THEN " +
            s"${range.value.setScale(2, BigDecimal.RoundingMode.HALF_EVEN).toString()}"
        case (Some(min), None) ⇒
          s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn >= ${min.toString()} THEN " +
            s"${range.value.setScale(2, BigDecimal.RoundingMode.HALF_EVEN).toString()}"
        case _ ⇒ ""
      }).getOrElse("").concat(

      feeAmountRanges.tail.map(range ⇒
        (range.min, range.max) match {
          case (Some(min), Some(max)) ⇒
            Some(s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn > ${min.toString()} AND ${entity.alias.getOrElse(entity.name)}.$amountColumn <= ${max.toString()} THEN " +
              s"${range.value.setScale(2, BigDecimal.RoundingMode.HALF_EVEN).toString()}")
          case (None, Some(max)) ⇒
            Some(s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn <= ${max.toString()} THEN " +
              s"${range.value.setScale(2, BigDecimal.RoundingMode.HALF_EVEN).toString()}")
          case (Some(min), None) ⇒
            Some(s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn > ${min.toString()} THEN " +
              s"${range.value.setScale(2, BigDecimal.RoundingMode.HALF_EVEN).toString()}")
          case _ ⇒ None
        }).flatten.mkString(System.lineSeparator(), System.lineSeparator(), ""))

    if (feeAmountRanges.isEmpty) {
      s"0.0 as $alias"
    } else {
      s"""
         |CASE
         |${rangesExpression}
         |ELSE 0.0
         |END${alias.map(a ⇒ s" as $a").getOrElse("")}
     """.stripMargin.trim
    }
  }

  private def getStaircasePercentageFeeExpression(
    entity: Entity,
    amountColumn: String,
    feePercentRanges: Seq[FeeRange],
    alias: Option[String]): String = {

    val rangesExpression = feePercentRanges.headOption.map(range ⇒
      (range.min, range.max) match {
        case (Some(min), Some(max)) ⇒
          s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn >= ${min.toString()} AND ${entity.alias.getOrElse(entity.name)}.$amountColumn <= ${max.toString()} THEN " +
            s"${entity.alias.getOrElse(entity.name)}.$amountColumn * ${range.value.setScale(4, BigDecimal.RoundingMode.HALF_EVEN)}"
        case (None, Some(max)) ⇒
          s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn <= ${max.toString()} THEN " +
            s"${entity.alias.getOrElse(entity.name)}.$amountColumn * ${range.value.setScale(4, BigDecimal.RoundingMode.HALF_EVEN)}"
        case (Some(min), None) ⇒
          s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn >= ${min.toString()} THEN " +
            s"${entity.alias.getOrElse(entity.name)}.$amountColumn * ${range.value.setScale(4, BigDecimal.RoundingMode.HALF_EVEN)}"
        case _ ⇒ ""
      }).getOrElse("").concat(
      feePercentRanges.tail.map(range ⇒
        (range.min, range.max) match {
          case (Some(min), Some(max)) ⇒
            Some(s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn > ${min.toString()} AND ${entity.alias.getOrElse(entity.name)}.$amountColumn <= ${max.toString()} THEN " +
              s"${entity.alias.getOrElse(entity.name)}.$amountColumn * ${range.value.setScale(4, BigDecimal.RoundingMode.HALF_EVEN)}")
          case (None, Some(max)) ⇒
            Some(s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn <= ${max.toString()} THEN " +
              s"${entity.alias.getOrElse(entity.name)}.$amountColumn * ${range.value.setScale(4, BigDecimal.RoundingMode.HALF_EVEN)}")
          case (Some(min), None) ⇒
            Some(s"WHEN ${entity.alias.getOrElse(entity.name)}.$amountColumn > ${min.toString()} THEN " +
              s"${entity.alias.getOrElse(entity.name)}.$amountColumn * ${range.value.setScale(4, BigDecimal.RoundingMode.HALF_EVEN)}")
          case _ ⇒ None
        }).flatten.mkString(System.lineSeparator(), System.lineSeparator(), ""))

    if (feePercentRanges.isEmpty) {
      s"0.0 as $alias"
    } else {
      s"""
         |CASE
         |${rangesExpression}
         |ELSE 0.0
         |END${alias.map(a ⇒ s" as $a").getOrElse("")}
     """.stripMargin.trim
    }
  }

  def getThirdPartyFeesCalculationExpressionById(
    entity: Entity,
    amountColumn: String,
    alias: Option[String],
    feeProfileId: String): Future[ServiceResponse[String]] = Future {

    tpFeeProfileDao.getThirdPartyFeeProfile(feeProfileId).fold(
      _.asDomainError.toLeft,
      {
        case Some(profile) if profile.calculationMethod === "flat_fee" ⇒
          getFlatFeeExpression(profile.feeAmount.getOrElse(BigDecimal("0.0")), alias).toRight

        case Some(profile) if profile.calculationMethod === "staircase_flat_fee" ⇒
          getStaircaseFlatFeeExpression(entity, amountColumn, profile.ranges.map(_.map(_.asDomain(isPercentage = false))).getOrElse(Nil), alias).toRight

        case Some(profile) if profile.calculationMethod === "flat_percentage" ⇒
          getFlatPercentageExpression(entity, amountColumn, profile.feeRatio.getOrElse(BigDecimal("0.0")), alias).toRight

        case Some(profile) if profile.calculationMethod === "staircase_flat_percentage" ⇒
          getStaircasePercentageFeeExpression(entity, amountColumn, profile.ranges.map(_.map(_.asDomain(isPercentage = true))).getOrElse(Nil), alias).toRight

        case Some(profile) ⇒
          unknownError("Unexpected calculation method").toLeft

        case None ⇒
          notFoundError("Third party fee profile not found.").toLeft
      })
  }

  private def internalGetThirdPartyFeesCalculationExpressionByCriteria(
    entity: Entity,
    amountColumn: String,
    alias: Option[String],
    providerName: String,
    currencyCode: Option[String],
    transactionType: Option[String]): ServiceResponse[String] =
    {
      val criteria = (providerName, currencyCode, transactionType).asDao
      tpFeeProfileDao.getThirdPartyFeeProfileByCriteria(criteria, None, None, None).fold(
        _.asDomainError.toLeft,
        _.headOption match {
          case Some(profile) if profile.calculationMethod === "flat_fee" ⇒
            getFlatFeeExpression(profile.feeAmount.getOrElse(BigDecimal("0.0")), alias).toRight

          case Some(profile) if profile.calculationMethod === "staircase_flat_fee" ⇒
            getStaircaseFlatFeeExpression(entity, amountColumn, profile.ranges.map(_.map(_.asDomain(isPercentage = false))).getOrElse(Nil), alias).toRight

          case Some(profile) if profile.calculationMethod === "flat_percentage" ⇒
            getFlatPercentageExpression(entity, amountColumn, profile.feeRatio.getOrElse(BigDecimal("0.0")), alias).toRight

          case Some(profile) if profile.calculationMethod === "staircase_flat_percentage" ⇒
            getStaircasePercentageFeeExpression(entity, amountColumn, profile.ranges.map(_.map(_.asDomain(isPercentage = true))).getOrElse(Nil), alias).toRight

          case Some(profile) ⇒
            unknownError("Unexpected calculation method").toLeft

          case None ⇒
            notFoundError("Third party fee profile not found.").toLeft
        })
    }

  def getThirdPartyFeesCalculationExpressionByCriteria(
    entity: Entity,
    amountColumn: String,
    alias: Option[String],
    providerName: String,
    currencyCode: Option[String],
    transactionType: Option[String]): Future[ServiceResponse[String]] =
    Future {
      internalGetThirdPartyFeesCalculationExpressionByCriteria(entity, amountColumn, alias, providerName, currencyCode, transactionType)
    }

  def getCompleteThirdPartyFeesCalculationNestedExpression(
    entity: Entity,
    amountColumn: String,
    alias: Option[String]): Future[ServiceResponse[String]] =
    Future {
      val criteria = TransactionCriteria.empty.asDao(isOtherPartyNotNull = Some(true))
      val grouping = TransactionGrouping(institution = true, transactionType = true, currencyCode = true).asDao2
      val baseGetExpressionCall = internalGetThirdPartyFeesCalculationExpressionByCriteria(entity, amountColumn, None, _, _, _)
      txnGroupingDao.getTransactionGroups(criteria, grouping)
        .fold(
          _.asDomainError.toLeft,
          results ⇒ {
            val nestedExpression = if (results.nonEmpty) results.foldLeft("CASE") {
              (expression, txnGroup) ⇒
                {
                  (txnGroup.provider, txnGroup.transactionType, txnGroup.currencyCode) match {
                    case (Some(provider), Some(txnType), Some(currCode)) ⇒
                      val outerExpression = expression + System.lineSeparator() + s"WHEN ${ProviderSqlDao.TableAlias}.${Provider.cName} = '$provider' " +
                        s"AND ${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cType} = '$txnType' " +
                        s"AND ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} = '$currCode' THEN"
                      baseGetExpressionCall.apply(provider, txnGroup.currencyCode, txnGroup.transactionType) match {
                        case Right(innerExpression) ⇒
                          outerExpression + System.lineSeparator() + innerExpression
                        case Left(error) if error.code == ErrorCodes.NotFound ⇒
                          outerExpression + System.lineSeparator() + "0.0"
                        case Left(error) ⇒ throw new SqlExpressionCreatorException(s"Error inside the nested CASE WHEN expression. Reason: ${error.code}")
                      }

                    case (Some(provider), Some(txnType), None) ⇒
                      val outerExpression = expression + System.lineSeparator() + s"WHEN ${ProviderSqlDao.TableAlias}.${Provider.cName} = '$provider' " +
                        s"AND ${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cType} = '$txnType' " +
                        s"AND ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} IS NULL THEN"
                      baseGetExpressionCall.apply(provider, txnGroup.currencyCode, txnGroup.transactionType) match {
                        case Right(innerExpression) ⇒
                          outerExpression + System.lineSeparator() + innerExpression
                        case Left(error) if error.code == ErrorCodes.NotFound ⇒
                          outerExpression + System.lineSeparator() + "0.0"
                        case Left(error) ⇒ throw new SqlExpressionCreatorException(s"Error inside the nested CASE WHEN expression. Reason: ${error}")
                      }

                    case (Some(provider), None, Some(currCode)) ⇒
                      val outerExpression = expression + System.lineSeparator() + s"WHEN ${ProviderSqlDao.TableAlias}.${Provider.cName} = '$provider' " +
                        s"AND ${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cType} IS NULL " +
                        s"AND ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} = '$currCode' THEN"
                      baseGetExpressionCall.apply(provider, txnGroup.currencyCode, txnGroup.transactionType) match {
                        case Right(innerExpression) ⇒
                          outerExpression + System.lineSeparator() + innerExpression
                        case Left(error) if error.code == ErrorCodes.NotFound ⇒
                          outerExpression + System.lineSeparator() + "0.0"
                        case Left(error) ⇒ throw new SqlExpressionCreatorException(s"Error inside the nested CASE WHEN expression. Reason: ${error}")
                      }

                    case (Some(provider), None, None) ⇒
                      val outerExpression = expression + System.lineSeparator() + s"WHEN ${ProviderSqlDao.TableAlias}.${Provider.cName} = '$provider' " +
                        s"AND ${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cType} IS NULL " +
                        s"AND ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} IS NULL THEN"
                      baseGetExpressionCall.apply(provider, txnGroup.currencyCode, txnGroup.transactionType) match {
                        case Right(innerExpression) ⇒
                          outerExpression + System.lineSeparator() + innerExpression
                        case Left(error) if error.code == ErrorCodes.NotFound ⇒
                          outerExpression + System.lineSeparator() + "0.0"
                        case Left(error) ⇒ throw new SqlExpressionCreatorException(s"Error inside the nested CASE WHEN expression. Reason: ${error}")
                      }

                    case _ ⇒ throw new IllegalStateException("other_party could not have been null. Fail to get third_party_fee_profile.")

                  }
                }
            } + System.lineSeparator() + s"ELSE 0.0 END${alias.map(a ⇒ s" as $a").getOrElse("")}"
            else s"0.0${alias.map(a ⇒ s" as $a").getOrElse("")}"

            logger.info("Expression created: " + System.lineSeparator() + nestedExpression)
            nestedExpression.toRight
          })
    }.recover {
      case error: Exception ⇒
        logger.error(error.getMessage, error)
        unknownError(error.getMessage).toLeft
    }
}
