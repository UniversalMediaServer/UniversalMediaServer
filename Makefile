#!/usr/bin/make
# make && sudo make install
VERSION=9.3.1-SNAPSHOT
PROGRAMDIR=/opt/ums
PREFIX=/usr/local
BINDIR=$(PREFIX)/bin
SYSTEMDDIR=$(PREFIX)/lib/systemd/system
# may be /usr/lib/systemd/system or /etc/systemd/system

build:
	mvn external:install
	mvn package

clean:
	mvn clean

install:
	# TODO create a maven profile "small" without executables
	mkdir -p $(DESTDIR)$(PROGRAMDIR)
	tar -xf target/ums-$(VERSION)-distribution.tar.gz --directory=$(PROGRAMDIR) --strip-components 1
	mkdir -p  $(DESTDIR)$(BINDIR)
	ln -s $(DESTDIR)$(PROGRAMDIR)/UMS.sh $(DESTDIR)$(BINDIR)/ums
	mkdir -p $(DESTDIR)$(SYSTEMDDIR)
	cp ums.service $(DESTDIR)$(SYSTEMDDIR)
	#systemctl enable ums
	#systemctl start ums

.PHONY: clean

uninstall:
	rm -f $(DESTDIR)$(SYSTEMDDIR)/ums.service
	rm -f $(DESTDIR)$(BINDIR)/ums
	rm -rf $(DESTDIR)$(PROGRAMDIR)

