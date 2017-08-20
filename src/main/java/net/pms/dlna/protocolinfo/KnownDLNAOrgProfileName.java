package net.pms.dlna.protocolinfo;


/**
 * This {@code enum} contains predefined {@code DLNA.ORG_PN} values that
 * isn't already defined in {@link DLNAProfiles}.
 *
 * @author Nadahar
 */
public enum KnownDLNAOrgProfileName implements DLNAOrgProfileName {
	// XXX Add profiles, there are lots missing

	/**
	 * AVC wrapped in MP4 baseline profile CIF30 with AAC LC audio.
	 */
	AVC_MP4_BL_CIF30_AAC_940,

	/**
	 * AVC HD/SD video with AC-3 audio including dual-mono channel mode, wrapped
	 * in MPEG-2 TS with valid timestamp for 24 Hz system.
	 */
	AVC_TS_HD_24_AC3_X_T,

	/**
	 * AVC HD/SD video with LPCM audio, wrapped in MPEG-2 TS with valid
	 * timestamp for 24 Hz system.
	 */
	AVC_TS_HD_24_LPCM_T,

	/**
	 * AVC HD/SD video with AC-3 audio including dual-mono channel mode, wrapped
	 * in MPEG-2 TS with valid timestamp for 50 Hz system.
	 */
	AVC_TS_HD_50_AC3_X_T,

	/**
	 * AVC HD/SD video with AC-3 audio including dual-mono channel mode,
	 * wrapped in MPEG-2 TS with valid timestamp for 60 Hz system.
	 */
	AVC_TS_HD_60_AC3_X_T,

	/**
	 * AVC HD/SD video with LPCM audio, wrapped in MPEG-2 TS with valid
	 * timestamp for 60 Hz system.
	 */
	AVC_TS_HD_60_LPCM_T,

	/**
	 * AVC wrapped in MPEG-2 transport stream, Main/High profile, with
	 * MPEG-2 AAC audio, with a valid non-zero timestamp field.
	 */
	AVC_TS_JP_AAC_T,

	/**
	 * AVC video wrapped in MPEG-2 transport stream, as constrained by SCTE
	 * standards, with AC-3, Enhanced AC-3, MPEG-4 HE-AAC v2 or MPEG-1 Layer II
	 * audio, without a timestamp field.
	 */
	AVC_TS_NA_ISO,

	/**
	 * AVC video wrapped in MPEG-2 transport stream, as constrained by SCTE
	 * standards, with AC-3, Enhanced AC-3, MPEG-4 HE-AAC v2 or MPEG-1 Layer II
	 * audio, with a valid non-zero timestamp field.
	 */
	AVC_TS_NA_T,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 24 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, with a zero value
	 * timestamp field.
	 */
	DTCP_AVC_TS_HD_24_AC3,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 24 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, without a timestamp
	 * field.
	 */
	DTCP_AVC_TS_HD_24_AC3_ISO,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 24 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, with a valid non-zero
	 * timestamp field.
	 */
	DTCP_AVC_TS_HD_24_AC3_T,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 50 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, with a zero value
	 * timestamp field.
	 */
	DTCP_AVC_TS_HD_50_AC3,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 50 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, without a timestamp
	 * field.
	 */
	DTCP_AVC_TS_HD_50_AC3_ISO,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 50 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, with a valid non-zero
	 * timestamp field.
	 */
	DTCP_AVC_TS_HD_50_AC3_T,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 60 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, with a zero value
	 * timestamp field.
	 */
	DTCP_AVC_TS_HD_60_AC3,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 60 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, without a timestamp
	 * field.
	 */
	DTCP_AVC_TS_HD_60_AC3_ISO,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video for a 60 Hz system, wrapped
	 * in MPEG-2 transport stream, with AC-3 audio, with a valid non-zero
	 * timestamp field.
	 */
	DTCP_AVC_TS_HD_60_AC3_T,

	/**
	 * DTCP-IP Link Protection, AVC HD/SD video with AC-3 audio including
	 * dual-mono channel mode, wrapped in MPEG-2 TS with valid timestamp for
	 * 60 Hz system.
	 */
	DTCP_AVC_TS_HD_60_AC3_X_T,

	/**
	 * DTCP-IP Link Protection, AVC wrapped in MPEG-2 transport stream,
	 * Main/High profile, with MPEG-2 AAC audio, with a valid non-zero
	 * timestamp field.
	 */
	DTCP_AVC_TS_JP_AAC_T,

	/**
	 * DTCP-IP Link Protection, AVC wrapped in MPEG-2 transport stream main
	 * profile HD with AAC LTP audio with valid timestamp field.
	 */
	DTCP_AVC_TS_MP_HD_AAC_LTP_T,

	/** DTCP-IP Link Protection, Profile for AV class media. */
	DTCP_MPEG1,

	/**
	 * DTCP-IP Link Protection, Profile defining ES encapsulation for transport
	 * of MPEG_PS_PAL over RTP.
	 */
	DTCP_MPEG_ES_PAL,

	/**
	 * DTCP-IP Link Protection, Profile defining ES encapsulation for transport
	 * of MPEG_PS_NTSC over RTP.
	 */
	DTCP_MPEG_ES_NTSC,

	/**
	 * DTCP-IP Link Protection, Profile defining ES encapsulation for transport
	 * of MPEG_PS_PAL_XAC3 over RTP.
	 */
	DTCP_MPEG_ES_PAL_XAC3,

	/**
	 * DTCP-IP Link Protection, Profile defining ES encapsulation for transport
	 * of MPEG_PS_NTSC_ XAC3 over RTP.
	 */
	DTCP_MPEG_ES_NTSC_XAC3,

	/** DTCP-IP Link Protection, Profile for NTSC-formatted AV class media. */
	DTCP_MPEG_PS_NTSC,

	/** DTCP-IP Link Protection, Profile for NTSC-formatted AV class media. */
	DTCP_MPEG_PS_NTSC_XAC3,

	/** DTCP-IP Link Protection, Profile for PAL-formatted AV class media. */
	DTCP_MPEG_PS_PAL,

	/** DTCP-IP Link Protection, Profile for PAL-formatted AV class media. */
	DTCP_MPEG_PS_PAL_XAC3,

	/**
	 * DTCP-IP Link Protection, Korea region profile for High Definition AV
	 * class utilizing a DLNA Transport Packet without a Timestamp field.
	 */
	DTCP_MPEG_TS_HD_KO,

	/**
	 * DTCP-IP Link Protection, Korea region profile for High Definition AV
	 * class utilizing a DLNA Transport Packet without a Timestamp field.
	 */
	DTCP_MPEG_TS_HD_KO_ISO,

	/**
	 * DTCP-IP Link Protection, Korea region profile for High Definition AV
	 * class utilizing a DLNA Transport Packet with a valid non-zero timestamp.
	 */
	DTCP_MPEG_TS_HD_KO_T,

	/**
	 * DTCP-IP Link Protection, Korea region profile for transcoded High
	 * Definition AV class media with a zero value timestamp.
	 */
	DTCP_MPEG_TS_HD_KO_XAC3,

	/**
	 * DTCP-IP Link Protection, Korea region profile for transcoded High Definition AV class media with a
	 * valid non-zero timestamp.
	 */
	DTCP_MPEG_TS_HD_KO_XAC3_T,

	/**
	 * DTCP-IP Link Protection, Korea region profile for transcoded High
	 * Definition AV class media without a Timestamp field.
	 */
	DTCP_MPEG_TS_HD_KO_XAC3_ISO,

	/**
	 * DTCP-IP Link Protection, North America region profile for transcoded High
	 * Definition AV class media with a zero value timestamp.
	 */
	DTCP_MPEG_TS_HD_NA_XAC3,

	/**
	 * DTCP-IP Link Protection, North America region profile for transcoded High
	 * Definition AV class media with a valid non-zero timestamp.
	 */
	DTCP_MPEG_TS_HD_NA_XAC3_T,

	/**
	 * DTCP-IP Link Protection, North America region profile for transcoded High
	 * Definition AV class media without a Timestamp field.
	 */
	DTCP_MPEG_TS_HD_NA_XAC3_ISO,

	/**
	 * DTCP-IP Link Protection, North America region profile for High
	 * Definition AV class utilizing a DLNA Transport Packet with zero value
	 * timestamp.
	 */
	DTCP_MPEG_TS_HD_NA,

	/**
	 * DTCP-IP Link Protection, North America region profile for High
	 * Definition AV class utilizing a DLNA Transport Packet without a
	 * Timestamp field.
	 */
	DTCP_MPEG_TS_HD_NA_ISO,

	/**
	 * DTCP-IP Link Protection, North America region profile for High
	 * Definition AV class utilizing a DLNA Transport Packet with a valid
	 * non-zero timestamp.
	 */
	DTCP_MPEG_TS_HD_NA_T,

	/**
	 * DTCP-IP Link Protection, MPEG-2 Main Profile at Main, High-1 440 and
	 * High Level with MPEG-2 AAC encapsulated in MPEG-2 TS with valid
	 * timestamp.
	 */
	DTCP_MPEG_TS_JP_T,

	/**
	 * DTCP-IP Link Protection, MPEG-2 Main Profile at Low Level with AAC LC
	 * audio encapsulated in MPEG-2 transport stream with zero value timestamp.
	 */
	DTCP_MPEG_TS_MP_LL_AAC,

	/**
	 * DTCP-IP Link Protection, MPEG-2 Main Profile at Low Level with AAC LC
	 * audio encapsulated in MPEG-2 transport stream with valid timestamp.
	 */
	DTCP_MPEG_TS_MP_LL_AAC_T,

	/**
	 * DTCP-IP Link Protection, MPEG-2 Main Profile at Low Level with AAC LC
	 * audio encapsulated in MPEG-2 transport stream without a Timestamp field.
	 */
	DTCP_MPEG_TS_MP_LL_AAC_ISO,

	/**
	 * DTCP-IP Link Protection, MPEG-2 Main Profile at Main Level with AC-3
	 * encapsulated in MPEG-2 TS with valid timestamp for 525/60 system.
	 */
	DTCP_MPEG_TS_SD_60_AC3_T,

	/**
	 * DTCP-IP Link Protection, European region profile for Standard
	 * Definition AV class utilizing a DLNA Transport Packet with zero value
	 * timestamp.
	 */
	DTCP_MPEG_TS_SD_EU,

	/**
	 * DTCP-IP Link Protection, MPEG-2 Video, wrapped in MPEG-2 transport
	 * stream, Main Profile, Standard Definition, with AC-3 audio, without a
	 * timestamp field.
	 */
	DTCP_MPEG_TS_SD_EU_AC3_ISO,

	/**
	 * DTCP-IP Link Protection, MPEG-2 Video, wrapped in MPEG-2 transport
	 * stream, Main Profile Standard Definition, with AC-3 audio, with a
	 * valid non-zero timestamp field.
	 */
	DTCP_MPEG_TS_SD_EU_AC3_T,

	/**
	 * DTCP-IP Link Protection, European region profile for Standard
	 * Definition AV class utilizing a DLNA Transport Packet without a
	 * Timestamp field.
	 */
	DTCP_MPEG_TS_SD_EU_ISO,

	/**
	 * DTCP-IP Link Protection, European region profile for Standard
	 * Definition AV class utilizing a DLNA Transport Packet with a valid
	 * non-zero timestamp.
	 */
	DTCP_MPEG_TS_SD_EU_T,

	/**
	 * DTCP-IP Link Protection, MPEG-2 Video, encapsulated in MPEG-2
	 * transport stream, Main Profile at Main Level, with MPEG-1 L2 audio,
	 * with a valid non-zero timestamp field.
	 */
	DTCP_MPEG_TS_SD_JP_MPEG1_L2_T,

	/**
	 * DTCP-IP Link Protection, Korea region profile for Standard Definition AV
	 * utilizing a DLNA Transport Packet with zero value timestamp.
	 */
	DTCP_MPEG_TS_SD_KO,

	/**
	 * DTCP-IP Link Protection, Korea region profile for Standard Definition AV
	 * class utilizing a DLNA Transport Packet without a Timestamp field.
	 */
	DTCP_MPEG_TS_SD_KO_ISO,

	/**
	 * DTCP-IP Link Protection, Korea region profile for Standard Definition AV
	 * class utilizing a DLNA Transport Packet with a valid non-zero timestamp.
	 */
	DTCP_MPEG_TS_SD_KO_T,

	/**
	 * DTCP-IP Link Protection, Korea region profile for Standard Definition AV
	 * class media with a zero value timestamp.
	 */
	DTCP_MPEG_TS_SD_KO_XAC3,

	/**
	 * DTCP-IP Link Protection, Korea region profile for Standard Definition AV
	 * class media with a valid non-zero timestamp.
	 */
	DTCP_MPEG_TS_SD_KO_XAC3_T,

	/**
	 * DTCP-IP Link Protection, Korea region profile for Standard Definition AV
	 * class media without a Timestamp field.
	 */
	DTCP_MPEG_TS_SD_KO_XAC3_ISO,

	/**
	 * DTCP-IP Link Protection, North America region profile for Standard
	 * Definition AV class utilizing a DLNA Transport Packet with zero value
	 * timestamp.
	 */
	DTCP_MPEG_TS_SD_NA,

	/**
	 * DTCP-IP Link Protection, North America region profile for Standard
	 * Definition AV class utilizing a DLNA Transport Packet without a
	 * Timestamp field.
	 */
	DTCP_MPEG_TS_SD_NA_ISO,

	/**
	 * DTCP-IP Link Protection, North America region profile for Standard
	 * Definition AV class utilizing a DLNA Transport Packet with a valid
	 * non-zero timestamp.
	 */
	DTCP_MPEG_TS_SD_NA_T,

	/**
	 * DTCP-IP Link Protection, North America region profile for Standard
	 * Definition AV class media with a zero value timestamp.
	 */
	DTCP_MPEG_TS_SD_NA_XAC3,

	/**
	 * DTCP-IP Link Protection, North America region profile for Standard
	 * Definition AV class media with a valid non-zero timestamp.
	 */
	DTCP_MPEG_TS_SD_NA_XAC3_T,

	/**
	 * DTCP-IP Link Protection, North America region profile for Standard
	 * Definition AV class media without a Timestamp field.
	 */
	DTCP_MPEG_TS_SD_NA_XAC3_ISO,

	/**
	 * DTCP-IP Link Protection, High resolution video (Main Profile at High
	 * Level) with full WMA audio.
	 */
	DTCP_WMVHIGH_FULL,

	/**
	 * DTCP-IP Link Protection, High resolution video (Main Profile at High
	 * Level) with WMA professional audio.
	 */
	DTCP_WMVHIGH_PRO,

	/**
	 * DTCP-IP Link Protection, Medium resolution video (Main Profile at Medium
	 * Level) with baseline WMA audio.
	 */
	DTCP_WMVMED_BASE,

	/**
	 * DTCP-IP Link Protection, Medium resolution video (Main Profile at Medium
	 * Level) with full WMA audio.
	 */
	DTCP_WMVMED_FULL,

	/**
	 * DTCP-IP Link Protection, Medium resolution video (Main Profile at Medium
	 * Level) with WMA professional audio.
	 */
	DTCP_WMVMED_PRO,

	/** Profile for image media class content of high resolution. */
	GIF_LRG,

	/** Profile for image media class content of high resolution. */
	JPEG_LRG,

	/** Profile for large icons */
	JPEG_LRG_ICO,

	/** Profile for image media class content of medium resolution. */
	JPEG_MED,

	/**
	 * Profile for image media class content. Values &lt;H&gt; and &lt;V&gt;
	 * indicate the horizontal and vertical resolutions in pixel numbers.
	 */
	JPEG_RES_H_V,

	/** Profile for image media class content of small resolution. */
	JPEG_SM,

	/** Profile for small icons */
	JPEG_SM_ICO,

	/** Profile for image thumbnails. */
	JPEG_TN,

	/**
	 * Profile defining ES encapsulation for transport of MPEG_PS_PAL over RTP.
	 */
	MPEG_ES_PAL,

	/**
	 * Profile defining ES encapsulation for transport of MPEG_PS_PAL_XAC3 over
	 * RTP.
	 */
	MPEG_ES_PAL_XAC3,

	/**
	 * Profile defining ES encapsulation for transport of MPEG_PS_NTSC over RTP.
	 */
	MPEG_ES_NTSC,

	/**
	 * Profile defining ES encapsulation for transport of MPEG_PS_NTSC_ XAC3
	 * over RTP.
	 */
	MPEG_ES_NTSC_XAC3,

	/**
	 * Korea region profile for High Definition AV class utilizing a DLNA
	 * Transport Packet with zero value timestamp.
	 */
	MPEG_TS_HD_KO,

	/**
	 * Korea region profile for High Definition AV class utilizing a DLNA
	 * Transport Packet without a Timestamp field.
	 */
	MPEG_TS_HD_KO_ISO,

	/**
	 * Korea region profile for High Definition AV class utilizing a DLNA
	 * Transport Packet with a valid non-zero timestamp.
	 */
	MPEG_TS_HD_KO_T,

	/**
	 * Korea region profile for transcoded High Definition AV class media with a
	 * zero value timestamp.
	 */
	MPEG_TS_HD_KO_XAC3,

	/**
	 * Korea region profile for transcoded High Definition AV class media with a
	 * valid non-zero timestamp.
	 */
	MPEG_TS_HD_KO_XAC3_T,

	/**
	 * Korea region profile for transcoded High Definition AV class media
	 * without a Timestamp field.
	 */
	MPEG_TS_HD_KO_XAC3_ISO,

	/**
	 * North America region profile for transcoded High Definition AV class
	 * media with a zero value timestamp.
	 */
	MPEG_TS_HD_NA_XAC3,

	/**
	 * North America region profile for transcoded High Definition AV class
	 * media with a valid non-zero timestamp.
	 */
	MPEG_TS_HD_NA_XAC3_T,

	/**
	 * North America region profile for transcoded High Definition AV class
	 * media without a Timestamp field.
	 */
	MPEG_TS_HD_NA_XAC3_ISO,

	/**
	 * MPEG-2 Main Profile at Low Level with AAC LC audio encapsulated in MPEG-2
	 * transport stream with zero value timestamp.
	 */
	MPEG_TS_MP_LL_AAC,

	/**
	 * MPEG-2 Main Profile at Low Level with AAC LC audio encapsulated in MPEG-2
	 * transport stream with valid timestamp.
	 */
	MPEG_TS_MP_LL_AAC_T,

	/**
	 * MPEG-2 Main Profile at Low Level with AAC LC audio encapsulated in MPEG-2
	 * transport stream without a Timestamp field.
	 */
	MPEG_TS_MP_LL_AAC_ISO,

	/**
	 * Korea region profile for Standard Definition AV class media with a zero
	 * value timestamp.
	 */
	MPEG_TS_SD_KO_XAC3,

	/**
	 * Korea region profile for Standard Definition AV class media with a valid
	 * non-zero timestamp.
	 */
	MPEG_TS_SD_KO_XAC3_T,

	/**
	 * Korea region profile for Standard Definition AV class media without a
	 * Timestamp field.
	 */
	MPEG_TS_SD_KO_XAC3_ISO,

	/**
	 * North America region profile for Standard Definition AV class media with
	 * a zero value timestamp.
	 */
	MPEG_TS_SD_NA_XAC3,

	/**
	 * North America region profile for Standard Definition AV class media with
	 * a valid non-zero timestamp.
	 */
	MPEG_TS_SD_NA_XAC3_T,

	/**
	 * North America region profile for Standard Definition AV class media
	 * without a Timestamp field.
	 */
	MPEG_TS_SD_NA_XAC3_ISO,

	/**
	 * MPEG-2 Main Profile at Main, High-1 440 and High Level with MPEG-2
	 * AAC encapsulated in MPEG-2 TS with valid timestamp.
	 */
	MPEG_TS_JP_T,

	/**
	 * MPEG-2 HD/SD video wrapped in MPEG-2 transport stream as constrained by
	 * SCTE-43 standards, with AC-3 audio, without a timestamp field.
	 */
	MPEG_TS_NA_ISO,

	/**
	 * MPEG-2 Video, wrapped in MPEG-2 transport stream, Main Profile,
	 * Standard Definition, with AC-3 audio, without a timestamp field.
	 */
	MPEG_TS_SD_EU_AC3_ISO,

	/**
	 * MPEG-2 Video, wrapped in MPEG-2 transport stream, Main Profile
	 * Standard Definition, with AC-3 audio, with a valid non-zero timestamp
	 * field.
	 */
	MPEG_TS_SD_EU_AC3_T,

	/**
	 * MPEG-2 Video, encapsulated in MPEG-2 transport stream, Main Profile
	 * at Main Level, with MPEG-1 L2 audio, with a valid non-zero timestamp
	 * field.
	 */
	MPEG_TS_SD_JP_MPEG1_L2_T,

	/**
	 * Korea region profile for Standard Definition AV utilizing a DLNA
	 * Transport Packet with zero value timestamp.
	 */
	MPEG_TS_SD_KO,

	/**
	 * Korea region profile for Standard Definition AV class utilizing a DLNA
	 * Transport Packet without a Timestamp field.
	 */
	MPEG_TS_SD_KO_ISO,

	/**
	 * Korea region profile for Standard Definition AV class utilizing a DLNA
	 * Transport Packet with a valid non-zero timestamp.
	 */
	MPEG_TS_SD_KO_T,

	/** Profile for image class content of high resolution. */
	PNG_LRG,

	/** Profile for large icons */
	PNG_LRG_ICO,

	/** Profile for small icons */
	PNG_SM_ICO,

	/** Profile for image thumbnails */
	PNG_TN,

	/** Unofficial, unspecified WAV */
	WAV,

	/** WMA content (bit rates less than 193 kbit/s). */
	WMABASE,

	/** WMA content. */
	WMAFULL,

	/** WMA Lossless – stereo 16 bit, 2 channel. */
	WMALSL,

	/** WMA Lossless – Surround 24 bit, 5.1 channel. */
	WMALSL_MULT5,

	/** WMA professional version. */
	WMAPRO,

	/**
	 * WMDRM-ND Link Protection System, WMA content (bit rates less than 193
	 * kbit/s).
	 */
	WMDRM_WMABASE,

	/** WMDRM-ND Link Protection System, WMA content. */
	WMDRM_WMAFULL,

	/**
	 * WMDRM-ND Link Protection System, WMA Lossless – stereo 16 bit, 2
	 * channel.
	 */
	WMDRM_WMALSL,

	/**
	 * WMDRM-ND Link Protection System, High resolution video (Main Profile
	 * at High Level) with full WMA audio.
	 */
	WMDRM_WMVHIGH_FULL,

	/**
	 * WMDRM-ND Link Protection System, Medium resolution video (Main
	 * Profile at Medium Level) with baseline WMA audio.
	 */
	WMDRM_WMVMED_BASE,

	/**
	 * WMDRM-ND Link Protection System, Medium resolution video (Main
	 * Profile at Medium Level) with full WMA audio.
	 */
	WMDRM_WMVMED_FULL,

	/**
	 * High resolution video (Main Profile at High Level) with full WMA
	 * audio.
	 */
	WMVHIGH_FULL,

	/**
	 * High resolution video (Main Profile at High Level) with WMA
	 * professional audio.
	 */
	WMVHIGH_PRO,

	/** HighMAT profile. */
	WMVHM_BASE,

	/**
	 * Medium resolution video (Main Profile at Medium Level) with baseline
	 * WMA audio.
	 */
	WMVMED_BASE,

	/**
	 * Medium resolution video (Main Profile at Medium Level) with full WMA
	 * audio.
	 */
	WMVMED_FULL,

	/**
	 * Medium resolution video (Main Profile at Medium Level) with WMA
	 * professional audio.
	 */
	WMVMED_PRO,

	/**
	 * Low resolution video (Simple Profile at Low Level) with baseline WMA
	 * audio.
	 */
	WMVSPLL_BASE,

	/**
	 * Low resolution video (Simple profile at Medium Level) with baseline
	 * WMA audio.
	 */
	WMVSPML_BASE,

	/**
	 * Low resolution video (Simple Profile at Medium Level) with MP3 audio.
	 */
	WMVSPML_MP3;

	@Override
	public String getAttributeString() {
		return NAME + "=" + super.toString();
	}

	@Override
	public ProtocolInfoAttributeName getName() {
		return NAME;
	}

	@Override
	public String getNameString() {
		return NAME.getName();
	}

	@Override
	public String getValue() {
		return super.toString();
	}

	@Override
	public String toString() {
		return NAME + " = " + super.toString();
	}

}
