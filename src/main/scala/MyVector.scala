object MyVector {

  implicit class VectorWithLimit[A](val self: Vector[A]) {

    def append(a: A, maxSize: Int): Vector[A] = {
      if (self.size < maxSize)
        self :+ a
      else
        self
    }
  }
}

/*class DataVector[A: Ordering](maxSize: Int) {
  var v: Vector[A] = Vector[A]()

  def append(a: A): Boolean = {
    if (v.size < maxSize) {
      v = v :+ a
      true
    } else false
  }

  def sortedIt(): Iterator[A] = v.sorted.iterator

  def size: Int = v.size
}*/
