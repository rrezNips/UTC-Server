package net.benjaminurquhart.utcserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main {
	
	public static final List<Lobby> LOBBIES = new ArrayList<>();
	
	public static void main(String[] args) throws Exception {
		SpriteTable.init();
		
		ServerSocket server = new ServerSocket(6666);
		
		int nextLobby = 0;
		
		Lobby l;
		boolean added = false;
		try {
			while(true) {
				Socket socket = server.accept();
				
				System.out.println("Received connection from " + socket.getRemoteSocketAddress());
				
				added = false;
				for(Lobby lobby : LOBBIES) {
					if(lobby.addClient(socket)) {
						added = true;
						break;
					}
				}
				
				if(!added) {
					l = new Lobby(nextLobby++, 8);
					LOBBIES.add(l);
					l.start();
					
					// yeah idk
					if(!l.addClient(socket)) {
						socket.close();
					}
				}
			}
		}
		finally {
			server.close();
		}
	}
}
