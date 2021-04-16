PROJECT 1:

==================================================================
							[SOME NOTES]
==================================================================

1. Server Is Running On localhost 8000.
	
2. Clients are all using unique ClientPubKey and ClientPrivKey 
	-> A key pair is generated for each user. Server must have access 
	to all Client's Key Pairs. In JSON Request Server knows the user
	its communicating with. So the server knows where and which the respective
	key pair is and search it by userID on the keys folder. 

3. Proofs are Signed by users in the proximity of the prover(3 "squares" of distance)
	
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
		[HOW TO RUN THE PROJECT][SOMEWHAT AUTOMATED]
==================================================================
In the Tests directory:
1.bash init.sh (this will compile the server and client , run the server, define positions of users in the different epochs(to avoid manual work))
2.Open several terminals(up to 10, since 10 users are created before hand, however only 6 are required to the demonstration of the project)
3.In the terminals created, run (still in the Tests directory) "bash clieni.sh", where i is from 1 to 4(is enough) or 9(if more testing required) (IN ORDER).
4.Run also "bash super_client.sh" in a terminal in order to have a super user created and working

5.[OPERATIONS] |
	    	   |
			    -> Client 1 terminal: Leave it be , when client 2 asks to submit report, client 1 will act as witness
				-> Client 2 terminal: Write "1 0" and press enter -> Client 2 will submit report of it's position at epoch 0 and receive Positive response from the server
				-> Client 3 terminal: Write "1 0" and press enter and wait for answer. Then write "3 (enter key) followed by the userID(olie) and epoch(0)"
					Wait for positive response				
				-> Client 4 terminal: Write "3 (enter key) userID(olie) and epoch(0)", but permission denied is displayed, since this user is not a super user.
				-> Super_user Client: Try the same command as last one. Then try "4 (enter key) (1,2) 0"
					Check positive results


==================================================================
					[HOW TO RUN THE PROJECT] [MANUALLY]
==================================================================
In the root directory (HDTF_PROJ):
// All Dependency Classes will also be compliled. 

1.javac -cp server/gson-2.6.2.jar: server/Server.java
2.javac -cp client/gson-2.6.2.jar: client/Client.java

3.java -cp server/gson-2.6.2.jar: server.Server 8000

4.(In a new terminal window): java -cp client/gson-2.6.2.jar: client.Client mary localhost 8001
5.(In a new terminal window): java -cp client/gson-2.6.2.jar: client.Client john localhost 8002
6.(In a new terminal window): java -cp client/gson-2.6.2.jar: client.Client SU_ha localhost 8003

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
							[END]
==================================================================
