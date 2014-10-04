package utest.runner

import sbt.testing._
import utest.ExecutionContext
import utest.framework.TestSuite

import scala.concurrent.Await
import scala.util.{Failure, Success}
import concurrent.duration._
class JvmRunner(val args: Array[String],
                val remoteArgs: Array[String])
                extends GenericRunner{

  def doStuff(s: Seq[String], loggers: Seq[Logger], name: String) = {
    val cls = Class.forName(name + "$")
    val suite = cls.getField("MODULE$").get(cls).asInstanceOf[TestSuite]
    Await.result(
      utest.runSuite(
        suite,
        s.toArray,
        args,
        s => if(s.toBoolean) success.incrementAndGet() else failure.incrementAndGet(),
        msg => loggers.foreach(_.info(progressString + name + "" + msg)),
        msg => addFailure(progressString + name + "" + msg),
        s => total.addAndGet(s.toInt)
      ).andThen{
        case Failure(ex) => throw ex
        case Success(res) => addResult(res)
      } (ExecutionContext.RunNow),
      Duration.Inf
    )
  }
}

