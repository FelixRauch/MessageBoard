package at.tugraz.ist.qs2020

import at.tugraz.ist.qs2020.actorsystem.{Message, SimulatedActor}
import at.tugraz.ist.qs2020.messageboard.MessageStore.USER_BLOCKED_AT_COUNT
import at.tugraz.ist.qs2020.messageboard.UserMessage
import at.tugraz.ist.qs2020.messageboard.Worker.MAX_MESSAGE_LENGTH
import at.tugraz.ist.qs2020.messageboard.clientmessages._
import org.junit.runner.RunWith
import org.scalacheck.Prop.{classify, forAll}
import org.scalacheck.{Gen, Properties}

import scala.jdk.CollectionConverters._

@RunWith(classOf[ScalaCheckJUnitPropertiesRunner])
class MessageBoardProperties extends Properties("MessageBoardProperties") {

  val validMessageGen: Gen[String] = Gen.asciiPrintableStr.map(s =>
    if (s.length <= MAX_MESSAGE_LENGTH) s else s.substring(0, MAX_MESSAGE_LENGTH)
  )
  property("message length: Publish + Ack [R1]") = forAll { (author: String, message: String) =>
    val sut = new SUTMessageBoard
    sut.getDispatcher.tell(new InitCommunication(sut.getClient, sut.getCommId))
    while (sut.getClient.receivedMessages.isEmpty)
      sut.getSystem.runFor(1)
    val initAck = sut.getClient.receivedMessages.remove.asInstanceOf[InitAck]
    val worker: SimulatedActor = initAck.worker

    worker.tell(new Publish(new UserMessage(author, message), sut.getCommId))
    while (sut.getClient.receivedMessages.isEmpty)
      sut.getSystem.runFor(1)
    val reply = sut.getClient.receivedMessages.remove()

    worker.tell(new FinishCommunication(sut.getCommId))
    while (sut.getClient.receivedMessages.isEmpty)
      sut.getSystem.runFor(1)
    sut.getClient.receivedMessages.remove.asInstanceOf[FinishAck]

    // The following classify is optional, it prints stats on the generated values.
    // But the check inside is required.
    classify(message.length <= MAX_MESSAGE_LENGTH, "valid message length", "invalid message length") {
      reply.isInstanceOf[OperationAck] == message.length <= MAX_MESSAGE_LENGTH
    }
  }

  property("R5 Retrieve Message") =
    forAll(Gen.alphaStr, Gen.nonEmptyListOf(validMessageGen)) { (author: String, messages: List[String]) =>
      val sut = new SUTMessageBoard
      sut.getDispatcher.tell(new InitCommunication(sut.getClient, sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      val initAck = sut.getClient.receivedMessages.remove.asInstanceOf[InitAck]
      val worker: SimulatedActor = initAck.worker

      var publishedMessages = List[String]()

      for (i <- messages.indices) {
      worker.tell(new Publish(new UserMessage(author, messages(i)), sut.getCommId))
        while (sut.getClient.receivedMessages.isEmpty)
          sut.getSystem.runFor(1)
        val reply = sut.getClient.receivedMessages.remove()

        if(reply.isInstanceOf[OperationAck])
          {
            publishedMessages = publishedMessages ::: List[String](messages(i))
          }
      }

      //println("publishedMessages: " + publishedMessages.)

      worker.tell(new RetrieveMessages(author, sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      val reply = sut.getClient.receivedMessages.remove()
      val retrievedMessages = reply.asInstanceOf[FoundMessages].messages


      // here would be a worker.tell, e.g. in a loop

      worker.tell(new FinishCommunication(sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      sut.getClient.receivedMessages.remove()


      // here would be a check

      retrievedMessages.asScala.forall((message: UserMessage) => publishedMessages.contains(message.getMessage)) && publishedMessages.size == retrievedMessages.size()

    }

  property("R2 Publish Same Message Twice") =
      forAll(Gen.alphaStr, Gen.nonEmptyListOf(validMessageGen)) { (author: String, messages: List[String]) =>
        val sut = new SUTMessageBoard
        sut.getDispatcher.tell(new InitCommunication(sut.getClient, sut.getCommId))
        while (sut.getClient.receivedMessages.isEmpty)
          sut.getSystem.runFor(1)
        val initAck = sut.getClient.receivedMessages.remove.asInstanceOf[InitAck]
        val worker: SimulatedActor = initAck.worker

        var publishedMessages = List[String]()

        for (i <- messages.indices) {
          worker.tell(new Publish(new UserMessage(author, messages(i)), sut.getCommId))
          worker.tell(new Publish(new UserMessage(author, messages(i)), sut.getCommId))
          while (sut.getClient.receivedMessages.size() < 2)
            sut.getSystem.runFor(1)
          val reply1 = sut.getClient.receivedMessages.remove()
          val reply2 = sut.getClient.receivedMessages.remove()

          if(reply1.isInstanceOf[OperationAck])
          {
            publishedMessages = publishedMessages ::: List[String](messages(i))
          }

          assert(reply2.isInstanceOf[OperationFailed])
        }

        //println("publishedMessages: " + publishedMessages.)

        worker.tell(new RetrieveMessages(author, sut.getCommId))
        while (sut.getClient.receivedMessages.isEmpty)
          sut.getSystem.runFor(1)
        val reply = sut.getClient.receivedMessages.remove()
        val retrievedMessages = reply.asInstanceOf[FoundMessages].messages


        // here would be a worker.tell, e.g. in a loop

        worker.tell(new FinishCommunication(sut.getCommId))
        while (sut.getClient.receivedMessages.isEmpty)
          sut.getSystem.runFor(1)
        sut.getClient.receivedMessages.remove()


        // here would be a check

        retrievedMessages.asScala.forall((message: UserMessage) => publishedMessages.contains(message.getMessage)) && publishedMessages.size == retrievedMessages.size()

      }

  property("R3 Like/Dislike Existing Message") =
    forAll { (author: String, message: String) =>
      val sut = new SUTMessageBoard
      sut.getDispatcher.tell(new InitCommunication(sut.getClient, sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      val initAck = sut.getClient.receivedMessages.remove.asInstanceOf[InitAck]
      val worker: SimulatedActor = initAck.worker

      var publishedMessages = List[String]()

        val msg = new UserMessage(author, message)
        worker.tell(new Publish(msg, sut.getCommId))
        while (sut.getClient.receivedMessages.isEmpty)
          sut.getSystem.runFor(1)
        val reply1 = sut.getClient.receivedMessages.remove()

        worker.tell(new Like("TestClient", sut.getCommId, msg.getMessageId))
        while (sut.getClient.receivedMessages.isEmpty)
          sut.getSystem.runFor(1)
        val reply2 = sut.getClient.receivedMessages.remove()

        worker.tell(new Dislike("TestClient", sut.getCommId, msg.getMessageId))
        while (sut.getClient.receivedMessages.isEmpty)
          sut.getSystem.runFor(1)
        val reply3 = sut.getClient.receivedMessages.remove()


      //println("publishedMessages: " + publishedMessages.)

      // here would be a worker.tell, e.g. in a loop

      worker.tell(new FinishCommunication(sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      sut.getClient.receivedMessages.remove()

      // here would be a check
      classify(message.length <= MAX_MESSAGE_LENGTH, "valid message length", "invalid message length") {
        (reply1.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH)) && (reply2.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH)) && (reply3.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH))
      }
    }
  property("R3 Like/Dislike Existing Message twice") =
    forAll { (author: String, message: String) =>
      val sut = new SUTMessageBoard
      sut.getDispatcher.tell(new InitCommunication(sut.getClient, sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      val initAck = sut.getClient.receivedMessages.remove.asInstanceOf[InitAck]
      val worker: SimulatedActor = initAck.worker

      var publishedMessages = List[String]()

      val msg = new UserMessage(author, message)
      worker.tell(new Publish(msg, sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      val reply1 = sut.getClient.receivedMessages.remove()

      worker.tell(new Like("TestClient", sut.getCommId, msg.getMessageId))
      worker.tell(new Like("TestClient", sut.getCommId, msg.getMessageId))
      while (sut.getClient.receivedMessages.size() < 2)
        sut.getSystem.runFor(1)
      val reply2 = sut.getClient.receivedMessages.remove()
      val reply2_2 = sut.getClient.receivedMessages.remove()
      assert(reply2_2.isInstanceOf[OperationFailed])

      worker.tell(new Dislike("TestClient", sut.getCommId, msg.getMessageId))
      worker.tell(new Dislike("TestClient", sut.getCommId, msg.getMessageId))
      while (sut.getClient.receivedMessages.size() < 2)
        sut.getSystem.runFor(1)
      val reply3 = sut.getClient.receivedMessages.remove()
      val reply3_2 = sut.getClient.receivedMessages.remove()
      assert(reply3_2.isInstanceOf[OperationFailed])

      //println("publishedMessages: " + publishedMessages.)

      // here would be a worker.tell, e.g. in a loop

      worker.tell(new FinishCommunication(sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      sut.getClient.receivedMessages.remove()

      // here would be a check
      classify(message.length <= MAX_MESSAGE_LENGTH, "valid message length", "invalid message length") {
        (reply1.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH)) && (reply2.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH)) && (reply3.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH))
      }

    }

}
