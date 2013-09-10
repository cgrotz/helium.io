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

	var RoadrunnerRPC = (function (_super) {
	    __extends(RoadrunnerRPC, _super);
	    function RoadrunnerRPC(uri) {
	        _super.call(this, uri);
	    }
	    RoadrunnerRPC.prototype.attachListener = function (path, event_type) {
	        _super.prototype.sendRpc.call(this, 'attachListener', {
	    		path: path,
	    		event_type: event_type
	    	});
	    };
	    RoadrunnerRPC.prototype.detachListener = function (path, event_type) {
	        _super.prototype.sendRpc.call(this, 'detachListener', {
	    		path: path,
	    		event_type: event_type
	    	});
	    };
	    RoadrunnerRPC.prototype.attachQuery = function (path, query) {
	        _super.prototype.sendRpc.call(this, 'attachQuery', {
	    		path: path,
	    		"query" : stringify(query)
	    	});
	    };
	    RoadrunnerRPC.prototype.detachQuery = function (path, query) {
	        _super.prototype.sendRpc.call(this, 'detachQuery', {
	    		path: path,
	    		"query" : stringify(query)
	    	});
	    };
	    RoadrunnerRPC.prototype.send = function (path, data) {
	        _super.prototype.sendRpc.call(this, 'event', {
	    		path: path,
	    		data: data
	    	});
	    };
	    RoadrunnerRPC.prototype.push = function (path, name, data) {
	        _super.prototype.sendRpc.call(this, 'push', {
	    		path: path,
	    		name: name,
	    		data: data
	    	});
	    };
	    RoadrunnerRPC.prototype.set = function (path, data, priority) {
	        _super.prototype.sendRpc.call(this, 'set', {
	    		path: path,
	    		data: data,
	    		priority: priority
	    	});
	    };
	    RoadrunnerRPC.prototype.update = function (path, data) {
	        _super.prototype.sendRpc.call(this, 'update', {
	    		path: path,
	    		data: data
	    	});
	    };
	    RoadrunnerRPC.prototype.setPriority = function (path, priority) {
	        _super.prototype.sendRpc.call(this, 'setPriority', {
	    		path: path,
	    		priority: priority
	    	});
	    };
	    RoadrunnerRPC.prototype.pushOnDisconnect = function (path, name, payload) {
	        _super.prototype.sendRpc.call(this, 'pushOnDisconnect', {
	    		path: path,
	    		name: name,
	    		payload: payload
	    	});
	    };
	    RoadrunnerRPC.prototype.setOnDisconnect = function (path, data,priority) {
	        _super.prototype.sendRpc.call(this, 'setOnDisconnect', {
	    		path: path,
	    		data: data,
	    		priority: priority
	    	});
	    };
	    RoadrunnerRPC.prototype.updateOnDisconnect = function (path, data) {
	        _super.prototype.sendRpc.call(this, 'updateOnDisconnect', {
	    		path: path,
	    		data: data
	    	});
	    };
	    RoadrunnerRPC.prototype.removeOnDisconnect = function (path) {
	        _super.prototype.sendRpc.call(this, 'removeOnDisconnect', {
	    		path: path
	    	});
	    };
	    
	    return RoadrunnerRPC;
	})(RPC);

	var RoadrunnerOnDisconnect = (function() {
		function RoadrunnerOnDisconnect(path, con) {
			this.con = con;
			this.path = path;
		}

		RoadrunnerOnDisconnect.prototype.push = function(payload) {
			var name = UUID.generate();
			this.con.pushOnDisconnect(this.path, name, payload);
		};

		RoadrunnerOnDisconnect.prototype.set = function(payload) {
			this.con.setOnDisconnect(this.path, payload);
		};

		RoadrunnerOnDisconnect.prototype.update = function(payload) {
			this.con.updateOnDisconnect(this.path, payload);
		};

		RoadrunnerOnDisconnect.prototype.setWithPriority = function(payload, priority) {
			this.con.setOnDisconnect(this.path, payload, priority);
		};
		RoadrunnerOnDisconnect.prototype.remove = function() {
			this.con.removeOnDisconnect(this.path);
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
			this.rpc = new RoadrunnerRPC(path);
			this.rpc.addMessageHandler(function(message) {
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
			this.rpc.attachListener(this.path, event_type);
		};

		Roadrunner.prototype.once = function(event_type, callback) {
			this.events_once[event_type] = callback;
		};

		Roadrunner.prototype.off = function(event_type, callback) {
			this.events[event_type] = null;
			this.events_once[event_type] = null;
			this.rpc.detachListener(this.path, event_type);
		};

		Roadrunner.prototype.query = function(query, child_added, child_changed, child_removed) {
			this.events['query_child_added'] = child_added;
			this.events['query_child_changed'] = child_changed;
			this.events['query_child_removed'] = child_removed;
			this.rpc.attachQuery(this.path, stringify(query));
		};

		Roadrunner.prototype.remove_query = function(query) {
			this.rpc.detachQuery(this.path, stringify(query));
		};

		Roadrunner.prototype.send = function(data) {
			this.rpc.send(this.path, data);
		};

		Roadrunner.prototype.push = function(data) {
			var name = UUID.generate();
			this.rpc.push(this.path, data, name);
			return new Roadrunner(this.path + "/" + name);
		};

		Roadrunner.prototype.set = function(data) {
			this.rpc.set(this.path, data);
			if (data != null) {
				return new Roadrunner(this.path);
			} else {
				return null;
			}
		};

		Roadrunner.prototype.update = function(content) {
			this.rpc.update(this.path, data);
			if (content != null) {
				return new Roadrunner(this.path);
			} else {
				return null;
			}
		};

		Roadrunner.prototype.setWithPriority = function(data, priority) {
			this.rpc.set(this.path, data, priority);
			if (data != null) {
				return new Roadrunner(this.path);
			} else {
				return null;
			}
		};

		Roadrunner.prototype.setPriority = function(priority) {
			this.rpc.setPriority(this.path, priority);
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
			return new RoadrunnerOnDisconnect(this.path, this.rpc);
		};

		return Roadrunner;
	})();

	window.Roadrunner = Roadrunner;
})(window);
