var bump = (function() {
//	console.log('jquery '+$.fn.jquery);
	var STOPPED = 0;
	var PLAYING = 1;
	var PAUSED = 2;
	var PLAYCONTROL = 1;
	var VOLUMECONTROL = 2;

	var isTouchDevice = window.screenX == 0 && ('ontouchstart' in window || 'onmsgesturechange' in window);
	var enabled = false;
	var renderer = null;
	var addr = null;
	var state = {'playback':STOPPED,'mute':'true'};
	var editmode = 0
	var selindex = -1;
	var here = null;
	var icons = null;

	function start(address, uri, title) {
		if (! enabled) {
			enabled = true;
			skin(address);
			hookup();
			setButtons();
			addr=address+'/bump/';
			getRenderers();
			status();
		}
		here = [title !== undefined ? title:document.title,0,uri !== undefined ? uri:location];
		selindex = -1;
		refresh('{"playlist":[]}');
	}

	// -------------------------------------------
	// edit this function to customize appearance
	// -------------------------------------------
	
	function skin(address) {
		
		// icons
		
		icons = {
			'prev'   :address+'/files/bump/skin/prev16.png',
			'rew'    :address+'/files/bump/skin/rew16.png',
			'play'   :address+'/files/bump/skin/play16.png',
			'pause'  :address+'/files/bump/skin/pause16.png',
			'stop'   :address+'/files/bump/skin/stop16.png',
			'fwd'    :address+'/files/bump/skin/fwd16.png',
			'next'   :address+'/files/bump/skin/next16.png',
			'vol'    :address+'/files/bump/skin/vol16.png',
			'mute'   :address+'/files/bump/skin/mute16.png',
			'add'    :address+'/files/bump/skin/add16.png',
			'remove' :address+'/files/bump/skin/remove16.png',
			'close'  :address+'/files/bump/skin/close16.png',
		};
		
		// layout (rearrange/retag but preserve classes+ids)
		
		$('body').append([
			'<div class="bumpcontainer"><table class="bumppanel"><tr>',
			'<td id="bumpsettings">',
				'<select id="bplaylist"/>',
				'<span id="bumpadd" title="Add to playlist"/>',
				'<span id="bumprm" title="Remove from playlist"/>',
				'<select id="brenderers""/>',
			'</td>',
			'<td><input id="bumpvol"/></td>',
			'<td id="bumpmute"/>',
			'<td id="bumpctrl"/>',
			'<td id="bumppos" title="show/hide playlist">00:00</td>',
			'<td><div id="bexit"><img id="bclose"/></div></td>',
			'</tr></table></div>'
		].join(''));

		addButton('prev', '#bumpctrl');
		addButton('rew', '#bumpctrl');
		addButton('play', '#bumpctrl');
		addButton('stop', '#bumpctrl');
		addButton('fwd', '#bumpctrl');
		addButton('next', '#bumpctrl');
		addButton('mute', '#bumpmute');
		addButton('add', '#bumpadd');
		addButton('remove', '#bumprm');

		// css
		
		$('.bumpcontainer').css({
			position:'fixed',
			zIndex:'2147483647',
			right:'22px',
			top:'55px',
		});

		$('* .bumppanel').css({
			verticalAlign:'middle',
//			maxWidth:'200px',
			font:'normal sans-serif '+(isTouchDevice?'6px':'4px'),
			fontWeight:'500',
			textDecoration:'none',
			textAlign:'center',
			color:'#fff',
			backgroundColor:'#729fcf',
			outline:'none',
			margin:'0',
			padding:'0',
			border:'0',
			borderColor:'#729fcf',
			borderRadius:'2px',
			'-moz-border-radius':'2px',
			'-webkit-border-radius':'2px',
			boxShadow:' 16px 4px 2px rgba(136,136,136,0.5)',
			'-moz-box-shadow':'16px 4px 2px rgba(136,136,136,0.5)',
			'-webkit-box-shadow':'16px 4px 2px rgba(136,136,136,0.5)',
			appearance:'none',
			'-moz-appearance':'none',
			'-webkit-appearance':'none',
		});

		$('.bumpbtn').css({
			display:'inline-block',
			width:isTouchDevice?'32px':'24px',
			height:isTouchDevice?'24px':'16px',
		});

		$('#bumpvol').css({
			width:'70px',
		});

		$('#bumppos').css({
			cursor:'pointer',
		});

		$('#bexit').css({
			verticalAlign:'top',
			marginRight:'-20px',
			color:'#fff',
			cursor:'pointer',
		});

		$('#brenderers').css({
			maxWidth:'120px',
		});

		$('#bplaylist').css({
			maxWidth:'360px',
		});

		// future styles for as yet uncreated elements go here
		$('head').append(['<style>',
			'.bumpcontainer .bselected {font-style:italic;color:blue;}',
			'</style>',
		].join(''));
	}

	function hookup() {
		$('#bplaylist').mousedown(function(){bump.getPlaylist(1);})
			.click(function(){bump.edited(1);})
			.blur(function(){bump.edited(0);});
		$('#brenderers').change(function(){bump.setRenderer();});
		$('#bumpvol').attr('type','range').attr('max',100)
			.change(function(){bump.setVol(this.value);})
			.on('input',function(){bump.setVol(this.value);});
		$('#bumppos').click(function(){bump.settings();});
		$('#bexit').click(function(){bump.exit();});
		$('#bclose').attr('src',icons['close']).attr('alt','x');
	}

	function settings() {
		$('#bumpsettings').toggle();
	}

	function setRenderer() {
		renderer = $("#brenderers option:selected").attr('value');
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
		console.log('press: '+addr+b+'/'+renderer+query);
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
				if (playlist[i][0] === here[0]) {
					found = i;
				}
			}
			if (found < 0) {
				vars['playlist'].splice(0, 0, here);
				found = 0;
			}
			setSelect('#bplaylist', vars['playlist'], selindex > -1 ? selindex:found);
			if (editmode == 1) editmode++;
		}
	}
	
	function setState(newstate) {
		var last = state.playback;
		state = newstate;
		if (state.playback != last) {
			setButtons();
		}
		$('#bumpvol').val(state.volume);
		$('#bumpvol').attr('disabled', state.mute === 'true');
		$('#bumppos').html((state.position === 'null' ? "00:00" : state.position.replace('00:',''))+' / '+state.duration.replace('00:',''));
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
			background:'url('+icons[state.playback==PLAYING ? 'pause':'play']+') no-repeat center center'
		});
		$('#bmutebutton').css({
			background:'url('+icons[state.mute === 'true' ? 'mute':'vol']+') no-repeat center center'
		});
	}

	function tog(buttons, off) {
		$(buttons).css({
			cursor:(off ? 'default':'pointer'),
			opacity:(off ? '.3':'1'),
		});
	}

	function addButton(name, parent) {
		var b = $('<a class="bumpbtn" id="b'+name+'button" href="javascript:bump.press(\''+name+'\')"/>');
		$(parent).append(b);
		b.css({
			background:'url('+icons[name]+') no-repeat center center',
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
	}
}());

