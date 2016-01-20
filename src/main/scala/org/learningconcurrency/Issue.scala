package org.learningconcurrency

import java.util.concurrent.{ForkJoinPool, LinkedBlockingQueue}

/**
  * Created by kaiyin on 1/19/16.
  */
import org.learningconcurrency.ch3.Ch3.execute
object Issue {
  def main(args: Array[String]) {

    val queue = new LinkedBlockingQueue[String]
    for (i <- 1 to 5500) queue.offer(i.toString)
    val executor = new ForkJoinPool()
    def execute(body: => Unit) = executor.execute(new Runnable {
      override def run(): Unit = {
        body
      }
    })
    execute {
      val it = queue.iterator
      while (it.hasNext) log(it.next())
    }
    for (i <- 1 to 5500) queue.poll()
    Thread.sleep(1000)
  }
}
