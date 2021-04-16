#!/bin/sh
cd ..
javac -cp server/gson-2.6.2.jar: server/Server.java 
java -cp server/gson-2.6.2.jar: server.Server 8000