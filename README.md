# README #

#Installation
Move to the nodejs-server direectory and type:

    npm install johnny-five websocket.io

it loads some node-modules and dependencies. After it's finished open the file

> socketServer.js

and look for the 

> portName
 
variable. Set it to the COMport to which the arduino board is connected. Save and open the file

> Scripts/script.js

and replace the IP-Address in 
`var settings = {	host: 'ws://192.168.174.1:9000'	};` 

with the one the server runs on. Save, make sure you're in the nodejs-server folder and type:

    node server.js

This will start the nodejs-server. Wait for the "Initialized" output in the console. After that you're good to go.
You may now connect with your device with #serverip:8080, where #serverip is the ip you changed previously.

Project Idea
![IMG_0103.JPG](https://bitbucket.org/repo/5Axey7/images/4171111834-IMG_0103.JPG)