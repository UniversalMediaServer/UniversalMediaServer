var bump = (function() {
//	console.log('jquery '+$.fn.jquery);
	var STOPPED = 0;
	var PLAYING = 1;
	var PAUSED = 2;
	var PLAYCONTROL = 1;
	var VOLUMECONTROL = 2;

	var icons = {
		'prev'   :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADvSURBVDiN3ZIxSsRAGIXfSyaQwgMEsQgIQmDxDNOtIUtOIWnEytor7PbB2gMMBLTzDDaCEAgshJR2BgbyWwVmYVZZu92vm/dmvn9gBjh6QneR5/lFlmXnaZp+t21rPfuptVZd101zoNw2iqInEVnGcZwDeJnzqqqivu/vST4CeAZw5xX4KMuyGIZhTfIKAEheuv1eQVEUWRiGGxFZ/jYg8IUiooMgeP/r8F7BIXgFJN+mabom+frvGzRN82GMuSG5Ivl5sGDGGNMkSbIQkQcAXyLSuv3OK1hrb5VSZ+M4bt28rmsLYA1go7Xe+XwnwA9UZUlvaHVhfwAAAABJRU5ErkJggg==',
		'rew'    :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAC3SURBVDiN7c8xSgNRFIXhzyHIYNyCWMfKNdg6j6AwnW4inWIr2IiLsHyFxZ1FiLtwD4pPwRkrZQhqirT5y8M9557DhrXZ+k2cz+d7fd/f4jgippBS2sElFjiKiEeYjI1t226XUhZ9319hindomuYEd9hffjYZmXdLKU+YjQ9SSvc4+2tCtXrl//wE5Jxf6ro+xAVev/WIOB+G4RTPKxvknD8i4qaqqhkyPqHrugcc4Bpv67besMQXxRg0zZhvOjUAAAAASUVORK5CYII=',
		'play'   :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACrSURBVDiNY2AYfsDBwYGFgYGBkVj1zOgCurq6k9XV1Zeoqan9dnBwOHP27Nl/+AxgQhdgZGRUZmBgEGBkZOx98eLFFT8/P2+SDEAG////V/v///8WPz+/Hd7e3pokG4BkkDsTE9MlX1/fSLIMwAeIMoCRkXHnv3//9DZv3rwcXY6FgMZbDAwMRZs2bdpKtAv+//9/l4GB4cP///+LJSQkdPBpxgpITUjDAAAARn40yZpHhVUAAAAASUVORK5CYII=',
		'pause'  :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAABsSURBVDiN7ZGxCcAgEEVfQg4cKaULOEhGcAJHyArpXcAy26QVREgRCxWr1L7m4P/jc/yDydILxpgd0ABKqRMgxngUO3jv73p/G4RqwAGklK6iuTIt0ASsv+6eAQ2jLwS+thGRByDnbCtv0vEC04QVoxO3hiAAAAAASUVORK5CYII=',
		'stop'   :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAABaSURBVDiNY2AYBYzoAr6+vhoMDAzlWOT+MzAwdG7evPkGsiALFkPrGBgYInFYyM7AwBCFLMCERRE2MZxy+BQTBQanAf/wqMeQwxYLTQwMDD8ZcEQj0U4bQQAA9u4MULcVw3YAAAAASUVORK5CYII=',
		'fwd'    :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACrSURBVDiN7ZAxCsJAEADnTpGI/sEvWPqIbKFwpa+wULAXKz9heSCEi61vsNJa/IKJaUxsLhACUUFLp9od2CkW/nyNKgcRGQEHYAOsnHOp9wmw11rPoii61gO6tneBJXAKw3DsXQsweZ6fRWRujOm8CpQMlFI7EdlWXA9YZ1l2NMb03wU+pilwKYpi4pybVlwCLIIgGFprb6Vs1w7v+CfGcZx69wBs0xP//IAnzvEyXeExrXgAAAAASUVORK5CYII=',
		'next'   :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADxSURBVDiN1ZLBSsNAEIbnXxsSkIDH3BeEhtI3EHKKgsk+Rk5CqT6Hh977FEtz6C3gG4hQkAg55iRiPARCMp4KWzSs9tb/NvP/fMwwQ3TyOjOLKIomVVXxb8E4js/DMJRSSq8sy899X5gh3/dXaZq+J0lyn2WZY3qe510R0c5xnLXZPwAAkER0AeCxrusXpdStbYXJmMHMl0S0UUpt+75fjuXEmGGAroUQz8wcHQWwyQoAsB2GYQ6g+BcAwCuARGt9k+f57k8TMPMbEX0w80MQBDOtdW6b8OAKTdMsiqK4I6Ifz9S27ZPrutOu675s0BPTN/XJTAaU6DjyAAAAAElFTkSuQmCC',
		'vol'    :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAG2SURBVDiNpZKxa1NRFIe/c98Nebi4uIgKYtEqRLSKBaWglCwmeXnJKI7qJrjZUbMVnPoHdBERJA4hmQRrFkEQbR1cHAp16eYqL76893NJJG1TQvE3fvfc795zOPCfsaMUV6vVUhAEi8Vi8VW73f4D4KYV1uv1+Vqtdmc/D4LgiqT1JElejNkBQaVSuSCp75z7J2g2m5eiKFqW9A4YAI/jOL51QFCr1c4HQdAHTk7yLMvuAhuSFoFngMvzfA3A4jguS1oCkPQQOAVgZq0gCF7nee4lFSVtArvD4fCc9/4XEA4Gg+M+z/MysDJtFlmW3ZP01Dl3UdJ3oFQoFOYlbQFLYRhenTrEiWwDxyQtm9mnEVsANkc/vj5LMDOzBHPAbzP7IOnmiG0B1wDM7Kt3zr2XlIwOH0g6Pb4t6Q3wVtIJoATspmn6w3u/AGRJknzbs4mNRmMuy7I+cMbMWt1u9zlAFEUrwKqkqpldBlaBL71e78aeFjqdzjZwG/g5yc1sQ1LZzD4DLSB3zj05tPFGo3F22irX6/X7URQpiqK1MfPTBJ1OZwfY2c+99x/TNH0UhuHLQ18/av4CbemsC6LSJJgAAAAASUVORK5CYII=',
		'mute'   :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAHGSURBVDiNlVHBahNRFD33vsnAuEiLKLRI4iLYES10ZReCOyEQZoYsOot+iOuW/oMLv0BwIcoMySe4cCO4aBhCocFQi6BNBCfNvHm3i05gkqbU3NV9h3sO55wHrDDtdrsRhuFaGeNlh77v15fhWutnk8nkpe/792aYtYT8VESeABiUBbXW58x8aox5DGAHwJcbDoIgcAvy3IjIjm3bu8Ph8FgplQJ40Gw2NwHACoLABQBmdrTWtTIxDMMNAL/SNP2b5/l6rVZ7lGXZCRE9t237PoAzyxizBQDGmBuZ0zR9wcynzHxsjNnN8/yhbdsnWZZBRKq3lrhgfyPLsotirw4Gg3HheO2/BO6aOwWI6GelUlkv9nG9Xq8CgDFmBAAWMycAoLV2mHmuRMdxvuK6xFfMnDDz+XQ6JaVUopT6AwBUJrRarS1mdgEgjuNohnue58dxHAVBcCgi36Io+uR53h4zb89F6HQ6CYDeYgzLsr4DgIgcAPjoed57IvogIgdq8ThJkt+NRuNfv98fz7Ber3cBAK7rXgJ4TUTbhfs3S0vsdrs/bil0VHqKiIxW+kYReVusn6/16N1KAkR0RET7URS1iWifiI6uAGvMtPCy9R3bAAAAAElFTkSuQmCC',
		'add'    :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7gAABu4BYYv7vAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACwSURBVDiNxVK7DYMwFLxDKV6DRBnadGzhSbIHE7AHk3iLdG6htETj7qWIiRBx7JCG63y+nywDZ4PbgzFGmqa55gze+9laGz4CjDFS17UD0BZKp2VZbmtItbKxuWQGgHa7ssopVXVQ1SGnyQaQHEmOhwNiayciTkQcgO7bkuyCX3BJkSR7AH0IoYvUg2RKmg5Yoar3GHhswW5JFu838N7PAKaSAcAUta+S7c0/X/l8PAGCNT1uYDkzegAAAABJRU5ErkJggg==',
		'remove' :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7gAABu4BYYv7vAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACISURBVDiNY2AYaMCIzHFwcOAQEBCQwKfhw4cPLw4cOPADwwAHBwcOXl7eewwMDJIELH3++fNnJZghTDBRqM2ENDMwMDBIIruSCZ9KYgBtDPj//387AwODJjKGilHfBSzYBBkZGSsZGBgq0cSwGjCIAvHDhw8vGBgYnhOh5zlULQMDAxWS8sADAHCIK7yIbl52AAAAAElFTkSuQmCC',
	};

	var isTouchDevice = window.screenX == 0 && ('ontouchstart' in window || 'onmsgesturechange' in window);
	var enabled = false;
	var renderer = null;
	var addr = null;
	var state = {'playback':STOPPED,'mute':'true'};
	var editmode = 0
	var selindex = -1;
	var here = null;

	function start(address, uri, title) {
		if (! enabled) {
			enabled = true;
			build();
			setButtons();
			addr=address+'/bump/';
			getRenderers();
			status();
		}
		here = [title !== undefined ? title:document.title,0,uri !== undefined ? uri:location];
		selindex = -1;
		refresh('{"playlist":[]}');
	}

	function build() {
		$('body').append([
		'<div class="bumpcontainer"><table class="bumppanel"><tr>',
		'<td id="bumpsettings">',
			'<select id="bplaylist" onmousedown="bump.getPlaylist(1)" onChange="bump.edited(1)" onBlur="bump.edited(0)"/>',
			'<span id="bumpadd" title="Add to playlist"/>',
			'<span id="bumprm" title="Remove from playlist"/>',
			'<select id="brenderers" onChange="bump.setRenderer()"/>',
		'</td>',
		'<td><input id="bumpvol" type="range" max="100" style="width:70px" onchange="bump.setVol(this.value)" oninput="bump.setVol(this.value)"/></td>',
		'<td id="bumpmute"/>',
		'<td id="bumpctrl"/>',
		'<td id="bumppos" onclick="bump.settings()" title="show/hide playlist">00:00</td>',
		'<td id="bexit" style="cursor:pointer" onclick="bump.exit()">',
			'<div style="vertical-align:top;margin-right:-14px;color:#fff"><img src="', icons['close'], '" alt="x"/></div>',
		'</td>',
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

		$('.bumpcontainer').css({
			position:'fixed',
			zIndex:'2147483647',
			right:'20px',
			top:'55px',
		});

		$('* .bumppanel').css({
			verticalAlign:'middle',
//			maxWidth:'200px',
			font: 'normal sans-serif '+(isTouchDevice?'6px':'4px'),
			fontWeight: '500',
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
			boxShadow:' 14px 4px 2px rgba(136,136,136,0.5)',
			'-moz-box-shadow':'14px 4px 2px rgba(136,136,136,0.5)',
			'-webkit-box-shadow':'14px 4px 2px rgba(136,136,136,0.5)',
			appearance:'none',
			'-moz-appearance':'none',
			'-webkit-appearance':'none',
		});

		$('.bumpbtn').css({
			display: 'inline-block',
			width: isTouchDevice?'32px':'24px',
			height: isTouchDevice?'24px':'16px',
		});

		$('#bumppos').css({
			cursor:'pointer',
		});

		$('#brenderers').css({
			maxWidth:'120px',
		});

		$('#bplaylist').css({
			maxWidth:'360px',
		});
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
			$(select).append($('<option value="'+val+(sel ? '" selected="selected"':'"')+(marked ? ' style="font-style:italic;color:blue"':'')+'>'+name+'</option>'));
		}
		setButtons();
		if (!$(select+' option:selected')) {
			$(select+' option[0]').attr('selected', 'selected');
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

