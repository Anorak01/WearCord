# WearCord
A simple custom discord client from scratch made for WearOS. (Tested with Ticwatch E3 - WearOS 3.5)

The project in its current state should be considered to be a "spike", me testing stuff out, 
as it is literally my first Android app and Kotlin experience.
It is full of dead code, bad practices, *very* suboptimal networking, the whole lot. The UI is.. not good, but usable.
Definitely don't use it to learn.

If we forget about that, its pretty usable for just quickly checking messages in your dms and guilds.

### Supports:
- Displaying dms, guilds (their channels) and messages inside channels.
- Messages are displayed as they are, no gif/emoji support, no replies, only text and images are supported.


### Plans:
- I plan to eventually get most of messages - gif/emoji support, proper replies, links(not sure about those yet).
- Fix to guild channel sorting will come as well.
- Improvements to the UI.


### Mentions:
Project makes use of [DiscordQrAuthLib](https://github.com/0x3C50/DiscordQrAuthLib) for its QR code auth


## Disclaimer:
As usual.. this is a custom Discord client, Discord prohibits custom clients in their TOS, I don't know anyone
whose gotten banned yet, yada yada yada...

Do note that I have not taken any special care in making sure my http queries mimic the official client properly,
use this app at your own risk.