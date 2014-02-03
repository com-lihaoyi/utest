package utest.util

/**
 * Created by haoyi on 2/3/14.
 */
object ArgParse {
  def find[T](prefix: String, parse: String => T, default: T)
             (implicit args: Array[String]): T = {
    args.find(_.startsWith(prefix))
        .fold(default)(s => parse(s.drop(prefix.length)))
  }
}
