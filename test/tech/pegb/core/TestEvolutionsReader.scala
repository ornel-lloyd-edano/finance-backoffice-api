package tech.pegb.core

import play.api.db.evolutions.{ClassLoaderEvolutionsReader, Evolution}

class TestEvolutionsReader extends ClassLoaderEvolutionsReader() {

  override def evolutions(db: String): Seq[Evolution] = {
    super.evolutions(db)
      .map(evolution ⇒ {
        val up = prepareSql(evolution.sql_up)
        val down = prepareSql(evolution.sql_down)
        evolution.copy(sql_up = up, sql_down = down)
      })
  }

  private val engineParam = " ENGINE=ndbcluster"
  private val unsignedParam = " UNSIGNED"
  private val onUpdateTimestampParam = " ON UPDATE CURRENT_TIMESTAMP"
  private val uniqueKeyRegex = """(UNIQUE )(KEY .*)(\(.*\))""".r
  private val hardcode1 = """"unchecked","Default""""
  private val hardcode2 = """"checked","with KYC""""
  private val unsupportedDropIndex = """ALTER TABLE `customer_limit_profile` DROP INDEX `unique_customer_limit_profile`;"""
  private val unsupportedDropIndex2 = """ALTER TABLE `agent_limit_profile` DROP INDEX `unique_agent_limit_profile`;"""

  private def prepareSql(sql: String): String = {
    val substitutions = Seq(
      "ALTER TABLE `permissions` DROP FOREIGN KEY `permissions_ibfk_1`;",
      "ALTER TABLE `permissions` DROP FOREIGN KEY `permissions_ibfk_3`;",
      "ALTER TABLE `permissions` DROP INDEX `unique_business_unit_scope`;",
      "ALTER TABLE `permissions` DROP INDEX `unique_role_scope`;")
      .zip(Seq(
        "ALTER TABLE `permissions` DROP CONSTRAINT IF EXISTS `permissions_ibfk_1`;",
        "ALTER TABLE `permissions` DROP CONSTRAINT IF EXISTS `permissions_ibfk_3`;",
        "DROP INDEX IF EXISTS `unique_business_unit_scope`;",
        "DROP INDEX IF EXISTS `unique_role_scope`;"))
    val prep1 = sql
      .replace(engineParam, "")
      .replace(unsignedParam, "")
      .replace(onUpdateTimestampParam, "")
      .replace(hardcode1, "'unchecked','Default'")
      .replace(hardcode2, "'checked','with KYC'")
      .replace(unsupportedDropIndex, "")
      .replace(unsupportedDropIndex2, "")
    //val prep2 = substitutions.foldLeft(prep1)((q, subPair) ⇒ q.replace(subPair._1, subPair._2))
    val prep3 = uniqueKeyRegex.replaceAllIn(prep1, "$1$3")
    prep3
  }
}
