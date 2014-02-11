package utest

/**
 * Created by haoyi on 2/5/14.
 */
object Main {
  def main(args: Array[String]): Unit = {
    val thing = Seq(1, 2, 3)
    assertMatch(thing){case Seq(1, _, 3) =>}
    ()
  }
}
