# Mutant description
Describe what is wrong with the implementations.

## Mutant 1
If you want to like a message you need Test-Client-Name, Comminication ID and Message ID.
At this Mutant we only have Test-Client-Name (BOB). Additionally the order do call the like function is wrong.
After the Client Name "BOB" there schould be the Communication ID (as you cann see: new Like("TestClient", Communication ID, Message ID)),
instead there is the message. And at the Mutant one, the last Parameter is BOB again, but here should be the message ID.

## Mutant 2


## Mutant 3
The function Retrieve does not exist

## Mutant 4
Assuming that msg_w_11ch means message with 11 characters than this command will fail. Because it is only allowed
to publish a message with a maximum of 10 characters.

## Mutant 5
The function Retrieve does not exist

## Bonus Mutant 81
Not done.

## Bonus Mutant 82
Not done.
