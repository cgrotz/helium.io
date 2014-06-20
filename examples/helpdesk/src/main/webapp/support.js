angular.module('helpdesk', [ 'helium' ]).controller('HelpdeskCtrl', [ '$scope', '$timeout', 'angularHelium', 'angularHeliumCollection', 'angularHeliumQuery', function($scope, $timeout, angularHelium, angularHeliumCollection,angularHeliumQuery) {
	var rr = new Helium('http://localhost:8080/helpdesk/repo');
	var webRTC;
	$scope.sessions = angularHeliumQuery(rr,function(val){return (val.status == 'Support requested');});
	
	$scope.support = function(selected_session){
		console.log('Supporting session: '+selected_session.$id,selected_session);
		var session = rr.child(selected_session.$id);
		$scope.name = angularHelium(session.child('name'), $scope, 'name', "");
		$scope.job = angularHelium(session.child('job'), $scope, 'job', "");
		$scope.country = angularHelium(session.child('country'), $scope, 'country', "");
		$scope.status = angularHelium(session.child('status'), $scope, 'status', "");
		$('#processes').hide();
		$('#form').show();
		$('#video').show();
		webRTC = new HeliumWebRTC('http://localhost:8080/helpdesk/repo/rtc', '#webrtc-localVideoElement', '#webrtc-remoteVideoElement');
		webRTC.start(function(){
			webRTC.handshake();
		});
	};
	
	$scope.remove = function(selected_session){
		console.log('Remove session: '+selected_session.$id);
		rr.child(selected_session.$id).set(null);
	};
} ]);