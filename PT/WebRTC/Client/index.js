'use strict';

var socketIO = require('socket.io');

const myArgs = process.argv.slice(2);

const static_pub = "public";
const views = "index";

if(myArgs.length == 0)
    throw Error ('No port specified');

var express = require('express');
var http = require('http');

var app = express();
app.set("view engine", "ejs")

app.use(express.static(static_pub));

var bridge_num = -1;

app.get("/", function (req, res) {
    bridge_num = req.query.bridge;
    res.render(views);
});

var server = http.createServer(app);
server.listen(process.env.PORT || myArgs[0]);

var io = socketIO(server);

io.sockets.on('connection', function (socket) {
    socket.emit("bridge",bridge_num);
});