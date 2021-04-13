
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

3. Proofs should be Sing By Each User. (Implement Later)
	
	1. A -> B: (requestProf);
		1.a B signs the proof with its own priv key: {"proof"}userPrivKeyB -> proofSignB

	2. B -> A: (proofSignB). 
		2.a User A does not understand what B sends because has no B public Key;
		2.b USer A cannot forge B identity becaus the proof its signed!. (Integrity + Authentication)
	
	3. A -> Server: proofSignB.
		2. a Server Deciphers proofSignB with user B public Key.
		2. b Server Knows that B send a valid proof about user A's location.


In root folder (HDTF_proj):
==================================================================
							[CLIENT]
==================================================================
// All Dependency Classes will also be compliled. 

>> javac -cp server/gson-2.6.2.jar: server/SecureServer.java 

>> java -cp server/gson-2.6.2.jar: server.SecureServer 8000 keys/bob.privkey keys/bob.pubkey keys/alice.pubkey keys/secret.key
==================================================================
							[SERVER]
==================================================================
// All Dependency Classes will also be coompliled. 

>> javac -cp client/gson-2.6.2.jar: client/Client.java


>> java -cp client/gson-2.6.2.jar: client.Client mary localhost 8001
>> java -cp client/gson-2.6.2.jar: client.Client john localhost 8002

==================================================================
							[END]
==================================================================
