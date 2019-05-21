(function() {
	var module = angular.module('ums.menu', []);

	function menuController($scope, $window, $cookies) {
		$scope.setSmallFont = function($event) {
			$event.preventDefault();
			$window.chooseFontSize('small');
		};
		$scope.setMediumFont = function($event) {
			$event.preventDefault();
			$window.chooseFontSize('medium');
		};
		$scope.setBigFont = function($event) {
			$event.preventDefault();
			$window.chooseFontSize('big');
		};
		$scope.setExtraBigFont = function($event) {
			$event.preventDefault();
			$window.chooseFontSize('extrabig');
		};
		$scope.setPadColor = function($event) {
			$event.preventDefault();
			var pad = $cookies.get('pad') || 0;
			if (isNaN(pad)) {
				pad = 0;
			}

			pad = ++pad % 3;

			var d = new Date();
			d.setFullYear(d.getFullYear()+1);
			$cookies.put('pad', pad, { expires: d, path: '/' });
		};
		$scope.chooseView = function(view, $event) {
			$event.preventDefault();
			$window.chooseView(view);
		};
	}
	module.controller('menuController', menuController);
}());