angular.module('helpdesk', [ 'helium' ]).controller('HelpdeskCtrl', [ '$scope', '$timeout', 'angularHelium', 'angularHeliumCollection', function($scope, $timeout, angularHelium, angularHeliumCollection) {
	var rr = new Helium('http://localhost:8080/helpdesk/repo').push({});
	var webRTC;
	$scope.name = angularHelium(rr.child('name'), $scope, 'name', "");
	$scope.job = angularHelium(rr.child('job'), $scope, 'job', "");
	$scope.country = angularHelium(rr.child('country'), $scope, 'country', "");
	$scope.status = angularHelium(rr.child('status'), $scope, 'status', "");
	
	$scope.requestSupport =function(){
		$('#support').hide();
		$('#video').show();
		rr.child('status').set('Support requested');
		console.log('Requesting support for session: '+rr.name());
		webRTC = new HeliumWebRTC('http://localhost:8080/repo/rtc', '#webrtc-localVideoElement', '#webrtc-remoteVideoElement');
		webRTC.start(function(){
			webRTC.handshake();
		});
	};
} ]);