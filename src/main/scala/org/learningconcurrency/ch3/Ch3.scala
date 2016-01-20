package org.learningconcurrency.ch3

/**
  * Created by kaiyin on 1/17/16.
  */

import java.util.concurrent.{LinkedBlockingQueue, LinkedBlockingDeque}
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean, AtomicLong}

import scala.annotation.tailrec
import scala.concurrent._
import scala.concurrent.forkjoin.ForkJoinPool
import org.learningconcurrency.log
import scala.concurrent.duration.SECONDS

object Ch3 {
  val executor = new ForkJoinPool()
  val executor1 = ExecutionContext.global

  //  def execute(body: => Unit) = ExecutionContext.global.execute(new Runnable {
  //    override def run(): Unit = {
  //      body
  //    }
  //  })
  def execute(body: => Unit) = executor.execute(new Runnable {
    override def run(): Unit = {
      body
    }
  })

  object AtomicUid {
    private val uid = new AtomicLong(0L)

    def getUid(): Long = uid.incrementAndGet()
  }

  object AtomicLock extends App {
    private val lock = new AtomicBoolean(false)

    def mySynchronized(body: => Unit): Unit = {
      while (!lock.compareAndSet(false, true)) {}
      try body finally lock.set(false)
    }

    var count = 0
    for (i <- 0 until 10) execute {
      mySynchronized {
        count += 1
      }
    }
    Thread.sleep(1000)
    log(s"Count is: $count")
  }

  class Entry(val isDir: Boolean) {
    val state = new AtomicReference[State](new Idle)

  }

  @tailrec private def prepareForDelete(entry: Entry): Boolean = {
    val s0 = entry.state.get
    s0 match {
      case i: Idle =>
        if (entry.state.compareAndSet(s0, new Deleting)) true
        else prepareForDelete(entry)
      case c: Creating =>
        log("File is being created, cannot delete")
        false
      case c: Copying =>
        log("File is being created, cannot delete")
        false
      case d: Deleting =>
        false
    }
  }

  sealed trait State

  class Idle extends State

  class Creating extends State

  class Copying(val n: Int) extends State

  class Deleting extends State


  //  object LazyValsCreate {
  //    lazy val obj = new AnyRef
  //    lazy val non = s"made by ${Thread.currentThread.getName}"
  //    def run(): Unit = {
  //      execute {
  //        log(s"EC sees obj = $obj")
  //        log(s"EC sees non = $non")
  //      }
  //      log(s"Main sees obj = $obj")
  //      log(s"Main sees non = $non")
  //    }
  //  }

  object LazyValsUnderTheHood extends App {
    @volatile private var _bitmap = false
    private var _obj: AnyRef = _

    def obj = if (_bitmap) _obj
    else this.synchronized {
      if (!_bitmap) {
        _obj = new AnyRef
        _bitmap = true
      }
      _obj
    }

    log(s"$obj")
    log(s"$obj")
  }


  private val messages = new LinkedBlockingDeque[String]()
  val logger = new Thread {
    setDaemon(true)

    override def run() = while (true) log(messages.take())
  }

  def logMsg(msg: String): Unit = {
    messages.offer(msg)
  }


  def main(args: Array[String]) {
    //    executor.execute(new Runnable {
    //      override def run(): Unit = {
    //        log("Task run.")
    //      }
    //    })
    //    executor1.execute(new Runnable {
    //      override def run(): Unit = log("Running")
    //    })
    //    execute {
    //      log("hello")
    //    }
    //
    //    execute {
    //      log(s"Uid async: ${AtomicUid.getUid()}")
    //    }
    //    log(s"Uid: ${AtomicUid.getUid()}")
    //    executor.shutdown()
    //    executor.awaitTermination(2, SECONDS)
    //
    //    logger.start()
    //    logMsg("A new log msg!")
    val queue = new LinkedBlockingQueue[String]
    for (i <- 1 to 5500) queue.offer(i.toString)
    execute {
      val it = queue.iterator
      while (it.hasNext) log(it.next())
    }
    for (i <- 1 to 5500) queue.poll()
    Thread.sleep(1000)

  }
}
