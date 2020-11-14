package pav

/** a "point" as represented by Pav
  * @param x1 left boundary
  * @param x2 right boundary
  * @param ySum sum of all y values
  * @param size count of all y values */
case class PavPoint(
    x1: Double,
    x2: Double,
    ySum: Double,
    size: Double) {
  def rightMerge(x2New: Double, ySum2: Double, size2: Double): PavPoint = PavPoint(x1, x2New, ySum + ySum2, size + size2)
  def rightMerge(p2: PavPoint): PavPoint = rightMerge(p2.x2, p2.ySum, p2.size)
  def y = ySum / size
  def x = (x1 + x2) / 2
  def tupled = ((x1 + x2) / 2) -> y
}

object PavPoint {
  def empty = PavPoint(Double.MinValue, Double.MinValue, Double.MinValue, 0)
}
