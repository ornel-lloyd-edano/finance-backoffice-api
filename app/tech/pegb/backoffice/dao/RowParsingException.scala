package tech.pegb.backoffice.dao

class RowParsingException(msg: String) extends Exception(msg + " [A column may have been removed, renamed or column type might have changed]") {

}
