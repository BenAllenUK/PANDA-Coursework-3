'use strict'
var net = require('net');
var events = require('events');
var util = require('util');


function PlayerServer() {
    var self = this;
    this.server = net.createServer(function(player) {
        player.on('data', function (data){
            self.onInput(player, data);
        })
    });
}
util.inherits(PlayerServer, events.EventEmitter);


PlayerServer.prototype.onInput = function(player, data) {
    var message = JSON.parse(data);
    var self = this;
    if(message.type == "REGISTER"){
        self.emit('register', player, message.student_id);
    }  else if(message.type == "MOVE"){
        self.emit('move', player, message);
    }
}

PlayerServer.prototype.listen = function(port) {
    this.server.listen(port);
}

PlayerServer.prototype.close = function() {
    this.server.close();
}


module.exports = PlayerServer;