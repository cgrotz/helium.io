/**
 * UUID.js: The RFC-compliant UUID generator for JavaScript.
 *
 * @fileOverview
 * @author  LiosK
 * @version 3.2
 * @license The MIT License: Copyright (c) 2010-2012 LiosK.
 */

/** @constructor */
var UUID;

UUID = (function(overwrittenUUID) {

	// Core Component {{{

	/** @lends UUID */
	function UUID() {
	}

	/**
	 * The simplest function to get an UUID string.
	 * 
	 * @returns {string} A version 4 UUID string.
	 */
	UUID.generate = function() {
		var rand = UUID._getRandomInt, hex = UUID._hexAligner;
		return hex(rand(32), 8) // time_low
				+ "-" + hex(rand(16), 4) // time_mid
				+ "-" + hex(0x4000 | rand(12), 4) // time_hi_and_version
				+ "-" + hex(0x8000 | rand(14), 4) // clock_seq_hi_and_reserved
				// clock_seq_low
				+ "-" + hex(rand(48), 12); // node
	};

	/**
	 * Returns an unsigned x-bit random integer.
	 * 
	 * @param {int}
	 *            x A positive integer ranging from 0 to 53, inclusive.
	 * @returns {int} An unsigned x-bit random integer (0 <= f(x) < 2^x).
	 */
	UUID._getRandomInt = function(x) {
		if (x < 0)
			return NaN;
		if (x <= 30)
			return (0 | Math.random() * (1 << x));
		if (x <= 53)
			return (0 | Math.random() * (1 << 30))
					+ (0 | Math.random() * (1 << x - 30)) * (1 << 30);
		return NaN;
	};

	/**
	 * Returns a function that converts an integer to a zero-filled string.
	 * 
	 * @param {int}
	 *            radix
	 * @returns {function(num&#44; length)}
	 */
	UUID._getIntAligner = function(radix) {
		return function(num, length) {
			var str = num.toString(radix), i = length - str.length, z = "0";
			for (; i > 0; i >>>= 1, z += z) {
				if (i & 1) {
					str = z + str;
				}
			}
			return str;
		};
	};

	UUID._hexAligner = UUID._getIntAligner(16);

	// }}}

	// UUID Object Component {{{

	/**
	 * Names of each UUID field.
	 * 
	 * @type string[]
	 * @constant
	 * @since 3.0
	 */
	UUID.FIELD_NAMES = [ "timeLow", "timeMid", "timeHiAndVersion",
			"clockSeqHiAndReserved", "clockSeqLow", "node" ];

	/**
	 * Sizes of each UUID field.
	 * 
	 * @type int[]
	 * @constant
	 * @since 3.0
	 */
	UUID.FIELD_SIZES = [ 32, 16, 16, 8, 8, 48 ];

	/**
	 * Generates a version 4 {@link UUID}.
	 * 
	 * @returns {UUID} A version 4 {@link UUID} object.
	 * @since 3.0
	 */
	UUID.genV4 = function() {
		var rand = UUID._getRandomInt;
		return new UUID()._init(rand(32), rand(16), // time_low time_mid
		0x4000 | rand(12), // time_hi_and_version
		0x80 | rand(6), // clock_seq_hi_and_reserved
		rand(8), rand(48)); // clock_seq_low node
	};

	/**
	 * Converts hexadecimal UUID string to an {@link UUID} object.
	 * 
	 * @param {string}
	 *            strId UUID hexadecimal string representation
	 *            ("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx").
	 * @returns {UUID} {@link UUID} object or null.
	 * @since 3.0
	 */
	UUID.parse = function(strId) {
		var r, p = /^\s*(urn:uuid:|\{)?([0-9a-f]{8})-([0-9a-f]{4})-([0-9a-f]{4})-([0-9a-f]{2})([0-9a-f]{2})-([0-9a-f]{12})(\})?\s*$/i;
		if (r = p.exec(strId)) {
			var l = r[1] || "", t = r[8] || "";
			if (((l + t) === "") || (l === "{" && t === "}")
					|| (l.toLowerCase() === "urn:uuid:" && t === "")) {
				return new UUID()._init(parseInt(r[2], 16), parseInt(r[3], 16),
						parseInt(r[4], 16), parseInt(r[5], 16), parseInt(r[6],
								16), parseInt(r[7], 16));
			}
		}
		return null;
	};

	/**
	 * Initializes {@link UUID} object.
	 * 
	 * @param {uint32}
	 *            [timeLow=0] time_low field (octet 0-3).
	 * @param {uint16}
	 *            [timeMid=0] time_mid field (octet 4-5).
	 * @param {uint16}
	 *            [timeHiAndVersion=0] time_hi_and_version field (octet 6-7).
	 * @param {uint8}
	 *            [clockSeqHiAndReserved=0] clock_seq_hi_and_reserved field
	 *            (octet 8).
	 * @param {uint8}
	 *            [clockSeqLow=0] clock_seq_low field (octet 9).
	 * @param {uint48}
	 *            [node=0] node field (octet 10-15).
	 * @returns {UUID} this.
	 */
	UUID.prototype._init = function() {
		var names = UUID.FIELD_NAMES, sizes = UUID.FIELD_SIZES;
		var bin = UUID._binAligner, hex = UUID._hexAligner;

		/**
		 * List of UUID field values (as integer values).
		 * 
		 * @type int[]
		 */
		this.intFields = new Array(6);

		/**
		 * List of UUID field values (as binary bit string values).
		 * 
		 * @type string[]
		 */
		this.bitFields = new Array(6);

		/**
		 * List of UUID field values (as hexadecimal string values).
		 * 
		 * @type string[]
		 */
		this.hexFields = new Array(6);

		for ( var i = 0; i < 6; i++) {
			var intValue = parseInt(arguments[i] || 0);
			this.intFields[i] = this.intFields[names[i]] = intValue;
			this.bitFields[i] = this.bitFields[names[i]] = bin(intValue,
					sizes[i]);
			this.hexFields[i] = this.hexFields[names[i]] = hex(intValue,
					sizes[i] / 4);
		}

		/**
		 * UUID version number defined in RFC 4122.
		 * 
		 * @type int
		 */
		this.version = (this.intFields.timeHiAndVersion >> 12) & 0xF;

		/**
		 * 128-bit binary bit string representation.
		 * 
		 * @type string
		 */
		this.bitString = this.bitFields.join("");

		/**
		 * UUID hexadecimal string representation
		 * ("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx").
		 * 
		 * @type string
		 */
		this.hexString = this.hexFields[0] + "-" + this.hexFields[1] + "-"
				+ this.hexFields[2] + "-" + this.hexFields[3]
				+ this.hexFields[4] + "-" + this.hexFields[5];

		/**
		 * UUID string representation as a URN
		 * ("urn:uuid:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx").
		 * 
		 * @type string
		 */
		this.urn = "urn:uuid:" + this.hexString;

		return this;
	};

	UUID._binAligner = UUID._getIntAligner(2);

	/**
	 * Returns UUID string representation.
	 * 
	 * @returns {string} {@link UUID#hexString}.
	 */
	UUID.prototype.toString = function() {
		return this.hexString;
	};

	/**
	 * Tests if two {@link UUID} objects are equal.
	 * 
	 * @param {UUID}
	 *            uuid
	 * @returns {bool} True if two {@link UUID} objects are equal.
	 */
	UUID.prototype.equals = function(uuid) {
		if (!(uuid instanceof UUID)) {
			return false;
		}
		for ( var i = 0; i < 6; i++) {
			if (this.intFields[i] !== uuid.intFields[i]) {
				return false;
			}
		}
		return true;
	};

	// }}}

	// UUID Version 1 Component {{{

	/**
	 * Generates a version 1 {@link UUID}.
	 * 
	 * @returns {UUID} A version 1 {@link UUID} object.
	 * @since 3.0
	 */
	UUID.genV1 = function() {
		var now = new Date().getTime(), st = UUID._state;
		if (now != st.timestamp) {
			if (now < st.timestamp) {
				st.sequence++;
			}
			st.timestamp = now;
			st.tick = UUID._getRandomInt(4);
		} else if (Math.random() < UUID._tsRatio && st.tick < 9984) {
			// advance the timestamp fraction at a probability
			// to compensate for the low timestamp resolution
			st.tick += 1 + UUID._getRandomInt(4);
		} else {
			st.sequence++;
		}

		// format time fields
		var tf = UUID._getTimeFieldValues(st.timestamp);
		var tl = tf.low + st.tick;
		var thav = (tf.hi & 0xFFF) | 0x1000; // set version '0001'

		// format clock sequence
		st.sequence &= 0x3FFF;
		var cshar = (st.sequence >>> 8) | 0x80; // set variant '10'
		var csl = st.sequence & 0xFF;

		return new UUID()._init(tl, tf.mid, thav, cshar, csl, st.node);
	};

	/**
	 * Re-initializes version 1 UUID state.
	 * 
	 * @since 3.0
	 */
	UUID.resetState = function() {
		UUID._state = new UUID._state.constructor();
	};

	/**
	 * Probability to advance the timestamp fraction: the ratio of tick
	 * movements to sequence increments.
	 * 
	 * @type float
	 */
	UUID._tsRatio = 1 / 4;

	/**
	 * Persistent state for UUID version 1.
	 * 
	 * @type UUIDState
	 */
	UUID._state = new function UUIDState() {
		var rand = UUID._getRandomInt;
		this.timestamp = 0;
		this.sequence = rand(14);
		this.node = (rand(8) | 1) * 0x10000000000 + rand(40); // set multicast
		// bit '1'
		this.tick = rand(4); // timestamp fraction smaller than a millisecond
	};

	/**
	 * @param {Date|int}
	 *            time ECMAScript Date Object or milliseconds from 1970-01-01.
	 * @returns {object}
	 */
	UUID._getTimeFieldValues = function(time) {
		var ts = time - Date.UTC(1582, 9, 15);
		var hm = ((ts / 0x100000000) * 10000) & 0xFFFFFFF;
		return {
			low : ((ts & 0xFFFFFFF) * 10000) % 0x100000000,
			mid : hm & 0xFFFF,
			hi : hm >>> 16,
			timestamp : ts
		};
	};

	// }}}

	// Misc. Component {{{

	/**
	 * Reinstalls {@link UUID.generate} method to emulate the interface of
	 * UUID.js version 2.x.
	 * 
	 * @since 3.1
	 * @deprecated Version 2.x. compatible interface is not recommended.
	 */
	UUID.makeBackwardCompatible = function() {
		var f = UUID.generate;
		UUID.generate = function(o) {
			return (o && o.version == 1) ? UUID.genV1().hexString : f
					.call(UUID);
		};
		UUID.makeBackwardCompatible = function() {
		};
	};

	/**
	 * Preserves the value of 'UUID' global variable set before the load of
	 * UUID.js.
	 * 
	 * @since 3.2
	 * @type object
	 */
	UUID.overwrittenUUID = overwrittenUUID;

	// }}}

	return UUID;

})(UUID);

// vim: et ts=2 sw=2 fdm=marker fmr&

// Roadrunner
var roadrunner_endpoint;

function Snapshot(message, roadrunner_connection) {
	var payload = message.payload;
	var name = message.name;
	var priority = message.priority;
	var path = message.path;
	var parent = message.parent;
	var hasChildren = message.hasChildren;
	var numChildren = message.numChildren;
	this.val = function() {
		// console.debug("Snapshot return val",payload);
		return payload;
	};
	this.name = function() {
		// console.debug("Snapshot return name",name);
		return name;
	};
	this.parent = function() {
		// console.debug("Snapshot return path",path);
		return parent;
	};
	this.path = function() {
		// console.debug("Snapshot return path",path);
		return path;
	};
	this.ref = function() {
		console.debug("Snapshot return ref", path);
		return new Roadrunner(path);
	};
	this.getPriority = function() {
		// console.debug("Snapshot return priority",priority);
		return priority;
	};
	this.child = function(childPath) {
		// console.debug("Snapshot return child",childPath);
		new Roadrunner(path + "/" + childPath);
	};
	this.forEach = function(childAction) {
		var dataRef = new Roadrunner(path);
		dataRef.on("child_added", childAction);
	};
	this.hasChildren = function() {
		// console.debug("Snapshot return hasChildren",hasChildren);
		return hasChildren;
	};
	this.numChildren = function() {
		// console.debug("Snapshot return numChildren",numChildren);
		return numChildren;
	};
}

function RoadrunnerConnection(url) {
	var self = this;
	self.url = url;
	var messages = [];
	if (roadrunner_endpoint == null) {
		if (!window.WebSocket) {
			window.WebSocket = window.MozWebSocket;
		}
		if (window.WebSocket) {
			roadrunner_endpoint = new WebSocket(url);
			roadrunner_endpoint.onopen = function(event) {
				for ( var i = 0; i < messages.length; i++) {
					roadrunner_endpoint.send(JSON.stringify(messages[i]));
				}
			};
			roadrunner_endpoint.onclose = function(event) {
			};
		}

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

		console.debug('Receiving Message from Server: ',message);
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
		console.debug('Sending Message to Server: ',message);
		if (roadrunner_endpoint.readyState == window.WebSocket.OPEN) {
			roadrunner_endpoint.send(JSON.stringify(message));
		} else {
			messages.push(message);
		}
	};

	this.sendSimpleMessage = function(message) {
		console.debug('Sending Message to Server: ',message);
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
		// console.debug("Roadrunner return ref data",childname);
		return new Roadrunner(path + "/" + childname);
	};

	this.on = function(event_type, callback) {
		// console.debug("Roadrunner set on handler",event_type);
		events[event_type] = callback;
		roadrunner_connection.sendMessage("attached_listener", path, {
			"type" : event_type
		});
	};

	this.once = function(event_type, callback) {
		// console.debug("Roadrunner set once handler",event_type);
		events_once[event_type] = callback;
	};

	this.off = function(event_type, callback) {
		// console.debug("Roadrunner set off",event_type);
		events[event_type] = null;
		events_once[event_type] = null;
		roadrunner_connection.sendMessage("detached_listener", path, {
			"type" : event_type
		});
	};

	this.push = function(data) {
		// console.debug("Roadrunner push data",data);
		var name = UUID.generate();
		roadrunner_connection.sendMessage('push', path, data, name);
		return new Roadrunner(path + "/" + name);
	};

	this.limit = function(limit) {
		var queryName = UUID.generate();
		var message = {
			type : 'query',
			name : queryName,
			'query' : query
		};
		//roadrunner_connection.queryHandlers[queryName] = callback;
		roadrunner_connection.sendSimpleMessage(message);
		events[queryName] = callback;
	};
	
	this.set = function(data) {
		// console.debug("Roadrunner set data",data);
		roadrunner_connection.sendMessage('set', path, data);
		if (data != null) {
			return new Roadrunner(path);
		} else {
			return null;
		}
	};

	this.auth = function(authToken, onComplete, onCancel) {
	};

	this.unauth = function(data) {
	};

	this.parent = function() {
		// console.debug("Roadrunner return parent",parentPath);
		new Roadrunner(parentPath);
	};

	this.root = function() {
		// console.debug("Roadrunner return root",rootPath);
		new Roadrunner(rootPath);
	};

	this.toString = function() {
		// console.debug("toString");
	};

	this.name = function() {
		var name = path.substring(path.lastIndexOf('/') + 1, path.length);
		// console.debug("Roadrunner return name",name);
		return name;
	};

	this.update = function(content) {
		// console.debug("Roadrunner execute update",content);
		roadrunner_connection.sendMessage('set', path, content);
	};
};
