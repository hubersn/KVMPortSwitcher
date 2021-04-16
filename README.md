# KVMPortSwitcher
A CLI and UI tool to switch ports on your KVM switch via IP. It currently supports the [TESmart HDMI KVM switches](https://www.tesmart.com/) that feature an Ethernet connection.

KVMPortSwitcher is written in Java, so it needs a Java Runtime Environment (JRE, JDK...) to work. This makes it cross-platform in theory, but its hotkey feature in the UI part of the tool - courtesy of JNativeHook - relies on native code that only runs on (according to JNativeHook documentation) Windows (versions 2000 to 10, x86 and AMD64), Linux (X11 only - x86, AMD64, ARM7, Aarch64) and Apple OS X (AMD64). The CLI tool should work everywhere.

## License
The main code is licensed under "The Unlicense" to maximize your freedom. Basically, this license gives you the right to do what you want with the code. The only restrictions are because of Copyright law, i.e. you cannot claim that you have written the code yourself.

The global hotkey feature is provided via JNativeHook, which is used under LGPL. It is the release version 2.1.0, unchanged. See [JNativeHook GitHub Project](https://github.com/kwhat/jnativehook).

## Why?
There are three standard ways to control the input of your TESmart KVM switch: via keyboard shortcuts (but that only works when the keyboard is connected to the dedicated keyboard USB port, which is not compatible with all keyboards, and the shortcuts are "strange"), via IR (the remote control is that gizmo that you won't find when you need to switch the port, and access to ports 1 and 10-16 is a bit unfriendly) and via buttons (which is a bit strange - if the LED display is off, you need two presses, if not, you need one press, and access to ports 10-16 need two presses in any case). So all "standard" ways have their distinct disadvantages. Less so for the 8 port variant of course.

You might have noticed that LAN (Ethernet) port at the front of your KVM switch. This allows to control the KVM switch via IP. This also has restrictions (the IP address of the switch is hardcoded to 192.168.1.10, although apparently later models have this configurable), but if you can live with those, it allows specific software to be written to provide a better user experience. Which is what KVMPortSwitcher tries to do.

There is also a small utility available from TESmart called "HDMI Switch controller". It takes ages to connect to the switch, and has no keyboard shortcut control for port selection. It is still useful to configure your switch however.

## Usage
The CLI variant is in com.hubersn.kvmportswitcher.KVMPortSwitcher. You can either select a port or let it tell you the currently selected port. For use info, exec "com.hubersn.kvmportswitcher.KVMPortSwitcher -?"

The UI variant is in com.hubersn.kvmportswitcher.ui.swing.KVMPortSwitcherUI (and by default does not open a window with 16 buttons) and registers global keyboard shortcuts for selecting the ports. Port 1 to 12 are Ctrl+Fn, Ports 13 to 16 are Ctrl+3 to Ctrl+6. Yes, this is almost completely arbitrary. Change the code to change my choices :-) It also installs a tray icon which should be easily recognized because of its complete lack of colour. You can open the window at any time by using the "Open" item from they tray icon's popup menu.

The currently selected port is highlighted. There is no code to regularly update this e.g. via a background thread, but it is updated if you select the port via the tool (but you won't see it cause you switched away :-)). If you choose "Open" from the tray icon's popup menu, the currently selected port is updated from the KVM switch's state.

Exit the tool via the tray icon's popup menu "Exit" item. Closing the tool's window will NOT exit!

By supplying the "-show" parameter to KVMPortSwitcherUI, you can force the tool to initially open its window. I don't know why you would want that...

## Java compatibility
The main code is compatible with Java 7, but JNativeHook demands at least Java 8.

## The Code
One class for the Swing UI (KVMPortSwitcherUI), one class for the CLI (KVMPortSwitcher - which also has the "controller" inside that does the IP communication). The repo is a complete Eclipse project, but since it only has two dependencies - the Java runtime as well as the JNativeHook jar - it is very simple to develop and compile in the IDE of choice.

So the code should be simplicity itself, if there wasn't a strange effect encountered during usage - frequently, but not always. The symptom was that the native key listener received the same event over and over again. This seems to happen if you synchronously execute the switch to a different port inside the listener itself - maybe some kind of strange race condition between the OS and its key buffer, I have no idea. The solution is to execute the
IP communication in a separate thread and allow the native key listener code to finish quickly. Is this the best and/or only solution? Is it a bug in JNativeHook? I have not yet investigated thoroughly.

## History
This is the first release.

## Future development
If no KVM switch is found at the expected IP address and port, the tool does not behave well. Connection timeout, blocking the UI, general unpleasantness. Avoid such a situation.

Apart from that...things I will probably do because they are easy and even vaguely useful for my use case:
* configurable port button names
* i18n
* configurable keyboard shortcuts
* support other KVM switch config options like buzzer and LED timeout settings

Things I might be persuaded to do if someone is interested:
* configurable KVM switch models (8 port or 16 port)
* support control via serial
* support different IP-controllable KVM switches (if protocols are available)

___
All trademarks acknowledged - TESmart, Java, possibly others.
