package net.pms.renderers.devices.players;

import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;

public interface BasicPlayer extends ActionListener {

	public void setURI(String uri, String metadata);

	public void pressPlay(String uri, String metadata);

	public void pressStop();

	public void play();

	public void pause();

	public void stop();

	public void next();

	public void prev();

	public void forward();

	public void rewind();

	public void mute();

	public void setVolume(int volume);

	public void add(int index, String uri, String name, String metadata, boolean select);

	public void remove(String uri);

	public void setBuffer(long mb);

	public PlayerState getState();

	public int getControls();

	public DefaultComboBoxModel getPlaylist();

	public void connect(ActionListener listener);

	public void disconnect(ActionListener listener);

	public void alert();

	public void start();

	public void reset();

	public void close();

}
