echo "Servidor e Cliente em compilação, aguarde."
javac -cp ".:json-java.jar" *.java
jar --create --file bin/Game.jar Game.class Game$ClientHandler.class Player.class Revolver.class
jar --create --file bin/Client.jar GameClient.class
