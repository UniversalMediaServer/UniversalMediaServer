/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
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
package net.pms.store.container;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import net.pms.Messages;
import net.pms.iam.User;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserVirtualFolder extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserVirtualFolder.class);
	private static final String JWT_SECRET = CONFIGURATION.getJwtSecret();

	public UserVirtualFolder(Renderer renderer, User user, boolean isLogOut) {
		super(renderer, isLogOut ? Messages.getString("LogOut") : user.getDisplayName(), null);
		String encrypted = encrypt(user.getId());
		setId(isLogOut ? "$LogOut/" : "$LogIn/" + encrypted);
	}

	private static String encrypt(final int value) {
		byte[] bytes = encryptor(ByteBuffer.allocate(4).putInt(value).array(), JWT_SECRET, false);
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}

	public static Integer decrypt(final String value) {
		try {
			byte[] bytes = new byte[value.length() / 2];
			for (int i = 0; i < bytes.length; i++) {
				int index = i * 2;
				int val = Integer.parseInt(value.substring(index, index + 2), 16);
				bytes[i] = (byte) val;
			}
			byte[] decrypted = encryptor(bytes, JWT_SECRET, true);
			return ByteBuffer.wrap(decrypted).getInt();
		} catch (NumberFormatException | BufferUnderflowException e) {
			return null;
		}
	}

	private static byte[] encryptor(final byte[] value, final String secret, boolean decrypt) {
		try {
			byte[] key = secret.getBytes(StandardCharsets.UTF_8);
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			if (decrypt) {
				cipher.init(Cipher.DECRYPT_MODE, secretKey);
			} else {
				cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			}
			return cipher.doFinal(value);
		} catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeyException e) {
			LOGGER.debug("Error while {}: {}", decrypt ? "decrypting" : "encrypting", e.toString());
		}
		return new byte[0];
	}

}
