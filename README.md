WoChat
======

A *web socket*-based chat for **Wisdom of Crowds** experiments. Server has been developed in Java using Netty, while client has been written in Javascript using JQuery.

Building and Running
--------------------

After cloning the whole git project you can build and run the software by executing:

    $ ./build.sh
    $ ./run.sh

or, if you prefer all at once, just call the following script:

    $ ./build-and-run.sh
   
The *run.sh* script is there for your convenience. Otherwise, after building, you can find the distribution package (i.e., **WoChat.tar.gz**) inside the *target* folder. You can extract it wherever you like, then run the software by executing:

    $ cd WoChat/
    $ ./startup.sh

If the server properly starts, then open your browser (*Chrome or Firefox are recommended*) and go to:

    http://127.0.0.1:8080/