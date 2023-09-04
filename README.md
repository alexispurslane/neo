<div align="center">
  <img src="https://github.com/alexispurslane/bloc/blob/develop/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png?raw=true" width="128" height="128" style="display: block; margin: 0 auto"/>
  <h1>Bloc</h1>
  <p>The Revolt Client for Revolutionaries</p>
  <a href="https://github.com/alexispurslane/bloc/actions/workflows/android-ci.yml"><img src="https://github.com/alexispurslane/bloc/actions/workflows/android-ci.yml/badge.svg?branch=main"/></a>
</div>

---

<p align="center">
  
<img src="https://github.com/alexispurslane/bloc/assets/1920151/7796940e-9007-449a-9a9d-63f301ea85e8" width="24%"/>
<img src="https://github.com/alexispurslane/bloc/assets/1920151/bca4b906-51ce-4210-9f2e-c80e62910aa8" width="24%"/>
<img src="https://github.com/alexispurslane/bloc/assets/1920151/dcda0fe0-dc8a-4ce9-985e-a966fa3c9b78" width="24%"/>
<img src="https://github.com/alexispurslane/bloc/assets/1920151/56feaed3-2be2-40af-a28d-a56f9f310913" width="24%"/>

</p>

Bloc is a fully Android-native client for the [Revolt](https://revolt.chat) chat service that treats self-hosted instances as first-class citizens. It aims to be the client of choice for those who want the *finer* things in life, through focusing on a few key features.

## Features

- 🚀 **Built with the most modern development tools, frameworks, and architectural best-practices.** Not bleeding-edge, but cutting-edge. Bloc is written in [Kotlin](https://kotlinlang.org/) and [Jetpack Compose](https://developer.android.com/jetpack/compose) with a constant and conscious focus on code quality, clarity, maintainability, and performance, because when something looks good on the inside, it works better on the outside too.
- 🔍 **A clear, clean, consistent, and minimalist Material You-based design.** Free/Libre and Open Source Android applications don't have to be ugly, and Bloc is out to prove that.
- ✨ **A gesture-first interface that makes you feel like you're in Minority Report.** Bloc's interface is built around gestures first and foremost, with every single one carefully tuned for momentum and repeatability, so that interacting with it feels like communicating with the app telepathically.
- 🔐 **A privacy and independence conscious design.** Not only does Bloc treat self-hosted Revolt servers as first-class citizens, it also treats users of deGoogled Android (such as those on LineageOS, DivestOS, CalyxOS, or GrapheneOS) and other privacy-conscious Android users as first-class citizens as well, by using a persistant foreground service to recieve notifications instantaneously over WebSockets at a minor battery cost instead of receiving notifications through Google's Firebase Cloud Messaging Platform.[^1]

## Installation

Eventually Bloc will be released on F-Droid. For now, once there is a public Alpha available, you should be able to get it in the "releases" sidebar on GitHub.


[<img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png"
    alt="Get it on GitHub"
    height="80">](https://github.com/alexispurslane/bloc/releases/latest)

## Roadmap

### Done (as of 1.0-alpha.1)

- [X] Log in to any Revolt server, including two factor authentication and MFA recovery codes
- [X] Get list of servers you're a member of, with full server icons, banners, and names
- [X] List of channels (uncategorized and by-category) in each server including channel icons
- [X] Visit channels and see old messages (including seamless scrollback in the timeline) and recieve new ones immediately
- [X] Send messages back (and see own messages, of course)
- [X] Render all Markdown, and render user mentions as clickable links that open their profile
- [X] Viewable profiles (including own) including avatar, dispaly name, username, discriminator, user banner, status, and bio
- [X] Remember last server/channel, and the last used channel for each server and automatically open them when the app/server is opened
- [X] Respect user server-specific names, roles, and icons, as well as masquerades
- [X] Display image attachments (collapsable, automatically collapsed for >2 attachments)
- [X] Functional push notifications that work even when the app is fully closed, showing all relevant information and grouped by channel in your notifications tray. (This is something RVMob doesn't have yet!)
- [X] Fully smooth and jank-free back history scrolling and new message scrolling (also something RVMob doesn't have!)
- [X] Make you able to tap on someone's PFP to see their profile in a slide up card

### To-Do

- [ ] Make the "jump to bottom" banner
- [ ] Make the notification listener service work before the first time you open the app (it works after that even if you close the app but if you reboot it won't work again till you open it)
- [ ] Implement displaying custom emoji
- [ ] Direct messages
- [ ] Implement seeing who is on a server
- [ ] Allow sending custom emoji
- [ ] Display and allow replies
- [ ] Allow users to @mention others
- [ ] Display/allow emoji message reactions
- [ ] "User is typing" alerts
- [ ] Read receipts
- [ ] Compact (IRC) mode
- [ ] Add ability to change font size and theme
- [ ] Add ability to add attachments to messages
- [ ] Act on server/channel/user update events (for instance, when user statuses change)
- [ ] Allow user blocking/friending/unfriending/dealing with requests and message reporting

## Acknowledgements

- In-app vectors and icons by <a href="https://www.figma.com/community/file/1166831539721848736?ref=svgrepo.com" target="_blank">Solar Icons</a> in CC Attribution License via <a href="https://www.svgrepo.com/" target="_blank">SVG Repo</a>
- <a href="https://www.flaticon.com/free-icons/bandana" title="bandana icons">Bandana icon created by Freepik - Flaticon</a>

[^1]: Firebase Cloud Messaging (FCM) is what most applications on Android use. With FCM, app developers must register the server they want to produce push notifications with Google's cloud infrastructure, and then put a manifest file in their application indicating they want to recieve notifications from that server. Then Google Play Services (a piece of closed-source data-hoovering spyware) takes the list of all the applications that want push notifications and all the servers they want them from, and listens to Google's central Firebase Cloud Messaging servers, which use the configuration the app developer created on Google's cloud platform to relay push notifications from the various sources they originate from to Google Play Services, which then distributes them to the relevant apps. This of course creates privacy and centralization/monopoly concerns, since it requires all apps that want push notifications to use Google Play Services (thus locking Android users into using a version of Android made by Google) and routes all notifications through Google's services. On the other hand, Bloc's architecture bypasses that at a very, very small battery cost (["consumes about 0-1% of battery in 17h of use"](https://docs.ntfy.sh/faq/?h=battery) according to the creators of the foreground service algorithm I use). Bloc's architecture also allows recieveing push notifications from arbitrary self-hosted Revolt servers with no further setup, whereas getting traditional FCM push notifications to work would require each owner of a self-hosted Revolt server to individually connect their server to the FCM servers and then release a custom build of the app.
