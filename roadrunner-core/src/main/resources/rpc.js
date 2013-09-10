var RPC = (function() {
	function RPC(url) {
		var wsurl;
		if (url.indexOf("http://") == 0) {
			wsurl = url.replace('http://', 'ws://');
		} else if (url.indexOf("https://") == 0) {
			wsurl = url.replace('https://', 'wss://');
		} else {
			throw "Illegal URL Schema";
		}
		if (window.rpcEndpoint == null) {
			this.websocket = new ReconnectingWebSocket(wsurl);
			this.websocket.onopen = this.onopen;
			this.websocket.onclose = this.onclose;
			this.websocket.onmessage = this.onmessage;
			this.websocket.onerror = this.onerror;
			this.websocket.handlers = [];
			this.websocket.errors = [];
			this.websocket.messages = [];
			this.websocket.stateListeners = [];
			this.websocket.messageListeners = [];
			window.rpcEndpoint = this.websocket;
		}
		else
		{
			this.websocket = window.rpcEndpoint;
		}
	}
	RPC.prototype.onopen = function(evt) {
		for ( var i = 0; i < this.messages.length; i++) {
			this.send(JSON.stringify(this.messages[i]));
		}
		this.messages = [];
		for ( var i = 0; i < this.stateListeners.length; i++) {
			stateListeners[i](true);
		}
	};
	RPC.prototype.onclose = function(evt) {
		for ( var i = 0; i < this.stateListeners.length; i++) {
			stateListeners[i](false);
		}
	};
	RPC.prototype.addStateListener = function(callback) {
		this.websocket.stateListeners.push(callback);
	};
	RPC.prototype.addMessageHandler = function(callback) {
		this.websocket.messageListeners.push(callback);
	};
	RPC.prototype.onmessage = function(evt) {
		var data = JSON.parse(evt.data);
		if(data.type == 'rpc')
		{
			if (data.state == 'ok' && this.handlers[data.id]) {
				this.handlers[data.id](data.resp);
				delete this.handlers[data.id];
			} else if (data.state == 'error' && this.errors[data.id]) {
				this.errors[data.id](data.resp);
				delete this.errors[data.id];
			}
		}
		else
		{
			for(var i = 0; i < this.messageListeners.length; i++)
			{
				this.messageListeners[i](data);
			}
		}
	};
	RPC.prototype.onerror = function(evt) {
	};
	RPC.prototype.sendRpc = function(method, args, callback, error) {
		var id = UUID.generate();
		var msg = {
			id : id,
			method : method,
			args : args
		};
		if (callback) {
			this.websocket.handlers[id] = callback;
		}
		if (error) {
			this.websocket.errors[id] = error;
		}
		if (this.websocket.readyState == window.WebSocket.OPEN) {
			this.websocket.send(JSON.stringify(msg));
		} else {
			this.websocket.messages.push(msg);
		}
	};
	return RPC;
})();