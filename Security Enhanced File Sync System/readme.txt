
------------- Java Version--------------------------------------------------------------------------

The jar files were compiled by java version 1.7.0_17.

------------- Security Description --------------------------------------------------------------------------

This system is designed to synchronise files on both server and client sides in a secure network environment. All the messages and data passed over the network is encrypted. However, after these messages arrive at client console, necessary synchronisation messages will be displayed on it, in order to let user knows current synchronisation status. Therefore, it is highly recommended that user can keep his/her machine in a physically secure environment.

------------- Connection Password--------------------------------------------------------------------------

The server-client connection password is 123456

------------- Step 1: Run Server--------------------------------------------------------------------------

Run the syncserver.jar first with the server file directory as parameter. The command format is as followings.

java -jar syncserver.jar filedirectory

------------- Step 2:Run Client--------------------------------------------------------------------------

When the message "Server has been established" shows up on server console, open up another console and run the syncclient.jar with host IP and client file directory as parameters. If you are running both Server and Client on the same machine, the host IP can be expressed as localhost. If they are running on different machines, host IP is referring to the IP address of the server machine. In addition, it is worth noting that port number has been set as 6666 by default. The command format is as followings.

java -jar syncclient.jar hostIP filedirectory

------------- Step 3: Role and Block Size Negotiation --------------------------------------------------------------------------

All the user interaction will be proceeded on client console.

-- Firstly, the user will be asked to enter the password for secure connection. The correct password is 123456. The connection to server will not be established until correct password is provided.

-- Secondly, the user will be asked to decide the client's role (Source or Destination). What user needs to do is to type Yes or No.

-- Secondly, the user needs to type an integer which will be utilised as the synchronisation block size. By default, the block size is 1024 K. But user can customise the size themselves. A example answer would be: 1000

-- When one round of synchronisation is finished, the system will wait for user input to decide whether it should go on with another round of synchronisation or exit the system. If the user chooses to continue on, the system will behave as before and will fist ask the user to decide source/destination role, and then specify the block size. Therefore, the user can have a different role or block size in every synchronisation. If the user chooses to exit, both the client and server will be terminated.

