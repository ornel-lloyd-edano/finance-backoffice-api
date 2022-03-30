package tech.pegb.backoffice.util

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait WithSmartString {
  import RichCaseClass._
  override def toString: String = {
    this.toSmartString
  }
}

object RichCaseClass {

  implicit class ToRichCaseClass[T](arg: T)(implicit ev: TypeTag[T], ev2: ClassTag[T]) {
    def toSmartString = {
      val tpe = ev.tpe
      val allAccessors = tpe.decls.collect { case method: MethodSymbol if method.isCaseAccessor ⇒ method }

      val m = runtimeMirror(getClass.getClassLoader)
      val im = m.reflect(arg)
      val caseClassName = arg.getClass.getSimpleName
      allAccessors.map { sym ⇒
        val fldMirror = im.reflectField(sym)

        val value = fldMirror.get
        sym.name + " = " + value
      }.mkString(s"$caseClassName(", ", ", ")")
    }
  }

}
