package at.tugraz.ist.qs2020

import at.tugraz.ist.qs2020.simple.SimpleFunctions._
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

  private val nonEmptyIntSetGen: Gen[Set[Int]] = Gen.nonEmptyContainerOf[Set, Int](Arbitrary.arbitrary[Int])

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

  property("maximum: biggest") = forAll(nonEmptyIntListGen) { (xs: List[Int]) =>
    val maximum = max(xs)
    xs.nonEmpty ==> xs.indices.forall((i: Int) => xs(i) <= maximum)
  }

  // minimal index

  //this might be redundant
  property("minimum: valid index") = forAll(nonEmptyIntListGen) { (xs: List[Int]) =>
    val minimum = minIndex(xs)
    minimum >= 0 && minimum < xs.length
  }

  property("minimum: smallest") = forAll(nonEmptyIntListGen) { (xs: List[Int]) =>
    val minimum = minIndex(xs)
    xs.nonEmpty ==> xs.indices.forall((i: Int) => xs(i) >= xs(minimum))
  }

  // symmetric difference

  property("symDiff: unique elements") = forAll(nonEmptyIntSetGen, nonEmptyIntSetGen) { (xs: Set[Int], ys: Set[Int]) =>
    var as  = xs.toList
    var bs  = ys.toList

    //In case sets are equal make them distinct
    if (xs.equals(ys))
    {
      as = List(xs.sum + 1) ::: xs.toList
    }

    val symDiff = symmetricDifference(xs.toList, ys.toList)

    def count(a: Int, as: List[Int]) = as.count(_ == a)

    as.nonEmpty && bs.nonEmpty ==> symDiff.indices.forall((i: Int) => count(symDiff(i), as) + count(symDiff(i), bs) == 1)
  }


  property("intersection: duplicate elements") = forAll(nonEmptyIntSetGen, nonEmptyIntSetGen) { (xs: Set[Int], ys: Set[Int]) =>
    var as  = xs.toList
    var bs  = ys.toList

    //In case sets are equal make them distinct
    if (xs.equals(ys))
    {
      as = List(xs.sum + 1) ::: xs.toList
    }

    val nums = intersection(as, bs)

    def count(a: Int, as: List[Int]) = as.count(_ == a)

    as.nonEmpty && bs.nonEmpty ==> nums.indices.forall((i: Int) =>  {count(nums(i), as) + count(nums(i), bs) == 2})
  }

  // Smallest missing positive integer

  property("smallestMissingPositiveInteger: ") = forAll(nonEmptyIntListGen) { (xs: List[Int]) =>

    val smallestInt = smallestMissingPositiveInteger(xs)

    /*var smallestInt: Int = 1
    var test: Boolean = true
    while (test)
    {
      if (!xs.contains(smallestInt))
      {
        test = false
      }
      smallestInt = smallestInt + 1
    }*/

    //This means that, the list may not contain the smallestMissingInteger, that all elements between 0 and smallestInt must be included in the list if smallInt isn't exactly 1
    xs.nonEmpty && !xs.contains(smallestInt) && ((1 until smallestInt).toSet.subsetOf(xs.toSet) || smallestInt == 1)
  }
  // TODO: symmetricDifference() properties

  // intersection

  // TODO: intersection() properties

  // Smallest missing positive integer

  // TODO: smallestMissingPositiveInteger() properties

}
