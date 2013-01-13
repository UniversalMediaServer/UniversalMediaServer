package net.pms.xmlwise;

/**
 * Generic exception when parsing xml.
 * 
 * @author Christoffer Lerno
 */
public class XmlParseException extends Exception {
	private static final long serialVersionUID = -3246260520113823143L;

	public XmlParseException(Throwable cause) {
		super(cause);
	}

	public XmlParseException(String message) {
		super(message);
	}

	public XmlParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
