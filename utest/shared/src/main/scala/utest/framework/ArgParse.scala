package utest
package framework
//import acyclic.file
/**
 * Crappy but good-enough version of a command-line parsing library
 */
object ArgParse {
  /**
   * Searches the given tokenized arg list for arguments matching a prefix.
   *
   * @param prefix The prefix to match
   * @param parse The function to use to parse the string into a value of type `T`
   * @param default If the prefix is not found, the default
   * @param default2 If the prefix is found but no value is given, the default
   */
  def find[T](prefix: String, parse: String => T, default: T, default2: T)
             (implicit args: Array[String]): T = {
    args.find(_.startsWith(prefix))
        .fold(default){s =>
      val remainder = s.drop(prefix.length)
      if (remainder.length == 0) default2
      else parse(remainder.drop(1))
    }
  }
}
