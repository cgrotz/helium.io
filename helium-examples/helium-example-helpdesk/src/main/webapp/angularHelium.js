"use strict";

// Define the `helium` module under which all AngularHelium services
// will live.
angular.module("helium", []).value("Helium", Helium);

// Define the `angularHelium` service for implicit syncing.
// `angularHelium` binds a
// model to $scope and keeps the data synchronized with a Helium location
// both ways.
angular.module("helium").factory("angularHelium", [ "$q", "$parse", "$timeout", function($q, $parse, $timeout) {
	// The factory returns a new instance of the `AngularHelium` object,
	// defined
	// below, everytime it is called. The factory takes 4 arguments:
	//
	// * `ref`: A Helium URL or reference. A reference with limits
	// or queries applied may be provided.
	// * `$scope`: The scope with which the bound model is associated.
	// * `name`: The name of the model.
	// * `type`: The type of data that will be stored it the model
	// (or is present on the Helium URL provided). Pass in
	// `{}` for Object, `[]` for Array (default), `""` for
	// String and `true` for Boolean.
	return function(ref, scope, name, type) {
		var af = new AngularHelium($q, $parse, $timeout, ref);
		return af.associate(scope, name, type);
	};
} ]);

// The `AngularHelium` object that implements implicit synchronization.
function AngularHelium($q, $parse, $timeout, ref) {
	this._q = $q;
	this._parse = $parse;
	this._timeout = $timeout;

	this._initial = true;
	this._remoteValue = false;

	// `ref` can either be a string (URL to a Helium location), or a
	// `Helium` object.
	if (typeof ref == "string") {
		this._fRef = new Helium(ref);
	} else {
		this._fRef = ref;
	}
}
AngularHelium.prototype = {
	// This function is called by the factory to create a new 2-way binding
	// between a particular model in a `$scope` and a particular Helium
	// location.
	associate : function($scope, name, type) {
		var self = this;
		if (type == undefined) {
			type = [];
		}
		var deferred = this._q.defer();
		var promise = deferred.promise;
		// We're currently listening for value changes to implement synchronization.
		// This needs to be optimized, see
		// [Ticket #25](https://github.com/helium/angularHelium/issues/25).
		this._fRef.on("value", function(snap) {
			var resolve = false;
			if (deferred) {
				resolve = deferred;
				deferred = false;
			}
			self._remoteValue = type;
			if (snap && snap.val() != undefined) {
				var val = snap.val();
				// If the remote type doesn't match what was provided, log a message
				// and exit.
				if (typeof val != typeof type) {
					self._log("Error: type mismatch");
					return;
				}
				// Also distinguish between objects and arrays.
				var check = Object.prototype.toString;
				if (check.call(type) != check.call(val)) {
					self._log("Error: type mismatch");
					return;
				}
				self._remoteValue = angular.copy(val);
				// If the new remote value is the same as the local value, ignore.
				if (angular.equals(val, self._parse(name)($scope))) {
					return;
				}
			}
			// Update the local model to reflect remote changes.
			self._timeout(function() {
				self._resolve($scope, name, resolve, self._remoteValue)
			});
		});
		return promise;
	},

	// Disassociation added via
	// [pull request
	// #34](https://github.com/helium/angularHelium/pull/34).
	// This function is provided to the promise returned by `angularHelium`
	// when it is fulfilled. Invoking it will stop the two-way synchronization.
	disassociate : function() {
		var self = this;
		if (self._unregister) {
			self._unregister();
		}
		this._fRef.off("value");
	},

	// If `deferred` is a valid promise, it will be resolved with `val`, and
	// the model will be watched for future (local) changes. `$scope[name]`
	// will also be updated to the provided value.
	_resolve : function($scope, name, deferred, val) {
		var self = this;
		this._parse(name).assign($scope, angular.copy(val));
		this._remoteValue = angular.copy(val);
		if (deferred) {
			deferred.resolve(function() {
				self.disassociate();
			});
			this._watch($scope, name);
		}
	},

	// Watch for local changes.
	_watch : function($scope, name) {
		var self = this;
		self._unregister = $scope.$watch(name, function() {
			// When the local value is set for the first time, via the .on('value')
			// callback, we ignore it.
			if (self._initial) {
				self._initial = false;
				return;
			}
			// If the new local value matches the current remote value, we don't
			// trigger a remote update.
			var val = JSON.parse(angular.toJson(self._parse(name)($scope)));
			if (angular.equals(val, self._remoteValue)) {
				return;
			}
			self._fRef.ref().set(val);
		}, true);
		// Also watch for scope destruction and unregister.
		$scope.$on("$destroy", function() {
			self.disassociate();
		});
	},

	// Helper function to log messages.
	_log : function(msg) {
		if (console && console.log) {
			console.log(msg);
		}
	}
};

// Define the `angularHeliumCollection` service for explicit syncing.
// `angularHeliumCollection` provides a collection object that you can
// modify.
// [Original
// code](https://github.com/petebacondarwin/angular-helium/blob/master/ng-helium-collection.js)
// by @petebacondarwin.
angular.module("helium").factory("angularHeliumCollection", [ "$timeout", function($timeout) {
	return function(collectionUrlOrRef, initialCb) {
		// An internal representation of a model present in the collection.
		function angularHeliumItem(ref, index) {
			this.$ref = ref.ref();
			this.$id = ref.name();
			this.$index = index;
			angular.extend(this, {
				priority : ref.getPriority()
			}, ref.val());
		}

		var indexes = {};
		var collection = [];

		// The provided ref can either be a string (URL to a Helium location)
		// or an object of type `Helium`. Helium objects with limits or
		// queries applies may also be provided.
		var collectionRef;
		if (typeof collectionUrlOrRef == "string") {
			collectionRef = new Helium(collectionUrlOrRef);
		} else {
			collectionRef = collectionUrlOrRef;
		}

		function getIndex(prevId) {
			return prevId ? indexes[prevId] + 1 : 0;
		}

		// Add an item to the local collection.
		function addChild(index, item) {
			indexes[item.$id] = index;
			collection.splice(index, 0, item);
		}

		// Remove an item from the local collection.
		function removeChild(id) {
			var index = indexes[id];
			collection.splice(index, 1);
			indexes[id] = undefined;
		}

		// Update an existing child in the local collection.
		function updateChild(index, item) {
			collection[index] = item;
		}

		// Move an existing child to a new location in the collection (usually
		// triggered by a priority change).
		function moveChild(from, to, item) {
			collection.splice(from, 1);
			collection.splice(to, 0, item);
			updateIndexes(from, to);
		}

		// Update the index table.
		function updateIndexes(from, to) {
			var length = collection.length;
			to = to || length;
			if (to > length) {
				to = length;
			}
			for ( var index = from; index < to; index++) {
				var item = collection[index];
				item.$index = indexes[item.$id] = index;
			}
		}

		// Trigger the initial callback, if one was provided.
		if (initialCb && typeof initialCb == "function") {
			collectionRef.once("value", initialCb);
		}

		// Attach handlers for remote child added, removed, changed and moved
		// events.

		collectionRef.on("child_added", function(data, prevId) {
			$timeout(function() {
				var index = getIndex(prevId);
				addChild(index, new angularHeliumItem(data, index));
				updateIndexes(index);
			});
		});

		collectionRef.on("child_removed", function(data) {
			$timeout(function() {
				var id = data.name();
				var pos = indexes[id];
				removeChild(id);
				updateIndexes(pos);
			});
		});

		collectionRef.on("child_changed", function(data, prevId) {
			$timeout(function() {
				var index = indexes[data.name()];
				var newIndex = getIndex(prevId);
				var item = new angularHeliumItem(data, index);

				updateChild(index, item);
				if (newIndex !== index) {
					moveChild(index, newIndex, item);
				}
			});
		});

		// `angularHeliumCollection` exposes three methods on the collection
		// returned.

		// Add an object to the remote collection. Adding an object is the
		// equivalent of calling `push()` on a Helium reference.
		collection.add = function(item, cb) {
			var ref;
			if (!cb) {
				ref = collectionRef.push(item);
			} else {
				ref = collectionRef.push(item, cb);
			}
			return ref;
		};

		// Remove an object from the remote collection.
		collection.remove = function(itemOrId, cb) {
			var item = angular.isString(itemOrId) ? collection[indexes[itemOrId]] : itemOrId;
			if (!cb) {
				item.$ref.remove();
			} else {
				item.$ref.remove(cb);
			}
		};

		// Update an object in the remote collection.
		collection.update = function(itemOrId, cb) {
			var item = angular.isString(itemOrId) ? collection[indexes[itemOrId]] : itemOrId;
			var copy = {};
			// Update all properties, unless they're ones created by Angular.
			angular.forEach(item, function(value, key) {
				if (key.indexOf("$") !== 0) {
					copy[key] = value;
				}
			});
			if (!cb) {
				item.$ref.set(copy);
			} else {
				item.$ref.set(copy, cb);
			}
		};

		return collection;
	};
} ]);

angular.module("helium").factory("angularHeliumQuery", [ "$timeout", function($timeout) {
	return function(collectionUrlOrRef, query, initialCb) {
		// An internal representation of a model present in the collection.
		function angularHeliumItem(ref, index) {
			this.$ref = ref.ref();
			this.$id = ref.name();
			this.$index = index;
			angular.extend(this, {
				priority : ref.getPriority()
			}, ref.val());
		}

		var indexes = {};
		var collection = [];

		// The provided ref can either be a string (URL to a Helium location)
		// or an object of type `Helium`. Helium objects with limits or
		// queries applies may also be provided.
		var collectionRef;
		if (typeof collectionUrlOrRef == "string") {
			collectionRef = new Helium(collectionUrlOrRef);
		} else {
			collectionRef = collectionUrlOrRef;
		}

		function getIndex(prevId) {
			return prevId ? indexes[prevId] + 1 : 0;
		}

		// Add an item to the local collection.
		function addChild(index, item) {
			indexes[item.$id] = index;
			collection.splice(index, 0, item);
		}

		// Remove an item from the local collection.
		function removeChild(id) {
			var index = indexes[id];
			collection.splice(index, 1);
			indexes[id] = undefined;
		}

		// Update an existing child in the local collection.
		function updateChild(index, item) {
			collection[index] = item;
		}

		// Move an existing child to a new location in the collection (usually
		// triggered by a priority change).
		function moveChild(from, to, item) {
			collection.splice(from, 1);
			collection.splice(to, 0, item);
			updateIndexes(from, to);
		}

		// Update the index table.
		function updateIndexes(from, to) {
			var length = collection.length;
			to = to || length;
			if (to > length) {
				to = length;
			}
			for ( var index = from; index < to; index++) {
				var item = collection[index];
				item.$index = indexes[item.$id] = index;
			}
		}

		// Trigger the initial callback, if one was provided.
		if (initialCb && typeof initialCb == "function") {
			collectionRef.once("value", initialCb);
		}

		// Attach handlers for remote child added, removed, changed and moved
		// events.
		collectionRef.query(query, function(data, prevId) {
			$timeout(function() {
				var index = getIndex(prevId);
				addChild(index, new angularHeliumItem(data, index));
				updateIndexes(index);
			});
		}, function(data, prevId) {
			$timeout(function() {
				var index = indexes[data.name()];
				var newIndex = getIndex(prevId);
				var item = new angularHeliumItem(data, index);

				updateChild(index, item);
				if (newIndex !== index) {
					moveChild(index, newIndex, item);
				}
			});
		}, function(data) {
			$timeout(function() {
				var id = data.name();
				var pos = indexes[id];
				removeChild(id);
				updateIndexes(pos);
			});
		});

		// `angularHeliumCollection` exposes three methods on the collection
		// returned.

		// Add an object to the remote collection. Adding an object is the
		// equivalent of calling `push()` on a Helium reference.
		collection.add = function(item, cb) {
			var ref;
			if (!cb) {
				ref = collectionRef.push(item);
			} else {
				ref = collectionRef.push(item, cb);
			}
			return ref;
		};

		// Remove an object from the remote collection.
		collection.remove = function(itemOrId, cb) {
			var item = angular.isString(itemOrId) ? collection[indexes[itemOrId]] : itemOrId;
			if (!cb) {
				item.$ref.remove();
			} else {
				item.$ref.remove(cb);
			}
		};

		// Update an object in the remote collection.
		collection.update = function(itemOrId, cb) {
			var item = angular.isString(itemOrId) ? collection[indexes[itemOrId]] : itemOrId;
			var copy = {};
			// Update all properties, unless they're ones created by Angular.
			angular.forEach(item, function(value, key) {
				if (key.indexOf("$") !== 0) {
					copy[key] = value;
				}
			});
			if (!cb) {
				item.$ref.set(copy);
			} else {
				item.$ref.set(copy, cb);
			}
		};

		return collection;
	};
} ]);
