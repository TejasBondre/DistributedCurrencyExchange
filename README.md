Distributed Control of Currency Exchange Values using Totally-Ordered Multicast with Lamport's Clocks
=====================================================================================================

Description: This project aims to have a distributed application which maintains the value of currency in form (buyValue, sellValue). The application is supposed to run on three nodes which communicate using TCP sockets. All the nodes should see the updates to the currency value in same order. This is done using Lamport's logical clocks.

*
- Name: Tejas Bondre
- contact: tejasbondre@gmail.com


* Program info:
---------------

The program will be divided into three layers.


1. Application layer
--------------------

This layer will consists of following classes:
A. Lamport.java: 
B. CurrencyValue.java


2. Distribution Layer
----------------------

This layer will be the heart of the program and will be organized into a package called middleware along with third layer. It will consist of following classes
A. DistributionLayer.java
B. Message.java
C. LogicalClock.java
D. LogicalTimeComparator.java
E. LogWriter.java


3. Socket threads
------------------

This will be the lowest layer. These threads will handle the socket communication.
A. SocketThread.java
