package utest

import utest.framework.Tree

import scala.collection.mutable

/**
  * Parses query strings.
  *
  * Fastparse grammar:
  *
  * val quoted: P[String] = P("\"" ~/ CharsWhile(_ != '"').! ~ "\"")
  * val ident: P[String] = P(CharIn('a' to 'z', 'A' to 'Z', "_-").rep(1).!)
  * val item: P[String] = P(quoted | ident)
  * val chain: P[Any] = P(item.rep(1, sep="." ~/) ~ ("." ~/ parseCurlies).?)
  * val commas: P[Any] = P(chain.rep(1, sep=","~/))
  * val curlies: P[Any] = P(commas | "{" ~/ curlies ~ "}")
  *
  * But here written manually to avoid the dependency on FastParse.
  */
class TestQueryParser(input: String) {

  type Parsed[T] = Either[String, (T, Int)]
  type Trees = Seq[Tree[String]]
  def curlies(index: Int): Parsed[Trees] = {
    input.lift(index) match {
      case Some('{') =>
        curlies(index + 1) match{
          case Right((v, i)) => input.lift(i)  match{
            case Some('}') => Right((v, i + 1))
            case _ => Left(s"Expected closing bracket at index $i")
          }
          case l => l
        }
      case Some(c) => commas(index) match{
        case Right((v, i)) => Right((v, i))
        case Left(e) => Left(e)
      }
      case None => Left(s"Unexpected end of input at index $index")
    }
  }

  def repSep[T](index0: Int, p: Int => Parsed[T], sep: Char): Parsed[Seq[T]] = {
    def rec(index: Int, acc: List[T]): Parsed[Seq[T]] = {
      p(index) match{
        case Right((v, i)) =>
          val newAcc = v :: acc
          input.lift(i) match{
            case Some(`sep`) => rec(i+1, newAcc)
            case _ => Right((newAcc.reverse, i))
          }
        case Left(e) if index == index0 => Left(e)
        case _ => Right((acc.reverse, index-1))
      }
    }
    rec(index0, Nil)
  }
  def commas(index: Int): Parsed[Trees] = repSep(index, chain, ',')
  def treeify(s: Seq[String], end: Trees): Tree[String] = {
    s.reverseIterator.foldLeft(end)(
      (children, value) => List(Tree(value, children:_*))
    ).head
  }
  def chain(index: Int): Parsed[Tree[String]] = repSep(index, item, '.') match{
    case Left(e) => Left(e)
    case Right((v, i)) =>
      if (input.lift(i) == Some('.')) curlies(i+1) match{
        case Right((v2, i2)) => Right(treeify(v, v2), i2)
        case Left(e) => Left(e)
      } else Right((treeify(v, Nil), i))
  }

  def item(index: Int): Parsed[String] = {
    input.lift(index) match{
      case Some('"') =>
        input.indexOf('"', index+1) match {
          case -1 => Left(s"Unclosed quote at index $index")
          case n => Right(input.substring(index + 1, n), n+1)
        }
      case Some(c) if delimiters(c) => Left(s"Expected non-delimiter at index $index")
      case Some(c) =>
        input.indexWhere(delimiters.contains, index) match{
          case -1 =>
            if (index == input.length) Left(s"Expected identifier ati ndex $index}")
            else Right(input.substring(index, input.length), input.length)
          case n =>
            if (index == n) Left(s"Expected identifier at index $index")
            else Right(input.substring(index, n), n)
        }
      case None => Left(s"Unexpected end of input at index $index")
    }
  }

  val delimiters = "{}.,".toSet
}

object TestQueryParser{
  def apply(input: String) = {
    parse(input).fold(e => throw new QueryParseError(input, e), x => x)
  }
  def parse(input: String) = new TestQueryParser(input).curlies(0) match{
    case Right((v, i)) =>
      if (i == input.length) Right(collapse(v))
      else Left(s"Expected end of input at index $i")
    case Left(e) => Left(e)
  }

  /**
    * Combine common prefixes, converting
    *
    * {foo.bar,foo.baz}
    *
    * into
    *
    * foo.{bar,baz}
    */
  def collapse(input: TestQueryParser#Trees): TestQueryParser#Trees = {
    val mapping = mutable.Map.empty[String, Int]
    val ordered = mutable.Buffer.empty[List[Tree[String]]]
    for(subtree <- input){
      mapping.get(subtree.value) match{
        case None =>
          mapping(subtree.value) = ordered.length
          ordered.append(List(subtree))
        case Some(i) =>
          ordered(i) = subtree :: ordered(i)
      }
    }
    (for (grouping <- ordered) yield {
      Tree(grouping.head.value,
        collapse(grouping.reverse.flatMap(_.children)):_*
      )
    }).toSeq
  }
}
case class QueryParseError(input: String, msg: String)
  extends Exception("[" + input + "] " + msg)
