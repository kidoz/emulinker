# Arch Linux Package

This directory contains files for building an Arch Linux package for EmuLinker.

## Building from local source

For development, modify the PKGBUILD to use the local source:

```bash
# Edit PKGBUILD and change source to:
source=("git+file:///path/to/emulinker")
sha256sums=('SKIP')

# Build the package
makepkg -si
```

## Building from release

The default PKGBUILD fetches source from GitHub releases:

```bash
makepkg -si
```

## Package contents

After installation:

| Path | Description |
|------|-------------|
| `/usr/bin/emulinker` | Wrapper script |
| `/usr/share/java/emulinker/emulinker.jar` | Application JAR |
| `/etc/emulinker/` | Configuration files |
| `/usr/lib/systemd/system/emulinker.service` | Systemd service |
| `/var/lib/emulinker/` | Working directory (logs) |

## Managing the service

```bash
# Start
sudo systemctl start emulinker

# Enable at boot
sudo systemctl enable emulinker

# View logs
journalctl -u emulinker -f

# Check status
systemctl status emulinker
```

## Configuration

Edit `/etc/emulinker/application.properties` to configure:

- Server name and location
- Network ports
- User limits
- Admin credentials

Changes require a service restart:

```bash
sudo systemctl restart emulinker
```

## Firewall

Open the required UDP ports:

```bash
sudo firewall-cmd --permanent --add-port=27888/udp
sudo firewall-cmd --permanent --add-port=27889-27899/udp
sudo firewall-cmd --reload
```

Or with ufw:

```bash
sudo ufw allow 27888/udp
sudo ufw allow 27889:27899/udp
```
