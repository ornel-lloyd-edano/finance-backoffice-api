package tech.pegb.backoffice.api.i18n.dto

case class I18nPairToRead(
    key: String,
    text: String) {
  def toJsonKeyValString: String = {
    s"""
       |"$key":"$text"
       |""".stripMargin.replace(System.lineSeparator(), "")
  }
}
