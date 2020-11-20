#!/usr/bin/bash
cd /usr/share/rserver/
# check if rserver is known in systemctl, if that is the case stop it, and remove it safely
if  systemctl list-unit-files | grep rserver
then
  systemctl stop rserver
  systemctl disable rserver
  rm -f /etc/systemd/system/rserver.service
  systemctl daemon-reload
  podman rm rserver
fi

/usr/share/rserver/start.bash
podman generate systemd --name rserver > /etc/systemd/system/rserver.service
podman stop rserver
systemctl daemon-reload
systemctl enable rserver
systemctl start rserver