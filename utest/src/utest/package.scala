
import utest.asserts._
import scala.concurrent.duration._

/**
 * Created by haoyi on 1/24/14.
 */
package object utest extends utest.asserts.Asserts{

  implicit val retryInterval: RetryInterval = new RetryInterval(100.millis)
  implicit val retryMax: RetryMax = new RetryMax(1.second)

  type Show = asserts.Show

  object test{
    @annotation.compileTimeOnly("test - method should only be used directly inside a Tests{} macro")
    def -(x: => Any) = ()

    @annotation.compileTimeOnly("test{} method should only be used directly inside a Tests{} macro")
    def apply(x: => Any) = ()

    def apply(name: String) = Apply(name)
    case class Apply(name: String){
      @annotation.compileTimeOnly("test() -  method should only be used directly inside a Tests{} macro")
      def -(x: => Any) = ()

      @annotation.compileTimeOnly("test()() method should only be used directly inside a Tests{} macro")
      def apply(x: => Any) = ()
    }
  }


}

