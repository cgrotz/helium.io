var endpoint;

function Snapshot(message) {
	var self = this;
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
	this.child = function(childPath){
		
	};
	this.forEach = function(childAction){
		
	};
	this.hasChildren = function(){
		
	};
	this.numChildren = function(){
		
	};
	this.ref = function(){
		return new Roadrunner(path);
	};
	this.getPriority = function(){
		return priority;
	};
}

function Roadrunner(roadrunner_url) {
	var events = {};
	var path = roadrunner_url;
	var self = this;
	if (endpoint == null) {
		if (!window.WebSocket) {
			window.WebSocket = window.MozWebSocket;
		}
		if (window.WebSocket) {
			endpoint = new WebSocket(roadrunner_url);
			endpoint.onopen = function(event) {
			};
			endpoint.onclose = function(event) {
			};
		}
	}

	endpoint.onmessage = function(event) {
		var message = JSON.parse(event.data);
		if (Object.prototype.toString.call(message) === '[object Array]') {
			for ( var i = 0; i < message.length; i++) {
				self.handleMessage(message[i]);
			}
		} else {
			self.handleMessage(message);
		}
	};

	this.handleMessage = function(message) {
		var snapshot = new Snapshot(message);
		var callback = events[message.type];
		if (callback != null) {
			callback(snapshot);
		}
	};

	this.sendMessage = function(type, path, payload) {
		var message = {
			type : type,
			path : path,
			payload : payload
		};
		endpoint.send(JSON.stringify(message));
	};

	this.child = function(childname) {
		return new Roadrunner(path + "/" + childname);
	};

	this.on = function(event_type, callback) {
		events[event_type] = callback;
	};

	this.push = function(data) {
		this.sendMessage('push', path, data);
	};

	this.set = function(data) {
		this.sendMessage('set', path, data);
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