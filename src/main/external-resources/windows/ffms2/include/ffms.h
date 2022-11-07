//  Copyright (c) 2007-2018 Fredrik Mellbin
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

#ifndef FFMS_H
#define FFMS_H

// Version format: major - minor - micro - bump
#define FFMS_VERSION ((2 << 24) | (40 << 16) | (0 << 8) | 0)

#include <stdint.h>
#include <stddef.h>


/********
*	The following preprocessor voodoo ensures that all API symbols are exported
*	as intended on all supported platforms, that non-API symbols are hidden (where possible),
*	and that the correct calling convention and extern declarations are used.
*	The end result should be that linking to FFMS2 Just Works.
********/

// Convenience for C++ users.
#if defined(__cplusplus)
#	define FFMS_EXTERN_C extern "C"
#else
#	define FFMS_EXTERN_C
#endif

// On win32, we need to ensure we use stdcall with all compilers.
#if defined(_WIN32) && !defined(_WIN64)
#	define FFMS_CC __stdcall
#else
#	define FFMS_CC
#endif

// compiler-specific deprecation attributes
#ifndef FFMS_EXPORTS
#	if defined(__GNUC__) && (__GNUC__ >= 4)
#		define FFMS_DEPRECATED __attribute__((deprecated))
#	elif defined(_MSC_VER)
#		define FFMS_DEPRECATED __declspec(deprecated)
#	endif
#endif

#ifndef FFMS_DEPRECATED
// Define as empty if the compiler doesn't support deprecation attributes or
// if we're building FFMS2 itself
#	define FFMS_DEPRECATED
#endif

// And now for some symbol hide-and-seek...
#if defined(_WIN32) && !defined(FFMS_STATIC) // MSVC
#	if defined(FFMS_EXPORTS) // building the FFMS2 library itself, with visible API symbols
#		define FFMS_API(ret) FFMS_EXTERN_C __declspec(dllexport) ret FFMS_CC
#		define FFMS_DEPRECATED_API(ret) FFMS_EXTERN_C FFMS_DEPRECATED __declspec(dllexport) ret FFMS_CC
#	else // building something that depends on FFMS2
#		define FFMS_API(ret) FFMS_EXTERN_C __declspec(dllimport) ret FFMS_CC
#		define FFMS_DEPRECATED_API(ret) FFMS_EXTERN_C FFMS_DEPRECATED __declspec(dllimport) ret FFMS_CC
#	endif // defined(FFMS_EXPORTS)
// GCC 4 or later: export API symbols only. Some GCC 3.x versions support the visibility attribute too,
// but we don't really care enough about that to add compatibility defines for it.
#elif defined(__GNUC__) && __GNUC__ >= 4
#	define FFMS_API(ret) FFMS_EXTERN_C __attribute__((visibility("default"))) ret FFMS_CC
#	define FFMS_DEPRECATED_API(ret) FFMS_EXTERN_C FFMS_DEPRECATED __attribute__((visibility("default"))) ret FFMS_CC
#else // fallback for everything else
#	define FFMS_API(ret) FFMS_EXTERN_C ret FFMS_CC
#	define FFMS_DEPRECATED_API(ret) FFMS_EXTERN_C FFMS_DEPRECATED ret FFMS_CC
#endif // defined(_MSC_VER)


// we now return you to your regularly scheduled programming.

typedef struct FFMS_ErrorInfo {
    int ErrorType;
    int SubType;
    int BufferSize;
    char *Buffer;
} FFMS_ErrorInfo;

typedef struct FFMS_VideoSource FFMS_VideoSource;
typedef struct FFMS_AudioSource FFMS_AudioSource;
typedef struct FFMS_Indexer FFMS_Indexer;
typedef struct FFMS_Index FFMS_Index;
typedef struct FFMS_Track FFMS_Track;

typedef enum FFMS_Errors {
    // No error
    FFMS_ERROR_SUCCESS = 0,

    // Main types - where the error occurred
    FFMS_ERROR_INDEX = 1,			// index file handling
    FFMS_ERROR_INDEXING,			// indexing
    FFMS_ERROR_POSTPROCESSING,		// video postprocessing (libpostproc)
    FFMS_ERROR_SCALING,				// image scaling (libswscale)
    FFMS_ERROR_DECODING,			// audio/video decoding
    FFMS_ERROR_SEEKING,				// seeking
    FFMS_ERROR_PARSER,				// file parsing
    FFMS_ERROR_TRACK,				// track handling
    FFMS_ERROR_WAVE_WRITER,			// WAVE64 file writer
    FFMS_ERROR_CANCELLED,			// operation aborted
    FFMS_ERROR_RESAMPLING,			// audio resampling (libavresample)

    // Subtypes - what caused the error
    FFMS_ERROR_UNKNOWN = 20,		// unknown error
    FFMS_ERROR_UNSUPPORTED,			// format or operation is not supported with this binary
    FFMS_ERROR_FILE_READ,			// cannot read from file
    FFMS_ERROR_FILE_WRITE,			// cannot write to file
    FFMS_ERROR_NO_FILE,				// no such file or directory
    FFMS_ERROR_VERSION,				// wrong version
    FFMS_ERROR_ALLOCATION_FAILED,	// out of memory
    FFMS_ERROR_INVALID_ARGUMENT,	// invalid or nonsensical argument
    FFMS_ERROR_CODEC,				// decoder error
    FFMS_ERROR_NOT_AVAILABLE,		// requested mode or operation unavailable in this binary
    FFMS_ERROR_FILE_MISMATCH,		// provided index does not match the file
    FFMS_ERROR_USER					// problem exists between keyboard and chair
} FFMS_Errors;

typedef enum FFMS_SeekMode {
    FFMS_SEEK_LINEAR_NO_RW = -1,
    FFMS_SEEK_LINEAR = 0,
    FFMS_SEEK_NORMAL = 1,
    FFMS_SEEK_UNSAFE = 2,
    FFMS_SEEK_AGGRESSIVE = 3
} FFMS_SeekMode;

typedef enum FFMS_IndexErrorHandling {
    FFMS_IEH_ABORT = 0,
    FFMS_IEH_CLEAR_TRACK = 1,
    FFMS_IEH_STOP_TRACK = 2,
    FFMS_IEH_IGNORE = 3
} FFMS_IndexErrorHandling;

typedef enum FFMS_TrackType {
    FFMS_TYPE_UNKNOWN = -1,
    FFMS_TYPE_VIDEO,
    FFMS_TYPE_AUDIO,
    FFMS_TYPE_DATA,
    FFMS_TYPE_SUBTITLE,
    FFMS_TYPE_ATTACHMENT
} FFMS_TrackType;

typedef enum FFMS_SampleFormat {
    FFMS_FMT_U8 = 0,
    FFMS_FMT_S16,
    FFMS_FMT_S32,
    FFMS_FMT_FLT,
    FFMS_FMT_DBL
} FFMS_SampleFormat;

typedef enum FFMS_AudioChannel {
    FFMS_CH_FRONT_LEFT = 0x00000001,
    FFMS_CH_FRONT_RIGHT = 0x00000002,
    FFMS_CH_FRONT_CENTER = 0x00000004,
    FFMS_CH_LOW_FREQUENCY = 0x00000008,
    FFMS_CH_BACK_LEFT = 0x00000010,
    FFMS_CH_BACK_RIGHT = 0x00000020,
    FFMS_CH_FRONT_LEFT_OF_CENTER = 0x00000040,
    FFMS_CH_FRONT_RIGHT_OF_CENTER = 0x00000080,
    FFMS_CH_BACK_CENTER = 0x00000100,
    FFMS_CH_SIDE_LEFT = 0x00000200,
    FFMS_CH_SIDE_RIGHT = 0x00000400,
    FFMS_CH_TOP_CENTER = 0x00000800,
    FFMS_CH_TOP_FRONT_LEFT = 0x00001000,
    FFMS_CH_TOP_FRONT_CENTER = 0x00002000,
    FFMS_CH_TOP_FRONT_RIGHT = 0x00004000,
    FFMS_CH_TOP_BACK_LEFT = 0x00008000,
    FFMS_CH_TOP_BACK_CENTER = 0x00010000,
    FFMS_CH_TOP_BACK_RIGHT = 0x00020000,
    FFMS_CH_STEREO_LEFT = 0x20000000,
    FFMS_CH_STEREO_RIGHT = 0x40000000
} FFMS_AudioChannel;

typedef enum FFMS_Resizers {
    FFMS_RESIZER_FAST_BILINEAR = 0x0001,
    FFMS_RESIZER_BILINEAR = 0x0002,
    FFMS_RESIZER_BICUBIC = 0x0004,
    FFMS_RESIZER_X = 0x0008,
    FFMS_RESIZER_POINT = 0x0010,
    FFMS_RESIZER_AREA = 0x0020,
    FFMS_RESIZER_BICUBLIN = 0x0040,
    FFMS_RESIZER_GAUSS = 0x0080,
    FFMS_RESIZER_SINC = 0x0100,
    FFMS_RESIZER_LANCZOS = 0x0200,
    FFMS_RESIZER_SPLINE = 0x0400
} FFMS_Resizers;

typedef enum FFMS_AudioDelayModes {
    FFMS_DELAY_NO_SHIFT = -3,
    FFMS_DELAY_TIME_ZERO = -2,
    FFMS_DELAY_FIRST_VIDEO_TRACK = -1
} FFMS_AudioDelayModes;

typedef enum FFMS_ChromaLocations {
    FFMS_LOC_UNSPECIFIED = 0,
    FFMS_LOC_LEFT = 1,
    FFMS_LOC_CENTER = 2,
    FFMS_LOC_TOPLEFT = 3,
    FFMS_LOC_TOP = 4,
    FFMS_LOC_BOTTOMLEFT = 5,
    FFMS_LOC_BOTTOM = 6
} FFMS_ChromaLocations;

typedef enum FFMS_ColorRanges {
    FFMS_CR_UNSPECIFIED = 0,
    FFMS_CR_MPEG = 1, // 219*2^(n-8), i.e. 16-235 with 8-bit samples
    FFMS_CR_JPEG = 2 // 2^n-1, or "fullrange"
} FFMS_ColorRanges;

typedef enum FFMS_Stereo3DType {
    FFMS_S3D_TYPE_2D = 0,
    FFMS_S3D_TYPE_SIDEBYSIDE,
    FFMS_S3D_TYPE_TOPBOTTOM,
    FFMS_S3D_TYPE_FRAMESEQUENCE,
    FFMS_S3D_TYPE_CHECKERBOARD,
    FFMS_S3D_TYPE_SIDEBYSIDE_QUINCUNX,
    FFMS_S3D_TYPE_LINES,
    FFMS_S3D_TYPE_COLUMNS
} FFMS_Stereo3DType;

typedef enum FFMS_Stereo3DFlags {
    FFMS_S3D_FLAGS_INVERT = 1
} FFMS_Stereo3DFlags;

typedef enum FFMS_MixingCoefficientType {
    FFMS_MIXING_COEFFICIENT_Q8 = 0,
    FFMS_MIXING_COEFFICIENT_Q15 = 1,
    FFMS_MIXING_COEFFICIENT_FLT = 2
} FFMS_MixingCoefficientType;

typedef enum FFMS_MatrixEncoding {
    FFMS_MATRIX_ENCODING_NONE = 0,
    FFMS_MATRIX_ENCODING_DOBLY = 1,
    FFMS_MATRIX_ENCODING_PRO_LOGIC_II = 2,
    FFMS_MATRIX_ENCODING_PRO_LOGIC_IIX = 3,
    FFMS_MATRIX_ENCODING_PRO_LOGIC_IIZ = 4,
    FFMS_MATRIX_ENCODING_DOLBY_EX = 5,
    FFMS_MATRIX_ENCODING_DOLBY_HEADPHONE = 6
} FFMS_MatrixEncoding;

typedef enum FFMS_ResampleFilterType {
    FFMS_RESAMPLE_FILTER_CUBIC = 0,
    FFMS_RESAMPLE_FILTER_SINC = 1, /* misnamed as multiple windowsed sinc filters exist, actually called BLACKMAN_NUTTALL */
    FFMS_RESAMPLE_FILTER_KAISER = 2
} FFMS_ResampleFilterType;

typedef enum FFMS_AudioDitherMethod {
    FFMS_RESAMPLE_DITHER_NONE = 0,
    FFMS_RESAMPLE_DITHER_RECTANGULAR = 1,
    FFMS_RESAMPLE_DITHER_TRIANGULAR = 2,
    FFMS_RESAMPLE_DITHER_TRIANGULAR_HIGHPASS = 3,
    FFMS_RESAMPLE_DITHER_TRIANGULAR_NOISESHAPING = 4
} FFMS_AudioDitherMethod;

typedef enum FFMS_LogLevels {
    FFMS_LOG_QUIET = -8,
    FFMS_LOG_PANIC = 0,
    FFMS_LOG_FATAL = 8,
    FFMS_LOG_ERROR = 16,
    FFMS_LOG_WARNING = 24,
    FFMS_LOG_INFO = 32,
    FFMS_LOG_VERBOSE = 40,
    FFMS_LOG_DEBUG = 48,
    FFMS_LOG_TRACE = 56
} FFMS_LogLevels;

typedef struct FFMS_ResampleOptions {
    int64_t ChannelLayout;
    FFMS_SampleFormat SampleFormat;
    int SampleRate;
    FFMS_MixingCoefficientType MixingCoefficientType;
    double CenterMixLevel;
    double SurroundMixLevel;
    double LFEMixLevel;
    int Normalize;
    int ForceResample;
    int ResampleFilterSize;
    int ResamplePhaseShift;
    int LinearInterpolation;
    double CutoffFrequencyRatio;
    FFMS_MatrixEncoding MatrixedStereoEncoding;
    FFMS_ResampleFilterType FilterType;
    int KaiserBeta;
    FFMS_AudioDitherMethod DitherMethod;
} FFMS_ResampleOptions;


typedef struct FFMS_Frame {
    const uint8_t *Data[4];
    int Linesize[4];
    int EncodedWidth;
    int EncodedHeight;
    int EncodedPixelFormat;
    int ScaledWidth;
    int ScaledHeight;
    int ConvertedPixelFormat;
    int KeyFrame;
    int RepeatPict;
    int InterlacedFrame;
    int TopFieldFirst;
    char PictType;
    int ColorSpace;
    int ColorRange;
    /* Introduced in FFMS_VERSION ((2 << 24) | (21 << 16) | (0 << 8) | 0) */
    int ColorPrimaries;
    int TransferCharateristics;
    int ChromaLocation;
    /* Introduced in FFMS_VERSION ((2 << 24) | (27 << 16) | (0 << 8) | 0) */
    int HasMasteringDisplayPrimaries;  /* Non-zero if the 4 fields below are valid */
    double MasteringDisplayPrimariesX[3];
    double MasteringDisplayPrimariesY[3];
    double MasteringDisplayWhitePointX;
    double MasteringDisplayWhitePointY;
    int HasMasteringDisplayLuminance; /* Non-zero if the 2 fields below are valid */
    double MasteringDisplayMinLuminance;
    double MasteringDisplayMaxLuminance;
    int HasContentLightLevel; /* Non-zero if the 2 fields below are valid */
    unsigned int ContentLightLevelMax;
    unsigned int ContentLightLevelAverage;
} FFMS_Frame;

typedef struct FFMS_TrackTimeBase {
    int64_t Num;
    int64_t Den;
} FFMS_TrackTimeBase;

typedef struct FFMS_FrameInfo {
    int64_t PTS;
    int RepeatPict;
    int KeyFrame;
    int64_t OriginalPTS;
} FFMS_FrameInfo;

typedef struct FFMS_VideoProperties {
    int FPSDenominator;
    int FPSNumerator;
    int RFFDenominator;
    int RFFNumerator;
    int NumFrames;
    int SARNum;
    int SARDen;
    int CropTop;
    int CropBottom;
    int CropLeft;
    int CropRight;
    int TopFieldFirst;
    FFMS_DEPRECATED int ColorSpace; /* Provided in FFMS_Frame */
    FFMS_DEPRECATED int ColorRange; /* Provided in FFMS_Frame */
    double FirstTime;
    double LastTime;
    /* Introduced in FFMS_VERSION ((2 << 24) | (24 << 16) | (0 << 8) | 0) */
    int Rotation;
    int Stereo3DType;
    int Stereo3DFlags;
    /* Introduced in FFMS_VERSION ((2 << 24) | (30 << 16) | (0 << 8) | 0) */
    double LastEndTime;
    int HasMasteringDisplayPrimaries;  /* Non-zero if the 4 fields below are valid */
    double MasteringDisplayPrimariesX[3];
    double MasteringDisplayPrimariesY[3];
    double MasteringDisplayWhitePointX;
    double MasteringDisplayWhitePointY;
    int HasMasteringDisplayLuminance; /* Non-zero if the 2 fields below are valid */
    double MasteringDisplayMinLuminance;
    double MasteringDisplayMaxLuminance;
    int HasContentLightLevel; /* Non-zero if the 2 fields below are valid */
    unsigned int ContentLightLevelMax;
    unsigned int ContentLightLevelAverage;
    /* Introduced in FFMS_VERSION ((2 << 24) | (31 << 16) | (0 << 8) | 0) */
    int Flip;
} FFMS_VideoProperties;

typedef struct FFMS_AudioProperties {
    int SampleFormat;
    int SampleRate;
    int BitsPerSample;
    int Channels;
    int64_t ChannelLayout; // should probably be a plain int, none of the constants are >32 bits long
    int64_t NumSamples;
    double FirstTime;
    double LastTime;
    /* Introduced in FFMS_VERSION ((2 << 24) | (30 << 16) | (0 << 8) | 0) */
    double LastEndTime;
} FFMS_AudioProperties;

typedef int (FFMS_CC *TIndexCallback)(int64_t Current, int64_t Total, void *ICPrivate);

/* Most functions return 0 on success */
/* Functions without error message output can be assumed to never fail in a graceful way */
FFMS_API(void) FFMS_Init(int, int); /* Pass 0 to both arguments, kept to partially preserve abi */
FFMS_API(void) FFMS_Deinit();
FFMS_API(int) FFMS_GetVersion();
FFMS_API(int) FFMS_GetLogLevel();
FFMS_API(void) FFMS_SetLogLevel(int Level);
FFMS_API(FFMS_VideoSource *) FFMS_CreateVideoSource(const char *SourceFile, int Track, FFMS_Index *Index, int Threads, int SeekMode, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(FFMS_AudioSource *) FFMS_CreateAudioSource(const char *SourceFile, int Track, FFMS_Index *Index, int DelayMode, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(void) FFMS_DestroyVideoSource(FFMS_VideoSource *V);
FFMS_API(void) FFMS_DestroyAudioSource(FFMS_AudioSource *A);
FFMS_API(const FFMS_VideoProperties *) FFMS_GetVideoProperties(FFMS_VideoSource *V);
FFMS_API(const FFMS_AudioProperties *) FFMS_GetAudioProperties(FFMS_AudioSource *A);
FFMS_API(const FFMS_Frame *) FFMS_GetFrame(FFMS_VideoSource *V, int n, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(const FFMS_Frame *) FFMS_GetFrameByTime(FFMS_VideoSource *V, double Time, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(int) FFMS_GetAudio(FFMS_AudioSource *A, void *Buf, int64_t Start, int64_t Count, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(int) FFMS_SetOutputFormatV2(FFMS_VideoSource *V, const int *TargetFormats, int Width, int Height, int Resizer, FFMS_ErrorInfo *ErrorInfo); /* Introduced in FFMS_VERSION ((2 << 24) | (15 << 16) | (3 << 8) | 0) */
FFMS_API(void) FFMS_ResetOutputFormatV(FFMS_VideoSource *V);
FFMS_API(int) FFMS_SetInputFormatV(FFMS_VideoSource *V, int ColorSpace, int ColorRange, int Format, FFMS_ErrorInfo *ErrorInfo); /* Introduced in FFMS_VERSION ((2 << 24) | (17 << 16) | (1 << 8) | 0) */
FFMS_API(void) FFMS_ResetInputFormatV(FFMS_VideoSource *V);
FFMS_API(FFMS_ResampleOptions *) FFMS_CreateResampleOptions(FFMS_AudioSource *A); /* Introduced in FFMS_VERSION ((2 << 24) | (15 << 16) | (4 << 8) | 0) */
FFMS_API(int) FFMS_SetOutputFormatA(FFMS_AudioSource *A, const FFMS_ResampleOptions*options, FFMS_ErrorInfo *ErrorInfo); /* Introduced in FFMS_VERSION ((2 << 24) | (15 << 16) | (4 << 8) | 0) */
FFMS_API(void) FFMS_DestroyResampleOptions(FFMS_ResampleOptions *options); /* Introduced in FFMS_VERSION ((2 << 24) | (15 << 16) | (4 << 8) | 0) */
FFMS_API(void) FFMS_DestroyIndex(FFMS_Index *Index);
FFMS_API(int) FFMS_GetFirstTrackOfType(FFMS_Index *Index, int TrackType, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(int) FFMS_GetFirstIndexedTrackOfType(FFMS_Index *Index, int TrackType, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(int) FFMS_GetNumTracks(FFMS_Index *Index);
FFMS_API(int) FFMS_GetNumTracksI(FFMS_Indexer *Indexer);
FFMS_API(int) FFMS_GetTrackType(FFMS_Track *T);
FFMS_API(int) FFMS_GetTrackTypeI(FFMS_Indexer *Indexer, int Track);
FFMS_API(FFMS_IndexErrorHandling) FFMS_GetErrorHandling(FFMS_Index *Index);
FFMS_API(const char *) FFMS_GetCodecNameI(FFMS_Indexer *Indexer, int Track);
FFMS_API(const char *) FFMS_GetFormatNameI(FFMS_Indexer *Indexer);
FFMS_API(int) FFMS_GetNumFrames(FFMS_Track *T);
FFMS_API(const FFMS_FrameInfo *) FFMS_GetFrameInfo(FFMS_Track *T, int Frame);
FFMS_API(FFMS_Track *) FFMS_GetTrackFromIndex(FFMS_Index *Index, int Track);
FFMS_API(FFMS_Track *) FFMS_GetTrackFromVideo(FFMS_VideoSource *V);
FFMS_API(FFMS_Track *) FFMS_GetTrackFromAudio(FFMS_AudioSource *A);
FFMS_API(const FFMS_TrackTimeBase *) FFMS_GetTimeBase(FFMS_Track *T);
FFMS_API(int) FFMS_WriteTimecodes(FFMS_Track *T, const char *TimecodeFile, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(FFMS_Indexer *) FFMS_CreateIndexer(const char *SourceFile, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(void) FFMS_TrackIndexSettings(FFMS_Indexer *Indexer, int Track, int Index, int); /* Pass 0 to last argument, kapt to preserve abi. Introduced in FFMS_VERSION ((2 << 24) | (21 << 16) | (0 << 8) | 0) */
FFMS_API(void) FFMS_TrackTypeIndexSettings(FFMS_Indexer *Indexer, int TrackType, int Index, int); /* Pass 0 to last argument, kapt to preserve abi. Introduced in FFMS_VERSION ((2 << 24) | (21 << 16) | (0 << 8) | 0) */
FFMS_API(void) FFMS_SetProgressCallback(FFMS_Indexer *Indexer, TIndexCallback IC, void *ICPrivate); /* Introduced in FFMS_VERSION ((2 << 24) | (21 << 16) | (0 << 8) | 0) */
FFMS_API(FFMS_Index *) FFMS_DoIndexing2(FFMS_Indexer *Indexer, int ErrorHandling, FFMS_ErrorInfo *ErrorInfo); /* Introduced in FFMS_VERSION ((2 << 24) | (21 << 16) | (0 << 8) | 0) */
FFMS_API(void) FFMS_CancelIndexing(FFMS_Indexer *Indexer);
FFMS_API(FFMS_Index *) FFMS_ReadIndex(const char *IndexFile, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(FFMS_Index *) FFMS_ReadIndexFromBuffer(const uint8_t *Buffer, size_t Size, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(int) FFMS_IndexBelongsToFile(FFMS_Index *Index, const char *SourceFile, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(int) FFMS_WriteIndex(const char *IndexFile, FFMS_Index *Index, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(int) FFMS_WriteIndexToBuffer(uint8_t **BufferPtr, size_t *Size, FFMS_Index *Index, FFMS_ErrorInfo *ErrorInfo);
FFMS_API(void) FFMS_FreeIndexBuffer(uint8_t **BufferPtr);
FFMS_API(int) FFMS_GetPixFmt(const char *Name);
#endif
