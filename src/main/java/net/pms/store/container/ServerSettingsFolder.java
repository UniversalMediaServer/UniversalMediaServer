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

import java.io.File;
import java.io.IOException;
import net.pms.io.StreamGobbler;
import net.pms.renderers.Renderer;
import net.pms.store.ResumeObj;
import net.pms.store.item.VirtualVideoAction;
import net.pms.store.item.VirtualVideoActionLocalized;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;

/**
 * Server Settings folder. Used by manageRoot, so it is usually used as a folder
 * at the root folder. Child objects are created when this folder is created.
 */
public class ServerSettingsFolder extends LocalizedStoreContainer {

	public ServerSettingsFolder(Renderer renderer) {
		super(renderer, "ServerSettings");
	}

	public static ServerSettingsFolder getServerSettingsFolder(Renderer renderer) {
		ServerSettingsFolder res = new ServerSettingsFolder(renderer);
		VideoSettingsFolder vsf = VideoSettingsFolder.getVideoSettingsFolder(renderer);

		if (vsf != null) {
			res.addChild(vsf);
		}

		if (renderer.getUmsConfiguration().getScriptDir() != null) {
			final File scriptDir = new File(renderer.getUmsConfiguration().getScriptDir());

			if (scriptDir.exists()) {
				res.addChild(new LocalizedStoreContainer(renderer, "Scripts") {
					@Override
					public void discoverChildren() {
						File[] files = scriptDir.listFiles();
						if (files != null) {
							for (File file : files) {
								String childrenName = file.getName().replace("_", " ");
								int pos = childrenName.lastIndexOf('.');

								if (pos != -1) {
									childrenName = childrenName.substring(0, pos);
								}

								final File f = file;

								addChild(new VirtualVideoAction(renderer, childrenName, true, null) {
									@Override
									public boolean enable() {
										try {
											ProcessBuilder pb = new ProcessBuilder(f.getAbsolutePath());
											pb.redirectErrorStream(true);
											Process pid = pb.start();
											// consume the error and output process streams
											StreamGobbler.consume(pid.getInputStream());
											pid.waitFor();
										} catch (IOException e) {
											//continue
										} catch (InterruptedException e) {
											Thread.currentThread().interrupt();
										}

										return true;
									}
								});
							}
						}
					}
				});
			}
		}

		// Resume file management
		if (renderer.getUmsConfiguration().isResumeEnabled()) {
			res.addChild(new LocalizedStoreContainer(renderer, "ManageResumeFiles") {
				@Override
				public void discoverChildren() {
					final File[] files = ResumeObj.resumeFiles();
					addChild(new VirtualVideoActionLocalized(renderer, "DeleteAllFiles", true, null) {
						@Override
						public boolean enable() {
							for (File f : files) {
								f.delete();
							}
							getParent().getChildren().remove(this);
							return false;
						}
					});
					for (final File f : files) {
						String childrenName = FileUtil.getFileNameWithoutExtension(f.getName());
						childrenName = childrenName.replaceAll(ResumeObj.CLEAN_REG, "");
						addChild(new VirtualVideoAction(renderer, childrenName, false, null) {
							@Override
							public boolean enable() {
								f.delete();
								getParent().getChildren().remove(this);
								return false;
							}
						});
					}
				}
			});
		}

		// Restart UMS
		res.addChild(new VirtualVideoActionLocalized(renderer, "RestartUms", true, "images/store/action-restart.png") {
			@Override
			public boolean enable() {
				ProcessUtil.reboot();
				// Reboot failed if we get here
				return false;
			}
		});

		// Shut down computer
		res.addChild(new VirtualVideoActionLocalized(renderer, "ShutDownComputer", true, "images/store/action-shutdown.png") {
			@Override
			public boolean enable() {
				ProcessUtil.shutDownComputer();
				// Shutdown failed if we get here
				return false;
			}
		});

		return res;
	}

}
