# BITS Wi-Fi Login

[<img src="demo/screenshot.png" width="25%" height="25%">](https://play.google.com/store/apps/details?id=dev.sparshg.bitslogin) [<img src="demo/icon.png" width="50%" height="50%">](https://play.google.com/store/apps/details?id=dev.sparshg.bitslogin)

App that allows you to login to BITS (Pilani, Goa, Hyderabad) campus Wi-Fi  without requiring you to sign-in manually.

> By a student who got fed up connecting to the wi-fi login portal everytime.

[<img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height=60px />](https://play.google.com/store/apps/details?id=dev.sparshg.bitslogin)

## How it works

- **Pilani (FortiGate):** the app asks `connectivitycheck.gstatic.com` over Wi-Fi. If the portal answers instead, it
  opens the login page (which carries a one-time token), signs in over HTTPS to `fw.bits-pilani.ac.in` with its
  certificate verified, and confirms you're online.
- **Goa (Sophos):** signs in through `campnet.bits-goa.ac.in`, the same way the campus web page does.
- **Hyderabad (Sophos):** signs in through `172.16.0.30`. That portal only accepts logins over plain HTTP (like its
  own page), so the app only sends it when the network redirects to that portal.
- **When:** as soon as Wi-Fi connects, when Android flags a captive portal, and when the screen turns on; otherwise
  once a minute while the screen is on and every 15 minutes while it's off.
- **Keepalive:** BITS Pilani sessions end 12 hours after login. With *Session Keepalive* on (the default), the app
  refreshes the session every 4 hours like the portal's own keepalive page does; it can be turned off in the app.
- **Safety:** if the portal turns the password down twice, the app stops and tells you (FortiGate locks accounts
  after 3 failures); it resumes when you save the credentials again.

## Troubleshooting

- **VPN** (e.g. Cloudflare WARP / 1.1.1.1): exclude this app from the VPN, or pause it while logging in.
- **Private DNS:** set it to *Automatic*. A custom provider can't resolve the campus firewall.
- **"Wi-Fi login rejected":** fix your username or password in the app.
