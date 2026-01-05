#!/usr/bin/env bash
set -e

sudo apt update
sudo apt install -y \
  xvfb \
  x11vnc \
  openbox \
  novnc \
  websockify \
  xterm \
  dbus-x11 \
  firefox

# Start virtual display
Xvfb :0 -screen 0 1280x800x16 &
export DISPLAY=:0
sleep 2

# Start window manager
openbox-session &

# Start VNC server
x11vnc -display :0 -nopw -forever -shared -rfbport 5900 &

# Start noVNC
websockify --web=/usr/share/novnc/ 6080 localhost:5900
