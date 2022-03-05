package volk.steam.libraryexport
package util

import scala.annotation.tailrec

object Utils {

  implicit class FunctionCompositionUtil[A, B](func: A => B) {
    def >>[D](d: B => D): A => D = func.andThen(d)
    def <<[D](d: D => A): D => B = d.andThen(func)
    def |>[D](d: B => D): A => D = func.andThen(d)
  }

  implicit class PipingUtil[T](value: T) {
    def |>[B](func: T => B): B               = func(value)
    def |>?(check: Boolean)(func: T => T): T = if (check) func(value) else value
  }

  implicit class ListUtil[T](list: List[T]) {

    /** count when the given predicate function returns true and false
      * @return (amount of truths, amount of falses)
      */
    def countBoth(pred: T => Boolean): (Int, Int) = {
      @tailrec
      def go(trues: Int, falses: Int, left: List[T]): (Int, Int) =
        left match {
          case Nil => (trues, falses)
          case x :: xs =>
            if (pred(x)) go(trues + 1, falses, xs)
            else go(trues, falses + 1, xs)
        }

      go(0, 0, list)
    }
  }

}
