(function() {
	var module = angular.module('ums', ['ums.browse', 'ngRoute']);

	module.config(function($routeProvider) {
		$routeProvider.otherwise('/browse/0');
	});
	
	module.run(function($http, $rootScope){
		$http.get('/ums').then(function(response){
			$rootScope.upnpAllowed = response.data.upnpAllowed;
			$rootScope.upnpControl = response.data.upnpControl;
			$rootScope.messages = response.data.messages;
		});
	});
}());