import scala.annotation.tailrec
import scala.collection.JavaConverters._

import java.util.{LinkedList, Iterator => JavaIterator}

/** object allowing to perform isotonic regression, with the "regression" method */
object Pav {
  import PavBin.syntax._

  type PavPoints = IndexedSeq[PavPoint]

  /** performs isotonic regression using PAV algorithm.
   * Merge indentical bins (same X) during the process.
   * (Perhaps consolidating identical bins as a preprocess (two passes in place
   * of one pass) would lead to more accurate results?) */
  def regression[A](inputs: Iterator[A])(implicit ev:PavBin[A]): PavPoints = {

    // as Scala do not allow to remove element of a ListBuffer in o(1), we
    //  have to use Java, here
    val bins = new LinkedList[PavPoint]

    /** check if there is new violators in the right, and aggregate them into the pool */
    @tailrec def rightRegression(currentBin: PavPoint): PavPoints = if (inputs.hasNext) {
      val bin: A = inputs.next
      if (bin.yValue < currentBin.y || (bin.x1Value == currentBin.x1 && bin.x2Value == currentBin.x2)) {
        // we merge bin when
        // 1) bins share the same x (duplicated bin, needed to exhibit sensible statistics on y)
        // 2) a pair violation occurs
        val newBin = currentBin.rightMerge(bin.x2Value, bin.sumYValue, bin.binSize)
        bins.removeLast
        val leftBin = leftRegression(newBin, bins.descendingIterator)
        bins.add(leftBin)
        rightRegression(leftBin)
      } else { // everything OK
        val point = PavPoint(bin.x1Value, bin.x2Value, bin.sumYValue, bin.binSize)
        bins.add(point)
        rightRegression(point)
      }
    } else {
      bins.iterator.asScala.toIndexedSeq
    }

    /** check if there is new violators in the left, and aggregate them into the pool */
    @tailrec def leftRegression(currentBin: PavPoint, it: JavaIterator[PavPoint]): PavPoint = if (it.hasNext) {
      val bin = it.next
      if (bin.y > currentBin.y) { // backward pair violation
        val mergedBin = bin.rightMerge(currentBin)
        it.remove
        leftRegression(mergedBin, it)
      } else { // no violation
        currentBin
      }
    } else {
      currentBin
    }

    rightRegression(PavPoint.empty)
  }

}