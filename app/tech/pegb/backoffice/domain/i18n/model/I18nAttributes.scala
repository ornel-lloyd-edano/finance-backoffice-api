package tech.pegb.backoffice.domain.i18n.model

import tech.pegb.backoffice.util.Implicits._

object I18nAttributes {

  case class I18nKey(underlying: String) {
    assert(underlying.hasSomething, "empty i18n key")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid i18n key [${underlying}]")
  }

  case class I18nText(underlying: String) {
    assert(underlying.hasSomething, "empty i18n text")
  }

  case class I18nLocale(underlying: String) {
    assert(underlying.hasSomething, "empty i18n locale")
  }

  case class I18nPlatform(underlying: String) {
    assert(underlying.hasSomething, "empty i18n platform")
  }
}
