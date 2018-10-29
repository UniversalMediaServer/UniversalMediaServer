$(document).ready(function($) {
	//jQuery.noConflict();  //just uncomment when migrating to $ 3+
	// Cuando se presiona una tecla sobre disparo la funcion keyDown
	$('body').keydown(function(event) {
		keyDown(event);
	});
});
$(document).on('fullscreenchange webkitfullscreenchange mozfullscreenchange MSFullscreenChange', function() {
	if(IsFullScreenCurrently()) {
		console.log('Full Screen Mode Enabled');
	}
	else {
		console.log('Full Screen Mode Disabled');
	}
});
var keyboard = {};
/*SOUND FEEDBACK TEST
if(g.sound_redesign){
	var b={};
	a.aa({
		soundDescriptorToSoundIdMap:(
		b["focus-change"]="airstream_move",
		b["list-horizontal-nav"]="airstream_move",
		b["list-vertical-nav"]="airstream_move",
		b["list-end"]="airstream_thunk",
		b.enter="airstream_select",
		b.escape="airstream_move",
		b["pivot-focus-change"]="airstream_move",
		b)})}
else b={},
	a.aa({
		soundDescriptorToSoundIdMap:(
		b["focus-change"]="same-light",
		b["list-horizontal-nav"]="same-toggle",
		b["list-vertical-nav"]="same-toggle",
		b["list-end"]="same-heavy",
		b.enter="cross-enter",
		b.escape="cross-back",
		b["pivot-focus-change"]="same-heavy",
		b)})
		})();
*/

var level = {
	"HOME"	  	: 0,
	"BROWSE"	: 1,
	"MEDIA"	 	: 2,
	"PLAY"	  	: 3,
	"VIDEO"	 	: 4,
	"KEYBOARD"	: 5/*,
	"SHOW"	  	: 6,
	"TRAILER"   : 7,
	"MUSICA"	: 8,
	"VARIADO"   : 9,
	"SERIE"	 	: 10,
	"FOOTER"	: 12,
	"HEADER"	: 13,
	"MENU"	  	: 14,
	"DOCUMENTAL"  : 15*/
};
var curLevel = level.HOME;
var status = 0;
var fullScreen = false;
$('#search-box').focus();
// Busco la funcion adecuada dependiendo de la tecla presionada
function keyDown(event) {
	switch (event.keyCode) {
		case VK_LEFT: {
			event.preventDefault(); //prevent default if it is body
			if (curLevel == level.HOME) {
				moveInHome(event.keyCode);
				break;
			}
			if (curLevel == level.HEADER) {
				moveInHeader(event.keyCode);
				break;
			}
			if (curLevel == level.MENU) {
				moveInMenu(event.keyCode);
				break;
			}
			if (curLevel == level.MEDIA) {
				moveInMedia(event.keyCode);
				break;
			}
			if (curLevel == level.PLAY) {
				moveInPlay(event.keyCode);
				break;
			}
			if (curLevel == level.FOOTER) {
				moveInFooter(event.keyCode);
				break;
			}
			if (curLevel == level.KEYBOARD) {
				moveInKeyboard(event.keyCode);
				break;
			}
			break;
		}
		case VK_RIGHT: {
			event.preventDefault(); //prevent default if it is body
			if (curLevel == level.HOME) {
				moveInHome(event.keyCode);
				break;
			}
			if (curLevel == level.HEADER) {
				moveInHeader(event.keyCode);
				break;
			}
			if (curLevel == level.BROWSE) {
				moveInBrowse(event.keyCode);
				break;
			}
			if (curLevel == level.MENU) {
				moveInMenu(event.keyCode);
				break;
			}
			if (curLevel == level.MEDIA) {
				moveInMedia(event.keyCode);
				break;
			}
			if (curLevel == level.PLAY) {
				moveInPlay(event.keyCode);
				break;
			}
			if (curLevel == level.FOOTER) {
				moveInFooter(event.keyCode);
				break;
			}
			if (curLevel == level.KEYBOARD) {
				moveInKeyboard(event.keyCode);
				break;
			}
			break;
		}
		case VK_DOWN: {
			event.preventDefault(); //prevent default if it is body
			if (curLevel == level.HOME) {
				moveInHome(event.keyCode);
				break;
			}
			if (curLevel == level.HEADER) {
				moveInHeader(event.keyCode);
				break;
			}
			if (curLevel == level.BROWSE) {
				moveInBrowse(event.keyCode);
				break;
			}
			if (curLevel == level.MEDIA) {
				moveInMedia(event.keyCode);
				break;
			}
			if (curLevel == level.PLAY) {
				moveInPlay(event.keyCode);
				break;
			}
			if (curLevel == level.VIDEO) {
				moveInVideo(event.keyCode);
				break;
			}
			if (curLevel == level.KEYBOARD) {
				moveInKeyboard(event.keyCode);
				break;
			}
			break;
		}
		case VK_UP: {
			event.preventDefault(); //prevent default if it is body
			if (curLevel == level.HOME) {
				moveInHome(event.keyCode);
				break;
			}
			if (curLevel == level.HEADER) {
				moveInHeader(event.keyCode);
				break;
			}
			if (curLevel == level.BROWSE) {
				moveInBrowse(event.keyCode);
				break;
			}
			if (curLevel == level.MEDIA) {
				moveInMedia(event.keyCode);
				break;
			}
			if (curLevel == level.PLAY) {
				moveInPlay(event.keyCode);
				break;
			}
			if (curLevel == level.FOOTER) {
				moveInFooter(event.keyCode);
				break;
			}
			if (curLevel == level.KEYBOARD) {
				moveInKeyboard(event.keyCode);
				break;
			}
			break;
		}
		case VK_ENTER: {
			event.preventDefault(); //prevent default if it is body
			if (curLevel == level.PLAY) {
				moveInPlay(event.keyCode);
				break;
			}
			else
			{
				emuleClick();
			}
			break;
		}
		case VK_RETURN: {
			window.location.reload();
			break;
		}
		case VK_DASH: {
			if (curLevel == level.PLAY) {
				moveInPlay(event.keyCode);
				break;
			}
			else
			{
				window.location = "http://www.youtube.com/tv/";
				break;
			}
			break;
		}
		case VK_PLAY: {
			if (curLevel == level.PLAY) {
				moveInPlay(event.keyCode);
				break;
			}
			break;
		}
		case VK_STOP: {
			if (curLevel == level.PLAY) {
				moveInPlay(event.keyCode);
				break;
			}
			break;
		}
		// add
		/*case VK_MTS:
			if (window.NetCastBack) {
				window.NetCastBack();
			}
			break;
		}*/
		//MOSTRAR CONTROLES EN PANTALLA
		case VK_1/*VK_INFO*/: {
			showHideControls();
			break;
		}
	}
	//moveToFocus();
}
// Funcion para navegar el Header
// key : codigo ASCII de boton presionado
function moveInHome(key) {
	var onFocus = $('.onFocus');
	switch (key) {
		case VK_DOWN: {
			if($("#navbar").find("li:first").hasClass("open")==true)
			{				
				if ($("#navbar").find("li:first").find("li:last").hasClass("onFocus")==true)
				{
					break;
				}
				if ($("#navbar").find("li:first").find("li").hasClass("onFocus")==true) {
					onFocus.removeClass('onFocus');
					changeFocus(onFocus.next());
				}
				else {
					onFocus.removeClass('onFocus');
					$("#navbar").find("li:first").find("li:first").addClass('onFocus');
				}
				break;
			}
			else if($('#Folders').text()!=""){
				onFocus.removeClass('onFocus');
				$('#Folders').find('li:first').addClass('onFocus');
				curLevel = level.BROWSE;
			}
			else if($('#VideoContainer').text()!=""){
				onFocus.removeClass('onFocus');
				if($('#toolbar').find('button:first').attr('disabled')!='disabled')
				{
                    $('#toolbar').find('button:first').addClass('onFocus');
				}
				else if($('#toolbar').find('button:nth-child(2)').attr('disabled')!='disabled')
				{
                    $('#toolbar').find('button:nth-child(2)').addClass('onFocus');
				}
				else{
                    $('#toolbar').find('button:nth-child(3)').addClass('onFocus');
				}
				curLevel = level.PLAY;
			}
			break;
		}
		case VK_UP: {
			if($("#navbar").find("li:first").hasClass("open")==true)
			{
				if ($("#navbar").find("li:first").find("li:first").hasClass("onFocus")==true)
				{
					onFocus.removeClass('onFocus');
					$("#navbar").find("li:first a.dropdown-toggle").addClass('onFocus');
					$("#navbar").find("li:first").removeClass("open");
				}
				else if ($("#navbar").find("li:first").find("li").hasClass("onFocus")==true) {
					changeFocus(onFocus.prev());
				}
			}
			break;
		}
		case VK_RIGHT: {
			if($('#HomeButton').hasClass("onFocus")==true)
			{
				onFocus.removeClass('onFocus');
				$("#trigger-overlay").addClass('onFocus');
			}
			else
			{
				onFocus.removeClass('onFocus');
				$("#navbar").find(".dropdown-toggle").addClass('onFocus');
			}			
			break;
		}
		case VK_LEFT: {
			if($("#navbar").find(".dropdown-toggle").hasClass("onFocus")==true)
			{
				onFocus.removeClass('onFocus');
				$("#trigger-overlay").addClass('onFocus');
			}
			else
			{				
				onFocus.removeClass('onFocus');
				$('#HomeButton').addClass('onFocus');
			}			
			break;
		}
	}
}
// Funcion para navegar la seccion de BROWSE
// key : codigo ASCII de boton presionado
function moveInBrowse(key) {
	var onFocus = $('.onFocus');
	switch (key) {
		case VK_UP: {
			if ($('.onFocus').prev().length>0) {
				changeFocus(onFocus.prev());
				folderScroll();
			}
			else
			{
				$('.onFocus').removeClass('onFocus');
				$('#HomeButton').addClass('onFocus');
				curLevel = level.HOME;
			}
			break;
		}
		case VK_DOWN: {
			if ($('.onFocus').next().length>0) {
				changeFocus(onFocus.next());
				folderScroll();
			}
			break;
		}
		case VK_RIGHT: {
			onFocus.removeClass('onFocus');
			$('#Media').find('li:first').addClass('onFocus');
			curLevel = level.MEDIA;
			moveToFocus();
			break;
		}
	}
}

// Funcion para navegar el Category
// key : codigo ASCII de boton presionado
function moveInMedia(key) {
	var onFocus = $('.onFocus');
	//var step = jumpingStep();
	switch (key) {
		case VK_LEFT: {

			if ($('.onFocus').prev().length>0) {
				changeFocus(onFocus.prev());
				moveToFocus();
			}
			else
			{
				$('.onFocus').removeClass('onFocus');
				$('#Folders').find('li:first').addClass('onFocus');
				curLevel = level.BROWSE;
			}
			break;
		}
		case VK_RIGHT: {

			if ($('.onFocus').next().length>0) {
				changeFocus(onFocus.next());
				moveToFocus();
			}
			break;
		}
		case VK_UP: {
			if ($('.onFocus').prev().length>0) {
				changeFocus(onFocus.prev());
				moveToFocus();
			}
			else
			{
				$('.onFocus').removeClass('onFocus');
				$('#HomeButton').addClass('onFocus');
				curLevel = level.HOME;
			}
			break;
		}
		case VK_DOWN: {
			if ($('.onFocus').next().length>0) {
				changeFocus(onFocus.next());
				moveToFocus();
			}
			break;
		}
	}
}
// Funcion para navegar el Menu
// key : codigo ASCII de boton presionado
function moveInPlay(key) {
	var onFocus = $('.onFocus');
	switch (key) {
		case VK_LEFT: {
			if (onFocus.prev().hasClass('btn') && onFocus.prev().attr('disabled')!='disabled') {
				changeFocus(onFocus.prev());
			}
			else if (onFocus.prev().prev().hasClass('btn') && onFocus.prev().prev().attr('disabled')!='disabled') {
                changeFocus(onFocus.prev().prev());
            }
			break;
		}
		case VK_RIGHT: {
			if (onFocus.next().hasClass('btn') && onFocus.next().attr('disabled')!='disabled') {
				changeFocus(onFocus.next());
			}
            else if (onFocus.next().next().hasClass('btn') && onFocus.next().next().attr('disabled')!='disabled') {
                changeFocus(onFocus.next().next());
            }
			break;
		}
		/*case VK_UP: {
			onFocus.removeClass('onFocus');
			curLevel = level.MEDIA;
			$('#tv-Category').find('.tv-btn:last').addClass('onFocus');
			break;
		}
		case VK_DOWN: {
			onFocus.removeClass('onFocus');
			curLevel = level.VIDEO;
			$('#tv-Video').find('.tv-btn:first').addClass('onFocus');
			break;
		}*/
		case VK_STOP: {
            history.back();
			break;
		}
		case VK_STOP_2: {
            history.back();
            break;
        }
		case VK_PLAY_PAUSE: {
			var myPlayer = videojs("player");
			if (myPlayer.paused()) {
				myPlayer.play();
			  }
			  else {
				myPlayer.pause();
			  }
			break;
		}
		case VK_PLAY_PAUSE_2:{
            var myPlayer = videojs("player");
            if (myPlayer.paused()) {
                myPlayer.play();
            }
            else {
                myPlayer.pause();
            }
            break;
		}
		case VK_PLAY: {
            var myPlayer = videojs("player");
            myPlayer.play();
            break;
        }
		case VK_PAUSE: {
			var myPlayer = videojs("player");
			myPlayer.pause();
			break;
		}
		case VK_TRACK_PREVIOUS:{
            window.location.replace('/play/{{prevId}}?html5=1');
            break;
		}
		case VK_TRACK_NEXT:{
            window.location.replace('/play/{{nextId}}?html5=1');
            break;
		}
		case VK_PLAY_PAUSE:{
            var myPlayer = videojs("player");
            if (myPlayer.paused()) {
                myPlayer.play();
            }
            else {
                myPlayer.pause();
            }
            break;
		}
		case VK_RED:{
            window.location.replace('/play/{{id1}}?flash=1');
		}
		case VK_ASPECT: {
            var myPlayer = videojs("player");
            if(IsFullScreenCurrently())
				GoOutFullscreen();
			else
				GoInFullscreen(myPlayer);
            break;
		}
		case VK_DASH:{
			var myPlayer = videojs("player");
            if(IsFullScreenCurrently())
				GoOutFullscreen();
			else
				GoInFullscreen(myPlayer);
			break;
		}
		case VK_ENTER: {
            if(IsFullScreenCurrently())
				GoOutFullscreen();
			else
				emuleClick();
			break;
		}
	}
}

// Funcion para navegar el Menu
// key : codigo ASCII de boton presionado
function moveInVideo(key) {
	var onFocus = $('.onFocus');
	switch (key) {
		case VK_LEFT: {
			if (onFocus.prev().hasClass('tv-btn')) {
				changeFocus(onFocus.prev());
			}
			break;
		}
		case VK_RIGHT: {
			if (onFocus.next().hasClass('tv-btn')) {
				changeFocus(onFocus.next());
			}
			break;
		}
		case VK_UP: {
			onFocus.removeClass('onFocus');
			curLevel = level.PLAY;
			$('#tv-pelicula').find('.tv-btn:last').addClass('onFocus');
			break;
		}
		case VK_DOWN: {
			onFocus.removeClass('onFocus');
			curLevel = level.FOOTER;
			$('#tv-Footer').find('.tv-btn:first').addClass('onFocus');
			break;
		}
	}
}

// Funcion para navegar el Footer
// key : codigo ASCII de boton presionado
function moveInFooter(key) {
	var onFocus = $('.onFocus');
	switch (key) {
		case VK_LEFT: {
			if (onFocus.prev().hasClass('tv-btn')) {
				changeFocus(onFocus.prev());
			} else {
				onFocus.removeClass('onFocus');
				$('#tv-serie').addClass('growCard');
				$('#tv-serie').find('.tv-btn:first').addClass('onFocus');
				curLevel = level.SERIE;
			}
			break;
		}
		case VK_RIGHT: {
			if (onFocus.next().hasClass('tv-btn')) {
				changeFocus(onFocus.next());
			}
			break;
		}
		case VK_UP: {
			$('.onFocus').removeClass('onFocus');
			$('#tv-video').addClass('growCard');
			$('#tv-video').find('.tv-btn:first').addClass('onFocus');
			curLevel = level.VIDEO;
			break;
		}
	}
}

// Funcion para navegar el KEYBOARD
// key : codigo ASCII de boton presionado
function moveInKeyboard(key) {
	var onFocus = $('.onFocus');
	switch (key) {
		case VK_LEFT: {
			
			if (onFocus.prev().hasClass('jQKeyboardBtn')) {
				changeFocus(onFocus.prev());
			}
			else if(onFocus.parent().hasClass('col-xs-4'))
			{
				changeFocus(onFocus.parent().prev().find('.jQKeyboardBtn:first'));
			}
			break;
		}
		case VK_RIGHT: {
			if (onFocus.next().hasClass('jQKeyboardBtn')) {
				changeFocus(onFocus.next());
			}
			else if(onFocus.parent().hasClass('col-xs-4'))
			{
				changeFocus(onFocus.parent().next().find('.jQKeyboardBtn:first'));
			}
			break;
		}
		case VK_UP: {
			if (onFocus.parent().prev().hasClass('jQKeyboardRow')) {
				var index = onFocus.index()+1;
				onFocus.parent().prev().find('.jQKeyboardBtn:nth-child('+index+')');
				if(onFocus.parent().prev().find('.jQKeyboardBtn:nth-child('+index+')').hasClass('jQKeyboardBtn')){
					changeFocus(onFocus.parent().prev().find('.jQKeyboardBtn:nth-child('+index+')'));
				}
				else
				{
					changeFocus(onFocus.parent().next().find('.jQKeyboardBtn:first'));
				}
			}
			else if(onFocus.parent().hasClass('col-xs-4'))
			{
				changeFocus(onFocus.parent().parent().prev().find('.jQKeyboardBtn:first'));
			}
			break;
		}
		case VK_DOWN: {
			if(onFocus.hasClass('jQKeyboard')){
				onFocus.blur();
				changeFocus($('.jQKeyboardRow:first').find('button:first').addClass("onFocus"));
				break;
			}
			if (onFocus.parent().next().hasClass('jQKeyboardRow')) {				
				var index = onFocus.index()+1;
				onFocus.parent().next().find('.jQKeyboardBtn:nth-child('+index+')');
				if(onFocus.parent().next().find('.jQKeyboardBtn:nth-child('+index+')').hasClass('jQKeyboardBtn') && !onFocus.parent().next().hasClass('special-keys-container')){
					changeFocus(onFocus.parent().next().find('.jQKeyboardBtn:nth-child('+index+')'));
				}
				else if(onFocus.parent().next().find('.jQKeyboardBtn:last').hasClass('jQKeyboardBtn') && !onFocus.parent().next().hasClass('special-keys-container')){
					changeFocus(onFocus.parent().next().find('.jQKeyboardBtn:last'));
				}
				else if(onFocus.parent().next().hasClass('special-keys-container'))
				{
					changeFocus(onFocus.parent().next().find('.col-xs-4:first').find('.jQKeyboardBtn:first'));
				}
				else
				{
					changeFocus(onFocus.parent().next().find('.jQKeyboardBtn:first'));
				}
			}
			break;
		}
	}
}
/*case VK_UP: {
			if (onFocus.parent().prev().hasClass('jQKeyboardRow')) {
				var index = $('.onFocus').index()+1;
				if($('.onFocus').parent().prev().find('.jQKeyboardBtn:nth-child('+index+')').hasClass('jQKeyboardBtn')){
					changeFocus($('.onFocus').parent().prev().find('.jQKeyboardBtn:nth-child('+index+')'));
				}
				else if($('.onFocus').parent().prev().find('.jQKeyboardBtn:last').hasClass('jQKeyboardBtn')){
					changeFocus($('.onFocus').parent().prev().find('.jQKeyboardBtn:last'));
				}
				else
				{
					changeFocus(onFocus.parent().prev().find('.jQKeyboardBtn:first'));
				}
			}
			else if(onFocus.parent().hasClass('col-xs-4'))
			{
				changeFocus(onFocus.parent().parent().prev().find('.jQKeyboardBtn:first'));
			}
			break;
		}
		case VK_DOWN: {
			if (onFocus.parent().next().hasClass('jQKeyboardRow')) {
				var index = $('.onFocus').index()+1;
				$('.onFocus').parent().next().find('.jQKeyboardBtn:nth-child('+index+')');
				if($('.onFocus').parent().next().find('.jQKeyboardBtn:nth-child('+index+')').hasClass('jQKeyboardBtn')){
					changeFocus($('.onFocus').parent().next().find('.jQKeyboardBtn:nth-child('+index+')'));
				}
				else if($('.onFocus').parent().next().find('.jQKeyboardBtn:last').hasClass('jQKeyboardBtn')){
					changeFocus($('.onFocus').parent().next().find('.jQKeyboardBtn:last'));
				}
				else
				{
					changeFocus(onFocus.parent().next().find('.jQKeyboardBtn:first'));
				}
			}
			break;
		}*/
function changeFocus(newfocusOb) {
	var object = newfocusOb;
	$('.onFocus').removeClass('onFocus');
	object.addClass('onFocus');
}
// Funcion que emula cliquear sobre la div en foco.
function emuleClick() {
	var link_level1 = $('.onFocus').attr('href');
	var link_level2 = $('.onFocus').parent('a').attr('href');
	var link_level3 = $('.onFocus').find('a:first').attr('href');
	if (link_level1 != undefined) {
		if(link_level1!="#keyboard" && link_level1!="#") {
			window.location = link_level1;
		} 
		else if(link_level1=="#") {
			$('.onFocus').click();
		} else {launchVirtualKeyboard();}
	} else if (link_level2 != undefined) {
		if(link_level2!="#keyboard" && link_level2!="#") {
			window.location = link_level2;
		} else if(link_level2=="#") {
			$('.onFocus').click();
		} else {launchVirtualKeyboard();}
	} else if (link_level3 != undefined) {
		if(link_level3!="#keyboard" && link_level3!="#") {
			window.location = link_level3;
		} else if(link_level3=="#") {
			$('.onFocus').click();
		} else {launchVirtualKeyboard();}
	} else {
		$('.onFocus').click();
	}
}
//Funcion para poner el foco en el centro de la pantalla
function moveToFocus(){
	var el = $('.onFocus');
	var elOffset = el.offset().top;
	var elHeight = el.height();
	var windowHeight = $(window).height();
	var offset;

	if (elHeight < windowHeight) {
		offset = elOffset - ((windowHeight / 2) - (elHeight / 2));
	}
	else {
		offset = elOffset;
	}
	var speed = 300;
	$('html, body').animate({scrollTop:offset}, speed);
}
//TODO: Replace folderScroll() function, this one is buggy.
function folderScroll(){
	var el = $('.onFocus');
	var elOffset = el.offset().top;
	var elHeight = el.height();
	var windowHeight = $(window).height();
	var offset;

	if (elHeight < windowHeight) {
		offset = elOffset - ((windowHeight / 2) - (elHeight / 2));
	}
	else {
		offset = elOffset;
	}
	var speed = 300;
	$('#Folders').animate({scrollTop:offset}, speed);
}
//Funcion para mostrar teclado virtual en pantalla
function launchVirtualKeyboard() {
	
}

/*****************************
 * PLAYER FUNCTIONS
 *****************************/
//Launch fullscreen mode using fullscreen key in player
function GoInFullscreen(element) {
	if(element.requestFullscreen)
		element.requestFullscreen();
	else if(element.mozRequestFullScreen)
		element.mozRequestFullScreen();
	else if(element.webkitRequestFullscreen)
		element.webkitRequestFullscreen();
	else if(element.msRequestFullscreen)
		element.msRequestFullscreen();
}
function GoOutFullscreen() {
	if(document.exitFullscreen)
		document.exitFullscreen();
	else if(document.mozCancelFullScreen)
		document.mozCancelFullScreen();
	else if(document.webkitExitFullscreen)
		document.webkitExitFullscreen();
	else if(document.msExitFullscreen)
		document.msExitFullscreen();
}
// Returns the DOM Node of the element which is in full-screen
// Returns null if no element in full-screen
function CurrentFullScreenElement() {
	return (document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement || document.msFullscreenElement || null);
}
function IsFullScreenCurrently() {
	var full_screen_element = document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement || document.msFullscreenElement || null;
	
	// If no element is in full-screen
	if(full_screen_element === null)
		return false;
	else
		return true;
}

/*function enterFullscreen(){
    var myPlayer = videojs("player");
    if (document.fullscreenEnabled || /* Standard syntax */
        /*document.webkitFullscreenEnabled || /* Chrome, Safari and Opera syntax */
        /*document.mozFullScreenEnabled ||/* Firefox syntax */
        /*document.msFullscreenEnabled/* IE/Edge syntax *) {
        /*myPlayer.requestFullscreen();
		document.documentElement.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
    }
}
function isVideoInFullscreen() {
    if (window.fullScreen!=false || document.fullscreenElement!=null || document.webkitFullscreenElement!=null || document.msFullscreenElement!=null || document.mozFullScreenElement!=null)
    {return true;}
    return false;
}*/
function play(){
    var myPlayer = videojs("player");
    myPlayer.play();
}
function pause(){
    var myPlayer = videojs("player");
    myPlayer.pause();
}
function playpause(){
    var myPlayer = videojs("player");
    if (myPlayer.paused()) {
        myPlayer.play();
    }
    else {
        myPlayer.pause();
    }
}

/* END PLAYER FUNCTIONS*/

function jumpingStep()
{
	var canvasSize = $("#Media").width();
	var cardSize = $("#Media").find("li:first").width();
	var step = canvasSize/cardSize;
	step = Math.floor(step);
	return step;
}

function showHideControls()
{
	if(status==1)
	{
		$('#box1').addClass("hidden");
		$('#box2').addClass("hidden");
		status = 0;
	}
	else {
		$('#box1').removeClass("hidden");
		$('#box2').removeClass("hidden");
		status = 1;
	}
}