package net.pms.network.mediaserver.javahttpserver;

import java.util.Date;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);
    private static int twoHoursInMs = 7200000;
    public static Boolean containsValidJwt(HttpExchange exchange) {
        return true;
    }

    public static String signJwt(String username) {
        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            String token = JWT.create()
                .withIssuer("UMS")
                .withSubject(username)
                .withExpiresAt(new Date(System.currentTimeMillis()+twoHoursInMs))
                .withClaim("username", username)
                .sign(algorithm);
            return token;
        } catch (JWTCreationException e){
            LOGGER.error("Error signing JWT: {}", e.getMessage());
        }
        return null;
    }

    public static DecodedJWT decodeJwt(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt;
        } catch (JWTDecodeException e){
            LOGGER.error("Error decoding JWT: {}", e.getMessage());
        }
        return null;
    }

    public static String getUsernameFromJWT(HttpExchange exchange) {
        final List<String> authHeader = exchange.getRequestHeaders().get("Authorization");
		final String token = authHeader.get(0).replace("Bearer ", "");
        try {
            DecodedJWT jwt = decodeJwt(token);
            return jwt.getClaim("username").asString();
        } catch (JWTDecodeException e){
            LOGGER.error("Error decoding JWT: {}", e.getMessage());
        }
        return null;
    }

    public static Boolean isLoggedIn(HttpExchange exchange) {
		final List<String> authHeader = exchange.getRequestHeaders().get("Authorization");
		final String token = authHeader.get(0).replace("Bearer ", "");
		try {
			Algorithm algorithm = Algorithm.HMAC256("secret"); //use more secure key
			JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer("UMS")
				.build();
			verifier.verify(token);
			return true;
		} catch (JWTVerificationException e){
			LOGGER.error("Error verifying JWT: {}", e.getMessage());
			return false;
		}
	}
}