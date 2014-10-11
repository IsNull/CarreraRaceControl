var socketServer = require('./socketServer');
socketServer.init(8081, 9001, process.argv[2], process.argv[3]);