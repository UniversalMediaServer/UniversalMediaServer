var viewType = 'grid';
var fontSize = 'small';

function changeMargins() {
    $('#Media').css({paddingRight: '0px',});//MODDED
	var total_w = $('#Media').width();
	var cells = $('#Media li');
	var aspect = 16 / 9;
	var images_w = 0;
	var row_h = 180;
	var row_start = 0;
	var spaces = 1;

	if (viewType === 'grid') {
		for (var i = 0; i < cells.length; i++) {
			images_w += (180 * aspect);
			var avail_w = total_w - ++spaces * 20;
			var wrap = images_w > avail_w;
			if (wrap || i === cells.length - 1) {
				if (wrap) {
					row_h = avail_w / images_w * 180;
				}
				var cell_w = row_h * aspect;

				// Normalize cell heights for current row
				for (var c = row_start; c <= i; c++) {
					var caption_w = cell_w - 43;
					$(cells[c]).find('.caption').css({
						width : caption_w + 'px',
						maxWidth : caption_w + 'px',
					});
					$(cells[c]).find('.thumb').css({
						width : 'auto',
						height : row_h + 'px',
						maxWidth : cell_w + 'px',
						maxHeight : row_h + 'px',
                        borderBottomRightRadius: '0px',//MODDED
                        borderBottomLeftRadius: '0px',//MODDED
                        borderTopRightRadius: '3px',//MODDED
					});
                    $(cells[c]).find('.thumb').removeClass('pull-left');//MODDED
                    $(cells[c]).find('.box-meta').css({
                        borderBottomLeftRadius: '3px',//MODDED
                        borderBottomRightRadius: '3px',//MODDED
                        borderTopRightRadius: '0px',//MODDED
                    });
					$(cells[c]).css({
						height : row_h + 'px',
						width : cell_w + 'px',
                        float: 'left',
					});
				}
				images_w = 0;
				row_start = i + 1;
				spaces = 1;
			}
		}
	} else if (viewType === 'dynamic') {
		$('ul#Media li a:first-child').css({width: 'inherit', height: 'inherit'});
		aspect = new Array(cells.length);

		for (var i = 0; i < cells.length; i++) {
			var thumb = $(cells[i]).find('.thumb')[0];
			aspect[i] = thumb.naturalWidth / thumb.naturalHeight;
			images_w += (180 * aspect[i]);
			var avail_w = total_w - ++spaces * 20;
			var wrap = images_w > avail_w;
			if (wrap || i === cells.length - 1) {
				if (wrap) {
					row_h = avail_w / images_w * 180;
				}
				// Normalize cell heights for current row
				for (var c = row_start; c <= i; c++) {
					var cell_w = row_h * aspect[c];
					var caption_w = cell_w - 48;
					$(cells[c]).find('.caption').css({
						width : caption_w + 'px',
						maxWidth : caption_w + 'px',
					});
					$(cells[c]).find('.thumb').css({
						width : 'auto',
						height : row_h + 'px',
						maxWidth : cell_w + 'px',
						maxHeight : row_h + 'px',
                        borderBottomRightRadius: '0px',//MODDED
                        borderBottomLeftRadius: '0px',//MODDED
                        borderTopRightRadius: '3px',//MODDED
					});
                    $(cells[c]).find('.thumb').removeClass('pull-left');//MODDED
					$(cells[c]).css({
                        height : row_h + 'px',
                        width : cell_w + 'px',
                        float: 'left',
					});
                    $(cells[c]).find('.box-meta').css({
                        position: 'initial',
                        borderBottomLeftRadius: '3px',//MODDED
                        borderBottomRightRadius: '3px',//MODDED
                        borderTopRightRadius: '0px',//MODDED
                    });
				}
				images_w = 0;
				row_start = i + 1;
				spaces = 1;
			}
		}
    } else if (viewType === 'list') {
        $('#Media').css({paddingRight: '40px',});//MODDED
        for (var i = 0; i < cells.length; i++) {
            images_w += (180 * aspect);
            var avail_w = total_w - ++spaces * 20;
            var wrap = images_w > avail_w;
            if (wrap || i === cells.length - 1) {
                if (wrap) {
                    row_h = avail_w / images_w * 180;
                }
                var cell_w = row_h * aspect;

                // Normalize cell heights for current row
                for (var c = row_start; c <= i; c++) {
                    var caption_w = cell_w - 43;
                    $(cells[c]).find('.caption').css({
                        width : '100%',
                        maxWidth : '100%',
                    });
                    $(cells[c]).find('.thumb').css({
                        width : 'auto',
                        height : row_h + 'px',
                        maxWidth : cell_w + 'px',
                        maxHeight : '40px',
                        borderTopRightRadius: '0px',//MODDED
                        borderBottomLeftRadius: '3px',//MODDED
                    });
                    $(cells[c]).find('.thumb').addClass('pull-left');//MODDED
                    $(cells[c]).css({
                        height : row_h + 'px',
                        float: 'left',
                        width: '100%',	//MODDED
                    });
                    $(cells[c]).find('.box-meta').css({
                        borderBottomLeftRadius: '0px',//MODDED
                        borderTopRightRadius: '3px',//MODDED
                    });
                }
                images_w = 0;
                row_start = i + 1;
                spaces = 1;
            }
        }
    }

}

function changeFontSize() {
    if (fontSize === 'small') {
        $('.caption').css({fontSize: '12px',});
        $('#settingsContainer ul li a').css({fontSize: '12px',});
    } else if (fontSize === 'medium') {
        $('.caption').css({fontSize: '16px',});
        $('#settingsContainer ul li a').css({fontSize: '16px',});
    } else if (fontSize === 'big') {
        $('.caption').css({fontSize: '18px',});
        $('#settingsContainer ul li a').css({fontSize: '18px',});
    } else if (fontSize === 'extrabig') {
        $('.caption').css({fontSize: '22px',});
        $('#settingsContainer ul li a').css({fontSize: '22px',});
    }
}

function scrollActions() {
	if ($(window).width() > 1080) {
		$("#Menu, #ContentPage #Menu #HomeButton, ul#Folders, ul#Media").stop();
		if ($(window).scrollTop() === 0) {
			$("#Menu").animate({height: 95}, 200);
			$("#ContentPage #Menu #HomeButton").animate({height: 93}, 200);
			$("ul#Folders").animate({top: 94}, 200);
			$("ul#Media").animate({paddingTop: 115}, 200);
		} else {
			$("#Menu").animate({height: 53}, 200);
			$("#ContentPage #Menu #HomeButton").animate({height: 51}, 200);
			$("ul#Folders").animate({top: 52}, 200);
			$("ul#Media").animate({paddingTop: 73}, 200);
		}
	} else {
		$("#Menu, #ContentPage #Menu #HomeButton, ul#Media").stop();
		$("#Menu").animate({height: 53}, 200);
		$("#ContentPage #Menu #HomeButton").animate({height: 51}, 200);
		$("ul#Media").animate({paddingTop: 20}, 200);
	}
}

function searchFun(url, txt) {
	var str = prompt(txt);
	if (str !== null) {
		window.location.assign(url+'?str='+str)
	}
	return false;
}

function umsAjax(u, reload) {
	$.ajax({url: u}).done(function() {
		if(reload) {
			window.location.reload();
		}
	});
}

var polling, refused;

function poll() {
	$('body').append('<div id="notices"><div/></div>');
	polling = setInterval(function(){
		$.ajax({ url: '/poll',
			success: function(json){
				refused = 0;
				if (json) {
					//console.log('json: '+json)
					var ops = JSON.parse(json), i;
					for (i = 0; i < ops.length; i++) {
						var args = ops[i];
						switch (args[0]) {
							case 'seturl':
								window.location.replace(args[1]);
								break;
							case 'control':
								if (typeof control === 'function') {
									control(args[1], args[2]);
								}
								break;
							case 'notify':
								notify(args[1], args[2]);
								break;
						}
					}
				}
			},
			error: function() {
				if (++refused > 10) {
					clearInterval(polling);
				}
			}
		});
	}, 1000);
}

function notify(icon, msg) {
	//console.log('notify: '+icon+': '+msg);
	var notice = $('<div class="notice"><span class="icon '+icon+'"></span><span class="msg">'+msg+'</msg></div>');
	notice.insertAfter($('#notices').children(':last'));
	setTimeout(function() {
		notice.fadeOut('slow', function(){notice.remove();});
	}, 5000);
}

function chooseView(view) {
	Cookies.remove('view');
	Cookies.set('view', view, { expires: 365, path: '/' });
	viewType = view;
	changeMargins();

	if (view === 'grid') {
		$('#ViewGrid').addClass('active');
		$('#ViewDynamic').removeClass('active');
	} else if (view === 'dynamic') {
		$('#ViewGrid').removeClass('active');
		$('#ViewDynamic').addClass('active');
	}
}

function chooseFontSize(font) {
    Cookies.remove('font');
    Cookies.set('font', font, { expires: 365, path: '/' });
    fontSize = font;
    changeFontSize();

    if (font === 'small') {
        $('.dropdown-menu li .small').addClass('active');
        $('.dropdown-menu li .medium').removeClass('active');
        $('.dropdown-menu li .big').removeClass('active');
        $('.dropdown-menu li .extrabig').removeClass('active');
    } else if (font === 'medium') {
        $('.dropdown-menu li .small').removeClass('active');
        $('.dropdown-menu li .medium').addClass('active');
        $('.dropdown-menu li .big').removeClass('active');
        $('.dropdown-menu li .extrabig').removeClass('active');
    } else if (font === 'big') {
        $('.dropdown-menu li .small').removeClass('active');
        $('.dropdown-menu li .medium').removeClass('active');
        $('.dropdown-menu li .big').addClass('active');
        $('.dropdown-menu li .extrabig').removeClass('active');
    } else if (font === 'extrabig') {
        $('.dropdown-menu li .small').removeClass('active');
        $('.dropdown-menu li .medium').removeClass('active');
        $('.dropdown-menu li .big').removeClass('active');
        $('.dropdown-menu li .extrabig').addClass('active');
    }

}

function setPadColor(cycle) {
	var pad = Cookies.get('pad') || 'PadBlack';
	if (cycle) {
		pad = pad === 'PadBlack' ? 'PadGrey' : pad === 'PadGrey' ? 'PadNone' : 'PadBlack';
	}
	//console.log('pad='+pad);
	$('#Media li, #ViewPadColor').removeClass('PadBlack PadGrey PadNone').addClass(pad);
	Cookies.set('pad', pad, { expires: 365, path: '/' });
}

function initSettings() {
	$(".HoverMenu").hover(
		function () {
			$('#SettingsMenu').slideDown('fast');
			$(".bumpcontainer").animate({ top: '92px' }, 'fast');
		}, 
		function () {
			$('#SettingsMenu').slideUp('fast');
			$(".bumpcontainer").animate({ top: '55px' }, 'fast');
		}
	);
}

$(document).ready(function() {
	initSettings();
	viewType = Cookies.get('view') || 'grid';
	chooseView(viewType);
    fontSize = Cookies.get('font') || 'small';
    chooseFontSize(fontSize);
	setPadColor();

	if ($('#Media').length) {
		$(window).bind('load resize', changeMargins);
	}

	if ($('#Folders').length) {
		$('#Folders li').bind('contextmenu', function(){
			return false;
		});
	}

	if ($('#Menu').length) {
		$(window).bind('load resize scroll', scrollActions);
	}

	poll();
});
