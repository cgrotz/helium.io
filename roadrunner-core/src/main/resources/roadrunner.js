var stringify = function(obj, prop) {
	var placeholder = '____PLACEHOLDER____';
	var fns = [];
	var json = JSON.stringify(obj, function(key, value) {
		if (typeof value === 'function') {
			fns.push(value);
			return placeholder;
		}
		return value;
	}, 2);
	json = json.replace(new RegExp('"' + placeholder + '"', 'g'), function(
			_) {
		return fns.shift();
	});
	return json;
};
	
(function(window) {
	var Snapshot = (function() {
		function Snapshot(message) {
			this.nodePayload = message.payload;
			this.nodeName = message.name;
			this.nodePath = message.path;
			this.nodeParent = message.parent;
			this.nodeHasChildren = message.hasChildren;
			this.nodeNumChildren = message.numChildren;
			this.nodePriority = message.priority;
		}
		Snapshot.prototype.val = function() {
			return this.nodePayload;
		};
		Snapshot.prototype.name = function() {
			return this.nodeName;
		};
		Snapshot.prototype.parent = function() {
			return this.nodeParent;
		};
		Snapshot.prototype.path = function() {
			return this.nodePath;
		};
		Snapshot.prototype.ref = function() {
			return new Roadrunner(this.nodePath + "/" + this.nodeName);
		};
		Snapshot.prototype.child = function(childPath) {
			new Roadrunner(this.nodePath + "/" + this.nodeName + "/" + childPath);
		};
		Snapshot.prototype.forEach = function(childAction) {
			var dataRef = new Roadrunner(this.nodePath);
			dataRef.on("child_added", childAction);
		};
		Snapshot.prototype.hasChildren = function() {
			return this.nodeHasChildren;
		};
		Snapshot.prototype.numChildren = function() {
			return this.nodeNumChildren;
		};
		Snapshot.prototype.getPriority = function() {
			return this.nodePriority;
		};
		return Snapshot;
	})();

	var RoadrunnerConnection = (function() {
		function RoadrunnerConnection(url) {
			this.url = url;
			this.messageHandlers = [];
			if (window.roadrunnerEndpoint == null) {
				var wsurl;
				if (url.indexOf("http://") == 0) {
					wsurl = url.replace('http://', 'ws://');
				} else if (url.indexOf("https://") == 0) {
					wsurl = url.replace('https://', 'wss://');
				} else {
					throw "Illegal URL Schema";
				}
				var endpoint = new ReconnectingWebSocket(wsurl);
				endpoint.messages = [];
				endpoint.connectionHandlers = [];
				endpoint.onopen = function(event) {
					console.debug("Connection established resyncing");
					for ( var i = 0; i < this.messages.length; i++) {
						endpoint.send(stringify(this.messages[i]));
					}
					this.messages = [];
					for ( var i = 0; i < this.connectionHandlers.length; i++) {
						this.connectionHandlers[i](true);
					}
				};
				endpoint.onclose = function(event) {
					console.debug("Connection lost trying reconnecting");
					for ( var i = 0; i < this.connectionHandlers.length; i++) {
						this.connectionHandlers[i](false);
					}
				};
				endpoint.onmessage = function(event) {
					var message = JSON.parse(event.data);
					console.debug('Receiving Message from Server: ', message);
					for ( var i = 0; i < endpoint.eventhandlers.length; i++) {
						var handler = endpoint.eventhandlers[i];
						handler(message);
					}
				};
				window.roadrunnerEndpoint = endpoint;
			}
		}
		RoadrunnerConnection.prototype.addMessageHandler = function(messageHandler) {
			if(window.roadrunnerEndpoint.eventhandlers == undefined)
			{
				window.roadrunnerEndpoint.eventhandlers = [];
			}
			window.roadrunnerEndpoint.eventhandlers.push(function(message) {
				if (Object.prototype.toString.call(message) === '[object Array]') {
					for ( var i = 0; i < message.length; i++) {
						messageHandler(message[i]);
					}
				} else {
					messageHandler(message);
				}
			});
		};
		RoadrunnerConnection.prototype.connectionHandler = function(connectionHandler) {
			window.roadrunnerEndpoint.connectionHandlers.push(connectionHandler);
		};
		RoadrunnerConnection.prototype.send = function(message) {
			console.debug('Sending Message to Server: ', message);
			if (window.roadrunnerEndpoint.readyState == window.WebSocket.OPEN) {
				window.roadrunnerEndpoint.send(stringify(message));
			} else {
				window.roadrunnerEndpoint.messages.push(message);
			}
		};

		RoadrunnerConnection.prototype.sendMessage = function(type, path,
				payload, name) {
			var message = {
				type : type,
				name : name,
				path : path,
				payload : payload
			};
			console.debug('Sending Message to Server: ', message);
			if (window.roadrunnerEndpoint.readyState == window.WebSocket.OPEN) {
				window.roadrunnerEndpoint.send(stringify(message));
			} else {
				window.roadrunnerEndpoint.messages.push(message);
			}
		};
		RoadrunnerConnection.prototype.sendMessageWithPriority = function(type,
				path, payload, priority) {
			var message = {
				type : type,
				path : path,
				payload : payload,
				priority : priority
			};
			console.debug('Sending Message to Server: ', message);
			if (window.roadrunnerEndpoint.readyState == window.WebSocket.OPEN) {
				window.roadrunnerEndpoint.send(stringify(message));
			} else {
				window.roadrunnerEndpoint.messages.push(message);
			}
		};
		RoadrunnerConnection.prototype.sendPriorityChange = function(type,
				path, priority) {
			var message = {
				type : type,
				path : path,
				priority : priority
			};
			console.debug('Sending Message to Server: ', message);
			if (window.roadrunnerEndpoint.readyState == window.WebSocket.OPEN) {
				window.roadrunnerEndpoint.send(stringify(message));
			} else {
				window.roadrunnerEndpoint.messages.push(message);
			}
		};

		RoadrunnerConnection.prototype.sendSimpleMessage = function(message) {
			console.debug('Sending Message to Server: ', message);
			if (window.roadrunnerEndpoint.readyState == window.WebSocket.OPEN) {
				window.roadrunnerEndpoint.send(stringify(message));
			} else {
				messages.push(message);
			}
		};
		return RoadrunnerConnection;
	})();

	var RoadrunnerOnDisconnect = (function() {
		function RoadrunnerOnDisconnect(path, con) {
			this.con = con;
			this.path = path;
		}

		RoadrunnerOnDisconnect.prototype.push = function(payload) {
			var name = UUID.generate();
			var message = {
				type : 'onDisconnect',
				handler : 'push',
				name : name,
				path : this.path,
				payload : payload
			};
			this.con.send(message);
		};

		RoadrunnerOnDisconnect.prototype.set = function(payload) {
			var message = {
				type : 'onDisconnect',
				handler : 'set',
				path : this.path,
				payload : payload
			};
			this.con.send(message);
		};

		RoadrunnerOnDisconnect.prototype.update = function(payload) {
			var message = {
				type : 'onDisconnect',
				handler : 'update',
				path : this.path,
				payload : payload
			};
			this.con.send(message);
		};

		RoadrunnerOnDisconnect.prototype.setWithPriority = function(payload, priority) {
			var message = {
				type : 'onDisconnect',
				handler : 'set',
				path : this.path,
				payload : payload,
				priority : priority
			};
			this.con.send(message);
		};
		RoadrunnerOnDisconnect.prototype.remove = function() {
			var message = {
				type : 'onDisconnect',
				handler : 'remove',
				path : this.path
			};
			this.con.send(message);
		};
		return RoadrunnerOnDisconnect;
	})();

	var RoadrunnerOnlineSwitch = (function() {
		function RoadrunnerOnlineSwitch(path, connectionHandler) {
			var con = new RoadrunnerConnection(path);
			con.handleMessage = function(message) {

			};
			con.connectionHandler(connectionHandler);
		}
		return RoadrunnerOnlineSwitch;
	})();

	var Roadrunner = (function() {
		function Roadrunner(path) {
			var self = this;
			this.events = {};
			this.events_once = {};
			this.path = path;
			this.rootPath;
			this.parentPath;
			this.roadrunner_connection = new RoadrunnerConnection(path);
			this.roadrunner_connection.addMessageHandler(function(message) {
				if (self.events[message.type] != null) {
					var snapshot = new Snapshot(message);
					var workpath = self.path;
					// if (workpath.indexOf(snapshot.path()) == 0) {
					if (workpath === snapshot.path()) {
						var callback = self.events[message.type];
						if (callback != null) {
							callback(snapshot, message.prevChildName);
						}

						var callback = self.events_once[message.type];
						if (callback != null) {
							callback(snapshot, message.prevChildName);
							self.events_once[message.type] = null;
						}
					}
				}
			});
		}
		Roadrunner.prototype.child = function(childname) {
			return new Roadrunner(this.path + "/" + childname);
		};

		Roadrunner.prototype.on = function(event_type, callback) {
			this.events[event_type] = callback;
			this.roadrunner_connection.sendMessage("attached_listener", this.path, {
				"type" : event_type
			});
		};

		Roadrunner.prototype.once = function(event_type, callback) {
			this.events_once[event_type] = callback;
		};

		Roadrunner.prototype.off = function(event_type, callback) {
			this.events[event_type] = null;
			this.events_once[event_type] = null;
			this.roadrunner_connection.sendMessage("detached_listener", this.path, {
				"type" : event_type
			});
		};

		Roadrunner.prototype.query = function(query, child_added, child_changed, child_removed) {
			this.events['query_child_added'] = child_added;
			this.events['query_child_changed'] = child_changed;
			this.events['query_child_removed'] = child_removed;
			this.roadrunner_connection.sendMessage("attach_query", this.path, {
				"query" : stringify(query)
			});
		};

		Roadrunner.prototype.remove_query = function(query) {
			this.roadrunner_connection.sendMessage("detach_query", this.path, {
				"query" : stringify(query)
			});
		};

		Roadrunner.prototype.send = function(data) {
			this.roadrunner_connection.sendMessage('event', this.path, data, null);
		};

		Roadrunner.prototype.push = function(data) {
			var name = UUID.generate();
			this.roadrunner_connection.sendMessage('push', this.path, data, name);
			return new Roadrunner(this.path + "/" + name);
		};

		Roadrunner.prototype.set = function(data) {
			this.roadrunner_connection.sendMessage('set', this.path, data);
			if (data != null) {
				return new Roadrunner(this.path);
			} else {
				return null;
			}
		};

		Roadrunner.prototype.update = function(content) {
			this.roadrunner_connection.sendMessage('update', this.path, content);
			if (content != null) {
				return new Roadrunner(this.path);
			} else {
				return null;
			}
		};

		Roadrunner.prototype.setWithPriority = function(data, priority) {
			this.roadrunner_connection.sendMessageWithPriority('set', this.path, data,
					priority);
			if (data != null) {
				return new Roadrunner(this.path);
			} else {
				return null;
			}
		};

		Roadrunner.prototype.setPriority = function(priority) {
			this.roadrunner_connection.sendPriorityChange('setPriority', this.path, priority);
			return new Roadrunner(this.path);
		};

		Roadrunner.prototype.parent = function() {
			new Roadrunner(this.parentPath);
		};

		Roadrunner.prototype.root = function() {
			new Roadrunner(this.rootPath);
		};

		Roadrunner.prototype.name = function() {
			var name = this.path.substring(this.path.lastIndexOf('/') + 1, this.path.length);
			return name;
		};

		Roadrunner.prototype.ref = function() {
			return this;
		};

		Roadrunner.prototype.onDisconnect = function() {
			return new RoadrunnerOnDisconnect(this.path, this.roadrunner_connection);
		};

		return Roadrunner;
	})();

	window.Roadrunner = Roadrunner;
})(window);
