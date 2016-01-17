package org

/**
  * Created by kaiyin on 1/12/16.
  */
package object learningconcurrency {
  def log(msg: String): Unit = {
    println(s"${Thread.currentThread.getName}: $msg")
  }
}
