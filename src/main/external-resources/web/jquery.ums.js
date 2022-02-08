if(typeof jQuery==='undefined')throw new Error('requires jQuery');
if(typeof Cookies==='undefined')throw new Error('requires js.cookie');

var viewType = 'grid';
var fontSize = 'small';
var isSingleRow;

function changeMargins() {
	if (isSingleRow === undefined) {
		isSingleRow = $('#Media').hasClass('media-single-row');
	}
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
			var avail_w = (total_w - ++spaces * 20) + 20;
			var wrap = images_w > avail_w;
			if (wrap || i === cells.length - 1) {
				if (wrap) {
					row_h = avail_w / images_w * 180;
					if (isSingleRow === true) {
						$('#Media').height((row_h + 40) + 'px');
					}
				}
				var cell_w = row_h * aspect;

				// Normalize cell heights for current row
				for (var c = row_start; c <= i; c++) {
					$(cells[c]).find('.thumb').css({
						width: 'auto',
						height: row_h + 'px',
						maxWidth: cell_w + 'px',
						maxHeight: row_h + 'px',
						borderBottomRightRadius: '0px',
						borderBottomLeftRadius: '0px',
						borderTopRightRadius: '3px',
					});
					$(cells[c]).find('.thumb').removeClass('pull-left');
					$(cells[c]).find('.box-meta').css({
						borderBottomLeftRadius: '3px',
						borderBottomRightRadius: '3px',
						borderTopRightRadius: '0px',
					});
					$(cells[c]).css({
						height: row_h + 'px',
						width: cell_w + 'px',
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
			var avail_w = (total_w - ++spaces * 20) + 20;
			var wrap = images_w > avail_w;
			if (wrap || i === cells.length - 1) {
				if (wrap) {
					row_h = avail_w / images_w * 180;
				}
				// Normalize cell heights for current row
				for (var c = row_start; c <= i; c++) {
					var cell_w = row_h * aspect[c];
					$(cells[c]).find('.thumb').css({
						width: 'auto',
						height: row_h + 'px',
						maxWidth: cell_w + 'px',
						maxHeight: row_h + 'px',
						borderBottomRightRadius: '0px',
						borderBottomLeftRadius: '0px',
						borderTopRightRadius: '3px',
					});
					$(cells[c]).find('.thumb').removeClass('pull-left');
					$(cells[c]).css({
						height: row_h + 'px',
						width: cell_w + 'px',
						float: 'left',
					});
					$(cells[c]).find('.box-meta').css({
						position: 'initial',
						borderBottomLeftRadius: '3px',
						borderBottomRightRadius: '3px',
						borderTopRightRadius: '0px',
					});
				}
				images_w = 0;
				row_start = i + 1;
				spaces = 1;
			}
		}
	} else if (viewType === 'list') {
		$('#Media').css({paddingRight: '40px', });
		for (var i = 0; i < cells.length; i++) {
			images_w += (180 * aspect);
			var avail_w = (total_w - ++spaces * 20) + 20;
			var wrap = images_w > avail_w;
			if (wrap || i === cells.length - 1) {
				if (wrap) {
					row_h = avail_w / images_w * 180;
				}
				var cell_w = row_h * aspect;

				// Normalize cell heights for current row
				for (var c = row_start; c <= i; c++) {
					$(cells[c]).find('.caption').css({
						width: '100%',
						maxWidth: '100%',
					});
					$(cells[c]).find('.thumb').css({
						width: 'auto',
						height: row_h + 'px',
						maxWidth: cell_w + 'px',
						maxHeight: '40px',
						borderTopRightRadius: '0px',
						borderBottomLeftRadius: '3px',
					});
					$(cells[c]).find('.thumb').addClass('pull-left');
					$(cells[c]).css({
						height: row_h + 'px',
						float: 'left',
						width: '100%',
					});
					$(cells[c]).find('.box-meta').css({
						borderBottomLeftRadius: '0px',
						borderTopRightRadius: '3px',
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
		$('.caption').css({fontSize: '12px', });
		$('#settingsContainer ul li a').css({fontSize: '12px', });
	} else if (fontSize === 'medium') {
		$('.caption').css({fontSize: '16px', });
		$('#settingsContainer ul li a').css({fontSize: '16px', });
	} else if (fontSize === 'big') {
		$('.caption').css({fontSize: '18px', });
		$('#settingsContainer ul li a').css({fontSize: '18px', });
	} else if (fontSize === 'extrabig') {
		$('.caption').css({fontSize: '22px', });
		$('#settingsContainer ul li a').css({fontSize: '22px', });
	}
}

function scrollActions() {
	if ($(window).width() > 1080) {
		$("#Menu, #ContentPage #Menu #HomeButton, ul#Folders, ul#Media").stop();
		if ($(window).scrollTop() === 0) {
			$("#Menu").animate({height: 95}, 200);
			$("#ContentPage #Menu #HomeButton").animate({height: 93}, 200);
			$("ul#Folders").animate({top: 94}, 200);
//			$("ul#Media").animate({paddingTop: 115}, 200);
		} else {
			$("#Menu").animate({height: 53}, 200);
			$("#ContentPage #Menu #HomeButton").animate({height: 51}, 200);
			$("ul#Folders").animate({top: 52}, 200);
//			$("ul#Media").animate({paddingTop: 73}, 200);
		}
	} else {
		$("#Menu, #ContentPage #Menu #HomeButton, ul#Media").stop();
		$("#Menu").animate({height: 53}, 200);
		$("#ContentPage #Menu #HomeButton").animate({height: 51}, 200);
//		$("ul#Media").animate({paddingTop: 20}, 200);
	}
}

function searchFun(url, txt) {
	var str = prompt(txt);
	if (str !== null) {
		window.location.assign(url + '?str=' + str)
	}
	return false;
}

function umsAjax(u, reload) {
	$.ajax({url: u}).done(function () {
		if (reload) {
			window.location.reload();
		}
	});
}

var polling, refused, streamevent;

function serverDataHandler(data) {
	switch (data[0]) {
		case 'seturl':
			window.location.replace(data[1]);
			break;
		case 'control':
			if (typeof control === 'function') {
				control(data[1], data[2]);
			}
			break;
		case 'notify':
			notify(data[1], data[2]);
			break;
		case 'close':
			//warn(data[1], data[2], data[3]);
			streamevent.close();
			break;
	}
}

function poll() {
	$('body').append('<div id="notices"><div/>');
	polling = setInterval(function () {
		$.ajax({url: '/poll',
			success: function (json) {
				refused = 0;
				if (!$.isEmptyObject(json)) {
					for (i = 0; i < json.length; i++) {
						serverDataHandler(json[i]);
					}
				}
			},
			error: function () {
				if (++refused > 10) {
					clearInterval(polling);
				}
			}
		});
	}, 1000);
}

function stream() {
	$('body').append('<div id="notices"><div/>');
	streamevent = new EventSource("/event-stream");
	streamevent.addEventListener('message', function(event) {
		refused = 0;
		if (event.data) {
			var data = JSON.parse(event.data);
			serverDataHandler(data);
		}
	});
	streamevent.addEventListener('ping', function() {
		refused = 0;
	});
	streamevent.onerror = function() {
		if (++refused > 10) {
			console.error("UMS Server unreachable");
			streamevent.close();
		}
	};
	window.addEventListener("beforeunload", function (e) {
		streamevent.close();
	});
}

function notify(icon, msg) {
	//console.log('notify: '+icon+': '+msg);
	var notice = $('<div class="notice"><span class="icon ' + icon + '"></span><span class="msg">' + msg + '</span></div>');
	notice.insertAfter($('#notices').children(':last'));
	setTimeout(function () {
		notice.fadeOut('slow', function () {
			notice.remove();
		});
	}, 5000);
}

function warn(icon, msg, btn) {
	var notice = $('<div class="notice"><span class="icon ' + icon + '"></span><span class="msg">' + msg + '</span><button class="btn btn-sm" onclick="$(\'#notices\').html(\'\');">' + btn + '</button></div>');
	notice.insertAfter($('#notices').children(':last'));
}

function chooseView(view) {
	Cookies.remove('view', { sameSite: 'Strict' });
	Cookies.set('view', view, { sameSite: 'Strict', expires: 365, path: '/' });
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
    Cookies.remove('font', { sameSite: 'Strict' });
    Cookies.set('font', font, { sameSite: 'Strict', expires: 365, path: '/' });
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
	Cookies.set('pad', pad, { sameSite: 'Strict', expires: 365, path: '/' });
}

function initSettings() {
	$(".HoverMenu").hover(
		function () {
			$('#SettingsMenu').slideDown('fast');
			$(".bumpcontainer").animate({top: '92px'}, 'fast');
		},
		function () {
			$('#SettingsMenu').slideUp('fast');
			$(".bumpcontainer").animate({top: '55px'}, 'fast');
		}
	);
}

/**
 * @see https://stackoverflow.com/questions/3942878/how-to-decide-font-color-in-white-or-black-depending-on-background-color
 * @return {boolean} whether a dark color is needed to be visible on the incoming color
 */
function isDarkColorNeededOnThisColor(rgbColor) {
      /**
       * Given a color in RGB format, assign either a light
       * or dark color.
       *
       * @see https://stackoverflow.com/questions/3942878/how-to-decide-font-color-in-white-or-black-depending-on-background-color
       */
      const RGB_MAX = 255;
      const RGB_CUTOFF = 0.03928; // See http://entropymine.com/imageworsener/srgbformula/
      const RGB_SLOPE = 0.055;
      const RGB_DENOMINATOR = 1.055;
      const RGB_EXPONENT = 2.4;
      const LINEAR_RGB = 12.92; // R, G, B * LINEAR_RBG = RGB_CUTTOFF
      const PERCEIVED_WEIGHTING_RED_LIGHT = 0.2126;
      const PERCEIVED_WEIGHTING_GREEN_LIGHT = 0.7152;
      const PERCEIVED_WEIGHTING_BLUE_LIGHT = 0.0722; // Given equal quantities of RGB, humans perceive them in a weighted way
      const LIGHT_DARK_THRESHOLD = 0.179;

      for (let i = 0; i < rgbColor.length; ++i) {
        rgbColor[i] /= RGB_MAX;

        if (rgbColor[i] <= RGB_CUTOFF) {
          rgbColor[i] = rgbColor[i] / LINEAR_RGB ;
        } else {
          rgbColor[i] = Math.pow((rgbColor[i] + RGB_SLOPE) / RGB_DENOMINATOR, RGB_EXPONENT);
        }
      }

      let iterator = 0;

      backgroundLightness = PERCEIVED_WEIGHTING_RED_LIGHT * rgbColor[iterator];
      iterator++;
      backgroundLightness +=  PERCEIVED_WEIGHTING_GREEN_LIGHT * rgbColor[iterator];
      iterator++;
      backgroundLightness += PERCEIVED_WEIGHTING_BLUE_LIGHT * rgbColor[iterator];

      return (backgroundLightness > LIGHT_DARK_THRESHOLD);
}

/**
 * Sets the background color of the page to one based on the poster.
 *
 * @see https://stackoverflow.com/a/2541680
 */
function setBackgroundColorFromPoster() {
	var poster = document.getElementById('poster');
	if (!poster || !poster.src) {
		// console.log('poster was not ready to analyze')
		return;
	}
	var rgb = getAverageRGB(poster);
	document.body.style.backgroundColor = 'rgb(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ')';
	$('body').addClass(isDarkColorNeededOnThisColor([rgb.r, rgb.g, rgb.b]) ? 'dark' : 'light');

	function getAverageRGB(imgEl) {
		var blockSize = 5, // only visit every 5 pixels
			defaultRGB = {r: 0, g: 0, b: 0}, // for non-supporting envs
			canvas = document.createElement('canvas'),
			context = canvas.getContext && canvas.getContext('2d'),
			data, width, height,
			i = -4,
			length,
			rgb = {r: 0, g: 0, b: 0},
			count = 0;

		if (!context) {
			return defaultRGB;
		}

		height = canvas.height = imgEl.naturalHeight || imgEl.offsetHeight || imgEl.height;
		width = canvas.width = imgEl.naturalWidth || imgEl.offsetWidth || imgEl.width;

		context.drawImage(imgEl, 0, 0);

		try {
			data = context.getImageData(0, 0, width, height);
		} catch (e) {
			/* security error, img on diff domain */
			console.log('Could not get image data');
			return defaultRGB;
		}

		length = data.data.length;

		while ((i += blockSize * 4) < length) {
			++count;
			rgb.r += data.data[i];
			rgb.g += data.data[i + 1];
			rgb.b += data.data[i + 2];
		}

		// ~~ used to floor values
		rgb.r = ~~(rgb.r / count);
		rgb.g = ~~(rgb.g / count);
		rgb.b = ~~(rgb.b / count);

		return rgb;
	}
}

function populateMetadataDisplayFromGlobalVars() {
	var isDark = $('body').hasClass('dark');
	var badgeClass = isDark ? 'badge-light' : 'badge-dark';
	if (actors && actors[0]) {
		var actorLinks = [];
		for (var i = 0; i < actors.length; i++) {
			var actor = actors[i];
			actorLinks.push('<a href="/browse/' + actor.id + '" class="badge ' + badgeClass + '">' + actor.name + '</a>');
		}
		$('.actors').html('<strong>' + actorsTranslation + ':</strong> ' + actorLinks.join(''));
	}
	if (awards) {
		$('.awards').html('<strong>' + awardsTranslation + ':</strong> ' + awards);
	}
	if (country && country.id) {
		$('.country').html('<strong>' + countryTranslation + ':</strong> <a href="/browse/' + country.id + '" class="badge ' + badgeClass + '">' + country.name + '</a>');
	}
	if (director && director.id) {
		$('.director').html('<strong>' + directorTranslation + ':</strong> <a href="/browse/' + director.id + '" class="badge ' + badgeClass + '">' + director.name + '</a>');
	}
	if (imdbID) {
		$('h1').append(' <a href="https://www.imdb.com/title/' + imdbID + '/"><i class=\"fab fa-imdb\"></i></a>');
	}
	if (genres && genres[0]) {
		var genreLinks = [];
		for (var i = 0; i < genres.length; i++) {
			var genre = genres[i];
			genreLinks.push('<a href="/browse/' + genre.id + '" class="badge ' + badgeClass + '">' + genre.name + '</a>');
		}
		$('.genres').html('<strong>' + genresTranslation + ':</strong> ' + genreLinks.join(''));
	}
	if (plot) {
		$('.plot').html('<strong>' + plotTranslation + ':</strong> ' + plot);
	}
	if (poster) {
		var img = new Image();
		img.onload = function() { setBackgroundColorFromPoster(); }
		img.crossOrigin = '';
		img.id = 'poster';
		img.src = poster;
		$('.posterContainer').html(img);
	}
	if (rated && rated.id) {
		$('.rated').html('<strong>' + ratedTranslation + ':</strong> <a href="/browse/' + rated.id + '" class="badge ' + badgeClass + '">' + rated.name + '</a>');
	}
	if (ratings && ratings[0]) {
		$('.ratings').html('<strong>' + ratingsTranslation + ':</strong>');
		var ratingsList = "<ul>";
		for (var i = 0; i < ratings.length; i++) {
			ratingsList += '<li>' + ratings[i].source + ': ' + ratings[i].value + '</li>';
		}
		ratingsList += "</ul>";
		$('.ratings').append(ratingsList);
	}
	if (startYear) {
		$('.startYear').html('<strong>' + yearStartedTranslation + ':</strong> ' + startYear);
	}
	if (totalSeasons) {
		$('.totalSeasons').html('<strong>' + totalSeasonsTranslation + ':</strong> ' + totalSeasons);
	}

}

$(document).ready(function () {
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
		$('#Folders li').bind('contextmenu', function () {
			return false;
		});
	}

	if ($('#Menu').length) {
		$(window).bind('load resize scroll', scrollActions);
	}

	if (typeof (EventSource) !== "undefined") {
		stream();
	} else {
		poll();
	}
});
