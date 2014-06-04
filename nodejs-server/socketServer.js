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
        five = require("johnny-five"),
        board, car1, car2;

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
                console.log(pathname);
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
                res.write('File not found: ' + pathname);
                res.end();
            } else {
                res.write(data);
                res.end();
            }
        });
    },

    socketListen = function (port) {
        socketDomain.on('error', function (err) {
            console.log('Error caught in socket domain:' + err);
        });

        socketDomain.on('uncaughtException', function (err) {
            console.error(err.stack);
            console.log("Node NOT Exiting...");
        });

        function registerSocketCloseListener(socket) {

            socket.on('error', function (err) {
                console.log('Error caught in socket:' + err + ". If this is ECONNRESET it's pretty much save to ignore. Probably due to a F5 refresh on a mobile.");
            });
            socket.on('uncaughtException', function (err) {
                console.log('UncaughtException caught in socket:' + err);
            });

            socket.on('close', function () {
                try {
                    socket.close();
                    socket.destroy();
                    console.log('Socket closed!');
                    for (var i = 0; i < sockets.length; i++) {
                        if (sockets[i] == socket) {
                            sockets.splice(i, 1);
                            console.log('Removing socket from collection. Collection length: ' + sockets.length);
                            break;
                        }
                    }

                    if (sockets.length == 0) {
                        clearInterval(timerID);
                        data = null;
                    }
                } catch (e) {
                    console.log(e);
                }
            });
        }

        function registerSocketConnectionListener() {
            socketServer.on('connection', function (socket) {


                console.log('Connected to client');
                sockets.push(socket);

                socket.on('message', function (data) {

                    if (data.contains("C1")) {
                        var speed = data.split("C1: ")[1];
                        car1.start(speed);
                    } else if (data.contains("C2")) {
                        var speed = data.split("C2: ")[1];
                        car2.start(speed);
                    }
                });

                registerSocketCloseListener(socket);

            });
        }

        socketDomain.run(function () {
            socketServer = ws.listen(port);

            socketServer.on('listening', function () {
                console.log('SocketServer is running');
            });

            registerSocketConnectionListener();
        });
    },


    init = function (httpPort, socketPort, comPort) {
        initArduinoBoardAndCars(comPort);
        httpListen(httpPort);
        socketListen(socketPort);
    };

    return {
        init: init
    };
}();

module.exports = socketServer;