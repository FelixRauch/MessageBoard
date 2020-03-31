package at.tugraz.ist.qs2020;

import at.tugraz.ist.qs2020.actorsystem.Message;
import at.tugraz.ist.qs2020.actorsystem.SimulatedActor;
import at.tugraz.ist.qs2020.actorsystem.SimulatedActorSystem;
import at.tugraz.ist.qs2020.messageboard.*;
import at.tugraz.ist.qs2020.messageboard.clientmessages.*;
import at.tugraz.ist.qs2020.messageboard.dispatchermessages.Stop;
import at.tugraz.ist.qs2020.messageboard.messagestoremessages.MessageStoreMessage;
import at.tugraz.ist.qs2020.messageboard.messagestoremessages.RetrieveFromStore;
import org.junit.Assert;
import org.junit.Test;

import javax.jws.soap.SOAPBinding;
import java.util.*;

/**
 * Simple actor, which can be used in tests, e.g. to check if the correct messages are sent by workers.
 * This actor can be sent to workers as client.
 */
class TestClient extends SimulatedActor {

    /**
     * Messages received by this actor.
     */
    final Queue<Message> receivedMessages;

    TestClient() {
        receivedMessages = new LinkedList<>();
    }

    /**
     * does not implement any logic, only saves the received messages
     *
     * @param message Non-null message received
     */
    @Override
    public void receive(Message message) {
        receivedMessages.add(message);
    }
}

class TestDispatcher extends Dispatcher {

    public TestDispatcher(SimulatedActorSystem system, int numberOfWorkers) {
        super(system, numberOfWorkers);
    }

    public MessageStore getMsgStore() {
        return super.messageStore;
    }
}

/*class TestWorker extends Worker {

    public TestWorker(Worker worker)
    {
        this.dispatcher = worker.dispatcher;
        this.messageStore = messageStore;
        this.ongoingCommunications = new HashMap<>();
        this.system = system;
        this.stopping = false;
    }
}*/

/*class TestWorkerHelper extends WorkerHelper
{
    public WorkerHelper workerHelper;
    public TestWorkerHelper(WorkerHelper workerHelper)
    {
        super(null,null,null,null);
        this.workerHelper = workerHelper;
        workerHelper.message;
    }
}*/

class TestMessage extends MessageStoreMessage {

    public TestMessage() {
    }
}
public class MessageBoardTests {

    @Test
    public void testInvalidFinishCommunication() throws UnknownClientException {
        // testing only the acks
        SimulatedActorSystem system = new SimulatedActorSystem();
        Dispatcher dispatcher = new Dispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client = new TestClient();
        system.spawn(client);

        // send request and run system until a response is received
        // communication id is chosen by clients

        Assert.assertEquals(0,dispatcher.getTimeSinceSystemStart());
        system.runFor(1);
        Assert.assertEquals(1,dispatcher.getTimeSinceSystemStart());

        dispatcher.tell(new InitCommunication(client, 10));

        while (client.receivedMessages.size() == 0)
            system.runFor(1);

        Message initAckMessage = client.receivedMessages.remove();
        Assert.assertEquals(InitAck.class, initAckMessage.getClass());

        InitAck initAck = (InitAck) initAckMessage;
        Assert.assertEquals(Long.valueOf(10), initAck.communicationId);
        SimulatedActor worker = initAck.worker;

        // end the communication with wrong communication id
        worker.tell(new FinishCommunication(-1));
        Assert.assertThrows(UnknownClientException.class, () -> {
            while (client.receivedMessages.size() == 0)
                system.runFor(1);
        });


        //-----End Routine

        worker.tell(new FinishCommunication(10));
        while (client.receivedMessages.size() == 0)
            system.runFor(1);

        Message finAckMessage = client.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage.getClass());
        FinishAck finAck = (FinishAck) finAckMessage;

        Assert.assertEquals(Long.valueOf(10), finAck.communicationId);

        dispatcher.tell(new Stop());


        while (system.getActors().size() > 1)
            system.runFor(1);
        system.stop(dispatcher);

    }

    @Test
    public void testSystemTime() throws UnknownClientException {
        // testing only the acks
        SimulatedActorSystem system = new SimulatedActorSystem();
        Dispatcher dispatcher = new Dispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client = new TestClient();
        system.spawn(client);

        // send request and run system until a response is received
        // communication id is chosen by clients

        Assert.assertEquals(0,dispatcher.getTimeSinceSystemStart());
        system.runFor(1);
        Assert.assertEquals(1,dispatcher.getTimeSinceSystemStart());

        Assert.assertEquals(1, system.getCurrentTime());
        int currentTime = system.getCurrentTime();
        system.runUntil(system.getCurrentTime() + 5);
        Assert.assertEquals(system.getCurrentTime(), currentTime + 6); // runUntil runs till endtime is over so current time is endtime + 1

        dispatcher.tell(new Stop());


        while (system.getActors().size() > 1)
            system.runFor(1);
        system.stop(dispatcher);

    }

    @Test
    public void testCommAfterStop() throws UnknownClientException {
        // testing only the acks
        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        TestClient client2 = new TestClient();
        system.spawn(client1);
        system.spawn(client2);

        dispatcher.tell(new InitCommunication(client1, 1));
        dispatcher.tell(new InitCommunication(client2, 20));

        while (client1.receivedMessages.size() == 0 ||
                client2.receivedMessages.size() == 0)
            system.runFor(1);

        //get workers for each client

        SimulatedActor worker1 = ((InitAck) client1.receivedMessages.remove()).worker;
        SimulatedActor worker2 = ((InitAck) client2.receivedMessages.remove()).worker;

        // test with wrong communication id

        worker1.tell(new Stop());

        worker1.tell(new Publish(new UserMessage(client1.toString(), "Test Msg"), -1));

        Assert.assertThrows(UnknownClientException.class, () -> {
            while (client1.receivedMessages.size() == 0)
                system.runFor(1);
        });

        // test with correct communication id

        worker2.tell(new Stop());

        worker2.tell(new Publish(new UserMessage(client2.toString(), "Test Msg"), 20));


        while (client2.receivedMessages.size() == 0)
            system.runFor(1);

        Message opAck = client2.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, opAck.getClass());

        dispatcher.tell(new Stop());

        while (system.getActors().size() > 2)
            system.runFor(1);
        system.stop(dispatcher);

    }

    @Test
    public void publishTest() throws UnknownClientException {


        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        system.spawn(client1);

        //Get access to private fields

        dispatcher.tell(new InitCommunication(client1, 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        SimulatedActor worker1 = ((InitAck) client1.receivedMessages.remove()).worker;

        //Publish Message and check for correct handling

        worker1.tell(new Publish(new UserMessage("client1", "Test Msg"), 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        Message opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        //Check if Message has been processed by worker

        UserMessage userMessage = ((Publish) worker1.getMessageLog().get(worker1.getMessageLog().size() - 1)).message;

        Assert.assertEquals(userMessage.getMessage(), "Test Msg");
        Assert.assertEquals(userMessage.getMessageId(), 0);
        Assert.assertEquals(userMessage.getAuthor(), "client1");

        //Check if Message has been delivered successfully

        worker1.tell(new RetrieveMessages("client1", 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        Message operation = client1.receivedMessages.remove();
        Assert.assertEquals(FoundMessages.class, operation.getClass());

        List<UserMessage> messages = ((FoundMessages) operation).messages;

        Assert.assertEquals(messages.get(0).getMessage(), "Test Msg");
        Assert.assertEquals(messages.get(0).getMessageId(), 0);
        Assert.assertEquals(messages.get(0).getAuthor(), "client1");

        //Publish Same message twice

        worker1.tell(new Publish(new UserMessage("client1", "Test Msg"), 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, operation.getClass());

        //Publish another message by same user

        worker1.tell(new Publish(new UserMessage("client1", "Test Msg1"), 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, operation.getClass());

        //Publish Same message by other user

        worker1.tell(new Publish(new UserMessage("client2", "Test Msg"), 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, operation.getClass());

        //Cause invalid communication id error (UnknownClientException)

        worker1.tell(new Publish(new UserMessage("client1", "Test Msg"), -1));

        Assert.assertThrows(UnknownClientException.class, () -> {
                    while (client1.receivedMessages.size() == 0)
                        system.runFor(1);
                }
        );

        //cause OperationFailed error (all causes)

        // Wrong Message id

        // -Message too long

        worker1.tell(new Publish(new UserMessage("client1", "Test Msg123456"), 1));
        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        Message opFailed = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, opFailed.getClass());


        // -Likes > 0
        worker1.tell(new Like("client1", 1, 0));
        worker1.tell(new RetrieveMessages("client1", 1));

        while (client1.receivedMessages.size() < 2)
            system.runFor(1);

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(FoundMessages.class, operation.getClass());

        messages = ((FoundMessages) operation).messages;
        Assert.assertEquals(messages.size(), 2);
        Publish publish = new Publish(messages.get(0), 1);

        worker1.tell(publish);
        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        opFailed = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, opFailed.getClass());


        // -Dislikes > 0
        worker1.tell(new Publish(new UserMessage("client1", "HasDislike"), 1));
        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        worker1.tell(new Dislike("client1", 1, 1));
        worker1.tell(new RetrieveMessages("client1", 1));

        while (client1.receivedMessages.size() < 2)
            system.runFor(1);

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(FoundMessages.class, operation.getClass());

        messages = ((FoundMessages) operation).messages;
        Assert.assertEquals(messages.size(), 3);
        publish = new Publish(messages.get(1), 1);

        worker1.tell(publish);
        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        opFailed = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, opFailed.getClass());


        // Publish retrieved message again

        worker1.tell(new Publish(new UserMessage("client1", "Copy"), 1));
        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        worker1.tell(new RetrieveMessages("client1", 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(FoundMessages.class, operation.getClass());

        messages = ((FoundMessages) operation).messages;
        Assert.assertEquals(messages.size(), 4);
        publish = new Publish(messages.get(3), 1);

        worker1.tell(publish);
        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        opFailed = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, opFailed.getClass());


        //-----End Routine


        worker1.tell(new FinishCommunication(1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        Message finAckMessage = client1.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage.getClass());
        FinishAck finAck = (FinishAck) finAckMessage;

        Assert.assertEquals(Long.valueOf(1), finAck.communicationId);

        dispatcher.tell(new Stop());

        dispatcher.tell(new InitCommunication(client1, 1));


        while (system.getActors().size() > 1)
            system.runFor(1);
        system.stop(dispatcher);

    }

    @Test
    public void LikeDislikeTest() throws UnknownClientException {

        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        system.spawn(client1);

        dispatcher.tell(new InitCommunication(client1, 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        SimulatedActor worker1 = ((InitAck) client1.receivedMessages.remove()).worker;

        //Publish Message and check for correct handling

        worker1.tell(new Publish(new UserMessage("client1", "LikeTest"), 1));


        // Adding valid Likes
        worker1.tell(new Like("TestClient", 1, 0));

        worker1.tell(new Dislike("TestClient", 1, 0));

        worker1.tell(new RetrieveMessages("client1", 1));

        while( client1.receivedMessages.size() < 4) {
            system.runFor(1);
        }

        Message opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        Message operation = client1.receivedMessages.remove();
        Assert.assertEquals(FoundMessages.class, operation.getClass());

        List<UserMessage> messages = ((FoundMessages) operation).messages;
        Assert.assertEquals(messages.size(), 1);

        Assert.assertEquals(messages.get(0).getLikes().get(0), "TestClient");

        Assert.assertEquals(messages.get(0).getDislikes().get(0), "TestClient");

        Assert.assertEquals(messages.get(0).toString(), "client1:LikeTest liked by :TestClient disliked by :TestClient");

        // Adding invalid Likes (wrong communication id)

        worker1.tell(new Like("TestClient", -1, 0));

        Assert.assertThrows(UnknownClientException.class, () -> {
            while (client1.receivedMessages.size() == 0)
                system.runFor(1);
        });

        // Adding like with invalid mId
        worker1.tell(new Like("TestClient", 1, 20));

        //Try to like twice by same user
        worker1.tell(new Like("TestClient", 1, 0));

        while (client1.receivedMessages.size() < 2) {
            system.runFor(1);
        }

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, operation.getClass());

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, operation.getClass());

        // Adding invalid Dislike

        worker1.tell(new Dislike("TestClient", -1, 0));

        Assert.assertThrows(UnknownClientException.class, () -> {
            while (client1.receivedMessages.size() == 0)
                system.runFor(1);
        });

        // Adding like with invalid mId
        worker1.tell(new Dislike("TestClient", 1, 20));

        //Try to like twice by same user
        worker1.tell(new Dislike("TestClient", 1, 0));

        while (client1.receivedMessages.size() < 2) {
            system.runFor(1);
        }

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, operation.getClass());

        operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, operation.getClass());

        //-----End Routine
        worker1.tell(new FinishCommunication(1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        Message finAckMessage = client1.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage.getClass());
        FinishAck finAck = (FinishAck) finAckMessage;

        Assert.assertEquals(Long.valueOf(1), finAck.communicationId);

        dispatcher.tell(new Stop());

        dispatcher.tell(new InitCommunication(client1, 1));


        while (system.getActors().size() > 1)
            system.runFor(1);
        system.stop(dispatcher);
    }

    @Test
    public void RetrieveMsgTest() throws UnknownClientException {
        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        TestClient client2 = new TestClient();
        system.spawn(client1);
        system.spawn(client2);

        dispatcher.tell(new InitCommunication(client1, 1));
        dispatcher.tell(new InitCommunication(client2, 2));

        while (client1.receivedMessages.size() == 0 ||
                client2.receivedMessages.size() == 0)
            system.runFor(1);

        //get workers for each client

        SimulatedActor worker1 = ((InitAck) client1.receivedMessages.remove()).worker;
        SimulatedActor worker2 = ((InitAck) client2.receivedMessages.remove()).worker;

        // client 1 publishes 3 messages

        worker1.tell(new Publish(new UserMessage("client1", "TestMsg1"), 1));
        worker1.tell(new Publish(new UserMessage("client1", "TestMsg2"), 1));
        worker1.tell(new Publish(new UserMessage("client1", "TestMsg3"), 1));

        while (client1.receivedMessages.size() < 3)
            system.runFor(1);

        Message opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());


        // client2 retrieve Messages

        worker2.tell(new RetrieveMessages("client1", 2));

        while (client2.receivedMessages.size() == 0)
            system.runFor(1);

        Message operation = client2.receivedMessages.remove();
        Assert.assertEquals(FoundMessages.class, operation.getClass());

        List<UserMessage> retrievedMessages = ((FoundMessages) operation).messages;

        Assert.assertEquals(retrievedMessages.size(), 3);

        Assert.assertEquals(retrievedMessages.get(0).getMessage(), "TestMsg1");
        Assert.assertEquals(retrievedMessages.get(1).getMessage(), "TestMsg2");
        Assert.assertEquals(retrievedMessages.get(2).getMessage(), "TestMsg3");
        //Assert.assertEquals(retrievedMessages.get(0).toString());

        Assert.assertEquals(retrievedMessages.get(0).getAuthor(), "client1");
        Assert.assertEquals(retrievedMessages.get(1).getAuthor(), "client1");
        Assert.assertEquals(retrievedMessages.get(2).getAuthor(), "client1");

        // Try to retrieve message with wrong parameters (UnknownClientException)

        worker2.tell(new RetrieveMessages("client1", -1));

        Assert.assertThrows(UnknownClientException.class, () -> {
            while (client2.receivedMessages.size() == 0)
                system.runFor(1);
        });

        // Try to retrieve message from invalid author

        worker2.tell(new RetrieveMessages("InvalidClient", 2));

        while (client2.receivedMessages.size() == 0)
            system.runFor(1);

        operation = client2.receivedMessages.remove();
        Assert.assertEquals(FoundMessages.class, operation.getClass());

        Assert.assertEquals(((FoundMessages)operation).messages.size(), 0);

        //-----End Routine

        worker1.tell(new FinishCommunication(1));
        worker2.tell(new FinishCommunication(2));

        while (client1.receivedMessages.size() == 0 || client2.receivedMessages.size() == 0)
            system.runFor(1);

        // Acknowledge communication end

        Message finAckMessage1 = client1.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage1.getClass());
        FinishAck finAck1 = (FinishAck) finAckMessage1;
        Assert.assertEquals(Long.valueOf(1), finAck1.communicationId);

        Message finAckMessage2 = client2.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage2.getClass());
        FinishAck finAck2 = (FinishAck) finAckMessage2;
        Assert.assertEquals(Long.valueOf(2), finAck2.communicationId);

        dispatcher.tell(new Stop());

        while (system.getActors().size() > 2)
            system.runFor(1);
        system.stop(dispatcher);
    }

    @Test
    public void ReportTest() throws UnknownClientException {
        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        TestClient client2 = new TestClient();
        system.spawn(client1);
        system.spawn(client2);


        dispatcher.tell(new InitCommunication(client1, 1));
        dispatcher.tell(new InitCommunication(client2, 2));

        while (client1.receivedMessages.size() == 0 ||
                client2.receivedMessages.size() == 0)
            system.runFor(1);

        //get workers for each client

        SimulatedActor worker1 = ((InitAck) client1.receivedMessages.remove()).worker;
        SimulatedActor worker2 = ((InitAck) client2.receivedMessages.remove()).worker;

        // client 1 publishes one offensive message

        worker1.tell(new Publish(new UserMessage("client1", "Insult"), 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        Message opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        // client2 retrieve Messages

        worker2.tell(new RetrieveMessages("client1", 2));

        while (client2.receivedMessages.size() == 0)
            system.runFor(1);

        Message operation = client2.receivedMessages.remove();
        Assert.assertEquals(FoundMessages.class, operation.getClass());

        List<UserMessage> retrievedMessages = ((FoundMessages) operation).messages;

        Assert.assertEquals(retrievedMessages.size(), 1);
        Assert.assertEquals(retrievedMessages.get(0).getMessage(), "Insult");
        Assert.assertEquals(retrievedMessages.get(0).getAuthor(), "client1");


        // Try to report user with wrong communication id

        worker2.tell(new Report("client2", -1, "client1"));

        Assert.assertThrows(UnknownClientException.class, () -> {
            while (client2.receivedMessages.size() == 0)
                system.runFor(1);
        });

        // client2 is offended and reports the message

        worker2.tell(new Report("client2", 2, "client1"));
        worker2.tell(new Report("client3", 2, "client1"));
        worker2.tell(new Report("client4", 2, "client1"));
        worker2.tell(new Report("client5", 2, "client1"));
        worker2.tell(new Report("client6", 2, "client1"));

        // client1 tires to publish, an succeeds  because he is below report limit of 5

        worker1.tell(new Publish(new UserMessage("client1", "BadStuff"), 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        // Add 5th report to ban client1
        worker2.tell(new Report("client7", 2, "client1"));

        //client1 tries to publish, but is banned
        worker1.tell(new Publish(new UserMessage("client1", "BadStuff"), 1));

        while (client2.receivedMessages.size() < 6 || client1.receivedMessages.size() == 0)
            system.runFor(1);

        opAck = client2.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client2.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client2.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client2.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client2.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        opAck = client2.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        Message userBanned = client1.receivedMessages.remove();
        Assert.assertEquals(UserBanned.class, userBanned.getClass());

        // Try to like as banned user

        worker1.tell(new Like("client1", 1, 0));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        userBanned = client1.receivedMessages.remove();
        Assert.assertEquals(UserBanned.class, userBanned.getClass());

        // Try to dislike as banner user

        worker1.tell(new Dislike("client1", 1, 0));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        userBanned = client1.receivedMessages.remove();
        Assert.assertEquals(UserBanned.class, userBanned.getClass());

        // Try to report as banner user

        worker1.tell(new Report("client1", 1, "client2"));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        userBanned = client1.receivedMessages.remove();
        Assert.assertEquals(UserBanned.class, userBanned.getClass());

        // Try to report same user twice as same client

        worker2.tell(new Report("client2", 2, "client1"));

        while (client2.receivedMessages.size() == 0)
            system.runFor(1);

        operation = client2.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, operation.getClass());

        //-----End Routine
        worker1.tell(new FinishCommunication(1));
        worker2.tell(new FinishCommunication(2));

        while (client1.receivedMessages.size() == 0 || client2.receivedMessages.size() == 0)
            system.runFor(1);

        // Acknowledge communication end

        Message finAckMessage1 = client1.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage1.getClass());
        FinishAck finAck1 = (FinishAck) finAckMessage1;
        Assert.assertEquals(Long.valueOf(1), finAck1.communicationId);

        Message finAckMessage2 = client2.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage2.getClass());
        FinishAck finAck2 = (FinishAck) finAckMessage2;
        Assert.assertEquals(Long.valueOf(2), finAck2.communicationId);

        dispatcher.tell(new Stop());

        while (system.getActors().size() > 2)
            system.runFor(1);
        system.stop(dispatcher);
    }


    //Pointless test, remove
    @Test
    public void InvalidMessagesTest() throws UnknownClientException {
        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        system.spawn(client1);

        dispatcher.tell(new InitCommunication(client1, 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        //get workers for each client

        SimulatedActor worker1 = ((InitAck) client1.receivedMessages.remove()).worker;

        // client 1 publishes one offensive message

        worker1.tell(new Publish(new UserMessage("client1", "Insult"), 1));


        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        Message opAck = client1.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, opAck.getClass());

        // Send UserBanned message to worker, shouldn't be processed

        worker1.tell(new UserBanned(1));

        //-----End Routine


        worker1.tell(new FinishCommunication(1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        // Acknowledge communication end

        Message finAckMessage1 = client1.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage1.getClass());
        FinishAck finAck1 = (FinishAck) finAckMessage1;
        Assert.assertEquals(Long.valueOf(1), finAck1.communicationId);

        dispatcher.tell(new Stop());

        while (system.getActors().size() > 2)
            system.runFor(1);
        system.stop(dispatcher);
    }

    @Test
    public void wrongMessageIdInMsgStoreTest() throws UnknownClientException {
        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        system.spawn(client1);

        dispatcher.tell(new InitCommunication(client1, 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        //get workers for each client

        SimulatedActor worker1 = ((InitAck) client1.receivedMessages.remove()).worker;

        //Try to publish a message

        Publish publish = new Publish(new UserMessage("client1", "Insult"), 1);
        worker1.tell(publish);
        int i = 0;
        while (client1.receivedMessages.size() == 0) {
            if(i == 6)
            {
                //set msgId to != NEW_ID so operation fails in MessageStore.update
                publish.message.setMessageId(10);
            }
            system.runFor(1);
            i++;
        }

        Message operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, operation.getClass());

        //-----End Routine

        worker1.tell(new FinishCommunication(1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        // Acknowledge communication end

        Message finAckMessage1 = client1.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage1.getClass());
        FinishAck finAck1 = (FinishAck) finAckMessage1;
        Assert.assertEquals(Long.valueOf(1), finAck1.communicationId);

        dispatcher.tell(new Stop());

        while (system.getActors().size() > 2)
            system.runFor(1);
        system.stop(dispatcher);
    }

    @Test
    public void wrongMessageStoreMsgTest() throws UnknownClientException {
        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        //Get messageStore

        MessageStore messageStore = null;
        for(SimulatedActor simulatedActor : system.getActors())
        {
            if(simulatedActor instanceof MessageStore)
                messageStore = (MessageStore) simulatedActor;
        }

        messageStore.tell(new TestMessage());

        system.runFor(10);

        Assert.assertEquals(TestMessage.class, messageStore.getMessageLog().get(0).getClass());
        dispatcher.tell(new Stop());

        while (system.getActors().size() > 1)
            system.runFor(1);
        system.stop(dispatcher);
    }


    //  This is sketchy, and only done for the sake of full coverage. Maybe a more authentic way of testing WorkerHelper.tick()?
    @Test
    public void workerHelperTickTest() throws UnknownClientException {
        SimulatedActorSystem system = new SimulatedActorSystem();
        TestDispatcher dispatcher = new TestDispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client1 = new TestClient();
        system.spawn(client1);

        dispatcher.tell(new InitCommunication(client1, 1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        //get workers for each client

        SimulatedActor worker1 = ((InitAck) client1.receivedMessages.remove()).worker;

        MessageStore messageStore = null;
        for(SimulatedActor simulatedActor : system.getActors())
        {
            if(simulatedActor instanceof MessageStore)
                messageStore = (MessageStore) simulatedActor;
        }

        WorkerHelper workerHelper = new WorkerHelper(messageStore, client1, new TestMessage(), system);

        system.spawn(workerHelper);

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        Message operation = client1.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, operation.getClass());

        //-----End Routine

        worker1.tell(new FinishCommunication(1));

        while (client1.receivedMessages.size() == 0)
            system.runFor(1);

        // Acknowledge communication end

        Message finAckMessage1 = client1.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage1.getClass());
        FinishAck finAck1 = (FinishAck) finAckMessage1;
        Assert.assertEquals(Long.valueOf(1), finAck1.communicationId);

        dispatcher.tell(new Stop());

        while (system.getActors().size() > 2)
            system.runFor(1);
        system.stop(dispatcher);
    }

    // TODO: Implement test cases
}

