var bump = (function() {
//	console.log('jquery ' + $.fn.jquery);
	var STOPPED = 0;
	var PLAYING = 1;
	var PAUSED = 2;
	var PLAYCONTROL = 1;
	var VOLUMECONTROL = 2;

	var icons = {
		'prev':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADvSURBVDiN3ZIxSsRAGIXfSyaQwgMEsQgIQmDxDNOtIUtOIWnEytor7PbB2gMMBLTzDDaCEAgshJR2BgbyWwVmYVZZu92vm/dmvn9gBjh6QneR5/lFlmXnaZp+t21rPfuptVZd101zoNw2iqInEVnGcZwDeJnzqqqivu/vST4CeAZw5xX4KMuyGIZhTfIKAEheuv1eQVEUWRiGGxFZ/jYg8IUiooMgeP/r8F7BIXgFJN+mabom+frvGzRN82GMuSG5Ivl5sGDGGNMkSbIQkQcAXyLSuv3OK1hrb5VSZ+M4bt28rmsLYA1go7Xe+XwnwA9UZUlvaHVhfwAAAABJRU5ErkJggg==',
		'rew':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAC3SURBVDiN7c8xSgNRFIXhzyHIYNyCWMfKNdg6j6AwnW4inWIr2IiLsHyFxZ1FiLtwD4pPwRkrZQhqirT5y8M9557DhrXZ+k2cz+d7fd/f4jgippBS2sElFjiKiEeYjI1t226XUhZ9319hindomuYEd9hffjYZmXdLKU+YjQ9SSvc4+2tCtXrl//wE5Jxf6ro+xAVev/WIOB+G4RTPKxvknD8i4qaqqhkyPqHrugcc4Bpv67besMQXxRg0zZhvOjUAAAAASUVORK5CYII=',
		'play':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACrSURBVDiNY2AYfsDBwYGFgYGBkVj1zOgCurq6k9XV1Zeoqan9dnBwOHP27Nl/+AxgQhdgZGRUZmBgEGBkZOx98eLFFT8/P2+SDEAG////V/v///8WPz+/Hd7e3pokG4BkkDsTE9MlX1/fSLIMwAeIMoCRkXHnv3//9DZv3rwcXY6FgMZbDAwMRZs2bdpKtAv+//9/l4GB4cP///+LJSQkdPBpxgpITUjDAAAARn40yZpHhVUAAAAASUVORK5CYII=',
		'pause':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAABsSURBVDiN7ZGxCcAgEEVfQg4cKaULOEhGcAJHyArpXcAy26QVREgRCxWr1L7m4P/jc/yDydILxpgd0ABKqRMgxngUO3jv73p/G4RqwAGklK6iuTIt0ASsv+6eAQ2jLwS+thGRByDnbCtv0vEC04QVoxO3hiAAAAAASUVORK5CYII=',
		'stop':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAABaSURBVDiNY2AYBYzoAr6+vhoMDAzlWOT+MzAwdG7evPkGsiALFkPrGBgYInFYyM7AwBCFLMCERRE2MZxy+BQTBQanAf/wqMeQwxYLTQwMDD8ZcEQj0U4bQQAA9u4MULcVw3YAAAAASUVORK5CYII=',
		'fwd':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACrSURBVDiN7ZAxCsJAEADnTpGI/sEvWPqIbKFwpa+wULAXKz9heSCEi61vsNJa/IKJaUxsLhACUUFLp9od2CkW/nyNKgcRGQEHYAOsnHOp9wmw11rPoii61gO6tneBJXAKw3DsXQsweZ6fRWRujOm8CpQMlFI7EdlWXA9YZ1l2NMb03wU+pilwKYpi4pybVlwCLIIgGFprb6Vs1w7v+CfGcZx69wBs0xP//IAnzvEyXeExrXgAAAAASUVORK5CYII=',
		'next':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADxSURBVDiN1ZLBSsNAEIbnXxsSkIDH3BeEhtI3EHKKgsk+Rk5CqT6Hh977FEtz6C3gG4hQkAg55iRiPARCMp4KWzSs9tb/NvP/fMwwQ3TyOjOLKIomVVXxb8E4js/DMJRSSq8sy899X5gh3/dXaZq+J0lyn2WZY3qe510R0c5xnLXZPwAAkER0AeCxrusXpdStbYXJmMHMl0S0UUpt+75fjuXEmGGAroUQz8wcHQWwyQoAsB2GYQ6g+BcAwCuARGt9k+f57k8TMPMbEX0w80MQBDOtdW6b8OAKTdMsiqK4I6Ifz9S27ZPrutOu675s0BPTN/XJTAaU6DjyAAAAAElFTkSuQmCC',
//		'mute':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAGKSURBVDiNvZI9ixNRFIafcyYTHE2CW9kpFlqo0cUi/ezkD2ghrr8mf8VC1DIIgYTcjEWqFKIguKAgNrYr2dF8kLnHwsmSHQZN5Vtd3nt4zjnvvfA/5Zw7ds4d7XpaLhoMBi3n3KMqgIj8BK6NRqNblYDpdNqMougIuFTuPJlM7orIDCAIgsPhcHjlAqDf7zeXy2XivY/Knc3MvPf3oij6papfgZqqXgeoOeeO/7ZzGIZvN5vNJzO7s1gsbodh+H21Wt1U1YPKDMpar9cPROQbQJ7nB/P5/EeRx34AEWkCGYCqNmezWVb4jb0AZnYGNAC892edTqdR+NlegHq9/sHMbgAEQXDaarWuFoBTANkWpmnaMLPEzC4DJEnycns3Ho+fdrvdV1UNattDHMdZmqYOOIdsJSIC0Ov1DHgfBMGzPM9fA+0LK8RxnImIK37cuXam+QIc5nn+EWgDn2uUFMdxZmZvqsZV1Sfe+3f8yc4DjytDFBGr8r33z3d5wIt/vkJJbeBEVR8CJ8D931yenWZB2xtTAAAAAElFTkSuQmCC',
//		'vol':'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAG7AAABuwBHnU4NQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAFZSURBVDiNxZO7TptBEIW/s+vKyhsgFC4JFUV4A9LyACgVleXBESUFBVShTIlkrd2livIApIiUhJIKam4CRbxBZCkSu0Ph35b1YytUyalGZ3bPXPYs/EuklDyl9N3dNeLClEPLKaVfMzTugLe9Xm97RGgy2+/3F0spJ8C8mY1zKSUH9iWduvs34HeMcbXVat2NO+h2uwullB/A/JTKD8CBpHtJn4AXOedNgEalPnNmSRvu/hHYyznvAMeStiStTd1BHe5+6O6fASStlVLOK/55AsBKs9m8quLXMcYbwIFld9dzBC4Gg8GrKr7MOS8xXP61JG/87bakfeBd1fZZCOFNxZ8BBDOTmamUssjwnccwM7Xb7a/ALvAnxngUQtgYicHEDjqdzm0pZb0uUqEBfHD3OXffYuiDL1AzEgz9EEL4aWYL9VxK6RZ4Cbw3s+7MuSe9XhN48hf+Px4BerST2FjjuqwAAAAASUVORK5CYII=',
	};

	var isTouchDevice = window.screenX == 0 && ('ontouchstart' in window || 'onmsgesturechange' in window);
	var enabled = false;
	var renderer = null;
	var addr = null;
	var state = {'playback':STOPPED};
	var edited = false;
	var uri = null;
	var title = null;

	function start(address, u, t) {
		if (! enabled) {
			enabled = true;
			build();
//			setButtons();
			addr='http://'+address+'/bump/';
			getRenderers();
			status();
		}
		uri = u !== undefined ? u : null;
		title = t !== undefined ? t : null;
	}

	function build() {
		$('body').append('<div class="bumpcontainer"><div class="bumppanel"><div class="bumpplayer">'
			+ '<div id="bumptop"/>'
			+ '<div id="bumpctrl" style="float:left"/>'
			+ '<div id="bumppos" style="float:left" onclick="bump.settings()">00:00:00</div>'
			+ '<div id="bexit" style="float:right;width:10px;cursor:pointer" onclick="bump.exit()"><div style="float:right;margin-top:-5px;margin-right:-5px;color:#fff"><b>x</b></div></div>'
			+ '</div>'
			+ '<div id="bumpsettings">'
			+ '<select id="bplaylist" onmousedown="bump.getPlaylist()" onChange="bump.edited()"/>'
			+ '<select id="brenderers" onChange="bump.setRenderer()"/>'
			+ '</div></div></div>');

		$('.bumpcontainer').css({
			position:'fixed',
			zIndex:'99999',
			right:'12px',
			top:'50px',
		});

		$('.bumppanel').css({
			font: 'normal sans-serif ' + (isTouchDevice?'12px':'10px'),
			fontWeight: '500',
			textDecoration:'none',
			textAlign:'center',
			color:'#fff',
			backgroundColor:'#729fcf',
			margin:'0 0 6px 0',
			padding:'4px 8px',
			border:'1px',
			borderColor:'#729fcf',
			borderRadius:'3px',
			'-moz-border-radius':'3px',
			'-webkit-border-radius':'3px',
			boxShadow:' 4px 4px 2px rgba(136,136,136,0.5)',
			'-moz-box-shadow':'4px 4px 2px rgba(136,136,136,0.5)',
			'-webkit-box-shadow':'4px 4px 2px rgba(136,136,136,0.5)',
		});

//		$('#bumpctrl').append('<div style="float:left;width:16px"/>');
		addButton('prev', '#bumpctrl');
		addButton('rew', '#bumpctrl');
		addButton('play', '#bumpctrl');
		addButton('stop', '#bumpctrl');
		addButton('fwd', '#bumpctrl');
		addButton('next', '#bumpctrl');
//		$('#bumpctrl').append('<div id="bexit" style="float:right;width:10px;cursor:pointer" onclick="bump.exit()"><div style="float:right;margin-top:-5px;margin-right:-5px;color:#fff"><b>x</b></div></div>');

		$('.bumpbtn').css({
			display: 'inline-block',
			verticalAlign: 'middle',
			width: isTouchDevice?'32px':'24px',
			height: isTouchDevice?'32px':'24px',
			cursor:'pointer',
			backgroundColor:'#lightgrey',
			margin:'2px',
			/* padding:'4px 8px',*/
			borderColor:'#729fcf',
			border:'2px',
			borderRadius:'2px',
			'-moz-border-radius':'2px',
			'-webkit-border-radius':'2px',
		});

		$('.bumpbtn:disabled').css({
			backgroundColor:'green',
		});

		$('#bumpsettings').css({
			display:'inline-block',
			outline:'none',
			border:'0',
			appearance:'none',
			'-moz-appearance':'none',
			'-webkit-appearance':'none',
		});
	}

	function settings() {
		$('#bumpsettings').toggle();
	}

	function setRenderer() {
		renderer = $("#brenderers option:selected").attr('value');
//		getPlaylist();
	}

	function getRenderers() {
		$.get(addr+'renderers', refresh);
	}

	function getPlaylist() {
		$.get(addr+'playlist/'+renderer, refresh);
	}

	function press(b) {
		var uri = $("#bplaylist option:selected").attr('value');
		$.get(addr+b+'/'+renderer+'?uri='+escape(uri)+'&title='+encodeURIComponent(document.title), refresh);
		edited = false;
	}

	function status() {
		if (enabled) {
			$.get(addr+'status/'+renderer, refresh);
		}
	}

	function refresh(data) {
		var vars = $.parseJSON(data);
		if ('state' in vars) {
			setState(vars['state']);
		}
		if ('renderers' in vars) {
			setSelect('#brenderers', vars['renderers']);
			setRenderer();
		}
		if (!edited && 'playlist' in vars) {
			var here = [title == null ? document.title:title,0,uri == null ? location:uri];
			edited = uri == null;
			vars['playlist'].splice(0, 0, here);
			setSelect('#bplaylist', vars['playlist']);
		}
	}

	function setState(newstate) {
		var last = state.playback;
		state = newstate;
		if (state.playback != last) {
			setButtons();
		}
		$('#bumppos').html(state.position.replace('00:','')+' / '+state.duration.replace('00:',''));
		status();
	}

	function setSelect(select, opts) {
		$(select).html('');
		for (var i=0; i<opts.length; i++) {
			var name = opts[i][0];
			var sel = opts[i][1] == 1 ? ' selected="selected" ' : '';
			var val = opts[i][2];
			$(select).append($('<option value="'+val+'" '+sel+'>'+name+'</option>'));
		}
		if (!$(select+' option:selected')) {
			$(select+' option[0]').attr('selected', 'selected');
		}
	}

	function setButtons() {
		$('#bplaybutton').html('<img src="'+icons[state.playback==PLAYING ? 'pause':'play']+'"/>');
		var stopped = state.playback == STOPPED;
		$('#bprevbutton').attr('disabled', stopped);
		$('#brewbutton').attr('disabled', stopped);
		$('#bstopbutton').attr('disabled', stopped);
		$('#bfwdbutton').attr('disabled', stopped);
		$('#bnextbutton').attr('disabled', stopped);
		$('#brenderers').attr('disabled', !stopped);
	}

	function addButton(name, parent) {
		var b = $('<button class="bumpbtn" id="b'+name+'button" onclick="bump.press(\''+name+'\')"/>');
		$(parent).append(b);
		var img = new Image();
		img.src = icons[name];
		b.append(img);
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
		settings: function () {
			settings();
		},
		setRenderer: function () {
			setRenderer();
		},
		getPlaylist: function () {
			getPlaylist();
		},
		enabled: function () {
			return enabled;
		},
		edited: function () {
			edited = true;
		},
		exit: function () {
			exit();
		},
	}
}());

