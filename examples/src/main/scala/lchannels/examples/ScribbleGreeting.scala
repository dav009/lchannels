// lchannels - session programming in Scala
// Copyright (c) 2016, Alceste Scalas and Imperial College London
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
/** Implementation of the
 *  [[https://github.com/alcestes/scribble-java/blob/linear-channels/modules/linmp-scala/src/test/scrib/Greeting.scr greeting Scribble protocol]]
 *   
 *  @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
package lchannels.examples.scribblegreeting

import scala.concurrent.{blocking, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import lchannels._

/////////////////////////////////////////////////////////////////////////////
// Global type:
//    μ(X).C->S{Greet(String).S->C{Hello(String).X, Bye(String).end}, Quit(Unit).end}
/////////////////////////////////////////////////////////////////////////////

/** Server-side types (autogenerated by Scribble). */
package object S {
  // Local type for role S:
  //    μ(X).C&{Greet(String).C⊕{Hello(String).X, Bye(String).end}, Quit(Unit).end}
  
  // Input message types for multiparty sessions
  sealed abstract class MsgMPGreetOrQuit
  case class Greet(p: String, cont: MPByeOrHello) extends MsgMPGreetOrQuit
  case class Quit(p: Unit) extends MsgMPGreetOrQuit
  
  // Output message types for multiparty sessions
  case class Hello(p: String)
  case class Bye(p: String)
  
  // Multiparty session classes
  case class MPGreetOrQuit(c: In[binary.GreetOrQuit]) {
    def receive(implicit timeout: Duration = Duration.Inf) = {
      c.receive(timeout) match {
        case m @ binary.Greet(p) => {
          Greet(p, MPByeOrHello(m.cont))
        }
        case m @ binary.Quit(p) => {
          Quit(p)
        }
      }
    }
  }
  case class MPByeOrHello(c: Out[binary.ByeOrHello]) {
    def send(v: Bye) = {
      val cnt = c ! binary.Bye(v.p)
      ()
    }
    def send(v: Hello) = {
      val cnt = c !! binary.Hello(v.p)_
      MPGreetOrQuit(cnt)
    }
  }
}


/** Client-side types (autogenerated by Scribble). */
package object C {
  // Local type for role C:
  //    μ(X).S⊕{Greet(String).S&{Hello(String).X, Bye(String).end}, Quit(Unit).end}
  
  // Input message types for multiparty sessions
  sealed abstract class MsgMPByeOrHello
  case class Hello(p: String, cont: MPGreetOrQuit) extends MsgMPByeOrHello
  case class Bye(p: String) extends MsgMPByeOrHello
  
  // Output message types for multiparty sessions
  case class Greet(p: String)
  case class Quit(p: Unit)
  
  // Multiparty session classes
  case class MPGreetOrQuit(S: Out[binary.GreetOrQuit]) {
    def send(v: Greet) = {
      val cnt = S !! binary.Greet(v.p)_
      MPByeOrHello(cnt)
    }
    def send(v: Quit) = {
      val cnt = S ! binary.Quit(v.p)
      ()
    }
  }
  case class MPByeOrHello(S: In[binary.ByeOrHello]) {
    def receive(implicit timeout: Duration = Duration.Inf) = {
      S.receive(timeout) match {
        case m @ binary.Bye(p) => {
          Bye(p)
        }
        case m @ binary.Hello(p) => {
          Hello(p, MPGreetOrQuit(m.cont))
        }
      }
    }
  }
}

/** Classes representing messages (with continuations) in binary sessions
 *  (autogenerated by Scribble).
 */
package object binary {
  sealed abstract class GreetOrQuit
  case class Greet(p: String)(val cont: Out[ByeOrHello]) extends GreetOrQuit
  case class Quit(p: Unit)                               extends GreetOrQuit
  
  sealed abstract class ByeOrHello
  case class Bye(p: String)                               extends ByeOrHello
  case class Hello(p: String)(val cont: Out[GreetOrQuit]) extends ByeOrHello
}

/** Greeting protocol server implementation. */
object Server {
  import S._
  
  def apply(c: MPGreetOrQuit)
           (implicit timeout: Duration): Unit = {
    println(f"[S] Awaiting request from ${c}...")
    c.receive match {
      case Greet(whom, cont) => {
        println(f"[S] Got 'Greet(${whom})', answering Hello")
        val c2in = cont.send(Hello(whom))
        println("[S] Performing recursion...")
        apply(c2in)
      }
      case Quit(()) => {
        println(f"[S] Got Quit(), finishing")
      }
    }
  }
  
  def serve(factory: () => (In[binary.GreetOrQuit], Out[binary.GreetOrQuit]))
           (implicit ctx: ExecutionContext,
                     timeout: Duration): Out[binary.GreetOrQuit] = {
    val (cin, cout) = factory()
    val session = MPGreetOrQuit(cin) // Wrap binary chan in session object
    Future { blocking { apply(session)(timeout) } }
    cout
  }
  def serve()(implicit ctx: ExecutionContext,
                       timeout: Duration): Out[binary.GreetOrQuit] = {
    serve(() => LocalChannel.factory())
  }
}

/** Greeting protocol client implementation (no. 1). */
object Client1 {
  import C._
  
  def apply(c: MPGreetOrQuit)
           (implicit timeout: Duration): Unit = {
    println("[C1] Sending Greet(\"Jack\")...")
    val repc = c.send(Greet("Jack"))
    println("[C1] ...done.  Now waiting for answer...")
    repc.receive match {
      case Hello(who, cont) => {
        println(f"[C1] Received 'Hello(${who})', now quitting...")
        cont.send(Quit(()))
        println("[C1] ...done.")
      }
      case Bye(who) => {
        println(f"[C1] Received 'Bye(${who})', doing nothing")
      }
    }
  }
}

/** Greeting protocol client implementation (no. 2). */
object Client2 {
  import C._
  
  def apply(c: MPGreetOrQuit)
           (implicit timeout: Duration): Unit = {
    println(f"[C2] Sending ${Quit(())}")
    c.send(Quit(()))
  }
}

/** Client-server demo using local channels */
object Local extends App {
  // Helper method to ease external invocation
  def run() = main(Array())
  
  import scala.concurrent.{Await, Future}
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  
  implicit val timeout = 10.seconds
  
  // Create channel for client/server interaction...
  val (in1, out1) = LocalChannel.factory[binary.GreetOrQuit]
  // ...and spawn server and client (note: we wrap channels in sesison objs)
  println("[*] Spawning server and client 1...")
  val s1 = Future { Server(S.MPGreetOrQuit(in1)) }
  val C1 = Future { Client1(C.MPGreetOrQuit(out1)) }
  
  Await.result(s1, 10.seconds) // Wait for server termination

  // Create channel for client/server interaction...
  val (in2, out2) = LocalChannel.factory[binary.GreetOrQuit]
  // ...and spawn server and client (note: we wrap channels in sesison objs)
  println("[*] Spawning server and client 2...")
  val s2 = Future { Server(S.MPGreetOrQuit(in2)) }
  val c2 = Future { Client2(C.MPGreetOrQuit(out2)) }

  Await.result(s2, 10.seconds) // Wait for server termination
}

/** Client demo, interacing through a textual protocol over sockets */
object SocketClient extends App {
  // Helper method to ease external invocation
  def run() = main(Array())
  
  import java.io.{
    BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter
  }
  import java.net.Socket
  
  implicit val timeout = 30.seconds
    
  class HelloSocketManager(socket: Socket)
        extends SocketManager(socket) {
    import binary._
    
    private val outb = new BufferedWriter(new OutputStreamWriter(out))
    
    override def streamer(x: Any) = x match {
      case Greet(name) => outb.write(f"GREET ${name}\n"); outb.flush()
      case Quit(()) => outb.write("QUIT\n"); outb.flush(); close() // End
    }
    
    private val inb = new BufferedReader(new InputStreamReader(in))
    private val helloR = """HELLO (.+)""".r // Matches Hello(name)
    private val byeR = """BYE (.+)""".r     // Matches Bye(name)
    
    override def destreamer() = inb.readLine() match {
      case helloR(name) => Hello(name)(SocketOut[GreetOrQuit](this))
      case byeR(name) => close(); Bye(name) // Session end: close streams
      case e => { close(); throw new Exception(f"Bad message: '${e}'") }
    }
  }
  
  println("[*] Connecting to 127.0.0.1:1337...")
  val conn = new Socket("127.0.0.1", 1337) // Host & port of greeting server
  val sktm = new HelloSocketManager(conn)
  val c = SocketOut[binary.GreetOrQuit](sktm) // Output endpoint, towards greeting server
  Client1(C.MPGreetOrQuit(c))
}

/** Server demo, using actor-based channels */
object ActorServer extends App {
  // Helper method to ease external invocation
  def run() = main(Array())
  
  import scala.concurrent.duration.Duration
  import scala.concurrent.ExecutionContext.Implicits.global
  import com.typesafe.config.ConfigFactory
  import akka.actor.ActorSystem
  
  val config = ConfigFactory.load() // Loads resources/application.conf
  implicit val as = ActorSystem("GreetingServerSys",
                          config = Some(config.getConfig("GreetingServerSys")),
                          defaultExecutionContext = Some(global))
  
  ActorChannel.setDefaultEC(global)
  ActorChannel.setDefaultAS(as)
  
  implicit val timeout = Duration.Inf
  
  // We give a human-readable name ("greeting") to the server actor
  val (in, out) = ActorChannel.factory[binary.GreetOrQuit]("start");
  println(f"[*] Greeting server listening on: ${out.path}")
  Server(S.MPGreetOrQuit(in))
  
  Thread.sleep(2000) // Just to deliver pending actor messages
  as.terminate()
}

/** Client demo, using actor-based channels */
object ActorClient extends App {
  // Helper method to ease external invocation
  def run() = main(Array())
  
  import scala.concurrent.ExecutionContext.Implicits.global
  import com.typesafe.config.ConfigFactory
  import akka.actor.ActorSystem
  
  val config = ConfigFactory.load() // Loads resources/application.conf
  implicit val as = ActorSystem("GreetingClientSys",
                          config = Some(config.getConfig("GreetingClientSys")),
                          defaultExecutionContext = Some(global))
  
  ActorChannel.setDefaultEC(global)
  ActorChannel.setDefaultAS(as)
  
  implicit val timeout = 10.seconds
  
  val serverPath = "akka.tcp://GreetingServerSys@127.0.0.1:31337/user/start"
  println(f"[*] Connecting to ${serverPath}...")
  val c = ActorOut[binary.GreetOrQuit](serverPath)
  Client1(C.MPGreetOrQuit(c))

  Thread.sleep(2000) // Just to deliver pending actor messages
  // Cleanup and hut down the actor system
  ActorChannel.cleanup()
  as.terminate()
}

