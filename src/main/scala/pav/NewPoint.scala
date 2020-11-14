package pav

case class NewPoint(x: Double, y: Boolean)

object NewPoint {
  import scala.util.Random

  def random(): NewPoint = {
    val i = Random.nextDouble
    val j = Random.nextDouble
    NewPoint(i, j < i)
  }

  implicit val orderingInstance: Ordering[NewPoint] = new Ordering[NewPoint] {
    val doubleCompare = implicitly[Ordering[Double]].compare _
    def compare(x: NewPoint, y: NewPoint) = doubleCompare(x.x, y.x)
  }

  implicit val pavBinInstance = new PavBin[NewPoint] {
    def x1Value(a: NewPoint) = a.x
    def x2Value(a: NewPoint) = a.x
    def sumYValue(a: NewPoint) = if (a.y) 1.0 else 0.0
    def binSize(a: NewPoint) = 1
  }

}
