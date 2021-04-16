#!/bin/sh
rm ../usersPorts.txt
rm ../epoch*
touch ../epoch0Map.txt ../epoch1Map.txt ../epoch2Map.txt ../epoch3Map.txt ../epoch4Map.txt ../epoch5Map.txt ../epoch6Map.txt
echo  $'john 1 1\nmary 1 2\nolie 1 3\ncarl 5 5\nhomer 9 9\npenny 3 4\ncat 2 3\npeter 4 5\nray 5 1\nSU_ha 1 9' > ../epoch0Map.txt
echo  $'john 8 3\nmary 9 5\nolie 4 2\ncarl 7 6\nhomer 1 10\npenny 5 10\ncat 4 9\npeter 3 2\nray 6 7\nSU_ha 1 8' > ../epoch1Map.txt
echo  $'john 5 2\nmary 6 4\nolie 10 7\ncarl 1 9\nhomer 8 3\npenny 3 8\ncat 2 10\npeter 4 1\nray 9 7\nSU_ha 6 5' > ../epoch2Map.txt
echo  $'john 4 7\nmary 2 6\nolie 3 10\ncarl 1 8\nhomer 5 9\npenny 2 7\ncat 1 9\npeter 10 6\nray 3 5\nSU_ha 4 8' > ../epoch3Map.txt

bash server.sh
