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

/** Binary protocol classes for the three-buyer example.
 *  The classes in this package have been automatically generated from the
 *  multiparty game protocol:
 *  https://github.com/alcestes/scribble-java/blob/linear-channels/modules/linmp-scala/src/test/scrib/ThreeBuyer.scr
 *  
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk> */
package lchannels.examples.threebuyer.protocol.binary

import lchannels._
import lchannels.examples.threebuyer.protocol.bob.MPOkAOrQuitA

import java.time.ZonedDateTime

// Classes representing messages (with continuations) in binary sessions,
// for the main protocol (autogenerated by Scribble)
case class ShareA(p: Int)(val cont: Out[OkAOrQuitA])

sealed abstract class OkAOrQuitA
case class OkA(p: Unit) extends OkAOrQuitA
case class QuitA(p: Unit) extends OkAOrQuitA

case class Title(p: String)(val cont: Out[QuoteA])
case class QuoteA(p: Int)
case class QuoteB(p: Int)(val cont: Out[OkSOrQuitS])

sealed abstract class OkSOrQuitS
case class OkS(p: Unit)(val cont: In[Address]) extends OkSOrQuitS
case class QuitS(p: Unit) extends OkSOrQuitS

case class Address(p: String)(val cont: Out[Deliver])
case class Deliver(p: ZonedDateTime)

// Classes representing messages (with continuations) in binary sessions,
// for the delegation protocol (autogenerated by Scribble)
//
// TODO: these could be moved to a package only used by Bob and Carol
case class Contrib(p: Int)(val cont: In[Delegate])
case class Delegate(p: MPOkAOrQuitA)(val cont: Out[OkCOrQuitC])
sealed abstract class OkCOrQuitC
case class OkC(p: Unit) extends OkCOrQuitC
case class QuitC(p: Unit) extends OkCOrQuitC

/** Binary protocol classes for establishing binary connections
 *  (used e.g. in the actor-based demo)
 */
package object actor {
  case class ConnectA()(val cont: Null)
  case class ConnectB()(val cont: Null)
  case class ConnectC()(val cont: Null)
}
