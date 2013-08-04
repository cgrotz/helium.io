angular.module('helpdesk', [ 'roadrunner' ]).controller('HelpdeskCtrl', [ '$scope', '$timeout', 'angularRoadrunner', 'angularRoadrunnerCollection', 'angularRoadrunnerQuery', function($scope, $timeout, angularRoadrunner, angularRoadrunnerCollection,angularRoadrunnerQuery) {
	var rr = new Roadrunner('http://localhost:8080/helpdesk/repo');
	var webRTC;
	$scope.sessions = angularRoadrunnerQuery(rr,function(val){return (val.status == 'Support requested');});
	
	$scope.support = function(selected_session){
		console.log('Supporting session: '+selected_session.$id,selected_session);
		var session = rr.child(selected_session.$id);
		$scope.name = angularRoadrunner(session.child('name'), $scope, 'name', "");
		$scope.job = angularRoadrunner(session.child('job'), $scope, 'job', "");
		$scope.country = angularRoadrunner(session.child('country'), $scope, 'country', "");
		$scope.status = angularRoadrunner(session.child('status'), $scope, 'status', "");
		$('#processes').hide();
		$('#form').show();
		$('#video').show();
		webRTC = new RoadrunnerWebRTC('http://localhost:8080/helpdesk/repo/rtc', '#webrtc-localVideoElement', '#webrtc-remoteVideoElement');
		webRTC.start(function(){
			webRTC.handshake();
		});
	};
	
	$scope.remove = function(selected_session){
		console.log('Remove session: '+selected_session.$id);
		rr.child(selected_session.$id).set(null);
	};
} ]);