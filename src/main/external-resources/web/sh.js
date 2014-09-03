SyntaxHighlighter.brushes.ums = function() {
	this.regexList = [
		{ css: 'str',   regex: /\"[^"\r\n]*?\"/g },
		{ css: 'plain', regex: /with class .+\"/g },
		{ css: 'addr',  regex: /(?!\S+:\/\/)\d+\.\d+\.\d+\.\d+(:\d+)?/g },
		{ css: 'err',   regex: /error|ERROR|Error|[^.]*Exception/g },
		{ css: 'err',   regex: /\s\s\s\s(at\s[^\]]+\])/g },
//		{ css: 'tags',  regex: /(INFO|DEBUG|TRACE) [^\[]+/g },
		{ css: 'tags',  regex: /(INFO|DEBUG|TRACE) [^\[]+(\[(?!ffmpeg|mplayer|tsmuxer|mencoder|vlc)[^\]]*\])?/g },
		{ css: 'cmd',   regex: /(?:\[(ffmpeg|mplayer|tsmuxer|mencoder|vlc)[^\]]*\])(.+)/g },
	];
};
SyntaxHighlighter.brushes.ums.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.ums.aliases  = ['ums'];

SyntaxHighlighter.all()

