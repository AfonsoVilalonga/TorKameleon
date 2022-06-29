'use strict';

const myArgs = process.argv.slice(2);

//const static_pub = "/home/afonso/Desktop/Tese/WebRTC/Client/public";
//const views = "/home/afonso/Desktop/Tese/WebRTC/Client/views";

const static_pub = "public";
const views = "/views/index";

if(myArgs.length == 0)
    throw Error ('No port specified');

var express = require('express');
var http = require('http');

var app = express();
app.set("view engine", "ejs")

app.use(express.static(static_pub));

app.get("/", function (req, res) {
    res.render(views);
});

var server = http.createServer(app);
server.listen(process.env.PORT || myArgs[0]);