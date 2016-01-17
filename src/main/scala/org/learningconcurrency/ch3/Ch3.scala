package org.learningconcurrency.ch3

/**
  * Created by kaiyin on 1/17/16.
  */
import scala.concurrent._
import scala.concurrent.forkjoin.ForkJoinPool
import org.learningconcurrency.log

object Ch3 {
  val executor = new ForkJoinPool()

  def main(args: Array[String]) {
    executor.execute(new Runnable {
      override def run(): Unit = {
        log("Task run.")
      }
    })
  }
}
