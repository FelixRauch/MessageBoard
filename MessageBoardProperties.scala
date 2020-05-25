package at.tugraz.ist.qs2020

import at.tugraz.ist.qs2020.actorsystem.{Message, SimulatedActor}
import at.tugraz.ist.qs2020.messageboard.MessageStore.USER_BLOCKED_AT_COUNT
import at.tugraz.ist.qs2020.messageboard.UserMessage
import at.tugraz.ist.qs2020.messageboard.Worker.MAX_MESSAGE_LENGTH
import at.tugraz.ist.qs2020.messageboard.clientmessages._
import at.tugraz.ist.qs2020.messageboard.dispatchermessages.Stop
import org.junit.Assert
import org.junit.runner.RunWith
import org.scalacheck.Prop.{classify, forAll}
import org.scalacheck.{Arbitrary, Gen, Properties}

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
      classify(message.length <= MAX_MESSAGE_LENGTH, "valid like/dislike", "invalid like/dislike") {
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

  property("R6 Report User Twice") =
    forAll(Gen.nonEmptyListOf(Gen.choose(2,80))) { messages: List[Int] =>
      val sut = new SUTMessageBoard
      sut.getDispatcher.tell(new InitCommunication(sut.getClient, sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      val initAck = sut.getClient.receivedMessages.remove.asInstanceOf[InitAck]
      val worker: SimulatedActor = initAck.worker

      var clientList = List[SimulatedActor]()
      var workerList = List[SimulatedActor]()
      var operationResults = List[Message]()

      //Create one client for each unique object in the generated list
      //println("Set: " + messages.toSet + ", List: " + messages)
      messages.toSet.foreach((x: Int) =>
      {
        val client = new TestClient()
        sut.getSystem.spawn(client)
        clientList = clientList ::: List[SimulatedActor](client)

        sut.getDispatcher.tell(new InitCommunication(client, x))

        while (client.receivedMessages.size == 0)
        {
          sut.getSystem.runFor(1)
        }

        val worker1 = client.receivedMessages.remove.asInstanceOf[InitAck].worker
        workerList = workerList ::: List[SimulatedActor](worker1)

        worker1.tell(new Publish(new UserMessage(x.toString, "Insult"), x))

        while (client.receivedMessages.size == 0)
          sut.getSystem.runFor(1)

        val opAck = client.receivedMessages.remove
        Assert.assertEquals(classOf[OperationAck], opAck.getClass)

      })

      messages.foreach((x: Int) =>
      {
        worker.tell(new Report("client1", 1, x.toString))

        while (sut.getClient.receivedMessages.isEmpty)
          sut.getSystem.runFor(1)

        operationResults = operationResults ::: List[Message](sut.getClient.receivedMessages.remove())
      }
      )

      worker.tell(new FinishCommunication(sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      sut.getClient.receivedMessages.remove()

      //workerList.forall()

      operationResults.indices.forall((i: Int) => operationResults(i).isInstanceOf[OperationAck] == !messages.slice(0, i).contains(messages(i)))
      /*classify(message.length <= MAX_MESSAGE_LENGTH, "valid message length", "invalid message length") {
        (reply1.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH)) && (reply2.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH)) && (reply3.isInstanceOf[OperationAck] == (message.length() <= MAX_MESSAGE_LENGTH))
      }*/

    }


  property("R7 Report User Twice") =
    forAll(Gen.choose(2,10)) { clients: Int =>
      val sut = new SUTMessageBoard
      sut.getDispatcher.tell(new InitCommunication(sut.getClient, sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      val initAck = sut.getClient.receivedMessages.remove.asInstanceOf[InitAck]
      val worker: SimulatedActor = initAck.worker

      var clientList = List[SimulatedActor]()
      var workerList = List[SimulatedActor]()
      var operationResults = List[Message]()

      //Create one client for each unique object in the generated list
      //println("Clients: " + clients)

      val client = new TestClient()
      sut.getSystem.spawn(client)
      clientList = clientList ::: List[SimulatedActor](client)

      sut.getDispatcher.tell(new InitCommunication(client, 2))

      while (client.receivedMessages.size == 0)
      {
        sut.getSystem.runFor(1)
      }
      val op = client.receivedMessages.remove.asInstanceOf[InitAck]
      Assert.assertEquals(classOf[InitAck], op.getClass)
      val worker1 = op.worker

      for(i <- (2 to clients))
      {
        worker1.tell(new Report(i.toString, 2, "client1"))
      }

      worker.tell(new Publish(new UserMessage("client1", "insult"), sut.getCommId))

      while (sut.getClient.receivedMessages.size == 0)
        sut.getSystem.runFor(1)

      val publishOperation = sut.getClient.receivedMessages.remove

      //println("publish " + publishOperation)

      worker.tell(new Like("client1", sut.getCommId, 0))

      while (sut.getClient.receivedMessages.size == 0)
        sut.getSystem.runFor(1)

      val likeOperation = sut.getClient.receivedMessages.remove
      //println("like " + likeOperation)

      worker.tell(new Dislike("client1", sut.getCommId, 0))

      while (sut.getClient.receivedMessages.size == 0)
        sut.getSystem.runFor(1)

      val dislikeOperation = sut.getClient.receivedMessages.remove
      //println("dislike " + dislikeOperation)

      worker.tell(new Report("client1", sut.getCommId, "2"))

      while (sut.getClient.receivedMessages.size == 0)
        sut.getSystem.runFor(1)

      val reportOperation = sut.getClient.receivedMessages.remove

      worker.tell(new FinishCommunication(sut.getCommId))
      while (sut.getClient.receivedMessages.isEmpty)
        sut.getSystem.runFor(1)
      sut.getClient.receivedMessages.remove()

      classify(clients >= USER_BLOCKED_AT_COUNT + 1, "blocked", "not blocked") {
        ((publishOperation.isInstanceOf[OperationAck] && likeOperation.isInstanceOf[OperationAck] && dislikeOperation.isInstanceOf[OperationAck] && reportOperation.isInstanceOf[OperationAck]) == clients < USER_BLOCKED_AT_COUNT + 1)
      }
    }

}
