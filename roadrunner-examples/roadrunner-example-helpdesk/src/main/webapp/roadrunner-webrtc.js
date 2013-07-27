function RoadrunnerWebRTC(ref, sourceVideoSelector, remoteVideoSelector) {
	if (typeof ref == "string") {
    this.roadrunner = new Roadrunner(ref);
  } else {
    this.roadrunner = ref;
  }
	var sourceVideoElement = $(sourceVideoSelector).get(0);
	var remoteVideoElement = $(remoteVideoSelector).get(0);
	var self = this;
	var localStream = null;
	var peerConn = null;
	var mediaConstraints = {
		'mandatory' : {
			'OfferToReceiveAudio' : true,
			'OfferToReceiveVideo' : true
		}
	};

	// get the local video up
	this.start = function(callback) {
		navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || window.navigator.mozGetUserMedia || navigator.msGetUserMedia;
		window.URL = window.URL || window.webkitURL;
		navigator.getUserMedia({
			video : true,
			audio : true
		}, function(stream) {
			localStream = stream;
			if (sourceVideoElement.mozSrcObject) {
				sourceVideoElement.mozSrcObject = stream;
				sourceVideoElement.play();
			} else {
				try {
					sourceVideoElement.src = window.URL.createObjectURL(stream);
					sourceVideoElement.play();
				} catch (e) {
					console.error("Error setting video src: ", e);
				}
			}
			self.createPeerConnection();
			callback();
			self.registerRoadrunner();
		}, function(error) {
			console.error('An error occurred: [CODE ' + error.code + ']');
			return;
		});
	};

	this.handshake = function(){
		self.roadrunner.send({
			type : 'handshake'
		});
	};
	
	// start the connection upon user request
	this.connect = function() {
			if (peerConn == null) {
				self.start();
			}
			peerConn.createOffer(self.setLocalAndSendMessage, function(evt) {
				console.log(evt);
			}, mediaConstraints);
	};

	this.disconnect = function() {
			if (sourceVideoElement.mozSrcObject) {
				sourceVideoElement.mozSrcObject.stop();
				sourceVideoElement.src = null;
			} else {
				sourceVideoElement.src = "";
				localStream.stop();
			}
			self.roadrunner.send(JSON.stringify({
				type : "bye"
			}));
			stop();
			peerConn.close();
			peerConn = null;
	};

	// send SDP via socket connection
	this.setLocalAndSendMessage = function(sessionDescription) {
		peerConn.setLocalDescription(sessionDescription);
		self.roadrunner.send(sessionDescription);
	};

	this.createPeerConnection = function() {
		RTCPeerConnection = webkitRTCPeerConnection || mozRTCPeerConnection;
		var pc_config = {
			"iceServers" : []
		};
		try {
			peerConn = new RTCPeerConnection(pc_config);
		} catch (e) {
			console.error("Failed to create PeerConnection, exception: " + e.message);
		}
		// send any ice candidates to the other peer
		peerConn.onicecandidate = function(evt) {
			if (event.candidate) {
				self.roadrunner.send({
					type : "candidate",
					sdpMLineIndex : evt.candidate.sdpMLineIndex,
					sdpMid : evt.candidate.sdpMid,
					candidate : evt.candidate.candidate
				});
			}
		};
		peerConn.addStream(localStream);

		peerConn.addEventListener("addstream", function(event) {
			remoteVideoElement.src = window.URL.createObjectURL(event.stream);
		}, false);
		peerConn.addEventListener("removestream", function(event) {
			remoteVideoElement.src = "";
		}, false);
	};
	this.registerRoadrunner = function() {
		self.roadrunner.on('event', function(snapshot) {
			console.log('Websocket message recv:', snapshot.val());
			var evt = snapshot.val();
			if (evt.type === 'offer') {
				console.log("Received offer...");
				console.log('Creating remote session description...');
				peerConn.setRemoteDescription(new RTCSessionDescription(evt));
				console.log('Sending answer...');
				peerConn.createAnswer(self.setLocalAndSendMessage, function(evt) {
					console.log(evt);
				}, mediaConstraints);
			} else if (evt.type === 'answer') {
				console.log('Received answer...');
				console.log('Setting remote session description...');
				peerConn.setRemoteDescription(new RTCSessionDescription(evt));

			} else if (evt.type === 'candidate') {
				console.log('Received ICE candidate...');
				var candidate = new RTCIceCandidate({
					sdpMLineIndex : evt.sdpMLineIndex,
					sdpMid : evt.sdpMid,
					candidate : evt.candidate
				});
				console.log(candidate);
				peerConn.addIceCandidate(candidate);

			} else if (evt.type === 'bye') {
				console.log("Received bye");
				stop();
			} else if (evt.type === 'handshake') {
				self.connect();
			}
		});
	};
}