package tech.pegb.backoffice.dao.types.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser._
import anorm._
import javax.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.dao.types.entity.{Description, DescriptionToUpsert, DescriptionType}
import tech.pegb.backoffice.dao.{Dao, DaoError, SqlDao}
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserTiers

class TypesSqlDao @Inject() (override val dbApi: DBApi)
  extends TypesDao with SqlDao {

  //TODO refactor later
  val DescTypesTableName = "description_types"
  val DescTableName = "descriptions"
  val cId = "id"
  val cType = "type"
  val cCreatedAt = "created_at"
  val cCreatedBy = "created_by"
  val cUpdatedAt = "updated_at"
  val cUpdatedBy = "updated_by"
  val cName = "name"
  val cDescription = "description"
  val cDescTypesTableFK = "value_id"

  def parseRowToEntity(row: Row): (DescriptionType, Description) = {
    val descType = DescriptionType(
      id = row[Int](s"$DescTypesTableName.$cId"),
      `type` = row[String](s"$DescTypesTableName.$cType"),
      createdAt = row[LocalDateTime](s"$DescTypesTableName.$cCreatedAt"),
      createdBy = row[String](s"$DescTypesTableName.$cCreatedBy"),
      updatedAt = row[Option[LocalDateTime]](s"$DescTypesTableName.$cUpdatedAt"),
      updatedBy = row[Option[String]](s"$DescTypesTableName.$cUpdatedBy"))
    val desc = Description(
      id = row[Int](s"$DescTableName.$cId"),
      name = row[String](s"$DescTableName.$cName"),
      description = row[Option[String]](s"$DescTableName.$cDescription"))
    (descType, desc)
  }

  def parseRowToDescriptionType(row: Row): DescriptionType = {

    DescriptionType(
      id = row[Int](s"$DescTypesTableName.$cId"),
      `type` = row[String](s"$DescTypesTableName.$cType"),
      createdAt = row[LocalDateTime](s"$DescTypesTableName.$cCreatedAt"),
      createdBy = row[String](s"$DescTypesTableName.$cCreatedBy"),
      updatedAt = row[Option[LocalDateTime]](s"$DescTypesTableName.$cUpdatedAt"),
      updatedBy = row[Option[String]](s"$DescTypesTableName.$cUpdatedBy"))
  }

  override def fetchAllTypes: Dao.DaoResponse[Map[DescriptionType, Seq[Description]]] = {
    withConnection({ implicit connection ⇒
      //TODO pls extract descriptions, type and name to vals in singleton helper
      val x = SQL(
        s"""SELECT dt.id, dt.type,
           |dt.created_at, dt.created_by, dt.updated_at, dt.updated_by,
           |d.id, d.name, d.description
           |FROM descriptions d
           |INNER JOIN description_types dt ON d.value_id = dt.id;""".stripMargin)

      x.as(x.defaultParser.*)
        .map(parseRowToEntity(_)).groupBy(_._1).map {
          case (descType: DescriptionType, list) ⇒
            descType -> list.map(_._2)
        }

    }, s"Couldn't fetch types")
  }

  override def fetchCustomType(kind: String): Dao.DaoResponse[List[(Int, String, Option[String])]] = {
    withConnection({ implicit connection ⇒
      //TODO pls extract description_types to val in singleton helper
      SQL(
        s"""SELECT d.id as id, d.name as name, d.description as description FROM descriptions d
           |INNER JOIN description_types dt ON d.value_id = dt.id
           |WHERE dt.type = {kind};""".stripMargin)
        .on("kind" → kind)
        .as((int("id") ~ str("name") ~ str("description").? map flatten).*)
    }, s"Couldn't fetch values of type $kind")
  }

  //TODO instead of using string literals here, use variables from app.conf and in app.conf set the string literals
  override def getApplicationStages: R = {
    fetchCustomType("application_stages")
  }

  override def getChannels: R = {
    fetchCustomType("channels")
    //Seq("ANDROID_APP", "IOS_APP", "ATM")
  }

  override def getCompanies: R = {
    fetchCustomType("companies")
  }

  override def getCustomerTypes: R = {
    fetchCustomType("customer_types")
  }

  override def getCustomerTiers: R = {
    //fetchNames("customer_tiers")
    fetchCustomType("customer_tiers")
  }

  override def getBusinessUserTiers: R = {
    fetchCustomType(BusinessUserTiers.toString)
  }

  override def getCustomerSegments: R = {
    fetchCustomType("customer_tiers")
    //Seq("new", "occasional_active", "gold", "platinum")
  }

  override def getCustomerSubscriptions: R = {
    fetchCustomType("customer_subscriptions")
    //Seq("standard", "plus", "full")
  }

  override def getDocumentTypes: R = {
    fetchCustomType("document_types")
    //Seq("selfie", "national_id_front", "national_id_back", "liveness")
  }

  override def getEmployers: R = {
    // fetchNames("employers", "employer_name")
    fetchCustomType("employers")
  }

  override def getImageTypes: R = {
    fetchCustomType("image_types")
  }

  override def getIndividualUserTypes: R = {
    fetchCustomType("individual_user_types")
  }

  override def getInstruments: R = {
    fetchCustomType("instruments")
  }

  override def getTransactionTypes: R = {
    fetchCustomType("transaction_types")
  }

  override def getOccupations: R = {
    // fetchNames("occupations", "occupation_name")
    fetchCustomType("occupations")
  }

  override def getNationalities: R = {
    // fetchNames("nationalities", "nationality_name")
    fetchCustomType("nationalities")
  }

  override def getLimitTypes: R = {
    fetchCustomType("limit_profile_types")
  }

  override def getFeeTypes: R = {
    fetchCustomType("fee_profile_types")
  }

  override def getFeeCalculationMethod: R = {
    fetchCustomType("fee_calculation_methods")
  }

  override def getTimeIntervalTypes: R = {
    fetchCustomType("intervals")
  }

  override def getPlatformTypes: R = {
    fetchCustomType("platforms")
  }

  override def getLocales: R = {
    fetchCustomType("locales")
  }

  override def getTaskStatuses: R = {
    fetchCustomType("task_statuses")
  }

  override def getCommunicationChannels: R = {
    fetchCustomType("communication_channels")
  }

  override def getFeeMethods: R = {
    fetchCustomType("fee_methods")
  }

  override def getBusinessTypes: R = {
    fetchCustomType("business_types")
  }

  override def getBusinessCategories: R = {
    fetchCustomType("business_categories")
  }

  def insertDescription(existingKind: String, newValue: String, explanation: Option[String]): DaoResponse[Int] = {
    withConnection({ implicit connection ⇒
      SQL(
        s"""
           |INSERT INTO $DescTableName
           |($cName, $cDescription, $cDescTypesTableFK)
           |VALUES
           |({$cName}, {$cDescription}, (SELECT $cId FROM $DescTypesTableName WHERE $cType = {$cType}))
           |;
         """.stripMargin)
        .on(
          cName → newValue,
          cDescription → explanation,
          cType → existingKind).executeInsert(SqlParser.scalar[Int].single)
    }, s"Couldn't insert descriptions")
  }

  private def commonGetIdByType(`type`: String)(implicit conn: Connection): Option[Int] = {
    SQL(s"SELECT $cId FROM $DescTypesTableName WHERE $cType = {$cType}").on(cType → `type`)
      .as(SqlParser.scalar[Int].singleOpt)
  }

  def getDescTypeAndDescriptionsById(id: Long): Dao.DaoResponse[Option[(DescriptionType, Seq[Description])]] = {
    withConnection({ implicit connection ⇒
      findTypeAndDescriptionsInternalById(id.toInt).headOption
    }, s"Couldn't fetch description type")
  }

  private def commonChildInsert(
    valueId: Int,
    newValues: Seq[DescriptionToUpsert])(implicit connection: Connection) = {

    SQL(s"DELETE FROM $DescTableName WHERE $cDescTypesTableFK = {$cDescTypesTableFK}")
      .on(cDescTypesTableFK → valueId).execute()

    newValues.map { newValue ⇒

      SQL(s"""
             |INSERT INTO $DescTableName
             |($cName, $cDescription, $cDescTypesTableFK)
             |VALUES
             |${
        s"('${newValue.name}', ${newValue.description.map(d ⇒ s"'$d'").getOrElse("null")}, '$valueId' )"
      };""".stripMargin).execute()
    }

  }

  def insertType(newKind: String, createdAt: LocalDateTime,
    createdBy: String, newValues: Seq[DescriptionToUpsert]): DaoResponse[(DescriptionType, Seq[Description])] = {
    if (newValues.nonEmpty) {
      withTransaction({ implicit connection ⇒
        internalInsertType(newKind, createdAt, createdBy, newValues)
      }, s"Couldn't insert description_types")
    } else {
      Left(DaoError.PreconditionFailed(s"Inserting $DescTypesTableName is not allowed if no corresponding $DescTableName rows to insert"))
    }
  }

  private def internalInsertType(newKind: String, createdAt: LocalDateTime,
    createdBy: String, newValues: Seq[DescriptionToUpsert])(implicit conn: Connection) = {
    val rawInsertQuery =
      s"""
         |INSERT INTO $DescTypesTableName
         |($cType, $cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy)
         |VALUES
         |({$cType}, {$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy} );
  """.stripMargin

    val id = SQL(rawInsertQuery).on(
      cType → newKind,
      cCreatedAt → createdAt,
      cCreatedBy → createdBy,
      cUpdatedAt → createdAt, //not nullable in db and same as created at on insertion
      cUpdatedBy → createdBy).executeInsert(SqlParser.scalar[Int].single) //not nullable in db and same as created at on insertion
    commonChildInsert(id, newValues)
    findTypeAndDescriptionsInternalById(id).head
  }

  def bulkUpsert(
    existingKind: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime],
    newValues: Seq[DescriptionToUpsert],
    disableOptimisticLockCheck: Boolean = false): Dao.DaoResponse[(DescriptionType, Seq[Description])] = {
    if (newValues.nonEmpty) {
      withTransaction({ implicit connection ⇒
        val maybeExistingType = findDescriptionTypeByTypeName(existingKind)
        if (maybeExistingType.isEmpty && (disableOptimisticLockCheck || maybeExistingType.map(_.updatedAt == lastUpdatedAt).contains(true))) {
          internalInsertType(existingKind, updatedAt, updatedBy, newValues)
        } else if (maybeExistingType.isDefined && (disableOptimisticLockCheck || maybeExistingType.get.updatedAt == lastUpdatedAt)) {

          commonChildInsert(maybeExistingType.get.id, newValues)

          SQL(s"UPDATE $DescTypesTableName SET $cUpdatedAt = $cUpdatedAt, $cUpdatedBy = $cUpdatedBy " +
            s"WHERE $cType = {$cType}")
            .on(cUpdatedAt → updatedAt, cUpdatedBy → updatedBy, cType → existingKind)
            .executeUpdate()

          val id = commonGetIdByType(existingKind).get
          findTypeAndDescriptionsInternalById(id).head

        } else {
          throw new IllegalStateException(s"Upsert failed. type $existingKind has been modified by another process.")
        }
      }, s"Couldn't upsert description_types",
        {
          case err: IllegalStateException ⇒
            preconditionFailed(s"Inserting $DescTypesTableName is not allowed if no corresponding $DescTableName rows to insert")
          case err: Exception ⇒
            logger.error("error encountered in [bulkUpsert]", err)
            genericDbError(s"error encountered while performing bulkUpser of types")
        })
    } else {
      Left(genericDbError(s"Inserting $DescTypesTableName is not allowed if no corresponding $DescTableName rows to insert"))
    }
  }

  private def findTypeAndDescriptionsInternalById(id: Int)(implicit connection: Connection): Map[DescriptionType, Seq[Description]] = {

    val sqlQuery = SQL(
      s"""SELECT dt.id, dt.type,
         |dt.created_at, dt.created_by, dt.updated_at, dt.updated_by,
         |d.id, d.name, d.description
         |FROM descriptions d
         |INNER JOIN description_types dt ON d.value_id = dt.id where d.value_id=$id;""".stripMargin)

    sqlQuery.as(sqlQuery.defaultParser.*)
      .map(parseRowToEntity(_)).groupBy(_._1).map {
        case (descType, list) ⇒
          descType -> list.map(_._2)
      }
  }

  def findDescriptionTypeByTypeName(name: String)(implicit connection: Connection): Option[DescriptionType] = {

    val sqlQuery = SQL(
      s"""SELECT * FROM description_types where `type`='$name';""".stripMargin)

    sqlQuery.as(sqlQuery.defaultParser.singleOpt)
      .map(parseRowToDescriptionType(_))

  }

}
