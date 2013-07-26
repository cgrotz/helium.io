angular.module('helpdesk', [ 'roadrunner' ]).controller('HelpdeskCtrl', [ '$scope', '$timeout', 'angularRoadrunner', 'angularRoadrunnerCollection', function($scope, $timeout, angularRoadrunner, angularRoadrunnerCollection) {
	$scope.name = angularRoadrunner('http://localhost:8080/helpdesk/test/test/name', $scope, 'name', "");
	$scope.job = angularRoadrunner('http://localhost:8080/helpdesk/test/test/job', $scope, 'job', "");
	$scope.country = angularRoadrunner('http://localhost:8080/helpdesk/test/test/country', $scope, 'country', "");
} ]);