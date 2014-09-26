Distributed Control of Currency Exchange Values using Totally-Ordered Multicast with Lamport's Clocks
=====================================================================================================


Description: This project aims to have a distributed application which maintains the value of currency in form (buyValue, sellValue). The application is supposed to run on three nodes which communicate using TCP sockets. All the nodes should see the updates to the currency value in same order. This is done using Lamport's logical clocks.

* AUTHOR: 

- Name: Tejas Bondre
- contact: tejasbondre@gmail.com


* Program info:
---------------

The program is divided into three layers.


1. Application layer
--------------------

This layer consists of following classes:

A. Lamport.java: 

This class is the main application thread. It gets an object of currency value. It also starts the thread for the distribution layer (middleware). 	It then runs a loop where it generates random currency update and passes the message to the middleware. It also polls for the messages passed to it by the middleware and performs updates in those messages to the currency value.

B. CurrencyValue.java

This class is for the currency value which is initialized to (100,100). This class provides methods like updateValue() and getValue() which update the currency value by given delta and get the current currency value respectively.


2. Distribution Layer
----------------------

This layer is heart of the program and is organized into a package called middleware along with third layer. It consists of following classes

A. DistributionLayer.java

This class implements the core functionality of middleware and handles the messages and co-ordinates between communications as well as exit strategy. It also handles the
Lamport's logical clock and hence the timestamps as well as the ordering of the messages. 	It starts with creating number of sockets connected to other nodes. It then spawns a thread to handle each of those sockets.

Middleware accepts messages from the application layer, stamps them with lamport's logical time and puts them in queue of each socket thread. It also polls the incoming queue of each socket thread to pull any received messages. 

It processes the messages according to their type. Update messages are put in a priority queue and acknowledgments are sent for them. Acknowledgment messages are used for updating the acknowledgment counter of update messages in the queue. It also sends the exit messages to other queues as well as 'poke' messages to request exiting process to wait.

When we are done, it sends exit signals to the socket threads and then exits when cleanup is done.

Methods provided by this class include deliverMessages(), getMessages(), sendMessages(), createClientSockets(), createServerSockets() etc.

B. Message.java

This is class for the messages sent over the socket. It implements serializable interface so that messages can be sent over socket. Messages have the type, the senders logical time, senders process id. 

If it is an update message, it has update values as well as acknowledgment count.

If it is an acknowledgment message, it has the time stamp of message for which this is ack.

If it is an exit message, middleware is notified that some process is ready to exit. In this case, middleware can send a poke message, which tells the exiting process that we are not finished and it needs to wait. If this message is ignored, the sender will eventually exit.

If it is a poke message, the receiver understands that the sender is not yet done with all the operations and it needs to postpone the exit.

C. LogicalClock.java

This class is Lamport's logical clock. It provides methods like increment(), getTime(), setTime() which are used by middleware to increment the clock after event, get current time and adjust the clock if necessary.

D. LogicalTimeComparator.java

This class implements Comparator interface to provide a comparator for Message objects. The comparison is done based on the timestamp of the Message object. This is used as the comparator of PriorityQueue used by the middleware.

E. LogWriter.java

This class is used for writing all the logs in a particular format.



3. Socket threads
------------------

This is the lowest layer. These threads handle the socket communication.

A. SocketThread.java

This class is runnable. It is spawned by middleware and handles communication on a particular socket. It sends the messages coming from middleware over a socket using
ObjectOutputStream() and reads incoming messages using ObjectInputStream().



Other implementation details:
------------------------------

A. Communication between the threads:

Communications between the threads are done using consumer-producer model. There are two queues between the threads, one for each direction.

1. Application-Middleware: The application and middleware communicate using two synchronous queues. These are not priority queues. Application puts message in queue appToMid, and middleware puts message to application in midToApp. These queues are created by application and are passed to middleware in initialization phase.

2. Middleware-SocketThreads: Each socket thread has its two local queues. One for incoming messages and one for outgoing messages. It provides methods getMessage() and putMessage() so that middleware can push and pull messages from socket threads. 

We cannot have shared queue like Application-Middleware because java does not allow creating arrays of BlockingQueue of Message objects. Having fixed number of queues will cause loss of generalization.


B. Exit strategy:

It is difficult to decide exactly when to exit. This is because the other node may still generate some updates, or there are possible updates pending on other nodes' socket queues. The strategy I use is as follows:

1. Application layer sets a flag and interrupts middleware when it is done generating all the updates. This tells middleware the "Local queue is empty" condition is satisfied

2. After receiving interrupt and checking the flag, middleware changes its state and starts checking if PriorityQueue is empty. If this is not so, we can not exit. If the PriorityQueue is empty, then second condition "All messages are delivered" is met.

3. At this point, the node will send exit request message to other nodes. If any of the nodes still has updates to generate or has messages pending in priority queue, it will reply back with message type 'p'. We then set our exit flag to false and go back to state 2 mentioned above. If for significant time no one sends a 'p' message, then we exit.

This strategy means that the process might be OK with letting requesting process exit. In which case it will print "Pn finished". However, if remaining node requests Pn to wait by sending 'p' message, then "Pn finished" message will be printed in log again the next time exit request comes and we are OK with letting requesting process exit.

