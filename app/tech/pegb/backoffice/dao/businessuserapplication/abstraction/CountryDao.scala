package tech.pegb.backoffice.dao.businessuserapplication.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.businessuserapplication.dto.{CountryToUpsert}
import tech.pegb.backoffice.dao.businessuserapplication.entity.Country
import tech.pegb.backoffice.dao.businessuserapplication.sql.CountrySqlDao

@ImplementedBy(classOf[CountrySqlDao])
trait CountryDao extends Dao {

  def getCountries: DaoResponse[Seq[Country]]

  def upsertCountry(dto: Seq[CountryToUpsert]): DaoResponse[Unit]

}
