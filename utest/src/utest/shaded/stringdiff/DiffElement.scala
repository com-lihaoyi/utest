package utest.shaded.stringdiff

sealed trait DiffElement[Repr] extends Product with Serializable {

  def inFirstOrSecond: Boolean = false
  def inFirst: Boolean         = false
  def inSecond: Boolean        = false

  def map[B](f: Repr => B): DiffElement[B]

}

object DiffElement {

  final case class InBoth[Repr](v: Repr) extends DiffElement[Repr] {
    def map[B](f: Repr => B): DiffElement[B] = DiffElement.InBoth(f(v))
  }
  final case class InFirst[Repr](v: Repr) extends DiffElement[Repr] {
    override def inFirstOrSecond: Boolean    = true
    override def inFirst: Boolean            = true
    def map[B](f: Repr => B): DiffElement[B] = DiffElement.InFirst(f(v))
  }
  final case class InSecond[Repr](v: Repr) extends DiffElement[Repr] {
    override def inFirstOrSecond: Boolean    = true
    override def inSecond: Boolean           = true
    def map[B](f: Repr => B): DiffElement[B] = DiffElement.InSecond(f(v))
  }
  final case class Diff[Repr](x: Repr, y: Repr) extends DiffElement[Repr] {
    def map[B](f: Repr => B): DiffElement[B] = DiffElement.Diff(f(x), f(y))
  }

}
