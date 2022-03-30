package tech.pegb.backoffice.api

import tech.pegb.backoffice.util.Logging

object MimeType extends Logging {

  def fromFileExtension(arg: String): String = arg.toLowerCase match {
    case "jpeg" | "jpg" ⇒ "image/jpeg"
    case "tif" | "tiff" ⇒ "image/tiff"
    case "png" | "gif" | "bmp" ⇒ s"image/${arg.toLowerCase}"
    case "pdf" | "zip" | "rar" ⇒ s"application/${arg.toLowerCase}"
    case "doc" ⇒ "application/msword"
    case "docx" ⇒ "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    case "txt" | "text" ⇒ "text/plain;charset=UTF-8"
    case _ ⇒
      val defaultMimeType = "application/octet-stream"
      logger.warn(s"Unexpected file extension [$arg]. Defaulting to mime type [$defaultMimeType]")
      defaultMimeType
  }

}
