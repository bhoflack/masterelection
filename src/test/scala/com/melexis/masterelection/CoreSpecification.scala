package com.melexis.masterelection

import com.melexis.masterelection.Core.{InMemoryLockBackend, put}

import org.scalacheck.{Gen, Properties}
import org.scalacheck.Prop.forAll

import scala.util.{Failure, Success}

object CoreSpecification extends Properties("Core") {

  def pathGenerator = Gen.listOf(Gen.alphaChar).map(_.mkString)
  def valueGenerator = Gen.alphaStr
  def ttlGenerator = Gen.chooseNum(-100000, 100000)

  property("Only accept positive ttl's") = forAll(pathGenerator, valueGenerator, ttlGenerator) { 
    (path, value, ttl) =>
      val backend = new InMemoryLockBackend
      if (ttl >= 0) {
        put(path, value, ttl)(backend).equals(Success(Some(value)))
      }
      else
        put(path, value, ttl)(backend).equals(Failure(new InvalidTtlException(ttl)))
  }
  
  def positiveTtlGenerator = Gen.chooseNum(100, 500)

  property("Return None when an object is locked") = forAll(pathGenerator, valueGenerator, positiveTtlGenerator) {
    (path, value, ttl) => 
    val backend = new InMemoryLockBackend
    val result = for (
         first <- put(path, value, ttl)(backend);
         second <- put(path, value, ttl)(backend)
       ) yield (first, second)

    result.equals(Success((Some(value), None)))
  }

  def numberOfThreadsGenerator = Gen.chooseNum(1, 100)

  property("Exactly one thread can win the lock") = forAll(pathGenerator, valueGenerator, positiveTtlGenerator, numberOfThreadsGenerator) {
    (path, value, ttl, numberOfThreads) =>
      val backend = new InMemoryLockBackend
      val results = (0 to numberOfThreads).par.map { _ => put(path, value, ttl)(backend) }

      val hasLock = results.filter { case Success(Some(value)) => true
                                     case _ => false }

      hasLock.size == 1
  }

  val notTooLongTtlGenerator = Gen.chooseNum(1, 50)

  property("After the expiration the lock is available again") = forAll(pathGenerator, valueGenerator, valueGenerator, notTooLongTtlGenerator) {
    (path, value, value2, ttl) =>
      val backend = new InMemoryLockBackend
      put(path, value, ttl)(backend) == Success(Some(value))
      Thread.sleep(ttl + 1)
      put(path, value2, ttl)(backend) == Success(Some(value2))
  }
}
