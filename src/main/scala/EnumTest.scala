import org.squeryl._
import adapters._
import dsl._
import internals.Utils
import java.sql.ResultSet

import PolicyTypeViewItem.PolicyTypeViewItem

import scala.language.implicitConversions

object PolicyTypeViewItem extends Enumeration {
  type PolicyTypeViewItem = Value

  val Fixed = Value(1, "FP")
  val Variable = Value(2, "VP")
}

case class PolicyViewItem1(id: Long = 0,
                           name: String,
                           policyType: String //PolicyTypeViewItem,
) extends KeyedEntity[Long]

case class PolicyViewItem2(
    id: Long = 0,
    name: String,
    policyType: PolicyTypeViewItem
) extends KeyedEntity[Long] {

  def this() = this(
    id = 0L,
    name = "",
    policyType = PolicyTypeViewItem.Fixed
  )
}

trait MyTypes extends PrimitiveTypeMode {
  // ====== Read/Write Enums as string ======

  def enumValueTEF[A >: Enumeration#Value <: Enumeration#Value](
      ev: Enumeration#Value) = {
    new JdbcMapper[String, A] with TypedExpressionFactory[A, TEnumValue[A]] {
      val enu = Utils.enumerationForValue(ev)
      def extractNativeJdbcValue(rs: ResultSet, i: Int) = rs.getString(i)
      def defaultColumnLength: Int = stringTEF.defaultColumnLength
      def sample: A = ev
      def convertToJdbc(v: A) = v.toString
      def convertFromJdbc(s: String) = {
        enu.values
          .find(_.toString == s)
          .getOrElse(PrimitiveTypeSupport.DummyEnum.DummyEnumerationValue)
        // JDBC has no concept of null value for primitive types (ex. Int)
        // at this level, we mimic this JDBC flaw (the Option / None based on
        // jdbc.wasNull will get sorted out by optionEnumValueTEF)
      }
    }
  }

  override implicit def enumValueToTE[
      A >: Enumeration#Value <: Enumeration#Value](
      e: A): TypedExpression[A, TEnumValue[A]] =
    enumValueTEF[A](e).create(e)
}

object MyTypes extends MyTypes
import MyTypes._

object Schema1 extends Schema {
  val items1 = table[PolicyViewItem1]("items")
}

object Schema2 extends Schema {
  val items2 = table[PolicyViewItem2]("items")
}

object Main extends App {
  Class.forName("org.h2.Driver")
  SessionFactory.concreteFactory = Some(
    () =>
      Session.create(
        java.sql.DriverManager.getConnection("jdbc:h2:mem:test;MODE=MySQL"),
        new H2Adapter))

  import Schema1._
  import Schema2._

  transaction {
    Schema1.printDdl
    Schema1.create

    val i1 = PolicyViewItem1(name = "name1", policyType = "FP")
    items1.insert(i1)

    val i2 =
      PolicyViewItem2(name = "name2", policyType = PolicyTypeViewItem.Fixed)
    items2.insert(i2) // Should save same record as above

    val found1 = items1.allRows.toList
    println("Found1 " + found1)
    val found2 = items2.allRows.toList
    println("Found2 " + found2)
  }
}
