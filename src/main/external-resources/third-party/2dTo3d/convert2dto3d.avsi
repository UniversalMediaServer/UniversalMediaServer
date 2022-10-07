## Script for 2D to 3D On-the-Fly Real-time Video Conversion with Avisynth(+) and UniversalMediaServer 
## Integrations, updates (e.g. to support AviSynth+ 64 bit), corrections and additions for use in UniversalMediaServer by Dr. Thomas Richard Judge 2022.


## UniversalMediaServer generates multi-threading script wrapper code, plugin loading code and algorithm, output format, size and brightness options based on UI selections 
## before calling this function with the video2d clip and other parameters set.

function convert2dTo3d(clip video2d, int "algorithm", int "outputFormat", bool "resize", int "hzTargetSize", bool "incBrightness" ) {

# Defaults
	algorithm     = default(algorithm   	, 1 )
	outputFormat  = default(outputFormat    , 1 )
	resize        = default(resize      	, false )
	hzTargetSize  = default(hzTargetSize    , 1920    )
	incBrightness = default(incBrightness   , false   )

	## Convert to RGB32 to avoid the width restrictions
	video2d = ConvertToRGB32(video2d)

	## Get video width/height
	videoW = width(video2d)
	videoH = height(video2d)

	resize == true ? Eval("""
			video2d = Lanczos4Resize(video2d, hzTargetSize, hzTargetSize * videoH / videoW)
		""") : ""

	subtitle = "Invalid algorithm specified"

		algorithm == 2 ? Eval("""
			subtitle = "Motion and Lighting Depth Detection"
			video3d = JermanisFinal(video2d, outputFormat=outputFormat, resize=resize, hzTargetSize=hzTargetSize, incBrightness=incBrightness)
		""") : Eval("""
			video3d = BelevPulfrichBase(video2d, outputFormat=outputFormat, resize=resize, hzTargetSize=hzTargetSize, incBrightness=incBrightness)
			subtitle = "Pulfrich Base" # Default
		""") 
		
## 	video3d = Subtitle(video3d, subtitle, align=9)
	
	return video3d
}

function BelevPulfrichBase(clip video2d, int "outputFormat", bool "resize", int "hzTargetSize", bool "incBrightness" ) {
 
## https://3dvision-blog.com/1220-2d-to-3d-realtime-video-conversion-script-for-avisynth-v0-3/
## by Anton Belev at 3D Vision Blog 
## http://www.3dvision-blog.com
## Additionally extended by Martin Haverland at audiomh.de for quality resize
## and different output modes including interlaced format for use in non-3d specific players e.g. windows media player.
## Creation date 12/01/2010 - based on version 0.3b of original author's script (TJ)

## Get video width/height
videoW = width(video2d)
videoH = height(video2d)

## Set the frame stretch factor
## Lower the value 100 to increase frame stretch, may introduce ghosting

ResW = videoW + (videoW / 100)
CropW = (ResW - videoW) / 2

## Create variables for left and right frame with one frame difference
## This is the Pulfrich-like simulation that creates illusion of depth from movement
f1 = video2d
f2 = DeleteFrame(video2d, 0)


## Stretch the right frame to further the depth effect
f1 = LanczosResize(f1, ResW, videoH)
f1 = Crop(f1, 0, 0, videoW, videoH)

## Stretch the left frame to further the depth effect
f2 = LanczosResize(f2, ResW, videoH)
f2 = Crop(f2, CropW, 0, videoW, videoH) 

outputFormat == 2 ? Eval("""
		StackVertical(f2, f1)
	""") :  Eval("""
		StackHorizontal(f2, f1)
	""") 

## Output the two video frames in a page flipping format for shutter glasses etc.
## The Tweak as proposed by eslave is for brighter image, modify the value 30
# f1 = f1.ConvertToYV12.Tweak(Bright=30)
# f2 = f2.ConvertToYV12.Tweak(Bright=30)
# Interleave(f2,f1)


## Output the two video frames in anaglyph red-cyan as proposed by eslave
# MergeRGB(f2.ShowRed, f1.ShowGreen, f1.ShowBlue)

## For reversed anaglyph i.e. cyan-red
# MergeRGB(f1.ShowRed, f2.ShowGreen, f2.ShowBlue)


## Output the two video frames in anaglyph yellow-blue as proposed by eslave (untested)
# f1 = f1.ConvertToYV12(matrix="PC.601").tweak(bright=12.5, sat=1.25, coring=false)
# f1 = f1.ConvertToRGB32
# f1 = f1.Levels(0, 1.05, 255, 0, 255, coring=false)
# MergeRGB(f2.ShowRed, f1.ShowGreen, f1.ShowBlue)

## Output the two video frames in anaglyph blue-Yellow as proposed by eslave (untested)
# f2 = f2.ConvertToYV12(matrix="PC.601").tweak(bright=12.5, sat=1.25, coring=false)
# f2 = f2.ConvertToRGB32
# f2 = f2.Levels(0, 1.05, 255, 0, 255, coring=false)
# MergeRGB(f1.ShowRed, f2.ShowGreen, f2.ShowBlue)


## Output two video frames in Interlaced mode
## Used for Zalman Trimon, Acer Aspire 3D etc.
# f1 = SeparateFields(f1)
# f1 = SelectEven(f1)
# f2 = SeparateFields(f2)
# f2 = SelectOdd(f2)
# interleave(f2,f1)
# AssumeFieldBased()
# weave()

Return Last
}

function JermanisFinal(clip video2d, int "outputFormat", bool "resize", int "hzTargetSize", bool "incBrightness" ) {

################################################################
#                           2D to 3D CONVERSION  
#  Copyright (C) 2010   under GPL by    Branko Jermanis        <branko.jermanis@hi.htnet.hr>
#  My web pages: "Nikola Tesla and My Thoughts":   http://free-ri.htnet.hr/Branko/index.html
#
# This code is based on 2d-to-3d-03b.avs script from Anton Belev at 3D Vision Blog (http://www.3dvision-blog.com),
# and some ideas from fauxD code from eslave,
# and Caleb Davis ideas with light depth detection code,
# with indirect help of all that create this Avisynth language and useful plugin functions. Thanks to all...
# Tools that I use:
#  1.  Avisynth video scripting language
#  2.  AvsP script editor
#  3.  VirtualDub video editor (I use xvid compression for avi output)
#  4.  Media Player Classic
#  5. Download helper for Firefox browser (download from YouTube...)
#  6. WinFF convertor (flv to xvid conversion ...)
# For multimedia linux, I suggest Mint audio/video distribution:
# (I add real time kernel, JACK, Ardour, Hidrogen, Rosegarden... with M-Audio 1010LT sound card)
# Tip: press ESC for Grub, add gksudo in properties of icons, disable Timidity server on start and add starting line in JACK
# Creation date 20/12/2010 - based on original author's 0.9 version of the script - note this is very CPU/GPU intensive but works well (TJ)
################################################################

fL=video2d.ConvertToYV12

W = width(fL)
H = height(fL)

fR = DeleteFrame(fL, 0)  # Set the right frame with one frame difference

# If W is less than 640, algorithm for light depth will not work good enough (integer number will eat pixels, because it depend of dW/2).
dW =  W / 128   # More depth perception, but little more artifact on details (it is useful for me on 640x480 video)

######################    M O T I O N    D E P T H     D E T E C T I O N    ###############


## Motion mask:
super = MSuper(fL)
bvectors = MAnalyse(super, isb = true, blksize=32)  #  greatest blocksize, less problem with picture and best speed (I have been reported that on Vista 64 bit has error with 32 with some old version of mvtools2.dll). 

m = MMask(fL, bvectors, kind=3, ml=1)
m = RGBAdjust(ConvertToRGB32(m),1,1,1,1,-127,-255,-255)
m1 = RGBAdjust(ShowRed(m), .5, .5, .5, 1,0,0,0,0,255,255,255,1).Invert

fL = ConvertToRGB32(fL)   # Input in RGB, because of 'crop-even' problem with YV12 (on some movie)
fR = ConvertToRGB32(fR)

# Replace moving object on frame with opposite movement from next frame (copy/paste from R to L):
m = Layer( fL, Mask(fR, m1) )
fL = Layer( fR, Mask(fL, m1) )
fR = m

# Possible improvement in this part of code :
# 1. Some detection when  camera  rotate around object create opposite Pulfrich effect, and then it need opposite switching left/right frames (rare condition)
# 2. With high speed vertical movement, frames are too different and create bad visual overlap, and it will be better switch off this cut/paste system

######################  LIGHTING    DEPTH    DETECTION   ( ideas from Caleb Davis code)  ####################

# On 0.5 version I replace Overlay with Layer function. It is faster function (2x)...

# Add more pixels for less than HD video resolution because quality and depth improvement (for HD source, it can be disabled, for better speed):
# Disable as modern videos in 2022 tend to be HD (TJ)
# W = 2*W
# fL = LanczosResize(fL, W, H)
# fR = LanczosResize(fR, W, H)

#Create L mask:
fG = Greyscale(fL)
m1 = RGBAdjust(fG, 1,1,1,1, -127,-255,-255)  # For light  scenes
m2 = RGBAdjust(fG, 1,1,1,1, -70,-255,-255)    # For dark scenes
m = ConditionalFilter(m1, m1, m2, "AverageLuma(fG.ConvertToYV12)", ">", "70")  # Switch light/dark scenes

m1 = RGBAdjust(ShowRed(m), .5, .5, .5, 1,0,0,0,0,255,255,255,1)
m2 = Layer(m1, Mask(m1, m1), x= -1*dW, op="add")       # Resize mask to left
m12 = Layer(m2, Mask(m1, m1), x= dW, op="add")          # Resize mask to right

fL = Layer(fL, Mask(fL, m12), x= dW/2)                  # move light part of picture to right on L frame (and remove borders too)
fL = Layer(fL, Mask(fL, m12.Invert), x= -1*dW/2)  # move dark part of picture to left on L frame

#Create R mask:
fG = Greyscale(fR)
m1 = RGBAdjust(fG, 1,1,1,1, -127,-255,-255)  # For light  scenes
m2 = RGBAdjust(fG, 1,1,1,1, -70,-255,-255)    # For dark scenes
m = ConditionalFilter(m1, m1, m2, "AverageLuma(fG.ConvertToYV12)", ">", "70")  # Switch light/dark scenes

m1 = RGBAdjust(ShowRed(m), .5, .5, .5, 1,0,0,0,0,255,255,255,1)
m2 = Layer(m1, Mask(m1, m1), x= -1*dW, op="add")       # Resize mask to left
m12 = Layer(m2, Mask(m1, m1), x= dW, op="add")          # Resize mask to right

fR = Layer(fR, Mask(fR, m12.Invert), x= dW/2)  # move dark part of picture to right on R frame 
fR = Layer(fR, Mask(fR, m12), x= -1*dW/2)       # move light part of picture to left on R frame

# Going back to standard input Width (for HD source, it can be disabled, for better speed):
# Disable as modern videos in 2022 tend to be HD (TJ)
# W = W/2
# fL = LanczosResize(fL, W, H)
# fR = LanczosResize(fR, W, H)

# Possible improvement in this part of code :
# 1. Sometimes background is more white than foreground. Then it will be better if this is in reverse.

####################  OUTPUT TO GLASSES:  #############################


##  UNIVERSAL FILE FORMAT OUTPUT

outputFormat == 2 ? Eval("""
		StackVertical(fL, fR)
	""") :  Eval("""
		StackHorizontal(fL, fR)
	""") 

##################      TECHNOLOGICAL INFO:       #########################


### Good FPS conversion algorithm:
#super = MSuper(video)
#bvectors = MAnalyse(super, isb = true, blksize=4, chroma=true, truemotion=true, search = 3)
#fvectors = MAnalyse(super, isb = false, blksize=4, chroma=true, truemotion=true, search = 3 )
#video = MFlowFps(video, super, bvectors, fvectors, num=60, den=1)     # going to  60 fps

# Interesting setup:
# 1. DLP projector (ViewSonic 120Hz PJD6211 or better)
# 2. DLP-link active switching glasses without connection or emitters: XpanD X102
# 3. We need white LED emmiters (and electronic) for simulate this white flash from projector for possible using this kind of glasses on 120 Hz monitors.
# (white flash sync is when active glasses are in switching position, and both eyes are black)
# VirtualDub can open this script. Chose video-compress-xvid, and than Save as AVI, can create useful output file.
# You need lot of CPU power...

	Return Last
}
