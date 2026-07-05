#!/bin/bash
set -e

# Android Dev Environment Setup (no Android Studio)
# Detects host OS and sets up everything needed

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info() { echo -e "${GREEN}[+]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[x]${NC} $1"; }

OS="$(uname -s)"
ARCH="$(uname -m)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"

# ---------- 1. JDK ----------
install_jdk() {
  if java -version 2>&1 | grep -q 'version "17'; then
    info "JDK 17 already installed ($(java -version 2>&1 | head -1))"
    return
  fi
  info "Installing JDK 17..."
  case "$OS" in
    Linux)
      if command -v apt &>/dev/null; then
        sudo apt update -qq && sudo apt install -y -qq openjdk-17-jdk-headless
      elif command -v dnf &>/dev/null; then
        sudo dnf install -y java-17-openjdk-devel
      elif command -v pacman &>/dev/null; then
        sudo pacman -S --noconfirm jdk17-openjdk
      else
        err "Unknown package manager. Install JDK 17 manually."
        exit 1
      fi
      ;;
    Darwin)
      if ! command -v brew &>/dev/null; then
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
      fi
      brew install openjdk@17
      sudo ln -sf /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
      ;;
    *)
      err "Unsupported OS: $OS"; exit 1
      ;;
  esac
  info "JDK 17 installed"
}

# ---------- 2. Android SDK Command-Line Tools ----------
install_sdk() {
  if [ -f "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    info "Android SDK cmdline-tools already present"
    return
  fi
  info "Downloading Android SDK command-line tools..."
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  local url
  case "$OS" in
    Linux)  url="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" ;;
    Darwin) url="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip" ;;
  esac
  local tmp="$(mktemp -d)"
  cd "$tmp"
  wget -q "$url" -O cmdline-tools.zip
  unzip -q cmdline-tools.zip
  mv cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  rm -rf "$tmp"
  info "cmdline-tools installed at $ANDROID_SDK_ROOT/cmdline-tools/latest"
}

# ---------- 3. Accept Licenses & Install SDK Packages ----------
install_sdk_packages() {
  export JAVA_HOME
  export ANDROID_SDK_ROOT
  export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

  info "Accepting licenses..."
  yes | sdkmanager --licenses > /dev/null 2>&1

  info "Installing platform-tools, build-tools, platform..."
  sdkmanager \
    "platform-tools" \
    "build-tools;36.0.0" \
    "platforms;android-36" \
    2>&1 | tail -3
  info "SDK packages installed"
}

# ---------- 4. Install Emulator + System Image ----------
install_emulator() {
  export JAVA_HOME
  export ANDROID_SDK_ROOT
  export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

  local api="${1:-35}"
  local image="system-images;android-${api};google_apis;${ARCH}"

  info "Installing emulator..."
  sdkmanager "emulator" 2>&1 | tail -1

  # Use x86_64 image on x86_64 hosts, arm64-v8a on ARM hosts
  local arch_image
  case "$ARCH" in
    x86_64)  arch_image="system-images;android-${api};google_apis;x86_64" ;;
    aarch64|arm64) arch_image="system-images;android-${api};google_apis;arm64-v8a" ;;
    *) warn "Unknown arch $ARCH, trying x86_64"; arch_image="system-images;android-${api};google_apis;x86_64" ;;
  esac

  info "Installing $arch_image..."
  sdkmanager "$arch_image" 2>&1 | tail -1

  info "Creating AVD: Medium_Phone_API_${api}..."
  echo no | avdmanager create avd \
    -n "Medium_Phone_API_${api}" \
    -k "$arch_image" \
    -d "pixel_6" \
    -f 2>&1 | tail -3
}

# ---------- 5. Write local.properties ----------
write_properties() {
  local project_dir="${1:-.}"
  echo "sdk.dir=$ANDROID_SDK_ROOT" > "$project_dir/local.properties"
  info "Wrote $project_dir/local.properties"
}

# ---------- 6. Create a helper script for running ----------
write_helper() {
  mkdir -p "$ANDROID_SDK_ROOT"
  cat > "$ANDROID_SDK_ROOT/rafiq-env.sh" << 'HELPEOF'
# Source this file before building/running:
#   source ~/Android/Sdk/rafiq-env.sh

export JAVA_HOME
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
export PATH="$ANDROID_SDK_ROOT/platform-tools:$PATH"
export PATH="$ANDROID_SDK_ROOT/emulator:$PATH"

echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo "Java: $(java -version 2>&1 | head -1)"
echo "adb:  $(adb version 2>&1 | head -1)"
HELPEOF
  chmod +x "$ANDROID_SDK_ROOT/rafiq-env.sh"
  info "Helper script at $ANDROID_SDK_ROOT/rafiq-env.sh"
}

# ---------- MAIN ----------
main() {
  if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "Usage: $0 [--emulator [api-level]]"
    echo "  --emulator [api]  Also install emulator + system image (default API 35)"
    echo ""
    echo "Steps performed:"
    echo "  1. Install JDK 17"
    echo "  2. Download Android SDK command-line tools"
    echo "  3. Install platform-tools, build-tools, SDK platform"
    echo "  4. Optionally install emulator + create AVD"
    echo "  5. Write local.properties"
    echo "  6. Write env helper script"
    exit 0
  fi

  install_jdk
  install_sdk
  install_sdk_packages
  write_properties "$(pwd)"
  write_helper

  # Detect JAVA_HOME
  case "$OS" in
    Linux)  JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac)))) ;;
    Darwin) JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home") ;;
  esac
  export JAVA_HOME

  # Source helper to show status
  source "$ANDROID_SDK_ROOT/rafiq-env.sh"

  info ""
  info "===================================="
  info "Environment ready! Quick start:"
  info "  source $ANDROID_SDK_ROOT/rafiq-env.sh"
  info "  cd $(pwd) && ./gradlew assembleDebug"
  info "===================================="

  if [[ "$1" == "--emulator" ]]; then
    local api="${2:-35}"
    install_emulator "$api"
    info ""
    info "Emulator AVD 'Medium_Phone_API_${api}' created."
    info "Start it with:"
    info "  emulator -avd Medium_Phone_API_${api} -no-snapshot"
  fi
}

main "$@"
