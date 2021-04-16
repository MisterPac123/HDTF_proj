In root folder (HDTF_proj):

	-Compile Client:
	javac .\client\*.java

	-Run client:
	java -cp . client.Client (str) Username (Str) host (int) port





==================================================================
							[SOME NOTES]
==================================================================

1. Server Is Now Running On localhost 8000.
	-> Specify Port Later ong args.

2. Clients are all using The same ClientPubKey and ClientPrivKey 
	-> Generate A different key pair for each user. Server must have access 
	to all Client's Key Pairs. In JSON Request Server knows the user
	its communicating with. So the server knows where and which the respective
	key pair is and search it by userID on the keys folder. 
	[DONE]

3. Proofs should be Sing By Each User. (Implement Later)
	
	1. A -> B: (requestProf);
		1.a B signs the proof with its own priv key: {"proof"}userPrivKeyB -> proofSignB

	2. B -> A: (proofSignB). 
		2.a User A does not understand what B sends because has no B public Key;
		2.b USer A cannot forge B identity becaus the proof its signed!. (Integrity + Authentication)
	
	3. A -> Server: proofSignB.
		2. a Server Deciphers proofSignB with user B public Key.
		2. b Server Knows that B send a valid proof about user A's location.

4. In the end project should be running with maven or some
other framework.

In root folder (HDTF_proj):
==================================================================
							[SERVER]
==================================================================
// All Dependency Classes will also be compliled. 

>> javac -cp server/gson-2.6.2.jar: server/Server.java 


>> java -cp server/gson-2.6.2.jar: server.Server 8000

==================================================================
							[CLIENT]
==================================================================
// All Dependency Classes will also be coompiled. 

>> javac -cp client/gson-2.6.2.jar: client/Client.java


>> java -cp client/gson-2.6.2.jar: client.Client mary localhost 8001
>> java -cp client/gson-2.6.2.jar: client.Client john localhost 8002

==================================================================
							[END]
==================================================================


PROJECT 1:

==================================================================
							[SOME NOTES]
==================================================================

1. Server Is Now Running On localhost 8000.
	-> Specify Port Later ong args.

2. Clients are all using The same ClientPubKey and ClientPrivKey 
	-> Generate A different key pair for each user. Server must have access 
	to all Client's Key Pairs. In JSON Request Server knows the user
	its communicating with. So the server knows where and which the respective
	key pair is and search it by userID on the keys folder. 

3. Proofs are Signed by users in the proximity of the prover
	
	1. A -> B: (requestProf);
		1.a B signs the proof with its own priv key: {"proof"}userPrivKeyB -> proofSignB

	2. B -> A: (proofSignB). 
		2.a User A does not understand what B sends because has no B public Key;
		2.b USer A cannot forge B identity because the proof its signed!. (Integrity + Authentication)
	
	3. A -> Server: proofSignB.
		2. a Server Deciphers proofSignB with user B public Key.
		2. b Server Knows that B send a valid proof about user A's location.

4. Only the super user has permission to check other users locations

==================================================================
					[HOW TO RUN THE PROJECT]
==================================================================
In the root directory (HDTF_PROJ):
// All Dependency Classes will also be compliled. 

1.javac -cp server/gson-2.6.2.jar: server/Server.java
2.javac -cp client/gson-2.6.2.jar: client/Client.java

3.java -cp server/gson-2.6.2.jar: server.Server 8000

4.(In a new terminal window) java -cp client/gson-2.6.2.jar: client.Client mary localhost 8001
5.(In a new terminal window) java -cp client/gson-2.6.2.jar: client.Client john localhost 8002
6.(In a new terminal window) java -cp client/gson-2.6.2.jar: client.Client SU_ha localhost 8003

7.Add a new position to any user using the terminal (example: 2 2 2 2 -> addposition(option 2) , epoch2, x=2, y =2)

8.Add a new position to other user using the terminal (example: 2 2 1 1 -> addposition(option 2) , epoch2, x=1, y =1)

9. Submit a report by requesting it using the terminal (example:1 2 -> Request Location Proof, epoch2 and see the result)

10. Obtain Location report(example: in the prover terminal -> 3(enter key) john 2 , john and epoch)
Successful if looking for own report or by being a superuser, otherwise, error message will appear.

11.ObtainUsersAtLocation by the superuser (example: 4(enter key) (1,1) 2) and check the results.
If this last operation is performed by someone other than the superuser, error will display.

If required to run again first do:
	rm usersPorts.txt
	rm epoch*

==================================================================
		[HOW TO RUN THE PROJECT][FULLY AUTOMATED W/TESTS]
==================================================================
In the Tests directory:
1.bash init.sh (this will compile, run the server, define positions of users in the different epochs)
2.
