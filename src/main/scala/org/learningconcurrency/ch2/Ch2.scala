package org.learningconcurrency.ch2

import org.learningconcurrency.ch2.Ch2.SynchronizedNesting.Account

import scala.collection.mutable
import scala.util.Random

/**
  * Created by kaiyin on 1/12/16.
  */
object Ch2 {

  import org.learningconcurrency._


  def thread(body: => Unit): Thread = {
    val t = new Thread {
      override def run() = body
    }
    t.start()
    t
  }

  object Uid {
    var uidCount = 0L

    def getUid = this.synchronized {
      val freshUid = uidCount + 1
      uidCount = freshUid
      freshUid
    }

    def printUids(n: Int): Unit = this.synchronized {
      val uids = for (i <- 0 until n) yield getUid
      log(s"Generated uids: $uids")
    }
  }

  object SynchronizedNesting {

    import scala.collection._

    private val transfers = mutable.ArrayBuffer[String]()

    def logTransfer(name: String, n: Int) = transfers.synchronized {
      transfers += s"transfer to account '$name', $n euro"
    }

    class Account(val name: String, var money: Int) {
      val uid = Uid.getUid
    }

    def add(account: Account, n: Int) = account.synchronized {
      account.money += n
      if (n > 10) logTransfer(account.name, n)
    }
  }

  object SynchronizedNoDeadLock {

    import SynchronizedNesting.Account

    def send(a: Account, b: Account, n: Int) = {
      def adjust = {
        a.money -= n
        b.money += n
      }
      if (a.uid < b.uid) a.synchronized {
        b.synchronized {
          adjust
        }
      } else b.synchronized {
        a.synchronized {
          adjust
        }
      }
    }
  }

  //  object SynchronizedBadPool {
  //    private val tasks = collection.mutable.Queue[() => Unit]()
  //    val worker = new Thread {
  //      def poll: Option[() => Unit] = tasks.synchronized {
  //        if(tasks.nonEmpty) Some(tasks.dequeue()) else None
  //      }
  //      override def run = while(true) poll match {
  //        case Some(task) => task()
  //        case None =>
  //      }
  //    }
  //    worker.setName("Worker")
  //    worker.setDaemon(true)
  //    worker.start()
  //  }

  object SynchronizedGuardedBlocks {
    val lock = new AnyRef
    var message: Option[String] = None
    val greeter = thread {
      lock.synchronized {
        while (message == None) lock.wait()
        log(message.get)
      }
    }

    def msg = {
      lock.synchronized {
        message = Some("hello")
        lock.notify()
      }
      greeter.join()
    }
  }

  object SynchronizedPool {
    private val tasks = collection.mutable.Queue[() => Unit]()

    object Worker extends Thread {
      setDaemon(true)

      def poll = tasks.synchronized {
        while (tasks.isEmpty) tasks.wait
        tasks.dequeue()
      }

      override def run() = while (true) {
        val task = poll
        task()
      }
    }

    Worker.start()

    def asynchronous(body: => Unit) = tasks.synchronized {
      tasks.enqueue(() => body)
      tasks.notify()
    }
  }

  // graceful shutdown
  object SynchronizedPool1 {
    private val tasks = collection.mutable.Queue[() => Unit]()

    object Worker extends Thread {
      var terminated = false
      setDaemon(true)

      def poll: Option[() => Unit] = tasks.synchronized {
        while (tasks.isEmpty && !terminated) tasks.wait()
        if (!terminated) Some(tasks.dequeue()) else None
      }

      import scala.annotation.tailrec

      @tailrec override def run() = poll match {
        case Some(task) => task(); run()
        case None =>
      }

      def shutdown() = tasks.synchronized {
        terminated = true
        tasks.notify()
      }
    }

    Worker.start()

    def asynchronous(body: => Unit) = tasks.synchronized {
      tasks.enqueue(() => body)
      tasks.notify()
    }

    def shutdown(): Unit = tasks.synchronized {
      Worker.shutdown()
    }
  }

  class Page(val txt: String, var position: Int)

  val rand = new Random()

  object Volatile {
    val pages = for (i <- 1 to 15) yield new Page("Na" * rand.nextInt(1000) + " Batman!" + "Na" * rand.nextInt(500), -1)
    @volatile var found = false

    def run(): Unit = {
      for (p <- pages) yield thread {
        var i = 0
        while (i < p.txt.length && !found) if (p.txt(i) == '!') {
          p.position = i
          // a slight delay causes problems: other threads have time to find other occurences of exclamation point.
          Thread.sleep(1)
          found = true
        } else i += 1
        // if still not found, wait for another thread to find it.
        while (!found) {}
        log(s"results: ${pages.map(_.position)}")
      }
    }
  }

  object AntiVolatile {
    case class Found(var isFound: Boolean)
    val pages = for (i <- 1 to 15) yield new Page("Na" * rand.nextInt(1000) + " Batman!" + "Na" * rand.nextInt(500), -1)
    val found = Found(false)

    def run(): Unit = {
      for (p <- pages) yield thread {
        var i = 0
        var foundInThread = found.isFound
        while (i < p.txt.length && !foundInThread)
          if (p.txt(i) == '!') {
            found.synchronized {
              found match {
                case Found(true) => foundInThread = true
                case Found(false) => {
                  p.position = i
                  found.isFound = true
                  Thread.sleep(1)
                }
                case _ =>
              }
            }
          } else i += 1
        // if still not found, wait for another thread to find it.
        def wait(): Unit = {
          found match {
            case Found(false) => wait()
            case _ =>
          }
        }
        wait()
        log(s"results: ${pages.map(_.position)}")
      }
    }
  }

  def parallel[A, B](a: => A, b: => B): (A, B) = {
    var res1: A = null.asInstanceOf[A]
    var res2: B = null.asInstanceOf[B]
    val t1 = thread {
      res1 = a
    }
    val t2 = thread {
      res2 = b
    }
    t1.join()
    t2.join()
    (res1, res2)
  }


  def periodically(duration: Long)(b: => Unit): Thread = {
    val t = thread {
      while (true) {
        b
        Thread.sleep(duration)
      }
    }
    t
  }

  class SyncVar[T] {

    private var empty: Boolean = true

    private var x: T = null.asInstanceOf[T]

    def get(): T = this.synchronized {
      if (empty) throw new Exception("must be non-empty")
      else {
        empty = true
        x
      }
    }

    def put(x: T): Unit = this.synchronized {
      if (!empty) throw new Exception("must be empty")
      else {
        empty = false
        this.x = x
      }
    }

    def getWait(): T = this.synchronized {
      while (empty)
        this.wait()

      empty = true
      this.notify()
      x
    }

    def putWait(x: T): Unit = this.synchronized {
      while (!empty)
        this.wait()

      empty = false
      this.x = x
      this.notify()
    }

    def isEmpty = synchronized {
      empty
    }

    def nonEmpty = synchronized {
      !empty
    }

  }


  def main(args: Array[String]): Unit = {
    var x = 0
    log("here")
    val t = thread {
      log("running")
      //      Thread.sleep(2000)
      x += 10
    }
    log("...")
    log("...")
    // order of execution of log("running") is not guaranteed
    log("...")
    log("...")
    log("...")
    log("...")
    t.join()
    log("back to main")
    log(s"value of x: $x")

    val t1 = thread {
      Uid.printUids(5)
    }
    Uid.printUids(5)
    t1.join()

    val jane = new SynchronizedNesting.Account("Jane", 100)
    val threads = collection.mutable.ArrayBuffer[Thread]()
    for (_ <- 1 to 5) {
      threads += thread {
        SynchronizedNesting.add(jane, 15)
      }
    }
    threads.foreach {
      x => x.join()
    }
    println(jane.money)

    SynchronizedGuardedBlocks.msg

    SynchronizedPool.asynchronous {
      log("hello")
    }
    SynchronizedPool.asynchronous {
      log("world")
    }

    SynchronizedPool1.asynchronous {
      log("hello")
    }
    SynchronizedPool1.asynchronous {
      log("world")
      SynchronizedPool1.shutdown()
    }
    SynchronizedPool1.asynchronous {
      // has no effect, since the worker is already shutdown.
      log("are you still there?")
    }

    Volatile.run()
    AntiVolatile.run()

    val pres = parallel({
      (1 to 5).product
    }, {
      (1 to 100000).sum
    })
    println(pres)

//    periodically(500) {
//      log("firing")
//    }

    val syncVar = new SyncVar[Int]

    val producer = thread {
      var x = 0
      while (x < 15) {
        if (syncVar.isEmpty) {
          syncVar.put(x)
          x = x + 1
        }

      }
    }

    val consumer = thread {
      var x = 0
      while (x != 15) {
        if (syncVar.nonEmpty) {
          log(s"get = ${syncVar.get}")
          x = x + 1
        }
      }
    }

    producer.join()
    consumer.join()
  }

}
