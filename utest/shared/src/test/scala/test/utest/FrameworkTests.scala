package test.utest
import utest._
import utest.framework.TestPath
import scala.util.{Failure, Success}
import utest.framework.Result

import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure


object FrameworkTests extends utest.TestSuite{

  override def utestBeforeEach(path: Seq[String]): Unit = println("RUN " + path.mkString("."))
  override def utestAfterEach(path: Seq[String]): Unit = println("END " + path.mkString("."))

  implicit val ec = utest.framework.ExecutionContext.RunNow
  def tests = Tests{
    def testHelloWorld(tests: Tests) = {
      val results = TestRunner.run(tests)
      assert(tests.nameTree.length == 4)
      assert(tests.nameTree.leaves.length == 3)
      assert(results.leaves.length == 3)
      assert(results.leaves.count(_.value.isFailure) == 2)
      assert(results.leaves.count(_.value.isSuccess) == 1)
      results.leaves.map(_.value).toList
    }
    'helloWorld - {
      val tests = Tests{
        "test1"-{
          throw new Exception("test1")
        }
        "test2"-{
          1
        }
        "test3"-{
          val a = List[Byte](1, 2)
          a(10)
        }
      }
      testHelloWorld(tests)
    }
    'helloWorldSymbol - {
      val tests = Tests{
        'test1{
          throw new Exception("test1")
        }
        'test2{
          1
        }
        'test3{
          val a = List[Byte](1, 2)
          a(10)
        }
      }
      testHelloWorld(tests)
    }
    'helloWorldSymbol2 - {
      val tests = Tests{
        'test1-{
          throw new Exception("test1")
        }
        'test2-1

        'test3-{
          val a = List[Byte](1, 2)
          a(10)
        }
      }
      testHelloWorld(tests)
    }

    'failures - {
      'noSuchTest - {
        val tests = Tests{
          'test1 - {
            1
          }

        }
        try{
          println(TestRunner.run(tests, query=utest.TestQueryParser("does.not.exist")))
        }catch {case e @ NoSuchTestException(Seq("does")) =>
          assert(e.getMessage.contains("[does]"))
          e.getMessage
        }
      }
      'weirdTestName{
        val tests = Tests{
          "t est1~!@#$%^&*()_+{}|:';<>?,/'"-{
            1
          }
        }
        TestRunner.run(tests)

      }
      'testNestedBadly - {
        // Ideally should not compile, but until I
        // figure that out, a runtime error works great
        //
        // This does not compile
//        try{
//          val tests = Tests{
//            "outer"-{
//              if (true){
//                "inners"-{
//
//                }
//              }
//            }
//          }
//        }catch{case e: IllegalArgumentException =>
//          assert(e.getMessage.contains("inners"))
//          assert(e.getMessage.contains("nested badly"))
//          e.getMessage
//        }
      }
    }

    'extractingResults - {
     'basic - {
        val tests = Tests{
          'test1 - {
            "i am cow"
          }
          'test2 - {
            "1" - {
              1
            }
            "2" - {
              2
            }
            999
          }
          'test3{
            Seq('a', 'b')
          }
        }
        val results = TestRunner.run(tests)
        val expected = Seq("i am cow", 1, 2, Seq('a', 'b')).map(Success[Any])
        assert(results.leaves.map(_.value).toList == expected)
        results.leaves.map(_.value.get)
      }
      'onlyLastThingReturns - {
        val tests = TestSuite {
          12 + 2
          'omg - {
          }
        }
        val res = TestRunner.run(tests).leaves.next().value
        assert(res == Success(()))
      }
    }

    'nesting - {
      'importStatementsWork - {
        // issue #7, just needs to compile
        val tests = TestSuite {
          import math._
          'omg - {
          }
        }
        val res = TestRunner.run(tests).leaves.next().value
        assert(res == Success(()))
      }
      'lexicalScopingWorks - {
        val tests = Tests{
          val x = 1
          'outer - {
            val y = x + 1
            'inner - {
              val z = y + 1
              'innerest - {
                assert(
                  x == 1,
                  y == 2,
                  z == 3
                )
                (x, y, z)
              }
            }
          }
        }
        val results = TestRunner.run(tests)
        assert(results.leaves.count(_.value.isSuccess) == 1)
        results.leaves.map(_.value.get).toList
      }

      'runForking - {
        // Make sure that when you deal with mutable variables in the enclosing
        // scopes, multiple test runs don't affect each other.
        val tests = Tests{
          var x = 0
          'A - {
            x += 1
            'X - {
              x += 2
              assert(x == 3)
              x
            }
            'Y - {
              x += 3
              assert(x == 4)
              x
            }
          }
          'B - {
            x += 4
            'Z - {
              x += 5
              assert(x == 9)
              x
            }
          }
        }
        val results = TestRunner.run(tests)
        assert(results.leaves.count(_.value.isSuccess) == 3)
        results.leaves.map(_.value)
      }
    }

    // These are Fatal in Scala.JS. Ensure they're handled else they freeze SBT.
    'catchCastError - {
      val tests = Tests{
        'ah - {
          // This test is disabled until scala-native/scala-native#858 is not fixed.
          val isNative = sys.props("java.vm.name") == "Scala Native"
          assert(!isNative)
          0.asInstanceOf[String]
        }
      }
      val treeResult = TestRunner.run(tests, query=utest.TestQueryParser("ah"))
      val result = treeResult.leaves.toSeq

      assertMatch(result) {case Seq(Result("ah", Failure(_), _))=>}
    }

    'testSelection - {
      val tests = Tests{
        'A - {
          'C - {1}
        }
        'B - {
          'D - {2}
          'E - {3}
        }
      }

      val res1 = TestRunner.run(tests, query=utest.TestQueryParser("A.C")).leaves.toVector
      assertMatch(res1) {case Seq(Result("C", Success(1), _))=>}

      val res2 = TestRunner.run(tests, query=utest.TestQueryParser("A")).leaves.toVector
      assertMatch(res2) {case Seq(Result("C", Success(1), _))=>}

      assertMatch(TestRunner.run(tests, query=utest.TestQueryParser("B")).leaves.toSeq){case Seq(
        Result("D", Success(2), _),
        Result("E", Success(3), _)
      )=>}
    }
    'outerFailures - {
      // make sure that even when tests themselves fail, test
      // discovery still works and inner tests are visible

      var timesRun = 0

      val tests = Tests{
        timesRun += 1
        "A" - {
          assert(false)
          "B" - {
            "C" - {
              1
            }
          }
        }
      }
      // listing tests B and C works despite failure of A
      assertMatch(tests.nameTree.toSeq){ case Seq(_, "A", "B", "C")=>}
      assert(tests.nameTree.leaves.length == 1)
      val successes = TestRunner.run(tests).leaves.count(_.value.isSuccess)
      // Only the single outer test "C" gets run once, and it results in
      // one failure
      assert(successes == 0)
      assert(timesRun == 1)
      val res = TestRunner.run(tests).leaves.toSeq
      // Check that the right exceptions are thrown
      assertMatch(res){case Seq(
        Result("C", Failure(_: AssertionError), _)
      )=>}
      "timeRun: " + timesRun
    }
    'testPath - {
      'foo - {
        assert(implicitly[utest.framework.TestPath] == TestPath(Seq("testPath", "foo")))
      }
    }
  }
}
