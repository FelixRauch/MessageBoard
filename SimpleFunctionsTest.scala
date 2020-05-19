package at.tugraz.ist.qs2020

import at.tugraz.ist.qs2020.simple.SimpleFunctions._
import org.scalacheck.Prop.True
//import at.tugraz.ist.qs2020.simple.SimpleFunctionsMutant1._
import at.tugraz.ist.qs2020.simple.SimpleJavaFunctions
import org.junit.runner.RunWith
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.{Arbitrary, Gen, Properties}

// Consult the following scalacheck documentation
// https://github.com/typelevel/scalacheck/blob/master/doc/UserGuide.md#concepts
// https://github.com/typelevel/scalacheck/blob/master/doc/UserGuide.md#generators

@RunWith(classOf[ScalaCheckJUnitPropertiesRunner])
class SimpleFunctionsTest extends Properties("SimpleFunctionsTest") {

  private val nonEmptyIntListGen: Gen[List[Int]] = Gen.nonEmptyListOf(Arbitrary.arbitrary[Int])

  // insertionSort

  property("insertionSort: ordered") = forAll(nonEmptyIntListGen) { (xs: List[Int]) =>
    val sorted = insertionSort(xs)
    xs.nonEmpty ==> xs.indices.tail.forall((i: Int) => sorted(i - 1) <= sorted(i))
  }

  property("insertionSort: permutation") = forAll { (xs: List[Int]) =>
    val sorted = insertionSort(xs)

    def count(a: Int, as: List[Int]) = as.count(_ == a)

    xs.forall((x: Int) => count(x, xs) == count(x, sorted))
  }

  // insertionSort Java

  property("insertionSort Java: ordered") = forAll(nonEmptyIntListGen) { (xs: List[Int]) =>
    val sorted = SimpleJavaFunctions.insertionSort(xs.toArray)
    xs.nonEmpty ==> xs.indices.tail.forall((i: Int) => sorted(i - 1) <= sorted(i))
  }

  property("insertionSort Java: permutation") = forAll { (xs: List[Int]) =>
    val sorted = SimpleJavaFunctions.insertionSort(xs.toArray)

    def count(a: Int, as: List[Int]) = as.count(_ == a)

    xs.forall((x: Int) => count(x, xs) == count(x, sorted.toList))
  }

  // maximum

  // TODO: max() properties

  // minimal index

  // TODO: minIndex() properties

  // symmetric difference

  // TODO: symmetricDifference() properties

  // intersection

  // TODO: intersection() properties
  property("insertionSort Java: intersection") = forAll { (xs: List[Int], ys: List[Int]) =>
    var nums: List[Int] = List()

    for (i <- 0 until xs.length - 1)
    {
      for (j <- 0 until ys.length - 1)
      {
        if (xs(i) == ys (j))
        {
          if (!nums.contains(xs(i)))
          {
            nums = nums ::: List(xs(i))
          }
        }
      }
    }
    nums.nonEmpty ==> nums.indices.forall((i: Int) => ys.contains(nums(i)) == xs.contains(nums(i)))
  }
  // Smallest missing positive integer

  // TODO: smallestMissingPositiveInteger() properties
  property("insertionSort Java: smallestMissingPositiveInteger") = forAll(nonEmptyIntListGen) { (xs: List[Int]) =>
    var smallestInt: Int = 1
    var test: Boolean = true
    while (test)
    {
      if (!xs.contains(smallestInt))
      {
        test = false
      }
      smallestInt = smallestInt + 1
    }
    
    xs.nonEmpty ==> xs.indices.forall((i: Int) => xs(i) > smallestInt)
  }
}
