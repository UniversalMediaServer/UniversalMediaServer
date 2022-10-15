/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.formats.image;

/**
 * A representation of the Truevision Targa Graphic file format.
 *
 * @author Nadahar
 */
public class TGA extends ImageBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.TGA;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"tga",
			"icb",
			"vda",
			"vstrle"
		};
	}

	@Override
	public String mimeType() {
		/*
		 * application/tga,
		 * application/x-tga,
		 * application/x-targa,
		 * image/tga,
		 * image/x-tga,
		 * image/targa,
		 * image/x-targa
		 */
		return "image/x-tga";
	}

}
