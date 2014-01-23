WoChat
======

A *WebSocket*-based chat for **Wisdom of Crowds** experiments. The server-side has been developed in Java using Netty, while the client-side has been written in Javascript using JQuery.

Download
--------

You can easily clone the full project by executing:

    $ git clone https://github.com/francesco-ficarola/wochat.git

Building and Running
--------------------

After cloning the git repository you can build and run the software by executing:

    $ cd wochat/
    $ ./build.sh
    $ ./run.sh

or, if you prefer all at once, just call the following script:

    $ ./build-and-run.sh
   
The *run.sh* script is there for your convenience. Otherwise, after building, you can find the distribution package (i.e., **WoChat.tar.gz**) inside the *target* folder. You can extract it wherever you like, then run the software by executing:

    $ cd WoChat/
    $ ./startup.sh

If the server properly starts, then open your browser (*Chrome or Firefox are recommended*) and go to:

    http://127.0.0.1:8080/

Log-files
---------

All log-files are saved in the "logs" folder (from the repository root: target/WoChat/logs).

* *connections.log*: information about users' connections
* *interactions.log*: users' interactions (format parsable by [OpenBeaconParser](https://github.com/francesco-ficarola/OpenBeaconParser))
* *messages.log*: users' messages in CSV format
* *userslist.log*: list of all participants
* *wochat.log*: system log