## Script for 2D to 3D On-the-Fly Real-time Video Conversion with Avisynth(+) and UniversalMediaServer
## Copyright Dr. Thomas Richard Judge 2022.  

## UniversalMediaServer generates multi-threading script wrapper code, plugin loading code and algorithm, output format and size options based on UI selections 
## before calling this function with the video2d clip and other parameters set.

function convert2dTo3d(clip video2d, int "algorithm", int "outputFormat", bool "resize", int "hzTargetSize", float "frameStretchFactor", float "lightOffsetFactor") {

# Defaults
	algorithm     		= default(algorithm   	     , 1     )
	outputFormat  		= default(outputFormat       , 1     )
	resize        		= default(resize      	     , true  )
	hzTargetSize  		= default(hzTargetSize       , 1920  )
	frameStretchFactor  	= default(frameStretchFactor , 5.0  )
	lightOffsetFactor   	= default(lightOffsetFactor  , 3.0  )

	## Convert to RGB32 to avoid width restrictions
	video2d = ConvertToRGB32(video2d)

	## Get video width/height
	videoW = width(video2d)
	videoH = height(video2d)

	## 	
	## Values for outputFormat and their meaning 
	## 			
	## 1. FullSBS3dFormat=SBS (Full Side by Side) - Left Eye First
	## 2. FullTB3dFormat=TB/OU (Full Top/Bottom a.k.a Over/Under) - Left Eye First
	## 3. HalfSBS3dFormat=HSBS (Half Side by Side) - Left Eye First
	## 4. HalfTB3dFormat=HTB/HOU (Half Top/Bottom a.k.a Half Over/Under) - Left Eye First
	## 5. HalfUpSBS3dFormat=HSBS Upscaled (Half Side by Side) - Left Eye First
	## 6. HalfUpTB3dFormat=HTB/HOU Upscaled (Half Top/Bottom a.k.a Half Over/Under) - Left Eye First
	## 

	## If input video resizing is requested calculate the new input video size based on hzTargetSize, otherwise disable resizing 
	(resize == true && videoW > hzTargetSize) ? Eval("""
			videoH = hzTargetSize * videoH / videoW
			videoW = hzTargetSize
	""") : Eval("""
			resize = false
		""") 

	outputFormat == 3 ? Eval("""

	## If HSBS format is requested then half the width of the input video for processing efficiency. 
	## This halves the horizontal resolution for each eye for target devices that stretch Half SBS horizontally by a factor of two.

		videoW = videoW / 2
		resize = true
	""") : \
	outputFormat == 4 ? Eval("""

	## If HTB/HOU format is requested then half the height of the input video for processing efficiency. 
	## This halves the vertical resolution for each eye for target devices that stretch HTB/HOU vertically by a factor of two.

		videoH = videoH / 2
		resize = true
	""") : ""

	(algorithm == 2 && resize == true) ? Eval("""
		video2d = Lanczos4Resize(video2d, videoW, videoH)
		""") : ""
			
	subtitle = "Invalid algorithm specified"

		algorithm == 2 ? Eval("""
			subtitle = "Pulfrich and Lighting Depth Detection"
			video3d = PulfrichAndLight(video2d, outputFormat=outputFormat, videoW=videoW, videoH=videoH, frameStretchFactor=frameStretchFactor, lightOffsetFactor=lightOffsetFactor)
		""") : Eval("""
			subtitle = "Pulfrich Base" # Default
			video3d = PulfrichBase(video2d, outputFormat=outputFormat, videoW=videoW, videoH=videoH, frameStretchFactor=frameStretchFactor)
		""") 

	resize = false
	        
	videoW = width(video3d)
	videoH = height(video3d)

	## If SBS or TB/OU full format is requested then no need to resize (outputFormat == 1 or 2)

	outputFormat == 5 ? Eval("""

	## If HSBS Upscaled format is requested then double the height of the video. 
	## This retains full original resolution for each eye for target devices that stretch Half SBS horizontally by a factor of two.
	## CPU/GPU intensive for re-encoding and increased file size over the network compared to Full SBS and Half SBS.

		videoH = videoH * 2
		resize = true
	""") : \
	outputFormat == 6 ? Eval("""

	## HTB/HOU Upscaled format is requested then double the width of video. 
	## This retains full original resolution for each eye for target devices that stretch HTB/HOU vertically by a factor of two.
	## CPU/GPU intensive for re-encoding and increased file size over the network compared to Full TB and Half TB.

		videoW = videoW * 2
		resize = true
	""") : ""
	
	(resize == true) ? Eval("""
		video3d = Lanczos4Resize(video3d, videoW, videoH)
			""") : ""
	
## 	video3d = Subtitle(video3d, subtitle, align=9)
	
	return video3d
}

function PulfrichBase(clip video2d, int "outputFormat", int "videoW", int "videoH", float "frameStretchFactor") {
 
	## Based on:
	## https://3dvision-blog.com/1220-2d-to-3d-realtime-video-conversion-script-for-avisynth-v0-3/
	## by Anton Belev at 3D Vision Blog 
	## http://www.3dvision-blog.com
	## Rationalisation, removal of obsolete output formats and support for latest formats added by Dr. Thomas Richard Judge 2022. 

	## Create variables for left and right frame with one frame difference
	## This is the Pulfrich-like simulation that creates illusion of depth from movement
	fL = video2d
	fR = DeleteFrame(video2d, 0)

	Return StretchAndStackFrames(fL,fR,outputFormat=outputFormat,videoW=videoW,videoH=videoH,frameStretchFactor=frameStretchFactor)
}

function PulfrichAndLight(clip video2d, int "outputFormat", int "videoW", int "videoH",  float "frameStretchFactor", float "lightOffsetFactor") {

	# Based on code:
	#
	#                           2D to 3D CONVERSION  
	#  Copyright (C) 2010   under GPL by    Branko Jermanis        <branko.jermanis@hi.htnet.hr>
	#  My web pages: "Nikola Tesla and My Thoughts":   http://free-ri.htnet.hr/Branko/index.html
	#
	# This code is based on 2d-to-3d-03b.avs script from Anton Belev at 3D Vision Blog (http://www.3dvision-blog.com),
	# and some ideas from fauxD code from eslave,
	# and Caleb Davis ideas with light depth detection code,
	# with indirect help of all that create this Avisynth language and useful plugin functions. Thanks to all...
	# Creation date 20/12/2010 - based on original author's 0.9 version of the script - note this is very CPU/GPU intensive (TJ)
	################################################################

	fL = video2d
	fR = DeleteFrame(video2d, 0)

	## Adjusted dW calculation to be factor based (TJ)
	dW = Round(videoW * ( lightOffsetFactor / 1000.0 ) )

	## Set minimum dW value 
	dw < 2 ? Eval("""
		dw = 2
			""") : ""
					
	######################  LIGHTING    DEPTH    DETECTION   ( ideas from Caleb Davis code)  ####################

	#Create L mask:
	fG = Greyscale(fL)
	m1 = RGBAdjust(fG, 1,1,1,1, -127,-255,-255)   # For light  scenes
	m2 = RGBAdjust(fG, 1,1,1,1, -70,-255,-255)    # For dark scenes
	m = ConditionalFilter(m1, m1, m2, "AverageLuma(fG.ConvertToYV12)", ">", "70")  # Switch light/dark scenes

	m1 = RGBAdjust(ShowRed(m), .5, .5, .5, 1,0,0,0,0,255,255,255,1)
	m2 = Layer(m1, Mask(m1, m1), x= -1*dW, op="add")        # Resize mask to left
	m12 = Layer(m2, Mask(m1, m1), x= dW, op="add")          # Resize mask to right

	fL = Layer(fL, Mask(fL, m12), x= dW/2)                  # move light part of picture to right on L frame (and remove borders too)
	fL = Layer(fL, Mask(fL, m12.Invert), x= -1*dW/2)        # move dark part of picture to left on L frame

	#Create R mask:
	fG = Greyscale(fR)
	m1 = RGBAdjust(fG, 1,1,1,1, -127,-255,-255)   # For light  scenes
	m2 = RGBAdjust(fG, 1,1,1,1, -70,-255,-255)    # For dark scenes
	m = ConditionalFilter(m1, m1, m2, "AverageLuma(fG.ConvertToYV12)", ">", "70")  # Switch light/dark scenes

	m1 = RGBAdjust(ShowRed(m), .5, .5, .5, 1,0,0,0,0,255,255,255,1)
	m2 = Layer(m1, Mask(m1, m1), x= -1*dW, op="add")        # Resize mask to left
	m12 = Layer(m2, Mask(m1, m1), x= dW, op="add")          # Resize mask to right

	fR = Layer(fR, Mask(fR, m12.Invert), x= dW/2)   # move dark part of picture to right on R frame 
	fR = Layer(fR, Mask(fR, m12), x= -1*dW/2)       # move light part of picture to left on R frame

	###################### 

	Return StretchAndStackFrames(fL,fR,outputFormat=outputFormat,videoW=videoW,videoH=videoH,frameStretchFactor=frameStretchFactor)
}

function StretchAndStackFrames(clip fL, clip fR, int "outputFormat", int "videoW", int "videoH",  float "frameStretchFactor") {

	## Set the frame stretch factor
	ResW  = Round(videoW * (1.0 + ( frameStretchFactor / 100.0 ) ))
	CropW = (ResW - videoW) / 2

	## Stretch the right frame to further the depth effect
	fR = Lanczos4Resize(fR, ResW, videoH)
	fR = Crop(fR, 0, 0, videoW, videoH)

	## Stretch the left frame to further the depth effect
	fL = Lanczos4Resize(fL, ResW, videoH)
	fL = Crop(fL, CropW, 0, videoW, videoH) 

	outputFormat == 2 ? Eval("""
		StackVertical(fL, fR)
	"""): \
	outputFormat == 4 ? Eval("""
		StackVertical(fL, fR)
	"""): \	
	outputFormat == 6 ? Eval("""
		StackVertical(fL, fR)
	"""): \		
	  Eval("""
		StackHorizontal(fL, fR)
	""")

	Return Last
}
