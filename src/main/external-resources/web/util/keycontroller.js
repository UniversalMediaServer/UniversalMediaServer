jQuery(document).ready(function($) {
    //jQuery.noConflict();  //just uncomment when migrating to jQuery 3+
    // Cuando se presiona una tecla sobre disparo la funcion keyDown
    $('body').keydown(function(event) {
    	keyDown(event);
    });
});
var level = {
	"HOME"      : 0,
    "BROWSE"    : 1,
    "MEDIA"     : 2,
    "PLAY"      : 3,
    "VIDEO"     : 4/*,
    "DOCUMENTAL": 5,
    "SHOW"      : 6,
    "TRAILER"   : 7,
    "MUSICA"    : 8,
    "VARIADO"   : 9,
    "SERIE"     : 10,
    "FOOTER"    : 12,
	"HEADER"    : 13,
	"MENU"      : 14,
	"KEYBOARD"  : 15*/
};
var curLevel = level.HOME;
var status = 0;
var fullScreen = false;
jQuery('#search-box').focus();
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
            emuleClick();
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
        //LANZAR TECLADO VIRTUAL
        case VK_0: {
            if (curLevel != level.KEYBOARD) {
            	launchVirtualKeyboard();
            }
            break;
        }
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
	var onFocus = jQuery('.onFocus');
    switch (key) {
        case VK_DOWN: {
        	if(jQuery("#navbar").find("li:first").hasClass("open")==true)
        	{        		
        		if (jQuery("#navbar").find("li:first").find("li:last").hasClass("onFocus")==true)
        		{
        			break;
        		}
        		if (jQuery("#navbar").find("li:first").find("li").hasClass("onFocus")==true) {
        			onFocus.removeClass('onFocus');
                    changeFocus(onFocus.next());
                }
        		else {
        			onFocus.removeClass('onFocus');
        			jQuery("#navbar").find("li:first").find("li:first").addClass('onFocus');
        		}
        		break;
        	}
        	else if(jQuery('#Folders').text()!=""){
            	onFocus.removeClass('onFocus');
                jQuery('#Folders').find('li:first').addClass('onFocus');
                curLevel = level.BROWSE;
            }
            else if(jQuery('#VideoContainer').text()!=""){
            	onFocus.removeClass('onFocus');
                jQuery('#VideoContainer').find('button:first').addClass('onFocus');
                curLevel = level.PLAY;
            }
            break;
		}
        case VK_UP: {
        	if(jQuery("#navbar").find("li:first").hasClass("open")==true)
        	{
        		if (jQuery("#navbar").find("li:first").find("li:first").hasClass("onFocus")==true)
        		{
        			onFocus.removeClass('onFocus');
	        		jQuery("#navbar").find("li:first a.dropdown-toggle").addClass('onFocus');
	        		jQuery("#navbar").find("li:first").removeClass("open");
        		}
        		else if (jQuery("#navbar").find("li:first").find("li").hasClass("onFocus")==true) {
                    changeFocus(onFocus.prev());
                }
        	}
            break;
		}
        case VK_RIGHT: {
            onFocus.removeClass('onFocus');
            jQuery("#navbar").find("li:first a.dropdown-toggle").addClass('onFocus');
            break;
        }
        case VK_LEFT: {
        	onFocus.removeClass('onFocus');
            jQuery('#HomeButton').addClass('onFocus');
            break;
        }
    }
}
// Funcion para navegar la seccion de BROWSE
// key : codigo ASCII de boton presionado
function moveInBrowse(key) {
    var onFocus = jQuery('.onFocus');
    switch (key) {
        case VK_UP: {
            if (jQuery('.onFocus').prev().length>0) {
                changeFocus(onFocus.prev());
            }
            else
            {
                jQuery('.onFocus').removeClass('onFocus');
                jQuery('#HomeButton').addClass('onFocus');
                curLevel = level.HOME;
            }
            break;
        }
        case VK_DOWN: {
            if (jQuery('.onFocus').next().length>0) {
                changeFocus(onFocus.next());
            }
            break;
        }
        case VK_RIGHT: {
            onFocus.removeClass('onFocus');
            jQuery('#Media').find('li:first').addClass('onFocus');
            curLevel = level.MEDIA;
            moveToFocus();
            break;
        }
    }
}

// Funcion para navegar el Category
// key : codigo ASCII de boton presionado
function moveInMedia(key) {
	var onFocus = jQuery('.onFocus');
	//var step = jumpingStep();
	switch (key) {
		case VK_LEFT: {

            if (jQuery('.onFocus').prev().length>0) {
                changeFocus(onFocus.prev());
                moveToFocus();
            }
            else
            {
                jQuery('.onFocus').removeClass('onFocus');
                jQuery('#Folders').find('li:first').addClass('onFocus');
                curLevel = level.BROWSE;
            }
			break;
		}
		case VK_RIGHT: {

            if (jQuery('.onFocus').next().length>0) {
                changeFocus(onFocus.next());
                moveToFocus();
            }
            break;
		}
        case VK_UP: {
            if (jQuery('.onFocus').prev().length>0) {
                changeFocus(onFocus.prev());
                moveToFocus();
            }
            else
            {
                jQuery('.onFocus').removeClass('onFocus');
                jQuery('#HomeButton').addClass('onFocus');
                curLevel = level.HOME;
            }
            break;
        }
        case VK_DOWN: {
            if (jQuery('.onFocus').next().length>0) {
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
    var onFocus = jQuery('.onFocus');
    switch (key) {
        /*case VK_LEFT: {
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
            curLevel = level.MEDIA;
            jQuery('#tv-Category').find('.tv-btn:last').addClass('onFocus');
            break;
        }
        case VK_DOWN: {
            onFocus.removeClass('onFocus');
            curLevel = level.VIDEO;
            jQuery('#tv-Video').find('.tv-btn:first').addClass('onFocus');
            break;
        }*/
    	case VK_STOP: {
    		history.go(-1);
    		break;
    	}
        case VK_PLAY: {            
            var myPlayer = videojs("player");
            if (myPlayer.paused()) {
                myPlayer.play();
              }
              else {
                myPlayer.pause();
              }
            break;
        }
        case VK_PAUSE: {
        	var myPlayer = videojs("player");
        	myPlayer.pause();
            break;
        }
        case VK_ASPECT: {
        	 var myPlayer = videojs("player");
        	 if(fullScreen==false) {
        		 if (document.fullscreenEnabled || /* Standard syntax */
               	  document.webkitFullscreenEnabled || /* Chrome, Safari and Opera syntax */
               	  document.mozFullScreenEnabled ||/* Firefox syntax */
               	  document.msFullscreenEnabled/* IE/Edge syntax */) {
                    myPlayer.requestFullscreen();
                    fullScreen = true;
                  }
        		 break;
        	 }             
             else {
            	 myPlayer.exitFullscreen();
            	 fullScreen = false;
             }
             break;
        }
        case VK_DASH:{
        	var myPlayer = videojs("player");
       	 if(fullScreen==false) {
       		 if (document.fullscreenEnabled || /* Standard syntax */
              	  document.webkitFullscreenEnabled || /* Chrome, Safari and Opera syntax */
              	  document.mozFullScreenEnabled ||/* Firefox syntax */
              	  document.msFullscreenEnabled/* IE/Edge syntax */) {
                   myPlayer.requestFullscreen();
                   fullScreen = true;
                 }
       		 break;
       	 }             
            else {
           	 myPlayer.exitFullscreen();
           	 fullScreen = false;
            }
            break;
        }
        case VK_ENTER: {
        	var myPlayer = videojs("player");
       	 if(fullScreen==false) {
       		 if (document.fullscreenEnabled || /* Standard syntax */
              	  document.webkitFullscreenEnabled || /* Chrome, Safari and Opera syntax */
              	  document.mozFullScreenEnabled ||/* Firefox syntax */
              	  document.msFullscreenEnabled/* IE/Edge syntax */) {
                   myPlayer.requestFullscreen();
                   fullScreen = true;
                 }
       		 break;
       	 }             
            else {
           	 myPlayer.exitFullscreen();
           	 fullScreen = false;
            }
            break;
        }
    }
}

// Funcion para navegar el Menu
// key : codigo ASCII de boton presionado
function moveInVideo(key) {
    var onFocus = jQuery('.onFocus');
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
            jQuery('#tv-pelicula').find('.tv-btn:last').addClass('onFocus');
            break;
        }
        case VK_DOWN: {
            onFocus.removeClass('onFocus');
            curLevel = level.FOOTER;
            jQuery('#tv-Footer').find('.tv-btn:first').addClass('onFocus');
            break;
        }
    }
}

// Funcion para navegar el Footer
// key : codigo ASCII de boton presionado
function moveInFooter(key) {
	var onFocus = jQuery('.onFocus');
	switch (key) {
		case VK_LEFT: {
			if (onFocus.prev().hasClass('tv-btn')) {
				changeFocus(onFocus.prev());
            } else {
                onFocus.removeClass('onFocus');
                jQuery('#tv-serie').addClass('growCard');
                jQuery('#tv-serie').find('.tv-btn:first').addClass('onFocus');
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
			jQuery('.onFocus').removeClass('onFocus');
            jQuery('#tv-video').addClass('growCard');
            jQuery('#tv-video').find('.tv-btn:first').addClass('onFocus');
            curLevel = level.VIDEO;
            break;
		}
	}
}

// Funcion para navegar el KEYBOARD
// key : codigo ASCII de boton presionado
function moveInKeyboard(key) {
    var onFocus = jQuery('.onFocus');
    switch (key) {
        case VK_LEFT: {
            if (onFocus.prev().hasClass('key-btn')) {
                changeFocus(onFocus.prev());
            } else {
                onFocus.removeClass('onFocus');
                curLevel = level.BROWSE;
                jQuery('#tv-Promo').find('.tv-btn:first').addClass('onFocus');
            }
            break;
        }
        case VK_RIGHT: {
            if (onFocus.next().hasClass('key-btn')) {
                changeFocus(onFocus.next());
            } else {
                onFocus.removeClass('onFocus');
                curLevel = level.FOOTER;
                jQuery('#tv-Footer').find('.tv-btn:first').addClass('onFocus');
            }
            break;
        }
        case VK_UP: {
            if (onFocus.next().hasClass('key-btn')) {
                changeFocus(onFocus.next());
            } else {
                onFocus.removeClass('onFocus');
                curLevel = level.FOOTER;
                jQuery('#tv-Footer').find('.tv-btn:first').addClass('onFocus');
            }
            break;
        }
        case VK_DOWN: {
            if (onFocus.next().hasClass('key-btn')) {
                changeFocus(onFocus.next());
            } else {
                onFocus.removeClass('onFocus');
                curLevel = level.FOOTER;
                jQuery('#tv-Footer').find('.tv-btn:first').addClass('onFocus');
            }
            break;
        }
    }
}
function changeFocus(newfocusOb) {
	jQuery('.onFocus').removeClass('onFocus');
	newfocusOb.addClass('onFocus');
}
// Funcion que emula cliquear sobre la div en foco.
function emuleClick() {
    var link_level1 = jQuery('.onFocus').attr('href');
    var link_level2 = jQuery('.onFocus').parent('a').attr('href');
    var link_level3 = jQuery('.onFocus').find('a:first').attr('href');
    if (link_level1 != undefined) {
        if(link_level1!="#keyboard" && link_level1!="#") {
            window.location = link_level1;
        } 
        else if(link_level1=="#") {
        	jQuery('.onFocus').click();
        } else {launchVirtualKeyboard();}
	} else if (link_level2 != undefined) {
        if(link_level2!="#keyboard" && link_level2!="#") {
            window.location = link_level2;
        } else if(link_level2=="#") {
        	jQuery('.onFocus').click();
        } else {launchVirtualKeyboard();}
    } else if (link_level3 != undefined) {
        if(link_level3!="#keyboard" && link_level3!="#") {
            window.location = link_level3;
        } else if(link_level3=="#") {
        	jQuery('.onFocus').click();
        } else {launchVirtualKeyboard();}
    } else {
		jQuery('.onFocus').click();
	}
}
//Funcion para poner el foco en el centro de la pantalla
function moveToFocus(){
    var el = jQuery('.onFocus');
    var elOffset = el.offset().top;
    var elHeight = el.height();
    var windowHeight = jQuery(window).height();
    var offset;

    if (elHeight < windowHeight) {
        offset = elOffset - ((windowHeight / 2) - (elHeight / 2));
    }
    else {
        offset = elOffset;
    }
    var speed = 300;
    jQuery('html, body').animate({scrollTop:offset}, speed);

    //jQuery('html, body').stop().animate({scrollTop: jQuery('html').height()/2 - jQuery('.onFocus').height()/2}, 300);
}
//Funcion para mostrar teclado virtual en pantalla
function launchVirtualKeyboard() {
    jQuery('#myVirtualKeyboard').modal('toggle');
    jQuery('#search-box').focus();
}

function jumpingStep()
{
    var canvasSize = jQuery("#Media").width();
    var cardSize = jQuery("#Media").find("li:first").width();
    var step = canvasSize/cardSize;
    step = Math.floor(step);
    return step;
}

function showHideControls()
{
    if(status==1)
    {
        jQuery('#box1').addClass("hidden");
        jQuery('#box2').addClass("hidden");
        status = 0;
    }
    else {
        jQuery('#box1').removeClass("hidden");
        jQuery('#box2').removeClass("hidden");
        status = 1;
    }
}