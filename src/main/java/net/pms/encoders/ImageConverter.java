package net.pms.encoders;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import net.pms.Messages;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.formats.ImageFormat;
import net.pms.io.InternalJavaProcessImpl;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.network.HTTPResource;
import net.pms.util.ImagesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ImageConverter extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageConverter.class);
	public final static String ID = "imageconverter";

	@Override
	public JComponent config() {
		FormLayout layout = new FormLayout(
				"left:pref, 3dlu, p, 3dlu, 0:grow",
				"p, 3dlu, p, 3dlu, 0:grow"
			);
			PanelBuilder builder = new PanelBuilder(layout);
			builder.border(Borders.EMPTY);
			builder.opaque(false);

			CellConstraints cc = new CellConstraints();

			JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), cc.xyw(1, 1, 5));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
			builder.addLabel(Messages.getString("ImageConverter.0"), cc.xy(1, 3));
			return builder.getPanel();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public ProcessWrapper launchTranscode(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) throws IOException {
		params.waitbeforestart = 0;
		File file = new File(dlna.getSystemName());
		LOGGER.debug("Converting image {} to JPEG format", file);
		BufferedImage bufferedImage = ImageIO.read(file);
		int w = 0;
		int h = 0;
		// ensure that image size does'nt exceed the renderer capabilities
		if (bufferedImage.getWidth() > dlna.getDefaultRenderer().getMaxVideoWidth() || bufferedImage.getHeight() > dlna.getDefaultRenderer().getMaxVideoHeight()) {
			w = dlna.getDefaultRenderer().getMaxVideoWidth();
			h = dlna.getDefaultRenderer().getMaxVideoHeight();
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, FormatConfiguration.JPG, baos);
		ByteArrayInputStream imageInputStream = new ByteArrayInputStream(ImagesUtil.transcodeImage(baos.toByteArray(), w, h, null, ImageFormat.JPEG, true, false, true, false).getBytes(false));
		media.setSize(imageInputStream.available());
		media.setCodecV(FormatConfiguration.JPG);
		media.setContainer(FormatConfiguration.JPG);
		media.setMimeType(mimeType());
		return new InternalJavaProcessImpl(imageInputStream);
	}

	@Override
	public String mimeType() {
		return HTTPResource.JPEG_TYPEMIME;
	}

	@Override
	public String name() {
		return "Image Converter";
	}

	@Override
	public int purpose() {
		return MISC_PLAYER;
	}

	@Override
	public int type() {
		return Format.IMAGE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		return resource.getMedia().isImage();
	}

	@Override
	public String[] args() {
		return null;
	}

	@Override
	public String executable() {
		return NATIVE;
	}
}

