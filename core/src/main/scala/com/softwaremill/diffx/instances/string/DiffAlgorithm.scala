package com.softwaremill.diffx.instances.string

import java.util

trait DiffAlgorithm[T] {
  def diff(original: util.List[T], revised: util.List[T]): Patch[T]
}
