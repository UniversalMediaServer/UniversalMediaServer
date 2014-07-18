$(function(){
	$.ImageMargins = {
		changeMargins: function() {
			// Initialise variables
			var container              = null;
			var imageList              = null;
			var thumbnailContainerList = null;
			var spanList               = null;
			var imagesPerRow           = null;
			var totalSpaceMinusMargins = null;
			var correctWidth           = null;
			var correctHeight          = null;
			var totalWidth             = null;

			// Do the main margins
			container = document.getElementById("Media");
			if (container !== null) {
				imageList = container.getElementsByTagName("img");
				thumbnailContainerList = container.getElementsByTagName("li");
				spanList = container.getElementsByTagName("span");
				imagesPerRow = "";
				totalSpaceMinusMargins = "";
				correctWidth  = null;
				correctHeight = null;

				totalWidth = container.offsetWidth;
				totalWidth = totalWidth - 40;

				if (totalWidth > 5092) {
					imagesPerRow = 20;
				} else if (totalWidth > 4823) {
					imagesPerRow = 19;
				} else if (totalWidth > 4554) {
					imagesPerRow = 18;
				} else if (totalWidth > 4285) {
					imagesPerRow = 17;
				} else if (totalWidth > 4016) {
					imagesPerRow = 16;
				} else if (totalWidth > 3747) {
					imagesPerRow = 15;
				} else if (totalWidth > 3478) {
					imagesPerRow = 14;
				} else if (totalWidth > 3209) {
					imagesPerRow = 13;
				} else if (totalWidth > 2940) {
					imagesPerRow = 12;
				} else if (totalWidth > 2671) {
					imagesPerRow = 11;
				} else if (totalWidth > 2402) {
					imagesPerRow = 10;
				} else if (totalWidth > 2133) {
					imagesPerRow = 9;
				} else if (totalWidth > 1864) {
					imagesPerRow = 8;
				} else if (totalWidth > 1695) {
					imagesPerRow = 7;
				} else if (totalWidth > 1325) {
					imagesPerRow = 6;
				} else if (totalWidth > 1056) {
					imagesPerRow = 5;
				} else if (totalWidth > 787) {
					imagesPerRow = 4;
				} else if (totalWidth > 518) {
					imagesPerRow = 3;
				} else if (totalWidth > 320) {
					imagesPerRow = 2;
				} else {
					imagesPerRow = 1;
				}

				if (imageList.length >= imagesPerRow) {
					for (i = 0; i < imageList.length; i++) {
						if (correctWidth === null) {
							totalSpaceMinusMargins = totalWidth - (20 * (imagesPerRow - 1));
							correctWidth = (totalSpaceMinusMargins / imagesPerRow) - 0.1;
						}
						correctHeight = correctWidth / 1.78;

						spanList[i].style.width      = correctWidth  + "px";
						spanList[i].style.maxWidth   = correctWidth  + "px";
						imageList[i].style.maxWidth  = correctWidth  + "px";
						imageList[i].style.maxHeight = correctHeight + "px";
						imageList[i].style.width     = "auto";
						imageList[i].style.height    = "auto";

						if (!((i + 1) % imagesPerRow === 0)) {
							thumbnailContainerList[i].style.marginRight = "20px";
						} else {
							thumbnailContainerList[i].style.marginRight = "0";
						}
					}
				}
			}
		}
	};

	$(window).bind('load resize', $.ImageMargins.changeMargins);
});

$(document).ready(function() {
	document.oncontextmenu = function() {
		return false;
	};
});

function searchFun(url) {
	var str = prompt("Enter search string:");
	if (str !== null) {
		window.location.assign(url+'?str='+str)
	}
	return false;
}
