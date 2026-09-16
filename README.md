# מודעות (adsSwitch)

One screen on the phone: run the **local** Google Ads set (35 km around Be'er Sheva
and Midreshet Ben-Gurion), the **nationwide** set, or **turn everything off** — one tap
switches all campaigns together.

- **The work is the daemon's**: `d adsArea [--mode local|nationwide|off] [--json 1]`
  (`/opt/automateLinux/utilities/adsArea.py`). The nationwide ↔ local pairing is
  `/opt/automateLinux/data/ads-area-pairs.json` on the desktop, which is why the
  backend runs on the **desktop**.
- **Backend**: Next.js on `10.7.0.2:3155` (dev), `GET/POST /api/area`, bearer-token
  guarded (`ADS_SWITCH_API_TOKEN` in `.env.local`). It turns paid advertising on and
  off and listens on the LAN too, so it refuses every call without the token.
- **Phone**: `mobile/` (KMP), package `com.automatelinux.adsSwitch.dev`, launcher name
  **מודעות**. Base URL and token are baked in from the gitignored `mobile/.env`
  (`API_BASE_URL`, `API_TOKEN`). The phone reaches the backend directly over
  WireGuard, never through nginx.
