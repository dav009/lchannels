// lchannels - session programming in Scala
// Copyright (c) 2017, Alceste Scalas and Imperial College London
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
//
// * Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

/** Multiparty protocol classes for role Alice in three-buyer example.
 *  The classes in this package have been automatically generated from the
 *  multiparty game protocol:
 *  https://github.com/alcestes/scribble-java/blob/linear-channels/modules/linmp-scala/src/test/scrib/ThreeBuyer.scr
 *  
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk> */
package lchannels.examples.threebuyer.protocol.alice

import scala.concurrent.duration.Duration
import lchannels._
import lchannels.examples.threebuyer.protocol.binary

// Local type for role alice:
//    seller⊕{Title(String).seller&{QuoteA(Int).bob⊕{ShareA(Int).bob&{OkA(Unit).end, QuitA(Unit).end}}}}

// Input message types for multiparty sessions (autogenerated by Scribble)
case class QuoteA(p: Int, cont: MPShareA)
sealed abstract class MsgMPOkAOrQuitA
case class OkA(p: Unit) extends MsgMPOkAOrQuitA
case class QuitA(p: Unit) extends MsgMPOkAOrQuitA

// Output message types for multiparty sessions (autogenerated by Scribble)
case class Title(p: String)
case class ShareA(p: Int)

// Multiparty session classes (autogenerated by Scribble)
case class MPTitle(bob: Out[binary.ShareA], seller: Out[binary.Title]) {
  def send(v: Title) = {
    val cnt = seller !! binary.Title(v.p)_
    MPQuoteA(bob, cnt)
  }
}

case class MPQuoteA(bob: Out[binary.ShareA], seller: In[binary.QuoteA]) {
  def receive(implicit timeout: Duration = Duration.Inf) = {
    seller.receive(timeout) match {
      case m @ binary.QuoteA(p) => {
        QuoteA(p, MPShareA(bob, ()))
      }
    }
  }
}

case class MPShareA(bob: Out[binary.ShareA], seller: Unit) {
  def send(v: ShareA) = {
    val cnt = bob !! binary.ShareA(v.p)_
    MPOkAOrQuitA(cnt, seller)
  }
}

case class MPOkAOrQuitA(bob: In[binary.OkAOrQuitA], seller: Unit) {
  def receive(implicit timeout: Duration = Duration.Inf) = {
    bob.receive(timeout) match {
      case m @ binary.OkA(p) => {
        OkA(p)
      }
      case m @ binary.QuitA(p) => {
        QuitA(p)
      }
    }
  }
}
