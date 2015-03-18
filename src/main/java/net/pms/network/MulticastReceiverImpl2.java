/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package net.pms.network;
//package org.fourthline.cling.transport.impl;

import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.spi.DatagramProcessor;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.MulticastReceiver;
import org.fourthline.cling.model.UnsupportedDataException;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.impl.MulticastReceiverImpl;
import org.fourthline.cling.transport.impl.MulticastReceiverConfigurationImpl;

import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
//import java.util.logging.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation based on a UDP <code>MulticastSocket</code>.
 * <p>
 * Thread-safety is guaranteed through synchronization of methods of this service and
 * by the thread-safe underlying socket.
 * </p>
 * @author Christian Bauer
 */
public class MulticastReceiverImpl2 extends MulticastReceiverImpl {

	// Logger ids to write messages to the logs.
	private static final Logger LOGGER = LoggerFactory.getLogger(MulticastReceiverImpl2.class);
//	private static Logger log = Logger.getLogger(MulticastReceiver.class.getName());

//	final protected MulticastReceiverConfigurationImpl configuration;

//	protected Router router;
//	protected NetworkAddressFactory networkAddressFactory;
//	protected DatagramProcessor datagramProcessor;

//	protected NetworkInterface multicastInterface;
//	protected InetSocketAddress multicastAddress;
//	protected MulticastSocket socket;
	protected volatile boolean stopping = false;

	public MulticastReceiverImpl2(MulticastReceiverConfigurationImpl configuration) {
		super(configuration);
	}

//	public MulticastReceiverConfigurationImpl getConfiguration() {
//		return configuration;
//	}

	synchronized public void init(NetworkInterface networkInterface,
								  Router router,
								  NetworkAddressFactory networkAddressFactory,
								  DatagramProcessor datagramProcessor) throws InitializationException {
//		this.router = router;
//		this.networkAddressFactory = networkAddressFactory;
//		this.datagramProcessor = datagramProcessor;
//		this.multicastInterface = networkInterface;

		super.init(networkInterface, router, networkAddressFactory, datagramProcessor);
		try {
//			log.info("Creating wildcard socket (for receiving multicast datagrams) on port: " + configuration.getPort());
//			multicastAddress = new InetSocketAddress(configuration.getGroup(), configuration.getPort());

//			socket = new MulticastSocket(configuration.getPort());
//			socket.setReuseAddress(true);
//			socket.setReceiveBufferSize(32768); // Keep a backlog of incoming datagrams if we are not fast enough

//			log.info("Joining multicast group: " + multicastAddress + " on network interface: " + multicastInterface.getDisplayName());
//			socket.joinGroup(multicastAddress, multicastInterface);

			socket.setSoTimeout(1000);
		} catch (Exception ex) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex);
		}
	}

	synchronized public void stop() {
		LOGGER.debug("Stopping {}:{}", socket.getLocalAddress(), socket.getLocalPort());
		stopping = true;
		close();
	}
	synchronized public void close() {
		if (socket != null && !socket.isClosed()) {
			LOGGER.debug("Closing multicast socket {}:{}", socket.getLocalAddress(), socket.getLocalPort());
			try {
				LOGGER.debug("Leaving multicast group");
				socket.leaveGroup(multicastAddress, multicastInterface);
				// Well this doesn't work and I have no idea why I get "java.net.SocketException: Can't assign requested address"
			} catch (Exception ex) {
				LOGGER.debug("Could not leave multicast group: " + ex);
			}
			// So... just close it and ignore the log messages
			socket.close();
		}
	}

	public void run() {
		String socketAddr = "" + socket.getLocalAddress() + ":" + socket.getLocalPort();
		LOGGER.debug("Entering blocking receiving loop, listening for UDP datagrams on: " + socketAddr);

		while (! stopping) {

			try {
				byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
				DatagramPacket datagram = new DatagramPacket(buf, buf.length);

				socket.receive(datagram);

				InetAddress receivedOnLocalAddress =
						networkAddressFactory.getLocalAddress(
							multicastInterface,
							multicastAddress.getAddress() instanceof Inet6Address,
							datagram.getAddress()
						);

				LOGGER.debug(
						"UDP datagram received from: " + datagram.getAddress().getHostAddress() 
								+ ":" + datagram.getPort()
								+ " on local interface: " + multicastInterface.getDisplayName()
								+ " and address: " + receivedOnLocalAddress.getHostAddress()
				);

				router.received(datagramProcessor.read(receivedOnLocalAddress, datagram));

			} catch (SocketTimeoutException ex) {
				LOGGER.debug("udp timeout on {}: stopping={}", socketAddr, stopping);
				continue;
			} catch (SocketException ex) {
				LOGGER.debug("Socket closed");
				break;
			} catch (UnsupportedDataException ex) {
				LOGGER.info("Could not read datagram: " + ex.getMessage());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		LOGGER.debug("Exiting blocking receiving loop on: " + socketAddr);
		try {
			close();
//			if (!socket.isClosed()) {
//				LOGGER.debug("Closing multicast socket");
//				socket.close();
//			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}


}

