#!/usr/bin/env bash
set -e

echo "Fixing Yarn GPG key (if needed)..."

# Ensure keyrings directory exists
sudo mkdir -p /etc/apt/keyrings

# Install Yarn GPG key
curl -fsSL https://dl.yarnpkg.com/debian/pubkey.gpg | \
  gpg --dearmor | sudo tee /etc/apt/keyrings/yarn.gpg > /dev/null

sudo chmod 644 /etc/apt/keyrings/yarn.gpg

# Ensure correct Yarn repo entry
echo "deb [signed-by=/etc/apt/keyrings/yarn.gpg] https://dl.yarnpkg.com/debian stable main" | \
  sudo tee /etc/apt/sources.list.d/yarn.list > /dev/null

echo "Running apt update..."
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
