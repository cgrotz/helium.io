var roadrunner_endpoint;

function Snapshot(message) {
	var payload = message.payload;
	var name = message.name;
	var priority = message.priority;
	var path = message.path;
	this.val = function() {
		return payload;
	};
	this.name = function() {
		return name;
	};
	this.ref = function(){
		return new Roadrunner(path);
	};
	this.getPriority = function(){
		return priority;
	};
	this.child = function(childPath){
		
	};
	this.forEach = function(childAction){
		
	};
	this.hasChildren = function(){
		
	};
	this.numChildren = function(){
		
	};
}

function RoadrunnerConnection(url)
{
	var self = this;
	var messages = [];
	if (this.endpoint == null) {
		if (!window.WebSocket) {
			window.WebSocket = window.MozWebSocket;
		}
		if (window.WebSocket) {
			roadrunner_endpoint = new WebSocket(url);
			roadrunner_endpoint.onopen = function(event) {
				for(var i=0;i<messages.length;i++)
				{
					roadrunner_endpoint.send(JSON.stringify(messages[i]));
				}
			};
			roadrunner_endpoint.onclose = function(event) {
			};
		}
	}

	roadrunner_endpoint.onmessage = function(event) {
		var message = JSON.parse(event.data);
		if (Object.prototype.toString.call(message) === '[object Array]') {
			for ( var i = 0; i < message.length; i++) {
				self.handleMessage(message[i]);
			}
		} else {
			self.handleMessage(message);
		}
	};

	this.sendMessage = function(type, path, payload) {
		var message = {
			type : type,
			path : path,
			payload : payload
		};
		if(roadrunner_endpoint.readyState == window.WebSocket.OPEN)
		{
			roadrunner_endpoint.send(JSON.stringify(message));
		}
		else
		{
			messages.push(message);
		}
	};
}

function Roadrunner(path) {
	var events = {};
	var roadrunner_connection = new RoadrunnerConnection(path); 
	roadrunner_connection.handleMessage = function(message) {
		var snapshot = new Snapshot(message);
		var callback = events[message.type];
		if (callback != null) {
			callback(snapshot);
		}
	};
	
	this.child = function(childname) {
		return new Roadrunner(path + "/" + childname);
	};

	this.on = function(event_type, callback) {
		events[event_type] = callback;
	};

	this.push = function(data) {
		roadrunner_connection.sendMessage('push', path, data);
	};

	this.set = function(data) {
		roadrunner_connection.sendMessage('set', path, data);
	};

	this.auth = function( authToken, onComplete, onCancel) {
	};
	
	this.unauth = function(data) {
	};

	this.parent = function() {
	};

	this.root = function() {
	};

	this.toString = function() {
	};

	this.name = function() {
	};
};