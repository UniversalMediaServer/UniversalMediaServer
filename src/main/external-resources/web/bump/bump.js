isTouchDevice = window.screenX == 0 && ('ontouchstart' in window || 'onmsgesturechange' in window);

var bump = (function() {
//	console.log('jquery '+$.fn.jquery);
	var STOPPED = 0;
	var PLAYING = 1;
	var PAUSED = 2;
	var PLAYCONTROL = 1;
	var VOLUMECONTROL = 2;

	var enabled = false;
	var renderer = null;
	var addr = null;
	var state = {'playback':STOPPED,'mute':'true'};
	var editmode = 0
	var selindex = -1;
	var here = null;
	var img = null;
	var sliding = false;

	function start(address, uri, title) {
		if (! enabled) {
			enabled = true;
			addr=address+'/bump/';
			bumpskin();
			hookup();
			setButtons();
			getRenderers();
		}
		here = [title !== undefined ? title:document.title,0,uri !== undefined ? uri:location];
		selindex = -1;
		refresh('{"playlist":[]}');
	}

	function hookup() {
		$('#bplaylist').mousedown(function(){bump.getPlaylist(1);})
			.click(function(){bump.edited(1);})
			.blur(function(){bump.edited(0);});
		$('#brenderers').change(function(){bump.setRenderer();});
		$('#bumpvol').attr('type','range').attr('max',100);
		$('#bumpvol').keyup(function(){bump.sliding(false);})
			.mouseup(function(){bump.sliding(false);})
			.keydown(function(){bump.sliding(true);})
			.mousedown(function(){bump.sliding(true);})
			.change(function(){bump.setVol(this.value);})
			.on('input',function(){bump.setVol(this.value);});
		$('#bumppos').click(function(){bump.settings();});
		$('#bexit').click(function(){bump.exit();});
		$('#bclose').attr('src',img['close']).attr('alt','x');
	}

	function settings() {
		$('#bumpsettings').toggle();
	}

	function setRenderer() {
		renderer = $("#brenderers option:selected").attr('value');
		status();
		getPlaylist();
	}

	function getRenderers() {
		$.get(addr+'renderers', refresh);
	}

	function getPlaylist(mode) {
		editmode = mode||0;
		$.get(addr+'playlist/'+renderer, refresh);
	}

	function press(b) {
		if($('#b'+b+'button').css('opacity')<1) return;
		var query = '';
		var sel = $('#bplaylist option:selected');
		if (sel) {
			var uri = sel.attr('value');
			var title = sel.html();
			query = '?uri='+encodeURIComponent(uri)+'&title='+encodeURIComponent(title);
		}
//		console.log('press: '+addr+b+'/'+renderer+query);
		$.get(addr+b+'/'+renderer+query, refresh);
		selindex = -1;
	}

	function mute() {
		$.get(addr+'mute/'+renderer, refresh);
	}

	function setVol(vol) {
		$.get(addr+'setvolume/'+renderer+'?vol='+vol, refresh);
	}

	function status() {
		if (enabled) {
			$.get(addr+'status/'+renderer, refresh);
		}
	}

	function refresh(data) {
		var vars = $.parseJSON(data);
		if ('uuid' in vars && vars['uuid'] !== renderer) return;
		if ('state' in vars) {
			setState(vars['state']);
		}
		if ('renderers' in vars) {
			setSelect('#brenderers', vars['renderers']);
			setRenderer();
		}
		if (editmode < 2 && 'playlist' in vars) {
			var found = -1;
			var playlist = vars['playlist'];
			for(var i=0; i < playlist.length; i++) {
				if (playlist[i][0] === here[0] && !playlist[i][2].indexOf('$i$') === 0) {
					found = i;
				}
			}
			if (found < 0) {
				vars['playlist'].splice(0, 0, here);
				found = 0;
			}
			setSelect('#bplaylist', vars['playlist'], selindex > -1 ? selindex:found);
			tog('#bremovebutton,#bclearbutton', $('#bplaylist > option').length < 2);
			if (editmode == 1) editmode++;
		}
	}
	
	function setState(newstate) {
		var last = state.playback;
		state = newstate;
		if (state.playback != last) {
			setButtons();
		}
		if (!sliding) {
			$('#bumpvol').val(state.volume);
		}
		$('#bumpvol').attr('disabled', state.mute === 'true');
		$('#bumppos').html(state.position+(state.position == '0:00' ? '' : state.duration == '0:00' ? '' : (' / '+state.duration)));
		status();
	}

	function setSelect(select, opts, index) {
		$(select).html('');
		var override = index!==undefined && index>-1;
		for (var i=0; i<opts.length; i++) {
			var name = opts[i][0];
			var marked = opts[i][1]==1;
			var sel = override ? index==i:marked;
			var val = opts[i][2];
			$(select).append($('<option value="'+val+(sel ? '" selected="selected"':'"')+(marked ? ' class="bselected"':'')+'>'+name+'</option>'));
		}
		setButtons();
		if (!$(select+' option:selected')) {
			$(select+' option[0]').attr('class','bmarked').attr('selected','selected');
		}
	}

	function setButtons() {
		var stopped = state.playback == STOPPED;
//		$('#brenderers').attr('disabled', !stopped);
		tog('#brewbutton,#bstopbutton,#bfwdbutton', stopped);
		tog('#bprevbutton,#bnextbutton', $('#bplaylist > option').length < 2);
		$('#bplaybutton').css({
			background:'url('+img[state.playback==PLAYING ? 'pause':'play']+') no-repeat center center'
		});
		$('#bmutebutton').css({
			background:'url('+img[state.mute === 'true' ? 'mute':'vol']+') no-repeat center center'
		});
	}

	function tog(buttons, off) {
		$(buttons).css({
			cursor:(off ? 'default':'pointer'),
			opacity:(off ? '.3':'1'),
		});
	}

	function setImages(i) {
		img = {}
		var first = true;
		for(var k in i){
			if (first && i[k].indexOf('data:') == 0) {
				img = i;
				break;
			}
			first = false;
			img[k] = addr+'skin.'+i[k];
		}
		return img;
	}

	function addButton(name, parent, tooltip) {
		var b = $('<a class="bumpbtn" id="b'+name+'button" href="javascript:bump.press(\''+name+'\')"'+
			(tooltip ? (' title="'+tooltip+'"'):'')+'/>');
		$(parent).append(b);
		b.css({
			background:'url('+img[name]+') no-repeat center center',
		});
	}

	function fade(e) {
		e.fadeOut('slow', function(){e.remove();});
	}

	function exit() {
		enabled = false;
		fade($('.bumpcontainer'));
	}

	return {
		start: function (addr, uri, title) {
			start(addr, uri, title);
		},
		press: function (b) {
			press(b);
		},
		setImages: function (i) {
			return setImages(i);
		},
		addButton: function (name, parent, tooltip) {
			addButton(name, parent, tooltip);
		},
		setVol: function (v) {
			setVol(v);
		},
		settings: function () {
			settings();
		},
		setRenderer: function () {
			setRenderer();
		},
		getPlaylist: function (mode) {
			getPlaylist(mode);
		},
		enabled: function () {
			return enabled;
		},
		edited: function (ok) {
			if (ok) selindex = $('#bplaylist option:selected').index();
			editmode = 0;
		},
		exit: function () {
			exit();
		},
		sliding: function (bool) {
			sliding = bool;
		},
	}
}());

