function changeMargins() {
	var totalWidth = $('#Media').width() - 40,
		imagesPerRow = Math.ceil(totalWidth / 320),
		totalSpaceMinusMargins = totalWidth - (20 * (imagesPerRow - 1)),
		correctWidth = totalSpaceMinusMargins / imagesPerRow,
		correctHeight = correctWidth / 1.78,
		correctWidthSpan = correctWidth - 32;

	$('#Media .caption').css({
		width : correctWidthSpan + 'px',
		maxWidth : correctWidthSpan + 'px',
	});
	$('#Media .thumb').css({
		width : 'auto',
		height : 'auto',
		maxWidth : correctWidth + 'px',
		maxHeight : correctHeight + 'px',
	});
}

$(document).ready(function() {
	if ($('#Media').length) {
		$(window).bind('load resize', changeMargins);
	}
	if ($('#Folders').length) {
		$('#Folders li').bind('contextmenu', function(){
			return false;
		});
	}
});

function searchFun(url) {
	var str = prompt("Enter search string:");
	if (str !== null) {
		window.location.assign(url+'?str='+str)
	}
	return false;
}
