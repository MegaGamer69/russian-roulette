# Java Russian Roulette (JRR)
A JDK 7.0 compatible multiplayer game made by me!

### Requirements
1. Java JRE 7.0 or above ([link](https://www.oracle.com/java/technologies/downloads/));
3. LAN/VPN server service (e.g.: Hamachi or Radmin VPN);

### How 2 Start Match?
Start your server on the LAN service (untested).
Its recommended that you and other players to use the same LAN server service.
If you receive a "Lost Connection" error, ensure that the server is started-up.
Start the LAN server port at 8080.

### Commands
You can run commands, that includes:

Pick the revolver if no one picked it:

```
/Pick
```

Fire with the revolver if you are with it:

```
/Fire <target username here>
```

To pass the revolver to other player:

```
/Pass <player username here>
```

To send a message on the chat that have no moderation (!):

```
/Send <type your textual message here>
```

To spin the revolver:

```
/Spin
```

To Cheat the next revolver bullet:

```
/Cheat
```

### How 2 Build From Source?
You can build from source by running simply:

```sh
javac ./*.java
```

Launch the server:

```sh
java Game
```

Clients connect via:
```sh
java GameClient
```

### Notes
If you are a streamer (like Tyroz, Felipe Neto or any other)
or content creator (like some Arena Breakout YouTubers, i love this game), send me honest feedbacks!
