package utest.asserts

object DummyTypeclass {
  implicit def DummyImplicit[T] = new DummyTypeclass[T]
}
class DummyTypeclass[+T]