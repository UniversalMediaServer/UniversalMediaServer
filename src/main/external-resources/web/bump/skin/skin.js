// To modify the bump player's appearance edit the embedded html, css, and filenames
// below, leaving the enclosing jquery/javascript code and syntax intact.

// Images:
//    - Edit the filenames only.
//    - Files must be in the same folder as this script.
//    - To add other images append items here
//	        'foo'    :'foo32.png',
//      and refer to them later in css/html like so:
//	        background:'url(' + img['foo'] + ') no-repeat',
//    - To boost performance you can use base64 data instead of filenames, e.g.
//         'prev'   :'data:image/png;base64,iVBORw0KGgoAAAANSUhEU...',

var img = bump.setImages({
	'prev'   :'prev16.png',
	'rew'    :'rew16.png',
	'play'   :'play16.png',
	'pause'  :'pause16.png',
	'stop'   :'stop16.png',
	'fwd'    :'fwd16.png',
	'next'   :'next16.png',
	'vol'    :'vol16.png',
	'mute'   :'mute16.png',
	'add'    :'add16.png',
	'remove' :'remove16.png',
	'clear'  :'clear16.png',
	'close'  :'close16.png',
});

// Layout: rearrange/retag/extend the html below but preserve existing classes+ids.

$('body').append([
	'<div class="bumpcontainer"><table class="bumppanel"><tr>',
	'<td id="bumpsettings">',
		'<select id="bplaylist"/>',
		'<span id="bplaylistctrl"/>',
		'<select id="brenderers"/>',
	'</td>',
	'<td><input id="bumpvol"/></td>',
	'<td id="bumpmute"/>',
	'<td id="bumpctrl"/>',
	'<td id="bumppos" title="Show/hide playlist">0:00</td>',
	'<td><div id="bexit"><img id="bclose"/></div></td>',
	'</tr></table></div>'
].join(''));

// Remove/reorder any of these buttons as desired
// (button styles are defined in the 'bumpbtn' class).

bump.addButton('prev', '#bumpctrl');
bump.addButton('rew', '#bumpctrl');
bump.addButton('play', '#bumpctrl');
bump.addButton('stop', '#bumpctrl');
bump.addButton('fwd', '#bumpctrl');
bump.addButton('next', '#bumpctrl');
bump.addButton('mute', '#bumpmute');
bump.addButton('add', '#bplaylistctrl', 'Add to playlist');
bump.addButton('remove', '#bplaylistctrl', 'Remove from playlist');
bump.addButton('clear', '#bplaylistctrl', 'Clear playlist');

// css: go crazy. Modify/add/remove blocks as appropriate using jquery selector syntax 

$('.bumpcontainer').css({
	position:'fixed',    /* Do not scroll out of view on this or other websites */
	zIndex:'2147483647', /* MAX_INT, i.e. hopefully topmost when invoked as bookmarklet on other websites */
	right:'22px',
	top:'55px',
});

$('* .bumppanel').css({
	verticalAlign:'middle',
//	maxWidth:'200px',
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
	minWidth:'80px',
	textAlign:'right',
	padding:'0 3px 0 3px',
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

// styles for as yet uncreated elements go here
$('head').append(['<style>',
	'.bumpcontainer .bselected {font-style:italic;color:blue;}',
	'</style>',
].join(''));

