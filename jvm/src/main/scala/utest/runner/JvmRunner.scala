package utest.runner

import utest.toTestSeq
import sbt.testing._
import sbt.testing
import collection.mutable
import utest.framework.{TestTreeSeq, TestSuite, Result}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.ExecutionContext.Implicits.global
import utest.util.{ArgParse, Tree}
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

class JvmRunner(val args: Array[String],
                val remoteArgs: Array[String])
                extends GenericRunner{

  def doStuff(s: Seq[String], loggers: Seq[Logger], name: String) = {
    val cls = Class.forName(name + "$")
    val suite = cls.getField("MODULE$").get(cls).asInstanceOf[TestSuite]
    val res = utest.runSuite(
      suite,
      s.toArray,
      args,
      s => if(s.toBoolean) success.incrementAndGet() else failure.incrementAndGet(),
      msg => loggers.foreach(_.info(progressString + name + "" + msg)),
      msg => addFailure(progressString + name + "" + msg),
      s => total.addAndGet(s.toInt)
    )

    addResult(res)
  }
}

