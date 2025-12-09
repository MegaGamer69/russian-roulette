echo "Servidor e Cliente em compilação, aguarde."
javac -cp ".:json-java.jar" *.java
jar cfm bin/Server.jar ./MANIFEST_Server.txt Game.class 'Game$ClientHandler.class' Player.class Revolver.class
jar cfm bin/Client.jar ./MANIFEST_Client.txt GameClient.class 'GameClient$1.class' 'GameClient$UpdateManager.class'
