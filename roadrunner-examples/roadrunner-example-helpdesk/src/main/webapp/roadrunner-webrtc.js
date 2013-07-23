function RoadrunnerWebRTC(roadrunner, sourceVideoSelector, remoteVideoSelector) {
	var sourceVideoElement = $(sourceVideoSelector).get(0);
	var remoteVideoElement = $(remoteVideoSelector).get(0);
	var self = this;

	var localStream = null;
	var peerConn = null;
	var started = false;
	var mediaConstraints = {
		'mandatory' : {
			'OfferToReceiveAudio' : true,
			'OfferToReceiveVideo' : true
		}
	};

	// get the local video up
	this.start = function() {
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
		}, function(error) {
			console.error('An error occurred: [CODE ' + error.code + ']');
			return;
		});
	}

	// start the connection upon user request
	this.connect = function() {
		if (!started) {
			self.createPeerConnection();
			peerConn.createOffer(self.setLocalAndSendMessage, function(evt) {
				console.log(evt);
			}, mediaConstraints);
			started = true;
		} else {
			alert("Already started");
		}
	}

	// send SDP via socket connection
	this.setLocalAndSendMessage = function(sessionDescription) {
		peerConn.setLocalDescription(sessionDescription);
		roadrunner.send(sessionDescription);
	}
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
				roadrunner.send({
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
		}, false)
	}

	roadrunner.on('event', function(snapshot) {
		console.log('Websocket message recv:', snapshot.val());
		var evt = snapshot.val();
		if (evt.type === 'offer') {
			console.log("Received offer...")
			if (!started) {
				self.createPeerConnection();
				started = true;
			}
			console.log('Creating remote session description...');
			peerConn.setRemoteDescription(new RTCSessionDescription(evt));
			console.log('Sending answer...');
			peerConn.createAnswer(self.setLocalAndSendMessage, function(evt) {
				console.log(evt);
			}, mediaConstraints);

		} else if (evt.type === 'answer' && started) {
			console.log('Received answer...');
			console.log('Setting remote session description...');
			peerConn.setRemoteDescription(new RTCSessionDescription(evt));

		} else if (evt.type === 'candidate' && started) {
			console.log('Received ICE candidate...');
			var candidate = new RTCIceCandidate({
				sdpMLineIndex : evt.sdpMLineIndex,
				sdpMid : evt.sdpMid,
				candidate : evt.candidate
			});
			console.log(candidate);
			peerConn.addIceCandidate(candidate);

		} else if (evt.type === 'bye' && started) {
			console.log("Received bye");
			stop();
		}
	});
}