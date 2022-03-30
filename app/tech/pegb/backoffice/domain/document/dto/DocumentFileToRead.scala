package tech.pegb.backoffice.domain.document.dto

case class DocumentFileToRead(
    id: String,
    content: Array[Byte],
    filename: Option[String] = None,
    fileType: Option[String] = None) {

}
