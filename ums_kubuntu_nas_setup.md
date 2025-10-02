> ## ⚡️ AI-Assisted Solution Notice
> This guide was created through **AI + human collaboration**, tested and verified in a real-world setup. It is not part of the official UMS documentation but may help others facing the same issue.

# Universal Media Server on Kubuntu with NAS Shares (AI/Assistant Guided Solution)

## Problem

When running **Universal Media Server (UMS)** on Linux (tested on Kubuntu 24.04), with media stored on a **NAS share mounted via CIFS**, shared folders configured through the web interface are saved into:

- `/var/lib/ums/config/SHARED.conf` (JSON)
- `/var/lib/ums/config/UMS.conf` (INI-style, with `shared_folders = ...`)

However:

- After reboot, **SHARED.conf persists correctly** (it still lists all shared folders).  
- But UMS **ignores SHARED.conf** on startup and only reads `UMS.conf`.  
- This causes network shares (e.g., `/mnt/nas_backup`) to **disappear** from the web UI and TV clients until re-added manually.

## Solution

We created a pair of **systemd units and shell scripts** that ensure UMS always restores the correct shared folder list from `SHARED.conf` into `UMS.conf` at boot.

### Key points

- `ums-save.service` saves a snapshot of current shared folders before shutdown (optional).  
- `ums-restore.service` parses `SHARED.conf` on boot and rewrites the `shared_folders = ...` line in `UMS.conf`.  
- UMS is restarted automatically so the updated config takes effect.  
- Works reliably even with NAS shares mounted via `/etc/fstab`.

---

## Scripts

Place these in `/usr/local/bin/` and make them executable (`chmod +x`).

### `/usr/local/bin/ums-save-shares.sh`

```bash
#!/bin/bash
# Save current shared folders (optional safety snapshot)

STAMP=$(date +%Y%m%d%H%M%S)
SRC="/var/lib/ums/config/SHARED.conf"
DEST="/var/lib/ums/shared_folders-${STAMP}.json"

if [ -f "$SRC" ]; then
    cp "$SRC" "$DEST"
    echo "$(date) - Saved $SRC to $DEST" >> /var/log/ums/ums-save-${STAMP}.log
fi
```

### `/usr/local/bin/ums-restore-shares.sh`

```bash
#!/bin/bash
# Restore shared folders from SHARED.conf into UMS.conf

CONF="/var/lib/ums/config/UMS.conf"
SHARED="/var/lib/ums/config/SHARED.conf"
STAMP=$(date +%Y%m%d%H%M%S)
LOG="/var/log/ums/ums-restore-${STAMP}.log"

if [ -f "$SHARED" ]; then
    FOLDERS=$(grep -oP '"file":\s*"\K[^"]+' "$SHARED" | paste -sd,)
    if [ -n "$FOLDERS" ]; then
        sed -i '/^shared_folders/d' "$CONF"
        echo "" >> "$CONF"
        echo "shared_folders = $FOLDERS" >> "$CONF"
        echo "$(date) - Restored shared_folders from $SHARED into $CONF" >> "$LOG"
        echo "Content: $FOLDERS" >> "$LOG"
        systemctl restart ums
        echo "$(date) - UMS restarted" >> "$LOG"
    fi
fi
```

---

## Systemd Units

### `/etc/systemd/system/ums-save.service`

```ini
[Unit]
Description=Save UMS shared folders before shutdown
DefaultDependencies=no
Before=shutdown.target reboot.target halt.target

[Service]
Type=oneshot
ExecStart=/bin/true
ExecStop=/usr/local/bin/ums-save-shares.sh
RemainAfterExit=yes

[Install]
WantedBy=halt.target reboot.target shutdown.target
```

Enable with:

```bash
systemctl enable ums-save.service
```

---

### `/etc/systemd/system/ums-restore.service`

```ini
[Unit]
Description=Restore UMS shared folders at boot
After=local-fs.target network-online.target
Wants=network-online.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/ums-restore-shares.sh

[Install]
WantedBy=multi-user.target
```

Enable with:

```bash
systemctl enable ums-restore.service
```

---

## Verification

After reboot:

1. Check `/var/lib/ums/config/UMS.conf` → it should contain the latest `shared_folders = ...` line.  
2. `systemctl status ums` → UMS restarts cleanly.  
3. Web UI (`http://<server-ip>:9001`) and TV clients see **all shared folders**, including NAS paths.  

---

## Tested Environment

- OS: Kubuntu 24.04 (fresh install)
- Hardware: Intel i7-4700 system (Sabertooth)
- UMS: Version 13.10.0 (Linux x86_64 build)
- Java: OpenJDK 17
- Storage: NAS mounted via CIFS (`/mnt/nas_backup`) with movies/music folders

---

## Notes

- Tested on Kubuntu 24.04, UMS 13.10.0.  
- Handles both local folders and CIFS-mounted NAS shares.  
- Minimal overhead — restore runs once per boot.  
- If NAS mounts are slow, adjust `After=` and `Wants=` dependencies in `ums-restore.service`.

---

## Credits

This solution was developed in collaboration with an **AI assistant** and refined through live debugging and testing. It is shared here to help other users facing the same UMS reboot issue with NAS-mounted shares.

✅ This workaround ensures **UMS shared folders survive reboots** reliably, even when NAS mounts are involved.

