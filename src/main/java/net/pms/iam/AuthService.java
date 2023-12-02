/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.iam;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {

	/**
	 * This class is not meant to be instantiated.
	 */
	private AuthService() {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String JWT_SECRET = CONFIGURATION.getJwtSecret();
	private static final int TWO_HOURS_IN_MS = 7200000;
	private static final String JWT_ISSUER = "UMS";

	public static String signJwt(int id, String host) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
			return JWT.create()
				.withIssuer(JWT_ISSUER)
				.withSubject(host)
				.withExpiresAt(new Date(System.currentTimeMillis() + TWO_HOURS_IN_MS))
				.withClaim("id", id)
				.sign(algorithm);
		} catch (JWTCreationException e) {
			LOGGER.warn("Error signing JWT: {}", e.getMessage());
		}
		return null;
	}

	private static DecodedJWT decodeJwt(String token) {
		try {
			return JWT.decode(token);
		} catch (JWTDecodeException e) {
			LOGGER.warn("Error decoding JWT: {}", e.getMessage());
		}
		return null;
	}

	private static int getUserIdFromJWT(String token) {
		try {
			DecodedJWT jwt = decodeJwt(token);
			if (jwt != null) {
				return jwt.getClaim("id").asInt();
			}
		} catch (JWTDecodeException e) {
			LOGGER.warn("Error decoding JWT: {}", e.getMessage());
		}
		return 0;
	}

	public static boolean isValidToken(String token, String host) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
			JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer(JWT_ISSUER)
				.withSubject(host)
				.build();
			verifier.verify(token);
			return true;
		} catch (JWTVerificationException e) {
			LOGGER.warn("Error verifying JWT: {}", e.getMessage());
			return false;
		}
	}

	private static Account getAccountLoggedIn(String authHeader, String host) {
		final String token = authHeader.replace("Bearer ", "");
		if (isValidToken(token, host)) {
			int userId = getUserIdFromJWT(token);
			return AccountService.getAccountByUserId(userId);
		}
		return null;
	}

	public static Account getAccountLoggedIn(List<String> authHeaders, String host, boolean isLocalhost) {
		if (!isEnabled() || (isLocalhost && isLocalhostAsAdmin())) {
			return AccountService.getFakeAdminAccount();
		}
		if (authHeaders == null || authHeaders.isEmpty()) {
			return null;
		}
		return getAccountLoggedIn(authHeaders.get(0), host);
	}

	public static Account getAccountLoggedIn(HttpServletRequest req) {
		if (!isEnabled() ||
			(req.getRemoteAddr().equals(req.getLocalAddr()) && isLocalhostAsAdmin())
			) {
			return AccountService.getFakeAdminAccount();
		}
		String authHeader = req.getHeader("Authorization");
		if (authHeader == null) {
			return null;
		}
		return getAccountLoggedIn(authHeader, req.getRemoteAddr());
	}

	public static Account getAccountLoggedIn(String authHeader, String host, boolean isLocalhost) {
		if (!isEnabled() || (isLocalhost && isLocalhostAsAdmin())) {
			return AccountService.getFakeAdminAccount();
		}
		if (authHeader == null) {
			return null;
		}
		return getAccountLoggedIn(authHeader, host);
	}

	public static Account getPlayerAccountLoggedIn(HttpServletRequest req) {
		if (isPlayerRequest(req) && !isPlayerEnabled()) {
			Account account = new Account();
			Group group = new Group();
			group.setId(Integer.MAX_VALUE);
			int permissions = Permissions.WEB_PLAYER_BROWSE;
			if (CONFIGURATION.useWebPlayerControls()) {
				permissions |= Permissions.DEVICES_CONTROL;
			}
			if (CONFIGURATION.useWebPlayerDownload()) {
				permissions |= Permissions.WEB_PLAYER_DOWNLOAD;
			}
			group.setPermissions(permissions);
			account.setGroup(group);
			User user = new User();
			user.setId(Integer.MAX_VALUE);
			user.setGroupId(Integer.MAX_VALUE);
			account.setUser(user);
			return account;
		}
		return getAccountLoggedIn(req);
	}

	private static boolean isPlayerRequest(HttpServletRequest req) {
		return CONFIGURATION.useWebPlayerServer() && req.getLocalPort() == CONFIGURATION.getWebPlayerServerPort();
	}

	public static boolean isEnabled() {
		return CONFIGURATION.isAuthenticationEnabled();
	}

	public static boolean isPlayerEnabled() {
		return isEnabled() && (!CONFIGURATION.useWebPlayerServer() || CONFIGURATION.isWebPlayerAuthenticationEnabled());
	}

	public static void setEnabled(boolean value) {
		CONFIGURATION.setAuthenticationEnabled(value);
	}

	public static boolean isLocalhostAsAdmin() {
		return CONFIGURATION.isAuthenticateLocalhostAsAdmin();
	}

	public static void setLocalhostAsAdmin(boolean value) {
		CONFIGURATION.setAuthenticateLocalhostAsAdmin(value);
	}
}
