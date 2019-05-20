(function() {
	var module = angular.module('ums', ['ums.browse', 'ums.play', 'ums.doc', 'ums.menu', 'ngRoute']);

	module.config(function($routeProvider) {
		$routeProvider.otherwise('/browse/0');
	});

	module.run(function($http, $rootScope, $location){
		$http.get('/ums').then(function(response){
			$rootScope.upnpAllowed = response.data.upnpAllowed;
			$rootScope.upnpControl = response.data.upnpControl;
			$rootScope.messages = response.data.messages;
		});

		$rootScope.$watch(function() {
			return $location.path();
		}, function(newValue) {
			$rootScope.locationPath = newValue
		})
	});
}());