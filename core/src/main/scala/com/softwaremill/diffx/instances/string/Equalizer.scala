package com.softwaremill.diffx.instances.string

trait Equalizer[T] {
  def equals(original: T, revised: T): Boolean
}
object Equalizer {
  def default[T]: Equalizer[T] = new Equalizer[T] {
    override def equals(original: T, revised: T): Boolean = {
      original.equals(revised)
    }
  }
}
