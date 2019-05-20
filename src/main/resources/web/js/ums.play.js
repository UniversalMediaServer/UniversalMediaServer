(function() {
	var module = angular.module('ums.play', ['ngRoute', 'ngCookies']);

	module.config(function($routeProvider) {
		$routeProvider.when('/play/:id', {
			controller: 'playController',
			templateUrl: '/templates/play.html'
		});
	});
	
	function umsPoster() {
		return {
			restrict: 'A',
			priority : 100,
			link : function defaultLinkFn(scope, element, attr) {
				scope.$watch(attr['umsPoster'], function(newValue, oldValue) {
					if (newValue) {
						attr.$set('poster', '/thumb/' + newValue);
					}
				});
			}
		}
	}
	module.directive('umsPoster', umsPoster);

	function playService($http) {
		var state = {};
		return {
			get : function(id) {
				return $http.get('/play/' + id);
			},

			status: function(k, v, wait) {
				//console.log('status '+JSON.stringify(arguments));
				state[k] = v;
				if (! wait) {
					$http.post('/playerstatus/', JSON.stringify(state));
				}
			}
		};
	}
	module.service('playService', playService);


	function htmlPlayback($timeout, $window, playService) {
		return {
			templateUrl: '/templates/html-playback.html',
			link: function($scope, element) {
				$timeout(function() {
					var player = document.getElementById('player');
					var volumeStatus = function () {
						playService.status('mute', player.muted ? '1' : '0', true);
						playService.status('volume', (player.volume * 100).toFixed(0));
					};
					player.addEventListener('playing', function(){playService.status('playback', 'PLAYING');});
					player.addEventListener('play', function(){playService.status('playback', 'PLAYING');});
					player.addEventListener('pause', function(){playService.status('playback', 'PAUSED');});
					player.addEventListener('canplay', function(){playService.status('playback', 'STOPPED');});
					player.addEventListener('ended', function(){playService.status('playback', 'STOPPED');});
					player.addEventListener('error', function(){playService.status('playback', 'STOPPED');});
					player.addEventListener('timeupdate', function(){playService.status('position', player.currentTime.toFixed(0));});
					player.addEventListener('volumechange', volumeStatus);

					if ($scope.media.autoContinue) {
						player.on('ended', $scope.$apply($scope.next), false);
					}
					if ($scope.media.autoplay) {
						player.play();
					}
					if ($scope.media.push) {
						$scope.play = function() {
							player.play()
						};
						$scope.pause = function() {player.pause(!player.paused)};
						$scope.stop = function() {player.pause()};
						$scope.setvolume = function() {
							player.volume = arg/100
						};
						
						$scope.mute = function () {player.muted = !player.muted};
						// TODO what is this for
						$window.control = function (op, arg) {
							//console.log('control '+JSON.stringify(arguments));
							var player = document.getElementById('player');
							switch (op) {
								case 'play':
									player.play();
									break;
								case 'pause':
									;
									break;
								case 'stop':
									player.pause();
									break;
								case 'setvolume':
									;
									break;
								case 'mute':
									player.muted = !player.muted;
									break;
							}
						}
					}

					$scope.$on('$destroy', function() {
						playService.status('playback','STOPPED');
					});

					// Send initial status
					volumeStatus();
				});
			}
		};
	}
	module.directive('htmlPlayback', htmlPlayback);


	function imagePlayback($location, $timeout) {
		return {
			templateUrl: '/templates/image-playback.html',
			link: function($scope) {
				if ($scope.media.delay  && $scope.media.nextStep.id) {
					$timeout(function() {
						$location.path('/play/' + $scope.media.nextStep.id);
					}, $scope.media.delay);
				}

				var img, imgcontainer, zoomed = false;

				$scope.fit = function(event) {
					var rx = 0, ry = 0;
					zoomed = !zoomed;
	 				if (!zoomed) {
						img.addClass('imgfit');
					} else {
						// Get the relative click point
						var pos = img.offset();
						rx = (event.clientX - pos.left) / img.width();
						ry = (event.clientY - pos.top) / img.height();
						img.removeClass('imgfit');
					}
					$scope.zoom(rx, ry);
				};

				$scope.zoom = function(rx, ry) {
					var w = img.prop('naturalWidth'),
						h = img.prop('naturalHeight');
					if (w > img.width() || h > img.height()) {
						img.addClass('zoomin');
						img.removeClass('zoomout');
						window.scrollTo(0, 0);
						imgcontainer.addClass('noScroll');
					} else {
						img.removeClass('zoomin');
						if (w > imgcontainer.width() || h > imgcontainer.height()) {
							imgcontainer.removeClass('noScroll');
							img.addClass('zoomout');
							if (rx || ry) {
								// Center on/near the relative click point
								var pos = img.offset();
								window.scrollTo(pos.left + rx * w - $(window).width() / 2,
									pos.top + ry * h - $(window).height() / 2);
							}
						} else {
							img.removeClass('zoomout');
						}
					}
				};

				$timeout(function() {
						if (!img) {
							img = $('#Image');
							imgcontainer = $('#ImageContainer');
						}
						$('body').height($(window).height());
						var top = $('#Menu').height();
						imgcontainer.css({ top: '' + top + 'px' });
						imgcontainer.height($('body').height() - top);
						$scope.zoom();
				});
			}
		};
	}
	module.directive('imagePlayback', imagePlayback);


	function flashPlayback() {
		return {
			templateUrl: '/templates/flash-playback.html',
			link: function($scope) {
				var api = $('.player').flowplayer({
					ratio: 9 / 16,
					flashfit: true
				});
				api.bind('load', function(){status('playback', 'PLAYING');});
				api.bind('pause',  function(){status('playback', 'PAUSED');});
				api.bind('resume',  function(){status('playback', 'PLAYING');});
				api.bind('stop', function(){status('playback','STOPPED');});
				api.bind('finish', function(){status('playback', 'STOPPED');});
				api.bind('unload', function(){status('playback', 'STOPPED');});
				api.bind('progress', function(e, api, time){status('position', time.toFixed(0));});
				api.bind('mute', function(e, api){status('mute', api.muted ? '1' : '0');});
				api.bind('volume', function(e, api, vol){status('volume', (vol * 100).toFixed(0));});
				if ($scope.autoContinue) {
					api.bind('finish', next);
				}
				$scope.$on('$destroy', function(){status('playback','STOPPED');});

				// Send initial status
				status('mute', api.muted ? '1' : '0', true);
				status('volume', (api.volume * 100).toFixed(0));

				if ($scope.media.push) {
					window.control = function(op, arg) {
						//console.log('control '+JSON.stringify(arguments));
						var api = flowplayer();
						switch (op) {
							case 'play':
								api.play();
								break;
							case 'pause':
								api.pause();
								break;
							case 'stop':
								api.stop();
								break;
							case 'setvolume':
								api.volume(arg/100);
								break;
							case 'mute':
								api.mute(!api.muted);
								break;
						}
					}
				}
			}
		};
	}
	module.directive('flashPlayback', flashPlayback);


	function playController($scope, playService, $route, $window, $location, $cookies) {
		$scope.$watch(function() {
			return $cookies.get('htmlPlayback');
		}, function(newValue, oldValue) {
			$scope.htmlPlayback = newValue !== 'false';
		})

		playService.get($route.current.params.id).then(function(response) {
			$scope.media = response.data;

		});

		$scope.next = function() {
			$location.path('/play/' + $scope.media.nextStep.id);
		}

		$scope.html5 = function(enabled) {
			$cookies.put('htmlPlayback', enabled);
		}

		$scope.prev = function() {
			$location.path('/play/' + $scope.media.prevStep.id);
		}

		$scope.getPlaylistTitle = function() {
			return $scope.media.inPlaylist ? $scope.messages['Web.4'] : $scope.messages['Web.5'];
		}

		$scope.getPlaylistSymbol = function() {
			return $scope.media.inPlaylist ? '+' : '-';
		}

		$scope.changePlaylist = function($event) {
			$event.preventDefault();
			$window.umsAjax('/playlist/' + ($scope.media.inPlaylist ? 'del' : 'add') + '/' + $scope.media.id, false);
		}
	}

	module.controller('playController', playController);
}());