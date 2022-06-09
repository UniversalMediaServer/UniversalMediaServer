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

	public static String signJwt(String username) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
			String token = JWT.create()
					.withIssuer("UMS")
					.withSubject(username)
					.withExpiresAt(new Date(System.currentTimeMillis() + TWO_HOURS_IN_MS))
					.withClaim("username", username)
					.sign(algorithm);
			return token;
		} catch (JWTCreationException e) {
			LOGGER.error("Error signing JWT: {}", e.getMessage());
		}
		return null;
	}

	public static DecodedJWT decodeJwt(String token) {
		try {
			DecodedJWT jwt = JWT.decode(token);
			return jwt;
		} catch (JWTDecodeException e) {
			LOGGER.error("Error decoding JWT: {}", e.getMessage());
		}
		return null;
	}

	public static String getUsernameFromJWT(List<String> authHeader) {
		if (authHeader == null || authHeader.isEmpty()) {
			return null;
		}
		final String token = authHeader.get(0).replace("Bearer ", "");
		try {
			DecodedJWT jwt = decodeJwt(token);
			return jwt.getClaim("username").asString();
		} catch (JWTDecodeException e) {
			LOGGER.error("Error decoding JWT: {}", e.getMessage());
		}
		return null;
	}

	public static Boolean isLoggedIn(List<String> authHeader) {
		if (authHeader == null || authHeader.isEmpty()) {
			return false;
		}
		final String token = authHeader.get(0).replace("Bearer ", "");
		try {
			Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
			JWTVerifier verifier = JWT.require(algorithm)
					.withIssuer("UMS")
					.build();
			verifier.verify(token);
			return true;
		} catch (JWTVerificationException e) {
			LOGGER.error("Error verifying JWT: {}", e.getMessage());
			return false;
		}
	}
}
