# FFmpegSource2 VapourSynth User Manual

Opens files using FFmpeg and (almost) nothing else.
May be frame accurate on good days.
The source is MIT licensed and can be obtained from https://github.com/FFMS/ffms2/.
The precompiled binary is GPL3 licensed.
If you are religious you may consider this the second coming.

## Donate

Donate if you like this software.
Collecting weird clips from the internet and making them play takes more time than you'd think.

[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=fredrik%2emellbin%40gmail%2ecom&lc=US&item_name=IVTC%2eORG&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHostedGuest)

## Limitations
 - No audio support in VapourSynth
 - Because of LAVF's demuxer, most raw streams (such as elementary h264 and other mpeg video streams) will fail to work properly.
 - Interlaced H.264 mostly works these days, but seeking may occasionally result in corruption.
 - Transport Streams will not decode reliably without seekmode -1.
 - Open-GOP H.264 will sometimes produces corruption when seeking.

## Compatibility

 - AVI, MKV, MP4, FLV: Frame accurate
 - WMV: Frame accurate(?) but avformat seems to pick keyframes relatively far away
 - OGM: Frame accurate(?)
 - VOB, MPG: Seeking seems to be off by one or two frames now and then
 - M2TS, TS: Seeking seems to be off a few frames here and there
 - Image files: Most formats can be opened if seekmode=-1 is set, no animation support

## Indexing and You

Before FFMS2 can open a file, it must be indexed first so that keyframe/sample positions are known and seeking is easily accomplished.
This is done automatically when using `Source()`,
but if you want to you can invoke the indexing yourself by calling `Index()`, or by running `ffmsindex`.
By default the index is written to a file so it can be reused the next time you open the same file, but this behavior can be turned off if desired.

If you wonder why FFMS2 takes so long opening files, the indexing is the answer.
If you want a progress report on the indexing, you can use the supplied `ffmsindex` commandline program.

## Function reference

### Index
```
ffms2.Index(string source[, string cachefile = source + ".ffindex", int[] indextracks = [],
    int errorhandling = 3, bint overwrite = False])
```
Indexes a number of tracks in a given source file and writes the index file to disk, where it can be picked up and used by `Source`.
Normally you do not need to call this function manually; it's invoked automatically if necessary by `Source`.
It does, however, give you more control over how indexing is done.

Note that this function returns an integer, not a clip (since it doesn't open video, nor audio).
The return value isn't particularly interesting, but for the record it's 0 if the index file already exists (and is valid) and overwrite was not enabled, 1 if the index file was created and no previous index existed, and 2 if the index file was created by overwriting an existing, valid index file.

#### Arguments

##### string source
The source file to index.

##### string cachefile = source + ".ffindex"
The filename of the index file (where the indexing data is saved).
Defaults to `sourcefilename.ffindex`.

##### int[] indextracks = []
An array containing a list of which audio tracks should be indexed (all video tracks are always indexed; you have no choice in the matter).
It's possible to use -1 to simply tell it to index all tracks.´without having to list them individually.

Note that FFMS2's idea about what track has what number may be completely different from what any other application might think.

##### int errorhandling = 3
Controls what happens if an audio decoding error is encountered during indexing.
Possible values are:

 - **0**: Raise an error and abort indexing. No index file is written.
 - **1**: Clear the affected track (effectively making it silent) and continue.
 - **2**: Stop indexing the track but keep all the index entries so far, effectively ending the track where the error occured.
 - **3**: Pretend it's raining and continue anyway. This is the default; if you encounter odd noises in the audio, try mode 0 instead and see if it's FFMS2's fault.

##### bint overwrite = False
If set to true, `FFIndex()` will reindex the source file and overwrite the index file even if the index file already exists and is valid.
Mostly useful for trackmask changes and testing.

### Source
```
ffms2.Source(string source[, int track = -1, bint cache = True,
    string cachefile = source + ".ffindex", int fpsnum = -1, int fpsden = 1,
    int threads = -1, string timecodes = "", int seekmode = 1,
    int width = -1, int height = -1, string resizer = "BICUBIC",
    int format, bint alpha = False])
```
Opens video. Will invoke indexing of all video tracks (but no audio tracks) if no valid index file is found.

#### Arguments

##### string source
The source file to open.

##### int track = -1
The video track number to open, as seen by the relevant demuxer.
Track numbers start from zero, and are guaranteed to be continous (i.e. there must be a track 1 if there is a track 0 and a track 2). -1 means open the first video track.
Note that FFMS2's idea about what track has what number may (or may not) be completely different from what some other application might think.

##### bint cache = True
If set to true (the default), `Source` will first check if the `cachefile` contains a valid index, and if it does, that index will be used.
If no index is found, all video tracks will be indexed, and the indexing data will be written to `cachefile` afterwards.
If set to false, `Source` will not look for an existing index file; instead all video tracks will be indexed when the script is opened, and the indexing data will be discarded after the script is closed; you will have to index again next time you open the script.

##### string cachefile = source + ".ffindex"
The filename of the index file (where the indexing data is saved).
Defaults to `sourcefilename.ffindex`.
Note that if you didn't change this parameter from its default value and `Source` encounters an index file that doesn't seem to match the file it's trying to open, it will automatically reindex and then overwrite the old index file.
On the other hand, if you *do* change it, `Source` will assume you have your reasons and throw an error instead if the index doesn't match the file.

##### int fpsnum = -1, int fpsden = 1
Controls the framerate of the output; used for VFR to CFR conversions.
If `fpsnum` is less than or equal to zero (the default), the output will contain the same frames that the input did, and the frame rate reported to VapourSynth will be set based on the input clip's average frame duration.
If `fpsnum` is greater than zero, `Source` will force a constant frame rate, expressed as a rational number where `fpsnum` is the numerator and `fpsden` is the denominator.
This may naturally cause `Source` to drop or duplicate frames to achieve the desired frame rate, and the output is not guaranteed to have the same number of frames that the input did.

##### int threads = -1
The number of decoding threads to request from libavcodec.
Setting it to less than or equal to zero means it defaults to the number of logical CPU's reported by the OS.
Note that this setting might be completely ignored by libavcodec under a number of conditions; most commonly because a lot of decoders actually do not support multithreading.

##### string timecodes = ""
Filename to write Matroska v2 timecodes for the opened video track to.
If the file exists, it will be truncated and overwritten.
Set to the empty string to disable timecodes writing (this is the default).

##### int seekmode = 1
Controls how seeking is done.
Mostly useful for getting uncooperative files to work.
Valid modes are:

 - **-1**: Linear access without rewind; i.e. will throw an error if each successive requested frame number isn't bigger than the last one.
   Only intended for opening images but might work on well with some obscure video format.
 - **0**: Linear access (i.e. if you request frame `n` without having requested all frames from 0 to `n-1` in order first, all frames from 0 to `n` will have to be decoded before `n` can be delivered).
   The definition of slow, but should make some formats "usable".
 - **1**: Safe normal. Bases seeking decisions on the keyframe positions reported by libavformat.
 - **2**: Unsafe normal. Same as mode 1, but no error will be thrown if the exact seek destination has to be guessed.
 - **3**: Aggressive. Seeks in the forward direction even if no closer keyframe is known to exist. Only useful for testing and containers where libavformat doesn't report keyframes properly.

##### int width = -1, int height = -1
Sets the resolution of the output video, in pixels.
Setting either dimension to less than or equal to zero means the resolution of the first decoded video frame is used for that dimension.
These parameters are mostly useful because FFMS2 supports video streams that change resolution mid-stream which would otherwise have to be handled with a more complicated script.

##### string resizer = "BICUBIC"
The resizing algorithm to use if rescaling the image is necessary.
If the video uses subsampled chroma but your chosen output colorspace does not, the chosen resizer will be used to upscale the chroma planes, even if you did not request an image rescaling.
The available choices are `FAST_BILINEAR`, `BILINEAR`, `BICUBIC` (default), `X`, `POINT`, `AREA`, `BICUBLIN`, `GAUSS`, `SINC`, `LANCZOS` and `SPLINE`.

##### int format
Convert the output from whatever it was to the given format. If not specified the best matching output format is used.

##### bint alpha = False
Output the alpha channel as a second clip if it is present in the file. When set to True an array of two clips will be returned with alpha in the second one. If there is alpha information present.

#### Exported VapourSynth frame properties
There are several useful frame properties that are set. See the VapourSynth manual for a detailed explanation of them.

 - _DurationNum
 - _DurationDen
 - _AbsoluteTime
 - _SARNum
 - _SARDen
 - _Matrix
 - _Primaries
 - _Transfer
 - _ChromaLocation
 - _ColorRange
 - _PictType
 - _FieldBased

### SetLogLevel
```
ffms2.SetLogLevel(int Level = -8)
```
Sets the FFmpeg logging level, i.e. how much diagnostic spam it prints to STDERR.
Since many applications that open VapourSynth scripts do not provide a way to display things printed to STDERR, and since it's rather hard to make any sense of the printed messages unless you're quite familiar with FFmpeg internals, the usefulness of this function is rather limited for end users. It's mostly intended for debugging.
Defaults to quiet (no messages printed); a list of meaningful values can be found in `ffms.h`.

### GetLogLevel
```
ffms2.GetLogLevel()
```
Returns the current log level, as an integer.

### GetVersion
```
ffms2.GetVersion()
```
Returns the FFMS2 version, as a string.

