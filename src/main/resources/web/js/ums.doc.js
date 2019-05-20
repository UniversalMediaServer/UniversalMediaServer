(function() {
	var module = angular.module('ums.doc', ['ngRoute']);

	module.config(function($routeProvider) {
		$routeProvider.when('/doc', {
			controller: 'docController',
			templateUrl: '/templates/doc.html'
		});
	});

	function docService($http) {
		return {
			get : function() {
				return $http.get('/doc');
			}
		};
	}
	module.service('docService', docService);

	function docController($scope, docService) {
		docService.get().then(function(response) {
			$scope.logs = response.data.logs;
			$scope.cache = response.data.cache;
		});
	}
	module.controller('docController', docController);
}());