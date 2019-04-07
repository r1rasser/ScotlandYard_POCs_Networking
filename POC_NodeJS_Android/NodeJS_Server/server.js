const express = require('express');
const cors = require('cors');
const app = express();
let http = require('http').Server(app);
let io = require('socket.io')(http);
const bodyParser = require('body-parser');

app.use(cors());

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

app.get("/", (req, res) => {
    console.log("Welcome requested :)");
    res.status(200).json({"response":"Welcome to test server 1.0"});
});

io.on('connection', (client) => {
    console.log('Client ' + client.id + ' connected');
    client.on('disconnect', () => {
        console.log('Client ' + client.id + ' disconnected');
    });
    client.on('message', (message) => {
        client.emit('messageRec',{"message":message});
    });
});

http.listen(3000,'server_host', () => {
    console.log("Listening on port 3000...");
});