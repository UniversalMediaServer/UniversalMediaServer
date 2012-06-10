package net.pms.configuration;

class ProgramPathDisabler implements ProgramPaths {
	private boolean disableVlc = false;
	private boolean disableMencoder = false;
	private boolean disableFfmpeg = false;
	private boolean disableMplayer = false;
	private boolean disableDCraw = false;
	private boolean disableIMConvert = false;
	private boolean disableInterFrame = false;
	private final ProgramPaths ifEnabled;

	public ProgramPathDisabler(ProgramPaths ifEnabled) {
		this.ifEnabled = ifEnabled;
	}

	@Override
	public String getEac3toPath() {
		return ifEnabled.getEac3toPath();
	}

	@Override
	public String getFfmpegPath() {
		return disableFfmpeg ? null : ifEnabled.getFfmpegPath();
	}

	@Override
	public String getFlacPath() {
		return ifEnabled.getFlacPath();
	}

	@Override
	public String getMencoderPath() {
		return disableMencoder ? null : ifEnabled.getMencoderPath();
	}

	@Override
	public String getMplayerPath() {
		return disableMplayer ? null : ifEnabled.getMplayerPath();
	}

	@Override
	public String getTsmuxerPath() {
		return ifEnabled.getTsmuxerPath();
	}

	@Override
	public String getVlcPath() {
		return disableVlc ? null : ifEnabled.getVlcPath();
	}

	public void disableVlc() {
		disableVlc = true;
	}

	public void disableMencoder() {
		disableMencoder = true;
	}

	public void disableFfmpeg() {
		disableFfmpeg = true;
	}

	public void disableMplayer() {
		disableMplayer = true;
	}

	@Override
	public String getDCRaw() {
		return disableDCraw ? null : ifEnabled.getDCRaw();
	}

	@Override
	public String getIMConvertPath() {
		return disableIMConvert ? null : ifEnabled.getIMConvertPath();
	}

	@Override
	public String getInterFramePath() {
		return disableInterFrame ? null : ifEnabled.getInterFramePath();
	}
}
