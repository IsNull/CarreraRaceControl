

var socketServer = function () {

    var data = null,
        timerID = null,
        sockets = [],
        socketServer = null,
    /* Add module imports here */
        ws = require('websocket.io'),
        http = require('http'),
        fs = require('fs'),
        url = require('url'),
        domain = require('domain'),
        socketDomain = domain.create(),
        httpDomain = domain.create(),
        globalDebug = false,
        five = require("johnny-five"),
        board, car1, car2, playerOneSet, playerTwoSet;

    function initArduinoBoardAndCars(comPort) {
        board = new five.Board({port: comPort})
        board.on("ready", function () {
            car1 = new five.Motor({
                pin: 6,
                current: {
                    pin: 6,
                    freq: 0,
                    range: [0, 255]
                }
            });

            car2 = new five.Motor({
                pin: 10,
                current: {
                    pin: 10,
                    freq: 0,
                    range: [0, 255]
                }
            });
	        // Start the car with initial speed
            car1.start(45);
        });
    }

    httpListen = function (port) {
        httpDomain.on('error', function (err) {
            console.log('Error caught in http domain:' + err);
        });
        httpDomain.on('uncaughtException', function (err) {
            console.log('Error caught in http domain:' + err);
        });

        httpDomain.run(function () {
            http.createServer(function (req, res) {
                var pathname = url.parse(req.url).pathname;
                console.log(pathname.blue);
                if (pathname == '/' || pathname == '/index.html') {
                    readFile(res, 'public/index.html');
                } else {
                    readFile(res, 'public/' + pathname);
                }
            }).listen(port, "0.0.0.0");
        });
    },

        readFile = function (res, pathname) {
            fs.readFile(pathname, function (err, data) {
                if (err) {
                    console.log(err.message);
                    res.writeHead(404, {
                        'content-type': 'text/html'
                    });
                    res.write('File not found: '.red + pathname);
                    res.end();
                } else {
                    res.write(data);
                    res.end();
                }
            });
        },

        socketListen = function (port) {
            socketDomain.on('error', function (err) {
                console.log('Error caught in socket domain:'.red + err);
            });

            socketDomain.on('uncaughtException', function (err) {
                console.error(err.stack);
                console.log("Node NOT Exiting...".yellow);
            });

            function closeSocketAndRemoveFromList(socket) {
                if (socket.player === 1) {
                    playerOneSet = false;
                    car1.stop();
                    console.log("Player 1 can be set again".yellow);
                } else if (socket.player === 2) {
                    playerTwoSet = false;
                    car2.stop();
                    console.log("Player 2 can be set again".yellow);
                }
                socket.close();
                socket.destroy();
                console.log('Socket closed!'.yellow);
                for (var i = 0; i < sockets.length; i++) {
                    if (sockets[i] == socket) {
                        sockets.splice(i, 1);
                        console.log('Removing socket from collection. Collection length: '.yellow + sockets.length);
                        break;
                    }
                }
            }

            function registerSocketCloseListener(socket) {

                socket.on('error', function (err) {
                    console.log('Error caught in socket:'.red + err + ". If this is ECONNRESET it's pretty much save to ignore. Probably due to a F5 refresh on a mobile.".red);
                });

                socket.on('uncaughtException', function (err) {
                    console.log('UncaughtException caught in socket:' + err + "".red);
                });

                socket.on('close', function () {
                    try {
                        closeSocketAndRemoveFromList(socket);
                        if (sockets.length == 0) {
                            clearInterval(timerID);
                            data = null;
                        }
                    } catch (e) {
                        console.log(e);
                    }
                });
            }

            function setPlayerIfPossible(data, socket) {
                if (data.indexOf("setPlayerC1") !== -1) {
                    if (playerOneSet) {
                        return false;
                    } else {
                        playerOneSet = true;
                        socket.player = 1;
                        console.log("Set player 1!".green)
                        return true;
                    }
                } else if (data.indexOf("setPlayerC2") !== -1) {
                    if (playerTwoSet) {
                        return false;
                    } else {
                        playerTwoSet = true;
                        socket.player = 2;
                        console.log("Set player 2!".green)
                        return true;
                    }
                }
            }

            function registerSocketConnectionListener() {
                socketServer.on('connection', function (socket) {

                    console.log('Connected to client'.green);
                    sockets.push(socket);

                    socket.on('message', function (data) {

                        if (globalDebug) {
                            var string = "server received message: " + data;
                            console.log(string.greyBG);
                        }

                        if (data.indexOf("setPlayer") !== -1) {
                            if (setPlayerIfPossible(data, socket)) {
                                socket.send("ok");
                            } else {
                                socket.send("fault");
                                closeSocketAndRemoveFromList(socket);
                                console.log("Removed socket because the player is already set!".yellow);
                            }
                        } else {
                            if (data.contains("C1:")) {
                                var speed = parseInt(data.split("C1: ")[1]);
                                if (globalDebug) {
                                    console.log("Setting speed to car1: '".greyBG + speed + "'");
                                }
                                 car1.speed(speed);
                                 //console.log("Speed set without error!");
                            } else if (data.contains("C2:")) {
                                var speed = parseInt(data.split("C2: ")[1]);
                                if (globalDebug) {
                                    console.log("New speed: ".greyBG + speed)
                                }
                                car2.start(speed);
                            }
                        }
                    });

                    registerSocketCloseListener(socket);

                });
            }

            socketDomain.run(function () {
                socketServer = ws.listen(port);

                socketServer.on('listening', function () {
                    console.log('SocketServer is running'.green);
                });

                registerSocketConnectionListener();
            });
        },


        init = function (httpPort, socketPort, comPort, debug) {
            if (debug && debug.indexOf("-d") !== -1) {
                globalDebug = true;
            }
            initArduinoBoardAndCars(comPort);
            httpListen(httpPort);
            socketListen(socketPort);
            console.log("I'm ready and set. Try to connect now! Debug is".green, globalDebug==true ? "on".green : "off".green);
        };

    return {
        init: init
    };
}();

module.exports = socketServer;