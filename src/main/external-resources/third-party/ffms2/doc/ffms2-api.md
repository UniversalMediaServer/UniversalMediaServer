# FFmpegSource2 API Documentation
FFmpegSource2 (FFMS2) is a wrapper library around Libav/FFmpeg, plus some additional components to deal with file formats libavformat has (or used to have) problems with.
It gives you a convenient way to say "open and decompress this media file for me, I don't care how you do it", without having to bother with the sometimes less than straightforward and less than perfectly documented Libav/FFmpeg internals.
May be frame and/or sample accurate on good days.
The library is written in C++, but the public API is C-friendly and it is possible to simply include and link directly with a pure C application.

The source is MIT licensed and can be obtained from GitHub; see https://github.com/FFMS/ffms2.

## Limitations
FFMS2 does not mux or encode anything.
FFMS2 does not give you fine control over codec internals.
FFMS2 does not let you demux raw compressed data, you get it decompressed or not at all.
FFMS2 does not provide you with a good solution for realtime playback, since it needs to index the input file before you can retreive frames or audio samples.
FFMS2 does not currently handle things like subtitles, file attachments or chapters.
FFMS2's video frame and audio sample retrieval functions are not threadsafe; you may only have one request going at a time, per source context.

## Compilation
FFMS2 has the following dependencies:

 - **[FFmpeg][ffmpeg]**
    - Further recommended configuration options: `--disable-debug --disable-muxers --disable-encoders --disable-filters --disable-hwaccels --disable-network --disable-devices
 - **[zlib][zlib]**

Compiling the library on non-Windows is trivial; the usual `./configure && make && make install` will suffice if FFmpeg and zlib are installed to the default locations.

### Windows-specific compilation notes
You have several options on how to build FFMS2 on Windows.

You can build both FFmpeg and FFMS2 with MinGW-w64, FFmpeg with clang-cl and FFMS2 with VC++ (shared only), or both with VC++.
The standard Avisynth 2.5 plugin requires building FFMS2 with VC++, while the Avisynth C plugin (which supports Avisynth 2.6) requires building with MinGW (and using the `c_plugin`branch). VapouSynth works as-is with MinGW-w64.

These days building everything with MinGW works without doing anything unusual.

You'll have to manually add the location which you installed the headers and libraries to to VC++'s search paths (and if you're building both 32-bit and 64-bit, be sure to add the correct ones).

[ffmpeg]: http://www.ffmpeg.org
[zlib]: http://www.zlib.net

## Quickstart guide for impatient people
If you don't want to know anything about anything and just want to open some video with FFMS2 in the absolutely simplest possible manner, without selective indexing, progress reporting, saved index files, keyframe or timecode reading or anything like that, here's how to do it with the absolutely bare minimum of code.

```c++
#include <ffms.h>

int main (...) {
  /* Initialize the library. */
  FFMS_Init(0, 0);

  /* Index the source file. Note that this example does not index any audio tracks. */
  char errmsg[1024];
  FFMS_ErrorInfo errinfo;
  errinfo.Buffer      = errmsg;
  errinfo.BufferSize  = sizeof(errmsg);
  errinfo.ErrorType   = FFMS_ERROR_SUCCESS;
  errinfo.SubType     = FFMS_ERROR_SUCCESS;
  const char *sourcefile = "somefilename";
  
  FFMS_Indexer *indexer = FFMS_CreateIndexer(sourcefile, &errinfo);
  if (indexer == NULL) {
    /* handle error (print errinfo.Buffer somewhere) */
  }
  
  FFMS_Index *index = FFMS_DoIndexing2(indexer, FFMS_IEH_ABORT, &errinfo);
  if (index == NULL) {
    /* handle error (you should know what to do by now) */
  } 

  /* Retrieve the track number of the first video track */
  int trackno = FFMS_GetFirstTrackOfType(index, FFMS_TYPE_VIDEO, &errinfo);
  if (trackno < 0) {
    /* no video tracks found in the file, this is bad and you should handle it */
    /* (print the errmsg somewhere) */
  }

  /* We now have enough information to create the video source object */
  FFMS_VideoSource *videosource = FFMS_CreateVideoSource(sourcefile, trackno, index, 1, FFMS_SEEK_NORMAL, &errinfo);
  if (videosource == NULL) {
    /* handle error */
  }

  /* Since the index is copied into the video source object upon its creation,
  we can and should now destroy the index object. */
  FFMS_DestroyIndex(index);

  /* Retrieve video properties so we know what we're getting.
  As the lack of the errmsg parameter indicates, this function cannot fail. */
  const FFMS_VideoProperties *videoprops = FFMS_GetVideoProperties(videosource);

  /* Now you may want to do something with the info, like check how many frames the video has */
  int num_frames = videoprops->NumFrames;

  /* Get the first frame for examination so we know what we're getting. This is required
  because resolution and colorspace is a per frame property and NOT global for the video. */
  const FFMS_Frame *propframe = FFMS_GetFrame(videosource, 0, &errinfo);

  /* Now you may want to do something with the info; particularly interesting values are:
  propframe->EncodedWidth; (frame width in pixels)
  propframe->EncodedHeight; (frame height in pixels)
  propframe->EncodedPixelFormat; (actual frame colorspace)
  */

  /* If you want to change the output colorspace or resize the output frame size,
  now is the time to do it. IMPORTANT: This step is also required to prevent
  resolution and colorspace changes midstream. You can you can always tell a frame's
  original properties by examining the Encoded* properties in FFMS_Frame. */
  /* See libavutil/pixfmt.h for the list of pixel formats/colorspaces.
  To get the name of a given pixel format, strip the leading PIX_FMT_
  and convert to lowercase. For example, PIX_FMT_YUV420P becomes "yuv420p". */

  /* A -1 terminated list of the acceptable output formats. */
  int pixfmts[2];
  pixfmts[0] = FFMS_GetPixFmt("bgra");
  pixfmts[1] = -1;

  if (FFMS_SetOutputFormatV2(videosource, pixfmts, propframe->EncodedWidth, propframe->EncodedHeight,
                             FFMS_RESIZER_BICUBIC, &errinfo)) {
    /* handle error */
  }

  /* now we're ready to actually retrieve the video frames */
  int framenumber = 0; /* valid until next call to FFMS_GetFrame* on the same video object */
  const FFMS_Frame *curframe = FFMS_GetFrame(videosource, framenumber, &errinfo);
  if (curframe == NULL) {
    /* handle error */
  }
  /* do something with curframe */
  /* continue doing this until you're bored, or something */

  /* now it's time to clean up */
  FFMS_DestroyVideoSource(videosource);

  /* uninitialize the library. */
  FFMS_Deinit();

  return 0;
}
```
And that's pretty much it. Easy, ain't it?

## Indexing and You
Before opening a media file with FFMS2, you **must** index it.
This is to ensure that keyframe positions, timecode data and other interesting things are known so that frame-accurate seeking is easily possible.

First, you create an indexer object using [FFMS_CreateIndexer][CreateIndexer] and the source filename.
The indexer can be queried for information about the file using [FFMS_GetNumTracksI][GetNumTracksI], [FFMS_GetTrackTypeI][GetTrackTypeI] and [FFMS_GetCodecNameI][GetCodecNameI] to determine how many tracks there are and what their respective types are.
You can configure things like what tracks to index using [FFMS_TrackIndexSettings][TrackIndexSettings], [FFMS_TrackTypeIndexSettings][TrackTypeIndexSettings], [FFMS_SetAudioNameCallback][SetAudioNameCallback] and [FFMS_SetProgressCallback][SetProgressCallback].
When you have done so, you call [FFMS_DoIndexing2][DoIndexing2] to perform the actual indexing.
If you change your mind and decide there are no tracks interesting to you in the file, call [FFMS_CancelIndexing][CancelIndexing].
Both [FFMS_DoIndexing2][DoIndexing2] and [FFMS_CancelIndexing][CancelIndexing] destroys the indexer object and frees its memory.

When you have indexed the file you can write the index object to a disk file using [FFMS_WriteIndex][WriteIndex], which is useful if you expect to open the same file more than once, since it saves you from reindexing it every time.
It can be particularly time-saving with very large files or files with a lot of audio tracks, since both of those can take quite some time to index.

To create an index object from a saved disk file, use [FFMS_ReadIndex][ReadIndex].
Note that index files can only be read by the exact same version of FFMS2 as they were written with.
Attempting to open an index written with a different version will give you an index mismatch error.
If you want to verify that a given index file actually is an index of the source file you think it is, use [FFMS_IndexBelongsToFile][IndexBelongsToFile].

## Constants for primaries, transfer and matrix
The constants for these are the same as defined in ISO/IEC 23001-8_2013 § 7.1-7.3. To avoid unnecessary duplication they are no longer declared in ffms.h. If you still need them either transcribe them yourself or include libavutil/pixfmt.h from FFmpeg.

## Function Reference

Most functions that can fail in one way or another (as well as some that should be able to but currently don't) support error reporting using the `ErrorInfo` parameter.
Example:
```c++
char errmsg[1024];
FFMS_ErrorInfo errinfo;
errinfo.Buffer      = errmsg;
errinfo.BufferSize  = sizeof(errmsg);
errinfo.ErrorType   = FFMS_ERROR_SUCCESS;
errinfo.SubType     = FFMS_ERROR_SUCCESS;

const FFMS_Frame *frame = FFMS_GetFrame(vb, frameno, &errinfo);
/* failure? */
if (frame == NULL) {
  printf("failed to get frame number %d, error message: %s", frameno, errinfo.Buffer);
  /* etc... */
}
```
How many characters you want to allocate to the error message is up to you; if you allocate too few the messages will get truncated.
1024 should be enough for anyone.

[errorhandling]: #function-reference

### FFMS_Init - initializes the library

[Init]: #ffms_init---initializes-the-library
```c++
void FFMS_Init(int Unused, int Unused2);
```
Initializes the FFMS2 library.
This function must be called once at the start of your program, before doing any other FFMS2 function calls.
This function is threadsafe and will only run once.

#### Arguments

##### `int Unused`

This argument is no longer used and is left only for API/ABI compatibility. Pass 0.

##### `int Unused2`

This argument is also no longer used and is left only for API/ABI compatibility. Pass 0.

### FFMS_Deinit - deinitializes the library

[Deinit]: ##ffms_deinit---deinitializes-the-library
```c++
void FFMS_Deinit()
```
Deinitializes the FFMS2 library.
This function must be called once at the end of your program, and no more FFMS2 function calls must be made.
This function is threadsafe and will only be run once.

### FFMS_GetLogLevel - gets FFmpeg message level

[GetLogLevel]: #ffms_getloglevel---gets-ffmpeg-message-level
```c++
int FFMS_GetLogLevel();
```
Retrieves FFmpeg's current logging/message level (i.e. how much diagnostic noise it prints to `STDERR`).
If you want to make any sense of the returned value you'd better `#include <libavutil/log.h>` from the FFmpeg source tree to get the relevant constant definitions.
Alternatively, just copy the relevant constant definitions into your own code and hope the FFmpeg devs doesn't change them randomly for no particular reason like they do with everything else.

### FFMS_SetLogLevel - sets FFmpeg message level

[SetLogLevel]: #ffms_setloglevel---sets-ffmpeg-message-level
```c++
void FFMS_SetLogLevel(int Level);
```
Sets FFmpeg's logging/message level; see [FFMS_GetLogLevel][GetLogLevel] for details.

### FFMS_CreateVideoSource - creates a video source object

[CreateVideoSource]: #ffms_createvideosource---creates-a-video-source-object
```c++
FFMS_VideoSource *FFMS_CreateVideoSource(const char *SourceFile, int Track, FFMS_Index *Index,
    int Threads, int SeekMode, FFMS_ErrorInfo *ErrorInfo);
```
Creates a `FFMS_VideoSource` object with the given properties.
The `FFMS_VideoSource` object represents a video stream, and can be passed to other functions to retreive frames and metadata from said stream.
The video stream in question must be indexed first (see the indexing functions).
Note that the index object is copied into the `FFMS_VideoSource` object upon its creation, so once you've created the video source you can generally destroy the index object immediately, since all info you can retrieve from it is also retrievable from the `FFMS_VideoSource` object.

#### Arguments

##### `const char *SourceFile`
The source file to open. Can be an absolute or relative path.

##### `int Track`
The track number of the video track to open, as seen by the relevant demuxer.
See [FFMS_GetNumTracks][GetNumTracks], [FFMS_GetTrackType][GetTrackType], [FFMS_GetFirstTrackOfType][GetFirstTrackOfType] and their variants for further information on how to determine this.

##### `FFMS_Index *Index`
A pointer to a FFMS_Index object containing indexing information for the track you want to open.

##### `int Threads`
The number of decoding threads to use.
Anything less than 1 will use threads equal to the number of CPU cores.
Values >1 have no effect if FFmpeg was not compiled with threading support.

##### `int SeekMode`
For a list of valid values, see [FFMS_SeekMode][SeekMode].
Controls how seeking (random access) is handled and hence affects frame accuracy.
You will almost always want to use `FFMS_SEEK_NORMAL`.

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns a pointer to the created `FFMS_VideoSource` object on success.
Returns `NULL` and sets `ErrorMsg` on failure.

### FFMS_CreateAudioSource - creates an audio source object

[CreateAudioSource]: #ffms_createaudiosource---creates-an-audio-source-object
```c++
FFMS_AudioSource *FFMS_CreateAudioSource(const char *SourceFile, int Track, FFMS_Index *Index, int DelayMode,
    FFMS_ErrorInfo *ErrorInfo);
```
Does exactly the same thing as [FFMS_CreateVideoSource][CreateVideoSource], but for audio tracks.
Except for DelayMode, arguments and return values are identical.

#### Arguments

##### `int DelayMode`
Controls how audio with a non-zero first PTS is handled; in other words what FFMS does about audio delay.
Possible arguments are:

 - **FFMS_DELAY_NO_SHIFT**: No adjustment is made; the first decodable audio sample becomes the first sample in the output.
   May lead to audio/video desync.
 - **FFMS_DELAY_TIME_ZERO**: Samples are created (with silence) or discarded so that sample 0 in the decoded audio starts at time zero.
 - **FFMS_DELAY_FIRST_VIDEO_TRACK**: Samples are created (with silence) or discarded so that sample 0 in the decoded audio starts at the same time as frame 0 of the first video track.
   This is what most users want and is a sane default.
 - **Any integer >= 0**: Identical to `FFMS_DELAY_FIRST_VIDEO_TRACK`, but interprets the argument as a track number and adjusts the audio relative to the video track with that number (if the given track number isn't a video track, audio source creation will fail).

### FFMS_DestroyVideoSource, FFMS_DestroyAudioSource - deallocates a video or audio source object

[DestroyVideoSource]: #ffms_destroyvideosource-ffms_destroyaudiosource---deallocates-a-video-or-audio-source-object
[DestroyAudioSource]: #ffms_destroyvideosource-ffms_destroyaudiosource---deallocates-a-video-or-audio-source-object
```c++
void FFMS_DestroyVideoSource(FFMS_VideoSource *V);
void FFMS_DestroyAudioSource(FFMS_AudioSource *A);
```
Deallocates the given `FFMS_VideoSource` or `FFMS_AudioSource` object and frees the memory allocated by [FFMS_CreateVideoSource][CreateVideoSource] or [FFMS_CreateAudioSource][CreateAudioSource], respectively.

### FFMS_GetVideoProperties - retrieves video properties

[GetVideoProperties]: #ffms_getvideoproperties---retrieves-video-properties
```c++
const FFMS_VideoProperties *FFMS_GetVideoProperties(FFMS_VideoSource *V);
```
Retreives the video properties from the given `FFMS_VideoSource` object and stores them in a [FFMS_VideoProperties][VideoProperties] struct.
Returns a pointer to said struct.

### FFMS_GetAudioProperties - retrieves audio properties

[GetAudioProperties]: #ffms_getaudioproperties---retrieves-audio-properties
```c++
const FFMS_AudioProperties *FFMS_GetAudioProperties(FFMS_AudioSource *A);
```
Does the exact same thing as [FFMS_GetVideoProperties][GetVideoProperties], but for an `FFMS_AudioSource` object.
See [FFMS_AudioProperties][AudioProperties].

### FFMS_GetFrame - retrieves a given video frame

[GetFrame]: #ffms_getframe---retrieves-a-given-video-frame
```c++
const FFMS_Frame *FFMS_GetFrame(FFMS_VideoSource *V, int n, FFMS_ErrorInfo *ErrorInfo);
```
Gets and decodes a video frame from the video stream represented by the given `FFMS_VideoSource` object and stores it in a [FFMS_Frame][Frame] struct.
The colorspace and resolution of the frame can be changed by calling [FFMS_SetOutputFormatV2][SetOutputFormatV2] with the appropriate parameters before calling this function.
Note that this function is not thread-safe (you can only request one frame at a time from a given `FFMS_VideoSource` object) and that the returned pointer to the `FFMS_Frame` is a `const` pointer.

#### Arguments

##### `FFMS_VideoSource *V`
A pointer to the `FFMS_VideoSource` object that represents the video stream you want to retrieve a frame from.

##### `int n`
The frame number to get. Frame numbering starts from zero, and hence the first frame is number 0 (not 1) and the last frame is number `FFMS_VideoProperties->NumFrames` minus 1.
Requesting a frame number beyond the stream end or before the stream start (i.e. negative) may cause undefined behavior.

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns a pointer to the `FFMS_Frame` on success. Returns `NULL` and sets `ErrorMsg` on failure.

The returned frame is owned by the given `FFMS_VideoSource`, and remains valid until the video source is destroyed, a different frame is requested from the video source, or the video source's input or output format is changed.
Note that while `FFMS_GetFrame` tends to return the same pointer with each call, it is not safe to rely on this as the output frame will sometimes be reallocated.

### FFMS_GetFrameByTime - retrieves a video frame at a given timestamp

[GetFrameByTime]: #ffms_getframebytime---retrieves-a-video-frame-at-a-given-timestamp
```c++
const FFMS_Frame *FFMS_GetFrameByTime(FFMS_VideoSource *V, double Time, FFMS_ErrorInfo *ErrorInfo);
```
Does the exact same thing as [FFMS_GetFrame][GetFrame] except instead of giving it a frame number you give it a timestamp in seconds, and it will retrieve the frame that starts closest to that timestamp.
This function exists for the people who are too lazy to build and traverse a mapping between frame numbers and timestamps themselves.

### FFMS_GetAudio - decodes a number of audio samples

[GetAudio]: #ffms_getaudio---decodes-a-number-of-audio-samples
```c++
int FFMS_GetAudio(FFMS_AudioSource *A, void *Buf, int64_t Start, int64_t Count, FFMS_ErrorInfo *ErrorInfo);
```
Decodes the requested audio samples from the audio stream represented by the given `FFMS_AudioSource` object and stores them in the given buffer.
Note that this function is not thread-safe; you can only request one decoding operation at a time from a given `FFMS_AudioSource` object.

#### Arguments

##### `FFMS_AudioSource *A`
A pointer to the `FFMS_AudioSource` object that represents the audio stream you want to get samples from.

##### `void *Buf`
A pointer to the buffer where the decoded samples will end up.
You are responsible for allocating and freeing this buffer yourself, so you better check the [FFMS_AudioProperties][GetAudioProperties] for the sample format, number of channels, channel layout etc first, so you know how much memory you need to allocate.
The formula to calculate the required size of the buffer is (obviously) `num_bytes = bytes_per_sample * num_channels * num_samples`.

##### `int64_t Start, int64_t Count`
The range of samples you want decoded.
The output is `Count` samples long, starting from `Start` (inclusive).
Like video frame numbers, sample numbers start from zero and hence the last sample in the stream is number `FFMS_AudioProperties->NumSamples` minus 1.
Requesting samples beyond the stream end or before the stream start may result in undefined behavior.

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns 0 on success.
Returns non-0 and sets `ErrorMsg` on failure.

### FFMS_SetOutputFormatV2 - sets the output format for video frames

[SetOutputFormatV2]: #ffms_setoutputformatv2---sets-the-output-format-for-video-frames
```c++
int FFMS_SetOutputFormatV2(FFMS_VideoSource *V, int *TargetFormats, int Width, int Height, int Resizer,
    FFMS_ErrorInfo *ErrorInfo);
```
Sets the colorspace and frame dimensions to be used for output of frames from the given `FFMS_VideoSource` by all further calls to [FFMS_GetFrame][GetFrame] and [FFMS_GetFrameByTime][GetFrameByTime], until next time you call `FFMS_SetOutputFormatV2` or [FFMS_ResetOutputFormatV][ResetOutputFormatV].
You can change the output format at any time without having to reinitialize the `FFMS_VideoSource` object or anything else.
Can be used to convert the video to grayscale or monochrome if you are so inclined.
If you provided a list of more than one colorspace/pixelformat, you should probably check the [FFMS_Frame][Frame] properties afterwards to see which one got selected.
And remember to do so for EVERY frame or you may get a nasty surprise.
Added in version 2.16.3.0 and replaces `FFMS_SetOutputFormatV`.

#### Arguments

##### `FFMS_VideoSource *V`
A pointer to the `FFMS_VideoSource` object that represents the video stream you want to change the output format for.

##### `int *TargetFormats`
The desired output colorspace(s).
It is a -1 terminated list of acceptable output colorspace to choose from.
The destination that gives the least lossy conversion from the source colorspace will automatically be selected, **on a frame basis**.
To get the integer constant representing a given colorspace, see [FFMS_GetPixFmt][GetPixFmt].

Example:
```c++
int targetformats[3];
targetformats[0] = pixfmt1;
targetformats[1] = pixfmt2;
targetformats[2] = -1;
```

##### `int Width, int Height`
The desired image dimensions, in pixels.
If you do not want to resize just pass the input dimensions.
Passing invalid dimensions (like 0 or negative) has undefined behavior.

##### `int Resizer`
The desired image resizing algorithm, represented by an integer as enumerated in [FFMS_Resizers][Resizers].
You must choose one even if you're not actually rescaling the image, because the video may change resolution mid-stream and then you will be using a resizer whether you want it or not (you will only know that the resolution changed after you actually decoded a frame with a new resolution), and it may also get used for rescaling subsampled chroma planes.

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns 0 on success.
Returns non-0 and sets `ErrorMsg` on failure.

### FFMS_ResetOutputFormatV - resets the video output format

[ResetOutputFormatV]: #ffms_resetoutputformatv---resets-the-video-output-format
```c++
void FFMS_ResetOutputFormatV(FFMS_VideoSource *V);
```
Resets the output format for the given `FFMS_VideoSource` object so that no conversion takes place.
Note that the results of this function may vary wildly, particularly if the video changes resolution mid-stream.
If you call it, you'd better call [FFMS_GetFrame][GetFrame] afterwards and examine the properties to see what you actually ended up with.

### FFMS_SetInputFormatV - override the source format for video frames

[SetInputFormatV]: #ffms_setinputformatv---override-the-source-format-for-video-frames
```c++
int FFMS_SetInputFormatV(FFMS_VideoSource *V, int ColorSpace, int ColorRange, int PixelFormat,
    FFMS_ErrorInfo *ErrorInfo);
```
Override the source colorspace passed to SWScale for conversions and resizing for all further calls to [FFMS_GetFrame][GetFrame] and [FFMS_GetFrameByTime][GetFrameByTime], until next time you call `FFMS_SetInputFormatV` or [FFMS_ResetInputFormatV][ResetInputFormatV].
You can change the input format at any time without having to reinitialize the `FFMS_VideoSource` object or anything else.
This is intended primarily for compatibility with programs which use the wrong YUV colorspace when converting to or from RGB, but can also be useful for files which have incorrect colorspace flags.
Values passed are not checked for sanity; if you wish you may tell FFMS2 to pretend that a RGB files is actually YUV using this function, but doing so is unlikely to have useful results.
This function only has an effect if the output format is also set with [FFMS_SetOutputFormatV2][SetOutpurFormatV2].
Added in version 2.17.1.0.

#### Arguments

##### `FFMS_VideoSource *V`
A pointer to the `FFMS_VideoSource` object that represents the video stream you want to change the input format for.

##### `int ColorSpace`
The desired input colorspace, or FFMS_CS_UNSPECIFIED to leave it unchanged.
See [FFMS_ColorSpaces][ColorSpaces].

##### `int ColorRange`
The desired input colorrange, or FFMS_CR_UNSPECIFIED to leave it unchanged.
See [FFMS_ColorRanges][ColorRanges].

##### `int PixelFormat`
The desired input pixel format; see [FFMS_GetPixFmt][GetPixFmt]. `FFMS_GetPixFmt("")` will leave the pixel format unchanged.

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns 0 on success.
Returns non-0 and sets `ErrorMsg` on failure.

### FFMS_ResetInputFormatV - resets the video input format

[ResetInputFormatV]: #ffms_resetinputformatv---resets-the-video-input-format
```c++
void FFMS_ResetInputFormatV(FFMS_VideoSource *V);
```
Resets the input format for the given `FFMS_VideoSource` object to the values specified in the source file.

### FFMS_DestroyIndex - deallocates an index object

[DestroyIndex]: #ffms_destroyindex---deallocates-an-index-object
```c++
void FFMS_DestroyFFMS_Index(FFMS_Index *Index);
```
Deallocates the given `FFMS_Index` object and frees the memory that was allocated when it was created.

### FFMS_GetErrorHandling - gets which error handling mode was used when creating the given index

[GetErrorHandling]: #ffms_geterrorhandling---gets-which-error-handling-mode-was-used-when-creating-the-given-index
```c++
int FFMS_GetErrorHandling(FFMS_Index *Index);
```
Returns the value of the ErrorHandling parameter which was passed to [FFMS_DoIndexing2][DoIndexing2].

### FFMS_GetFirstTrackOfType - gets the track number of the first track of a given type

[GetFirstTrackOfType]: #ffms_getfirsttrackoftype---gets-the-track-number-of-the-first-track-of-a-given-type
```c++
int FFMS_GetFirstTrackOfType(FFMS_Index *Index, int TrackType, FFMS_ErrorInfo *ErrorInfo);
```
Finds the first track of the given [FFMS_TrackType][TrackType] in the given `FFMS_Index` and returns its track number, suitable for use as an argument to [FFMS_CreateVideoSource][CreateVideoSource] or [FFMS_CreateAudioSource][CreateAudioSource], as well as to some other functions.

#### Arguments

##### `FFMS_Index *Index`
A pointer to the `FFMS_Index` object that represents the media file you want to look for tracks in.

##### `int TrackType`
The track type to look for.
See [FFMS_TrackType][TrackType].

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns the track number (an integer greater than or equal to 0) on success.
Returns a negative integer and sets ErrorMsg on failure (i.e. if no track of the given type was found).

### FFMS_GetFirstIndexedTrackOfType - gets the track number of the first track of a given type

[GetFirstIndexedTrackOfType]: #ffms_getfirstindexedtrackoftype---gets-the-track-number-of-the-first-track-of-a-given-type
```c++
int FFMS_GetFirstIndexedTrackOfType(FFMS_Index *Index, int TrackType, FFMS_ErrorInfo *ErrorInfo);
```
Does the exact same thing as [FFMS_GetFirstTrackOfType][GetFirstTrackOfType] but ignores tracks that have not been indexed.

### FFMS_GetNumTracks - gets the number of tracks in a given index

[GetNumTracks]: #ffms_getnumtracks---gets-the-number-of-tracks-in-a-given-index
```c++
int FFMS_GetNumTracks(FFMS_Index *Index);
```
Returns the total number of tracks in the media file represented by the given `FFMS_Index`.

### FFMS_GetNumTracksI - gets the number of tracks in a given indexer

[GetNumTracksI]: #ffms_getnumtracksi---gets-the-number-of-tracks-in-a-given-indexer
```c++
int FFMS_GetNumTracksI(FFMS_Indexer *Indexer);
```
Returns the total number of tracks in the media file represented by the given `FFMS_Indexer`.
In other words, does the same thing as [FFMS_GetNumTracks][GetNumTracks] but does not require indexing the entire file first.

### FFMS_GetTrackType - gets the track type of a given track

[GetTrackType]: #ffms_gettracktype---gets-the-track-type-of-a-given-track
```c++
int FFMS_GetTrackType(FFMS_Track *T);
```
Returns an integer representing the [FFMS_TrackType][TrackType] of the track represented by the given `FFMS_Track` object.

### FFMS_GetTrackTypeI - gets the track type of a given track

[GetTrackTypeI]: #ffms_gettracktypei---gets-the-track-type-of-a-given-track
```c++
int FFMS_GetTrackTypeI(FFMS_Indexer *Indexer, int Track);
```
Returns an integer representing the [FFMS_TrackType][TrackType] of the track number `Track` in the media file represented by the given `FFMS_Indexer`.
In other words, does the same thing as [FFMS_GetTrackType][GetTrackType], but does not require having indexed the file first.
If you have indexed the file, use [FFMS_GetTrackType][GetTrackType] instead since the `FFMS_Indexer` object is destructed when the index is created.
Note that specifying an invalid track number may lead to undefined behavior.

### FFMS_GetCodecNameI - gets the name of the codec used for a given track

[GetCodecNameI]: #ffms_getcodecnamei---gets-the-name-of-the-codec-used-for-a-given-track
```c++
const char *FFMS_GetCodecNameI(FFMS_Indexer *Indexer, int Track);
```
Returns the human-readable name ("long name" in FFmpeg terms) of the codec used in the given track number in the media file represented by the given `FFMS_Indexer` object.
Useful if you want to, say, pop up a menu asking the user which tracks he or she wishes to index.
Note that specifying an invalid track number may lead to undefined behavior.

### FFMS_GetFormatNameI - gets the name of the container format used in the given indexer

[GetFormatNameI]: #ffms_getformatnamei---gets-the-name-of-the-container-format-used-in-the-given-indexer
```c++
const char *FFMS_GetFormatNameI(FFMS_Indexer *Indexer);
```
Returns the human-readable name ("long name" in FFmpeg terms) of the container format used by the file represented by the given `FFMS_Indexer`.

### FFMS_GetNumFrames - gets the number of frames in a given track

[GetNumFrames]: #ffms_getnumframes---gets-the-number-of-frames-in-a-given-track
```c++
int FFMS_GetNumFrames(FFMS_Track *T);
```
Returns the number of frames in the track represented by the given `FFMS_Track`.
For a video track this is the number of video frames, which can be useful; for an audio track it's the number of packets, which is almost never useful, since nothing in the API exposes those.
A return value of 0 indicates the track has not been indexed.

### FFMS_GetFrameInfo - gets information about a given frame

[GetFrameInfo]: #ffms_getframeinfo---gets-information-about-a-given-frame
```c++
const FFMS_FrameInfo *FFMS_GetFrameInfo(FFMS_Track *T, int Frame);
```
Gets information about the given frame (identified by its frame number) from the indexing information in the given `FFMS_Track` and stores it in a [FFMS_FrameInfo][FrameInfo] struct.
Using this function on a `FFMS_Track` representing a non-video track has undefined behavior.

#### Arguments

##### `FFMS_Track *T`
A pointer to the `FFMS_Track` object that represents the video track containing the frame you want to get information about.

##### `int Frame`
The frame number to get information about.
See [FFMS_GetFrame][GetFrame] for information about frame numbers.
Requesting information about a frame before the start or after the end of the video track may result in undefined behavior, so don't do that.

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns a pointer to the [FFMS_FrameInfo][FrameInfo] struct on success.
Returns `NULL` and sets `ErrorMsg` on failure.

### FFMS_GetTrackFromIndex - retrieves track info from an index

[GetTrackFromIndex]: #ffms_gettrackfromindex---retrieves-track-info-from-an-index
```c+++
FFMS_Track *FFMS_GetTrackFromIndex(FFMS_Index *Index, int Track);
```
Gets track data for the given track number from the given `FFMS_Index` object, stores it in a `FFMS_Track` object and returns a pointer to it.
Use this function if you don't want to (or cannot) open the track with [FFMS_CreateVideoSource][CreateVideoSource] or [FFMS_CreateAudioSource][CreateAudioSource] first.
If you already have a `FFMS_VideoSource` or `FFMS_AudioSource` object it's safer to use [FFMS_GetTrackFromVideo][GetTrackFromVideo] or [FFMS_GetTrackFromAudio][GetTrackFromAudio] instead.
Note that specifying a nonexistent or invalid track number leads to undefined behavior (usually an access violation).
Also note that the returned `FFMS_Track` object is only valid until its parent `FFMS_Index` object is destroyed.

#### Arguments

##### `FFMS_Index *Index`
A pointer to the `FFMS_Index` object that represents the media file containing the track whose index information you want to get.

##### `int Track`
The track number, as seen by the relevant demuxer (see [FFMS_GetNumTracks][GetNumTracks], [FFMS_GetTrackType][GetTrackType], [FFMS_GetFirstTrackOfType][GetFirstTrackOfType] and their variants).

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns the `FFMS_Track` on success.
Note that requesting indexing information for a track that has not been indexed will not cause an error, it will just return an empty `FFMS_Track` (check for >0 frames using [FFMS_GetNumFrames][GetNumFrames] to see if the returned object actually contains indexing information).

### FFMS_GetTrackFromVideo, FFMS_GetTrackFromAudio - retrieves track info from audio or video source

[GetTrackFromVideo]: #ffms_gettrackfromvideo-ffms_gettrackfromaudio---retrieves-track-info-from-audio-or-video-source
[GetTrackFromAudio]: #ffms_gettrackfromvideo-ffms_gettrackfromaudio---retrieves-track-info-from-audio-or-video-source
```c++
FFMS_Track *FFMS_GetTrackFromVideo(FFMS_VideoSource *V);
FFMS_Track *FFMS_GetTrackFromAudio(FFMS_AudioSource *A);
```
Gets information about the track represented by the given `FFMS_VideoSource` or `FFMS_AudioSource` object and returns a pointer to a `FFMS_Track` object containing said information.
It's generally safer to use these functions instead of [FFMS_GetTrackFromIndex][GetTrackFromIndex], since unlike that function they cannot cause access violations if you specified an nonexistent tracknumber, return a `FFMS_Track` that doesn't actually contain any indexing information, or return an object that ceases to be valid when the index is destroyed.
Note that the returned `FFMS_Track` object is only valid until its parent `FFMS_VideoSource` or `FFMS_AudioSource` object is destroyed.

### FFMS_GetTimeBase - retrieves the time base for the given track

[GetTimeBase]: #ffms_gettimebase---retrieves-the-time-base-for-the-given-track
```c++
const FFMS_TrackTimeBase *FFMS_GetTimeBase(FFMS_Track *T);
```
Finds the basic time unit for the track represented by the given `FFMS_Track`, stores it in a [FFMS_TrackTimeBase][TrackTimeBase] struct and returns a pointer to said struct.
Note that it is only meaningful for video tracks.

### FFMS_WriteTimecodes - writes timecodes for the given track to disk

[WriteTimecodes]: #ffms_writetimecodes---writes-timecodes-for-the-given-track-to-disk
```c++
int FFMS_WriteTimecodes(FFMS_Track *T, const char *TimecodeFile, FFMS_ErrorInfo *ErrorInfo);
```
Writes Matroska v2 timecodes for the track represented by the given `FFMS_Track` to the given file.
Only meaningful for video tracks.

#### Arguments

##### `FFMS_Track *T`
A pointer to the `FFMS_Track` object that represents the video track you want to write timecodes for.

##### `const char *TimecodeFile`
The filename to write to.
Can be a relative or absolute path.
The file will be truncated and overwritten if it already exists.

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns 0 on success.
Returns non-0 and sets `ErrorMsg` on failure.

### FFMS_CreateIndexer - creates an indexer object for the given file

[CreateIndexer]: #ffms_createindexer---creates-an-indexer-object-for-the-given-file
```c++
FFMS_Indexer *FFMS_CreateIndexer(const char *SourceFile, FFMS_ErrorInfo *ErrorInfo);
```
Creates a `FFMS_Indexer` object for the given `SourceFile` and returns a pointer to it.
See [Indexing and You](#indexing-and-you) for details on how to use the indexer.
Is basically a shorthand for `FFMS_CreateIndexerWithDemuxer(SourceFile, FFMS_SOURCE_DEFAULT, ErrorInfo)`.

#### Return values
Returns a pointer to the `FFMS_Indexer` on success.
Returns `NULL` and sets `ErrorMsg` on failure.

#### Return values
Returns a pointer to the `FFMS_Indexer` on success.
Returns `NULL` and sets `ErrorMsg` on failure.

### FFMS_DoIndexing2 - indexes the file represented by an indexer object

[DoIndexing2]: #ffms_doindexing2---indexes-the-file-represented-by-an-indexer-object
```c++
FFMS_Index *FFMS_DoIndexing2(FFMS_Indexer *Indexer, int ErrorHandling, FFMS_ErrorInfo *ErrorInfo);
```
Runs the passed indexer and returns a `FFMS_Index` object representing the file in question.
See the Indexing and You section for more details about indexing.
Note that calling this function destroys the `FFMS_Indexer` object and frees the memory allocated by [FFMS_CreateIndexer][CreateIndexer] (even if indexing fails for any reason).

#### Arguments

##### `FFMS_Indexer *Indexer`
The Indexer to run.
Created with [FFMS_CreateIndexer][CreateIndexer] and configured with [FFMS_TrackIndexSettings][TrackIndexSettings], [FFMS_TrackTypeIndexSettings][TrackTypeIndexSettings], [FFMS_SetAudioNameCallback][SetAudioNameCallback] and [FFMS_SetProgressCallback][SetProgressCallback].

##### `int ErrorHandling`
Depending on the setting audio decoding errors will have different results.
See [FFMS_IndexErrorHandling][IndexErrorHandling] for valid values.
FFMS_IEH_STOP_TRACK should be the best default to just make it work.
Has no effect for tracks which have dumping enabled, which will always cause indexing to fail on audio decoding errors.

##### `FFMS_ErrorInfo *ErrorInfo`
See [Error handling][errorhandling].

#### Return values
Returns a pointer to the created `FFMS_Index` on success.
Returns `NULL` and sets `ErrorMsg` on failure.


### FFMS_TrackIndexSettings - enable or disable indexing of a track

[TrackIndexSettings]: #ffms_trackindexsettings---enable-or-disable-indexing-of-a-track
```c++
void FFMS_TrackIndexSettings(FFMS_Indexer *Indexer, int Track, int Index, int Dump);
```

Enable or disable indexing of a track, and for audio tracks, enable or disable dumping the decoded audio during indexing.

#### Arguments

##### `FFMS_Indexer *Indexer`
The indexer to configure.

##### `int Track`
The track index to configure.

##### `int Index`
Enable indexing the given track if non-zero, and disable if zero.
By default, all video tracks are indexed and all audio tracks are not.

##### `int Dump`
Currently ignored.

By default, no tracks are dumped.

### FFMS_TrackTypeIndexSettings - enable or disable indexing of a tracks of a given type

[TrackTypeIndexSettings]: #ffms_tracktypeindexsettings---enable-or-disable-indexing-of-a-tracks-of-a-given-type
```c++
void FFMS_TrackTypeIndexSettings(FFMS_Indexer *Indexer, int TrackType, int Index, int Dump);
```

Like [FFMS_TrackIndexSettings][TrackIndexSettings], but configures all tracks of the given [FFMS_TrackType][TrackType] at once.

### FFMS_SetProgressCallback - set callback function for indexing progress updates

[SetProgressCallback]: #ffms_setprogresscallback---set-callback-function-for-indexing-progress-updates
```c++
void FFMS_SetProgressCallback(FFMS_Indexer *Indexer, TIndexCallback IC, void *ICPrivate);
```

If you supply a progress callback, FFMS2 will call it regularly during indexing to report progress and give you the chance to interrupt indexing.

The callback should have the following signature:
```c++
int FFMS_CC FunctionName(int64_t Current, int64_t Total, void *ICPrivate);
```
The callback function's arguments are as follows:
 - `int64_t Current, int64_t Total` - The indexing progress (amount done/total amount).
 - `void *Private` - the same pointer as the one you passed as the `Private` argument to `FFMS_SetProgressCallback`.
   Can be used for anything you like, but one example (in a GUI program) is to use it for passing a progress ticker object that you can update with each call to the indexing function.

Return 0 from the callback function to continue indexing, non-0 to cancel indexing (returning non-0 will make `FFMS_DoIndexing2` fail with the reason "indexing cancelled by user").

### FFMS_CancelIndexing - destroys the given indexer object

[CancelIndexing]: #ffms_cancelindexing---destroys-the-given-indexer-object
```c++
void FFMS_CancelIndexing(FFMS_Indexer *Indexer);
```
Destroys the given `FFMS_Indexer` object and frees the memory allocated by [FFMS_CreateIndexer][CreateIndexer].

### FFMS_ReadIndex - reads an index file from disk

[ReadIndex]: #ffms_readindex---reads-an-index-file-from-disk
```c++
FFMS_Index *FFMS_ReadIndex(const char *IndexFile, FFMS_ErrorInfo *ErrorInfo);
```
Attempts to read indexing information from the given `IndexFile`, which can be an absolute or relative path.
Returns the `FFMS_Index` on success; returns `NULL` and sets `ErrorMsg` on failure.

### FFMS_ReadIndexFromBuffer - reads an index from a user-supplied buffer

[ReadIndexFromBuffer]: #ffms_readindexfrombffer---reads-an-index-from-a-user-supplied-buffer
```c++
FFMS_Index *FFMS_ReadIndexFromBuffer(const uint8_t *Buffer, size_t Size, FFMS_ErrorInfo *ErrorInfo);
```

Attempts to read indexing information from the super supplied buffer, `Buffer, of size `Size`.
Returns the `FFMS_Index` on success; returns `NULL` and sets `ErrorMsg` on failure.

### FFMS_IndexBelongsToFile - check if a given index belongs to a given file

[IndexBelongsToFile]: #ffms_indexbelongstofile---check-if-a-given-index-belongs-to-a-given-file
```c++
int FFMS_IndexBelongsToFile(FFMS_Index *Index, const char *SourceFile, FFMS_ErrorInfo *ErrorInfo);
```
Makes a heuristic (but very reliable) guess about whether the given `FFMS_Index` is an index of the given `SourceFile` or not.
Useful to determine if the index object you just read with [FFMS_ReadIndex][ReadIndex] is actually relevant to your interests, since the only two ways to pair up index files with source files are a) trust the user blindly, or b) comparing the filenames; neither is very reliable.

#### Arguments

##### `FFMS_Index *Index`
The index object to check.

##### `const char *SourceFile`
The source file to verify the index against.

#### Return values
Returns 0 if the given index is determined to belong to the given file.
Returns non-0 and sets `ErrorMsg` otherwise.

### FFMS_WriteIndex - writes an index object to disk

[WriteIndex]: #ffms_writeindex---writes-an-index-object-to-disk
```c++
int FFMS_WriteIndex(const char *IndexFile, FFMS_Index *TrackIndices, FFMS_ErrorInfo *ErrorInfo);
```
Writes the indexing information from the given `FFMS_Index` to the given `IndexFile` (which can be an absolute or relative path; it will be truncated and overwritten if it already exists).
Returns 0 on success; returns non-0 and sets `ErrorMsg` on failure.

### FFMS_WriteIndexToBuffer - writes an index to memory

[WriteIndexToBuffer]: #ffms_writeindextobuffer---writes-an-index-to-memory
```c++
int FFMS_WriteIndexToBuffer(uint8_t **BufferPtr, size_t *Size, FFMS_Index *Index, FFMS_ErrorInfo *ErrorInfo)
```

Writes the indexing information from the given `FFMS_Index` to memory, and sets `BufferPtr` to point to the buffer, and `Size` to the size of the returned buffer. Users are required to free the resulting buffer with [FFMS_FreeIndexBuffer][FreeIndexBuffer].
Returns 0 on success; returns non-0 and sets `ErrorMsg` on failure.

### FFMS_FreeIndexBuffer - frees a buffer allocated by [FFMS_WriteIndexToBuffer][WriteIndexToBuffer]

[FreeIndexBuffer]: #ffms_freeindexbuffer---frees-a-buffer-alloctaed-by-ffms_writeindextobuffer
```c++
void FFMS_FreeIndexBuffer(uint8_t **BufferPtr)
```

Frees the buffer pointed to by `BufferPtr` and sets it to NULL.

### FFMS_GetPixFmt - gets a colorspace identifier from a colorspace name

[GetPixFmt]: #ffms_getpixfmt---gets-a-colorspace-identifier-from-a-colorspace-name
```c++
int FFMS_GetPixFmt(const char *Name);
```
Translates a given colorspace/pixel format `Name` to an integer constant representing it, suitable for passing to [FFMS_SetOutputFormatV2][SetOutputFormatV2] (after some manipulation, see that function for details).
This function exists so that you don't have to include a FFmpeg header file in every single program you ever write.
For a list of colorspaces and their names, see `libavutil/pixfmt.h`.
To get the name of a colorspace, strip the leading `PIX_FMT_` and convert the remainder to lowercase.
For example, the name of `PIX_FMT_YUV420P` is `yuv420p`.
It is strongly recommended to use this function instead of including pixfmt.h directly, since this function guarantees that you will always get the constant definitions from the version of FFmpeg that FFMS2 was linked against.

#### Arguments

##### `const char *Name`
The name of the desired colorspace/pixel format, as a nul-terminated ASCII string.

#### Return values
Returns the integer constant representing the given colorspace/pixel format on success.
Returns the integer constant representing `PIX_FMT_NONE` (that is, -1) on failure (i.e. if no matching colorspace was found), but note that you can call `FFMS_GetPixFmt("none")` and get the same return value without it being a failed call, strictly speaking.

### FFMS_GetVersion - returns FFMS_VERSION constant

[GetVersion]: #ffms_getversion---returns-ffms_version-constant
```c++
int FFMS_GetVersion();
```
Returns the FFMS_VERSION constant as defined in ffms.h as an integer.

## Data Structures
The following public data structures may be of interest.

### FFMS_Frame

[Frame]: #ffms_frame
```c++
typedef struct {
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
  int ColorPrimaries;
  int TransferCharateristics;
  int ChromaLocation;
  int HasMasteringDisplayPrimaries;
  double MasteringDisplayPrimariesX[3];
  double MasteringDisplayPrimariesY[3];
  double MasteringDisplayWhitePointX;
  double MasteringDisplayWhitePointY;
  int HasMasteringDisplayLuminance;
  double MasteringDisplayMinLuminance;
  double MasteringDisplayMaxLuminance;
  int HasContentLightLevel;
  unsigned int ContentLightLevelMax;
  unsigned int ContentLightLevelAverage;
} FFMS_Frame;
```
A struct representing a video frame.
The fields are:
 - `uint8_t *Data[4]` - An array of pointers to the picture planes (fields containing actual pixel data).
   Planar formats use more than one plane, for example YV12 uses one plane each for the Y, U and V data.
   Packed formats (such as the various RGB32 flavors) use only the first plane.
   If you want to determine if plane `i` contains data or not, check for `FFMS_Frame->Linesize[i] != 0`.
 - `int Linesize[4]` - An array of integers representing the length of each scan line in each of the four picture planes, in bytes.
   In alternative terminology, this is the "pitch" of the plane.
   Usually, the total size in bytes of picture plane `i` is `FFMS_Frame->Linesize[i] * FFMS_VideoProperties->Height`, but do note that some pixel formats (most notably YV12) has vertical chroma subsampling, and then the U/V planes may be of a different height than the primary plane.
   This may be negative; if so the image is stored inverted in memory and `Data` actually points of the last row of the data.
   You usually do not need to worry about this, as it mostly works correctly by default if you're processing the image correctly.
 - `int EncodedWidth; int EncodedHeight` - The original resolution of the frame (in pixels), as encoded in the compressed file, before any scaling was applied.
   Note that must not necessarily be the same for all frames in a stream.
 - `int EncodedPixelFormat` - The original pixel format of the frame, as encoded in the compressed file.
 - `int ScaledWidth; int ScaledHeight;` - The output resolution of the frame (in pixels), i.e. the resolution of what is actually stored in the `Data` field. Set to -1 if no scaling is done.
 - `int ConvertedPixelFormat` - The output pixel format of the frame, i.e. the pixel format of what is actually stored in the `Data` field.
 - `int KeyFrame` - Nonzero if the frame is a keyframe, 0 otherwise.
 - `int RepeatPict` - An integer repesenting the RFF flag for this frame; i.e. the frame shall be displayed for `1+RepeatPict` time units, where the time units are expressed in the special RFF timebase available in `FFMS_VideoProperties->RFFDenominator` and `FFMS_VideoProperties->RFFNumerator`.
   Note that if you actually end up using this, you need to ignore the usual timestamps (calculated via the `FFMS_TrackTimeBase` and the frame PTS) since they are fundamentally incompatible with RFF flags.
 - `int InterlacedFrame` - Nonzero if the frame was coded as interlaced, zero otherwise.
 - `int TopFieldFirst` - Nonzero if the frame has the top field first, zero if it has the bottom field first.
   Only relevant if `InterlacedFrame` is nonzero.
 - `char PictType` - A single character denoting coding type (I/B/P etc) of the compressed frame.
   See the Constants and Preprocessor Definitions section for more information about what the different letters mean.
 - `int ColorSpace` - Identifies the YUV color coefficients used in the frame.
   Same as in the MPEG-2 specs; see the [FFMS_ColorSpaces][ColorSpaces] enum.
 - `int ColorRange` - Identifies the luma range of the frame.
   See the [FFMS_ColorRanges][ColorRanges] enum.
 - `int ColorPrimaries;` - Identifies the color primaries of the frame.
 - `int TransferCharateristics;` - Identifies the transfer characteristics of the frame.
 - `int ChromaLocation;` - Identifies the chroma location for the frame. Corresponds to [FFMS_ChromaLocations][ChromaLocations].
 - `int HasMasteringDisplayPrimaries;` - If this is non-zero, the following four properties are set.
 - `double MasteringDisplayPrimariesX[3];` - RGB chromaticy coordinates of the mastering display (x coord).
 - `double MasteringDisplayPrimariesY[3];` - RGB chromaticy coordinates of the mastering display (y coord).
 - `double MasteringDisplayWhitePointX;` - White point coordinate of the mastering display (x coord).
 - `double MasteringDisplayWhitePointY;` - White point coordinate of the mastering display (y coord).
 - `int HasMasteringDisplayLuminance;` - If this is non-zero, the following two properties are set.
 - `double MasteringDisplayMinLuminance;` - Minimum luminance of the mastering display (cd/m^2).
 - `double MasteringDisplayMaxLuminance;` - Maximum luminance of the mastering display (cd/m^2).
 - `int HasContentLightLevel;` - If this is non-zero, the following two properties are set.
 - `unsigned int ContentLightLevelMax;` - Maximum content luminance (cd/m^2).
 - `unsigned int ContentLightLevelAverage;` - Average content luminance (cd/m^2).

### FFMS_TrackTimeBase

[TrackTimeBase]: #ffms_tracktimebase
```c++
typedef struct {
  int64_t Num;
  int64_t Den;
} FFMS_TrackTimeBase;
```
A struct representing the basic time unit of a track, as a rational number where `Num` is the numerator and `Den` is the denominator.
Note that while this rational number may occasionally turn out to be equal to 1/framerate for some CFR video tracks, it really has no relation whatsoever with the video framerate and you should definitely not assume anything framerate-related based on it.

### FFMS_FrameInfo

[FrameInfo]: #ffms_frameinfo
```c++
typedef struct {
    int64_t PTS;
    int RepeatPict;
    int KeyFrame;
} FFMS_FrameInfo;
```
A struct representing basic metadata about a given video frame.
The fields are:
 - `int64_t PTS` - The decoding timestamp of the frame.
   To convert this to a timestamp in wallclock milliseconds, use the relation `int64_t timestamp = (int64_t)((FFMS_FrameInfo->PTS * FFMS_TrackTimeBase->Num) / (double)FFMS_TrackTimeBase->Den)`.
 - `int RepeatPict` - RFF flag for the frame; same as in `FFMS_Frame`, see that structure for an explanation.
 - `int KeyFrame` - Non-zero if the frame is a keyframe, zero otherwise.

### FFMS_VideoProperties

[VideoProperties]: #ffms_videoproperties
```c++
typedef struct {
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
  int ColorSpace; // [DEPRECATED]
  int ColorRange; // [DEPRECATED]
  double FirstTime;
  double LastTime;
  int Rotation;
  int Stereo3DType;
  int Stereo3DFlags;
  double LastEndTime;
  int HasMasteringDisplayPrimaries;
  double MasteringDisplayPrimariesX[3];
  double MasteringDisplayPrimariesY[3];
  double MasteringDisplayWhitePointX;
  double MasteringDisplayWhitePointY;
  int HasMasteringDisplayLuminance;
  double MasteringDisplayMinLuminance;
  double MasteringDisplayMaxLuminance;
  int HasContentLightLevel;
  unsigned int ContentLightLevelMax;
  unsigned int ContentLightLevelAverage;
  int Flip;
} FFMS_VideoProperties;
```
A struct containing metadata about a video track.
The fields are:
 - `int FPSDenominator; int FPSNumerator;` - The nominal framerate of the track, as a rational number.
   For Matroska files, this number is based on the average frame duration of all frames, while for everything else it's based on the duration of the first frame.
   While it might seem tempting to use these values to extrapolate wallclock timestamps for each frame, you really shouldn't do that since it makes your code unable to handle variable framerate properly.
   The ugly reality is that these values are pretty much only useful for informational purposes; they are only somewhat reliable for antiquated containers like AVI.
   Normally they should never be used for practical purposes; generate individual frame timestamps from `FFMS_FrameInfo->PTS` instead.
 - `int RFFDenominator; int RFFNumerator;` - The special RFF timebase, as a rational number.
   See `RepeatPict` in the `FFMS_Frame` documentation for more information.
 - `int NumFrames;` - The number of frames in the video track.
 - `int SARNum; int SARDen;` - The sample aspect ratio of the video frames, as a rational number where `SARNum` is the numerator and `SARDen` is the denominator.
   Note that this is a metadata setting that you are free to ignore, but if you want the proper display aspect ratio with anamorphic material, you should honor it.
   On the other hand, there are situations (like when encoding) where you should probably ignore it because the user expects it to be ignored.
 - `int CropTop; int CropBottom; int CropLeft; int CropRight;` - The number of pixels in each direction you should crop the frame before displaying it.
   Note that like the SAR, this is a metadata setting and you are free to ignore it, but if you want things to display 100% correctly you should honor it.
 - `int TopFieldFirst` - Nonzero if the stream has the top field first, zero if it has the bottom field first.
 - `int ColorSpace` - Identifies the YUV color coefficients used in the stream.
   Same as in the MPEG-2 specs; see the [FFMS_ColorSpaces][ColorSpaces] enum.
   The ColorSpace property in FFMS_Frame should be instead of this, as this can vary between frames unless you've asked for everything to be converted to a single value with `FFMS_SetOutputFormatV2`.
 - `int ColorRange` - Identifies the luma range of the stream.
   See the [FFMS_ColorRanges][ColorRanges] enum.
   The ColorRange property in FFMS_Frame should be instead of this, as this can vary between frames unless you've asked for everything to be converted to a single value with `FFMS_SetOutputFormatV2`.
 - `double FirstTime; double LastTime;` - The first and last timestamp of the stream respectively, in seconds.
   Useful if you want to know if the stream has a delay, or for quickly determining its length in seconds.
 - `int Rotation;` - The rotation of the video, in degrees.
 - `int Stereo3DType;` - The type of stereo 3D the video is. Corresponts to entries in [FFMS_Stereo3DType][Stereo3DType].
 - `int Stereo3DFlags;` - Stereo 3D flags. Corresponds to entries in [FFMS_Stereo3DFlags][Stereo3DFlags].
 - `double LastEndTime;` - The end time of the last packet of the stream, in milliseconds.
 - `int HasMasteringDisplayPrimaries;` - If this is non-zero, the following four properties are set.
 - `double MasteringDisplayPrimariesX[3];` - RGB chromaticy coordinates of the mastering display (x coord).
 - `double MasteringDisplayPrimariesY[3];` - RGB chromaticy coordinates of the mastering display (y coord).
 - `double MasteringDisplayWhitePointX;` - White point coordinate of the mastering display (x coord).
 - `double MasteringDisplayWhitePointY;` - White point coordinate of the mastering display (y coord).
 - `int HasMasteringDisplayLuminance;` - If this is non-zero, the following two properties are set.
 - `double MasteringDisplayMinLuminance;` - Minimum luminance of the mastering display (cd/m^2).
 - `double MasteringDisplayMaxLuminance;` - Maximum luminance of the mastering display (cd/m^2).
 - `int HasContentLightLevel;` - If this is non-zero, the following two properties are set.
 - `unsigned int ContentLightLevelMax;` - Maximum content luminance (cd/m^2).
 - `unsigned int ContentLightLevelAverage;` - Average content luminance (cd/m^2).
 - `int Flip;` - Flip direction to be applied *before* rotation: 0 for no operation, >0 for horizontal flip, <0 for vertical flip.

### FFMS_AudioProperties

[AudioProperties]: #ffms_audioproperties
```c++
typedef struct {
  int SampleFormat;
  int SampleRate;
  int BitsPerSample;
  int Channels;
  int64_t ChannelLayout;
  int64_t NumSamples;
  double FirstTime;
  double LastTime;
  double LastEndTime;
} FFMS_AudioProperties;
```
A struct containing metadata about an audio track.
The fields are:
 - `int SampleFormat` - An integer that represents the audio sample format.
    See [FFMS_SampleFormat][SampleFormat].
 - `int SampleRate` - The audio samplerate, in samples per second.
 - `int BitsPerSample` - The number of bits per audio sample.
   Note that this signifies the number of bits actually used to *code* each sample, not the number of bits used to *store* each sample, and may hence be different from what the `SampleFormat` would imply.
   Figuring out which bytes are significant and which aren't is left as an exercise for the reader.
 - `int Channels` - The number of audio channels.
 - `int64_t ChannelLayout` - The channel layout of the audio stream.
   Constructed by binary OR'ing the relevant integers from `FFMS_AudioChannel` together, which means that if the audio has the channel `FFMS_CH_EXAMPLE`, the operation `(ChannelOrder & FFMS_CH_EXAMPLE)` will evaluate to true.
   The samples are interleaved in the order the channels are listed in the [FFMS_AudioChannel][AudioChannel] enum.
 - `int64_t NumSamples` - The number of samples in the audio track.
 - `double FirstTime; double LastTime;` - The first and last timestamp of the stream respectively, in milliseconds.
   Useful if you want to know if the stream has a delay, or for quickly determining its length in seconds.
 - `double LastEndTime;` - The end time of the last packet of the stream, in milliseconds.

## Constants and Preprocessor Definitions
The following constants and preprocessor definititions defined in ffms.h are suitable for public usage.

### FFMS_Errors

[Errors]: #ffms_errors
```c++
enum FFMS_Errors {
  // No error
  FFMS_ERROR_SUCCESS = 0,

  // Main types - where the error occurred
  FFMS_ERROR_INDEX = 1,           // index file handling
  FFMS_ERROR_INDEXING,            // indexing
  FFMS_ERROR_POSTPROCESSING,      // video postprocessing (libpostproc)
  FFMS_ERROR_SCALING,             // image scaling (libswscale)
  FFMS_ERROR_DECODING,            // audio/video decoding
  FFMS_ERROR_SEEKING,             // seeking
  FFMS_ERROR_PARSER,              // file parsing
  FFMS_ERROR_TRACK,               // track handling
  FFMS_ERROR_WAVE_WRITER,         // WAVE64 file writer
  FFMS_ERROR_CANCELLED,           // operation aborted

  // Subtypes - what caused the error
  FFMS_ERROR_UNKNOWN = 20,        // unknown error
  FFMS_ERROR_UNSUPPORTED,         // format or operation is not supported with this binary
  FFMS_ERROR_FILE_READ,           // cannot read from file
  FFMS_ERROR_FILE_WRITE,          // cannot write to file
  FFMS_ERROR_NO_FILE,             // no such file or directory
  FFMS_ERROR_VERSION,             // wrong version
  FFMS_ERROR_ALLOCATION_FAILED,   // out of memory
  FFMS_ERROR_INVALID_ARGUMENT,    // invalid or nonsensical argument
  FFMS_ERROR_CODEC,               // decoder error
  FFMS_ERROR_NOT_AVAILABLE,       // requested mode or operation unavailable in this binary
  FFMS_ERROR_FILE_MISMATCH,       // provided index does not match the file
  FFMS_ERROR_USER                 // problem exists between keyboard and chair
};
```
Used to identify errors. Should be self-explanatory.

### FFMS_SeekMode

[SeekMode]: #ffms_seekmode
```c++
enum FFMS_SeekMode {
  FFMS_SEEK_LINEAR_NO_RW  = -1,
  FFMS_SEEK_LINEAR        = 0,
  FFMS_SEEK_NORMAL        = 1,
  FFMS_SEEK_UNSAFE        = 2,
  FFMS_SEEK_AGGRESSIVE    = 3
};
```
Used in [FFMS_CreateVideoSource][CreateVideoSource] to control the way seeking is handled.
Explanation of the values:
 - `FFMS_SEEK_LINEAR_NO_RW` - Linear access without rewind; i.e. will throw an error if each successive requested frame number isn't bigger than the last one.
   Only intended for opening images but might work on well with some obscure video format.
 - `FFMS_SEEK_LINEAR` - Linear access (i.e. if you request frame `n` without having requested frames 0 to `n-1` in order first, all frames from 0 to `n` will have to be decoded before `n` can be delivered).
   The definition of slow, but should make some formats "usable".
 - `FFMS_SEEK_NORMAL` - Safe normal.
   Bases seeking decisions on the keyframe positions reported by libavformat.
 - `FFMS_SEEK_UNSAFE` - Unsafe normal.
   Same as `FFMS_SEEK_NORMAL` but no error will be thrown if the exact destination has to be guessed.
 - `FFMS_SEEK_AGGRESSIVE` - Aggressive.
   Seeks in the forward direction even if no closer keyframe is known to exist.
   Only useful for testing and containers where libavformat doesn't report keyframes properly.

### FFMS_IndexErrorHandling

[IndexErrorHandling]: #ffms_indexerrorhandling
```c++
enum FFMS_IndexErrorHandling {
  FFMS_IEH_ABORT = 0,
  FFMS_IEH_CLEAR_TRACK = 1,
  FFMS_IEH_STOP_TRACK = 2,
  FFMS_IEH_IGNORE = 3
};
```
Used by the indexing functions to control behavior when a decoding error is encountered.
 - `FFMS_IEH_ABORT` - abort indexing and raise an error
 - `FFMS_IEH_CLEAR_TRACK` - clear all indexing entries for the track (i.e. return a blank track)
 - `FFMS_IEH_STOP_TRACK` - stop indexing but keep previous indexing entries (i.e. return a track that stops where the error occurred)
 - `FFMS_IEH_IGNORE` - ignore the error and pretend it's raining

### FFMS_TrackType

[TrackType]: #ffms_tracktype
```c++
enum FFMS_TrackType {
  FFMS_TYPE_UNKNOWN = -1,
  FFMS_TYPE_VIDEO,
  FFMS_TYPE_AUDIO,
  FFMS_TYPE_DATA,
  FFMS_TYPE_SUBTITLE,
  FFMS_TYPE_ATTACHMENT
};
```
Used for determining the type of a given track. Note that there are currently no functions to handle any type of track other than `FFMS_TYPE_VIDEO` and `FFMS_TYPE_AUDIO`.
See [FFMS_GetTrackType][GetTrackType], [FFMS_GetFirstTrackOfType][GetFirstTrackOfType] and their variants.

### FFMS_SampleFormat

[SampleFormat]: #ffms_sampleformat
```c++
enum FFMS_SampleFormat {
  FFMS_FMT_U8 = 0,
  FFMS_FMT_S16,
  FFMS_FMT_S32,
  FFMS_FMT_FLT,
  FFMS_FMT_DBL
};
```
Identifies various audio sample formats.

 - `FFMS_FMT_U8` - One 8-bit unsigned integer (`uint8_t`) per sample.
 - `FFMS_FMT_S16` - One 16-bit signed integer (`int16_t`) per sample.
 - `FFMS_FMT_S32` - One 32-bit signed integer (`int32_t`) per sample.
 - `FFMS_FMT_FLT` - One 32-bit (single precision) floating point value (`float_t`) per sample.
 - `FFMS_FMT_DBL` - One 64-bit (double precision) floating point value (`double_t`) per sample.

### FFMS_AudioChannel

[AudioChannel]: #ffms_audiochannel
```c++
enum FFMS_AudioChannel {
  FFMS_CH_FRONT_LEFT              = 0x00000001,
  FFMS_CH_FRONT_RIGHT             = 0x00000002,
  FFMS_CH_FRONT_CENTER            = 0x00000004,
  FFMS_CH_LOW_FREQUENCY           = 0x00000008,
  FFMS_CH_BACK_LEFT               = 0x00000010,
  FFMS_CH_BACK_RIGHT              = 0x00000020,
  FFMS_CH_FRONT_LEFT_OF_CENTER    = 0x00000040,
  FFMS_CH_FRONT_RIGHT_OF_CENTER   = 0x00000080,
  FFMS_CH_BACK_CENTER             = 0x00000100,
  FFMS_CH_SIDE_LEFT               = 0x00000200,
  FFMS_CH_SIDE_RIGHT              = 0x00000400,
  FFMS_CH_TOP_CENTER              = 0x00000800,
  FFMS_CH_TOP_FRONT_LEFT          = 0x00001000,
  FFMS_CH_TOP_FRONT_CENTER        = 0x00002000,
  FFMS_CH_TOP_FRONT_RIGHT         = 0x00004000,
  FFMS_CH_TOP_BACK_LEFT           = 0x00008000,
  FFMS_CH_TOP_BACK_CENTER         = 0x00010000,
  FFMS_CH_TOP_BACK_RIGHT          = 0x00020000,
  FFMS_CH_STEREO_LEFT             = 0x20000000,
  FFMS_CH_STEREO_RIGHT            = 0x40000000
};
```
Describes the audio channel layout of an audio stream.
The names should be self-explanatory.

As you might have noticed, these constants are the same as the ones used for the `dwChannelMask` property of Microsoft's `WAVEFORMATEXTENSIBLE` struct; see [its MSDN documentation page](http://msdn.microsoft.com/en-us/library/dd757714%28v=vs.85%29.aspx) for more information.
The exceptions to this convenient compatibility are `FFMS_CH_STEREO_LEFT` and `FFMS_CH_STEREO_RIGHT`, which are FFmpeg extensions.

### FFMS_Resizers

[Resizers]: #ffms_resizers
```c++
enum FFMS_Resizers {
  FFMS_RESIZER_FAST_BILINEAR  = 0x01,
  FFMS_RESIZER_BILINEAR       = 0x02,
  FFMS_RESIZER_BICUBIC        = 0x04,
  FFMS_RESIZER_X              = 0x08,
  FFMS_RESIZER_POINT          = 0x10,
  FFMS_RESIZER_AREA           = 0x20,
  FFMS_RESIZER_BICUBLIN       = 0x40,
  FFMS_RESIZER_GAUSS          = 0x80,
  FFMS_RESIZER_SINC           = 0x100,
  FFMS_RESIZER_LANCZOS        = 0x200,
  FFMS_RESIZER_SPLINE         = 0x400
};
```
Describes various image resizing algorithms, as used in the arguments to [FFMS_SetOutputFormatV2][SetOutputFormatV2].
The names should be self-explanatory.

### FFMS_AudioDelayModes

[AudioDelayModes]: #ffms_audiodelaymodes
```c++
enum FFMS_AudioDelayModes {
  FFMS_DELAY_NO_SHIFT           = -3,
  FFMS_DELAY_TIME_ZERO          = -2,
  FFMS_DELAY_FIRST_VIDEO_TRACK  = -1
};
```
Describes the different audio delay handling modes.
See [FFMS_CreateAudioSource][CreateAudioSource] for a detailed explanation.

### FFMS_ChromaLocations

[ChromaLocations]: #ffms_chromalocations
```c++
typedef enum FFMS_ChromaLocations {
    FFMS_LOC_UNSPECIFIED = 0,
    FFMS_LOC_LEFT = 1,
    FFMS_LOC_CENTER = 2,
    FFMS_LOC_TOPLEFT = 3,
    FFMS_LOC_TOP = 4,
    FFMS_LOC_BOTTOMLEFT = 5,
    FFMS_LOC_BOTTOM = 6
} FFMS_ChromaLocations;
```
Identifies the location of chroma samples in a frame.

### FFMS_ColorRanges

[ColorRanges]: #ffms_colorranges
```c++
enum FFMS_ColorRanges {
  FFMS_CR_UNSPECIFIED = 0,
  FFMS_CR_MPEG        = 1,
  FFMS_CR_JPEG        = 2,
};
```
Identifies the valid range of luma values in a YUV stream.
`FFMS_CR_MPEG` is the standard "TV range" with head- and footroom.
That is, valid luma values range from 16 to 235 with 8-bit color.
`FFMS_CR_JPEG` is "full range"; all representable luma values are valid.

### FFMS_Stereo3DType
[Stereo3DType]: #ffms_stereo3dtype
```c++
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
```
Identifies the type of stereo 3D the video is.

### FFMS_Stereo3DFlags
[Stereo3DFlags]: #ffms_stereo3dflags
```c++
typedef enum FFMS_Stereo3DFlags {
    FFMS_S3D_FLAGS_INVERT = 1
} FFMS_Stereo3DFlags;
```
Various flags for stereo 3D videos.

### FFMS_CC
```c++
#ifdef _WIN32
#   define FFMS_CC __stdcall
#else
#   define FFMS_CC
#endif
```
The calling convention used by FFMS2 API functions and callbacks.
Defined to `__stdcall` if `_WIN32` is defined. Otherwise defined, but not used.

### Picture types
As stored in `FFMS_Frame->PictType`:
```
I: Intra
P: Predicted
B: Bi-dir predicted
S: S(GMC)-VOP MPEG4
i: Switching Intra
p: Switching Predicted
b: FF_BI_TYPE (no good explanation available)
?: Unknown
```

## Audio Filename Format Strings
The following variables can be used:

 - `%sourcefile%` - same as the source file name, i.e. the file the audio is decoded from
 - `%trackn%` - the track number
 - `%trackzn%` - the track number zero padded to 2 digits
 - `%samplerate%` - the audio sample rate
 - `%channels%` - number of audio channels
 - `%bps%` - bits per sample
 - `%delay%` - delay, or more exactly the first timestamp encountered in the audio stream

Example string: `%sourcefile%_track%trackzn%.w64`
