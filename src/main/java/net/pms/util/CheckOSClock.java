package net.pms.util;

import java.awt.Component;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.PMS;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CheckOSClock implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckOSClock.class);
	private static final String[] NTPServers = {"0.pool.ntp.org", "1.pool.ntp.org", "2.pool.ntp.org", "3.pool.ntp.org"};

	private static String constructTimeOffsetString(long offsetMS, boolean localize) {
		String offset;
		if (Math.abs(offsetMS) > 7140000) { // 1 hour 59 minutes
			offset = String.format(Messages.getString("CheckOSClock.4", localize ? "" : "en"), Math.abs(Math.round(offsetMS/3600000)));
		} else if (Math.abs(offsetMS) > 179000) { // 2 minutes 59 seconds
			offset = String.format(Messages.getString("CheckOSClock.5", localize ? "" : "en"), Math.abs(Math.round(offsetMS/60000)));
		} else {
			offset = String.format(Messages.getString("CheckOSClock.6", localize ? "" : "en"), Math.abs(Math.round(offsetMS/1000)));
		}
		if (offsetMS > 0) {
			return String.format(localize ? Messages.getString("CheckOSClock.1") : "Computer clock is running %s to slow.", offset);
		} else {
			return String.format(localize ? Messages.getString("CheckOSClock.2") : "Computer clock is running %s to fast.", offset);
		}
	}

	private void handleTimeDiscrepancy(long offsetMS) {
		// The discrepancy threshold in milliseconds for warning the user
		if (Math.abs(offsetMS) > 500) {
			String message = constructTimeOffsetString(offsetMS, true) + " " + Messages.getString("CheckOSClock.3");
			LOGGER.warn(message);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(constructTimeOffsetString(offsetMS, false));
			}
			if (!PMS.isHeadless()) {
				if (PMS.get().getFrame() != null) {
					JOptionPane.showMessageDialog(
						SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame()),
						message,
						Messages.getString("Dialog.Warning"),
						JOptionPane.WARNING_MESSAGE
					);
				} else {
					JOptionPane.showMessageDialog(
							null,
							message,
							Messages.getString("Dialog.Warning"),
							JOptionPane.WARNING_MESSAGE
						);
				}
			}
		} else if (LOGGER.isDebugEnabled()) {
			if (Math.round(offsetMS/1000) == 0) {
				LOGGER.debug("Computer clock is correct");
			} else {
				LOGGER.debug(constructTimeOffsetString(offsetMS, false));
			}
		}
	}

	@Override
	public void run() {
    	if (NTPServers.length < 1) {
    		LOGGER.debug("No NTP servers to check, aborting..");
    		return;
    	}

        NTPUDPClient client = new NTPUDPClient();
        // Timeout in milliseconds
        client.setDefaultTimeout(10000);
        try {
            client.open();
            int i = 0;
            int j = (int) Math.round(Math.random() * 3);
            LOGGER.trace("j = {}", j);
            TimeInfo info = null;
            while (info == null && i < NTPServers.length) {
            	try {
					InetAddress hostAddr = InetAddress.getByName(NTPServers[j]);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("CheckOSClock: Resolved {}", hostAddr);
					}
					info = client.getTime(hostAddr);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("CheckOSClock: Got response from {}", hostAddr);
					}
				} catch (IOException e) {
					LOGGER.debug("Error querying NTP server \"{}\": {}", NTPServers[j], e.getMessage());
				}
            	i++;
            	if (j == NTPServers.length - 1) {
            		j = 0;
            	} else {
            		j++;
            	}
            }
            if (info != null) {
            	LOGGER.trace("CheckOSClock: Starting to compute details");
            	info.computeDetails();
            	if (LOGGER.isTraceEnabled()) {
            		LOGGER.trace("CheckOSClock: Done computing details. Delay was {}", info.getDelay());
            	}
            	Long offsetMS = info.getOffset();
            	if (offsetMS == null) {
            		LOGGER.warn("Could not calculate OS clock offset, got invalid result");
            	} else {
            		handleTimeDiscrepancy(offsetMS);
            	}
            } else {
            	LOGGER.debug("Could not query NTP server after {} attempts. Check network connectivity and DNS resolution", NTPServers.length);
            }
        } catch (SocketException e1) {
        	LOGGER.warn("Could not calculate OS clock offset: {}", e1.getMessage());
		} finally {
            client.close();
        }
    }
}
