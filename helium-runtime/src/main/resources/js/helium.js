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

var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
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
			return new Helium(this.nodePath + "/" + this.nodeName);
		};
		Snapshot.prototype.child = function(childPath) {
			new Helium(this.nodePath + "/" + this.nodeName + "/" + childPath);
		};
		Snapshot.prototype.forEach = function(childAction) {
			var dataRef = new Helium(this.nodePath);
			dataRef.on("child_added", childAction);
		};
		Snapshot.prototype.hasChildren = function() {
			return this.nodeHasChildren;
		};
		Snapshot.prototype.numChildren = function() {
			return this.nodeNumChildren;
		};
		return Snapshot;
	})();

	var HeliumRPC = (function (_super) {
	    __extends(HeliumRPC, _super);
	    function HeliumRPC(uri) {
	        _super.call(this, uri);
	    }
	    HeliumRPC.prototype.attachListener = function (path, event_type) {
	        _super.prototype.sendRpc.call(this, 'attachListener', {
	    		path: path,
	    		event_type: event_type
	    	});
	    };
	    HeliumRPC.prototype.detachListener = function (path, event_type) {
	        _super.prototype.sendRpc.call(this, 'detachListener', {
	    		path: path,
	    		event_type: event_type
	    	});
	    };
	    HeliumRPC.prototype.attachQuery = function (path, query) {
	        _super.prototype.sendRpc.call(this, 'attachQuery', {
	    		path: path,
	    		"query" : stringify(query)
	    	});
	    };
	    HeliumRPC.prototype.detachQuery = function (path, query) {
	        _super.prototype.sendRpc.call(this, 'detachQuery', {
	    		path: path,
	    		"query" : stringify(query)
	    	});
	    };
	    HeliumRPC.prototype.send = function (path, data) {
	        _super.prototype.sendRpc.call(this, 'event', {
	    		path: path,
	    		data: data
	    	});
	    };
	    HeliumRPC.prototype.push = function (path, name, data) {
	        _super.prototype.sendRpc.call(this, 'push', {
	    		path: path,
	    		name: name,
	    		data: data
	    	});
	    };
	    HeliumRPC.prototype.set = function (path, data) {
	        _super.prototype.sendRpc.call(this, 'set', {
	    		path: path,
	    		data: data
	    	});
	    };
	    HeliumRPC.prototype.update = function (path, data) {
	        _super.prototype.sendRpc.call(this, 'update', {
	    		path: path,
	    		data: data
	    	});
	    };
	    HeliumRPC.prototype.delete = function (path) {
	        _super.prototype.sendRpc.call(this, 'delete', {
	    		path: path
	    	});
	    };
	    HeliumRPC.prototype.authenticate = function (username, password) {
	        _super.prototype.sendRpc.call(this, 'authenticate', {
	    		username: username,
	    		password: password
	    	});
	    };
	    HeliumRPC.prototype.pushOnDisconnect = function (path, name, payload) {
	        _super.prototype.sendRpc.call(this, 'pushOnDisconnect', {
	    		path: path,
	    		name: name,
	    		payload: payload
	    	});
	    };
	    HeliumRPC.prototype.setOnDisconnect = function (path, data) {
	        _super.prototype.sendRpc.call(this, 'setOnDisconnect', {
	    		path: path,
	    		data: data
	    	});
	    };
	    HeliumRPC.prototype.updateOnDisconnect = function (path, data) {
	        _super.prototype.sendRpc.call(this, 'updateOnDisconnect', {
	    		path: path,
	    		data: data
	    	});
	    };
	    HeliumRPC.prototype.deleteOnDisconnect = function (path) {
	        _super.prototype.sendRpc.call(this, 'deleteOnDisconnect', {
	    		path: path
	    	});
	    };
	    
	    return HeliumRPC;
	})(RPC);

	var HeliumOnDisconnect = (function() {
		function HeliumOnDisconnect(path, con) {
			this.con = con;
			this.path = path;
		}

		HeliumOnDisconnect.prototype.push = function(payload) {
			var name = UUID.generate();
			this.con.pushOnDisconnect(this.path, name, payload);
		};

		HeliumOnDisconnect.prototype.set = function(payload) {
			this.con.setOnDisconnect(this.path, payload);
		};

		HeliumOnDisconnect.prototype.update = function(payload) {
			this.con.updateOnDisconnect(this.path, payload);
		};
		HeliumOnDisconnect.prototype.delete = function() {
			this.con.deleteOnDisconnect(this.path);
		};
		return HeliumOnDisconnect;
	})();

	var HeliumOnlineSwitch = (function() {
		function HeliumOnlineSwitch(path, connectionHandler) {
			var con = new HeliumConnection(path);
			con.handleMessage = function(message) {

			};
			con.connectionHandler(connectionHandler);
		}
		return HeliumOnlineSwitch;
	})();
	
	var Helium = (function() {
		function Helium(path) {
			var self = this;
			this.events = {};
			this.events_once = {};
			this.path = path;
			this.rootPath;
			this.parentPath;
			this.rpc = new HeliumRPC(path);
			this.rpc.addMessageHandler(function(message) {
				if (self.events[message.type] != null) {
					var snapshot = new Snapshot(message);
					var workpath = self.path;
					// if (workpath.indexOf(snapshot.path()) == 0) {
					if (workpath === snapshot.path() ||
					    (snapshot.path().endsWith("/") && workpath === snapshot.path().substring(0, snapshot.path().length-1))) {
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
		Helium.prototype.child = function(childname) {
			return new Helium(this.path + "/" + childname);
		};
		Helium.prototype.on = function(event_type, callback) {
			this.events[event_type] = callback;
			this.rpc.attachListener(this.path, event_type);
		};
		Helium.prototype.once = function(event_type, callback) {
			this.events_once[event_type] = callback;
		};
		Helium.prototype.off = function(event_type, callback) {
			this.events[event_type] = null;
			this.events_once[event_type] = null;
			this.rpc.detachListener(this.path, event_type);
		};
		Helium.prototype.query = function(query, child_added, child_changed, child_deleted) {
			this.events['query_child_added'] = child_added;
			this.events['query_child_changed'] = child_changed;
			this.events['query_child_deleted'] = child_deleted;
			this.rpc.attachQuery(this.path, stringify(query));
		};
		Helium.prototype.delete_query = function(query) {
			this.rpc.detachQuery(this.path, stringify(query));
		};
		Helium.prototype.send = function(data) {
			this.rpc.send(this.path, data);
		};
		Helium.prototype.push = function(data) {
			var name = UUID.generate();
			this.rpc.push(this.path, data, name);
			return new Helium(this.path + "/" + name);
		};
		Helium.prototype.set = function(data) {
			this.rpc.set(this.path, data);
			if (data != null) {
				return new Helium(this.path);
			} else {
				return null;
			}
		};
		Helium.prototype.update = function(content) {
			this.rpc.update(this.path, data);
			if (content != null) {
				return new Helium(this.path);
			} else {
				return null;
			}
		};
		Helium.prototype.authenticate = function(username, password) {
			this.rpc.authenticate(username, password);
			return this;
		};
		Helium.prototype.delete = function() {
			this.rpc.delete(this.path);
		};
		Helium.prototype.parent = function() {
			new Helium(this.parentPath);
		};

		Helium.prototype.root = function() {
			new Helium(this.rootPath);
		};
		Helium.prototype.name = function() {
			var name = this.path.substring(this.path.lastIndexOf('/') + 1, this.path.length);
			return name;
		};
		Helium.prototype.ref = function() {
			return this;
		};
		Helium.prototype.onDisconnect = function() {
			return new HeliumOnDisconnect(this.path, this.rpc);
		};
		return Helium;
	})();

	window.Helium = Helium;
})(window);
