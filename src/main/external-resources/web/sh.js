SyntaxHighlighter.defaults['gutter'] = false;
SyntaxHighlighter.defaults['toolbar'] = false;
SyntaxHighlighter.defaults['smart-tabs'] = false;
SyntaxHighlighter.defaults['quick-code'] = false;

var re_quoted = /\"[^"\r\n]*?\"/g;
var re_url = /\w+:\/\/[\w-.\/?%&=:@;#$]+/g;
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

// debug.log brush
SyntaxHighlighter.brushes.debug_log = function() {
	this.regexList = _log.concat([
		{ css: 'plain',   regex: /with class .+\"/g },
		{ css: 'tags',    regex: /(INFO|DEBUG|TRACE) [^\[]+(\[(?!ffmpeg|mplayer|tsmuxer|mencoder|vlc)[^\]]*\])?/g },
		{ css: 'cmd',     regex: /(?:\[(ffmpeg|mplayer|tsmuxer|mencoder|vlc)[^\]]*\])(.+)/g },
	]);
};
SyntaxHighlighter.brushes.debug_log.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.debug_log.aliases  = ['debug_log'];

// .conf .ini .properties brush
SyntaxHighlighter.brushes.conf = function() { this.regexList = _conf; };
SyntaxHighlighter.brushes.conf.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.conf.aliases  = ['conf'];

SyntaxHighlighter.regexLib['url'] = re_url;

SyntaxHighlighter.all()
