package utest

import scala.util.{Failure, Success}

import utest.framework._
import utest.framework.Result
import scala.util.Success
import scala.util.Failure


object FrameworkTests extends TestSuite{
  implicit val ec = utest.ExecutionContext.RunNow
  def tests = TestSuite{
    def testHelloWorld(test: util.Tree[Test]) = {
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
      val test = TestSuite{
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
      val test = TestSuite{
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
      val test = TestSuite{
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
        val test = TestSuite{
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
        val test = TestSuite{
          "t est1~!@#$%^&*()_+{}|:';<>?,/'"-{
            1
          }
        }
        test.run()

      }
      'testNestedBadly{
        // Ideally should not compile, but until I
        // figure that out, a runtime error works great
        try{
          val test = TestSuite{
            "outer"-{
              if (true){
                "inners"-{

                }
              }
            }
          }
        }catch{case e: IllegalArgumentException =>
          assert(e.getMessage.contains("inners"))
          assert(e.getMessage.contains("nested badly"))
          e.getMessage
        }
      }
    }

    'extractingResults{
     'basic{
        val test = TestSuite{
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
        val tests = TestSuite {
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
        val tests = TestSuite {
          import math._
          'omg{
          }
        }
        val res = tests.run().value.value
        assert(res == Success(()))
      }
      'lexicalScopingWorks{
        val test = TestSuite{
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
        val test = TestSuite{
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

    'testSelection{
      val tests = TestSuite{
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

      val tests = TestSuite{
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
  }
}
