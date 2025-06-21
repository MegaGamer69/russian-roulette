# Java Russian Roulette (JRR)
A multiplayer game compatible with JDK 7.0 made by me!

### Requirements
1. Java JRE 7.0 or above ([link](https://www.oracle.com/java/technologies/downloads/));
2. LAN/VPN server service (e.g.: Hamachi or Radmin VPN);

### How 2 Start A Match?
Start your server on the LAN service (untested).
It's recommended that you and other players to use the same LAN server service.
If you receive a "Lost Connection" error, make sure that the server is started-up.
Start the LAN server port at 8080.

### Commands
You can run commands, that includes:

Pick the revolver (if no one picket it yet):

```
/Pick
```

Fire with the revolver (if you are with it):

```
/Fire <target username here>
```

To pass the revolver to other player (you need to have the revolver!!!):

```
/Pass <player username here>
```

To send a unmoderated message on the chat:

```
/Send <type your textual message here>
```

To spin the revolver (you need to have the revolver):

```
/Spin
```

To Cheat the next revolver bullet (the next one after the current), and again, you need to have the revolver:

```
/Cheat
```

To drop the revolver (YOU NEED TO HAVE THE REVOLVER):

```
/Drop
```

### How 2 Build From Source?
You can build from source by running simply:

```sh
javac ./*.java
```

Or run this unique bash script (i forgot to create the Batch File version):

```
./build.sh
```

Launch the server:

```sh
java -jar Game.jar
```

To connect as a client:
```sh
java -jar GameClient.jar
```

### License
You can only see the repository, no mods or forks.

### Notes
If you're a streamer (like Tyroz, Felipe Neto or any other)
or content creator (like some Arena Breakout YouTubers — I love this game), fell free to send me honest feedback!
