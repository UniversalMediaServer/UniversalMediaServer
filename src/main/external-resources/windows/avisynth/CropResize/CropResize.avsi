# ===============================================================================
# ===============================================================================
#            CropResize 2022-11-19
# ===============================================================================
# ===============================================================================

function CropResize(clip Source, \
int "OutWidth", int "OutHeight", \
float "CL", float "CT", float "CR", float "CB", val "CropDAR", int "CSplit", \
bool "AutoC", int "CThresh", int "CStart", int "CSample", \
int "CPreview", int "CLine", \
val "InDAR", val "InSAR", val "OutDAR", val "OutSAR", bool "AutoDAR", \
int "Mod", int "HMod", \
bool "NoResize", bool "ResizeWO", \
bool "Borders", bool "CleanBorders", val "BColor", bool "Frosty", \
string "ColorConvert", string "ColorMode", \
string "Resizer", string "RStr", val "RingRepair", \
int "GMode", bool "RMode", int "PicDim", bool "Position", bool "Info")  {

C_R = " CropResize " + chr(10)
Assert(HasVideo(Source) && (width(Source) > 48) && (height(Source) > 48), C_R + \
" No video found or the source width or height is too small " + chr(10))
SourceAudio = !HasAudio(Source) ? Source : Source.KillVideo()
Source = Source.KillAudio()

# ===============================================================================
#            Globals
# ===============================================================================

Try{  IsPriorG1 = G1_PriorG1  }Catch(err){  IsPriorG1 = false  }
Try{  IsPriorG2 = G2_PriorG2  }Catch(err){  IsPriorG2 = false  }
Try{  IsPriorCropResize = gPriorCropResize  }Catch(err){  IsPriorCropResize = false  }
Try{  IsGCPreview = (0 <= G_CPreview < 4)  }Catch(err){  IsGCPreview = false  }
Try{  IsGResizer = defined(G_Resizer)  }Catch(err){  IsGResizer = false  }
Try{  IsGInfo = G_Info  }Catch(err){  IsGInfo = false  }
Try{  IsMX = CR_MeGUI_Support() && !IsPriorCropResize && defined(MeGUI_darx)  }Catch(err){  IsMX = false  }
Try{  IsMY = CR_MeGUI_Support() && !IsPriorCropResize && defined(MeGUI_dary)  }Catch(err){  IsMY = false  }
Try{  MInDAR = IsMX && IsMY ? float(MeGUI_darx) / float(MeGUI_dary) : 0.0  }Catch(err){  MInDAR = 0.0  }
Try{  MPassDAR = CR_MeGUI_Support() ? gMPassDAR : 0.0  }Catch(err){  MPassDAR = 0.0  }

global gPriorCropResize = true
GMode = default(GMode, 1)

Assert((0 <= GMode <= 2), C_R + " GMode must be 0, 1 or 2 for" + C_R + " (1 or 2 for GCropResize) " + chr(10))
Assert((GMode != 2) || IsPriorG2, C_R + " GMode cannot equal 2 for CropResize unless " + chr(10) + \
" GMode=2 for a previous global GCropResize instance " + chr(10))

IsGMode0 = (GMode == 0)
IsGMode1 = (GMode == 1) && IsPriorG1
IsGMode2 = (GMode == 2) && IsPriorG2
NoGMode = IsGMode0 || !IsPriorG1
IsGModeText = IsPriorG1 && (!IsGMode1 || IsPriorG2)
GAutoC = (IsGMode1 && G1_AutoC) || (IsGMode2 && G2_AutoC)
IsGAutoCPreview = (IsGMode1 && (G1_CPreview == 4)) || (IsGMode2 && (G2_CPreview == 4))

GInDAR = NoGMode ? 0.0 : IsGMode1 ? G1_InDAR : G2_InDAR
GInSAR = NoGMode ? 0.0 : IsGMode1 ? G1_InSAR : G2_InSAR
GOutDAR = !IsPriorG1 ? 0.0 : !IsGMode2 ? G1_OutDAR : G2_OutDAR
GOutSAR = !IsPriorG1 ? 0.0 : !IsGMode2 ? G1_OutSAR : G2_OutSAR

Source_Width = \
IsGMode1 && G1_AutoC ? G1_Source_Width : IsGMode2 && G2_AutoC ? G2_Source_Width : width(Source)
F_Source_Width = float(Source_Width)
Source_Height = \
IsGMode1 && G1_AutoC ? G1_Source_Height : IsGMode2 && G2_AutoC ? G2_Source_Height : height(Source)
F_Source_Height = float(Source_Height)

# ===============================================================================
#            Defaults
# ===============================================================================

OutWidth = default(OutWidth, 0)
OutWidth = !IsPriorG1 || (OutWidth != 0) ? OutWidth : !IsGMode2 ? G1_OutWidth : G2_OutWidth
OutHeight = default(OutHeight, 0)
OutHeight = !IsPriorG1 || (OutHeight != 0) ? OutHeight : !IsGMode2 ? G1_OutHeight : G2_OutHeight

UseWidth = (OutWidth == -1)
TryWidth = (OutWidth < -1)
OutWidth = !UseWidth && !TryWidth ? OutWidth : UseWidth ? 0 : -OutWidth
UseHeight = (OutHeight == -1)
TryHeight = (OutHeight < -1)
OutHeight = !UseHeight && !TryHeight ? OutHeight : UseHeight ? 0 : -OutHeight

CL = default(CL, NoGMode ? 0.0 : IsGMode1 ? G1_CL : G2_CL)
CT = default(CT, NoGMode ? 0.0 : IsGMode1 ? G1_CT : G2_CT)
CR = default(CR, NoGMode ? 0.0 : IsGMode1 ? G1_CR : G2_CR)
CB = default(CB, NoGMode ? 0.0 : IsGMode1 ? G1_CB : G2_CB)
CR = (CR == 0) ? 0.0 : (CR < 0) ? -CR : F_Source_Width - CL - CR
CB = (CB == 0) ? 0.0 : (CB < 0) ? -CB : F_Source_Height - CT - CB
CR_Test = ((CL + CR) < Source_Width)
CB_Test = ((CT + CB) < Source_Height)

CropDAR = default(CropDAR, NoGMode ? 0.0 : IsGMode1 ? G1_CropDAR : G2_CropDAR)
CSplit = default(CSplit, NoGMode ? 1 : IsGMode1 ? G1_CSplit : G2_CSplit)
AutoC = default(AutoC, false)
CThresh = default(CThresh, 30)
LastFrame = FrameCount(Source) - 1
CStart = default(CStart, (LastFrame > 3000) ? 250 : 0)
CSample = default(CSample, 5)
CPreview = default(CPreview, \
IsGCPreview ? G_CPreview : !IsPriorG1 || IsGAutoCPreview ? 0 : !IsGMode2 ? G1_CPreview : G2_CPreview)
CLine = default(CLine, !IsPriorG1 ? CR_CLine(Source_Width, Source_Height) : !IsGMode2 ? G1_CLine : G2_CLine)

IsInDAR = defined(InDAR)
IsInSAR = defined(InSAR)
IsOutDAR = defined(OutDAR)
IsOutSAR = defined(OutSAR)
InDAR = default(InDAR, 0.0)
InSAR = default(InSAR, 0.0)
OutDAR = default(OutDAR, 0.0)
OutSAR = default(OutSAR, 0.0)
AutoDAR = default(AutoDAR, !IsPriorG1 ? CR_AutoDAR() : !IsGMode2 ? G1_AutoDAR : G2_AutoDAR)

IsMod = defined(Mod)
Mod = default(Mod, !IsPriorG1 ? 4 : !IsGMode2 ? G1_Mod : G2_Mod)
F_Mod = float(Mod)
HMod = default(HMod, IsMod ? Mod : !IsPriorG1 ? Mod : !IsGMode2 ? G1_HMod : G2_HMod)
F_HMod = float(HMod)

NoResize = default(NoResize, NoGMode ? false : IsGMode1 ? G1_NoResize : G2_NoResize)
ResizeWO = default(ResizeWO, NoGMode ? false : IsGMode1 ? G1_ResizeWO : G2_ResizeWO)
Borders = default(Borders, NoGMode ? false : IsGMode1 ? G1_Borders : G2_Borders)
CleanBorders = default(CleanBorders, NoGMode ? false : IsGMode1 ? G1_CleanBorders : G2_CleanBorders)
BColor = default(BColor, NoGMode ? $000000 : IsGMode1 ? G1_BColor : G2_BColor)
Frosty = default(Frosty, NoGMode ? false : IsGMode1 ? G1_Frosty : G2_Frosty)

CC = default(ColorConvert, NoGMode ? CR_ColorConvert() : IsGMode1 ? G1_ColorConvert : G2_ColorConvert)
CM = default(ColorMode, NoGMode ? "" : IsGMode1 ? G1_ColorMode : G2_ColorMode)

IsResizer = defined(Resizer)
Resizer = \
default(Resizer, IsGResizer ? G_Resizer : !IsPriorG1 ? CR_Resizer_Default() : !IsGMode2 ? G1_Resizer : G2_Resizer)
RStr = default(RStr, IsResizer || (Resizer == "") ? "" : \
IsGResizer ? G_RStr : !IsPriorG1 ? CR_RStr_Default() : !IsGMode2 ? G1_RStr : G2_RStr)
RingRepair = default(RingRepair, !IsPriorG1 ? false : !IsGMode2 ? G1_RingRepair : G2_RingRepair)

RMode = default(RMode, IsPriorCropResize ? false : (IsPriorG1 && G_RMode))
PicDim = default(PicDim, !IsPriorG1 ? CR_Info_AutoPicDim() : !IsGMode2 ? G1_PicDim : G2_PicDim)
Position = default(Position, NoGMode ? false : IsGMode1 ? G1_Position : G2_Position)
Info = default(Info, IsGInfo ? G_Info : !IsPriorG1 ? false : !IsGMode2 ? G1_Info : G2_Info)

# ===============================================================================
#            Miscellaneous
# ===============================================================================

AvsStr = LCase(VersionString())
IsAvsPlus = (FindStr(AvsStr, "avisynth+") > 0)
IsAvsNeo = (FindStr(AvsStr, "avisynth neo") > 0)
NumA = IsAvsPlus || IsAvsNeo ? FindStr(AvsStr, "(r") + 2 : 0
NumB = IsAvsPlus || IsAvsNeo ? FindStr(AvsStr, ",") : 0
NumC = (NumB > NumA) ? NumB - NumA : undefined()
AvsBuild = (NumA > 2) ? int(value(MidStr(AvsStr, NumA, NumC))) : 0
IsAvsHBD = (AvsBuild > 2294)

IsRGBSource = IsRGB(Source)
Is411Source = IsYV411(Source)
Color_Format = PixelType(Source)
Source_Bits = IsAvsHBD ? BitsPerComponent(Source) : 8

WidthMod = Is411Source ? 4 : \
(IsAvsHBD && (Is420(Source) || Is422(Source))) || IsYV12(Source) || IsYV16(Source) || IsYUY2(Source) ? 2 : 1
HeightMod = (IsAvsHBD && Is420(Source)) || IsYV12(Source) ? 2 : 1
F_WidthMod = float(WidthMod)
F_HeightMod = float(HeightMod)

CroppedMoreLeft = (CL > CR)
CroppedMoreTop = (CT > CB)
CSplit1 = (CSplit == 1)
CSplit2 = (CSplit == 2)
IsAutoCPreview = AutoC && (CPreview == 4)
IsScriptCPreview = (0 < CPreview < 4) && !IsAutoCPreview && !IsGAutoCPreview
Info = Info && !IsAutoCPreview && !IsGAutoCPreview

# ===============================================================================
#            Configure Resizing
# ===============================================================================

IsResampleMT = IsAvsHBD ? FunctionExists("Spline36ResizeMT") : Eval("""  Try{  Spline36ResizeMT(xx=0)  }
Catch(MT_error){  (FindStr(LCase(MT_error), "there is no function named") == 0)  }  """)

IsResizer = (Resizer != "")
IsZResize = (LeftStr(Resizer, 2) == "z_")
IsDResize = (LeftStr(Resizer, 2) == "d_")
IsFResize = (LeftStr(Resizer, 2) == "f_")

Resizer = !IsResizer ? "Spline36" : \
IsZResize || IsDResize || IsFResize ? ((StrLen(Resizer) == 2) ? "Spline36" : MidStr(Resizer, 3)) : Resizer

IsResizeMTAppended = (RightStr(Resizer, 8) == "ResizeMT") || (RightStr(Resizer, 8) == "MTResize")
IsMTAppended = (RightStr(Resizer, 2) == "MT")
IsResizeMT = IsResizeMTAppended || IsMTAppended
IsResizeAppended = (RightStr(Resizer, 6) == "Resize")

Kernel = IsResizeMTAppended ? LeftStr(Resizer, StrLen(Resizer) - 8) : \
IsMTAppended ? LeftStr(Resizer, StrLen(Resizer) - 2) : \
IsResizeAppended ? LeftStr(Resizer, StrLen(Resizer) - 6) : Resizer

IsAvsKernel = (Kernel == "Bicubic") || (Kernel == "Bilinear") || (Kernel == "Blackman") || (Kernel == "Gauss") || \
(Kernel == "Lanczos") || (Kernel == "Lanczos3") || (Kernel == "Lanczos4") || (Kernel == "Point") || \
(Kernel == "Spline16") || (Kernel == "Spline36") || (Kernel == "Spline64") || (Kernel == "Sinc")

IsZKernel = (Kernel == "Bicubic") || (Kernel == "Bilinear") || (Kernel == "Lanczos") || \
(Kernel == "Lanczos3") || (Kernel == "Lanczos4") || (Kernel == "Point") || \
(Kernel == "Spline16") || (Kernel == "Spline36") || (Kernel == "Spline64")

IsDKernel = IsAvsKernel || (Kernel == "BlackmanMinlobe") || (Kernel == "Box") || (Kernel == "Cubic") || \
(Kernel == "Gaussian") || (Kernel == "Linear") || (Kernel == "Rect") || (Kernel == "Spline")

IsFKernel = IsDKernel
Original_K = Kernel
IsRStr = (RStr != "")

Kernel = (Kernel == "Lanczos3") || \
((IsZResize || IsDResize || IsFResize) && (Kernel == "Lanczos4")) ? "Lanczos" : Kernel

Resizer = IsZResize || IsDResize || IsFResize ? Kernel : \
(!IsResizer && IsResampleMT) || IsResizeMT ? Kernel + "ResizeMT" : \
IsAvsKernel || IsResizeAppended ? Kernel + "Resize"  : Kernel

RTaps = !IsZResize && !IsDResize && !IsFResize ? "" : \
(Original_K == "Lanczos3") ? (IsZResize ? ", filter_param_a=3" : ", Taps=3") : \
(Original_K == "Lanczos4") ? (IsZResize ? ", filter_param_a=4" : ", Taps=4") : ""

ResStr = !IsRStr ? "" : ", " + RStr
IsRingRepair = IsFloat(RingRepair) ? (RingRepair > 0) : RingRepair
NoResizing = NoResize || CleanBorders
ResizeWO = ResizeWO && !NoResizing
FullResizing = !NoResizing && !ResizeWO
IsAutoDAR = AutoDAR && FullResizing

# ===============================================================================
#            Insist
# ===============================================================================

# -------------------------------------------------------------------------------
#            OutWidth & OutHeight
# -------------------------------------------------------------------------------

Assert((!UseWidth && !TryWidth) || (!UseHeight && !TryHeight), C_R + \
" OutWidth & OutHeight cannot be negative at the same time " + chr(10))
Assert((!UseWidth && !TryWidth && !UseHeight && !TryHeight) || FullResizing, C_R + \
" OutWidth & OutHeight can only be negative in full resizing mode " + chr(10))
Assert((OutWidth == 0) || (WidthMod != 2) || (OutWidth % 2 == 0), C_R + \
" " + Color_Format + " requires a mod2 OutWidth " + chr(10))
Assert((OutWidth == 0) || (WidthMod != 4) || (OutWidth % 4 == 0), C_R + \
" " + Color_Format + " requires a mod4 OutWidth " + chr(10))
Assert((OutHeight == 0) || (HeightMod != 2) || (OutHeight % 2 == 0), C_R + \
" " + Color_Format + " requires a mod2 OutHeight " + chr(10))

# -------------------------------------------------------------------------------
#            Crop / AutoC / CThresh / CPreview
# -------------------------------------------------------------------------------

Assert((CL >= 0) && (CT >= 0), C_R + " CL & CT must be greater than, or equal to zero " + chr(10))
Assert(CR_Test, C_R + " Left plus right cropping must be less than the source width " + chr(10))
Assert(CB_Test, C_R + " Top plus bottom cropping must be less than the source height " + chr(10))
Assert((1 <= CSplit <= 3), C_R + " CSplit must be 1, 2 or 3 " + chr(10))
Assert(!GAutoC || !AutoC, C_R + " AutoC cannot be true for CropResize when global " + chr(10) + \
" auto-cropping has been enabled via the GCropResize function " + chr(10))
Assert(AutoC || (CPreview != 4), C_R + " AutoC must be true when CPreview=4 " + chr(10))
Assert(!AutoC || (CStart < LastFrame), C_R + \
string(LastFrame, " When AutoC=true CStart must be less than %.0f (the final frame number) ") + chr(10))
Assert((1 <= CThresh <= 255), C_R + " CThresh must be 1 thru 255 " + chr(10))
Assert((0 <= CPreview <= 4) && (!IsGCPreview || (0 <= G_CPreview <= 4)), C_R + \
" CPreview must be 0 thru 4 " + chr(10))
Assert((1 <= CR_CPreview() <= 3), C_R + " CR_CPreview() must be 1, 2 or 3 " + chr(10))
Assert((CLine >= 1), C_R + " CLine must be greater than, or equal to 1 " + chr(10))

# -------------------------------------------------------------------------------
#            CropDAR / InDAR / InSAR / OutDAR / OutSAR
# -------------------------------------------------------------------------------

Assert(((IsFloat(CropDAR) && (CropDAR == 0)) || (IsFloat(CropDAR) && !IsInt(CropDAR) && (CropDAR > 0))) && \
((IsFloat(InDAR) && (InDAR == 0)) || (IsFloat(InDAR) && !IsInt(InDAR) && (InDAR > 0))) && \
((IsFloat(InSAR) && (InSAR == 0)) || (IsFloat(InSAR) && !IsInt(InSAR) && (InSAR > 0))) && \
((IsFloat(OutDAR) && (OutDAR == 0)) || (IsFloat(OutDAR) && !IsInt(OutDAR) && (OutDAR > 0))) && \
((IsFloat(OutSAR) && (OutSAR == 0)) || (IsFloat(OutSAR) && !IsInt(OutSAR) && (OutSAR > 0))), C_R + \
" CropDAR, InDAR, InSAR, OutDAR & OutSAR " + chr(10) + \
" must be zero or a positive floating point number " + chr(10) + " (4.0/3.0 or 1.7778 etc) " + chr(10))
Assert((InDAR == 0) || (InSAR == 0), C_R + " InDAR & InSAR cannot be specified together " + chr(10))
Assert((OutDAR == 0) || (OutSAR == 0), C_R + " OutDAR & OutSAR cannot be specified together " + chr(10))

# -------------------------------------------------------------------------------
#            Mod
# -------------------------------------------------------------------------------

Assert((Mod == 1) || (Mod == 2) || (Mod == 4) || (Mod == 8) || (Mod == 16), C_R + \
" Mod must be 1, 2, 4, 8 or 16 " + chr(10))
Assert((HMod == 1) || (HMod == 2) || (HMod == 4) || (HMod == 8) || (HMod == 16), C_R + \
" HMod must be 1, 2, 4, 8 or 16 " + chr(10))
Assert((WidthMod != 2) || (Mod % 2 == 0), C_R + " " + Color_Format + " requires a minimum of Mod=2 " + chr(10))
Assert((WidthMod != 4) || (Mod % 4 == 0), C_R + " " + Color_Format + " requires a minimum of Mod=4 " + chr(10))
Assert((HeightMod != 2) || (HMod % 2 == 0), C_R + \
" " + Color_Format + " requires a minimum of HMod=2 " + chr(10))

# -------------------------------------------------------------------------------
#            NoResize / CleanBorders / ResizeWO
# -------------------------------------------------------------------------------

Assert(!NoResize || !ResizeWO, C_R + " NoResize & ResizeWO cannot be true at the same time " + chr(10))
Assert(!CleanBorders || !ResizeWO, C_R + \
" CleanBorders & ResizeWO cannot be true at the same time " + chr(10))
Assert((!NoResize && !CleanBorders) || (0 == OutDAR == OutSAR == GOutDAR == GOutSAR), C_R + \
" An OutDAR/OutSAR cannot be specified when " + chr(10) + \
" either NoResize or CleanBorders are true " + chr(10))
Assert(!ResizeWO || (CropDAR == 0) || \
(((OutDAR == 0) || (OutDAR >= CropDAR)) && ((GOutDAR == 0) || (GOutDAR >= CropDAR))), C_R + \
" When ResizeWO=true and both an OutDAR and a CropDAR are specified " + chr(10) + \
" OutDAR must be greater than or equal to CropDAR " + chr(10))
Assert(!ResizeWO || (!Borders && !Frosty) || (0 == OutDAR == GOutDAR) || (CropDAR > 0), C_R + \
" When an OutDAR is specified and ResizeWO=true, " + chr(10) + \
" borders cannot be enabled unless a CropDAR is also specified " + chr(10) + \
" (OutDAR and CropDAR together automatically enable borders) " + chr(10))
Assert(!ResizeWO || (!Borders && !Frosty) || (0 == OutDAR == GOutDAR) || (OutHeight == 0), C_R + \
" When ResizeWO=true, an OutDAR is specified and borders are enabled " + chr(10) + \
" OutHeight must be unspecified or equal to zero " + chr(10) + \
" (OutHeight can be used with an OutSAR to add top/bottom borders) " + chr(10))

# -------------------------------------------------------------------------------
#            Resizing
# -------------------------------------------------------------------------------

Assert(!IsResizeMT || (!IsZResize && !IsDResize && !IsFResize), C_R + \
""" When a resizer is prefixed with "Z_", "D_", or "F_" """ + chr(10) + \
" the resizer name cannot end with MT or ResizeMT etc " + chr(10))
Assert(!IsZResize || IsZKernel, C_R + " AVSResize has been specified for resizing (Z_" + Resizer + ") " + \
chr(10) + " but not with a supported resizer " + chr(10))
Assert(!IsDResize || IsDKernel, C_R + " DitherTools has been specified for resizing (D_" + Resizer + ") " + \
chr(10) + " but not with a supported resizer " + chr(10))
Assert(!IsFResize || IsFKernel, C_R + " FMTConv has been specified for resizing (F_" + Resizer + ") " + \
chr(10) + " but not with a supported resizer " + chr(10))
Assert(!IsZResize || !Is411Source, C_R + " Resizing YV411 isn't supported for AVSResize " + chr(10))
Assert(!IsDResize || (!IsRGBSource && (Source_Bits == 8)), C_R + \
" Resizing or color conversion with DitherTools only supports 8 bit YUV " + chr(10))
Assert(!IsFResize || !Is411Source, C_R + " Resizing YV411 isn't supported for FMTConv " + chr(10))
Assert(!IsZResize || IsAvsHBD, C_R + \
" Avisynth+ is required for resizing or color conversion with AVSResize " + chr(10))
Assert(!IsFResize || IsAvsHBD, C_R + \
" Avisynth+ is required for resizing or color conversion with FMTConv " + chr(10))

# -------------------------------------------------------------------------------
#            Ringing Repair
# -------------------------------------------------------------------------------

Assert(IsBool(RingRepair) || IsFloat(RingRepair), C_R + " RingRepair must be bool, integer or float " + chr(10))
Assert(IsBool(RingRepair) || (0 <= RingRepair <= 1), C_R + \
" When RingRepair is an integer or float, the allowed range is 0 to 1 " + chr(10))
Assert(!IsRingRepair || !IsRGBSource, C_R + " RingRepair doesn't support RGB " + chr(10))
Assert(!IsRingRepair || (!NoResize && !CleanBorders), C_R + \
" RingRepair cannot be enabled when resizing is disabled " + chr(10))

# -------------------------------------------------------------------------------
#            PicDim
# -------------------------------------------------------------------------------

Assert((0 <= PicDim <= 255), C_R + " PicDim must be 0 thru 255 " + chr(10))

# -------------------------------------------------------------------------------
#            MeGUI
# -------------------------------------------------------------------------------

Assert(!IsMX || (IsInt(MeGUI_darx) && (MeGUI_darx > 0)), C_R + \
" MeGUI_darx must be an integer greater than zero " + chr(10))
Assert(!IsMY || (IsInt(MeGUI_dary) && (MeGUI_dary > 0)), C_R + \
" MeGUI_dary must be an integer greater than zero " + chr(10))
Assert((IsMX == IsMY), C_R + " MeGUI_darx & MeGUI_dary must be specified together " + chr(10))

# ===============================================================================
#            Define Source
# ===============================================================================

AutoC && !IsAutoCPreview && !GAutoC && !IsGAutoCPreview ? Eval("""

AutoCroppedSource = \
Source.autocrop(mode=0, wMultOf=2, hMultOf=2, leftadd=0, topadd=0, rightadd=0, bottomadd=0, \
threshold=CThresh, samples=CSample, samplestartframe=CStart, sampleendframe=-1, aspect=0)

""", "Define Source Eval") : Eval("  AutoCroppedSource = Source  ")

# ===============================================================================
#            Widths, Heights & Aspect Ratios
# ===============================================================================

IsCropDAR = (CropDAR > 0)
IsGInDAR = (GInDAR > 0) && !IsInDAR && !IsInSAR
IsGInSAR = (GInSAR > 0) && !IsInDAR && !IsInSAR && !IsGInDAR
IsGOutDAR = (GOutDAR > 0) && !IsOutDAR && !IsOutSAR
IsGOutSAR = (GOutSAR > 0) && !IsOutDAR && !IsOutSAR && !IsGOutDAR
IsMInDAR = (MInDAR > 0) && !IsInDAR && !IsInSAR && !IsGInDAR && !IsGInSAR
IsMPassThru = (MPassDAR > 0) && !IsInDAR && !IsInSAR && !IsGInDAR && !IsGInSAR
IsMPassedThru = IsMPassThru && IsGMode1

InputDAR = (InDAR > 0) ? InDAR : IsGInDAR ? GInDAR : IsMInDAR ? MInDAR : IsMPassedThru ? MPassDAR : 0
InputSAR = (InSAR > 0) ? InSAR : IsGInSAR ? GInSAR : 0
OutputDAR = (OutDAR > 0) ? OutDAR : IsGOutDAR ? GOutDAR : 0
OutputSAR = (OutSAR > 0) ? OutSAR : IsGOutSAR ? GOutSAR : 0

IsInputDAR = (InputDAR > 0)
IsInputSAR = (InputSAR > 0)
IsOutputDAR = (OutputDAR > 0)
IsOutputSAR = (OutputSAR > 0)

IsBordered = Borders || Frosty || CleanBorders || (IsCropDAR && IsOutputDAR) || \
(FullResizing && IsCropDAR && (OutWidth > 0 < OutHeight))

AnamorphicInput = IsInputDAR || IsInputSAR
AnamorphicOutput = (NoResizing && AnamorphicInput) || IsOutputDAR || IsOutputSAR

Auto_Cropped_Width = width(AutoCroppedSource)
F_Auto_Cropped_Width = float(Auto_Cropped_Width)
Auto_Cropped_Height = height(AutoCroppedSource)
F_Auto_Cropped_Height = float(Auto_Cropped_Height)

Auto_Cropped_StorageAR = F_Auto_Cropped_Width / F_Auto_Cropped_Height
Auto_Cropped_DAR = !AnamorphicInput ? Auto_Cropped_StorageAR : IsInputDAR ? \
F_Auto_Cropped_Width * F_Source_Height * InputDAR / F_Auto_Cropped_Height / F_Source_Width : \
F_Auto_Cropped_Width * InputSAR / F_Auto_Cropped_Height

F_Source_Display_Width = !AnamorphicInput ? F_Source_Width : \
IsInputDAR ? InputDAR * F_Source_Height : F_Source_Width * InputSAR

Source_StorageAR = F_Source_Width / F_Source_Height
Source_DAR = !AnamorphicInput ? Source_StorageAR : \
IsInputDAR ? InputDAR : F_Source_Width * InputSAR / F_Source_Height
Source_SAR = !AnamorphicInput ? 1.0 : IsInputDAR ? F_Source_Height * InputDAR / F_Source_Width : InputSAR

F_Cropped_Source_Width = F_Auto_Cropped_Width - CL - CR
F_Cropped_Source_Height = F_Auto_Cropped_Height - CT - CB

# ===============================================================================
#            No Resizing
# ===============================================================================

NoResizing ? Eval("""

NR_WMod = !CleanBorders ? Mod : WidthMod
NR_HMod = !CleanBorders ? HMod : HeightMod
F_NR_WMod = !CleanBorders ? F_Mod : F_WidthMod
F_NR_HMod = !CleanBorders ? F_HMod : F_HeightMod

F_NR_CL_Adjust = CSplit1 && (fmod(CL, WidthMod) != 0) ? F_WidthMod - fmod(CL, WidthMod) : 0.0
F_NR_CT_Adjust = CSplit1 && (fmod(CT, HeightMod) != 0) ? F_HeightMod - fmod(CT, HeightMod) : 0.0
F_NR_CR_Adjust = CSplit1 && (fmod(CR, WidthMod) != 0) ? F_WidthMod - fmod(CR, WidthMod) : 0.0
F_NR_CB_Adjust = CSplit1 && (fmod(CB, HeightMod) != 0) ? F_HeightMod - fmod(CB, HeightMod) : 0.0

NR_Mod_Cropped_Width = \
floor((F_Cropped_Source_Width - F_NR_CL_Adjust - F_NR_CR_Adjust) / F_NR_WMod) * NR_WMod
F_NR_Mod_Cropped_Width = float(NR_Mod_Cropped_Width)

NR_Mod_Cropped_Height = \
floor((F_Cropped_Source_Height - F_NR_CT_Adjust - F_NR_CB_Adjust) / F_NR_HMod) * NR_HMod
F_NR_Mod_Cropped_Height = float(NR_Mod_Cropped_Height)

NR_Mod_Cropped_StorageAR = F_NR_Mod_Cropped_Width / F_NR_Mod_Cropped_Height
NR_Mod_Cropped_DAR = !AnamorphicInput ? NR_Mod_Cropped_StorageAR : IsInputDAR ? \
F_NR_Mod_Cropped_Width * F_Source_Height * InputDAR / F_NR_Mod_Cropped_Height / F_Source_Width : \
F_NR_Mod_Cropped_Width * InputSAR / F_NR_Mod_Cropped_Height

NR_Width = NR_Mod_Cropped_Width
F_NR_Width = F_NR_Mod_Cropped_Width
NR_Height = NR_Mod_Cropped_Height
F_NR_Height = F_NR_Mod_Cropped_Height

!IsCropDAR || (abs(CropDAR - NR_Mod_Cropped_DAR) < 0.000005) ? nop() : \
(CropDAR < NR_Mod_Cropped_DAR) ? Eval("

F_NR_Width = F_NR_Height * CropDAR / Source_SAR
F_NR_Width_Adjust = F_NR_Width - (floor(F_NR_Width / F_NR_WMod) * NR_WMod)
F_NR_Width_Adjust_Up = F_NR_Width + F_NR_WMod - F_NR_Width_Adjust
F_NR_Width_Adjust_Down = F_NR_Width - F_NR_Width_Adjust
F_NR_Width = (F_NR_Width_Adjust_Up > F_NR_Mod_Cropped_Width) ? F_NR_Width_Adjust_Down : \
(F_NR_Width_Adjust >= (F_NR_WMod / 2.0)) ? F_NR_Width_Adjust_Up : F_NR_Width_Adjust_Down
NR_Width = round(F_NR_Width)

") : Eval("

F_NR_Height = F_NR_Width / CropDAR * Source_SAR
F_NR_Height_Adjust = F_NR_Height - (floor(F_NR_Height / F_NR_HMod) * NR_HMod)
F_NR_Height_Adjust_Up = F_NR_Height + F_NR_HMod - F_NR_Height_Adjust
F_NR_Height_Adjust_Down = F_NR_Height - F_NR_Height_Adjust
F_NR_Height = (F_NR_Height_Adjust_Up > F_NR_Mod_Cropped_Height) ? F_NR_Height_Adjust_Down : \
(F_NR_Height_Adjust > (F_NR_HMod / 2.0)) ? F_NR_Height_Adjust_Up : F_NR_Height_Adjust_Down
NR_Height = round(F_NR_Height)

")

# -------------------------------------------------------------------------------
#            No Resizing - Cropping
# -------------------------------------------------------------------------------

F_NR_Extra_Width_Crop = F_Cropped_Source_Width - F_NR_Width - F_NR_CL_Adjust - F_NR_CR_Adjust
F_NR_Extra_Height_Crop = F_Cropped_Source_Height - F_NR_Height - F_NR_CT_Adjust - F_NR_CB_Adjust

F_NR_Extra_Crop_LR = CSplit1 ? 0.0 : max(0.0, F_NR_Extra_Width_Crop / 2.0)
F_NR_Extra_Crop_TB = CSplit1 ? 0.0 : max(0.0, F_NR_Extra_Height_Crop / 2.0)

F_NR_Extra_CL = CSplit1 ? (CroppedMoreLeft ? \
((WidthMod == 4) ? floor(F_NR_Extra_Width_Crop / 8.0) * 4 : \
(WidthMod == 2) ? floor(F_NR_Extra_Width_Crop / 4.0) * 2 : floor(F_NR_Extra_Width_Crop / 2.0)) : \
(WidthMod == 4) ? round(F_NR_Extra_Width_Crop / 8.0) * 4 : \
(WidthMod == 2) ? round(F_NR_Extra_Width_Crop / 4.0) * 2 : round(F_NR_Extra_Width_Crop / 2.0)) : 0.0

F_NR_Extra_CR = CSplit1 ? F_NR_Extra_Width_Crop - F_NR_Extra_CL : 0.0

F_NR_Extra_CT = CSplit1 ? (CroppedMoreTop ? \
((HeightMod == 2) ? floor(F_NR_Extra_Height_Crop / 4.0) * 2 : floor(F_NR_Extra_Height_Crop / 2.0)) : \
(HeightMod == 2) ? round(F_NR_Extra_Height_Crop / 4.0) * 2 : round(F_NR_Extra_Height_Crop / 2.0)) : 0.0

F_NR_Extra_CB = CSplit1 ? F_NR_Extra_Height_Crop - F_NR_Extra_CT : 0.0

F_NR_Extra_CL = F_NR_Extra_CL + F_NR_CL_Adjust
F_NR_Extra_CR = F_NR_Extra_CR + F_NR_CR_Adjust
F_NR_Extra_CT = F_NR_Extra_CT + F_NR_CT_Adjust
F_NR_Extra_CB = F_NR_Extra_CB + F_NR_CB_Adjust

NR_CL = round(max(0, CSplit1 ? CL + F_NR_Extra_CL : CSplit2 ? CL - fmod(CL, WidthMod) : 0))
NR_CT = round(max(0, CSplit1 ? CT + F_NR_Extra_CT : CSplit2 ? CT - fmod(CT, HeightMod) : 0))
NR_CR = round(max(0, CSplit1 ? CR + F_NR_Extra_CR : CSplit2 ? CR - fmod(CR, WidthMod) : 0))
NR_CB = round(max(0, CSplit1 ? CB + F_NR_Extra_CB : CSplit2 ? CB - fmod(CB, HeightMod) : 0))

NR_RCL = max(0, CSplit1 ? 0 : CSplit2 ? CL + F_NR_Extra_Crop_LR - NR_CL : CL + F_NR_Extra_Crop_LR)
NR_RCT = max(0, CSplit1 ? 0 : CSplit2 ? CT + F_NR_Extra_Crop_TB - NR_CT : CT + F_NR_Extra_Crop_TB)
NR_RCR = max(0, CSplit1 ? 0 : CSplit2 ? CR + F_NR_Extra_Crop_LR - NR_CR : CR + F_NR_Extra_Crop_LR)
NR_RCB = max(0, CSplit1 ? 0 : CSplit2 ? CB + F_NR_Extra_Crop_TB - NR_CB : CB + F_NR_Extra_Crop_TB)

F_NR_Cropped_Width = \
CSplit1 ? F_Cropped_Source_Width - F_NR_Extra_CL - F_NR_Extra_CR : \
F_Cropped_Source_Width - F_NR_Extra_Width_Crop
F_NR_Cropped_Height = \
CSplit1 ? F_Cropped_Source_Height - F_NR_Extra_CT - F_NR_Extra_CB : \
F_Cropped_Source_Height - F_NR_Extra_Height_Crop

# -------------------------------------------------------------------------------
#            No Resizing - Cropped Aspect Ratios
# -------------------------------------------------------------------------------

NR_Cropped_StorageAR = F_NR_Cropped_Width / F_NR_Cropped_Height
NR_Cropped_DAR = !AnamorphicInput ? NR_Cropped_StorageAR : IsInputDAR ? \
F_NR_Cropped_Width * F_Source_Height * InputDAR / F_NR_Cropped_Height / F_Source_Width : \
F_NR_Cropped_Width * InputSAR / F_NR_Cropped_Height

""", "No Resizing Eval") : ResizeWO ? Eval("""

# ===============================================================================
#            Resize Width Only
# ===============================================================================

F_RWO_CT_Adjust = CSplit1 && (fmod(CT, HeightMod) != 0) ? F_HeightMod - fmod(CT, HeightMod) : 0.0
F_RWO_CB_Adjust = CSplit1 && (fmod(CB, HeightMod) != 0) ? F_HeightMod - fmod(CB, HeightMod) : 0.0

F_RWO_Resized_Cropped_Width = !AnamorphicInput ? F_Cropped_Source_Width : \
IsInputDAR ? F_Cropped_Source_Width * F_Source_Height * InputDAR / F_Source_Width : \
F_Cropped_Source_Width * InputSAR

RWO_Mod_Cropped_Resized_Width = floor(F_RWO_Resized_Cropped_Width / F_Mod) * Mod
F_RWO_Mod_Cropped_Resized_Width = float(RWO_Mod_Cropped_Resized_Width)

RWO_Mod_Cropped_Height = \
floor((F_Cropped_Source_Height - F_RWO_CT_Adjust - F_RWO_CB_Adjust) / F_HMod) * HMod
F_RWO_Mod_Cropped_Height = float(RWO_Mod_Cropped_Height)

RWO_Resized_Cropped_DAR = F_RWO_Resized_Cropped_Width / F_RWO_Mod_Cropped_Height
RWO_Resized_Mod_Cropped_DAR = F_RWO_Mod_Cropped_Resized_Width / F_RWO_Mod_Cropped_Height
RWO_Resizing_DAR = IsCropDAR ? CropDAR : IsOutputDAR ? OutputDAR : RWO_Resized_Mod_Cropped_DAR

RWO_Width = (OutWidth > 0) && IsOutputDAR ? OutWidth : RWO_Mod_Cropped_Resized_Width
F_RWO_Width = float(RWO_Width)

RWO_Height = (RWO_Resizing_DAR <= RWO_Resized_Cropped_DAR) || \
(abs(RWO_Resizing_DAR - RWO_Resized_Cropped_DAR) < 0.000005) ? RWO_Mod_Cropped_Height : \
floor(F_RWO_Resized_Cropped_Width / RWO_Resizing_DAR / F_HMod) * HMod
F_RWO_Height = float(RWO_Height)

RWO_Resizing_SAR = \
!AnamorphicOutput ? 1.0 : IsOutputDAR ? F_RWO_Height * OutputDAR / F_RWO_Width : OutputSAR

F_RWO_Aspect_Width = F_RWO_Height * RWO_Resizing_DAR / RWO_Resizing_SAR
F_RWO_Aspect_Width = (abs(F_RWO_Aspect_Width - round(F_RWO_Aspect_Width)) < 0.000005) && \
(round(F_RWO_Aspect_Width) % WidthMod == 0) ? float(round(F_RWO_Aspect_Width)) : F_RWO_Aspect_Width

F_RWO_Width_Adjust = F_RWO_Aspect_Width - (floor(F_RWO_Aspect_Width / F_Mod) * Mod)
F_RWO_Width_Up = F_RWO_Aspect_Width + F_Mod - F_RWO_Width_Adjust
F_RWO_Width_Down = F_RWO_Aspect_Width - F_RWO_Width_Adjust

F_RWO_Adjusted_Width = (F_RWO_Width_Up > F_RWO_Width) ? F_RWO_Width_Down : \
((F_RWO_Width_Up - F_RWO_Aspect_Width) < (F_Mod / 2.0)) ? F_RWO_Width_Up : F_RWO_Width_Down
RWO_Adjusted_Width = round(F_RWO_Adjusted_Width)

F_RWO_Aspect_Width = \
IsOutputDAR ? (IsCropDAR ? F_RWO_Adjusted_Width : F_RWO_Aspect_Width) : F_RWO_Adjusted_Width

F_RWO_Resizing_Width = \
IsOutputDAR ? (IsCropDAR ? F_RWO_Adjusted_Width : F_RWO_Width) : F_RWO_Adjusted_Width
RWO_Resizing_Width = round(F_RWO_Resizing_Width)

# -------------------------------------------------------------------------------
#            Resize Width Only - Cropping
# -------------------------------------------------------------------------------

F_RWO_Extra_Width_Crop = \
!AnamorphicInput ? F_Cropped_Source_Width - (F_RWO_Aspect_Width * RWO_Resizing_SAR) : \
IsInputDAR ? F_Cropped_Source_Width - \
(F_RWO_Aspect_Width * RWO_Resizing_SAR * F_Source_Width / InputDAR / F_Source_Height) : \
F_Cropped_Source_Width - (F_RWO_Aspect_Width * RWO_Resizing_SAR / InputSAR)

F_RWO_Extra_Height_Crop = F_Cropped_Source_Height - F_RWO_Height - F_RWO_CT_Adjust - F_RWO_CB_Adjust

F_RWO_Extra_Crop_LR = max(0.0, F_RWO_Extra_Width_Crop / 2.0)
F_RWO_Extra_Crop_TB = CSplit1 ? 0.0 : max(0.0, F_RWO_Extra_Height_Crop / 2.0)

RWO_CL = round(max(0, CSplit1 ? CL + F_RWO_Extra_Crop_LR - fmod(CL + F_RWO_Extra_Crop_LR, WidthMod) : \
CSplit2 ? CL - fmod(CL, WidthMod) : 0))
RWO_CR = round(max(0, CSplit1 ? CR + F_RWO_Extra_Crop_LR - fmod(CR + F_RWO_Extra_Crop_LR, WidthMod) : \
CSplit2 ? CR - fmod(CR, WidthMod) : 0))

RWO_RCL = max(0, CSplit1 || CSplit2 ? CL + F_RWO_Extra_Crop_LR - RWO_CL : CL + F_RWO_Extra_Crop_LR)
RWO_RCR = max(0, CSplit1 || CSplit2 ? CR + F_RWO_Extra_Crop_LR - RWO_CR : CR + F_RWO_Extra_Crop_LR)

F_RWO_Extra_CT = CSplit1 ? (CroppedMoreTop ? \
((HeightMod == 2) ? floor(F_RWO_Extra_Height_Crop / 4.0) * 2 : floor(F_RWO_Extra_Height_Crop / 2.0)) : \
(HeightMod == 2) ? round(F_RWO_Extra_Height_Crop / 4.0) * 2 : round(F_RWO_Extra_Height_Crop / 2.0)) : 0.0

F_RWO_Extra_CB = CSplit1 ? F_RWO_Extra_Height_Crop - F_RWO_Extra_CT : 0.0

F_RWO_Extra_CT = F_RWO_Extra_CT + F_RWO_CT_Adjust
F_RWO_Extra_CB = F_RWO_Extra_CB + F_RWO_CB_Adjust

RWO_CT = round(max(0, CSplit1 ? CT + F_RWO_Extra_CT : CSplit2 ? CT - fmod(CT, HeightMod) : 0))
RWO_CB = round(max(0, CSplit1 ? CB + F_RWO_Extra_CB : CSplit2 ? CB - fmod(CB, HeightMod) : 0))

RWO_RCT = max(0, CSplit1 ? 0 : CSplit2 ? CT + F_RWO_Extra_Crop_TB - RWO_CT : CT + F_RWO_Extra_Crop_TB)
RWO_RCB = max(0, CSplit1 ? 0 : CSplit2 ? CB + F_RWO_Extra_Crop_TB - RWO_CB : CB + F_RWO_Extra_Crop_TB)

F_RWO_Cropped_Width = F_Cropped_Source_Width - F_RWO_Extra_Width_Crop
F_RWO_Cropped_Height = (CSplit == 1) ? F_Cropped_Source_Height - F_RWO_Extra_CT - F_RWO_Extra_CB : \
F_Cropped_Source_Height - F_RWO_Extra_Height_Crop

# -------------------------------------------------------------------------------
#            Resize Width Only - Cropped Aspect Ratios
# -------------------------------------------------------------------------------

RWO_Cropped_StorageAR = F_RWO_Cropped_Width / F_RWO_Cropped_Height
RWO_Cropped_DAR = !AnamorphicInput ? RWO_Cropped_StorageAR : IsInputDAR ? \
F_RWO_Cropped_Width * F_Source_Height * InputDAR / F_RWO_Cropped_Height / F_Source_Width : \
F_RWO_Cropped_Width * InputSAR / F_RWO_Cropped_Height

""", "Resize Width Only Eval") : Eval("""

# ===============================================================================
#            Full Resizing Defaults
# ===============================================================================

Cropped_Source_StorageAR = F_Cropped_Source_Width / F_Cropped_Source_Height
Cropped_Source_DAR = !AnamorphicInput ? Cropped_Source_StorageAR : IsInputDAR ? \
F_Cropped_Source_Width * F_Source_Height * InputDAR / F_Cropped_Source_Height / F_Source_Width : \
F_Cropped_Source_Width * InputSAR / F_Cropped_Source_Height

F_Resized_Cropped_Width = !AnamorphicInput ? F_Cropped_Source_Width : \
IsInputDAR ? F_Cropped_Source_Width * F_Source_Height * InputDAR / F_Source_Width : \
F_Cropped_Source_Width * InputSAR
Mod_Cropped_Resized_Width = floor(F_Resized_Cropped_Width / F_Mod) * Mod
F_Mod_Cropped_Resized_Width = float(Mod_Cropped_Resized_Width)

Mod_Cropped_Width = floor(F_Cropped_Source_Width / F_Mod) * Mod
F_Mod_Cropped_Width = float(Mod_Cropped_Width)
Mod_Cropped_Height = floor(F_Cropped_Source_Height / F_HMod) * HMod
F_Mod_Cropped_Height = float(Mod_Cropped_Height)

UseWidth = UseWidth && ((Mod_Cropped_Width - F_Cropped_Source_Width) == 0)
UseHeight = UseHeight && ((Mod_Cropped_Height - F_Cropped_Source_Height) == 0)
TryWidth = TryWidth && ((Outwidth - F_Cropped_Source_Width) == 0)
TryHeight = TryHeight && ((OutHeight - F_Cropped_Source_Height) == 0)
KeepWidth = UseWidth || TryWidth
KeepHeight = UseHeight || TryHeight

OutWidth = UseWidth ? Mod_Cropped_Width : OutWidth
OutHeight = UseHeight ? Mod_Cropped_Height : OutHeight

Resizing_Width = (OutWidth > 0) ? OutWidth : Mod_Cropped_Resized_Width
F_Resizing_Width = (OutWidth > 0) ? float(OutWidth) : F_Mod_Cropped_Resized_Width
Resizing_Height = (OutHeight > 0) ? OutHeight : Mod_Cropped_Height
F_Resizing_Height = (OutHeight > 0) ? float(OutHeight) : F_Mod_Cropped_Height

Resizing_StorageAR = F_Resizing_Width / F_Resizing_Height
Resizing_DAR = IsOutputDAR && IsBordered && !IsCropDAR ? Cropped_Source_DAR : \
IsCropDAR ? CropDAR : IsOutputDAR ? OutputDAR : Cropped_Source_DAR
Resizing_SAR = \
!AnamorphicOutput ? 1.0 : IsOutputDAR ? F_Resizing_Height * OutputDAR / F_Resizing_Width : OutputSAR

# ===============================================================================
#            AutoDAR
# ===============================================================================

IsAutoDAR = AutoDAR && !IsCropDAR && !IsOutputDAR && \
!KeepWidth && !KeepHeight && (IsBordered || !(OutWidth > 0 < OutHeight))

IsAutoDAR ? Eval("

IsAspect169 = ((!IsBordered ? 1.739 : 16.0 / 9.0) < Resizing_DAR < 1.829)
IsAspect43 = (1.27 < Resizing_DAR < (!IsBordered ? 1.35 : 4.0 / 3.0))
H169 = F_Resizing_Width * (IsOutputSAR ? OutputSAR : 1.0) * 9.0 / 16.0
H43 = F_Resizing_Width * (IsOutputSAR ? OutputSAR : 1.0) * 3.0 / 4.0
W169 = F_Resizing_Height * 16.0 / 9.0 / (IsOutputSAR ? OutputSAR : 1.0)
W43 = F_Resizing_Height * 4.0 / 3.0 / (IsOutputSAR ? OutputSAR : 1.0)
IsHeight169 = IsAspect169 && (abs(round(H169) - H169) < 0.000005) && (round(H169) % HMod == 0)
IsHeight43 = IsAspect43 && (abs(round(H43) - H43) < 0.000005) && (round(H43) % HMod == 0)
IsWidth169 = IsAspect169 && (abs(round(W169) - W169) < 0.000005) && (round(W169) % Mod == 0)
IsWidth43 = IsAspect43 && (abs(round(W43) - W43) < 0.000005) && (round(W43) % Mod == 0)

") : Eval("  IsHeight169=false   IsHeight43=false   IsWidth169=false   IsWidth43=false  ")

# ===============================================================================
#            Full Resizing (A) Height Calculated From Width
# ===============================================================================

FullResizeA = IsBordered ? (Resizing_DAR >= (Resizing_StorageAR * Resizing_SAR)) : \
(OutWidth > 0) ? true : (OutHeight > 0) ? false : \
IsHeight169 || IsHeight43 ? true : IsWidth169 || IsWidth43 ? false : true

FullResizeA ? Eval("

F_FRA_Aspect_Height = F_Resizing_Width * Resizing_SAR / Resizing_DAR
F_FRA_Aspect_Height = (abs(F_FRA_Aspect_Height - round(F_FRA_Aspect_Height)) < 0.000005) && \
(round(F_FRA_Aspect_Height) % HeightMod == 0) ? float(round(F_FRA_Aspect_Height)) : F_FRA_Aspect_Height

F_FRA_Height_Adjust = F_FRA_Aspect_Height - (floor(F_FRA_Aspect_Height / F_HMod) * HMod)
F_FRA_Height_Up = F_FRA_Aspect_Height + F_HMod - F_FRA_Height_Adjust
F_FRA_Height_Down = F_FRA_Aspect_Height - F_FRA_Height_Adjust

F_FRA_Adjusted_Height1 = KeepWidth ? F_FRA_Height_Down : \
((F_FRA_Height_Up - F_FRA_Aspect_Height) < (F_HMod / 2.0)) ? F_FRA_Height_Up : F_FRA_Height_Down

F_FRA_Adjusted_Height2 = IsHeight169 ? round(H169) : IsHeight43 ? round(H43) : F_FRA_Adjusted_Height1

F_FRA_Aspect_Height = IsOutputDAR && IsBordered && !IsCropDAR ? \
((OutHeight == 0) ? F_Resizing_Height : F_FRA_Adjusted_Height1) : \
IsOutputDAR ? (IsCropDAR ? F_FRA_Adjusted_Height1 : F_FRA_Aspect_Height) : \
IsBordered || (OutHeight == 0) ? F_FRA_Adjusted_Height2 : F_Resizing_Height

F_FRA_Resizing_Height = IsOutputDAR && IsBordered && !IsCropDAR ? F_FRA_Aspect_Height : \
IsOutputDAR ? (IsCropDAR ? F_FRA_Adjusted_Height1 : F_Resizing_Height) : F_FRA_Aspect_Height
FRA_Resizing_Height = round(F_FRA_Resizing_Height)

FRA_Cropping_DAR = F_Resizing_Width * Resizing_SAR / F_FRA_Aspect_Height

F_FRA_Extra_Width_Crop = (abs(FRA_Cropping_DAR - Cropped_Source_DAR) < 0.000005) || \
(FRA_Cropping_DAR > Cropped_Source_DAR) ? 0.0 : \
!AnamorphicInput ? F_Cropped_Source_Width - \
(F_Cropped_Source_Height * F_Resizing_Width * Resizing_SAR / F_FRA_Aspect_Height) : \
IsInputDAR ? F_Cropped_Source_Width - \
(F_Cropped_Source_Height * F_Resizing_Width * \
F_Source_Width * Resizing_SAR / InputDAR / F_FRA_Aspect_Height / F_Source_Height) : \
F_Cropped_Source_Width - \
(F_Cropped_Source_Height * F_Resizing_Width * Resizing_SAR / F_FRA_Aspect_Height / InputSAR)

F_FRA_Extra_Height_Crop = (abs(FRA_Cropping_DAR - Cropped_Source_DAR) < 0.000005) || \
(FRA_Cropping_DAR < Cropped_Source_DAR) ? 0.0 : \
!AnamorphicInput ? F_Cropped_Source_Height - \
(F_Cropped_Source_Width * F_FRA_Aspect_Height / F_Resizing_Width / Resizing_SAR) : \
IsInputDAR ? F_Cropped_Source_Height - \
(F_Cropped_Source_Width * F_FRA_Aspect_Height * \
F_Source_Height * InputDAR / F_Resizing_Width / Resizing_SAR / F_Source_Width) : \
F_Cropped_Source_Height - \
(F_Cropped_Source_Width * F_FRA_Aspect_Height * InputSAR / F_Resizing_Width / Resizing_SAR)

# -------------------------------------------------------------------------------
#            Full Resizing (A) Cropping
# -------------------------------------------------------------------------------

F_FRA_Extra_Crop_LR = max(0.0, F_FRA_Extra_Width_Crop / 2.0)
F_FRA_Extra_Crop_TB = max(0.0, F_FRA_Extra_Height_Crop / 2.0)

FRA_CL = \
round(max(0, CSplit1 ? CL + F_FRA_Extra_Crop_LR - fmod(CL + F_FRA_Extra_Crop_LR, WidthMod) : \
CSplit2 ? CL - fmod(CL, WidthMod) : 0))
FRA_CT = \
round(max(0, CSplit1 ? CT + F_FRA_Extra_Crop_TB - fmod(CT + F_FRA_Extra_Crop_TB, HeightMod) : \
CSplit2 ? CT - fmod(CT, HeightMod) : 0))
FRA_CR = \
round(max(0, CSplit1 ? CR + F_FRA_Extra_Crop_LR - fmod(CR + F_FRA_Extra_Crop_LR, WidthMod) : \
CSplit2 ? CR - fmod(CR, WidthMod) : 0))
FRA_CB = \
round(max(0, CSplit1 ? CB + F_FRA_Extra_Crop_TB - fmod(CB + F_FRA_Extra_Crop_TB, HeightMod) : \
CSplit2 ? CB - fmod(CB, HeightMod) : 0))

FRA_RCL = max(0, CSplit1 || CSplit2 ? CL + F_FRA_Extra_Crop_LR - FRA_CL : CL + F_FRA_Extra_Crop_LR)
FRA_RCT = max(0, CSplit1 || CSplit2 ? CT + F_FRA_Extra_Crop_TB - FRA_CT : CT + F_FRA_Extra_Crop_TB)
FRA_RCR = max(0, CSplit1 || CSplit2 ? CR + F_FRA_Extra_Crop_LR - FRA_CR : CR + F_FRA_Extra_Crop_LR)
FRA_RCB = max(0, CSplit1 || CSplit2 ? CB + F_FRA_Extra_Crop_TB - FRA_CB : CB + F_FRA_Extra_Crop_TB)

F_FRA_Cropped_Width = F_Cropped_Source_Width - F_FRA_Extra_Width_Crop
F_FRA_Cropped_Height = F_Cropped_Source_Height - F_FRA_Extra_Height_Crop

# -------------------------------------------------------------------------------
#            Full Resizing (A) Cropped Aspect Ratios
# -------------------------------------------------------------------------------

FRA_Cropped_StorageAR = F_FRA_Cropped_Width / F_FRA_Cropped_Height
FRA_Cropped_DAR = !AnamorphicInput ? FRA_Cropped_StorageAR : IsInputDAR ? \
F_FRA_Cropped_Width * F_Source_Height * InputDAR / F_FRA_Cropped_Height / F_Source_Width : \
F_FRA_Cropped_Width * InputSAR / F_FRA_Cropped_Height

", "FullResizeA Eval") : Eval("

# ===============================================================================
#            Full Resizing (B) Width Calculated From Height
# ===============================================================================

F_FRB_Aspect_Width = F_Resizing_Height * Resizing_DAR / Resizing_SAR
F_FRB_Aspect_Width = (abs(F_FRB_Aspect_Width - round(F_FRB_Aspect_Width)) < 0.000005) && \
(round(F_FRB_Aspect_Width) % WidthMod == 0) ? float(round(F_FRB_Aspect_Width)) : F_FRB_Aspect_Width

F_FRB_Width_Adjust = F_FRB_Aspect_Width - (floor(F_FRB_Aspect_Width / F_Mod) * Mod)
F_FRB_Width_Up = F_FRB_Aspect_Width + Mod - F_FRB_Width_Adjust
F_FRB_Width_Down = F_FRB_Aspect_Width - F_FRB_Width_Adjust

F_FRB_Adjusted_Width1 = KeepHeight ? F_FRB_Width_Down : \
((F_FRB_Width_Up - F_FRB_Aspect_Width) < (F_Mod / 2.0)) ? F_FRB_Width_Up : F_FRB_Width_Down

F_FRB_Adjusted_Width2 = IsWidth169 ? round(W169) : IsWidth43 ? round(W43) : F_FRB_Adjusted_Width1

F_FRB_Aspect_Width = IsOutputDAR && IsBordered && !IsCropDAR ? \
((OutWidth == 0) ? F_Resizing_Width : F_FRB_Adjusted_Width1) : \
IsOutputDAR ? (IsCropDAR ? F_FRB_Adjusted_Width1 : F_FRB_Aspect_Width) : \
IsBordered || (OutWidth == 0) ? F_FRB_Adjusted_Width2 : F_Resizing_Width

F_FRB_Resizing_Width = IsOutputDAR && IsBordered && !IsCropDAR ? F_FRB_Aspect_Width : \
IsOutputDAR ? (IsCropDAR ? F_FRB_Adjusted_Width1 : F_Resizing_Width) : F_FRB_Aspect_Width
FRB_Resizing_Width = round(F_FRB_Resizing_Width)

FRB_Cropping_DAR = F_FRB_Aspect_Width * Resizing_SAR / F_Resizing_Height

F_FRB_Extra_Width_Crop = (abs(FRB_Cropping_DAR - Cropped_Source_DAR) < 0.000005) || \
(FRB_Cropping_DAR > Cropped_Source_DAR) ? 0.0 : \
!AnamorphicInput ? F_Cropped_Source_Width - \
(F_Cropped_Source_Height * F_FRB_Aspect_Width * Resizing_SAR / F_Resizing_Height) : \
IsInputDAR ? F_Cropped_Source_Width - \
(F_Cropped_Source_Height * F_FRB_Aspect_Width * \
Resizing_SAR * F_Source_Width / InputDAR / F_Resizing_Height / F_Source_Height) : \
F_Cropped_Source_Width - \
(F_Cropped_Source_Height * F_FRB_Aspect_Width * Resizing_SAR / F_Resizing_Height / InputSAR)

F_FRB_Extra_Height_Crop = (abs(FRB_Cropping_DAR - Cropped_Source_DAR) < 0.000005) || \
(FRB_Cropping_DAR < Cropped_Source_DAR) ? 0.0 : \
!AnamorphicInput ? F_Cropped_Source_Height - \
(F_Cropped_Source_Width * F_Resizing_Height / F_FRB_Aspect_Width / Resizing_SAR) : \
IsInputDAR ? F_Cropped_Source_Height - \
(F_Cropped_Source_Width * F_Resizing_Height * \
F_Source_Height * InputDAR / F_FRB_Aspect_Width / Resizing_SAR / F_Source_Width) : \
F_Cropped_Source_Height - \
(F_Cropped_Source_Width * F_Resizing_Height * InputSAR / F_FRB_Aspect_Width / Resizing_SAR)

# -------------------------------------------------------------------------------
#            Full Resizing (B) Cropping
# -------------------------------------------------------------------------------

F_FRB_Extra_Crop_LR = max(0.0, F_FRB_Extra_Width_Crop / 2.0)
F_FRB_Extra_Crop_TB = max(0.0, F_FRB_Extra_Height_Crop / 2.0)

FRB_CL = \
round(max(0, CSplit1 ? CL + F_FRB_Extra_Crop_LR - fmod(CL + F_FRB_Extra_Crop_LR, WidthMod) : \
CSplit2 ? CL - fmod(CL, WidthMod) : 0))
FRB_CT = \
round(max(0, CSplit1 ? CT + F_FRB_Extra_Crop_TB - fmod(CT + F_FRB_Extra_Crop_TB, HeightMod) : \
CSplit2 ? CT - fmod(CT, HeightMod) : 0))
FRB_CR = \
round(max(0, CSplit1 ? CR + F_FRB_Extra_Crop_LR - fmod(CR + F_FRB_Extra_Crop_LR, WidthMod) : \
CSplit2 ? CR - fmod(CR, WidthMod) : 0))
FRB_CB = \
round(max(0, CSplit1 ? CB + F_FRB_Extra_Crop_TB - fmod(CB + F_FRB_Extra_Crop_TB, HeightMod) : \
CSplit2 ? CB - fmod(CB, HeightMod) : 0))

FRB_RCL = max(0, CSplit1 || CSplit2 ? CL + F_FRB_Extra_Crop_LR - FRB_CL : CL + F_FRB_Extra_Crop_LR)
FRB_RCT = max(0, CSplit1 || CSplit2 ? CT + F_FRB_Extra_Crop_TB - FRB_CT : CT + F_FRB_Extra_Crop_TB)
FRB_RCR = max(0, CSplit1 || CSplit2 ? CR + F_FRB_Extra_Crop_LR - FRB_CR : CR + F_FRB_Extra_Crop_LR)
FRB_RCB = max(0, CSplit1 || CSplit2 ? CB + F_FRB_Extra_Crop_TB - FRB_CB : CB + F_FRB_Extra_Crop_TB)

F_FRB_Cropped_Width = F_Cropped_Source_Width - F_FRB_Extra_Width_Crop
F_FRB_Cropped_Height = F_Cropped_Source_Height - F_FRB_Extra_Height_Crop

# -------------------------------------------------------------------------------
#            Full Resizing (B) Cropped Aspect Ratios
# -------------------------------------------------------------------------------

FRB_Cropped_StorageAR = F_FRB_Cropped_Width / F_FRB_Cropped_Height
FRB_Cropped_DAR = !AnamorphicInput ? FRB_Cropped_StorageAR : IsInputDAR ? \
F_FRB_Cropped_Width * F_Source_Height * InputDAR / F_FRB_Cropped_Height / F_Source_Width : \
F_FRB_Cropped_Width * InputSAR / F_FRB_Cropped_Height

", "FullResizeB Eval")   """)

# ===============================================================================
#            Output Width, Height & Aspect Ratios
# ===============================================================================

CRVideo_Width = \
NoResizing ? NR_Width : ResizeWO ? RWO_Resizing_Width : FullResizeA ? Resizing_Width : FRB_Resizing_Width
F_CRVideo_Width = float(CRVideo_Width)

CRVideo_Height = \
NoResizing ? NR_Height : ResizeWO ? RWO_Height : FullResizeA ? FRA_Resizing_Height : Resizing_Height
F_CRVideo_Height = float(CRVideo_Height)

CRVideo_StorageAR = F_CRVideo_Width / F_CRVideo_Height

CRVideo_DAR = NoResizing ? (!AnamorphicInput ? CRVideo_StorageAR : \
IsInputDAR ? F_CRVideo_Width * F_Source_Height * InputDAR / F_CRVideo_Height / F_Source_Width : \
F_CRVideo_Width * Source_SAR / F_CRVideo_Height) : \
!AnamorphicOutput ? CRVideo_StorageAR : \
ResizeWO ? F_CRVideo_Width * RWO_Resizing_SAR / F_CRVideo_Height : \
F_CRVideo_Width * Resizing_SAR / F_CRVideo_Height

Final_Width = CleanBorders ? Source_Width : \
!IsBordered ? CRVideo_Width : \
ResizeWO && IsOutputDAR ? RWO_Width : \
(NoResizing || ResizeWO) && (OutWidth > CRVideo_Width) ? OutWidth : \
FullResizing && (Resizing_StorageAR > CRVideo_StorageAR) ? \
((OutWidth == 0) ? Resizing_Width : \
(OutWidth > CRVideo_Width) ? OutWidth : CRVideo_Width) : CRVideo_Width
F_Final_Width = float(Final_Width)

Final_Height = CleanBorders ? Source_Height : \
!IsBordered ? CRVideo_Height : \
ResizeWO && IsOutputDAR ? CRVideo_Height : \
(NoResizing || ResizeWO) && (OutHeight > CRVideo_Height) ? OutHeight : \
FullResizing && (Resizing_StorageAR < CRVideo_StorageAR) ? \
((OutHeight == 0) ? Resizing_Height : \
(OutHeight > CRVideo_Height) ? OutHeight : CRVideo_Height) : CRVideo_Height
F_Final_Height = float(Final_Height)

Final_StorageAR = F_Final_Width / F_Final_Height

Final_DAR = NoResizing ? (!AnamorphicInput ? Final_StorageAR : \
IsInputDAR ? F_Final_Width * F_Source_Height * InputDAR / F_Final_Height / F_Source_Width : \
F_Final_Width * Source_SAR / F_Final_Height) : \
!AnamorphicOutput ? Final_StorageAR : \
ResizeWO ? F_Final_Width * RWO_Resizing_SAR / F_Final_Height : \
F_Final_Width * Resizing_SAR / F_Final_Height

IsAnamorphicInput = AnamorphicInput && (abs(Source_DAR - Source_StorageAR) > 0.000005)
IsAnamorphicOutput = AnamorphicOutput && (abs(Final_DAR - Final_StorageAR) > 0.000005)

F_Final_Display_Width = !IsAnamorphicOutput ? F_Final_Width : NoResizing ? F_Final_Width * Source_SAR : \
ResizeWO ? F_Final_Width * RWO_Resizing_SAR : F_Final_Width * Resizing_SAR

Final_SAR = !IsAnamorphicOutput ? 1.0 : NoResizing ? Source_SAR : ResizeWO ? RWO_Resizing_SAR : Resizing_SAR

# ===============================================================================
#            Configure Color Conversion
# ===============================================================================

(CC == "") && (CM == "") ? nop() : Eval("""

Assert((CC == "") || (CC == "AVSResize") || (CC == "ColorMatrix") || (CC == "DitherTools") || \
(CC == "FMTConv") || (CC == "HDRMatrix") || (CC == "HDRTools"), C_R + \
" ColorConvert=" + chr(34) + CC + chr(34) + " is not a supported type " + chr(10))

NoFunc = "there is no function named"
IsColorMatrix = IsAvsHBD ? FunctionExists("ColorMatrix") : \
Eval(" Try{ ColorMatrix(xx=0) }Catch(CMatrix_err){ (FindStr(LCase(CMatrix_err), NoFunc) == 0) } ")
IsDitherTools = IsAvsHBD ? FunctionExists("Dither_convert_yuv_to_rgb") : \
Eval(" Try{ Dither_convert_yuv_to_rgb(xx=0) }Catch(DTools_err){ (FindStr(LCase(DTools_err), NoFunc) == 0) } ")
IsHDRMatrix = IsAvsHBD ? FunctionExists("Matrix") : \
Eval(" Try{ Matrix(xx=0) }Catch(HMatrix_err){ (FindStr(LCase(HMatrix_err), NoFunc) == 0) } ")
IsStacks = IsAvsHBD && FunctionExists("ConvertToDoubleWidth")

IsSDSource = (F_Source_Display_Width <= 1056) && (Source_Height < 600)
IsHDSource = ((F_Source_Display_Width > 1056) || (Source_Height >= 600)) && \
((F_Source_Display_Width <= 1920) && (Source_Height <= 1080))
IsUHDSource = (F_Source_Display_Width > 1920) || (Source_Height > 1080)

IsSDOut = (F_Final_Display_Width <= 1056) && (Final_Height < 600)
IsHDOut = ((F_Final_Display_Width > 1056) || (Final_Height >= 600)) && \
((F_Final_Display_Width <= 1920) && (Final_Height <= 1080))
IsUHDOut = (F_Final_Display_Width > 1920) || (Final_Height > 1080)

CM = (CM != "") ? CM : \
!FullResizing || IsRGBSource || (IsAvsHBD && IsY(Source)) || IsY8(Source) ? "" : \
IsSDSource && IsHDOut ? "601-709" : \
IsSDSource && IsUHDOut ? "601-2020" : \
IsHDSource && IsSDOut ? "709-601" : \
IsHDSource && IsUHDOut ? "709-2020" : \
IsUHDSource && IsSDOut ? "2020-601" : \
IsUHDSource && IsHDOut ? "2020-709" : ""

Is601NP = (CM == "601N-601P") || (CM == "601P-601N")
Is601709 = (CM == "709-601") || (CM == "601-709")
Is601709_8 = Is601709 && (Source_Bits == 8)
Is601709NP = (CM == "709-601N") || (CM == "601N-709") || (CM == "709-601P") || (CM == "601P-709")
Is2020 = (CM == "2020-709") || (CM == "709-2020") || (CM == "2020-601") || (CM == "601-2020")
Is2020NP = (CM == "2020-601N") || (CM == "601N-2020") || (CM == "2020-601P") || (CM == "601P-2020")

Assert((CM == "") || Is601NP || Is601709 || Is601709NP || Is2020 || Is2020NP, C_R + \
" ColorMode=" + chr(34) + CM + chr(34) + " is not a supported color mode " + chr(10))

CC = (CC != "") ? CC : \
IsZResize && !Is411Source ? "AVSResize" : \
IsFResize && !Is411Source ? "FMTConv" : \
IsDResize && Is601709_8 ? "DitherTools" : \
IsHDRMatrix && ((Is601709 && IsStacks) || Is601709_8) && !Is411Source ? "HDRMatrix" : \
IsAvsHBD && FunctionExists("z_ConvertFormat") && !Is411Source ? "AVSResize" : \
IsAvsHBD && FunctionExists("fmtc_resample") && !Is411Source ? "FMTConv" : \
IsDitherTools && Is601709_8 ? "DitherTools" : \
IsColorMatrix && Is601709_8 && !Is411Source && !IsYV24(Source) ? "ColorMatrix" : \
IsAvsHBD && FunctionExists("ConvertYUVtoXYZ") && !Is411Source ? "HDRTools" : "none"

Assert((CC != "none"), \
C_R + " Unable to find a loaded plugin suitable for " + chr(34) + CM + chr(34) + " color conversion " + chr(10))

IsZResize = IsZResize || ((CC == "AVSResize") && (CM != "") && IsZKernel && !IsDResize && !IsFResize && !IsRStr)
IsDResize = IsDResize || ((CC == "DitherTools") && (CM != "") && IsDKernel && !IsZResize && !IsFResize && !IsRStr)
IsFResize = IsFResize || ((CC == "FMTConv") && (CM != "") && IsFKernel && !IsZResize && !IsDResize && !IsRStr)

Kernel = (Original_K == "Lanczos3") || \
((IsZResize || IsDResize || IsFResize) && (Original_K == "Lanczos4")) ? "Lanczos" : Original_K

Resizer = IsZResize || IsDResize || IsFResize ? Kernel : Resizer

RTaps = !IsZResize && !IsDResize && !IsFResize ? "" : \
(Original_K == "Lanczos3") ? (IsZResize ? ", filter_param_a=3" : ", Taps=3") : \
(Original_K == "Lanczos4") ? (IsZResize ? ", filter_param_a=4" : ", Taps=4") : ""

CC = (CC == "AVSResize") ? "AVSResize" : (CC == "ColorMatrix") ? "ColorMatrix" : \
(CC == "DitherTools") ? "DitherTools" : (CC == "FMTConv") ? "FMTConv" : \
(CC == "HDRMatrix") ? "HDRMatrix" : (CC == "HDRTools") ? "HDRTools" : ""

""", "ColorConvert Eval")

IsColorConversion = (CC != "") && (CM != "")

# ===============================================================================
#            Apply Cropping
# ===============================================================================

CCL = NoResizing ? NR_CL : ResizeWO ? RWO_CL : FullResizeA ? FRA_CL : FRB_CL
CCT = NoResizing ? NR_CT : ResizeWO ? RWO_CT : FullResizeA ? FRA_CT : FRB_CT
CCR = NoResizing ? NR_CR : ResizeWO ? RWO_CR : FullResizeA ? FRA_CR : FRB_CR
CCB = NoResizing ? NR_CB : ResizeWO ? RWO_CB : FullResizeA ? FRA_CB : FRB_CB

RCL = NoResizing ? NR_RCL : ResizeWO ? RWO_RCL : FullResizeA ? FRA_RCL : FRB_RCL
RCT = NoResizing ? NR_RCT : ResizeWO ? RWO_RCT : FullResizeA ? FRA_RCT : FRB_RCT
RCR = NoResizing ? NR_RCR : ResizeWO ? RWO_RCR : FullResizeA ? FRA_RCR : FRB_RCR
RCB = NoResizing ? NR_RCB : ResizeWO ? RWO_RCB : FullResizeA ? FRA_RCB : FRB_RCB

F_Cropped_Width = F_Auto_Cropped_Width - CCL - CCR - RCL - RCR
F_Cropped_Height = F_Auto_Cropped_Height - CCT - CCB - RCT - RCB

Cropped = (0 == CCL == CCT == CCR == CCB) ? AutoCroppedSource : \
AutoCroppedSource.crop(CCL, CCT, -CCR, -CCB)

NoResizer = NoResizing && (CSplit1 || (0 == NR_RCL == NR_RCR == NR_RCT == NR_RCB))
Upscale = !NoResizing && ((F_CRVideo_Width * F_CRVideo_Height / F_Cropped_Width / F_Cropped_Height) > 1)

# ===============================================================================
#            Configure RingRepair
# ===============================================================================

IsRRepair = IsRingRepair && !NoResizing && !IsRGBSource

Res_Multi = !IsRRepair ? nop() : Upscale ? \
F_CRVideo_Width * F_CRVideo_Height / F_Cropped_Height / F_Cropped_Width : \
F_Cropped_Width * F_Cropped_Height / F_CRVideo_Height / F_CRVideo_Width

Res_Multi = !IsRRepair ? nop() : min(max(Res_Multi, 1), 2.25)
Weight = !IsRRepair ? 0 : IsFloat(RingRepair) ? RingRepair : Spline(Res_Multi, 1, 0, 2.25, 1, 3.5, 0, true)
Weight = !IsRRepair ? 0 : min(max(Weight, 0), 1)

IsRingRepair = IsRRepair && (Weight > 0) && !IsDResize
IsDRingRepair = IsRRepair && (Weight > 0) && IsDResize
IsRRepair = IsRingRepair || IsDRingRepair

# ===============================================================================
#            Resize / Color Convert
# ===============================================================================

DoResize = IsZResize ? ".z_ConvertFormat(CRVideo_Width, CRVideo_Height, \
src_left=RCL, src_top=RCT, src_width=-RCR, src_height=-RCB, resample_filter=Resizer" + RTaps + ResStr + ")" : \
IsDResize ? ".Dither_convert_8_to_16().Dither_resize16" + (!IsDRingRepair ? "" : "nr") + \
"(CRVideo_Width, CRVideo_Height, src_left=RCL, src_top=RCT, src_width=-RCR, src_height=-RCB, \
kernel=Resizer" + RTaps + ResStr + ").DitherPost(mode=6, slice=false)" : \
IsFResize ? ".fmtc_resample(CRVideo_Width, CRVideo_Height, \
sx=RCL, sy=RCT, sw=-RCR, sh=-RCB, kernel=Resizer" + RTaps + ResStr + ")" : \
"." + Resizer + "(CRVideo_Width, CRVideo_Height, \
src_left=RCL, src_top=RCT, src_width=-RCR, src_height=-RCB" + ResStr + ")"

!IsColorConversion ? Eval("""

Cropped = NoResizer ? Cropped : \
IsYUY2(Source) && (IsRingRepair || IsZResize || IsDResize || IsFResize) ? Cropped.ConvertToYV16() : \
IsAvsHBD && IsPackedRGB(Source) && (IsZResize || IsFResize) ? \
(HasAlpha(Source) ? Cropped.ConvertToPlanarRGBA() : Cropped.ConvertToPlanarRGB()) : Cropped

Cropped = NoResizer || !IsRingRepair || !IsFResize || (Source_Bits == 16) ? Cropped : \
Cropped.fmtc_bitdepth(bits=16)

DoRingRepair = ".CR_RingRepair(Cropped, CRVideo_Width, CRVideo_Height, \
RCL, RCT, RCR, RCB, IsResampleMT, IsZResize, IsFResize, Weight)"

CRVideo = NoResizer ? Cropped : \
IsRingRepair && (!IsFResize || (Source_Bits == 16)) ? Eval("Cropped" + DoResize + DoRingRepair) : \
IsRingRepair ? Eval("Cropped" + DoResize + DoRingRepair + ".fmtc_bitdepth(bits=Source_Bits)") : \
!IsFResize || (Source_Bits == 16) ? Eval("Cropped" + DoResize) : \
Eval("Cropped" + ".fmtc_bitdepth(bits=16)" + DoResize + ".fmtc_bitdepth(bits=Source_Bits)")

CRVideo = NoResizer ? CRVideo : \
IsYUY2(Source) && (IsRingRepair || IsZResize || IsDResize || IsFResize) ? CRVideo.ConvertToYUY2() : \
IsAvsHBD && IsPackedRGB(Source) && (IsZResize || IsFResize) ? \
(IsRGB64(Source) ? CRVideo.ConvertToRGB64() : IsRGB48(Source) ? CRVideo.ConvertToRGB48() : \
IsRGB32(Source) ? CRVideo.ConvertToRGB32() : CRVideo.ConvertToRGB24()) : CRVideo

""", "Resize Eval") : Eval("""

CRVideo = CR_ColorConvertResize(Cropped, CRVideo_Width, CRVideo_Height, RCL, RCT, RCR, RCB, \
CC, CM, Is601NP, Is601709, Is601709NP, Is2020, Is2020NP, Color_Format, IsRGBSource, IsAvsHBD, \
Source_Bits, NoResizer, Resizer, ResStr, RTaps, DoResize, Upscale, IsRingRepair, IsDRingRepair, Weight, \
IsStacks, IsResampleMT, IsZResize, IsDResize, IsFResize, C_R)

""", "Resize & Color Convert Eval")

# ===============================================================================
#            Add Borders As Required
# ===============================================================================

IsBordered && ((Final_Width > CRVideo_Width) || (Final_Height > CRVideo_Height)) ? Eval("""

Additional_Width = Final_Width - CRVideo_Width
EvenWidthSplit = Additional_Width % (WidthMod * 2) == 0
F_WidthSplit = float(Additional_Width) / 2.0
BL_Adjust = F_WidthSplit - (floor(F_WidthSplit / F_WidthMod) * WidthMod)
BL_Adjust_Up = min(Additional_Width, round(F_WidthSplit + WidthMod - BL_Adjust))
BL_Adjust_Down = round(F_WidthSplit - BL_Adjust)
BL = EvenWidthSplit ? Additional_Width / 2 : CroppedMoreLeft ? BL_Adjust_Up : BL_Adjust_Down
BR = Additional_Width - BL

Additional_Height = Final_Height - CRVideo_Height
EvenHeightSplit = Additional_Height % (HeightMod * 2) == 0
F_HeightSplit = float(Additional_Height) / 2.0
BT_Adjust = F_HeightSplit - (floor(F_HeightSplit / F_HeightMod) * HeightMod)
BT_Adjust_Up = min(Additional_Height, round(F_HeightSplit + HeightMod - BT_Adjust))
BT_Adjust_Down = round(F_HeightSplit - BT_Adjust)
BT = EvenHeightSplit ? Additional_Height / 2 : CroppedMoreTop ? BT_Adjust_Up : BT_Adjust_Down
BB = Additional_Height - BT

FBL = (Final_StorageAR >= CRVideo_StorageAR) ? BL : 0
FBT = (Final_StorageAR >= CRVideo_StorageAR) ? 0 : BT
FBR = (Final_StorageAR >= CRVideo_StorageAR) ? BR : 0
FBB = (Final_StorageAR >= CRVideo_StorageAR) ? 0 : BB

Frosty = Frosty && (((Final_StorageAR >= CRVideo_StorageAR) && (BL > WidthMod < BR)) || \
((Final_StorageAR < CRVideo_StorageAR) && (BT > HeightMod < BB)))

FrostyStr = !Frosty ? "" : "CRVideo.CR_FrostyBorders(CRVideo_Width, CRVideo_Height, \
F_CRVideo_Width, F_CRVideo_Height, FBL, FBT, FBR, FBB, WidthMod, HeightMod, \
F_WidthMod, F_HeightMod, CRVideo_StorageAR, Final_StorageAR, IsResampleMT)"

CRVideo = !Frosty ? CRVideo.AddBorders(BL, BT, BR, BB, color=BColor) : \
((Final_StorageAR >= CRVideo_StorageAR) && (0 == BT == BB)) || \
((Final_StorageAR < CRVideo_StorageAR) && (0 == BL == BR)) ? Eval(FrostyStr) : \
(Final_StorageAR >= CRVideo_StorageAR) ? Eval(FrostyStr + ".AddBorders(0, BT, 0, BB, color=BColor)") : \
Eval(FrostyStr + ".AddBorders(BL, 0, BR, 0, color=BColor)")

""", "Add Borders Eval") : Eval("  BL=0   BT=0   BR=0   BB=0  ")

# ===============================================================================
#            Global Aspect Ratios & Resolutions
# ===============================================================================

CR_MeGUI_Support() ? Eval("""

MInDARX = IsMInDAR ? MeGUI_darx : 0
MInDARY = IsMInDAR ? MeGUI_dary : 0
global gMPassDAR = IsMInDAR ? MInDAR : IsMPassThru ? MPassDAR : 0.0
global gMPassDARX = IsMInDAR ? MeGUI_darx : IsMPassThru ? gMPassDARX : 0
global gMPassDARY = IsMInDAR ? MeGUI_dary : IsMPassThru ? gMPassDARY : 0

global MeGUI_darx = !IsAnamorphicOutput ? Undefined() : \
NoResizing && (Source_Width == Final_Width) && (Source_Height == Final_Height) ? \
(IsMInDAR ? MeGUI_darx : IsMPassThru ? gMPassDARX : ContinuedNumerator(Final_DAR, limit=10000)) : \
ContinuedNumerator(Final_DAR, limit=10000)

global MeGUI_dary = !IsAnamorphicOutput ? Undefined() : \
NoResizing && (Source_Width == Final_Width) && (Source_Height == Final_Height) ? \
(IsMInDAR ? MeGUI_dary : IsMPassThru ? gMPassDARY : ContinuedDenominator(Final_DAR, limit=10000)) : \
ContinuedDenominator(Final_DAR, limit=10000)

""", "Global Aspect Ratios 1 Eval") : nop()

# ===============================================================================
#            RMode
# ===============================================================================

Try{  F_gPriorWidth=float(gPriorWidth)   F_gPriorHeight=float(gPriorHeight)   IsPriorWidthHeight=true  }
Catch(err){  IsPriorWidthHeight=false  }

RMode ? Eval("""

global gPriorWidth = IsAutoCPreview || IsGAutoCPreview ? Source_Width : \
IsScriptCPreview ? Auto_Cropped_Width : Final_Width
global gPriorHeight = IsAutoCPreview || IsGAutoCPreview ? Source_Height : \
IsScriptCPreview ? Auto_Cropped_Height : Final_Height

""", "Global Aspect Ratios 2 Eval") : nop()

IsCommonResizing = IsPriorWidthHeight && (((IsAutoCPreview || IsGAutoCPreview) && \
((Source_Width != gPriorWidth) || (Source_Height != gPriorHeight))) || \
(IsScriptCPreview && ((Auto_Cropped_Width != gPriorWidth) || (Auto_Cropped_Height != gPriorHeight))) || \
(!IsScriptCPreview && !IsAutoCPreview && !IsGAutoCPreview && \
((Final_Width != gPriorWidth) || (Final_Height != gPriorHeight))))

IsRModeText = RMode || IsCommonResizing

# ===============================================================================
#            Cropping Previews
# ===============================================================================

# -------------------------------------------------------------------------------
#            AutoC Preview
# -------------------------------------------------------------------------------

IsAutoCPreview ? Eval("""

AutoCPreviewVideo = Source.autocrop(mode=1, wMultOf=2, hMultOf=2, leftadd=0, topadd=0, rightadd=0, \
bottomadd=0, threshold=CThresh, samples=CSample, samplestartframe=CStart, sampleendframe=-1, aspect=0)

""", "AutoC Preview Eval") : nop()

# -------------------------------------------------------------------------------
#            For Info & Script Cropping Previews
# -------------------------------------------------------------------------------

Info || IsScriptCPreview ? Eval("""

CRVideo_WMod = (CRVideo_Width % 16 == 0) ? 16 : (CRVideo_Width % 8 == 0) ? 8 : \
(CRVideo_Width % 4 == 0) ? 4 : (CRVideo_Width % 2 == 0) ? 2 : 1
CRVideo_HMod = (CRVideo_Height % 16 == 0) ? 16 : (CRVideo_Height % 8 == 0) ? 8 : \
(CRVideo_Height % 4 == 0) ? 4 : (CRVideo_Height % 2 == 0) ? 2 : 1

CRVideo_Mod_Text = string(CRVideo_WMod, "  (Mod %.0fx") + string(CRVideo_HMod, "%.0f)")

Extra_CL = \
NoResizing ? (CSplit1 ? F_NR_Extra_CL : F_NR_Extra_Crop_LR) : \
ResizeWO ? F_RWO_Extra_Crop_LR : FullResizeA ? F_FRA_Extra_Crop_LR : F_FRB_Extra_Crop_LR
Extra_CT = \
NoResizing ? (CSplit1 ? F_NR_Extra_CT : F_NR_Extra_Crop_TB) : \
ResizeWO ? (CSplit1 ? F_RWO_Extra_CT : F_RWO_Extra_Crop_TB) : \
FullResizeA ? F_FRA_Extra_Crop_TB : F_FRB_Extra_Crop_TB
Extra_CR = \
NoResizing ? (CSplit1 ? F_NR_Extra_CR : F_NR_Extra_Crop_LR) : \
ResizeWO ? F_RWO_Extra_Crop_LR : FullResizeA ? F_FRA_Extra_Crop_LR : F_FRB_Extra_Crop_LR
Extra_CB = \
NoResizing ? (CSplit1 ? F_NR_Extra_CB : F_NR_Extra_Crop_TB) : \
ResizeWO ? (CSplit1 ? F_RWO_Extra_CB : F_RWO_Extra_Crop_TB) : \
FullResizeA ? F_FRA_Extra_Crop_TB : F_FRB_Extra_Crop_TB

In_DisplayAR = IsMInDAR ? string(MInDARX, "  (%.0f:") + string(MInDARY, "%.0f)") : \
IsMPassedThru ? string(gMPassDARX, "  (%.0f:") + string(gMPassDARY, "%.0f)") : CR_FTF(Source_DAR)

Total_CL = CL + Extra_CL
Total_CT = CT + Extra_CT
Total_CR = CR + Extra_CR
Total_CB = CB + Extra_CB

Final_WMod = !IsBordered ? CRVideo_WMod : (Final_Width % 16 == 0) ? 16 : \
(Final_Width % 8 == 0) ? 8 : (Final_Width % 4 == 0) ? 4 : (Final_Width % 2 == 0) ? 2 : 1
Final_HMod = !IsBordered ? CRVideo_HMod : (Final_Height % 16 == 0) ? 16 : \
(Final_Height % 8 == 0) ? 8 : (Final_Height % 4 == 0) ? 4 : (Final_Height % 2 == 0) ? 2 : 1

Final_Mod_Text = string(Final_WMod, "  (Mod %.0fx") + string(Final_HMod, "%.0f)")

Source_WMod = (Source_Width % 16 == 0) ? 16 : (Source_Width % 8 == 0) ? 8 : \
(Source_Width % 4 == 0) ? 4 : (Source_Width % 2 == 0) ? 2 : 1
Source_HMod = (Source_Height % 16 == 0) ? 16 : (Source_Height % 8 == 0) ? 8 : \
(Source_Height % 4 == 0) ? 4 : (Source_Height % 2 == 0) ? 2 : 1

Source_Mod_Text = string(Source_WMod, "  (Mod %.0fx") + string(Source_HMod, "%.0f)")

""", "Info & Script Cropping Preview Eval") : nop()

# -------------------------------------------------------------------------------
#            For Script Cropping Previews
# -------------------------------------------------------------------------------

IsScriptCPreview && !Info ? Eval("""

SCP_Source = (!AutoC && !GAutoC ? "\n" : "") + \
string(Source_Width, "Source\n%.0f x ") + string(Source_Height, "%.0f") + Source_Mod_Text + \
"   DAR  " + CR_FS(Source_DAR, 6, true) + In_DisplayAR + \
(!IsAnamorphicInput ? "\nNon " : "\n") + "Anamorphic   SAR  " + CR_FS(Source_SAR, 6, true) + \
CR_FTF(Source_SAR) + (!AutoC && !GAutoC ? "" : \
string(Auto_Cropped_Width, "\nAuto Cropped Resolution  %.0f x ") + string(Auto_Cropped_Height,"%.0f"))

SCP_SpecifiedC = "\n\n\n\n\nSpecified Cropping" + (!AutoC && !GAutoC ? "\n(" : " (After AutoC)\n(") + \
CR_FS(CL) + ", " + CR_FS(CT) + ", " + CR_FS(-CR) + ", " + CR_FS(-CB) + ")"

SCP_TotalC = "\n\n\n\n\n\n\nTotal Cropping" + (!AutoC && !GAutoC ? "\n(" : " (After AutoC)\n(") + \
CR_FS(Total_CL) + ", " + CR_FS(Total_CT) + ", " + CR_FS(-Total_CR) + ", " + CR_FS(-Total_CB) + ")"

SCP_Output = "\n\n\n\n\n\n\n" + (!IsBordered ? "Output" : "Picture Excluding Borders") + \
string(CRVideo_Width, "\n%.0f x ") + string(CRVideo_Height, "%.0f") + CRVideo_Mod_Text + \
"   DAR  " + CR_FS(CRVideo_DAR, 6, true) + (NoResizing && \
(Final_Width == Source_Width) && (Final_Height == Source_Height) ? In_DisplayAR : CR_FTF(CRVideo_DAR)) + \
(!IsAnamorphicOutput ? "\nNon " : "\n") + "Anamorphic   SAR  " + CR_FS(Final_SAR, 6, true) + CR_FTF(Final_SAR)

SCP_W = !IsCommonResizing ? F_Auto_Cropped_Width : F_gPriorWidth
SCP_H = !IsCommonResizing ? F_Auto_Cropped_Height : F_gPriorHeight
SCP_Color1 = FullResizing ? $FFFFE0 : $F0FFFF
SCP_Color2 = FullResizing ? $F0FFFF : $FFFFE0
SCP_Size = min(SCP_W * 0.04, SCP_H * ((SCP_H < 300) ? 0.044 : (SCP_H < 400) ? 0.042 : \
(SCP_H < 500) ? 0.036 : (SCP_H < 600) ? 0.032 : (SCP_H < 1100) ? 0.028 : 0.026))
SCP_Y = (SCP_H * ((SCP_H < 300) ? 0.16 : (SCP_H < 400) ? 0.18 : (SCP_H < 500) ? 0.214 : \
(SCP_H < 600) ? 0.245 : (SCP_H < 1100) ? 0.285 : 0.31)) - (!AutoC && !GAutoC ? 0.0 : SCP_Size * -0.5)
SCP_LSP = (SCP_H < 400) ? 10 : 20

""", "Script Cropping Preview 1 Eval") : nop()

# -------------------------------------------------------------------------------
#            Script Cropping Previews
# -------------------------------------------------------------------------------

IsScriptCPreview ? Eval("""

YUV444Video = IsYV24(Source) || (IsAvsHBD && Is444(Source)) ? AutoCroppedSource : \
IsAvsHBD ? AutoCroppedSource.ConvertToYUV444() : AutoCroppedSource.ConvertToYV24()

PCL = round(CL)
PCT = round(CT)
PCR = round(CR)
PCB = round(CB)

CLine_L = (PCL == 0) ? 0 : (PCL >= CLine) ? CLine : PCL
CLine_T = (PCT == 0) ? 0 : (PCT >= CLine) ? CLine : PCT
CLine_R = (PCR == 0) ? 0 : (PCR >= CLine) ? CLine : PCR
CLine_B = (PCB == 0) ? 0 : (PCB >= CLine) ? CLine : PCB

Script_CL = (round(Total_CL) > PCL) ? round(Total_CL) : 0
Script_CT = (round(Total_CT) > PCT) ? round(Total_CT) : 0
Script_CR = (round(Total_CR) > PCR) ? round(Total_CR) : 0
Script_CB = (round(Total_CB) > PCB) ? round(Total_CB) : 0

SLine_L = (Script_CL == 0) ? 0 : (Script_CL >= CLine) ? CLine : Script_CL
SLine_T = (Script_CT == 0) ? 0 : (Script_CT >= CLine) ? CLine : Script_CT
SLine_R = (Script_CR == 0) ? 0 : (Script_CR >= CLine) ? CLine : Script_CR
SLine_B = (Script_CB == 0) ? 0 : (Script_CB >= CLine) ? CLine : Script_CB

OverlayVideo = (CPreview == 1) ? \
BlankClip(YUV444Video).crop(Script_CL, Script_CT, -Script_CR, -Script_CB)\
.AddBorders(SLine_L, SLine_T, SLine_R, SLine_B, color=$ADD8E6)\
.AddBorders(Script_CL - SLine_L, Script_CT - SLine_T, Script_CR - SLine_R, Script_CB - SLine_B)\
.crop(PCL, PCT, -PCR, -PCB).AddBorders(CLine_L, CLine_T, CLine_R, CLine_B, color=$FFFF00)\
.AddBorders(PCL - CLine_L, PCT - CLine_T, PCR - CLine_R, PCB - CLine_B) : \
(CPreview == 2) ? \
BlankClip(YUV444Video).crop(Script_CL, Script_CT, -Script_CR, -Script_CB)\
.AddBorders(Script_CL, Script_CT, Script_CR, Script_CB, color=$ADD8E6)\
.crop(PCL, PCT, -PCR, -PCB).AddBorders(PCL, PCT, PCR, PCB, color=$FFFF00) : \
YUV444Video.crop(Script_CL, Script_CT, -Script_CR, -Script_CB)\
.AddBorders(Script_CL, Script_CT, Script_CR, Script_CB, color=$FF0000)\
.crop(PCL, PCT, -PCR, -PCB).AddBorders(PCL, PCT, PCR, PCB)

MaskVideo = ((CPreview == 1) ? \
BlankClip(YUV444Video).crop(Script_CL, Script_CT, -Script_CR, -Script_CB)\
.AddBorders(SLine_L, SLine_T, SLine_R, SLine_B, color=$FFFFFF)\
.AddBorders(Script_CL - SLine_L, Script_CT - SLine_T, Script_CR - SLine_R, Script_CB - SLine_B)\
.crop(PCL, PCT, -PCR, -PCB).AddBorders(CLine_L, CLine_T, CLine_R, CLine_B, color=$e4e4e4)\
.AddBorders(PCL - CLine_L, PCT - CLine_T, PCR - CLine_R, PCB - CLine_B) : \
(CPreview == 2) ? \
BlankClip(YUV444Video).crop(Script_CL, Script_CT, -Script_CR, -Script_CB)\
.AddBorders(Script_CL, Script_CT, Script_CR, Script_CB, color=$7f7f7f)\
.crop(PCL, PCT, -PCR, -PCB).AddBorders(PCL, PCT, PCR, PCB, color=$4b4b4b) : \
BlankClip(YUV444Video, color=$FFFFFF).crop(Script_CL, Script_CT, -Script_CR, -Script_CB)\
.AddBorders(Script_CL, Script_CT, Script_CR, Script_CB, color=$4b4b4b)\
.crop(PCL, PCT, -PCR, -PCB).AddBorders(PCL, PCT, PCR, PCB))\
.ColorYUV(Levels="TV->PC")

ScriptCPreviewVideo = (CPreview == 1) || (CPreview == 2) ? \
Overlay(AutoCroppedSource, OverlayVideo, mask=MaskVideo) : \
Overlay(invert(AutoCroppedSource), OverlayVideo, mask=MaskVideo)

ScriptCPreviewVideo = \
!IsCommonResizing ? ScriptCPreviewVideo : ScriptCPreviewVideo.Spline36Resize(gPriorWidth, gPriorHeight)

ScriptCPreviewVideo = Info ? ScriptCPreviewVideo : ScriptCPreviewVideo.subtitle(SCP_Source + SCP_Output, \
y=SCP_Y, size=SCP_Size, text_color=SCP_Color1, align=8, lsp=SCP_LSP)\
.subtitle(SCP_SpecifiedC, y=SCP_Y, size=SCP_Size, text_color=$F0E68C, align=8, lsp=SCP_LSP)\
.subtitle(SCP_TotalC, y=SCP_Y, size=SCP_Size, text_color=$B0C4DE, align=8, lsp=SCP_LSP)

""", "Script Cropping Preview 2 Eval") : nop()

# ===============================================================================
#            Display Info
# ===============================================================================

Info ? Eval("""

InfoVideo = IsScriptCPreview ? ScriptCPreviewVideo : \
!IsCommonResizing ? CRVideo : CRVideo.Spline36Resize(gPriorWidth, gPriorHeight)

F_InfoVideo_Width = float(width(InfoVideo))
F_InfoVideo_Height = float(height(InfoVideo))
InfoVideo_StorageAR = F_InfoVideo_Width / F_InfoVideo_Height

I_PicDimScale = (Source_Bits == 10) ? 4.0 : (Source_Bits == 12) ? 16.0 : \
(Source_Bits == 14) ? 64.0 : (Source_Bits == 16) ? 256.0 : 1.0 / 256.0

I_RGBWhite = (Source_Bits == 8) ? 255.0 : (Source_Bits == 10) ? 1020.0 : \
(Source_Bits == 12) ? 4080.0 : (Source_Bits == 14) ? 16320.0 : \
(Source_Bits == 16) ? 65280.0 : 255.0 / 256.0

IsPicDim = (PicDim > 0)
I_Dim = (IsColorConversion || IsScriptCPreview) && !CR_Info_AutoPicDim_CPreview_ColorConvert() ? 0 : PicDim
I_Dim = (I_Dim == 0) ? 0 : (Source_Bits == 8) ? I_Dim : float(I_Dim) * I_PicDimScale
I_Dim = (I_Dim == 0) ? 0 : max(0, min(I_RGBWhite, I_RGBWhite - I_Dim))

I_Condition = AutoC || GAutoC || IsBordered || !IsAnamorphicOutput ? 0 : \
(AutoDAR ? 1 : 0) + ((CC != "") && (CM != "") ? 2 : (CC != "") ? 1 : 0) + (IsPicDim ? 1 : 0) + \
(IsRModeText ? 1 : 0) + (IsGModeText ? 1 : 0) + (Position ? 1 : 0) + (!AnamorphicInput ? -2 : 0)

I_Color1 = FullResizing ? $FFFFE0 : $F0FFFF
I_Color2 = FullResizing ? $F0FFFF : $FFFFE0
I_Color3 = IsColorConversion ? $FFA07A : I_Color1
I_S1 = (AutoC || GAutoC) && IsBordered
I_S2 = IsBordered
I_S3 = AutoC || GAutoC
I_S4 = !NoResizer && IsRStr && IsRRepair && (I_Condition == 7)

I_D = "\nDisplay Aspect Ratio   "
I_S = "\nSample Aspect Ratio   "
I_St = "\nStorage Aspect Ratio   "

I_Size = min(F_InfoVideo_Width * 0.0215, F_InfoVideo_Height * \
((F_InfoVideo_Height < 300) ? (I_S1 ? 0.023 : I_S2 ? 0.027 : I_S3 ? 0.028 : I_S4 ? 0.0325 : 0.034) : \
(F_InfoVideo_Height < 400) ? (I_S1 ? 0.025 : I_S2 ? 0.029 : I_S3 ? 0.03 : I_S4 ? 0.034 : 0.0355) : \
(F_InfoVideo_Height < 500) ? (I_S1 ? 0.023 : I_S2 ? 0.027 : I_S3 ? 0.028 : I_S4 ? 0.032 : 0.033) : \
(F_InfoVideo_Height < 600) ? (I_S1 ? 0.024 : I_S2 ? 0.028 : I_S3 ? 0.029 : I_S4 ? 0.033 : 0.034) : \
I_S1 ? 0.0245 : I_S2 ? 0.0285 : I_S3 ? 0.0295 : I_S4 ? 0.0335 : 0.0345))

I_X1 = I_Size * 2.0
I_X2 = I_Size * 27.0
I_Y = (F_InfoVideo_Height < 400) ? I_Size / 1.5 : I_Size
I_LSP = (F_InfoVideo_Height < 400) ? 10 : 20

# -------------------------------------------------------------------------------
#            Left Column
# -------------------------------------------------------------------------------

I_AvsVersion = !CR_Info_Formatted_AVS_Version() ? VersionString() : \
IsAvsPlus ? "AviSynth+  " + MidStr(AvsStr, 11, NumA - 14) + string(AvsBuild, "  (%.0f)") : \
(FindStr(AvsStr, "2.60") > 0) ? "AviSynth  2.60" : \
(FindStr(AvsStr, "2.61") > 0) ? "AviSynth  2.61" : VersionString()
I_AvsVersion = (I_AvsVersion == VersionString()) ? I_AvsVersion : I_AvsVersion + \
((FindStr(AvsStr, "i386") > 0) || (FindStr(AvsStr, "x86") > 0) ? "  32 bit" : \
(FindStr(AvsStr, "x86_64") > 0) || (FindStr(AvsStr, "x64") > 0) || \
(FindStr(AvsStr, "x86-64") > 0) || (FindStr(AvsStr, "amd64") > 0) ? "  64 bit" : "")

I_Num = FrameRateNumerator(Source)
I_Den = FrameRateDenominator(Source)
I_FRate = float(I_Num) / float(I_Den)

I_Source = string(Source_Width, "\nSource Resolution   %.0f x ") + string(Source_Height, "%.0f") + \
Source_Mod_Text + I_St + CR_FS(Source_StorageAR, 6, true) + CR_FTF(Source_StorageAR) + \
I_D + CR_FS(Source_DAR, 6, true) + In_DisplayAR + \
string(Source_Bits, "\n%.0f bit   ") + Color_Format + "   " + CR_FS(I_FRate, 4) + " fps" + \
((abs(round(I_FRate) - I_FRate) <= 0.00005) ? CR_FTF(I_FRate, 2) : \
string(I_Num, "  (%.0f / ") + string(I_Den, "%.0f)"))

I_AutoC = !AutoC && !GAutoC ? "" : \
string(Auto_Cropped_Width, "\n\nAuto Cropped Resolution   %.0f x ") + string(Auto_Cropped_Height, "%.0f") + \
I_St + CR_FS(Auto_Cropped_StorageAR, 6, true) + CR_FTF(Auto_Cropped_StorageAR) + \
I_D + CR_FS(Auto_Cropped_DAR, 6, true) + (NoResizing && (Auto_Cropped_Width == Source_Width) && \
(Auto_Cropped_Height == Source_Height) ? In_DisplayAR : CR_FTF(Auto_Cropped_DAR))

I_SpecifiedC = \
"\n\nSpecified Cropping\n(" + CR_FS(CL) + ", " + CR_FS(CT) + ", " + CR_FS(-CR) + ", " + CR_FS(-CB) + ")"

I_ExtraC = "\nAdditional Script Cropping\n(" + \
CR_FS(Extra_CL) + ", " + CR_FS(Extra_CT) + ", " + CR_FS(-Extra_CR) + ", " + CR_FS(-Extra_CB) + ")"

I_CSplit_Crop = NoResizer ? "" : \
string(CCL, "\n\nCrop (%.0f, ") + string(CCT, "%.0f, ") + string(-CCR, "%.0f, ") + string(-CCB, "%.0f)")

I_CSplit_Resizer = NoResizer ? "" : "\nResizer Cropping\n(" + \
CR_FS(RCL) + ", " + CR_FS(RCT) + ", " + CR_FS(-RCR) + ", " + CR_FS(-RCB) + ")"

I_TotalC = "\nTotal Cropping\n(" + CR_FS(Total_CL) + ", " + CR_FS(Total_CT) + ", " + \
CR_FS(-Total_CR) + ", " + CR_FS(-Total_CB) + (NoResizer ? ")\n\n\n\n" : ")")

I_Cropped_Resolution = "\n\nCropped Resolution   " + \
(NoResizing ? CR_FS(F_NR_Cropped_Width) + " x " + CR_FS(F_NR_Cropped_Height) : \
ResizeWO ? CR_FS(F_RWO_Cropped_Width) + " x " + CR_FS(F_RWO_Cropped_Height) : \
FullResizeA ? CR_FS(F_FRA_Cropped_Width) + " x " + CR_FS(F_FRA_Cropped_Height) : \
CR_FS(F_FRB_Cropped_Width) + " x " + CR_FS(F_FRB_Cropped_Height))

I_Cropped_StorageAR = NoResizing ? NR_Cropped_StorageAR : ResizeWO ? RWO_Cropped_StorageAR : \
FullResizeA ? FRA_Cropped_StorageAR : FRB_Cropped_StorageAR

I_Cropped_DAR = NoResizing ? NR_Cropped_DAR : ResizeWO ? RWO_Cropped_DAR : \
FullResizeA ? FRA_Cropped_DAR : FRB_Cropped_DAR
I_Cropped_DAR = (abs(I_Cropped_DAR - CRVideo_DAR) >= 0.00000015) ? I_Cropped_DAR : CRVideo_DAR

I_Cropped_ARs = I_St + CR_FS(I_Cropped_StorageAR, 6, true) + CR_FTF(I_Cropped_StorageAR) + \
I_D + CR_FS(I_Cropped_DAR, 6, true) + (NoResizing && (F_Cropped_Source_Width == Source_Width) && \
(F_Cropped_Source_Height == Source_Height) ? In_DisplayAR : CR_FTF(I_Cropped_DAR))

I_CRVideo_ARs = CRVideo_Mod_Text + \
I_St + CR_FS(CRVideo_StorageAR, 6, true) + CR_FTF(CRVideo_StorageAR) + \
I_D + CR_FS(CRVideo_DAR, 6, true) + (NoResizing && (CRVideo_Width == Source_Width) && \
(CRVideo_Height == Source_Height) ? In_DisplayAR : CR_FTF(CRVideo_DAR))

I_Output = !IsBordered ? \
string(CRVideo_Width, "\n\nOutput Resolution   %.0f x ") + string(CRVideo_Height, "%.0f") + I_CRVideo_ARs : \
string(CRVideo_Width, "\n\nOutput Exc Borders   %.0f x ") + string(CRVideo_Height, "%.0f") + I_CRVideo_ARs + \
string(BL, "\n\nAddBorders (%.0f, ") + string(BT, "%.0f, ") + string(BR, "%.0f, ") + string(BB, "%.0f)") + \
string(Final_Width, "\nOutput Inc Borders   %.0f x ") + string(Final_Height, "%.0f") + Final_Mod_Text + \
I_St + CR_FS(Final_StorageAR, 6, true) + CR_FTF(Final_StorageAR) + \
I_D + CR_FS(Final_DAR, 6, true) + (NoResizing && (Final_Width == Source_Width) && \
(Final_Height == Source_Height) ? In_DisplayAR : CR_FTF(Final_DAR))

I_Left_Column = I_AvsVersion + I_Source + I_AutoC + I_SpecifiedC + I_ExtraC + I_CSplit_Crop + \
I_CSplit_Resizer + I_TotalC + I_Cropped_Resolution + I_Cropped_ARs + I_Output

# -------------------------------------------------------------------------------
#            Right Column
# -------------------------------------------------------------------------------

I_NoResizerCrop = NoResizer || (ResizeWO && (CSplit1 || (0 == RWO_RCT == RWO_RCB)))

I_ResizingMode = "\n" + ((CleanBorders ? (I_NoResizerCrop ? "Clean Borders" : "Resampling  (CleanBorders)") : \
NoResizing ? (I_NoResizerCrop ? "No Resizing" : "Resampling  (NoResize)") : \
ResizeWO ? (I_NoResizerCrop ? "Resize Width Only" : "Resampling Height  (ResizeWO)") : "Full Resizing Mode"))

I_Resizer = NoResizer ? "" : "\nResizer   " + (IsZResize ? "Z_" : IsDResize ? "D_" : IsFResize ? "F_" : "") + \
Resizer + ((RTaps == ", filter_param_a=3") || (RTaps == ", Taps=3") ? string(3, "  (%.0f taps)") : \
(RTaps == ", filter_param_a=4") || (RTaps == ", Taps=4") ? string(4, "  (%.0f taps)") : "")

I_RStr = NoResizer || !IsRStr ? "" : "\nRStr   " + RStr

I_RingRepair = !IsRRepair ? "" : \
IsRingRepair ? "\nRingRepair Weight   " + CR_FS(Weight, 4, true) : "\nDitherTools RingRepair"

I_Input_ARs = \
(IsRStr && IsRRepair ? "" : (I_Condition != 7) ? (NoResizer ? "\n\n\n" : IsRStr || IsRRepair ? "\n" : "\n\n") : \
(NoResizer ? "\n\n" : IsRStr || IsRRepair ? "" : "\n")) + "\n\nInput Aspect" + \
(IsInDAR ? "   InDAR" + I_D + CR_FS(InDAR, 6, true) + CR_FTF(InDAR) + \
I_S + CR_FS(Source_SAR, 6, true) + CR_FTF(Source_SAR) : \
IsInSAR ? "   InSAR" + I_D + CR_FS(Source_DAR, 6, true) + CR_FTF(Source_DAR) + \
I_S + CR_FS(InSAR, 6, true) + CR_FTF(InSAR) : \
IsGInDAR ? "   Global InDAR" + I_D + CR_FS(GInDAR, 6, true) + CR_FTF(GInDAR) + \
I_S + CR_FS(Source_SAR, 6, true) + CR_FTF(Source_SAR) : \
IsGInSAR ? "   Global InSAR" + I_D + CR_FS(Source_DAR, 6, true) + CR_FTF(Source_DAR) + \
I_S + CR_FS(GInSAR, 6, true) + CR_FTF(GInSAR) : \
IsMInDAR ? "   MeGUI DAR" + I_D + CR_FS(MInDAR, 6, true) + \
string(MInDARX, "  (%.0f:") + string(MInDARY, "%.0f)") + \
I_S + CR_FS(Source_SAR, 6, true) + CR_FTF(Source_SAR) : \
IsMPassedThru ? "   MeGUI DAR" + I_D + CR_FS(MPassDAR, 6, true) + \
string(gMPassDARX, "  (%.0f:") + string(gMPassDARY, "%.0f)") + \
I_S + CR_FS(Source_SAR, 6, true) + CR_FTF(Source_SAR) : "") + \
(AnamorphicInput && !IsAnamorphicInput ? "\nNot Anamorphic" : !AnamorphicInput ? "   Not Specified" : "") + \
(!AnamorphicInput ? I_S + CR_FS(Source_SAR, 6, true) + CR_FTF(Source_SAR) : "") + \
(!IsAnamorphicInput ? "" : \
"\nDisplay Dimensions   " + CR_FS(F_Source_Display_Width, 2) + string(Source_Height, " x %.0f"))

I_Output_ARs = "\n\nOutput Aspect" + \
(NoResizing && IsAnamorphicOutput && (Final_Width == Source_Width) && (Final_Height == Source_Height) ? \
I_D + CR_FS(Final_DAR, 6, true) + In_DisplayAR + I_S + CR_FS(Final_SAR, 6, true) + CR_FTF(Final_SAR) : \
NoResizing && IsAnamorphicOutput ? \
I_D + CR_FS(Final_DAR, 6, true) + CR_FTF(Final_DAR) + I_S + CR_FS(Final_SAR, 6, true) + CR_FTF(Final_SAR) : \
IsOutDAR ? "   OutDAR" + I_D + CR_FS(OutDAR, 6, true) + CR_FTF(OutDAR) + \
I_S + CR_FS(Final_SAR, 6, true) + CR_FTF(Final_SAR) : \
IsOutSAR ? "   OutSAR" + I_D + CR_FS(Final_DAR, 6, true) + CR_FTF(Final_DAR) + \
I_S + CR_FS(OutSAR, 6, true) + CR_FTF(OutSAR) : \
IsGOutDAR ? "   Global OutDAR" + I_D + CR_FS(GOutDAR, 6, true) + CR_FTF(GOutDAR) + \
I_S + CR_FS(Final_SAR, 6, true) + CR_FTF(Final_SAR) : \
IsGOutSAR ? "   Global OutSAR" + I_D + CR_FS(Final_DAR, 6, true) + CR_FTF(Final_DAR) + \
I_S + CR_FS(GOutSAR, 6, true) + CR_FTF(GOutSAR) : "") + \
(AnamorphicOutput && !IsAnamorphicOutput ? "\nNot Anamorphic" : \
!AnamorphicOutput ? "   Not Specified" : "") + \
(!AnamorphicOutput ? I_S + CR_FS(Final_SAR, 6, true) + CR_FTF(Final_SAR) : "") + \
(!IsAnamorphicOutput ? "" : \
"\nDisplay Dimensions   " + CR_FS(F_Final_Display_Width, 2) + string(Final_Height, " x %.0f"))

I_MeGUI_DAR = !IsAnamorphicOutput || !CR_MeGUI_Support() ? "" : "\n\n" + \
((I_Condition < 6) ? "MeGUI Output DAR\n" : "") + ((I_Condition < 5) ? \
string(MeGUI_darx, "MeGUI_darx = %.0f\n") + string(MeGUI_dary, "MeGUI_dary = %.0f") : \
string(MeGUI_darx, "MeGUI_darx=%.0f   ") + string(MeGUI_dary, "MeGUI_dary=%.0f"))

LineBreak = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + ((AutoC || GAutoC) && IsBordered ? "\n\n\n\n\n\n\n\n\n" : \
IsBordered ? "\n\n\n\n\n" : AutoC || GAutoC ? "\n\n\n\n" : I_S4 ? "\n" : "")
CConvertLB = (CC == "") ? "\n\n" : (CM == "") ? "\n" : ""
PicDimLB = !IsPicDim ? "\n" : ""
GModeLB = !IsGModeText ? "\n" : ""
RModeLB = !IsRModeText ? "\n" : ""
PosLB = !Position ? "\n\n" : (I_Condition > 3) ? "\n" : ""
AutoDARLineBreak = !AutoDAR ? "" : LineBreak + CConvertLB + PicDimLB + GModeLB + RModeLB + PosLB
CConvertLineBreak = (CC == "") ? "" : LineBreak + "\n" + CConvertLB + PicDimLB + GModeLB + RModeLB + PosLB
PicDimLineBreak = !IsPicDim ? "" : LineBreak + "\n\n\n" + GModeLB + RModeLB + PosLB
GModeLineBreak = !IsGModeText ? "" : LineBreak + "\n\n\n\n" + RModeLB + PosLB
RModeLineBreak = !IsRModeText ? "" : LineBreak + "\n\n\n\n\n" + PosLB
PosLineBreak1 = !Position ? "" : LineBreak + "\n\n\n\n\n\n" + PosLB
PosLineBreak2 = !Position ? "" : (I_Condition > 3) ? "    " : "\n"

I_PreAutoDAR = !IsAutoDAR ? 0.0 : FullResizeA ? (!IsOutputSAR ? \
F_Resizing_Width / F_FRA_Adjusted_Height1 : F_Resizing_Width * OutputSAR / F_FRA_Adjusted_Height1) : \
!IsOutputSAR ? \
F_FRB_Adjusted_Width1 / F_Resizing_Height : F_FRB_Adjusted_Width1 * OutputSAR / F_Resizing_Height

I_AutoDAR = !AutoDAR ? "" : \
AutoDARLineBreak + "AutoDAR   " + (!IsAutoDAR ? "Not Applicable" : FullResizeA ? \
(IsHeight169 && (F_FRA_Adjusted_Height1 != F_FRA_Adjusted_Height2) ? string(I_PreAutoDAR, "%.6f -> 16:9") : \
IsHeight43 && (F_FRA_Adjusted_Height1 != F_FRA_Adjusted_Height2) ? string(I_PreAutoDAR, "%.6f -> 4:3") : \
IsHeight169 || IsHeight43 ? "Not Required" : "Out Of Range") : \
IsWidth169 && (F_FRB_Adjusted_Width1 != F_FRB_Adjusted_Width2) ? string(I_PreAutoDAR, "%.6f -> 16:9") : \
IsWidth43 && (F_FRB_Adjusted_Width1 != F_FRB_Adjusted_Width2) ? string(I_PreAutoDAR, "%.6f -> 4:3") : \
IsWidth169 || IsWidth43 ? "Not Required" : "Out Of Range")

I_ColorConvert = (CC == "") ? "" : \
CConvertLineBreak + "Color Conversion   " + (!IsColorConversion ? "None Required" : \
CC + (!IsColorConversion || !IsScriptCPreview ? "" : "\nNot Applied To Cropping Preview")) + \
(!IsColorConversion || IsScriptCPreview ? "" : "\n" + \
((CM == "709-2020") ? "Rec.709 -> Rec.2020" : \
(CM == "2020-709") ? "Rec.2020 -> Rec.709" : \
(CM == "601-709") && (CC != "HDRTools") ? "Rec.601 -> Rec.709" : \
(CM == "601-709") || (CM == "601P-709") ? "Rec.601 (470bg) -> Rec.709" : \
(CM == "601N-709") ? "Rec.601 (170m) -> Rec.709" : \
(CM == "709-601") && (CC != "HDRTools") ? "Rec.709 -> Rec.601" : \
(CM == "709-601") || (CM == "709-601P") ? "Rec.709 -> Rec.601 (470bg)" : \
(CM == "709-601N") ? "Rec.709 -> Rec.601 (170m)" : \
(CM == "2020-601") || (CM == "2020-601P") ? "Rec.2020 -> Rec.601 (470bg)" : \
(CM == "2020-601N") ? "Rec.2020 -> Rec.601 (170m)" : \
(CM == "601-2020") || (CM == "601P-2020") ? "Rec.601 (470bg) -> Rec.2020" : \
(CM == "601N-2020") ? "Rec.601 (170m) -> Rec.2020" : \
(CM == "601P-601N") ? "Rec.601 (470bg) -> Rec.601 (170m)" : \
(CM == "601N-601P") ? "Rec.601 (170m) -> Rec.601 (470bg)" : ""))

I_PicDim = !IsPicDim ? "" : PicDimLineBreak + "PicDim   " + ((I_Dim != 0) ? string(PicDim, "%.0f") : \
"Disabled  " + (IsScriptCPreview ? "(CropPreview)" : "(Color Conversion)"))

I_GMode = !IsGModeText ? "" : GModeLineBreak + "GMode   " + (IsGMode0 ? "0" : IsGMode1 ? "1" : "2")

I_RMode = !IsRModeText ? "" : RModeLineBreak + "RMode   " + \
(RMode ? "Enabled" : (IsScriptCPreview ? "CPreview " : "Output ") + \
string(gPriorWidth, "resized to %.0f x ") + string(gPriorHeight, "%.0f"))

I_Right_Column = "CropResize  2022-11-19" + \
I_ResizingMode + I_Resizer + I_RStr + I_RingRepair + I_Input_ARs + I_Output_ARs + I_MeGUI_DAR

# -------------------------------------------------------------------------------

InfoVideo = ((I_Dim == 0) ? InfoVideo : \
InfoVideo.Levels(0, 1.0, I_RGBWhite, 0, I_Dim, coring=false))\
.subtitle(I_Left_Column, x=I_X1, y=I_Y, size=I_Size, text_color=I_Color1, lsp=I_LSP)\
.subtitle(I_Right_Column, x=I_X2, y=I_Y, size=I_Size, text_color=I_Color1, lsp=I_LSP)
InfoVideo = !AutoDAR ? InfoVideo : \
InfoVideo.subtitle(I_AutoDAR, x=I_X2, y=I_Y, size=I_Size, text_color=I_Color1, lsp=I_LSP)
InfoVideo = (CC == "") ? InfoVideo : \
InfoVideo.subtitle(I_ColorConvert, x=I_X2, y=I_Y, size=I_Size, text_color=I_Color3, lsp=I_LSP)
InfoVideo = !IsPicDim ? InfoVideo : \
InfoVideo.subtitle(I_PicDim, x=I_X2, y=I_Y, size=I_Size, text_color=I_Color1, lsp=I_LSP)
InfoVideo = !IsGModeText ? InfoVideo : \
InfoVideo.subtitle(I_GMode, x=I_X2, y=I_Y, size=I_Size, text_color=I_Color1, lsp=I_LSP)
InfoVideo = !IsRModeText ? InfoVideo : \
InfoVideo.subtitle(I_RMode, x=I_X2, y=I_Y, size=I_Size, text_color=I_Color1, lsp=I_LSP)
InfoVideo = !Position ? InfoVideo : \
InfoVideo.CR_Position(X=I_X2, Y=I_Y, Size=I_Size, Color=I_Color2, \
LB1=PosLineBreak1, LB2=PosLineBreak2, lsp=I_LSP)

""", "Info Eval") : nop()

# ===============================================================================
#            CropResize Output
# ===============================================================================

Output = IsAutoCPreview ? AutoCPreviewVideo : IsGAutoCPreview ? Source : \
Info ? InfoVideo : IsScriptCPreview ? ScriptCPreviewVideo : CRVideo

# -------------------------------------------------------------------------------

!Info && (Position || \
((IsGModeText || IsRModeText) && (IsScriptCPreview || IsAutoCPreview || IsGAutoCPreview))) ? Eval("""

W = IsCommonResizing ? F_gPriorWidth : IsAutoCPreview || IsGAutoCPreview ? F_Source_Width : \
IsScriptCPreview ? F_Auto_Cropped_Width : F_Final_Width
H = IsCommonResizing ? F_gPriorHeight : IsAutoCPreview || IsGAutoCPreview ? F_Source_Height : \
IsScriptCPreview ? F_Auto_Cropped_Height : F_Final_Height

ModeText = \
(!IsGModeText && !IsRModeText) || (!IsScriptCPreview && !IsAutoCPreview && !IsGAutoCPreview) ? "" : \
(!IsGModeText ? "" : "GMode   " + (IsGMode0 ? "0" : IsGMode1 ? "1" : "2")) + \
(!IsRModeText ? "" : (!IsGModeText ? "" : "\n") + "RMode   " + \
(RMode ? "Enabled" : string(gPriorWidth, "Preview resized to %.0f x ") + string(gPriorHeight, "%.0f")))

Color1 = FullResizing ? $FFFFE0 : $F0FFFF
Color2 = FullResizing ? $F0FFFF : $FFFFE0

Size = min(W * 0.04, H * ((H < 300) ? 0.044 : (H < 400) ? 0.042 : \
(H < 500) ? 0.036 : (H < 600) ? 0.032 : (H < 1100) ? 0.028 : 0.026))
Y = !IsGModeText && !IsRModeText ? 0 : H - (Size * (!IsGModeText || !IsRModeText ? 1.0 : 2.0))
LSP = (H < 400) ? 10 : 20

Output = !IsCommonResizing ? Output : Output.Spline36Resize(gPriorWidth, gPriorHeight)
Output = !IsAutoCPreview ? Output : \
Output.subtitle("AutoCrop Preview", y=(H*0.5)+(Size*0.25), size=Size, text_color=$FFA07A, align=5, lsp=LSP)
Output = (ModeText == "") ? Output : \
Output.subtitle(ModeText, y=Y, size=Size, text_color=Color1, align=2, lsp=LSP)
Output = !Position ? Output : Output.CR_Position(Size=Size, Color=Color2, Align=8, lsp=LSP)

""", "Position & GMode & RMode Eval") : nop()

# -------------------------------------------------------------------------------

return !HasAudio(SourceAudio) ? Output : AudioDub(Output, SourceAudio)  }

# ===============================================================================
# ===============================================================================
#            Global GCropResize Function
# ===============================================================================
# ===============================================================================

function GCropResize(clip Source, \
int "OutWidth", int "OutHeight", \
float "CL", float "CT", float "CR", float "CB", val "CropDAR", int "CSplit", \
bool "AutoC", int "CThresh", int "CStart", int "CSample", \
int "CPreview", int "CLine", \
val "InDAR", val "InSAR", val "OutDAR", val "OutSAR", bool "AutoDAR", \
int "Mod", int "HMod", \
bool "NoResize", bool "ResizeWO", \
bool "Borders", bool "CleanBorders", val "BColor", bool "Frosty", \
string "ColorConvert", string "ColorMode", \
string "Resizer", string "RStr", val "RingRepair", \
int "GMode", bool "RMode", int "PicDim", bool "Position", bool "Info")  {

# -------------------------------------------------------------------------------

OutWidth = default(OutWidth, 0)
OutHeight = default(OutHeight, 0)
AutoC = default(AutoC, false)
CPreview = default(CPreview, 0)
CropDAR = default(CropDAR, 0.0)
CThresh = default(CThresh, 30)
LastFrame = FrameCount(Source) - 1
CStart = default(CStart, (LastFrame > 3000) ? 250 : 0)
CSample = default(CSample, 5)
InDAR = default(InDAR, 0.0)
InSAR = default(InSAR, 0.0)
OutDAR = default(OutDAR, 0.0)
OutSAR = default(OutSAR, 0.0)
GMode = default(GMode, 1)
IsGMode1 = (GMode == 1)
IsGMode2 = (GMode == 2)
IsMod = defined(Mod)
IsResizer = defined(Resizer)
global G_RMode = default(RMode, false)

# -------------------------------------------------------------------------------

Try{  IsPriorG1 = G1_PriorG1  }Catch(err){  IsPriorG1 = false  }

GC_R = " GCropResize " + chr(10)
Assert((0 < GMode <= 2), GC_R + " GMode must be 1 or 2 for" + GC_R + " (0, 1 or 2 for CropResize) " + chr(10))
Assert(AutoC || (CPreview != 4), GC_R + " When CPreview=4, AutoC must be true " + chr(10))
Assert(!AutoC || (CStart < LastFrame), GC_R + \
string(LastFrame, " When AutoC=true CStart must be less than %.0f (the final frame number) ") + chr(10))
Assert(((IsFloat(CropDAR) && (CropDAR == 0)) || (IsFloat(CropDAR) && !IsInt(CropDAR) && (CropDAR > 0))) && \
((IsFloat(InDAR) && (InDAR == 0)) || (IsFloat(InDAR) && !IsInt(InDAR) && (InDAR > 0))) && \
((IsFloat(InSAR) && (InSAR == 0)) || (IsFloat(InSAR) && !IsInt(InSAR) && (InSAR > 0))) && \
((IsFloat(OutDAR) && (OutDAR == 0)) || (IsFloat(OutDAR) && !IsInt(OutDAR) && (OutDAR > 0))) && \
((IsFloat(OutSAR) && (OutSAR == 0)) || (IsFloat(OutSAR) && !IsInt(OutSAR) && (OutSAR > 0))), GC_R + \
" CropDAR, InDAR, InSAR, OutDAR & OutSAR " + chr(10) + \
" must be zero or a positive floating point number " + chr(10) + " (4.0/3.0 or 1.7778 etc) " + chr(10))
Assert((InDAR == 0) || (InSAR == 0), GC_R + " InDAR & InSAR cannot be specified together " + chr(10))
Assert((OutDAR == 0) || (OutSAR == 0), GC_R + " OutDAR & OutSAR cannot be specified together " + chr(10))

# -------------------------------------------------------------------------------
#            GMode1
# -------------------------------------------------------------------------------

IsGMode1 ? Eval("""

IsPriorG1AutoC = IsPriorG1 && G1_AutoC
global G1_Source_Width = AutoC || !IsPriorG1AutoC ? width(Source) : G1_Source_Width
global G1_Source_Height = AutoC || !IsPriorG1AutoC ? height(Source) : G1_Source_Height
global G1_PriorG1 = true

Assert(!AutoC || !IsPriorG1AutoC, GC_R + " AutoC cannot be true for GCropResize(GMode=1) " + chr(10) + \
" after being enabled by a previous instance of GCropResize(GMode=1) " + chr(10))

global G1_OutWidth = (OutWidth == 0) && IsPriorG1 ? G1_OutWidth : OutWidth
global G1_OutHeight = (OutHeight == 0) && IsPriorG1 ? G1_OutHeight : OutHeight
global G1_CL = default(CL, !IsPriorG1 ? 0.0 : G1_CL)
global G1_CT = default(CT, !IsPriorG1 ? 0.0 : G1_CT)
global G1_CR = default(CR, !IsPriorG1 ? 0.0 : G1_CR)
global G1_CB = default(CB, !IsPriorG1 ? 0.0 : G1_CB)
global G1_CropDAR = default(CropDAR, !IsPriorG1 ? 0.0 : G1_CropDAR)
global G1_CSplit = default(CSplit, !IsPriorG1 ? 1 : G1_CSplit)
global G1_AutoC = IsPriorG1AutoC ? G1_AutoC : AutoC
global G1_CPreview = default(CPreview, !IsPriorG1 ? 0 : G1_CPreview)
global G1_CLine = default(CLine, !IsPriorG1 ? CR_CLine(G1_Source_Width, G1_Source_Height) : G1_CLine)
global G1_InDAR = default(InDAR, (InSAR > 0) || !IsPriorG1 ? 0.0 : G1_InDAR)
global G1_InSAR = default(InSAR, (InDAR > 0) || !IsPriorG1 ? 0.0 : G1_InSAR)
global G1_OutDAR = default(OutDAR, (OutSAR > 0) || !IsPriorG1 ? 0.0 : G1_OutDAR)
global G1_OutSAR = default(OutSAR, (OutDAR > 0) || !IsPriorG1 ? 0.0 : G1_OutSAR)
global G1_AutoDAR = default(AutoDAR, !IsPriorG1 ? CR_AutoDAR() : G1_AutoDAR)
global G1_Mod = default(Mod, !IsPriorG1 ? 4 : G1_Mod)
global G1_HMod = default(HMod, IsMod ? Mod : !IsPriorG1 ? G1_Mod : G1_HMod)
global G1_NoResize = default(NoResize, !IsPriorG1 ? false : G1_NoResize)
global G1_ResizeWO = default(ResizeWO, !IsPriorG1 ? false : G1_ResizeWO)
global G1_Borders = default(Borders, !IsPriorG1 ? false : G1_Borders)
global G1_CleanBorders = default(CleanBorders, !IsPriorG1 ? false : G1_CleanBorders)
global G1_BColor = default(BColor, !IsPriorG1 ? $000000 : G1_BColor)
global G1_Frosty = default(Frosty, !IsPriorG1 ? false : G1_Frosty)
global G1_ColorConvert = default(ColorConvert, !IsPriorG1 ? CR_ColorConvert() : G1_ColorConvert)
global G1_ColorMode = default(ColorMode, !IsPriorG1 ? "" : G1_ColorMode)
global G1_Resizer = default(Resizer, !IsPriorG1 ? CR_Resizer_Default() : G1_Resizer)
global G1_RStr = default(RStr, IsResizer ? "" : !IsPriorG1 ? CR_RStr_Default() : G1_RStr)
global G1_RingRepair = default(RingRepair, !IsPriorG1 ? false : G1_RingRepair)
global G1_PicDim = default(PicDim, !IsPriorG1 ? CR_Info_AutoPicDim() : G1_PicDim)
global G1_Position = default(Position, !IsPriorG1 ? false : G1_Position)
global G1_Info = default(Info, !IsPriorG1 ? false : G1_Info)

""", "GCropResize Defaults - GMode 1 Eval") : Eval("""

# -------------------------------------------------------------------------------
#            GMode2
# -------------------------------------------------------------------------------

Assert(IsPriorG1, GC_R + " For the first instance of GCropResize, GMode must equal one " + chr(10))
Try{  IsPriorG2 = G2_PriorG2  }Catch(err){  IsPriorG2 = false  }

IsPriorG2AutoC = IsPriorG2 && G2_AutoC
global G2_Source_Width = AutoC || !IsPriorG2AutoC ? width(Source) : G2_Source_Width
global G2_Source_Height = AutoC || !IsPriorG2AutoC ? height(Source) : G2_Source_Height
global G2_PriorG2 = true

Assert(!AutoC || !IsPriorG2AutoC, GC_R + " AutoC cannot be true for GCropResize(GMode=2) " + chr(10) + \
" after being enabled by a previous instance of GCropResize(GMode=2) " + chr(10))

global G2_OutWidth = (OutWidth == 0) ? (!IsPriorG2 ? G1_OutWidth : G2_OutWidth) : OutWidth
global G2_OutHeight = (OutHeight == 0) ? (!IsPriorG2 ? G1_OutHeight : G2_OutHeight) : OutHeight
global G2_CL = default(CL, !IsPriorG2 ? 0.0 : G2_CL)
global G2_CT = default(CT, !IsPriorG2 ? 0.0 : G2_CT)
global G2_CR = default(CR, !IsPriorG2 ? 0.0 : G2_CR)
global G2_CB = default(CB, !IsPriorG2 ? 0.0 : G2_CB)
global G2_CropDAR = default(CropDAR, !IsPriorG2 ? 0.0 : G2_CropDAR)
global G2_CSplit = default(CSplit, !IsPriorG2 ? 1 : G2_CSplit)
global G2_AutoC = IsPriorG2AutoC ? G2_AutoC : AutoC
global G2_CPreview = default(CPreview, !IsPriorG2 ? ((G1_CPreview == 4) ? 0 : G1_CPreview) : G2_CPreview)
global G2_CLine = default(CLine, !IsPriorG2 ? CR_CLine(G2_Source_Width, G2_Source_Height) : G2_CLine)
global G2_InDAR = default(InDAR, (InSAR > 0) || !IsPriorG2 ? 0.0 : G2_InDAR)
global G2_InSAR = default(InSAR, (InDAR > 0) || !IsPriorG2 ? 0.0 : G2_InSAR)
global G2_OutDAR = default(OutDAR, (OutSAR > 0) ? 0.0 : !IsPriorG2 ? G1_OutDAR : G2_OutDAR)
global G2_OutSAR = default(OutSAR, (OutDAR > 0) ? 0.0 : !IsPriorG2 ? G1_OutSAR : G2_OutSAR)
global G2_AutoDAR = default(AutoDAR, !IsPriorG2 ? G1_AutoDAR : G2_AutoDAR)
global G2_Mod = default(Mod, !IsPriorG2 ? G1_Mod : G2_Mod)
global G2_HMod = default(HMod, IsMod ? Mod : !IsPriorG2 ? G1_HMod : G2_HMod)
global G2_NoResize = default(NoResize, !IsPriorG2 ? false : G2_NoResize)
global G2_ResizeWO = default(ResizeWO, !IsPriorG2 ? false : G2_ResizeWO)
global G2_Borders = default(Borders, !IsPriorG2 ? false : G2_Borders)
global G2_CleanBorders = default(CleanBorders, !IsPriorG2 ? false : G2_CleanBorders)
global G2_BColor = default(BColor, !IsPriorG2 ? $000000 : G2_BColor)
global G2_Frosty = default(Frosty, !IsPriorG2 ? false : G2_Frosty)
global G2_ColorConvert = default(ColorConvert, !IsPriorG2 ? CR_ColorConvert() : G2_ColorConvert)
global G2_ColorMode = default(ColorMode, !IsPriorG2 ? "" : G2_ColorMode)
global G2_Resizer = default(Resizer, !IsPriorG2 ? G1_Resizer : G2_Resizer)
global G2_RStr = default(RStr, IsResizer ? "" : !IsPriorG2 ? G1_RStr : G2_RStr)
global G2_RingRepair = default(RingRepair, !IsPriorG2 ? G1_RingRepair : G2_RingRepair)
global G2_PicDim = default(PicDim, !IsPriorG2 ? G1_PicDim : G2_PicDim)
global G2_Position = default(Position, !IsPriorG2 ? false : G2_Position)
global G2_Info = default(Info, !IsPriorG2 ? G1_Info : G2_Info)

""", "GCropResize Defaults - GMode 2 Eval")

# -------------------------------------------------------------------------------
#            Global Auto Cropping
# -------------------------------------------------------------------------------

AutoC && (CPreview != 4) ? Eval("""

AutoCVideo = \
Source.autocrop(mode=0, wMultOf=2, hMultOf=2, leftadd=0, topadd=0, rightadd=0, bottomadd=0, \
threshold=CThresh, samples=CSample, samplestartframe=CStart, sampleendframe=-1, aspect=0)

""") : (CPreview == 4) ? Eval("""

H = height(Source)
Size = min(width(Source) * 0.04, H * ((H < 300) ? 0.044 : (H < 400) ? 0.042 : \
(H < 500) ? 0.036 : (H < 600) ? 0.032 : (H < 1100) ? 0.028 : 0.026))

AutoCPreviewVideo = \
Source.autocrop(mode=1, wMultOf=2, hMultOf=2, leftadd=0, topadd=0, rightadd=0, bottomadd=0, \
threshold=CThresh, samples=CSample, samplestartframe=CStart, sampleendframe=-1, aspect=0)\
.subtitle("Global AutoCrop Preview", y=(H*0.5)+(Size*0.25), size=Size, text_color=$FFA07A, align=5)

""") : nop()

# -------------------------------------------------------------------------------

return AutoC && (CPreview != 4) ? AutoCVideo : (CPreview == 4) ? AutoCPreviewVideo : Source  }

# ===============================================================================
# ===============================================================================
#            Color Convert & Resize Function
# ===============================================================================
# ===============================================================================

function CR_ColorConvertResize(clip Source, int CRVideo_Width, int CRVideo_Height, \
float RCL, float RCT, float RCR, float RCB, string CC, string CM, \
bool Is601NP, bool Is601709, bool Is601709NP, bool Is2020, bool Is2020NP, \
string Color_Format, bool IsRGBSource, bool IsAvsHBD, int Source_Bits, \
bool NoResizer, string Resizer, string ResStr, string RTaps, string DoResize, \
bool Upscale, bool IsRingRepair, bool IsDRingRepair, float Weight, bool IsStacks, \
bool IsResampleMT, bool IsZResize, bool IsDResize, bool IsFResize, string C_R)  {

# ===============================================================================

Assert(!IsRGBSource, C_R + " RGB isn't supported for color conversion " + chr(10))
Assert(!IsYV411(Source) || (CC == "DitherTools"), C_R + \
" Color conversion of YV411 is only supported using DitherTools " + chr(10))
Assert((IsAvsHBD && !IsY(Source)) || !IsY8(Source), C_R + \
" Video without chroma doesn't require color conversion " + chr(10))

Input = !IsYUY2(Source) ? Source : Source.ConvertToYV16()

DoRingRepair = ".CR_RingRepair(CConverted, CRVideo_Width, CRVideo_Height, \
RCL, RCT, RCR, RCB, IsResampleMT, IsZResize, IsFResize, Weight)"

To16Bit = (Source_Bits == 16) ? "" : IsFResize ? ".fmtc_bitdepth(bits=16)" : ".ConvertBits(16)"

Undo16Bit = (Source_Bits == 16) ? "" : IsFResize ? ".fmtc_bitdepth(bits=Source_Bits)" : \
".ConvertBits(Source_Bits, dither=(Source_Bits == 32) ? -1 : 1)"

(CC == "AVSResize") ? Eval("""

# ===============================================================================
#            AVSResize
# ===============================================================================

Assert(IsAvsHBD, C_R + " Avisynth+ is required for color conversion with AVSResize " + chr(10))

ColorMode = \
(CM == "601N-601P") ? "170m:601:170m:full=>470bg:601:470bg:full" : \
(CM == "601P-601N") ? "470bg:601:470bg:full=>170m:601:170m:full" : \
(CM == "601N-709") ? "170m:601:170m:full=>709:709:709:full" : \
(CM == "601P-709") ? "470bg:601:470bg:full=>709:709:709:full" : \
(CM == "601-709") ? "470bg:601:470bg:full=>709:601:470bg:full" : \
(CM == "601N-2020") ? "170m:601:170m:full=>2020:2020:2020:full" : \
(CM == "601-2020") || (CM == "601P-2020") ? "470bg:601:470bg:full=>2020:2020:2020:full" : \
(CM == "709-601N") ? "709:709:709:full=>170m:601:170m:full" : \
(CM == "709-601P") ? "709:709:709:full=>170m:601:170m:full" : \
(CM == "709-601") ? "709:709:709:full=>470bg:709:709:full" : \
(CM == "709-2020") ? "709:709:709:full=>2020:2020:2020:full" : \
(CM == "2020-601N") ? "2020:2020:2020:full=>170m:601:170m:full" : \
(CM == "2020-601P") || (CM == "2020-601") ? "2020:2020:2020:full=>470bg:601:470bg:full" : \
(CM == "2020-709") ? "2020:2020:2020:full=>709:709:709:full" : nop()

CConvert = ".z_ConvertFormat(colorspace_op=ColorMode)"

CConverted = NoResizer || (IsRingRepair && !IsFResize) ? Eval("Input" + To16Bit + CConvert + Undo16Bit) : \
IsRingRepair ? Eval("Input" + To16Bit + CConvert) : nop()

CCOut = NoResizer ? CConverted : \
IsRingRepair ? (!IsFResize ? Eval("CConverted" + DoResize + DoRingRepair) : \
Eval("CConverted" + DoResize + DoRingRepair + Undo16Bit)) : \
!IsFResize ? (UpScale ? Eval("Input" + To16Bit + CConvert + Undo16Bit + DoResize) : \
Eval("Input" + DoResize + To16Bit + CConvert + Undo16Bit)) : \
UpScale ? Eval("Input" + To16Bit + CConvert + DoResize + Undo16Bit) : \
Eval("Input" + To16Bit + DoResize + CConvert + Undo16Bit)

""", "Color Convert - AVSResize Eval") : (CC == "ColorMatrix") ? Eval("""

# ===============================================================================
#            ColorMatrix
# ===============================================================================

Assert(IsYV12(Source) || IsYUY2(Source) || IsYV16(Source), C_R + \
" Color conversion with ColorMatrix only supports YV12, YUY2 & YV16 " + chr(10))
Assert(!Is601NP && !Is601709NP && !Is2020NP, C_R + \
" When either 601N or 601P are specified as part of a " + chr(10) + \
" color conversion, ColorConvert cannot equal " + chr(34) + CC + chr(34) + " " + chr(10))
Assert(Is601709, C_R + \
" ColorMatrix can only be used for color conversions " + chr(10) + " between Rec.601 and Rec.709 " + chr(10))

CMode = (CM == "601-709") ? "Rec.601->Rec.709" : "Rec.709->Rec.601"
CConvert = ".ColorMatrix(mode=CMode, clamp=0)"
ToYUY2 = ".ConvertToYUY2()"
ToYV16 = ".ConvertToYV16()"

CConverted = (NoResizer || IsRingRepair) && IsYV16(Source) ? Eval("Source" + ToYUY2 + CConvert + ToYV16) : \
NoResizer || (IsRingRepair && IsYV12(Source)) ? Eval("Source" + CConvert) : \
IsRingRepair ? Eval("Source" + CConvert + ToYV16) : nop()

CConverted = \
NoResizer || (IsRingRepair && !IsFResize) ? CConverted : IsRingRepair ? Eval("CConverted" + To16Bit) : nop()

CCOut = NoResizer ? CConverted : \
IsRingRepair ? (IsYUY2(Source) ? (!IsFResize ? Eval("CConverted" + DoResize + DoRingRepair + ToYUY2) : \
Eval("CConverted" + DoResize + DoRingRepair + Undo16Bit + ToYUY2)) : \
!IsFResize ? Eval("CConverted" + DoResize + DoRingRepair) : \
Eval("CConverted" + DoResize + DoRingRepair + Undo16Bit)) : \
(IsYV12(Source) || (IsYUY2(Source) && !IsZResize && !IsDResize)) && !IsFResize ? \
(UpScale ? Eval("Source" + CConvert + DoResize) : Eval("Source" + DoResize + CConvert)) : \
IsYUY2(Source) && !IsFResize ? (UpScale ? Eval("Source" + CConvert + ToYV16 + DoResize + ToYUY2) : \
Eval("Source" + ToYV16 + DoResize + CConvert + ToYUY2)) : \
IsYV16(Source) && !IsFResize ? (UpScale ? Eval("Source" + ToYUY2 + CConvert + ToYV16 + DoResize) : \
Eval("Source" + DoResize + ToYUY2 + CConvert + ToYV16)) : \
IsYV12(Source) ? (UpScale ? Eval("Source" + CConvert + To16Bit + DoResize + Undo16Bit) : \
Eval("Source" + To16Bit + DoResize + Undo16Bit + CConvert)) : \
IsYUY2(Source) ? (UpScale ? Eval("Source" + CConvert + ToYV16 + To16Bit + DoResize + Undo16Bit + ToYUY2) : \
Eval("Source" + ToYV16 + To16Bit + DoResize + Undo16Bit + CConvert + ToYUY2)) : \
UpScale ? Eval("Source" + ToYUY2 + CConvert + ToYV16 + To16Bit + DoResize + Undo16Bit) : \
Eval("Source" + To16Bit + DoResize + Undo16Bit + ToYUY2 + CConvert + ToYV16)

""", "Color Convert - ColorMatrix Eval") : (CC == "DitherTools") ? Eval("""

# ===============================================================================
#            DitherTools
# ===============================================================================

Assert((Source_Bits == 8), C_R + " Color conversion with DitherTools only supports 8 bit YUV " + chr(10))
Assert(!Is601NP && !Is601709NP && !Is2020NP, C_R + \
" When either 601N or 601P are specified as part of a " + chr(10) + \
" color conversion, ColorConvert cannot equal " + chr(34) + CC + chr(34) + " " + chr(10))
Assert(Is601709, C_R + \
" DitherTools can only be used for color conversions " + chr(10) + " between Rec.601 and Rec.709 " + chr(10))

PType = PixelType(Input)
MatrixS = (CM == "601-709") ? "601" : "709"
MatrixD = (CM == "601-709") ? "709" : "601"
MatrixS8 = ".Dither_convert_yuv_to_rgb(matrix=MatrixS, lsb_in=false)"
MatrixD8 = ".Dither_convert_rgb_to_yuv(matrix=MatrixD, output=PType, lsb=false, slice=false, mode=6)"

DResize16 = ".Dither_resize16" + (IsDRingRepair ? "nr" : "") + "(CRVideo_Width, CRVideo_Height, \
src_left=RCL, src_top=RCT, src_width=-RCR, src_height=-RCB, kernel=Resizer" + RTaps + ResStr + ")"

CConverted = NoResizer || (IsRingRepair && !IsFResize) ? Eval("Input" + MatrixS8 + MatrixD8) : \
IsRingRepair ? Eval("Input" + MatrixS8 + MatrixD8 + To16Bit) : nop()

CCOut = NoResizer ? CConverted : \
IsRingRepair ? (!IsFResize ? Eval("CConverted" + DoResize + DoRingRepair) : \
Eval("CConverted" + DoResize + DoRingRepair + Undo16Bit)) : \
IsDResize ? (UpScale ? \
Eval("Input" + MatrixS8 + ".Dither_convert_rgb_to_yuv(matrix=MatrixD, output=PType, lsb=true)" + \
DResize16 + ".DitherPost(mode=6, slice=false)") : \
Eval("Input.Dither_convert_8_to_16()" + DResize16 + \
".Dither_convert_yuv_to_rgb(matrix=MatrixS, lsb_in=true)" + MatrixD8)) : \
!IsFResize ? (UpScale ? Eval("Input" + MatrixS8 + MatrixD8 + DoResize) : \
Eval("Input" + DoResize + MatrixS8 + MatrixD8)) : \
UpScale ? Eval("Input" + MatrixS8 + MatrixD8 + To16Bit + DoResize + Undo16Bit) : \
Eval("Input" + To16Bit + DoResize + Undo16Bit + MatrixS8 + MatrixD8)

""", "Color Convert - DitherTools Eval") : (CC == "FMTConv") ? Eval("""

# ===============================================================================
#            FMTConv
# ===============================================================================

Assert(IsAvsHBD, C_R + " Avisynth+ is required for color conversion with FMTConv " + chr(10))

MatrixS = (CM == "601-709") || (CM == "601-2020") || (CM == "601N-601P") || (CM == "601N-709") || \
(CM == "601N-2020") || (CM == "601P-601N") || (CM == "601P-709") || (CM == "601P-2020") ? "601" : \
(CM == "709-601") || (CM == "709-601N") || (CM == "709-601P") || (CM == "709-2020") ? "709" : \
(CM == "2020-601") || (CM == "2020-601N") || (CM == "2020-601P") || (CM == "2020-709") ? "2020" : nop()

MatrixD = (CM == "709-601") || (CM == "2020-601") || (CM == "601P-601N") || (CM == "709-601N") || \
(CM == "2020-601N") || (CM == "601N-601P") || (CM == "709-601P") || (CM == "2020-601P") ? "601" : \
(CM == "601-709") || (CM == "601N-709") || (CM == "601P-709") || (CM == "2020-709") ? "709" : \
(CM == "601-2020") || (CM == "601N-2020") || (CM == "601P-2020") || (CM == "709-2020") ? "2020" : nop()

PrimaryS = Is601709 ? "" : (CM == "601N-601P") || (CM == "601N-709") || (CM == "601N-2020") ? "601-525" : \
(CM == "601P-601N") || (CM == "601P-709") || (CM == "601-2020") || (CM == "601P-2020") ? "601-625" : \
(CM == "709-601N") || (CM == "709-601P") || (CM == "709-2020") ? "709" : \
(CM == "2020-601") || (CM == "2020-601N") || (CM == "2020-601P") || (CM == "2020-709") ? "2020" : nop()

PrimaryD = Is601709 ? "" : (CM == "601P-601N") || (CM == "709-601N") || (CM == "2020-601N") ? "601-525" : \
(CM == "601N-601P") || (CM == "709-601P") || (CM == "2020-601") || (CM == "2020-601P") ? "601-625" : \
(CM == "601N-709") || (CM == "601P-709") || (CM == "2020-709") ? "709" : \
(CM == "601-2020") || (CM == "601N-2020") || (CM == "601P-2020") || (CM == "709-2020") ? "2020" : nop()

TwoZero = "420"
TwoTwo = "422"
FourFour = "444"
TransferLinear = "Linear"

CConvert = (Is444(Input) ? "" : ".fmtc_resample(css=FourFour)") + \
(Is601709 ? ".fmtc_matrix(mats=MatrixS, matd=MatrixD, fulls=false, fulld=false, bits=16)" : \
".fmtc_matrix(mat=MatrixS, fulls=false, fulld=true, bits=16)\
.fmtc_transfer(transs=MatrixS, transd=TransferLinear)\
.fmtc_primaries(prims=PrimaryS, primd=PrimaryD)\
.fmtc_transfer(transs=TransferLinear, transd=MatrixD)\
.fmtc_matrix(mat=MatrixD, fulls=true, fulld=false, bits=16)") + \
(Is444(Input) ? "" : Is422(Input) ? ".fmtc_resample(css=TwoTwo)" : ".fmtc_resample(css=TwoZero)")

CConverted = NoResizer || (IsRingRepair && !IsFResize) ? Eval("Input" + To16Bit + CConvert + Undo16Bit) : \
IsRingRepair ? Eval("Input" + To16Bit + CConvert) : nop()

CCOut = NoResizer ? CConverted : \
IsRingRepair ? (!IsFResize ? Eval("CConverted" + DoResize + DoRingRepair) : \
Eval("CConverted" + DoResize + DoRingRepair + Undo16Bit)) : \
!IsFResize ? (UpScale ? Eval("Input" + To16Bit + CConvert + Undo16Bit + DoResize) : \
Eval("Input" + DoResize + To16Bit + CConvert + Undo16Bit)) : \
UpScale ? Eval("Input" + To16Bit + CConvert + DoResize + Undo16Bit) : \
Eval("Input" + To16Bit + DoResize + CConvert + Undo16Bit)

""", "Color Convert - FMTConv Eval") : (CC == "HDRMatrix") ? Eval("""

# ===============================================================================
#            HDRMatrix
# ===============================================================================

Assert(!Is601NP && !Is601709NP && !Is2020NP, C_R + \
" When either 601N or 601P are specified as part of a " + chr(10) + \
" color conversion, ColorConvert cannot equal " + chr(34) + CC + chr(34) + " " + chr(10))
Assert(Is601709, C_R + " HDRMatrix can only be used for color conversions " + chr(10) + \
" between Rec.601 and Rec.709 " + chr(10))
Assert(IsStacks || (Source_Bits == 8), C_R + " ConvertStacked.dll is required for color conversion of " + \
chr(10) + " high bit depth video with HDRMatrix " + chr(10))

NoFunc = "there is no function named"
IsHDRCore = IsAvsHBD ? FunctionExists("Bitdepth") : \
Eval(" Try{ Bitdepth(xx=0) }Catch(HCore_err){ (FindStr(LCase(HCore_err), NoFunc) == 0) } ")

Bits = IsStacks || IsHDRCore ? 16 : 8

CConvert = (IsStacks ? ".ConvertToDoubleWidth()" : IsHDRCore ? ".Bitdepth(From=8, To=16)" : "") + \
((CM == "601-709") ? \
".Matrix(From=601, To=709, Bitdepth=Bits)" : ".Matrix(From=709, To=601, Bitdepth=Bits)") + \
(IsStacks ? ".ConvertFromDoubleWidth()" : IsHDRCore ? ".Bitdepth(From=16, To=8)" : "")

CConverted = IsStacks && (NoResizer || (IsRingRepair && !IsFResize)) ? \
Eval("Input" + To16Bit + CConvert + Undo16Bit) : \
IsStacks && IsRingRepair ? Eval("Input" + To16Bit + CConvert) : \
NoResizer || (IsRingRepair && !IsFResize) ? Eval("Input" + CConvert) : \
IsRingRepair ? Eval("Input" + CConvert + To16Bit) : nop()

CCOut = NoResizer ? CConverted : \
IsRingRepair ? (!IsFResize ? Eval("CConverted" + DoResize + DoRingRepair) : \
Eval("CConverted" + DoResize + DoRingRepair + Undo16Bit)) : \
IsStacks ? (!IsFResize ? (UpScale ? Eval("Input" + To16Bit + CConvert + Undo16Bit + DoResize) : \
Eval("Input" + DoResize + To16Bit + CConvert + Undo16Bit)) : \
(UpScale ? Eval("Input" + To16Bit + CConvert + DoResize + Undo16Bit) : \
Eval("Input" + To16Bit + DoResize + CConvert + Undo16Bit))) : \
!IsFResize ? (UpScale ? Eval("Input" + CConvert + DoResize) : Eval("Input" + DoResize + CConvert)) : \
UpScale ? Eval("Input" + CConvert + To16Bit + DoResize + Undo16Bit) : \
Eval("Input" + To16Bit + DoResize + Undo16Bit + CConvert)

""", "Color Correct - HDRMatrix Eval") : (CC == "HDRTools") ? Eval("""

# ===============================================================================
#            HDRTools
# ===============================================================================

Assert(IsAvsHBD, C_R + " Avisynth+ is required for color conversion with HDRTools " + chr(10))

ColorA = (CM == "2020-601") || (CM == "2020-601N") || (CM == "2020-601P") || (CM == "2020-709") ? 1 : \
(CM == "709-601") || (CM == "709-601N") || (CM == "709-601P") || (CM == "709-2020") ? 2 : \
(CM == "601N-601P") || (CM == "601N-709") || (CM == "601N-2020") ? 3 : \
(CM == "601-709") || (CM == "601-2020") || (CM == "601P-601N") || \
(CM == "601P-709") || (CM == "601P-2020") ? 4 : nop()

ColorB = (CM == "601-2020") || (CM == "601N-2020") || (CM == "601P-2020") || (CM == "709-2020") ? 1 : \
(CM == "601-709") || (CM == "601N-709") || (CM == "601P-709") || (CM == "2020-709") ? 2 : \
(CM == "601P-601N") || (CM == "709-601N") || (CM == "2020-601N") ? 3 : \
(CM == "709-601") || (CM == "2020-601") || (CM == "601N-601P") || \
(CM == "709-601P") || (CM == "2020-601P") ? 4 : nop()

CConvert = (Is444(Source) ? "" : ".ConvertToYUV444()") + \
".ConvertYUVtoXYZ(Color=ColorA).ConvertXYZtoYUV(Color=ColorB, PColor=ColorA)" + \
(Is444(Source) ? "" : Is422(Source) ? ".ConvertToYUV422()" : ".ConvertToYUV420()")

CConverted = NoResizer || (IsRingRepair && !IsFResize) ? Eval("Input" + To16Bit + CConvert + Undo16Bit) : \
IsRingRepair ? Eval("Input" + To16Bit + CConvert) : nop()

CCOut = NoResizer ? CConverted : \
IsRingRepair ? (!IsFResize ? Eval("CConverted" + DoResize + DoRingRepair) : \
Eval("CConverted" + DoResize + DoRingRepair + Undo16Bit)) : \
!IsFResize ? (UpScale ? Eval("Input" + To16Bit + CConvert + Undo16Bit + DoResize) : \
Eval("Input" + DoResize + To16Bit + CConvert + Undo16Bit)) : \
UpScale ? Eval("Input" + To16Bit + CConvert + DoResize + Undo16Bit) : \
Eval("Input" + To16Bit + DoResize + CConvert + Undo16Bit)

""", "Color Convert - HDRTools Eval") : nop()

# ===============================================================================

CCOut = (CC == "ColorMatrix") || !IsYUY2(Source) ? CCOut : CCOut.ConvertToYUY2()

Assert((Color_Format == PixelType(CCOut)), C_R + \
" Source=" + Color_Format + "  Output=" + PixelType(CCOut) + " " + chr(10) + \
" Failed to convert back to the original format after " + CC + " color conversion " + chr(10))

return CCOut  }

# ===============================================================================
# ===============================================================================
#            Global FCropResize Function
# ===============================================================================
# ===============================================================================

function FCropResize(clip Source, \
bool "Frosty", int "FrostyStatic", int "Clone", bool "FrostyGradient", bool "FrostyPlain", bool "Plain", \
val "BColor", int "BDivider", val "BDColor", int "Feather", \
float "Bright", float "Cont", float "Sat", float "Blend", float "Blur", float "VBlur", int "Iterations", \
int "GPlugin", float "Texture", float "TextureC", int "TSoft", bool "Faster")  {

global G_CRFB = true
global CRFB_Frosty = default(Frosty, true)
global CRFB_FrostyStatic = default(FrostyStatic, undefined())
global CRFB_Clone = default(Clone, 0)
global CRFB_FrostyGradient = default(FrostyGradient, false)
global CRFB_FrostyPlain = default(FrostyPlain, false)
global CRFB_Plain = default(Plain, false)
global CRFB_BColor = default(BColor, undefined())
global CRFB_BDivider = default(BDivider, 0)
global CRFB_BDColor = default(BDColor, $000000)
global CRFB_Feather = default(Feather, 0)
global CRFB_Bright = default(Bright, undefined())
global CRFB_Cont = default(Cont, undefined())
global CRFB_Sat = default(Sat, undefined())
global CRFB_Blend = default(Blend, undefined())
global CRFB_Blur = default(Blur, undefined())
global CRFB_VBlur = default(VBlur, undefined())
global CRFB_Iterations = default(Iterations, 3)
global CRFB_GPlugin = default(GPlugin, 1)
global CRFB_Texture = default(Texture, undefined())
global CRFB_TextureC = default(TextureC, undefined())
global CRFB_TSoft = default(TSoft, undefined())
global CRFB_Faster = default(Faster, false)

return Source  }

# ===============================================================================
# ===============================================================================
#            Frosty Borders Function
# ===============================================================================
# ===============================================================================

function CR_FrostyBorders(clip CRVideo, \
int CRVWidth, int CRVHeight, float F_CRVWidth, float F_CRVHeight, \
int BL, int BT, int BR, int BB, int WidthMod, int HeightMod, float F_WidthMod, float F_HeightMod, \
float CRVideo_StorageAR, float Final_StorageAR, bool IsResampleMT)  {

# -------------------------------------------------------------------------------
#            Defaults
# -------------------------------------------------------------------------------

Try{  IsCRFB = G_CRFB  }Catch(err){  IsCRFB = false  }
IsPillarBox = (Final_StorageAR >= CRVideo_StorageAR)
TotalFrames = FrameCount(CRVideo)
LastFrame = TotalFrames - 1
F_VidSize = IsPillarBox ? F_CRVHeight : F_CRVWidth
CR_FB = " CropResize FrostyBorders " + chr(10)

Frosty = IsCRFB ? CRFB_Frosty : true
IsFrostyStatic = IsCRFB && defined(CRFB_FrostyStatic)
FrostyStatic = IsFrostyStatic ? CRFB_FrostyStatic : 0
Clone = IsCRFB ? CRFB_Clone : 0
Clone1 = (Clone == 1)
Clone2 = (Clone == 2)
Clone3 = (Clone == 3)
FrostyGradient = IsCRFB ? CRFB_FrostyGradient : false
FrostyPlain = IsCRFB ? CRFB_FrostyPlain : false
Plain = IsCRFB ? CRFB_Plain : false
BDivider = IsCRFB ? CRFB_BDivider : 0
BDColor = IsCRFB ? CRFB_BDColor : $000000
Feather = IsCRFB ? CRFB_Feather : 0
IsBlur = IsCRFB && defined(CRFB_Blur)
IsVBlur = IsCRFB && defined(CRFB_VBlur)
Iterations = IsCRFB ? CRFB_Iterations : 3
GPlugin = IsCRFB ? CRFB_GPlugin : 1
Faster = IsCRFB ? CRFB_Faster : false

BorderMin = Frosty || IsFrostyStatic || FrostyGradient ? (IsPillarBox && (WidthMod == 4) ? 32 : 16) : \
Clone1 || Clone2 || Clone3 ? (IsPillarBox && (WidthMod == 4) ? 8 : 4) : IsPillarBox ? WidthMod : HeightMod

SizeTest = \
(IsPillarBox && (BL >= BorderMin <= BR)) || (!IsPillarBox && (BT >= BorderMin <= BB))
IsBDivider = \
(BDivider > 0) && ((IsPillarBox && (BL > BorderMin < BR)) || (!IsPillarBox && (BT > BorderMin < BB)))
IsFeather = \
(Feather > 0) && ((IsPillarBox && (BL >= WidthMod <= BR)) || (!IsPillarBox && (BT >= HeightMod <= BB)))

FrostyPlain = FrostyPlain && !Plain
FrostyGradient = FrostyGradient && !FrostyPlain && !Plain && SizeTest
Clone1 = Clone1 || (Clone2 && !IsFeather) && !FrostyGradient && !FrostyPlain && !Plain && SizeTest
Clone2 = Clone2 && IsFeather && !FrostyGradient && !FrostyPlain && !Plain && SizeTest
Clone3 = Clone3 && !FrostyGradient && !FrostyPlain && !Plain && SizeTest
IsClone = Clone1 || Clone2 || Clone3
IsFrostyStatic = IsFrostyStatic && !FrostyGradient && !FrostyPlain && !Plain && !IsClone && SizeTest
Frosty = Frosty && !FrostyGradient && !FrostyPlain && !Plain && !IsClone && SizeTest
IsFrosty = Frosty || IsFrostyStatic
Plain = Plain || (!FrostyGradient && !FrostyPlain && !IsClone && !IsFrosty)
IsPlain = Plain || FrostyPlain

Feather = !IsFeather ? 0 : IsPillarBox ? \
min((Feather < WidthMod) ? WidthMod : Feather, floor(F_CRVWidth / 6.0 / F_WidthMod) * WidthMod) : \
min((Feather < HeightMod) ? HeightMod : Feather, floor(F_CRVHeight / 6.0 / F_HeightMod) * HeightMod)

BColor = IsCRFB && defined(CRFB_BColor) ? CRFB_BColor : FrostyGradient || FrostyPlain ? $008080 : $000000
Bright = IsCRFB && defined(CRFB_Bright) ? CRFB_Bright : IsPillarBox || Plain ? 0.0 : -25.0
Cont = IsCRFB && defined(CRFB_Cont) ? CRFB_Cont : IsPillarBox || Plain ? 1.0 : 0.66
Sat = IsCRFB && defined(CRFB_Sat) ? CRFB_Sat : Plain ? 1.0 : IsPillarBox ? 0.85 : 0.66
Blend = IsCRFB && defined(CRFB_Blend) ? CRFB_Blend : IsFrostyStatic ? 0.5 : IsFrosty ? 0.2 : 0.0

Blur = \
IsBlur ? ((CRFB_Blur >= 0) ? CRFB_Blur : -CRFB_Blur * F_VidSize / 720.0) : \
Clone1 || Clone2 ? F_VidSize / 75.0 : Clone3 ? F_VidSize / 25.0 : 0.0
VBlur = \
IsVBlur ? ((CRFB_VBlur >= 0) ? CRFB_VBlur : -CRFB_VBlur * F_VidSize / 720.0) : \
IsBlur || IsClone ? Blur : 0.0

Texture = IsCRFB && defined(CRFB_Texture) ? CRFB_Texture : IsClone ? 2.5 : !Plain ? 5.0 : 0.0
Texture = (Texture == 0) || (GPlugin == 0) ? 0.0 : (GPlugin == 1) ? Texture : floor(Texture * 6.0) * 8
TextureC = IsCRFB && defined(CRFB_TextureC) ? CRFB_TextureC : 0.0
TextureC = (TextureC == 0) || (GPlugin == 0) ? 0.0 : (GPlugin == 1) ? TextureC : floor(TextureC * 6.0) * 8
TSoft = IsCRFB && defined(CRFB_TSoft) ? CRFB_TSoft : Frosty ? 5 : Clone3 ? 1 : 0
IsTSoft = (TSoft > 0) && (Frosty || IsClone)

# ===============================================================================
#            Insist
# ===============================================================================

Assert(!IsRGB(CRVideo), CR_FB + " RGB is not supported for FrostyBorders " + chr(10))

Assert((0 <= FrostyStatic <= LastFrame), CR_FB + \
" The valid range for FrostyStatic is from zero to the final frame number " + \
string(LastFrame, "(0 to %.0f) ") + chr(10))

Assert(!IsPillarBox || (BDivider <= 0) || (WidthMod != 2) || (BDivider % 2 == 0), CR_FB + \
" " + PixelType(CRVideo) + " requires a Mod2 BDivider for pillarbox borders " + chr(10))
Assert(!IsPillarBox || (BDivider <= 0) || (WidthMod != 4) || (BDivider % 4 == 0), CR_FB + \
" " + PixelType(CRVideo) + " requires a Mod4 BDivider for pillarbox borders " + chr(10))
Assert(IsPillarBox || (BDivider <= 0) || ((HeightMod != 2) || (BDivider % 2 == 0)), CR_FB + \
" " + PixelType(CRVideo) + " requires a Mod2 BDivider for letterbox borders " + chr(10))

Assert((Feather == 0) || (BDivider <= 0), CR_FB + \
" Feather and BDivider cannot be used together (one must equal zero) " + chr(10))
Assert((Feather == 0) || !FrostyGradient, CR_FB + \
" Feather cannot be greater than zero when FrostyGradient=true " + chr(10))

Assert((0 <= GPlugin <= 2), CR_FB + " The valid choices for GPlugin are " + chr(10) + \
" 0 (no added grain), 1 (AddGrainC) or 2 (neo_f3kdb) " + chr(10))

Assert((0 <= Clone <= 3), CR_FB + " Clone can only be 0 (disabled), 1, 2 or 3 " + chr(10))
Assert((-255 <= Bright <= 255), CR_FB + " The valid range for Bright is -255 thru 255 " + chr(10))
Assert((0 <= Cont <= 10), CR_FB + " The valid range for Cont is 0.0 thru 10.0 " + chr(10))
Assert((0 <= Sat <= 10), CR_FB + " The valid range for Sat is 0.0 thru 10.0 " + chr(10))
Assert((0 <= Blend <= 1), CR_FB + " The valid range for Blend is 0.0 to 1.0 " + chr(10))
Assert((0 <= Texture) && (0 <= TextureC), CR_FB + " Texture and TextureC cannot be negative " + chr(10))
Assert((0 <= TSoft <= 7), CR_FB + " The valid range for TSoft is 0 to 7 " + chr(10))

# ===============================================================================
#            PillarBox Borders
# ===============================================================================

IsPillarBox ? Eval("""

BL = IsBDivider ? BL - BDivider : BL
BR = IsBDivider ? BR - BDivider : BR
FL = IsFeather ? max(BL, floor((BL + (Feather * 3)) / F_WidthMod) * WidthMod) : nop()
FR = IsFeather ? max(BR, floor((BR + (Feather * 3)) / F_WidthMod) * WidthMod) : nop()

# -------------------------------------------------------------------------------
#            PillarBox Strings
# -------------------------------------------------------------------------------

CRVideoStr = !IsBDivider ? ",CRVideo," : ",CRVideo.AddBorders(BDivider, 0, BDivider, 0, color=BDColor),"

FastBlurStr = (IsPlain && !IsFeather) || FrostyGradient ? "" : \
".FastBlur(Blur, y_blur=VBlur, iterations=Iterations, dither=true)"

FBordStr = ".AddBorders(0, 0, CRVWidth-FL+BL-FR+BR, 0),"
GaussMTStr = ".GaussResize" + (!IsResampleMT ? "" : "MT")

GaussLStr = !IsPlain ? GaussMTStr + "(BL, CRVHeight)" : nop()
GaussRStr = !IsPlain ? GaussMTStr + "(BR, CRVHeight)" : nop()
FGaussLStr = IsFeather ? GaussMTStr + "(FL, CRVHeight)" : nop()
FGaussRStr = IsFeather ? GaussMTStr + "(FR, CRVHeight)" : nop()

Gauss3LStr = Clone3 ? GaussMTStr + "(max(BL, floor(WidthCloneBorderL * RatioL / F_WidthMod) * WidthMod), \
max(CRVHeight, floor(CRVHeight * RatioL / F_HeightMod) * HeightMod))" : nop()
Gauss3RStr = Clone3 ? GaussMTStr + "(max(BR, floor(WidthCloneBorderR * RatioR / F_WidthMod) * WidthMod), \
max(CRVHeight, floor(CRVHeight * RatioR / F_HeightMod) * HeightMod))" : nop()

GrainStr = ((Texture == 0) && (TextureC == 0)) || (GPlugin == 0) ? "" : \
(GPlugin == 1) ? ".AddGrain(var=Texture, uvar=TextureC, constant=true)" : \
".neo_f3kdb(range=0, grainY=Texture, grainC=TextureC, dither_algo=1)"

LoopStr = ".Trim(1, 1).Loop(TotalFrames)"
SLoopStr = IsFrostyStatic ? ".Trim(FrostyStatic, end=FrostyStatic).Loop(TotalFrames)" : ""
TSoftStr = IsTSoft ? ".TemporalSoften(TSoft, 255, 255, 20, 2)" : ""
TweakStr = (Bright != 0.0) || (Cont != 1.0) || (Sat != 1.0) ? ".Tweak(bright=Bright, cont=Cont, sat=Sat)" : ""

TBStr = TweakStr + FastBlurStr
TBGStr = TweakStr + FastBlurStr + GrainStr

# -------------------------------------------------------------------------------
#            PillarBox Frosty Borders
# -------------------------------------------------------------------------------

FrostyCrop = IsFrosty && IsTSoft ? \
CRVWidth - max(BorderMin, round(F_CRVWidth / (!Faster ? 5.0 : 10.0) / F_WidthMod) * WidthMod) : nop()

FrostyLR = IsFrosty && IsTSoft ? \
Eval("StackHorizontal(CRVideo.crop(0, 0, -FrostyCrop, 0), CRVideo.crop(FrostyCrop, 0, 0, 0))" + TSoftStr) : nop()
WidthFrostyLR = IsFrosty && IsTSoft ? width(FrostyLR) : nop()

FrostyCrop = IsFrosty ? (IsTSoft ? WidthFrostyLR - \
max(BorderMin, round(float(WidthFrostyLR) / (!Faster ? 16.0 : 8.0) / F_WidthMod) * WidthMod) : \
CRVWidth - max(BorderMin, round(F_CRVWidth / 80.0 / F_WidthMod) * WidthMod)) : nop()

FrostyL = IsFrosty ? (IsTSoft ? FrostyLR : CRVideo).crop(0, 0, -FrostyCrop, 0) : nop()
FrostyR = IsFrosty ? (IsTSoft ? FrostyLR : CRVideo).crop(FrostyCrop, 0, 0, 0) : nop()

WidthFrostyL = IsFrosty ? width(FrostyL) : nop()
WidthFrostyR = IsFrosty ? width(FrostyR) : nop()

FrostyBlendL = IsFrosty ? ((Blend == 0) ? FrostyL : (Blend == 1) ? FrostyR : \
Overlay(FrostyL, FlipHorizontal(FrostyR), Opacity=Blend)) : nop()
FrostyBlendR = IsFrosty ? ((Blend == 0) ? FrostyR : (Blend == 1) ? FrostyL : \
Overlay(FrostyR, FlipHorizontal(FrostyL), Opacity=Blend)) : nop()

Blur = IsBlur ? Blur : IsFrosty ? max(WidthFrostyL, WidthFrostyR) + (!IsFeather ? 0 : Feather) : Blur
VBlur = IsVBlur ? VBlur : IsFrosty ? F_CRVHeight / 10.0 : VBlur

FrostyL = Frosty ? Eval("FrostyBlendL" + TBStr + GaussLStr + GrainStr + SLoopStr) : nop()
FrostyR = Frosty ? Eval("FrostyBlendR" + TBStr + GaussRStr + GrainStr + SLoopStr) : nop()

FFrostyL = Frosty && IsFeather ? Eval("FrostyBlendL" + TBStr + FGaussLStr + GrainStr + SLoopStr) : nop()
FFrostyR = Frosty && IsFeather ? Eval("FrostyBlendR" + TBStr + FGaussRStr + GrainStr + SLoopStr) : nop()

# -------------------------------------------------------------------------------
#            PillarBox Clone Borders
# -------------------------------------------------------------------------------

MaxCloneC = IsClone || (IsFrosty && IsFeather) ? floor(F_CRVWidth / 2.0 / F_WidthMod) * WidthMod : nop()
CloneCropL = IsClone || (IsFrosty && IsFeather) ? max(MaxCloneC, CRVWidth - BL) : nop()
CloneCropR = IsClone || (IsFrosty && IsFeather) ? max(MaxCloneC, CRVWidth - BR) : nop()
CloneBorderL = IsClone || (IsFrosty && IsFeather) ? CRVideo.crop(0, 0, -CloneCropL, 0) : nop()
CloneBorderR = IsClone || (IsFrosty && IsFeather) ? CRVideo.crop(CloneCropR, 0, 0, 0) : nop()
WidthCloneBorderL = IsClone || (IsFrosty && IsFeather) ? width(CloneBorderL) : nop()
WidthCloneBorderR = IsClone || (IsFrosty && IsFeather) ? width(CloneBorderR) : nop()

FlipCloneL = IsClone || (IsFrosty && IsFeather) ? \
FlipHorizontal(((WidthCloneBorderL == BL) ? CloneBorderL : Eval("CloneBorderL" + GaussLStr))) : nop()
FlipCloneR = IsClone || (IsFrosty && IsFeather) ? \
FlipHorizontal(((WidthCloneBorderR == BR) ? CloneBorderR : Eval("CloneBorderR" + GaussRStr))) : nop()

CloneLR = (Clone1 || Clone3) && IsTSoft ? Eval("StackHorizontal(CloneBorderL, CloneBorderR)" + TSoftStr) : nop()
WidthCloneLR = (Clone1 || Clone3) && IsTSoft ? width(CloneLR) : nop()

CloneL = \
Clone1 || Clone3 ? (IsTSoft ? CloneLR.crop(0, 0, -WidthCloneLR+WidthCloneBorderL, 0) : CloneBorderL) : nop()
CloneR = \
Clone1 || Clone3 ? (IsTSoft ? CloneLR.crop(WidthCloneLR-WidthCloneBorderR, 0, 0, 0) : CloneBorderR) : nop()

CloneBlendL = Clone1 || Clone3 ? ((Blend == 0) ? CloneL : (Blend == 1) ? CloneR : \
Overlay(CloneL, FlipHorizontal(CloneR), Opacity=Blend)) : nop()
CloneBlendR = Clone1 || Clone3 ? ((Blend == 0) ? CloneR : (Blend == 1) ? CloneL : \
Overlay(CloneR, FlipHorizontal(CloneL), Opacity=Blend)) : nop()

FCloneL = \
Clone1 && IsFeather ? ((WidthCloneBorderL == BL) ? CloneBlendL : Eval("CloneBlendL" + GaussLStr)) : nop()
FCloneR = \
Clone1 && IsFeather ? ((WidthCloneBorderR == BR) ? CloneBlendR : Eval("CloneBlendR" + GaussRStr)) : nop()

CloneL = Clone1 && !IsFeather ? ((WidthCloneBorderL == BL) ? \
Eval("CloneBlendL" + TBGStr) : Eval("CloneBlendL" + TBStr + GaussLStr + GrainStr)) : nop()
CloneR = Clone1 && !IsFeather ? ((WidthCloneBorderR == BR) ? \
Eval("CloneBlendR" + TBGStr) : Eval("CloneBlendR" + TBStr + GaussRStr + GrainStr)) : nop()

# -------------------------------------------------------------------------------
#            PillarBox Clone2 Borders
# -------------------------------------------------------------------------------

Clone2CropL = Clone2 ? max(MaxCloneC, CRVWidth - FL) : nop()
Clone2CropR = Clone2 ? max(MaxCloneC, CRVWidth - FR) : nop()
Clone2BL = Clone2 ? CRVideo.crop(0, 0, -Clone2CropL, 0) : nop()
Clone2BR = Clone2 ? CRVideo.crop(Clone2CropR, 0, 0, 0) : nop()
WidthClone2BL = Clone2 ? width(Clone2BL) : nop()
WidthClone2BR = Clone2 ? width(Clone2BR) : nop()

FlipClone2L = Clone2 ? FlipHorizontal(((WidthClone2BL == FL) ? Clone2BL : Eval("Clone2BL" + FGaussLStr))) : nop()
FlipClone2R = Clone2 ? FlipHorizontal(((WidthClone2BR == FR) ? Clone2BR : Eval("Clone2BR" + FGaussRStr))) : nop()

Clone2LR = Clone2 && IsTSoft ? Eval("StackHorizontal(Clone2BL, Clone2BR)" + TSoftStr) : nop()
WidthClone2LR = Clone2 && IsTSoft ? width(Clone2LR) : nop()

Clone2L = Clone2 ? (IsTSoft ? Clone2LR.crop(0, 0, -WidthClone2LR+WidthClone2BL, 0) : Clone2BL) : nop()
Clone2R = Clone2 ? (IsTSoft ? Clone2LR.crop(WidthClone2LR-WidthClone2BR, 0, 0, 0) : Clone2BR) : nop()

Clone2BlendL = Clone2 ? ((Blend == 0) ? Clone2L : (Blend == 1) ? Clone2R : \
Overlay(Clone2L, FlipHorizontal(Clone2R), Opacity=Blend)) : nop()
Clone2BlendR = Clone2 ? ((Blend == 0) ? Clone2R : (Blend == 1) ? Clone2L : \
Overlay(Clone2R, FlipHorizontal(Clone2L), Opacity=Blend)) : nop()

Clone2L = Clone2 ? ((WidthClone2BL == FL) ? Eval("Clone2BlendL" + TBGStr) : \
Eval("Clone2BlendL" + TBStr + FGaussLStr + GrainStr)) : nop()
Clone2R = Clone2 ? ((WidthClone2BR == FR) ? Eval("Clone2BlendR" + TBGStr) : \
Eval("Clone2BlendR" + TBStr + FGaussRStr + GrainStr)) : nop()

# -------------------------------------------------------------------------------
#            PillarBox Clone3 Borders
# -------------------------------------------------------------------------------

RatioL = Clone3 ? (WidthCloneBorderL - BL >= 0) ? 1.618 : float(BL) / float(WidthCloneBorderL) : nop()
RatioR = Clone3 ? (WidthCloneBorderR - BR >= 0) ? 1.618 : float(BR) / float(WidthCloneBorderR) : nop()

Clone3L = Clone3 ? Eval("CloneBlendL" + Gauss3LStr) : nop()
Clone3R = Clone3 ? Eval("CloneBlendR" + Gauss3RStr) : nop()
WidthClone3L = Clone3 ? width(Clone3L) : nop()
HeightClone3L = Clone3 ? height(Clone3L) : nop()
WidthClone3R = Clone3 ? width(Clone3R) : nop()
HeightClone3R = Clone3 ? height(Clone3R) : nop()

Clone3L_CL = Clone3 ? max(0, floor((WidthClone3L - BL) / F_WidthMod / 2.0) * WidthMod) : nop()
Clone3L_CR = Clone3 ? max(0, WidthClone3L - BL - Clone3L_CL) : nop()
Clone3L_CT = Clone3 ? max(0, floor((HeightClone3L - CRVHeight) / F_HeightMod / 2.0) * HeightMod) : nop()
Clone3L_CB = Clone3 ? max(0, HeightClone3L - CRVHeight - Clone3L_CT) : nop()
Clone3R_CL = Clone3 ? max(0, floor((WidthClone3R - BR) / F_WidthMod / 2.0) * WidthMod) : nop()
Clone3R_CR = Clone3 ? max(0, WidthClone3R - BR - Clone3R_CL) : nop()
Clone3R_CT = Clone3 ? max(0, floor((HeightClone3R - CRVHeight) / F_HeightMod / 2.0) * HeightMod) : nop()
Clone3R_CB = Clone3 ? max(0, HeightClone3R - CRVHeight - Clone3L_CT) : nop()

Clone3L = Clone3 ? Clone3L.crop(Clone3L_CL, Clone3L_CT, -Clone3L_CR, -Clone3L_CB) : nop()
Clone3R = Clone3 ? Clone3R.crop(Clone3R_CL, Clone3R_CT, -Clone3R_CR, -Clone3R_CB) : nop()

FClone3L = Clone3 && IsFeather ? Clone3L : nop()
FClone3R = Clone3 && IsFeather ? Clone3R : nop()

Clone3L = Clone3 && !IsFeather ? Eval("Clone3L" + TBGStr) : nop()
Clone3R = Clone3 && !IsFeather ? Eval("Clone3R" + TBGStr) : nop()

# -------------------------------------------------------------------------------
#            PillarBox FrostyGradient Borders
# -------------------------------------------------------------------------------

GradientLR = \
FrostyGradient ? BlankClip(CRVideo, width=88, height=CRVHeight).AddBorders(8, 0, 0, 0, color=BColor)\
.Tweak(bright=Bright, cont=Cont, sat=Sat).FastBlur(50, y_blur=0, iterations=3, dither=true) : nop()
GradientL = FrostyGradient ? Eval("FlipHorizontal(GradientLR)" + GaussLStr + GrainStr + LoopStr) : nop()
GradientR = FrostyGradient ? Eval("GradientLR" + GaussRStr + GrainStr + LoopStr) : nop()

# -------------------------------------------------------------------------------
#            PillarBox Plain Borders
# -------------------------------------------------------------------------------

PlainL = IsPlain && (BL >= WidthMod) ? \
Eval("BlankClip(CRVideo, width=BL, color=BColor)" + TweakStr + GrainStr + LoopStr) : nop()
PlainR = IsPlain && (BR >= WidthMod) ? \
Eval("BlankClip(CRVideo, width=BR, color=BColor)" + TweakStr + GrainStr + LoopStr) : nop()

PlainL2 = IsPlain && IsFeather ? Eval("BlankClip(CRVideo, width=BL, color=BColor)" + TweakStr + LoopStr) : nop()
PlainR2 = IsPlain && IsFeather ? Eval("BlankClip(CRVideo, width=BR, color=BColor)" + TweakStr + LoopStr) : nop()

# -------------------------------------------------------------------------------
#            PillarBox Frosty Video
# -------------------------------------------------------------------------------

FrostyBL = !IsFeather ? (IsFrosty ? FrostyL : Clone1 ? CloneL : Clone3 ? Clone3L : \
FrostyGradient ? GradientL : IsPlain && (BL >= WidthMod) ? PlainL : nop()) : \
IsFrosty || Clone1 || Clone2 || Clone3 ? FlipCloneL : IsPlain && (BL >= WidthMod) ? PlainL2 : nop()

FrostyBR = !IsFeather ? (IsFrosty ? FrostyR : Clone1 ? CloneR : Clone3 ? Clone3R : \
FrostyGradient ? GradientR :IsPlain && (BR >= WidthMod) ? PlainR : nop()) : \
IsFrosty || Clone1 || Clone2 || Clone3 ? FlipCloneR : IsPlain && (BR >= WidthMod) ? PlainR2 : nop()

FrostyOut = IsFrosty || IsClone || FrostyGradient || (IsPlain && (BL >= WidthMod <= BR)) ? \
Eval("StackHorizontal(FrostyBL" + CRVideoStr + "FrostyBR)") : \
IsPlain && (BL >= WidthMod) ? StackHorizontal(FrostyBL, CRVideo) : \
IsPlain && (BR >= WidthMod) ? StackHorizontal(CRVideo, FrostyBR) : CRVideo

# -------------------------------------------------------------------------------
#            PillarBox Feather Mask
# -------------------------------------------------------------------------------

FeatherMask = IsFeather ? BlankClip(CRVideo, width=CRVWidth-(Feather*2))\
.AddBorders(BL+Feather, 0, BR+Feather, 0, color=$FFFFFF).ColorYUV(Levels="TV->PC")\
.FastBlur(Feather/2.25, y_blur=0, iterations=3, dither=true) : nop()

FeatherMask = IsFeather ? Eval("FeatherMask" + LoopStr) : nop()

# -------------------------------------------------------------------------------
#            PillarBox Feather Clip
# -------------------------------------------------------------------------------

FeatherClip = IsFeather ? \
(IsFrosty ? StackHorizontal(FFrostyL.AddBorders(0, 0, CRVWidth-FL+BL-FR+BR, 0), FFrostyR) : \
Clone1 ? Eval("StackHorizontal(StackHorizontal(FCloneL, CRVideo.crop(0, 0, -CRVWidth+FL-BL, 0))" + \
TBGStr + FBordStr + "StackHorizontal(CRVideo.crop(CRVWidth-FR+BR, 0, 0, 0), FCloneR)" + TBGStr + ")")  : \
Clone2 ? StackHorizontal(Clone2L.AddBorders(0, 0, CRVWidth-FL+BL-FR+BR, 0), Clone2R) : \
Clone3 ? Eval("StackHorizontal(StackHorizontal(FClone3L, CRVideo.crop(0, 0, -CRVWidth+FL-BL, 0))" + \
TBGStr + FBordStr + "StackHorizontal(CRVideo.crop(CRVWidth-FR+BR, 0, 0, 0), FClone3R)" + TBGStr + ")") : \
Eval("StackHorizontal(BlankClip(CRVideo, width=FL, color=BColor)" + TweakStr + GrainStr + LoopStr + \
FBordStr + "BlankClip(CRVideo, width=FR, color=BColor)" + TweakStr + GrainStr + LoopStr + ")")) : nop()

# -------------------------------------------------------------------------------
#            PillarBox Output
# -------------------------------------------------------------------------------

FrostyOut = !IsFeather ? FrostyOut : Overlay(FrostyOut, FeatherClip, mask=FeatherMask)

""", "Frosty PillarBox Eval") : Eval("""

# ===============================================================================
#            LetterBox Borders
# ===============================================================================

BT = IsBDivider ? BT - BDivider : BT
BB = IsBDivider ? BB - BDivider : BB
FT = IsFeather ? max(BT, floor((BT + (Feather * 3)) / F_HeightMod) * HeightMod) : nop()
FB = IsFeather ? max(BB, floor((BB + (Feather * 3)) / F_HeightMod) * HeightMod) : nop()

# -------------------------------------------------------------------------------
#            LetterBox Strings
# -------------------------------------------------------------------------------

CRVideoStr = !IsBDivider ? ",CRVideo," : ",CRVideo.AddBorders(0, BDivider, 0, BDivider, color=BDColor),"

FastBlurStr = (IsPlain && !IsFeather) || FrostyGradient ? "" : \
".FastBlur(Blur, y_blur=VBlur, iterations=Iterations, dither=true)"

FBordStr = ".AddBorders(0, 0, 0, CRVHeight-FT+BT-FB+BB),"
GaussMTStr = ".GaussResize" + (!IsResampleMT ? "" : "MT")

GaussTStr = !IsPlain ? GaussMTStr + "(CRVWidth, BT)" : nop()
GaussBStr = !IsPlain ? GaussMTStr + "(CRVWidth, BB)" : nop()
FGaussTStr = IsFeather ? GaussMTStr + "(CRVWidth, FT)" : nop()
FGaussBStr = IsFeather ? GaussMTStr + "(CRVWidth, FB)" : nop()

Gauss3TStr = Clone3 ? GaussMTStr + "(max(CRVWidth, floor(CRVWidth * RatioT / F_WidthMod) * WidthMod), \
max(BT, floor(HeightCloneBorderT * RatioT / F_HeightMod) * HeightMod))" : nop()
Gauss3BStr = Clone3 ? GaussMTStr + "(max(CRVWidth, floor(CRVWidth * RatioB / F_WidthMod) * WidthMod), \
max(BB, floor(HeightCloneBorderB * RatioB / F_HeightMod) * HeightMod))" : nop()

GrainStr = ((Texture == 0) && (TextureC == 0)) || (GPlugin == 0) ? "" : \
(GPlugin == 1) ? ".AddGrain(var=Texture, uvar=TextureC, constant=true)" : \
".neo_f3kdb(range=0, grainY=Texture, grainC=TextureC, dither_algo=1)"

LoopStr = ".Trim(1, 1).Loop(TotalFrames)"
SLoopStr = IsFrostyStatic ? ".Trim(FrostyStatic, end=FrostyStatic).Loop(TotalFrames)" : ""
TSoftStr = IsTSoft ? ".TemporalSoften(TSoft, 255, 255, 20, 2)" : ""
TweakStr = (Bright != 0.0) || (Cont != 1.0) || (Sat != 1.0) ? ".Tweak(bright=Bright, cont=Cont, sat=Sat)" : ""

TBStr = TweakStr + FastBlurStr
TBGStr = TweakStr + FastBlurStr + GrainStr

# -------------------------------------------------------------------------------
#            LetterBox Frosty Borders
# -------------------------------------------------------------------------------

FrostyCrop = IsFrosty && IsTSoft ? \
CRVHeight - max(BorderMin, round(F_CRVHeight / (!Faster ? 5.0 : 10.0) / F_HeightMod) * HeightMod) : nop()

FrostyTB = IsFrosty && IsTSoft ? \
Eval("StackVertical(CRVideo.crop(0, 0, 0, -FrostyCrop), CRVideo.crop(0, FrostyCrop, 0, 0))" + TSoftStr) : nop()
HeightFrostyTB = IsFrosty && IsTSoft ? height(FrostyTB) : nop()

FrostyCrop = IsFrosty ? (IsTSoft ? HeightFrostyTB - \
max(BorderMin, round(float(HeightFrostyTB) / (!Faster ? 16.0 : 8.0) / F_HeightMod) * HeightMod) : \
CRVHeight - max(BorderMin, round(F_CRVHeight / 80.0 / F_HeightMod) * HeightMod)) : nop()

FrostyT = IsFrosty ? (IsTSoft ? FrostyTB : CRVideo).crop(0, 0, 0, -FrostyCrop) : nop()
FrostyB = IsFrosty ? (IsTSoft ? FrostyTB : CRVideo).crop(0, FrostyCrop, 0, 0): nop()

HeightFrostyT = IsFrosty ? height(FrostyT) : nop()
HeightFrostyB = IsFrosty ? height(FrostyB) : nop()

FrostyBlendT = IsFrosty ? ((Blend == 0) ? FrostyT : (Blend == 1) ? FrostyB : \
Overlay(FrostyT, FlipVertical(FrostyB), Opacity=Blend)) : nop()
FrostyBlendB = IsFrosty ? ((Blend == 0) ? FrostyB : (Blend == 1) ? FrostyT : \
Overlay(FrostyB, FlipVertical(FrostyT), Opacity=Blend)) : nop()

Blur = IsBlur ? Blur : IsFrosty ? F_CRVWidth / 10.0 : Blur
VBlur = IsVBlur ? VBlur : IsFrosty ? max(HeightFrostyT, HeightFrostyB) + (!IsFeather ? 0 : Feather) : VBlur

FrostyT = Frosty ? Eval("FrostyBlendT" + TBStr + GaussTStr + GrainStr + SLoopStr) : nop()
FrostyB = Frosty ? Eval("FrostyBlendB" + TBStr + GaussBStr + GrainStr + SLoopStr) : nop()

FFrostyT = Frosty && IsFeather ? Eval("FrostyBlendT" + TBStr + FGaussTStr + GrainStr + SLoopStr) : nop()
FFrostyB = Frosty && IsFeather ? Eval("FrostyBlendB" + TBStr + FGaussBStr + GrainStr + SLoopStr) : nop()

# -------------------------------------------------------------------------------
#            LetterBox Clone Borders
# -------------------------------------------------------------------------------

MaxCloneC = IsClone || (IsFrosty && IsFeather) ? floor(F_CRVHeight / 2.0 / F_HeightMod) * HeightMod : nop()
CloneCropT = IsClone || (IsFrosty && IsFeather) ? max(MaxCloneC, CRVHeight - BT) : nop()
CloneCropB = IsClone || (IsFrosty && IsFeather) ? max(MaxCloneC, CRVHeight - BB) : nop()
CloneBorderT = IsClone || (IsFrosty && IsFeather) ? CRVideo.crop(0, 0, 0, -CloneCropT) : nop()
CloneBorderB = IsClone || (IsFrosty && IsFeather) ? CRVideo.crop(0, CloneCropB, 0, 0) : nop()
HeightCloneBorderT = IsClone || (IsFrosty && IsFeather) ? height(CloneBorderT) : nop()
HeightCloneBorderB = IsClone || (IsFrosty && IsFeather) ? height(CloneBorderB) : nop()

FlipCloneT = IsClone || (IsFrosty && IsFeather) ? \
FlipVertical(((HeightCloneBorderT == BT) ? CloneBorderT : Eval("CloneBorderT" + GaussTStr))) : nop()
FlipCloneB = IsClone || (IsFrosty && IsFeather) ? \
FlipVertical(((HeightCloneBorderB == BB) ? CloneBorderB : Eval("CloneBorderB" + GaussBStr))) : nop()

CloneTB = (Clone1 || Clone3) && IsTSoft ? Eval("StackVertical(CloneBorderT, CloneBorderB)" + TSoftStr) : nop()
HeightCloneTB = (Clone1 || Clone3) && IsTSoft ? height(CloneTB) : nop()

CloneT = \
Clone1 || Clone3 ? (IsTSoft ? CloneTB.crop(0, 0, 0, -HeightCloneTB+HeightCloneBorderT) : CloneBorderT) : nop()
CloneB = \
Clone1 || Clone3 ? (IsTSoft ? CloneTB.crop(0, HeightCloneTB-HeightCloneBorderB, 0, 0) : CloneBorderB) : nop()

CloneBlendT = Clone1 || Clone3 ? ((Blend == 0) ? CloneT : (Blend == 1) ? CloneB : \
Overlay(CloneT, FlipVertical(CloneB), Opacity=Blend)) : nop()
CloneBlendB = Clone1 || Clone3 ? ((Blend == 0) ? CloneB : (Blend == 1) ? CloneT : \
Overlay(CloneB, FlipVertical(CloneT), Opacity=Blend)) : nop()

FCloneT = \
Clone1 && IsFeather ? ((HeightCloneBorderT == BT) ? CloneBlendT : Eval("CloneBlendT" + GaussTStr)) : nop()
FCloneB = \
Clone1 && IsFeather ? ((HeightCloneBorderB == BB) ? CloneBlendB : Eval("CloneBlendB" + GaussBStr)) : nop()

CloneT = Clone1 && !IsFeather ? ((HeightCloneBorderT == BT) ? \
Eval("CloneBlendT" + TBGStr) : Eval("CloneBlendT" + TBStr + GaussTStr + GrainStr)) : nop()
CloneB = Clone1 && !IsFeather ? ((HeightCloneBorderB == BB) ? \
Eval("CloneBlendB" + TBGStr) : Eval("CloneBlendB" + TBStr + GaussBStr + GrainStr)) : nop()

# -------------------------------------------------------------------------------
#            LetterBox Clone2 Borders
# -------------------------------------------------------------------------------

Clone2CropT = Clone2 ? max(MaxCloneC, CRVHeight - FT) : nop()
Clone2CropB = Clone2 ? max(MaxCloneC, CRVHeight - FB) : nop()
Clone2BT = Clone2 ? CRVideo.crop(0, 0, 0, -Clone2CropT) : nop()
Clone2BB = Clone2 ? CRVideo.crop(0, Clone2CropB, 0, 0) : nop()
HeightClone2BT = Clone2 ? height(Clone2BT) : nop()
HeightClone2BB = Clone2 ? height(Clone2BB) : nop()

FlipClone2T = Clone2 ? FlipVertical(((HeightClone2BT == FT) ? Clone2BT : Eval("Clone2BT" + FGaussTStr))) : nop()
FlipClone2B = Clone2 ? FlipVertical(((HeightClone2BB == FB) ? Clone2BB : Eval("Clone2BB" + FGaussBStr))) : nop()

Clone2TB = Clone2 && IsTSoft ? Eval("StackVertical(Clone2BT, Clone2BB)" + TSoftStr) : nop()
HeightClone2TB = Clone2 && IsTSoft ? height(Clone2TB) : nop()

Clone2T = Clone2 ? (IsTSoft ? Clone2TB.crop(0, 0, 0, -HeightClone2TB+HeightClone2BT) : Clone2BT) : nop()
Clone2B = Clone2 ? (IsTSoft ? Clone2TB.crop(0, HeightClone2TB-HeightClone2BB, 0, 0) : Clone2BB) : nop()

Clone2BlendT = Clone2 ? ((Blend == 0) ? Clone2T : (Blend == 1) ? Clone2B : \
Overlay(Clone2T, FlipVertical(Clone2B), Opacity=Blend)) : nop()
Clone2BlendB = Clone2 ? ((Blend == 0) ? Clone2B : (Blend == 1) ? Clone2T : \
Overlay(Clone2B, FlipVertical(Clone2T), Opacity=Blend)) : nop()

Clone2T = Clone2 ? ((HeightClone2BT == FT) ? Eval("Clone2BlendT" + TBGStr) : \
Eval("Clone2BlendT" + TBStr + FGaussTStr + GrainStr)) : nop()
Clone2B = Clone2 ? ((HeightClone2BB == FB) ? Eval("Clone2BlendB" + TBGStr) : \
Eval("Clone2BlendB" + TBStr + FGaussBStr + GrainStr)) : nop()

# -------------------------------------------------------------------------------
#            LetterBox Clone3 Borders
# -------------------------------------------------------------------------------

RatioT = Clone3 ? (HeightCloneBorderT - BT >= 0) ? 1.618 : float(BT) / float(HeightCloneBorderT) : nop()
RatioB = Clone3 ? (HeightCloneBorderB - BB >= 0) ? 1.618 : float(BB) / float(HeightCloneBorderB) : nop()

Clone3T = Clone3 ? Eval("CloneBlendT" + Gauss3TStr) : nop()
Clone3B = Clone3 ? Eval("CloneBlendB" + Gauss3BStr) : nop()
WidthClone3T = Clone3 ? width(Clone3T) : nop()
HeightClone3T = Clone3 ? height(Clone3T) : nop()
WidthClone3B = Clone3 ? width(Clone3B) : nop()
HeightClone3B = Clone3 ? height(Clone3B) : nop()

Clone3T_CL = Clone3 ? max(0, floor((WidthClone3T - CRVWidth) / F_WidthMod / 2.0) * WidthMod) : nop()
Clone3T_CR = Clone3 ? max(0, WidthClone3T - CRVWidth - Clone3T_CL) : nop()
Clone3T_CT = Clone3 ? max(0, floor((HeightClone3T - BT) / F_HeightMod / 2.0) * HeightMod) : nop()
Clone3T_CB = Clone3 ? max(0, HeightClone3T - BT - Clone3T_CT) : nop()
Clone3B_CL = Clone3 ? max(0, floor((WidthClone3B - CRVWidth) / F_WidthMod / 2.0) * WidthMod) : nop()
Clone3B_CR = Clone3 ? max(0, WidthClone3B - CRVWidth - Clone3B_CL) : nop()
Clone3B_CT = Clone3 ? max(0, floor((HeightClone3B - BB) / F_HeightMod / 2.0) * HeightMod) : nop()
Clone3B_CB = Clone3 ? max(0, HeightClone3B - BB - Clone3B_CT) : nop()

Clone3T = Clone3 ? Clone3T.crop(Clone3T_CL, Clone3T_CT, -Clone3T_CR, -Clone3T_CB) : nop()
Clone3B = Clone3 ? Clone3B.crop(Clone3B_CL, Clone3B_CT, -Clone3B_CR, -Clone3B_CB) : nop()

FClone3T = Clone3 && IsFeather ? Clone3T : nop()
FClone3B = Clone3 && IsFeather ? Clone3B : nop()

Clone3T = Clone3 && !IsFeather ? Eval("Clone3T" + TBGStr) : nop()
Clone3B = Clone3 && !IsFeather ? Eval("Clone3B" + TBGStr) : nop()

# -------------------------------------------------------------------------------
#            LetterBox FrostyGradient Borders
# -------------------------------------------------------------------------------

GradientTB = \
FrostyGradient ? BlankClip(CRVideo, width=CRVWidth, height=88).AddBorders(0, 8, 0, 0, color=BColor)\
.Tweak(bright=Bright, cont=Cont, sat=Sat).FastBlur(0, y_blur=50, iterations=3, dither=true) : nop()
GradientT = FrostyGradient ? Eval("FlipVertical(GradientTB)" + GaussTStr + GrainStr + LoopStr) : nop()
GradientB = FrostyGradient ? Eval("GradientTB" + GaussBStr + GrainStr + LoopStr) : nop()

# -------------------------------------------------------------------------------
#            LetterBox Plain Borders
# -------------------------------------------------------------------------------

PlainT = IsPlain && (BT >= HeightMod) ? \
Eval("BlankClip(CRVideo, height=BT, color=BColor)" + TweakStr + GrainStr + LoopStr) : nop()
PlainB = IsPlain && (BB >= HeightMod) ? \
Eval("BlankClip(CRVideo, height=BB, color=BColor)" + TweakStr + GrainStr + LoopStr) : nop()

PlainT2 = IsPlain && IsFeather ? Eval("BlankClip(CRVideo, height=BT, color=BColor)" + TweakStr + LoopStr) : nop()
PlainB2 = IsPlain && IsFeather ? Eval("BlankClip(CRVideo, height=BB, color=BColor)" + TweakStr + LoopStr) : nop()

# -------------------------------------------------------------------------------
#            LetterBox Frosty Video
# -------------------------------------------------------------------------------

FrostyBT = !IsFeather ? (IsFrosty ? FrostyT : Clone1 ? CloneT : Clone3 ? Clone3T : \
FrostyGradient ? GradientT : IsPlain && (BT >= HeightMod) ? PlainT : nop()) : \
IsFrosty || Clone1 || Clone2 || Clone3 ? FlipCloneT : IsPlain && (BT >= HeightMod) ? PlainT2 : nop()

FrostyBB = !IsFeather ? (IsFrosty ? FrostyB : Clone1 ? CloneB : Clone3 ? Clone3B : \
FrostyGradient ? GradientB : IsPlain && (BB >= HeightMod) ? PlainB : nop()) : \
IsFrosty || Clone1 || Clone2 || Clone3 ? FlipCloneB : IsPlain && (BB >= HeightMod) ? PlainB2 : nop()

FrostyOut = IsFrosty || IsClone || FrostyGradient || (IsPlain && (BT >= HeightMod <= BB)) ? \
Eval("StackVertical(FrostyBT" + CRVideoStr + "FrostyBB)") : \
IsPlain && (BT >= HeightMod) ? StackVertical(FrostyBT, CRVideo) : \
IsPlain && (BB >= HeightMod) ? StackVertical(CRVideo, FrostyBB) : CRVideo

# -------------------------------------------------------------------------------
#            LetterBox Feather Mask
# -------------------------------------------------------------------------------

FeatherMask = IsFeather ? BlankClip(CRVideo, height=CRVHeight-(Feather*2))\
.AddBorders(0, BT+Feather, 0, BB+Feather, color=$FFFFFF).ColorYUV(Levels="TV->PC")\
.FastBlur(0, y_blur=Feather/2.25, iterations=3, dither=true) : nop()

FeatherMask = IsFeather ? Eval("FeatherMask" + LoopStr) : nop()

# -------------------------------------------------------------------------------
#            LetterBox Feather Clip
# -------------------------------------------------------------------------------

FeatherClip = IsFeather ? \
(IsFrosty ? StackVertical(FFrostyT.AddBorders(0, 0, 0, CRVHeight-FT+BT-FB+BB),  FFrostyB) : \
Clone1 ? Eval("StackVertical(StackVertical(FCloneT, CRVideo.crop(0, 0, 0, -CRVHeight+FT-BT))" + \
TBGStr + FBordStr + "StackVertical(CRVideo.crop(0, CRVHeight-FB+BB, 0, 0), FCloneB)" + TBGStr + ")")  : \
Clone2 ? StackVertical(Clone2T.AddBorders(0, 0, 0, CRVHeight-FT+BT-FB+BB), Clone2B) : \
Clone3 ? Eval("StackVertical(StackVertical(FClone3T, CRVideo.crop(0, 0, 0, -CRVHeight+FT-BT))" + \
TBGStr + FBordStr + "StackVertical(CRVideo.crop(0, CRVHeight-FB+BB, 0, 0), FClone3B)" + TBGStr + ")") : \
Eval("StackVertical(BlankClip(CRVideo, height=FT, color=BColor)" + TweakStr + GrainStr + LoopStr + \
FBordStr + "BlankClip(CRVideo, height=FB, color=BColor)" + TweakStr + GrainStr + LoopStr + ")")) : nop()

# -------------------------------------------------------------------------------
#            LetterBox Output
# -------------------------------------------------------------------------------

FrostyOut = !IsFeather ? FrostyOut : Overlay(FrostyOut, FeatherClip, mask=FeatherMask)

""", "Frosty LetterBox Eval")

# ===============================================================================

return FrostyOut  }

# ===============================================================================
# ===============================================================================
#            Ringing Repair Function
# ===============================================================================
# ===============================================================================

function CR_RingRepair(clip Resized, clip Original, int CRVideo_Width, int CRVideo_Height, \
float RCL, float RCT, float RCR, float RCB, bool IsResampleMT, bool IsZResize, bool IsFResize, float Weight)  {

GaussVideo = IsZResize ? Original.z_ConvertFormat(CRVideo_Width, CRVideo_Height, \
src_left=RCL, src_top=RCT, src_width=-RCR, src_height=-RCB, resample_filter="Gauss", filter_param_a=100) : \
IsFResize ? Original.fmtc_resample(CRVideo_Width, CRVideo_Height, \
sx=RCL, sy=RCT, sw=-RCR, sh=-RCB, kernel="Gauss", a1=100) : \
Eval("Original.GaussResize" + (!IsResampleMT ? "" : "MT") + \
"(CRVideo_Width, CRVideo_Height, src_left=RCL, src_top=RCT, src_width=-RCR, src_height=-RCB, p=100)")

RepairedVideo = Resized.Repair(GaussVideo, 1)
return (Weight == 1) ? RepairedVideo : Resized.Merge(RepairedVideo, Weight)  }

# ===============================================================================
# ===============================================================================
#            Position Function
# ===============================================================================
# ===============================================================================

function CR_Position(clip Source, \
float "X", float "Y", float "Size", val "Color", int "Align", int "LSP", string "LB1", string "LB2")  {

Size = default(Size, 20)
Y = default(Y, Size)
LSP = default(LSP, 20)
LB1 = default(LB1, "")
LB2 = default(LB2, "\n")

GScriptClip(Source, """
Frame = string(current_frame, "%.0f")
F_Position = float(current_frame) * float(FrameRateDenominator()) / float(FrameRateNumerator())
Position = floor(F_Position)
HH = string(Position / 3600, "%02.0f:")
MM = string((Position / 60) % 60, "%02.0f:")
SS = string(Position % 60, "%02.0f.")
MS = string(floor((F_Position - Position) * 1000.0) % 1000, "%03.0f")
subtitle(LB1 + Frame + LB2 + HH + MM + SS + MS, x=X, y=Y, size=Size, text_color=Color, align=Align, lsp=LSP)
""", args="X, Y, Size, Color, Align, LSP, LB1, LB2")  }

# ===============================================================================
# ===============================================================================
#            Format String Functions
# ===============================================================================
# ===============================================================================

function CR_FS(float F, int "Limit", bool "Keep")  {
Limit = abs(default(Limit, 3))
Keep = default(Keep, false)
S = Eval("""string(F, "%.""" + string(Limit) + """f")""")
S = (Limit == 0) ? S : CR_RemoveTrailingZeros(S)
SLength = StrLen(S)
NewStr = (Limit == 0) || (FindStr(S, ".") < SLength) ? S : !Keep ? LeftStr(S, SLength - 1) : S + "0"
return (value(NewStr) == 0) && (FindStr(NewStr, "-") > 0) ? MidStr(NewStr, 2) : NewStr  }

# -------------------------------------------------------------------------------

function CR_RemoveTrailingZeros(string S)  {
ZeroPos = FindStr(S + "x", "0x")
return (ZeroPos == 0) ? S : CR_RemoveTrailingZeros(LeftStr(S, ZeroPos - 1))  }

# ===============================================================================
# ===============================================================================
#            Float To Fraction Function
# ===============================================================================
# ===============================================================================

function CR_FTF(float Decimal, int "Out")  {
Out = default(Out, 1)
Num = (Decimal == 0) ? "0" : string(ContinuedNumerator(Decimal, Limit=10000))
Den = (Decimal == 0) ? "1" : string(ContinuedDenominator(Decimal, Limit=10000))
return "  (" + Num + ((Out == 1) ? ":" : " / ") + Den + ")"  }

# ===============================================================================
# ===============================================================================
#            CropResize Defaults
# ===============================================================================
# ===============================================================================

# Setting these defaults incorrectly will probably break something, so take care when changing them.
# The functions below are required and cannot be commented out.

function CR_AutoDAR() { return false }
function CR_CLine(int W, int H) { return (W > 1920) || (H > 1080) ? 2 : 1 }
function CR_ColorConvert() { return "" }
function CR_CPreview() { return 1 }
function CR_Info_AutoPicDim() { return 0 }
function CR_Info_AutoPicDim_CPreview_ColorConvert() { return false }
function CR_Info_Formatted_AVS_Version() { return true }
function CR_MeGUI_Support() { return true }

# Default resizer functions
function CR_Resizer_Default() { return "" }
function CR_RStr_Default() { return "" }

# Alternative resizer functions
function CR_Resizer_X() { return "Resize8" }
function CR_RStr_X() { return """ Kernel="Spline36" """ }

# ===============================================================================
# ===============================================================================