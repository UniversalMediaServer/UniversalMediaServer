## Script for 2D to 3D On-the-Fly Real-time Video Conversion with Avisynth(+) and UniversalMediaServer
## Copyright Dr. Thomas Richard Judge 2022.  

## UniversalMediaServer generates multi-threading script wrapper code, plugin loading code and algorithm, output format and size options based on UI selections 
## before calling this function with the video2d clip and other parameters set.

function convert2dTo3d(clip video2d, int "algorithm", int "outputFormat", bool "resize", int "hzTargetSize", float "frameStretchFactor", float "lightOffsetFactor", bool "showMotionAndSceneDetection", int "mscThSCD1", int "mscThSCD2", bool "forceTargetDAR", float "targetDAR" )
{
# Defaults
	algorithm     					= default(algorithm   	     , 1     )
	outputFormat  					= default(outputFormat       , 1     )
	resize        					= default(resize      	     , true  )
	hzTargetSize  					= default(hzTargetSize       , 1920  )
	frameStretchFactor  			= default(frameStretchFactor , 8.0  )
	lightOffsetFactor   			= default(lightOffsetFactor  , 3.0  )
	showMotionAndSceneDetection 	= default(showMotionAndSceneDetection  , false )
	mscThSCD1						= default(mscThSCD1  , 248 )
	mscThSCD2						= default(mscThSCD2  , 30 )
	forceTargetDAR                  = default(forceTargetDAR  , false )
	targetDAR                       = default(targetDAR  , 16.0/9.0 )
	
	## Convert to RGB32 to avoid width restrictions
	video2d = ConvertToRGB32(video2d)

	## Optionally change the aspect ratio of the video to match the specified target Display Aspect Ratio (DAR) (using the CropResize script)
	## See, source/discussion here: https://forum.videohelp.com/threads/393752-CropResize-Cropping-resizing-script
	## 
	(forceTargetDAR == true) ? Eval("""
		video2d = CropResize(video2d, OutDAR=targetDAR)
		""") : ""
	
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

	## Create variables for left and right frame with one frame difference
	## This is the Pulfrich-like simulation that creates the illusion of depth from movement
	## Based on:
	## https://3dvision-blog.com/1220-2d-to-3d-realtime-video-conversion-script-for-avisynth-v0-3/
	## by Anton Belev at 3D Vision Blog 
	## http://www.3dvision-blog.com
	## Rationalisation, removal of obsolete output formats and support for latest formats added by Dr. Thomas Richard Judge 2022. 
	
	fL = video2d
	fR = DeleteFrame(video2d, 0)
	
	## Algorithmic Enhancement (by Thomas Judge):
	## 	 
	## The base Pulfrich-like 2D to 3D conversion algorithm takes two time delayed video frames and maps them to the left and right eye views.
	## This is both an extremely simple and effective approach. It relies on the Pulfrich effect. It does, however, require that there is 
	## relatively good correlation between the video frames selected for the left and right eye views. If this is not the case then the approach
	## temporarily breaks down. This typically happens when the source video contains fast moving action that generates excessive change from video 
	## frame to video frame or where there is a wholesale change of scene.
	## These large scale frame to frame changes, translating to large‌‌ discrepancies in the left and right eye views, both destroy the 3D Pulfrich
	## effect and cause a brief feeling of discomfort or disorientation for the viewer.
	## This algorithmic enhancement completely eliminates any sense of discomfort or disorientation due to rapid motion or scene changes. It does
	## this by first detecting rapid motion and scene changes between frames. It then ensures that, for those instances, the same view for the 
	## left eye and right eye is kept, thereby eliminating the risk of disorientation as the view temporarily reverts to 2D.	 
	## 
	## The code uses the motion vector analysis (MAnalyse) and scene change detection routines (MSCDetection) from the 
	## mvtools2 library, see documentation and github repository below:
	## 
	## https://htmlpreview.github.io/?https://github.com/pinterf/mvtools/blob/mvtools-pfmod/Documentation/mvtools2.html
	## https://github.com/pinterf/mvtools
	## 
	## In addition, it uses the mt_merge routine from the masktools2 library,
	## see documentation and github repository below:
	## 
	## http://avisynth.nl/index.php/MaskTools2/Mt_merge
	## https://github.com/pinterf/masktools
	## 
	## Although only a few lines of script are required to invoke these routines, they are very effective in addressing the excessive motion and
	## scene change issues outlined above.
	 	
	motionSource  = fR.ConvertToYV12()
	super    = motionSource.MSuper()
    vectors  = super.MAnalyse(isb = false)
    
   	## Algorithmic Enhancement (by Thomas Judge):
   	## 
   	## Performs motion compensation between the left and right eye frames to reduce/eliminate discomfort due to mis-alignment of frames.
	##   	
	## The MDepan and Depan functions are described at the links below:
	## 
	## Depan is described at link below:
	## http://www.volter.net/avisynth_en/externalfilters/depan.htm
	## 
	## MDepan is an alternative to DepanEstimate
	## MDepan is described here:
	##
	## https://htmlpreview.github.io/?https://github.com/pinterf/mvtools/blob/mvtools-pfmod/Documentation/mvtools2.html
	## 
    globalmotion = MDepan(motionSource,vectors)
    fR = Depan(motionSource,data=globalmotion,offset=1)

    ## 	Uncomment line below to show motion vectors in the right eye frame
    ## 	fR = super.MShow(vectors)     
    
	## 	From mvtools2 documentation for scene detection via MSCDetection, description of threshold parameters:
	## 	
	## 	thSCD1 (int): threshold which decides whether a block has changed between the previous frame and the current one.
	## 	When a block has changed, it means that motion estimation for it isn't relevant. It occurs for example at scene 
	## 	changes. So it is one of the thresholds used to tweak the scene changes detection engine. Raising it will lower
	## 	the number of blocks detected as changed. It may be useful for noisy or flickered video. The threshold is compared
	## 	to the SAD (Sum of Absolute Differences, a value which says how bad the motion estimation was ) value. For exactly 
	## 	identical blocks we have SAD=0. But real blocks are always different because of objects complex movement 
	## 	(zoom, rotation, deformation), discrete pixels sampling, and noise. Suppose we have two compared 8x8 blocks with 
	## 	every pixel different by 5. It this case SAD will be 8x8x5 = 320 (block will not detected as changed for thSCD1=400). 
	## 	If you use 4x4 blocks, SAD will be 320/4. If you use 16x16 blocks, SAD will be 320*4. Really this parameter is scaled
	## 	internally in MVTools, and you must always use reduced to block size 8x8 value. Default is 400 (since v.1.4.1).

	## 	thSCD2 (int): threshold which sets how many blocks have to change for the frame to be considered as a scene change. 
	## 	It is ranged from 0 to 255, 0 meaning 0 %, 255 meaning 100 %. Default is 130 ( which means 51 % ).    
    
    motionSceneDetection = motionSource.MSCDetection(vectors, thscd1=mscThSCD1, thscd2=mscThSCD2)

	##  If showMotionAndSceneDetection is set true then a pink image will display for the right eye 
	##  whenever significant motion or scene change is detected, instead of applying the merge with the left frame 
	##  using the mask as input.
    
    showMotionAndSceneDetection ? Eval("""
		fR = ConvertToRGB32(mt_merge(ConvertToPlanarRGBA(fR),ConvertToPlanarRGBA(motionSceneDetection ),ConvertToPlanarRGBA(motionSceneDetection),luma=true))
		""") : Eval("""
		fR = ConvertToRGB32(mt_merge(ConvertToPlanarRGBA(fR),ConvertToPlanarRGBA(fL),ConvertToPlanarRGBA(motionSceneDetection),luma=true))
		""") 
	
	subtitle = "Invalid algorithm specified"

		algorithm == 2 ? Eval("""
			subtitle = "Pulfrich and Lighting Depth Detection"
			video3d = PulfrichAndLight(fL,fR, outputFormat=outputFormat, videoW=videoW, videoH=videoH, frameStretchFactor=frameStretchFactor, lightOffsetFactor=lightOffsetFactor)
		""") : Eval("""
			subtitle = "Pulfrich Base" # Default
			video3d = StretchAndStackFrames(fL,fR,outputFormat=outputFormat,videoW=videoW,videoH=videoH,frameStretchFactor=frameStretchFactor)
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

function PulfrichAndLight(clip fL, clip fR, int "outputFormat", int "videoW", int "videoH",  float "frameStretchFactor", float "lightOffsetFactor") {

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
