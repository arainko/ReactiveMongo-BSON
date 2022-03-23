package io.github

import reactivemongo.api.bson.Macros
import scala.util.Success
import reactivemongo.api.bson._

final case class Person(name: String, age: Option[Int])

object Person {

  implicit def optionAsNullWriter[A](
      implicit
      writer: BSONWriter[A]
    ): BSONWriter[Option[A]] =
    BSONWriter.from(_.map(writer.writeTry).getOrElse(Success(BSONNull)))

  implicit def optionAsNullReader[A](
      implicit
      reader: BSONReader[A]
    ): BSONReader[Option[A]] =
    BSONReader.from {
      case BSONNull => Success(None)
      case value    => reader.readTry(value).map(Some.apply)
    }

  implicit val writer: BSONDocumentWriter[Person] =
    Macros.writerOpts[Person, MacroOptions.Verbose]

  // implicit val reader: BSONDocumentReader[Person] =
  //   Macros.reader[
  //     Person
  //   ] // type mismatch; found : Option[Option[Int] required: Option[Int]
}

object Testing extends App {
  val cos = Person("name", None)
  Person.writer.writeTry(cos).map(BSONDocument.pretty).foreach(println)
  println("asd")
}
