angular.module('helpdesk', [ 'roadrunner' ]).controller('HelpdeskCtrl', [ '$scope', '$timeout', 'angularRoadrunner', 'angularRoadrunnerCollection', function($scope, $timeout, angularRoadrunner, angularRoadrunnerCollection) {
	var rr = new Roadrunner('http://localhost:8080/helpdesk/repo').push({});
	var webRTC;
	$scope.name = angularRoadrunner(rr.child('name'), $scope, 'name', "");
	$scope.job = angularRoadrunner(rr.child('job'), $scope, 'job', "");
	$scope.country = angularRoadrunner(rr.child('country'), $scope, 'country', "");
	$scope.status = angularRoadrunner(rr.child('status'), $scope, 'status', "");
	
	$scope.requestSupport =function(){
		$('#support').hide();
		$('#video').show();
		rr.child('status').set('Support requested');
		console.log('Requesting support for session: '+rr.name());
		webRTC = new RoadrunnerWebRTC('http://localhost:8080/repo/rtc', '#webrtc-localVideoElement', '#webrtc-remoteVideoElement');
		webRTC.start(function(){
			webRTC.handshake();
		});
	};
} ]);