# README #

#Installation
Move to the RaceControl directory and type:

> npm install

it loads some node-modules and dependencies. 

Save and open the file

> Scripts/script.js

and replace the IP-Address in 
`var settings = {	host: 'ws://192.168.174.1:9000'	};` 

with the one the server runs on. Save, m


Run the server with: 
Replace in the following command COMX with the correct COM PORT

> node server.js COMX

This will start the nodejs-server. Wait for the "Initialized" output in the console. After that you're good to go.
You may now connect with your device with #serverip:8080, where #serverip is the ip you changed previously.

Project Idea
![IMG_0103.JPG](https://bitbucket.org/repo/5Axey7/images/4171111834-IMG_0103.JPG)