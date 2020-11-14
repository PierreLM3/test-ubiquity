package pav

/** type class for a Bin, allowing to represent a mean y value for an given x range */
trait PavBin[A] {
  /** left boundary of the bin */
  def x1Value(a: A): Double
  /** right boundary of the bin */
  def x2Value(a: A): Double
  /** sum of all y values */
  def sumYValue(a: A): Double
  /** count of all y values */
  def binSize(a: A): Double

  ///

  /** current y value */
  def yValue(a: A) = sumYValue(a) / binSize(a)
}

object PavBin {

  def apply[A](implicit bin: PavBin[A]): PavBin[A] = bin

  object syntax {

    implicit class PavBinOps[A](val self: A) extends AnyVal {
      def x1Value(implicit pavBin: PavBin[A]) = pavBin.x1Value(self)
      def x2Value(implicit pavBin: PavBin[A]) = pavBin.x2Value(self)
      def sumYValue(implicit pavBin: PavBin[A]) = pavBin.sumYValue(self)
      def yValue(implicit pavBin: PavBin[A]) = pavBin.yValue(self)
      def binSize(implicit pavBin: PavBin[A]) = pavBin.binSize(self)
    }
  }

  // instances

  implicit val tuplePavBinInstance: PavBin[(Int, Double)] = new PavBin[(Int, Double)] {
    def x1Value(a: (Int, Double)) = a._1.toDouble
    def x2Value(a: (Int, Double)) = a._1.toDouble
    def sumYValue(a: (Int, Double)) = a._2
    def binSize(a: (Int, Double)) = 1
  }

  implicit val tuple2PavBinInstance: PavBin[(Double, Double)] = new PavBin[(Double, Double)] {
    def x1Value(a: (Double, Double)) = a._1
    def x2Value(a: (Double, Double)) = a._1
    def sumYValue(a: (Double, Double)) = a._2
    def binSize(a: (Double, Double)) = 1
  }

  implicit val binInstance: PavBin[PavPoint] = new PavBin[PavPoint] {
    def x1Value(a: PavPoint) = a.x1
    def x2Value(a: PavPoint) = a.x2
    def sumYValue(a: PavPoint) = a.ySum
    def binSize(a: PavPoint) = a.size
  }
}
