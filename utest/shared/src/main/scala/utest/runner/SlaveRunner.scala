package utest.runner

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import scala.annotation.tailrec

final class SlaveRunner(args: Array[String],
                        remoteArgs: Array[String],
                        testClassLoader: ClassLoader,
                        send: String => Unit)
                        extends BaseRunner(args, remoteArgs, testClassLoader){

  var successCount: Int = 0
  var failureCount: Int = 0
  var totalCount: Int = 0

  send("h") // send hello message to get initial counts

  def addResult(r: String): Unit = send(s"r$r")

  def addFailure(r: String): Unit = {
    send(s"f$r")
  }
  def addTrace(trace: String): Unit = {
    send(s"c$trace")
  }

  def addTotal(v: Int): Unit = {
    totalCount += v // temporarily
    send(s"t$v")
  }

  def incSuccess(): Unit = {
    successCount += 1 // temporarily
    send(s"is")
  }

  def incFailure(): Unit = {
    failureCount += 1 // temporarily
    send(s"if")
  }

  def done(): String = "" // nothing to do, return value is ignored

  def receiveMessage(msg: String): Option[String] = {
    // We got an updated count message from the master
    val Array(s, f, t) = msg.split(',')
    successCount = s.toInt
    failureCount = f.toInt
    totalCount = t.toInt

    None // <- ignored
  }

}
