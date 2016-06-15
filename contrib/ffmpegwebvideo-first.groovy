import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.pms.PMS;
import net.pms.encoders.PlayerFactory;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.configuration.PmsConfiguration;

import com.chocolatey.pmsencoder.PMSEncoder;

// USAGE:
//   Place this file in your pmsencoder scripts directory
//   if you want to use PMSEncoder as a secondary engine
//   after FFmpeg Web Video.

init {
	$PMS.debug("ffmpegwebvideo_first.groovy: initializing...")
	def configuration = $PMS.getConfiguration()
	
	// register FFmpegWebVideo before PMSEncoder
	PlayerFactory.registerPlayer(new FFmpegWebVideo(configuration))

	// put ffmpegwebvideo first in engines list
	def engines = new ArrayList<String>(configuration.getEnginesAsList($PMS.getRegistry()))
	engines.removeAll([PMSEncoder.ID, FFmpegWebVideo.ID])
	engines.add(0, FFmpegWebVideo.ID)
	engines.add(1, PMSEncoder.ID)
	configuration.setEnginesAsList(engines)

	// spoof ID to disarm the upcoming Plugin.enable() call
	def field = PMSEncoder.getDeclaredField("ID")
	def modifiers = Field.class.getDeclaredField("modifiers");
	modifiers.setAccessible(true)
	modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL)
	field.set(field.get(PMSEncoder), FFmpegWebVideo.ID)
}

// note: the block below will be triggered by the first call to launchTranscode()

script {
	profile ('cleanup') {
		pattern {
			if (! PMSEncoder.ID.equals("pmsencoder")) {
				$PMS.debug("ffmpegwebvideo_first.groovy: cleaning up...")
				// undo ID spoof
				def field = PMSEncoder.getDeclaredField("ID")
				def modifiers = Field.class.getDeclaredField("modifiers")
				modifiers.setAccessible(true)
				modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL)
				final String id = "pmsencoder"
				field.set(field.get(PMSEncoder), id)
			}
			match { false }
		}
	}
}

