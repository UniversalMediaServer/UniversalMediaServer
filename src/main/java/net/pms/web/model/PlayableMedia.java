package net.pms.web.model;

public class PlayableMedia {

	private String id;

	private String name;

	private boolean push;

	private boolean src;

	private Integer delay; // image only

	private String type;

	private boolean autoplay;

	private boolean autoContinue;

	private String mime;

	private Integer sub;

	private Integer height;

	private Integer width;

	private boolean inPlaylist;

	private PlayableStep nextStep;

	private PlayableStep prevStep;

	public PlayableMedia(String id, String name, String type, boolean autoplay, String mime, Integer sub, Integer height, Integer width,
		boolean inPlaylist, PlayableStep[] steps, Integer delay, boolean push, boolean src, boolean autoContinue) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.autoplay = autoplay;
		this.mime = mime;
		this.sub = sub;
		this.height = height;
		this.width = width;
		this.inPlaylist = inPlaylist;
		this.prevStep = steps[0];
		this.nextStep = steps[1];
		this.delay = delay;
		this.push = push;
		this.src = src;
		this.autoContinue = autoContinue;
	}

	public boolean isAutoContinue() {
		return autoContinue;
	}

	public boolean getAutoplay() {
		return autoplay;
	}

	public Integer getDelay() {
		return delay;
	}

	public Integer getHeight() {
		return height;
	}

	public String getId() {
		return id;
	}

	public String getMime() {
		return mime;
	}

	public String getName() {
		return name;
	}

	public PlayableStep getNextStep() {
		return nextStep;
	}

	public PlayableStep getPrevStep() {
		return prevStep;
	}

	public Integer getSub() {
		return sub;
	}

	public String getType() {
		return type;
	}

	public Integer getWidth() {
		return width;
	}

	public boolean isInPlaylist() {
		return inPlaylist;
	}

	public boolean isPush() {
		return push;
	}

	public boolean isSrc() {
		return src;
	}

}
