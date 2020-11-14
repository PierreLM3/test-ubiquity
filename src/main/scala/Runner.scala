import pav.Pav
import pav.PavPoint

object Runner {

  def main(args: Array[String]): Unit = {
    val v1 = IndexedSeq(0 -> 0.14, 1 -> 0.30, 2 -> 0.51, 3 -> 0.58, 4 -> 0.58, 5 -> 0.65, 6 -> 0.76, 7 -> 0.78, 8 -> 0.86, 9 -> 0.98)
    val v2 = IndexedSeq(0 -> 0.14, 1 -> 0.30, 2 -> 0.58, 3 -> 0.58, 4 -> 0.51, 5 -> 0.30, 6 -> 0.76, 7 -> 0.78, 8 -> 0.86, 9 -> 0.98)

    val doubles: IndexedSeq[Double] = Pav.regression(v1.toIterator).map(_.y)
    println(doubles)

    val doubles1: IndexedSeq[Double] = Pav.regression(v2.toIterator).map(_.y)
    println(doubles1)

    Pav.regression(Iterator(PavPoint.empty))

    ()
  }
}
