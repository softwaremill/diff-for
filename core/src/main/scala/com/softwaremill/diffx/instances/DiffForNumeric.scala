package com.softwaremill.diffx.instances

import com.softwaremill.diffx._

private[diffx] class DiffForNumeric[T: Numeric] extends Diff[T] {
  override def apply(left: T, right: T, context: DiffContext): DiffResult = {
    val numeric = implicitly[Numeric[T]]
    if (!numeric.equiv(left, right)) {
      DiffResultValue(left, right)
    } else {
      IdenticalValue(left)
    }
  }
}
