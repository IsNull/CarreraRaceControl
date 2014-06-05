var socketServer = require('./socketServer');
socketServer.init(8080, 9000, process.argv[2], process.argv[3]);