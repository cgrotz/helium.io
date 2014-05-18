var baseRef = new Helium(heliumUrl);

angular.module('admin', [ 'helium' ]).controller('AdminCtrl', [ '$scope', '$timeout', 'angularHelium', 'angularHeliumCollection', function($scope, $timeout, angularHelium, angularHeliumCollection) {
	$scope.links = [];
	$scope.fields = angularHeliumCollection(baseRef);
	
	baseRef.on('child_added', function changeValue(snapshot) {
		if( snapshot.val() instanceof Object)
		{
			$scope.links.push({name: snapshot.name(), url: snapshot.path()});
		}
		else
		{
			//$scope.fields.push({name: snapshot.name(), value: angularHelium(snapshot.ref(), $scope, snapshot.name().replace(":",""), "")});
		}
	});
} ]);
/*
function changeValue(snapshot) {
	if( snapshot.val() instanceof Object)
	{
		var ele = $('#linkList').find('#'+snapshot.name());
		if(ele.length > 0)
		{
			console.log('add Link');
		}
		else if(ele.length > 0)
		{
			console.log('change Link');
		}
	}
	else
	{
		var ele = $('#form').find('#'+snapshot.name());
		if(ele.length > 0)
		{
			console.log('add value');
		}
		else if(ele.length > 0)
		{
			console.log('change value');
		}
	}
}

baseRef.on('child_added', changeValue);
baseRef.on('child_changed', changeValue);
baseRef.on('child_removed', function(snapshot){
	if( snapshot.val() instanceof Object)
	{
		console.log('remove link');
	}
	else
	{
		console.log('remove value');
	}
});*/