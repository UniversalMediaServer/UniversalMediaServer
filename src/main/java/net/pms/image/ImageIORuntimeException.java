package net.pms.image;

import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This is simply a checked wrapper for {@link RuntimeException}s thrown by
 * {@link ImageIO}. It is used to translate thrown {@link RuntimeException}s to
 * {@link IOException}s so they can be handled. This is needed because
 * {@link ImageIO} has the nasty habit of throwing {@link RuntimeException}s
 * when something goes wrong during an operation.
 *
 * @author Nadahar
 */

public class ImageIORuntimeException extends IOException {
	private static final long serialVersionUID = 1L;

	public ImageIORuntimeException(RuntimeException cause) {
		super(cause);
	}

	public ImageIORuntimeException(String message, RuntimeException cause) {
		super(message, cause);
	}

}
