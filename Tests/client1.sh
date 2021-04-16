cd ..
javac -cp client/gson-2.6.2.jar: client/Client.java
java -cp client/gson-2.6.2.jar: client.Client john localhost 8001 
expect "Choose option:"
send -- "1 0"