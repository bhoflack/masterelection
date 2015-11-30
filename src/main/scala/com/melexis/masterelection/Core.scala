package com.melexis.masterelection

import scala.collection.mutable
import scala.util.{Try, Success, Failure}


object Core {

  type Ttl = Int
  type Timestamp = Long

  trait LockBackend {

    case class Lock(value: String, ttl: Ttl, created_on: Timestamp)

    def put(name: String, value: String, ttl: Ttl): Try[Unit]
    def get(name: String): Try[Option[Lock]]

    def tryPut(name: String, value: String, ttl: Ttl): Try[Option[String]] =
      this.synchronized {
        get(name) flatMap { 
          case Some(Lock(v, ttl, created_on)) if created_on + ttl > System.currentTimeMillis => Success(None)
          case _ => put(name, value, ttl) flatMap { _ => Success(Some(value)) }
        }
      }
  }

  class InMemoryLockBackend extends LockBackend {
    val items = new mutable.HashMap[String, Lock]

    def put(name: String, value: String, ttl: Int) = Success(items.put(name, Lock(value, ttl, System.currentTimeMillis)))
    def get(name: String) = Success(items.get(name))
  }

  def put(path: String, value: String, ttl: Int)(backend: LockBackend): Try[Option[String]] = 
    if (ttl >= 0) {
      backend.tryPut(path, value, ttl)
    } else {
      Failure(new InvalidTtlException(ttl))
    }
}
