/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.iam;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;
import java.util.List;
import net.pms.PMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
	private static final String JWT_SECRET = PMS.getConfiguration().getJwtSecret();
	private static final int TWO_HOURS_IN_MS = 7200000;
	private static final String JWT_ISSUER = "UMS";

	public static String signJwt(int id, String host) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
			String token = JWT.create()
					.withIssuer(JWT_ISSUER)
					.withSubject(host)
					.withExpiresAt(new Date(System.currentTimeMillis() + TWO_HOURS_IN_MS))
					.withClaim("id", id)
					.sign(algorithm);
			return token;
		} catch (JWTCreationException e) {
			LOGGER.error("Error signing JWT: {}", e.getMessage());
		}
		return null;
	}

	private static DecodedJWT decodeJwt(String token) {
		try {
			DecodedJWT jwt = JWT.decode(token);
			return jwt;
		} catch (JWTDecodeException e) {
			LOGGER.error("Error decoding JWT: {}", e.getMessage());
		}
		return null;
	}

	private static int getUserIdFromJWT(String token) {
		try {
			DecodedJWT jwt = decodeJwt(token);
			return jwt.getClaim("id").asInt();
		} catch (JWTDecodeException e) {
			LOGGER.error("Error decoding JWT: {}", e.getMessage());
		}
		return 0;
	}

	public static Boolean isValidToken(String token, String host) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
			JWTVerifier verifier = JWT.require(algorithm)
					.withIssuer(JWT_ISSUER)
					.withSubject(host)
					.build();
			verifier.verify(token);
			return true;
		} catch (JWTVerificationException e) {
			LOGGER.error("Error verifying JWT: {}", e.getMessage());
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

	public static Account getAccountLoggedIn(List<String> authHeaders, String host) {
		if (authHeaders == null || authHeaders.isEmpty()) {
			return null;
		}
		return getAccountLoggedIn(authHeaders.get(0), host);
	}

	public static boolean validatePayload(int expire, String issuer, String subject, String host) {
		if (subject == null || host == null || !host.equals(subject) ||
				issuer == null || !issuer.equals(JWT_ISSUER)) {
			return false;
		}
		long currentTime = System.currentTimeMillis() / 1000L;
		long issuedTime = expire - (TWO_HOURS_IN_MS / 1000L);
		return expire > currentTime && issuedTime < currentTime;
	}

}
