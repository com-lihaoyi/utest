package test.utest
import utest._
import scala.util.{Failure, Success}

import utest.framework._
import utest.framework.Result
import scala.util.Success
import scala.util.Failure


object FrameworkTests extends utest.TestSuite{
  implicit val ec = utest.framework.ExecutionContext.RunNow
  def tests = this{
    def testHelloWorld(test: utest.framework.Tree[Test]) = {
      val results = test.run()
      assert(test.length == 4)
      assert(test.leaves.length == 3)
      assert(results.length == 4)
      assert(results.leaves.length == 3)
      assert(results.leaves.count(_.value.isFailure) == 2)
      assert(results.leaves.count(_.value.isSuccess) == 1)
      results.leaves.map(_.value).toList
    }
    'helloWorld{
      val test = this{
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
      testHelloWorld(test)
    }
    'helloWorldSymbol{
      val test = this{
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
      testHelloWorld(test)
    }
    'helloWorldSymbol2{
      val test = this{
        'test1-{
          throw new Exception("test1")
        }
        'test2-1

        'test3-{
          val a = List[Byte](1, 2)
          a(10)
        }
      }
      testHelloWorld(test)
    }

    'failures{
      'noSuchTest{
        val test = this{
          'test1{
            1
          }

        }
        try{
          println(test.run(testPath=Seq("does", "not", "exist")))
        }catch {case e @ NoSuchTestException("does", "not", "exist") =>
          assert(e.getMessage.contains("does.not.exist"))
          e.getMessage
        }
      }
      'weirdTestName{
        val test = this{
          "t est1~!@#$%^&*()_+{}|:';<>?,/'"-{
            1
          }
        }
        test.run()

      }
      'testNestedBadly{
        // Ideally should not compile, but until I
        // figure that out, a runtime error works great
        //
        // This does not compile
//        try{
//          val test = this{
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

    'extractingResults{
     'basic{
        val test = this{
          'test1{
            "i am cow"
          }
          'test2{
            "1"-{
              1
            }
            "2"-{
              2
            }
            999
          }
          'test3{
            Seq('a', 'b')
          }
        }
        val results = test.run()
        val expected = Seq("i am cow", 1, 2, Seq('a', 'b')).map(Success[Any])
        assert(results.leaves.map(_.value).toList == expected)
        results.map(_.value.get)
      }
      'onlyLastThingReturns{
        val tests = this {
          12 + 2
          'omg{
          }
        }
        val res = tests.run().value.value
        assert(res == Success(()))
      }
    }

    'nesting{
      'importStatementsWork{
        // issue #7, just needs to compile
        val tests = this {
          import math._
          'omg{
          }
        }
        val res = tests.run().value.value
        assert(res == Success(()))
      }
      'lexicalScopingWorks{
        val test = this{
          val x = 1
          'outer{
            val y = x + 1
            'inner{
              val z = y + 1
              'innerest{
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
        val results = test.run()
        assert(results.iterator.count(_.value.isSuccess) == 4)
        assert(results.leaves.count(_.value.isSuccess) == 1)
        results.leaves.map(_.value.get).toList
      }

      'runForking{
        // Make sure that when you deal with mutable variables in the enclosing
        // scopes, multiple test runs don't affect each other.
        val test = this{
          var x = 0
          'A{
            x += 1
            'X{
              x += 2
              assert(x == 3)
              x
            }
            'Y{
              x += 3
              assert(x == 4)
              x
            }
          }
          'B{
            x += 4
            'Z{
              x += 5
              assert(x == 9)
              x
            }
          }
        }
        val results = test.run()
        assert(results.leaves.count(_.value.isSuccess) == 3)
        results.map(_.value.get)
      }
    }

    // These are Fatal in Scala.JS. Ensure they're handled else they freeze SBT.
    'catchCastError{
      val tests = this{
        'ah {
          // This test is disabled until scala-native/scala-native#858 is not fixed.
          val isNative = sys.props("java.vm.name") == "Scala Native"
          assert(!isNative)
          0.asInstanceOf[String]
        }
     }
      assertMatch(tests.run(testPath=Seq("ah")).toSeq)
                 {case Seq(Result("ah", Failure(_), _))=>}
    }

    'testSelection{
      val tests = this{
        'A{
          'C{1}
        }
        'B{
          'D{2}
          'E{3}
        }
      }

      assertMatch(tests.run(testPath=Seq("A", "C")).toSeq)
                 {case Seq(Result("C", Success(1), _))=>}

      assertMatch(tests.run(testPath=Seq("A")).toSeq)
                 {case Seq(Result("A", Success(()), _), Result("C", Success(1), _))=>}

      assertMatch(tests.run(testPath=Seq("B")).toSeq){case Seq(
        Result("B", Success(()), _),
        Result("D", Success(2), _),
        Result("E", Success(3), _)
      )=>}
    }
    'outerFailures{
      // make sure that even when tests themselves fail, test
      // discovery still works and inner tests are visible

      var timesRun = 0

      val tests = this{
        timesRun += 1
        "A"-{
          assert(false)
          "B"-{
            "C"-{
              1
            }
          }
        }
      }
      // listing tests B and C works despite failure of A
      assertMatch(tests.toSeq.map(_.name)){ case Seq(_, "A", "B", "C")=>}
      assert(tests.run().iterator.count(_.value.isSuccess) == 1)
      // When a test fails, don't both trying to run any inner tests and just
      // die fail the immediately
      assert(timesRun == 2)
      val res = tests.run().toSeq
      // Check that the right exceptions are thrown
      assertMatch(res){case Seq(
        Result(_, Success(_), _),
        Result("A", Failure(_: AssertionError), _),
        Result("B", Failure(SkippedOuterFailure(Seq("A"), _: AssertionError)), _),
        Result("C", Failure(SkippedOuterFailure(Seq("A"), _: AssertionError)), _)
      )=>}
      "timeRun: " + timesRun
    }
    'testPath{
      'foo {
        assert(implicitly[utest.framework.TestPath] == TestPath(Seq("testPath", "foo")))
      }
    }
  }
}
