package org.learningconcurrency.ch1

/**
  * Created by kaiyin on 1/12/16.
  */
object Ch1ex {
  def compose[A, B, C](f: B => C, g: A => B): A => C = {
    x: A => f(g(x))
  }
  def fuse[A, B](a: Option[A], b: Option[B]): Option[(A, B)] = {
    for {
      i <- a
      j <- b
    } yield (i, j)
  }
  def check[T](ts: Seq[T])(pred: T => Boolean): Boolean = {
    ts.forall(pred)
  }
  def permutations(x: String): Seq[String] = {
    x.permutations.toSeq
  }
}


