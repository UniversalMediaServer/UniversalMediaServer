package net.pms.dlna;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class MediaMonitor extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);
    private final PmsConfiguration configuration;
	private final List<File> monitoredFolders;
	private final HashSet<String> oldEntries;

    public MediaMonitor(List<File> monitoredFolders) {
		super(Messages.getString("VirtualFolder.2"), "images/thumbnail-video-256.png");
        configuration = PMS.getConfiguration();
		this.monitoredFolders = monitoredFolders;
		oldEntries = new HashSet<>();
        parseMonitorFile();
	}

    @Override
    public boolean isRefreshNeeded() {
        return getLastModified() < FileUtil.getMaxLastModified(monitoredFolders) ||
               !LocalDate.now().isEqual(new LocalDate(getLastRefreshTime()));
    }

    @Override
    public void doRefreshChildren() {
        discoverChildren();
    }

    @Override
    public void discoverChildren() {
        List<File> newMedia = new ArrayList<>();

        for (File f : monitoredFolders) {
            newMedia.addAll(scanDir(f.listFiles()));
        }

        FileUtil.sort(newMedia);

        addNewMedia(newMedia);
        setLastModified(FileUtil.getMaxLastModified(monitoredFolders));
        notifyRefresh();
    }

    private List<File> scanDir(File[] files) {
        List<File> newMedia = new ArrayList<>();

        if(files == null)
            return newMedia;

		for (File f : files) {
			if (f.isFile()) {
				if (old(f.getAbsolutePath())) {
					continue;
				}

                newMedia.add(f);
			}
			if (f.isDirectory()) {
                newMedia.addAll(scanDir(f.listFiles()));
			}
		}

        return newMedia;
	}

    private void addNewMedia(List<File> newMedia) {
        Map<LocalDate, VirtualFolder> dateFolders = createDateFoldersMap();

        for (File f : newMedia) {
            LocalDate lastModifiedDate = new LocalDate(f.lastModified());

            VirtualFolder dateFolder = null;

            for (Map.Entry<LocalDate, VirtualFolder> entry : dateFolders.entrySet()) {
                LocalDate date = entry.getKey();
                if(date == null || date.isEqual(lastModifiedDate) || date.isBefore(lastModifiedDate)) {
                    dateFolder = entry.getValue();
                    break;
                }
            }

            if (dateFolder != null) {
                dateFolder.addChild(new RealFile(f));
            }
        }

        getChildren().clear();

        // add date folders to New Media folder
        for (VirtualFolder dateFolder : dateFolders.values()) {
            if(!configuration.isHideEmptyFolders() || dateFolder.getChildren().size() > 0) {
                addClearAll(dateFolder);
                addChild(dateFolder);
            }
        }

        // if only one date folder present, flatten into New Media folder
        if(getChildren().size() == 1) {
            DLNAResource onlyFolder = getChildren().get(0);
            getChildren().clear();
            for (DLNAResource child : onlyFolder.getChildren())
                addChild(child);
        }
    }

    private Map<LocalDate, VirtualFolder> createDateFoldersMap() {
        Map<LocalDate, VirtualFolder> dateFolders = new LinkedHashMap<>(); // LinkedHashMap iterates in insertion order

        dateFolders.put(LocalDate.now(), new VirtualFolder(Messages.getString("DateFolder.today"), "images/thumbnail-video-256.png"));
        dateFolders.put(LocalDate.now().minusWeeks(1), new VirtualFolder(Messages.getString("DateFolder.week"), "images/thumbnail-video-256.png"));
        dateFolders.put(LocalDate.now().minusMonths(1), new VirtualFolder(Messages.getString("DateFolder.month"), "images/thumbnail-video-256.png"));
        dateFolders.put(LocalDate.now().minusYears(1), new VirtualFolder(Messages.getString("DateFolder.year"), "images/thumbnail-video-256.png"));
        dateFolders.put(null, new VirtualFolder(Messages.getString("DateFolder.older"), "images/thumbnail-video-256.png"));

        return dateFolders;
    }

    private void addClearAll(final VirtualFolder folder) {
        folder.addChild(new VirtualVideoAction(Messages.getString("PMS.139"), true) {
            @Override
            public boolean enable() {
                for (DLNAResource res : folder.getChildren()) {
                    addToOld(res);
                }
                dumpFile();
                setDiscovered(false);
                return true;
            }
        });
    }

    public void stopped(DLNAResource res) {
        addToOld(res);
        dumpFile();
        setDiscovered(false);
	}

    private void addToOld(DLNAResource r) {
        if (!(r instanceof RealFile)) {
            return;
        }

        String path = ((RealFile) r).getFile().getAbsolutePath();
        if (old(path)) { // no duplicates!
            return;
        }

        oldEntries.add(path);
    }

	private boolean old(String str) {
		return oldEntries.contains(str);
	}

    private File monitorFile() {
        return new File(configuration.getDataFile("UMS.mon"));
    }

    private void parseMonitorFile() {
        File f = monitorFile();
        if (!f.exists()) {
            return;
        }
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                String str;

                while ((str = in.readLine()) != null) {
                    if (StringUtils.isEmpty(str)) {
                        continue;
                    }
                    str = str.trim();
                    if (str.startsWith("#")) {
                        continue;
                    }
                    if (str.startsWith("entry=")) {
                        String entry = str.substring(6);
                        if (!new File(entry.trim()).exists()) {
                            continue;
                        }
                        if (!oldEntries.contains(entry.trim())) {
                            oldEntries.add(entry.trim());
                        }
                    }
                }
            }
            dumpFile();
        } catch (Exception e) {
            LOGGER.error("Unable to parse in UMS.mon");
        }
    }

    private void dumpFile() {
        File f = monitorFile();
        Date now = new Date();
        try (FileWriter out = new FileWriter(f)) {
            StringBuilder sb = new StringBuilder();
            sb.append("######\n");
            sb.append("## NOTE!!!!!\n");
            sb.append("## This file is auto generated\n");
            sb.append("## Edit with EXTREME care\n");
            sb.append("## Generated: ");
            sb.append(now.toString());
            sb.append("\n");
            for (String str : oldEntries) {
                sb.append("entry=");
                sb.append(str);
                sb.append("\n");
            }
            out.write(sb.toString());
            out.flush();
        } catch (IOException e) {
            LOGGER.error("Unable to write out UMS.mon");
        }
    }
}
