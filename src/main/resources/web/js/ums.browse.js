(function() {
	var module = angular.module('ums.browse', ['ngRoute']);

	module.config(function($routeProvider) {
		$routeProvider.when('/browse/:id', {
			controller: 'browseController',
			templateUrl: '/templates/browse.html'
		});
	});

	function browseService($http) {
		return {
			get : function(id) {
				return $http.get('/browse/' + id);
			}
		};
	}

	module.service('browseService', browseService);

	function browseController($scope, browseService, $route, $window, $location, $cookies) {
		browseService.get($route.current.params.id).then(function(response) {
			$scope.medias = response.data.media;
			$scope.folders = response.data.folders;
			$scope.parentId = response.data.parentId;
		});
		$scope.$watch(function() {
			return $cookies.get('pad');
		}, function(newValue) {
			$scope.pad = isNaN(newValue) ? 0 : newValue;
		});

		$scope.bumpStart = function(media) {
			$window.bump.start($window.location.origin, '/play/' + media.id, media.name);
		};

		$scope.warn = function(media) {
			$window.notify('warn',messages['Web.2']);
		};

		$scope.addToPlaylist = function(media, $event) {
			$event.preventDefault();
			$window.umsAjax('/playlist/add/' + media.id, false)
		};

		$scope.removeFromPlaylist = function(media, $event) {
			$event.preventDefault();
			$window.umsAjax('/playlist/del/' + media.id, true);
		};

		$scope.back = function($event) {
			$event.preventDefault();
			if ($scope.parentId) {
				$location.path('/browse/' + $scope.parentId);
			}
			else {
				// TODO better ways to do this
				$window.history.back();
			}
		};

		$scope.play = function(media) {
			if (media.disabled) {
				$window.notify('warn', $scope.messages['Web.6']);
			}
			else {
				$location.path('/play/' + media.id);
			}
			console.log("play:" + media);
		}
	}

	module.controller('browseController', browseController);
}());