

// Roadrunner
var roadrunner_endpoint;

function Snapshot(message, roadrunner_connection) {
	var payload = message.payload;
	var name = message.name;
	var path = message.path;
	var parent = message.parent;
	var hasChildren = message.hasChildren;
	var numChildren = message.numChildren;
	this.val = function() {
		return payload;
	};
	this.name = function() {
		return name;
	};
	this.parent = function() {
		return parent;
	};
	this.path = function() {
		return path;
	};
	this.ref = function() {
		return new Roadrunner(path);
	};
	this.child = function(childPath) {
		new Roadrunner(path + "/" + childPath);
	};
	this.forEach = function(childAction) {
		var dataRef = new Roadrunner(path);
		dataRef.on("child_added", childAction);
	};
	this.hasChildren = function() {
		return hasChildren;
	};
	this.numChildren = function() {
		return numChildren;
	};
}

function RoadrunnerConnection(url) {
	var self = this;
	self.url = url;
	var messages = [];
	if (roadrunner_endpoint == null) {
		roadrunner_endpoint = new ReconnectingWebSocket(url);
		roadrunner_endpoint.onopen = function(event) {
			console.debug("Connection established resyncing");
			for ( var i = 0; i < messages.length; i++) {
				roadrunner_endpoint.send(JSON.stringify(messages[i]));
			}
			messages = [];
		};
		roadrunner_endpoint.onclose = function(event) {
			console.debug("Connection lost trying reconnecting");
		};

		roadrunner_endpoint.eventhandlers = [];
		roadrunner_endpoint.onmessage = function(event) {
			for ( var i = 0; i < roadrunner_endpoint.eventhandlers.length; i++) {
				var handler = roadrunner_endpoint.eventhandlers[i];
				handler(event);
			}
		};
	}
	roadrunner_endpoint.eventhandlers.push(function(event) {
		var message = JSON.parse(event.data);

		//console.debug('Receiving Message from Server: ',message);
		if (Object.prototype.toString.call(message) === '[object Array]') {
			for ( var i = 0; i < message.length; i++) {
				self.handleMessage(message[i]);
			}
		} else {
			self.handleMessage(message);
		}
	});

	this.sendMessage = function(type, path, payload, name) {
		var message = {
			type : type,
			name : name,
			path : path,
			payload : payload
		};
		//console.debug('Sending Message to Server: ',message);
		if (roadrunner_endpoint.readyState == window.WebSocket.OPEN) {
			roadrunner_endpoint.send(JSON.stringify(message));
		} else {
			messages.push(message);
		}
	};

	this.sendSimpleMessage = function(message) {
		//console.debug('Sending Message to Server: ',message);
		if (roadrunner_endpoint.readyState == window.WebSocket.OPEN) {
			roadrunner_endpoint.send(JSON.stringify(message));
		} else {
			messages.push(message);
		}
	};
}

function Roadrunner(path) {
	var events = {};
	var events_once = {};
	var self = this;
	self.path = path;
	var rootPath;
	var parentPath;
	var roadrunner_connection = new RoadrunnerConnection(path);
	roadrunner_connection.handleMessage = function(message) {
		var snapshot = new Snapshot(message, roadrunner_connection);
		var workpath = self.path;
		if (workpath.indexOf("/roadrunner") > -1) {
			workpath = workpath.substr(workpath.lastIndexOf("/roadrunner")+11, workpath.length);
		}
		// Direkt Change start (value)
		{
			try
			{
				if (snapshot.path().indexOf(workpath) < 1) {
					var callback = events['value'];
					if (callback != null) {
						callback(snapshot);
					}
	
					var callback = events_once['value'];
					if (callback != null) {
						callback(snapshot);
						events_once['value'] = null;
					}
				}
			}
			catch (e) {
				
			}
		}
		
		// Endswith /
		if(!(workpath.indexOf("/", workpath.length - "/".length) !== -1))
		{
			workpath = workpath + "/";
		}
		if (snapshot.path().indexOf(workpath) < 1) {
			var callback = events[message.type];
			if (callback != null) {
				callback(snapshot);
			}

			var callback = events_once[message.type];
			if (callback != null) {
				callback(snapshot);
				events_once[message.type] = null;
			}
		}
	};

	this.child = function(childname) {
		return new Roadrunner(path + "/" + childname);
	};

	this.on = function(event_type, callback) {
		events[event_type] = callback;
		roadrunner_connection.sendMessage("attached_listener", path, {
			"type" : event_type
		});
	};

	this.once = function(event_type, callback) {
		events_once[event_type] = callback;
	};

	this.off = function(event_type, callback) {
		events[event_type] = null;
		events_once[event_type] = null;
		roadrunner_connection.sendMessage("detached_listener", path, {
			"type" : event_type
		});
	};

	this.push = function(data) {		
		var name = UUID.generate();
		roadrunner_connection.sendMessage('push', path, data, name);
		return new Roadrunner(path + "/" + name);
	};
	
	this.set = function(data) {		
		roadrunner_connection.sendMessage('set', path, data);
		if (data != null) {
			return new Roadrunner(path);
		} else {
			return null;
		}
	};

	this.parent = function() {
		new Roadrunner(parentPath);
	};

	this.root = function() {
		new Roadrunner(rootPath);
	};

	this.name = function() {
		var name = path.substring(path.lastIndexOf('/') + 1, path.length);
		return name;
	};

	this.update = function(content) {
		roadrunner_connection.sendMessage('set', path, content);
	};
};
