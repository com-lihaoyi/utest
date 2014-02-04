package utest.runner

class JvmFramework extends GenericTestFramework{

  def runner(args: Array[String],
             remoteArgs: Array[String],
             testClassLoader: ClassLoader) = {
    new JvmRunner(args, remoteArgs)
  }
}
