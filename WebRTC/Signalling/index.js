'use strict';
var express = require('express');
var socketIO = require('socket.io');
var https = require('https');
var fs = require('fs');
var config = require('config');

const options = {
    key: fs.readFileSync(config.get('https.key')),
    cert: fs.readFileSync(config.get('https.pem'))
}

var app = express();
app.set("view engine", "ejs")

app.get("/", function (req, res) {
    res.render("index");
});

var server = https.createServer(options, app);
server.listen(process.env.PORT || config.get('https.port'), "0.0.0.0");

var io = socketIO(server);
var room_n = 0;

io.sockets.on('connection', function (socket) {
    
    socket.on('want_join_server', function() {
        socket.join(room_n);      
        io.sockets.in(room_n).emit("want_join", room_n);
  
        room_n++;
    });

    socket.on('create', function (room) {
        socket.broadcast.emit("bridge_join", room);
    });

    socket.on('message', function (message, room) {
        socket.in(room).emit('message', message, room);
    });

    socket.on('join', function (room) {
        var clientsInRoom = io.sockets.adapter.rooms[room];
        var numClients = clientsInRoom ? clientsInRoom.length : 0;
        if(numClients === 1 ){
            socket.join(room);
            io.sockets.in(room).emit('ready');
        } else{
            socket.emit('full', room);
        }
    });
});