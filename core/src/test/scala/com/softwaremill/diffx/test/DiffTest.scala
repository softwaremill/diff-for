package com.softwaremill.diffx.test

import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant
import java.util.UUID
import scala.collection.immutable.ListMap

class DiffTest extends AnyFreeSpec with Matchers {
  private val instant: Instant = Instant.now()
  val p1 = Person("p1", 22, instant)
  val p2 = Person("p2", 11, instant)

  "simple value" - {
    "diff" in {
      compare(1, 2) shouldBe DiffResultValue(1, 2)
    }
    "identity" in {
      compare(1, 1) shouldBe Identical(1)
    }
    "contravariant" in {
      compare(Some(1), Option(1)) shouldBe Identical(1)
    }
  }

  "options" - {
    "nullable" in {
      compare(Option.empty[Int], null: Option[Int]) shouldBe DiffResultValue(Option.empty[Int], null)
    }
  }

  "products" - {
    "identity" in {
      compare(p1, p1) shouldBe Identical(p1)
    }

    "nullable" in {
      compare(p1, null) shouldBe DiffResultValue(p1, null)
    }

    "diff" in {
      compare(p1, p2) shouldBe DiffResultObject(
        "Person",
        Map(
          "name" -> DiffResultString(List(DiffResultValue(p1.name, p2.name))),
          "age" -> DiffResultValue(p1.age, p2.age),
          "in" -> Identical(instant)
        )
      )
    }

    "difference between null and value" in {
      compare(p1.copy(name = null), p2) shouldBe DiffResultObject(
        "Person",
        Map(
          "name" -> DiffResultValue(null, p2.name),
          "age" -> DiffResultValue(p1.age, p2.age),
          "in" -> Identical(instant)
        )
      )
    }

    "two nulls should be equal" in {
      compare(p1.copy(name = null), p1.copy(name = null)) shouldBe an[Identical[_]]
    }

    "ignoring given fields" in {
      implicit val d: Diff[Person] = Derived[Diff[Person]].ignoreUnsafe("name").ignoreUnsafe("age")
      val p3 = p2.copy(in = Instant.now())
      compare(p1, p3) shouldBe DiffResultObject(
        "Person",
        Map(
          "name" -> Identical(p1.name),
          "age" -> Identical(p1.age),
          "in" -> DiffResultValue(p1.in, p3.in)
        )
      )
    }

    "nested products" in {
      val f1 = Family(p1, p2)
      val f2 = Family(p1, p1)
      compare(f1, f2) shouldBe DiffResultObject(
        "Family",
        Map(
          "first" -> Identical(p1),
          "second" -> DiffResultObject(
            "Person",
            Map(
              "name" -> DiffResultString(List(DiffResultValue(p2.name, p1.name))),
              "age" -> DiffResultValue(p2.age, p1.age),
              "in" -> Identical(instant)
            )
          )
        )
      )
    }

    "nested products ignoring nested fields" in {
      val f1 = Family(p1, p2)
      val f2 = Family(p1, p1)
      implicit val d: Diff[Family] = Derived[Diff[Family]].ignoreUnsafe("second", "name")
      compare(f1, f2) shouldBe DiffResultObject(
        "Family",
        Map(
          "first" -> Identical(p1),
          "second" -> DiffResultObject(
            "Person",
            Map(
              "name" -> Identical(p2.name),
              "age" -> DiffResultValue(p2.age, p1.age),
              "in" -> Identical(instant)
            )
          )
        )
      )
    }

    "nested products ignoring fields only in given path" in {
      val p1p = p1.copy(name = "other")
      val f1 = Family(p1, p2)
      val f2 = Family(p1p, p2.copy(name = "other"))
      implicit val d: Diff[Family] = Derived[Diff[Family]].ignoreUnsafe("second", "name")
      compare(f1, f2) shouldBe DiffResultObject(
        "Family",
        Map(
          "first" -> DiffResultObject(
            "Person",
            Map(
              "name" -> DiffResultString(List(DiffResultValue(p1.name, p1p.name))),
              "age" -> Identical(p1.age),
              "in" -> Identical(instant)
            )
          ),
          "second" -> Identical(p2)
        )
      )
    }

    "nested products ignoring nested products" in {
      val f1 = Family(p1, p2)
      val f2 = Family(p1, p1)
      implicit val d: Diff[Family] = Derived[Diff[Family]].ignoreUnsafe("second")
      compare(f1, f2) shouldBe Identical(f1)
    }

    "list of products" in {
      val o1 = Organization(List(p1, p2))
      val o2 = Organization(List(p1, p1, p1))
      compare(o1, o2) shouldBe DiffResultObject(
        "Organization",
        Map(
          "people" -> DiffResultObject(
            "List",
            Map(
              "0" -> Identical(p1),
              "1" -> DiffResultObject(
                "Person",
                Map(
                  "name" -> DiffResultString(List(DiffResultValue(p2.name, p1.name))),
                  "age" -> DiffResultValue(p2.age, p1.age),
                  "in" -> Identical(instant)
                )
              ),
              "2" -> DiffResultMissing(Person(p1.name, p1.age, instant))
            )
          )
        )
      )
    }
  }

  "coproducts" - {
    val right: Foo = Foo(
      Bar("asdf", 5),
      List(123, 1234),
      Some(Bar("asdf", 5))
    )
    val left: Foo = Foo(
      Bar("asdf", 66),
      List(1234),
      Some(right)
    )

    "sealed trait objects" - {
      "identity" in {
        compare[TsDirection](TsDirection.Outgoing, TsDirection.Outgoing) shouldBe an[Identical[_]]
      }
      "diff" in {
        compare[TsDirection](TsDirection.Outgoing, TsDirection.Incoming) shouldBe DiffResultValue(
          "com.softwaremill.diffx.test.TsDirection.Outgoing",
          "com.softwaremill.diffx.test.TsDirection.Incoming"
        )
      }
    }

    "identity" in {
      compare(left, left) shouldBe an[Identical[_]]
    }

    "nullable" in {
      compare[TsDirection](TsDirection.Outgoing, null: TsDirection) shouldBe DiffResultValue(TsDirection.Outgoing, null)
    }

    "diff" in {
      compare(left, right) shouldBe DiffResultObject(
        "Foo",
        Map(
          "bar" -> DiffResultObject("Bar", Map("s" -> Identical("asdf"), "i" -> DiffResultValue(66, 5))),
          "b" -> DiffResultObject("List", Map("0" -> DiffResultValue(1234, 123), "1" -> DiffResultMissing(1234))),
          "parent" -> DiffResultValue("com.softwaremill.diffx.test.Foo", "com.softwaremill.diffx.test.Bar")
        )
      )
    }
// TODO: uncomment once https://github.com/propensive/magnolia/issues/277 is resolved
//
//    "coproduct types with ignored fields" in {
//      sealed trait Base {
//        def id: Int
//        def name: String
//      }
//
//      final case class SubtypeOne(id: Int, name: String) extends Base
//      final case class SubtypeTwo(id: Int, name: String) extends Base
//      val left: Base = SubtypeOne(2, "one")
//      val right: Base = SubtypeOne(1, "one")
//      implicit val diff: Diff[Base] = Derived[Diff[Base]].ignoreUnsafe("id")
//      compare(left, right) shouldBe an[Identical[Base]]
//    }
  }

  "collections" - {
    "list" - {
      "identical" in {
        compare(List("a"), List("a")) shouldBe Identical(List("a"))
      }

      "nullable" in {
        compare(List.empty[Int], null: List[Int]) shouldBe DiffResultValue(List.empty, null)
      }

      "diff" in {
        compare(List("a"), List("B")) shouldBe DiffResultObject(
          "List",
          Map("0" -> DiffResultString(List(DiffResultValue("a", "B"))))
        )
      }

      "use ignored fields from elements" in {
        val o1 = Organization(List(p1, p2))
        val o2 = Organization(List(p1, p1, p1))
        implicit val d: Diff[Organization] = Derived[Diff[Organization]].ignoreUnsafe("people", "name")
        compare(o1, o2) shouldBe DiffResultObject(
          "Organization",
          Map(
            "people" -> DiffResultObject(
              "List",
              Map(
                "0" -> Identical(p1),
                "1" -> DiffResultObject(
                  "Person",
                  Map(
                    "name" -> Identical(p2.name),
                    "age" -> DiffResultValue(p2.age, p1.age),
                    "in" -> Identical(instant)
                  )
                ),
                "2" -> DiffResultMissing(Person(p1.name, p1.age, instant))
              )
            )
          )
        )
      }

      "compare lists using set-like comparator" in {
        val o1 = Organization(List(p1, p2))
        val o2 = Organization(List(p2, p1))
        implicit val om: ObjectMatcher[Person] = (left: Person, right: Person) => left.name == right.name
        implicit val dd: Diff[List[Person]] = Diff[Set[Person]].contramap(_.toSet)
        compare(o1, o2) shouldBe Identical(Organization(List(p1, p2)))
      }

      "should preserve order of elements" in {
        val l1 = List(1, 2, 3, 4, 5, 6)
        val l2 = List(1, 2, 3, 4, 5, 7)
        compare(l1, l2) shouldBe DiffResultObject(
          "List",
          ListMap(
            "0" -> Identical(1),
            "1" -> Identical(2),
            "2" -> Identical(3),
            "3" -> Identical(4),
            "4" -> Identical(5),
            "5" -> DiffResultValue(6, 7)
          )
        )
      }
    }
    "sets" - {
      "identity" in {
        compare(Set(1), Set(1)) shouldBe an[Identical[_]]
      }

      "nullable" in {
        compare(Set.empty[Int], null: Set[Int]) shouldBe DiffResultValue(Set.empty[Int], null)
      }

      "diff" in {
        val diffResult = compare(Set(1, 2, 3, 4, 5), Set(1, 2, 3, 4)).asInstanceOf[DiffResultSet]
        diffResult.diffs should contain theSameElementsAs List(
          DiffResultAdditional(5),
          Identical(4),
          Identical(3),
          Identical(2),
          Identical(1)
        )
      }
      "ignored fields from elements" in {
        val p2m = p2.copy(age = 33, in = Instant.now())
        implicit val d: Diff[Person] = Derived[Diff[Person]].ignoreUnsafe("age")
        implicit val im: ObjectMatcher[Person] = (left: Person, right: Person) => left.name == right.name
        compare(Set(p1, p2), Set(p1, p2m)) shouldBe DiffResultSet(
          List(
            Identical(p1),
            DiffResultObject(
              "Person",
              Map(
                "name" -> Identical(p2.name),
                "age" -> Identical(p2.age),
                "in" -> DiffResultValue(p1.in, p2m.in)
              )
            )
          )
        )
      }

      "mutable set" in {
        import scala.collection.{Set => mSet}
        val diffResult = compare(mSet(1, 2, 3, 4, 5), mSet(1, 2, 3, 4)).asInstanceOf[DiffResultSet]
        diffResult.diffs should contain theSameElementsAs List(
          DiffResultAdditional(5),
          Identical(4),
          Identical(3),
          Identical(2),
          Identical(1)
        )
      }

      "identical when products are identical using ignored" in {
        val p2m = p2.copy(age = 33, in = Instant.now())
        implicit val d: Diff[Person] = Derived[Diff[Person]].ignoreUnsafe("age").ignoreUnsafe("in")
        compare(Set(p1, p2), Set(p1, p2m)) shouldBe Identical(Set(p1, p2))
      }

      "propagate ignore fields to elements" in {
        val p2m = p2.copy(in = Instant.now())
        implicit val im: ObjectMatcher[Person] = (left: Person, right: Person) => left.name == right.name
        implicit val ds: Diff[Person] = Derived[Diff[Person]].ignoreUnsafe("age")
        compare(Set(p1, p2), Set(p1, p2m)) shouldBe DiffResultSet(
          List(
            Identical(p1),
            DiffResultObject(
              "Person",
              Map(
                "name" -> Identical(p2.name),
                "age" -> Identical(p2.age),
                "in" -> DiffResultValue(p1.in, p2m.in)
              )
            )
          )
        )
      }
      "set of products" in {
        val p2m = p2.copy(age = 33)
        compare(Set(p1, p2), Set(p1, p2m)) shouldBe DiffResultSet(
          List(DiffResultAdditional(p2), DiffResultMissing(p2m), Identical(p1))
        )
      }
      "override set instance" in {
        val p2m = p2.copy(age = 33)
        implicit def setDiff[T, C[W] <: scala.collection.Set[W]]: Diff[C[T]] =
          (left: C[T], _: C[T], _: List[Any]) => Identical(left)
        compare(Set(p1, p2), Set(p1, p2m)) shouldBe an[Identical[_]]
      }

      "set of products using instance matcher" in {
        val p2m = p2.copy(age = 33)
        implicit val im: ObjectMatcher[Person] = (left: Person, right: Person) => left.name == right.name
        compare(Set(p1, p2), Set(p1, p2m)) shouldBe DiffResultSet(
          List(
            Identical(p1),
            DiffResultObject(
              "Person",
              Map("name" -> Identical(p2.name), "age" -> DiffResultValue(p2.age, p2m.age), "in" -> Identical(p1.in))
            )
          )
        )
      }
    }
    "maps" - {
      "identical" in {
        val m1 = Map("a" -> 1)
        compare(m1, m1) shouldBe an[Identical[_]]
      }

      "nullable" in {
        compare(Map.empty[Int, Int], null: Map[Int, Int]) shouldBe DiffResultValue(Map.empty[Int, Int], null)
      }

      "simple diff" in {
        val m1 = Map("a" -> 1)
        val m2 = Map("a" -> 2)
        compare(m1, m2) shouldBe DiffResultMap(Map(Identical("a") -> DiffResultValue(1, 2)))
      }

      "simple diff - mutable map" in {
        val m1 = scala.collection.Map("a" -> 1)
        val m2 = scala.collection.Map("a" -> 2)
        compare(m1, m2) shouldBe DiffResultMap(Map(Identical("a") -> DiffResultValue(1, 2)))
      }

      "propagate ignored fields to elements" in {
        implicit val dm: Diff[Person] = Derived[Diff[Person]].ignoreUnsafe("age")
        compare(Map("first" -> p1), Map("first" -> p2)) shouldBe DiffResultMap(
          Map(
            Identical("first") -> DiffResultObject(
              "Person",
              Map(
                "name" -> DiffResultString(List(DiffResultValue(p1.name, p2.name))),
                "age" -> Identical(p1.age),
                "in" -> Identical(p1.in)
              )
            )
          )
        )
      }

      "identical when products are identical using ignore" in {
        implicit val dm: Diff[Person] =
          Derived[Diff[Person]].ignoreUnsafe("age").ignoreUnsafe("name")
        compare(Map("first" -> p1), Map("first" -> p2)) shouldBe Identical(Map("first" -> p1))
      }

      "maps by values" in {
        implicit def mapWithoutKeys[T, R: Diff]: Diff[Map[T, R]] =
          Diff[List[R]].contramap(_.values.toList)

        val person = Person("123", 11, Instant.now())
        compare(
          Map[String, Person]("i1" -> person),
          Map[String, Person]("i2" -> person)
        ) shouldBe Identical(List(person))
      }

      "ignore part of map's key using keys's diff specification" in {
        implicit def dm: Diff[KeyModel] = Derived[Diff[KeyModel]].ignore(_.id)

        val a1 = MyLookup(Map(KeyModel(UUID.randomUUID(), "k1") -> "val1"))
        val a2 = MyLookup(Map(KeyModel(UUID.randomUUID(), "k1") -> "val1"))
        compare(a1, a2) shouldBe Identical(a1)
      }

      "match values using object mapper" in {
        implicit val om: ObjectMatcher[KeyModel] = (left: KeyModel, right: KeyModel) => left.name == right.name
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val a1 = MyLookup(Map(KeyModel(uuid1, "k1") -> "val1"))
        val a2 = MyLookup(Map(KeyModel(uuid2, "k1") -> "val1"))
        compare(a1, a2) shouldBe DiffResultObject(
          "MyLookup",
          Map(
            "map" -> DiffResultMap(
              Map(
                DiffResultObject(
                  "KeyModel",
                  Map(
                    "id" -> DiffResultValue(uuid1, uuid2),
                    "name" -> Identical("k1")
                  )
                ) -> Identical("val1")
              )
            )
          )
        )
      }
    }
    "ranges" - {
      "identical" in {
        val r1 = 0 until 100
        val r2 = 0 until 100
        compare(r1, r2) shouldBe Identical(r1)
      }
      "dif" in {
        val r1 = 0 until 100
        val r2 = 0 until 99
        compare(r1, r2) shouldBe DiffResultValue(r1, r2)
      }
      "inclusive vs exclusive" in {
        val r1 = 0 until 100
        val r2 = 0 to 100
        compare(r1, r2) shouldBe DiffResultValue(r1, r2)
      }
    }
  }

  "Diff.useEquals" - {
    "Uses Object.equals instance for comparison" in {
      val a = new HasCustomEquals("aaaa")
      val z = new HasCustomEquals("zzzz")
      val not = new HasCustomEquals("not")
      val diffInstance = Diff.useEquals[HasCustomEquals]

      diffInstance.apply(a, z) shouldBe Identical(a)
      diffInstance.apply(a, not) shouldBe DiffResultValue(a, not)
    }
  }

  "strings" - {
    "equal strings should be equal" in {
      val left = "scalaIsAwesome"
      val right = "scalaIsAwesome"

      compare(left, right) shouldBe Identical(left)
    }

    "different strings should be different" in {
      val left = "scalaIsAwesome"
      val right = "diffxIsAwesome"

      compare(left, right) shouldBe DiffResultString(List(DiffResultValue(left, right)))
    }

    "multiline strings should be compared line by line" in {
      val left =
        """first
          |second
          |third
          |fourth""".stripMargin
      val right =
        """first
          |sec???
          |third""".stripMargin

      compare(left, right) shouldBe DiffResultString(
        List(
          Identical("first"),
          DiffResultValue("second", "sec???"),
          Identical("third"),
          DiffResultAdditional("fourth")
        )
      )
    }
  }
  "either" - {
    "equal rights should be identical" in {
      val e1: Either[String, String] = Right("a")
      compare(e1, e1) shouldBe Identical("a")

    }
    "equal lefts should be identical" in {
      val e1: Either[String, String] = Left("a")
      compare(e1, e1) shouldBe Identical("a")
    }
    "left and right should be different" in {
      val e1: Either[String, String] = Left("a")
      val e2: Either[String, String] = Right("a")
      compare(e1, e2) shouldBe DiffResultValue(e1, e2)
    }
  }
  "tuples" - {
    "tuple2" - {
      "equal tuples should be identical" in {
        compare((1, 2), (1, 2)) shouldBe Identical((1, 2))
      }
      "different first element should make them different" in {
        compare((1, 2), (3, 2)) shouldBe DiffResultObject(
          "Tuple2",
          Map("_1" -> DiffResultValue(1, 3), "_2" -> Identical(2))
        )
      }
      "different second element should make them different" in {
        compare((1, 3), (1, 2)) shouldBe DiffResultObject(
          "Tuple2",
          Map("_1" -> Identical(1), "_2" -> DiffResultValue(3, 2))
        )
      }
    }
    "tuple3" - {
      "equal tuples should be identical" in {
        compare((1, 2, 3), (1, 2, 3)) shouldBe Identical((1, 2, 3))
      }
      "different first element should make them different" in {
        compare((1, 2, 3), (4, 2, 3)) shouldBe DiffResultObject(
          "Tuple3",
          Map("_1" -> DiffResultValue(1, 4), "_2" -> Identical(2), "_3" -> Identical(3))
        )
      }
      "different second element should make them different" in {
        compare((1, 2, 3), (1, 4, 3)) shouldBe DiffResultObject(
          "Tuple3",
          Map("_1" -> Identical(1), "_2" -> DiffResultValue(2, 4), "_3" -> Identical(3))
        )
      }
      "different third element should make them different" in {
        compare((1, 2, 3), (1, 2, 4)) shouldBe DiffResultObject(
          "Tuple3",
          Map("_1" -> Identical(1), "_2" -> Identical(2), "_3" -> DiffResultValue(3, 4))
        )
      }
    }
  }
}

case class Person(name: String, age: Int, in: Instant)

case class Family(first: Person, second: Person)

case class Organization(people: List[Person])

sealed trait Parent

case class Bar(s: String, i: Int) extends Parent

case class Foo(bar: Bar, b: List[Int], parent: Option[Parent]) extends Parent

class HasCustomEquals(val s: String) {
  override def equals(obj: Any): Boolean = {
    obj match {
      case o: HasCustomEquals => this.s.length == o.s.length
      case _                  => false
    }
  }
}

sealed trait TsDirection

object TsDirection {
  case object Incoming extends TsDirection

  case object Outgoing extends TsDirection
}

case class KeyModel(id: UUID, name: String)

case class MyLookup(map: Map[KeyModel, String])
