package utest
package framework
//import acyclic.file

/**
  * An immutable tree where the middle-nodes and leaf-nodes each contain
  * different sorts of data, marked by the type params [[N]] and [[L]]
  */
sealed trait HTree[+N, +L]{
  def mapLeaves[T](f: L => T): HTree[N, T]
  def leaves: Iterator[L]
}
object HTree{
  case class Leaf[+L](value: L) extends HTree[Nothing, L]{
    def mapLeaves[T](f: L => T) = Leaf(f(value))
    def leaves = Iterator(this.value)
  }
  case class Node[+N, +L](value: N, children: HTree[N, L]*) extends HTree[N, L]{
    def mapLeaves[T](f: L => T) = Node(value, children.map(_.mapLeaves(f)):_*)
    def leaves = children.toIterator.flatMap(_.leaves)

  }
}
/**
 * An immutable tree with each node containing a value, and a `Seq` of
 * children. Provides all normal `Seq` functionality as well as some tree
 * specific methods.
 */
case class Tree[+T](value: T, children: Tree[T]*){
  /**
   * The number of nodes in this tree.
   */
  def length: Int = {
    children.foldLeft(1)(_ + _.length)
  }

  def map[V](f: T => V): Tree[V] = {
    Tree(f(value), children.map(_.map(f)):_*)
  }
  /**
   * An iterator over the values stored on the nodes of this tree, in a depth
   * first manner starting from the root.
   */
  def iterator: Iterator[T] = {
    Iterator(this.value) ++ children.flatMap(_.iterator)
  }

  def leafPaths: Iterator[List[T]] = {
    if (children.isEmpty) Iterator(List(this.value))
    else children.toIterator.flatMap(_.leafPaths).map(this.value :: _)
  }

  def toSeq: Seq[T] = iterator.toList
  /**
   * Returns an iterator for the values at the leaves of this tree
   */
  def leaves: Iterator[T] = {
    if (children.isEmpty) Iterator(this.value)
    else children.toIterator.flatMap(_.leaves)
  }

}
