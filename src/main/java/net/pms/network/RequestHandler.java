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
import java.util.StringTokenizer;

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.external.StartStopListenerDelegate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	public void run() {
		Request request = null;
		StartStopListenerDelegate startStopListenerDelegate = new StartStopListenerDelegate(
			socket.getInetAddress().getHostAddress());

		try {
			int receivedContentLength = -1;
			String headerLine = br.readLine();
			String userAgentString = null;
			StringBuilder unknownHeaders = new StringBuilder();
			String separator = "";
			RendererConfiguration renderer = null;

			InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
			InetAddress ia = remoteAddress.getAddress();

			// Apply the IP filter
			if (filterIp(ia)) {
				throw new IOException("Access denied for address " + ia + " based on IP filter");
			}

			LOGGER.trace("Opened request handler on socket " + socket);
			PMS.get().getRegistry().disableGoToSleep();

			while (headerLine != null && headerLine.length() > 0) {
				LOGGER.trace("Received on socket: " + headerLine);

				// The request object is created inside the while loop.
				if (request != null && request.getMediaRenderer() == null) {
					// The handler makes a couple of attempts to recognize a renderer from its requests.
					// IP address matches from previous requests are preferred, when that fails request
					// header matches are attempted and if those fail as well we're stuck with the
					// default renderer.

					// Attempt 1: try to recognize the renderer by its socket address from previous requests
					renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(ia);

					if (renderer != null) {
						PMS.get().setRendererfound(renderer);
						request.setMediaRenderer(renderer);
						LOGGER.trace("Matched media renderer \"" + renderer.getRendererName() + "\" based on address " + ia);
					}
				}

				if (renderer == null && headerLine != null
						&& headerLine.toUpperCase().startsWith("USER-AGENT")
						&& request != null) {
					userAgentString = headerLine.substring(headerLine.indexOf(":") + 1).trim();

					// Attempt 2: try to recognize the renderer by matching the "User-Agent" header
					renderer = RendererConfiguration.getRendererConfigurationByUA(userAgentString);

					if (renderer != null) {
						PMS.get().setRendererfound(renderer);
						request.setMediaRenderer(renderer);
						renderer.associateIP(ia);	// Associate IP address for later requests
						LOGGER.trace("Matched media renderer \"" + renderer.getRendererName() + "\" based on header \"" + headerLine + "\"");
					}
				}
				if (renderer == null && headerLine != null && request != null) {
					// Attempt 3: try to recognize the renderer by matching an additional header
					renderer = RendererConfiguration.getRendererConfigurationByUAAHH(headerLine);

					if (renderer != null) {
						PMS.get().setRendererfound(renderer);
						request.setMediaRenderer(renderer);
						renderer.associateIP(ia);	// Associate IP address for later requests
						LOGGER.trace("Matched media renderer \"" + renderer.getRendererName() + "\" based on header \"" + headerLine + "\"");
					}
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
					} else if (headerLine.toUpperCase().indexOf("RANGE: BYTES=") > -1) {
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
					} else if (headerLine.toLowerCase().indexOf("transfermode.dlna.org:") > -1) {
						request.setTransferMode(headerLine.substring(headerLine.toLowerCase().indexOf("transfermode.dlna.org:") + 22).trim());
					} else if (headerLine.toLowerCase().indexOf("getcontentfeatures.dlna.org:") > -1) {
						request.setContentFeatures(headerLine.substring(headerLine.toLowerCase().indexOf("getcontentfeatures.dlna.org:") + 28).trim());
					} else if (headerLine.toUpperCase().indexOf("TIMESEEKRANGE.DLNA.ORG: NPT=") > -1) { // firmware 2.50+
						String timeseek = headerLine.substring(headerLine.toUpperCase().indexOf("TIMESEEKRANGE.DLNA.ORG: NPT=") + 28);
						if (timeseek.endsWith("-")) {
							timeseek = timeseek.substring(0, timeseek.length() - 1);
						} else if (timeseek.indexOf("-") > -1) {
							timeseek = timeseek.substring(0, timeseek.indexOf("-"));
						}
						request.setTimeseek(Double.parseDouble(timeseek));
					} else if (headerLine.toUpperCase().indexOf("TIMESEEKRANGE.DLNA.ORG : NPT=") > -1) { // firmware 2.40
						String timeseek = headerLine.substring(headerLine.toUpperCase().indexOf("TIMESEEKRANGE.DLNA.ORG : NPT=") + 29);
						if (timeseek.endsWith("-")) {
							timeseek = timeseek.substring(0, timeseek.length() - 1);
						} else if (timeseek.indexOf("-") > -1) {
							timeseek = timeseek.substring(0, timeseek.indexOf("-"));
						}
						request.setTimeseek(Double.parseDouble(timeseek));
					} else {
						 // If we made it to here, none of the previous header checks matched.
						 // Unknown headers make interesting logging info when we cannot recognize
						 // the media renderer, so keep track of the truly unknown ones.
						boolean isKnown = false;
						
						// Try to match possible known headers.
						for (String knownHeaderString : KNOWN_HEADERS) {
							if (headerLine.toLowerCase().startsWith(knownHeaderString.toLowerCase())) {
								isKnown = true;
								break;
							}
						}
						
						if (!isKnown) {
							// Truly unknown header, therefore interesting. Save for later use.
							unknownHeaders.append(separator + headerLine);
							separator = ", ";
						}
					}
				} catch (Exception e) {
					LOGGER.error("Error in parsing HTTP headers", e);
				}

				headerLine = br.readLine();
			}

			if (request != null) {
				// Still no media renderer recognized?
				if (request.getMediaRenderer() == null) {
					
					// Attempt 4: Not really an attempt; all other attempts to recognize
					// the renderer have failed. The only option left is to assume the
					// default renderer.
					request.setMediaRenderer(RendererConfiguration.getDefaultConf());
					LOGGER.trace("Using default media renderer " + request.getMediaRenderer().getRendererName());

					if (userAgentString != null && !userAgentString.equals("FDSSDP")) {
						// We have found an unknown renderer
						LOGGER.info("Media renderer was not recognized. Possible identifying HTTP headers: User-Agent: "	+ userAgentString
								+ ("".equals(unknownHeaders.toString()) ? "" : ", " + unknownHeaders.toString()));
						PMS.get().setRendererfound(request.getMediaRenderer());
					}
				} else {
					if (userAgentString != null) {
						LOGGER.trace("HTTP User-Agent: " + userAgentString);
					}
					LOGGER.trace("Recognized media renderer " + request.getMediaRenderer().getRendererName());
				}
			}

			if (receivedContentLength > 0) {
				char buf[] = new char[receivedContentLength];
				br.read(buf);
				if (request != null) {
					request.setTextContent(new String(buf));
				}
			}

			if (request != null) {
				LOGGER.trace("HTTP: " + request.getArgument() + " / " + request.getLowRange() + "-" + request.getHighRange());
			}

			if (request != null) {
				request.answer(output, startStopListenerDelegate);
			}

			if (request != null && request.getInputStream() != null) {
				request.getInputStream().close();
			}

		} catch (IOException e) {
			LOGGER.trace("Unexpected IO error: " + e.getClass() + ": " + e.getMessage());
			if (request != null && request.getInputStream() != null) {
				try {
					LOGGER.trace("Closing input stream: " + request.getInputStream());
					request.getInputStream().close();
				} catch (IOException e1) {
					LOGGER.error("Error closing input stream", e);
				}
			}
		} finally {
			try {
				PMS.get().getRegistry().reenableGoToSleep();
				output.close();
				br.close();
				socket.close();
			} catch (IOException e) {
				LOGGER.error("Error closing connection: ", e);
			}

			startStopListenerDelegate.stop();
			LOGGER.trace("Close connection");
		}
	}
	
	/**
	 * Applies the IP filter to the specified internet address. Returns true
	 * if the address is not allowed and therefore should be filtered out,
	 * false otherwise.
	 * @param inetAddress The internet address to verify.
	 * @return True when not allowed, false otherwise.
	 */
	private boolean filterIp(InetAddress inetAddress) {
		return !PMS.getConfiguration().getIpFiltering().allowed(inetAddress);
	}
}
