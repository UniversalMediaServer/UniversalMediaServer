SyntaxHighlighter.defaults['gutter'] = false;
SyntaxHighlighter.defaults['toolbar'] = false;
SyntaxHighlighter.defaults['smart-tabs'] = false;
SyntaxHighlighter.defaults['quick-code'] = false;

var re_quoted = /\"[^"\r\n]*?\"/g;
var re_url = /\w+:\/\/[\w-.\/?%&=:@;#$+]+/g;
var re_addr = /\d+\.\d+\.\d+\.\d+(:\d+)?/g;
var re_err = /error|ERROR|Error|[^.]*Exception/g;
var re_traceback = /\s\s\s\sat\s[^\r\n]+/g;

var _log = [
	{ css: 'str',     regex: re_quoted },
	{ css: 'plain',   regex: re_url },
	{ css: 'addr',    regex: re_addr },
	{ css: 'err',     regex: re_err },
	{ css: 'errmsg',  regex: re_traceback },
];

var _conf = [
	{ css: 'comment', regex: /(^|[\r\n])[^\w]*#[^\r\n]*/g },
	{ css: 'section', regex: /(^|[\r\n])\s*\[[^\]#]*\]/g },
	{ css: 'plain',   regex: re_url },
	{ css: 'addr',    regex: re_addr },
	{ css: 'key',     regex: /(^|[\r\n])\s*[^#,=\r\n]+\s*=/g },
	{ css: 'keyword', regex: /True|False/gi },
];

// Generic log brush
SyntaxHighlighter.brushes.log = function() { this.regexList = _log; };
SyntaxHighlighter.brushes.log.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.log.aliases  = ['log'];

// logfile brush
SyntaxHighlighter.brushes.debug_log = function() {
	this.regexList = _log.concat([
		{ css: 'plain',   regex: /with class .+\"/g },
		{ css: 'tags',    regex: /(INFO|DEBUG|WARN|TRACE) [^\[]+(\[(?!ffmpeg|mplayer|tsmuxer|mencoder|vlc)[^\]]*\])?/g },
		{ css: 'cmd',     regex: /(?:\[(ffmpeg|mplayer|tsmuxer|mencoder|vlc)[^\]]*\])(.+)/g },
		{ css: 'setting', regex: /Reading (?!pipe:)[^:]+: [^\(]+/g },
	]);
};
SyntaxHighlighter.brushes.debug_log.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.debug_log.aliases  = ['debug_log'];

// .conf .ini .properties brush
SyntaxHighlighter.brushes.conf = function() { this.regexList = _conf; };
SyntaxHighlighter.brushes.conf.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.conf.aliases  = ['conf'];

SyntaxHighlighter.regexLib['url'] = re_url;


// Incremental as-you-scroll highlighting

var chunk_h = null;

function paint_visible_chunks() {
	var pos = $(window).scrollTop();
	var first = Math.floor((pos - 400) / chunk_h);
	var last = Math.floor((pos + $(window).height() + 400) / chunk_h);
	for (var i=first; i <= last; i++) {
		var chunk = $('pre#chunk_' + i);
		if (chunk.length) {
			//console.log('highlight '+chunk.attr('id'));
			SyntaxHighlighter.highlight(null, chunk[0]);
		}
	}
};

$(window).scroll(paint_visible_chunks);

function chop(rawtext, brush) {
	var lines = rawtext.split(/\r?\n/),
		len = lines.length;
	// Chop up the raw text into 100 line chunks
	for (var i=0, c=0; i < len; i+=100, c++) {
		var pre = $('<pre id="chunk_' + c + '" class="' + brush + '"/>');
		pre.append(lines.slice(i, i+100).join('\n'));
		$('body').append(pre);
	}
	return len;
}

function ready() {
	var raw = $('#rawtext');
	if (raw.length) {
		var len = chop(raw.text(), raw.attr('class'));
		chunk_h = raw.height() / len * 100;
		raw.remove();
		paint_visible_chunks();
	}
}

$(document).bind('ready', ready);

