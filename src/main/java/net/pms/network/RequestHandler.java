/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.external.StartStopListenerDelegate;
import net.pms.util.StringUtil;
import static net.pms.util.StringUtil.convertStringToTime;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class RequestHandler implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
	public final static int SOCKET_BUF_SIZE = 32768;
	private Socket socket;
	private OutputStream output;
	private BufferedReader br;

	// Used to filter out known headers when the renderer is not recognized
	private final static String[] KNOWN_HEADERS = {
		"Accept",
		"Accept-Language",
		"Accept-Encoding",
		"Callback",
		"Connection",
		"Content-Length",
		"Content-Type",
		"Date",
		"Host",
		"Nt",
		"Sid",
		"Timeout",
		"User-Agent"
	};

	public RequestHandler(Socket socket) throws IOException {
		this.socket = socket;
		this.output = socket.getOutputStream();
		this.br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
	}

	@Override
	public void run() {
		Request request = null;
		StartStopListenerDelegate startStopListenerDelegate = new StartStopListenerDelegate(socket.getInetAddress().getHostAddress());

		try {
			int receivedContentLength = -1;
			String userAgentString = null;
			ArrayList<String> identifiers = new ArrayList<>();
			RendererConfiguration renderer = null;

			InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
			InetAddress ia = remoteAddress.getAddress();

			boolean isSelf = ia.getHostAddress().equals(PMS.get().getServer().getHost());

			// Apply the IP filter
			if (filterIp(ia)) {
				throw new IOException("Access denied for address " + ia + " based on IP filter");
			}


			// The handler makes a couple of attempts to recognize a renderer from its requests.
			// IP address matches from previous requests are preferred, when that fails request
			// header matches are attempted and if those fail as well we're stuck with the
			// default renderer.

			// Attempt 1: try to recognize the renderer by its socket address from previous requests
			renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(ia);

			// If the renderer exists but isn't marked as loaded it means it's unrecognized
			// by upnp and we still need to attempt http recognition here.
			boolean unrecognized = renderer == null || !renderer.loaded;
			RendererConfiguration.SortedHeaderMap sortedHeaders = unrecognized ? new RendererConfiguration.SortedHeaderMap() : null;

			// Gather all the headers
			ArrayList<String> headerLines = new ArrayList<>();
			String line = br.readLine();
			while (line != null && line.length() > 0) {
				headerLines.add(line);
				if (sortedHeaders != null) {
					sortedHeaders.put(line);
				}
				line = br.readLine();
			}

			if (unrecognized) {
				// Attempt 2: try to recognize the renderer by matching headers
				renderer = RendererConfiguration.getRendererConfigurationByHeaders(sortedHeaders, ia);
			}

			for (String headerLine : headerLines) {

				// The request object is created inside the while loop.
				if (request != null && request.getMediaRenderer() == null && renderer != null) {
					request.setMediaRenderer(renderer);
				}
				if (headerLine.toUpperCase().startsWith("USER-AGENT")) {
					// Is the request from our own Cling service, i.e. self-originating?
					if (isSelf && headerLine.contains("UMS/")) {
						//LOGGER.trace("Ignoring self-originating request from {}:{}", ia, remoteAddress.getPort());
						return;
					}
					userAgentString = headerLine.substring(headerLine.indexOf(':') + 1).trim();
				} else if (renderer != null && headerLine.startsWith("X-PANASONIC-DMP-Profile:")) {
					PanasonicDmpProfiles.parsePanasonicDmpProfiles(headerLine, renderer);
				}

				try {
					StringTokenizer s = new StringTokenizer(headerLine);
					String temp = s.nextToken();
					if (temp.equals("SUBSCRIBE") || temp.equals("GET") || temp.equals("POST") || temp.equals("HEAD")) {
						request = new Request(temp, s.nextToken().substring(1));
						if (s.hasMoreTokens() && s.nextToken().equals("HTTP/1.0")) {
							request.setHttp10(true);
						}
					} else if (request != null && temp.toUpperCase().equals("CALLBACK:")) {
						request.setSoapaction(s.nextToken());
					} else if (request != null && temp.toUpperCase().equals("SOAPACTION:")) {
						request.setSoapaction(s.nextToken());
					} else if (headerLine.toUpperCase().contains("CONTENT-LENGTH:")) {
						receivedContentLength = Integer.parseInt(headerLine.substring(headerLine.toUpperCase().indexOf("CONTENT-LENGTH: ") + 16));
					} else if (headerLine.toUpperCase().contains("RANGE: BYTES=")) {
						String nums = headerLine.substring(headerLine.toUpperCase().indexOf("RANGE: BYTES=") + 13).trim();
						StringTokenizer st = new StringTokenizer(nums, "-");
						if (!nums.startsWith("-")) {
							request.setLowRange(Long.parseLong(st.nextToken()));
						}
						if (!nums.startsWith("-") && !nums.endsWith("-")) {
							request.setHighRange(Long.parseLong(st.nextToken()));
						} else {
							request.setHighRange(-1);
						}
					} else if (headerLine.toLowerCase().contains("transfermode.dlna.org:")) {
						request.setTransferMode(headerLine.substring(headerLine.toLowerCase().indexOf("transfermode.dlna.org:") + 22).trim());
					} else if (headerLine.toLowerCase().contains("getcontentfeatures.dlna.org:")) {
						request.setContentFeatures(headerLine.substring(headerLine.toLowerCase().indexOf("getcontentfeatures.dlna.org:") + 28).trim());
					} else if (headerLine.toUpperCase().contains("TIMESEEKRANGE.DLNA.ORG: NPT=")) { // firmware 2.50+
						String timeseek = headerLine.substring(headerLine.toUpperCase().indexOf("TIMESEEKRANGE.DLNA.ORG: NPT=") + 28);
						if (timeseek.endsWith("-")) {
							timeseek = timeseek.substring(0, timeseek.length() - 1);
						} else if (timeseek.indexOf('-') > -1) {
							timeseek = timeseek.substring(0, timeseek.indexOf('-'));
						}
						request.setTimeseek(convertStringToTime(timeseek));
					} else if (headerLine.toUpperCase().contains("TIMESEEKRANGE.DLNA.ORG : NPT=")) { // firmware 2.40
						String timeseek = headerLine.substring(headerLine.toUpperCase().indexOf("TIMESEEKRANGE.DLNA.ORG : NPT=") + 29);
						if (timeseek.endsWith("-")) {
							timeseek = timeseek.substring(0, timeseek.length() - 1);
						} else if (timeseek.indexOf('-') > -1) {
							timeseek = timeseek.substring(0, timeseek.indexOf('-'));
						}
						request.setTimeseek(convertStringToTime(timeseek));
					} else {
						/*
						 * If we made it to here, none of the previous header checks matched.
						 * Unknown headers make interesting logging info when we cannot recognize
						 * the media renderer, so keep track of the truly unknown ones.
						 */
						boolean isKnown = false;

						// Try to match possible known headers.
						String lowerCaseHeaderLine = headerLine.toLowerCase();
						for (String knownHeaderString : KNOWN_HEADERS) {
							if (lowerCaseHeaderLine.startsWith(knownHeaderString.toLowerCase())) {
								isKnown = true;
								break;
							}
						}

						// It may be unusual but already known
						if (renderer != null) {
							String additionalHeader = renderer.getUserAgentAdditionalHttpHeader();
							if (StringUtils.isNotBlank(additionalHeader) && lowerCaseHeaderLine.startsWith(additionalHeader)) {
								isKnown = true;
							}
						}

						if (!isKnown) {
							// Truly unknown header, therefore interesting. Save for later use.
							identifiers.add(headerLine);
						}
					}
				} catch (IllegalArgumentException e) {
					LOGGER.error("Error parsing HTTP headers: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			}

			if (request != null) {
				// Still no media renderer recognized?
				if (renderer == null) {
					// Attempt 3: Not really an attempt; all other attempts to recognize
					// the renderer have failed. The only option left is to assume the
					// default renderer.
					renderer = RendererConfiguration.resolve(ia, null);
					request.setMediaRenderer(renderer);
					if (renderer != null) {
						LOGGER.debug("Using default media renderer \"{}\"", renderer.getConfName());

						if (userAgentString != null && !userAgentString.equals("FDSSDP")) {
							// We have found an unknown renderer
							identifiers.add(0, "User-Agent: " + userAgentString);
							renderer.setIdentifiers(identifiers);
							LOGGER.info(
								"Media renderer was not recognized. Possible identifying HTTP headers:\n{}",
								StringUtils.join(identifiers, "\n")
							);
						}
					} else {
						// If RendererConfiguration.resolve() didn't return the default renderer
						// it means we know via upnp that it's not really a renderer.
						return;
					}
				} else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Recognized media renderer \"{}\"", renderer.getRendererName());
				}
			}

			if (receivedContentLength > 0) {
				char buf[] = new char[receivedContentLength];
				br.read(buf);
				if (request != null) {
					String textContent = new String(buf);
					request.setTextContent(textContent);
					if (LOGGER.isTraceEnabled()) {
						logMessageReceived(headerLines, textContent, socket.getRemoteSocketAddress(), renderer);
					}
				} else if (LOGGER.isTraceEnabled() ){
					logMessageReceived(headerLines, null, socket.getRemoteSocketAddress(), renderer);
				}
			}

			if (request != null) {
				request.answer(output, startStopListenerDelegate);
			}

			if (request != null && request.getInputStream() != null) {
				request.getInputStream().close();
			}
		} catch (IOException e) {
			LOGGER.error("Unexpected IO error in {}: {}", getClass().getName(), e.getMessage());
			// "An established connection was aborted by the software in your host machine"
			// is localized on Windows so there's no way to differentiate it
			// from other IOExceptions.
			//LOGGER.trace("", e);
			if (request != null && request.getInputStream() != null) {
				try {
					LOGGER.trace("Closing input stream: {}", request.getInputStream());
					request.getInputStream().close();
				} catch (IOException e1) {
					LOGGER.error("Error closing input stream: {}", e1);
					LOGGER.trace("", e1);
				}
			}
		} finally {
			try {
				output.close();
				br.close();
				socket.close();
			} catch (IOException e) {
				LOGGER.error("Error closing connection: {}", e.getMessage());
				LOGGER.trace("", e);
			}

			startStopListenerDelegate.stop();
		}
	}

	private static void logMessageReceived(List<String> headerLines, String content, SocketAddress remote, RendererConfiguration renderer) {
		StringBuilder header = new StringBuilder();
		String soapAction = null;

		if (headerLines != null) {
			if (!headerLines.isEmpty()) {
				header.append(headerLines.get(0)).append("\n\n");
			}
			if (headerLines.size() > 1) {
				header.append("HEADER:\n");
				for (int i = 1; i < headerLines.size(); i++) {
					if (isNotBlank(headerLines.get(i))) {
						header.append("  ").append(headerLines.get(i)).append("\n");
						if (headerLines.get(i).toUpperCase(Locale.ROOT).contains("SOAPACTION")) {
							soapAction = headerLines.get(i).toUpperCase(Locale.ROOT).replaceFirst("\\s*SOAPACTION:\\s*", "");
						}
					}
				}
			}
		} else {
			header.append("No header information available\n");
		}

		String formattedContent = null;
		if (StringUtils.isNotBlank(content)) {
			try {
				formattedContent = StringUtil.prettifyXML(content, 4);
			} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
				LOGGER.trace("XML parsing failed with:\n{}", e);
				formattedContent = "  Content isn't valid XML, using text formatting: " + e.getMessage()  + "\n";
				formattedContent += "    " + content.replaceAll("\n", "\n    ") + "\n";
			}
		}
		String requestType = "";
		// Map known requests to request type
		if (soapAction != null) {
			if (soapAction.contains("CONTENTDIRECTORY:1#BROWSE")) {
				requestType = "browse ";
			} else if (soapAction.contains("CONTENTDIRECTORY:1#SEARCH")) {
				requestType = "search ";
			}
		}
		String rendererName;
		if (renderer != null) {
			if (isNotBlank(renderer.getRendererName())) {
				if (isBlank(renderer.getConfName()) || renderer.getRendererName().equals(renderer.getConfName())) {
					rendererName = renderer.getRendererName();
				} else {
					rendererName = renderer.getRendererName() + " [" + renderer.getConfName() + "]";
				}
			} else if (isNotBlank(renderer.getConfName())) {
				rendererName = renderer.getConfName();
			} else {
				rendererName = "Unnamed";
			}
		} else {
			rendererName = "Unknown";
		}
		if (remote instanceof InetSocketAddress) {
			rendererName +=
				" (" + ((InetSocketAddress) remote).getAddress().getHostAddress() +
				":" + ((InetSocketAddress) remote).getPort() + ")";
		}

		LOGGER.trace(
			"Received a {}request from {}:\n\n{}{}",
			requestType,
			rendererName,
			header,
			StringUtils.isNotBlank(formattedContent) ? "\nCONTENT:\n" + formattedContent : ""
		);
	}

	/**
	 * Applies the IP filter to the specified internet address. Returns true
	 * if the address is not allowed and therefore should be filtered out,
	 * false otherwise.
	 *
	 * @param inetAddress The internet address to verify.
	 * @return True when not allowed, false otherwise.
	 */
	private static boolean filterIp(InetAddress inetAddress) {
		return !PMS.getConfiguration().getIpFiltering().allowed(inetAddress);
	}
}
