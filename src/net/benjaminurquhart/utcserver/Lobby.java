package net.benjaminurquhart.utcserver;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Lobby extends Thread {
	
	private int id;
	private Client[] clients;
	private ExecutorService setupService;
	private final Object CLIENT_LOCK = new Object();
	
	public Lobby(int id, int size) {
		this.id = id;
		this.clients = new Client[size];
		this.setupService = Executors.newFixedThreadPool(Math.max(1, size / 4));
	}
	
	public int numClients() {
		synchronized(CLIENT_LOCK) {
			int out = 0;
			for(int i = 0; i < clients.length; i++) {
				if(clients[i] != null && clients[i].isActive()) {
					out++;
				}
			}
			return out;
		}
	}
	
	public List<Client> getClients() {
		synchronized(CLIENT_LOCK) {
			List<Client> out = new ArrayList<>();
			for(Client client : clients) {
				if(client != null && client.isActive() && client.isSetup()) {
					out.add(client);
				}
			}
			return out;
		}
	}
	
	public Client getClientByID(int id) {
		for(Client client : clients) {
			if(client != null && client.getID() == id) {
				return client;
			}
		}
		return null;
	}
	
	public int getID() {
		return id;
	}
	
	public boolean hasSpace() {
		return numClients() < clients.length; 
	}
	
	public boolean addClient(Socket socket) throws IOException {
		synchronized(CLIENT_LOCK) {
			for(int i = 0; i < clients.length; i++) {
				if(clients[i] == null || !clients[i].isActive()) {
					System.out.println("Added client to lobby " + id);
					clients[i] = new Client(socket);
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public void run() {
		long start, end, diff;
		Client client = null;
		int clientID;
		while(true) {
			try {
				start = System.currentTimeMillis();
				synchronized(CLIENT_LOCK) {
					for(int i = 0; i < clients.length; i++) {
						client = clients[i];
						clientID = i + 1;
						if(client != null) {
							if(!client.isActive()) {
								System.out.println("Lost connection to client " + clientID);
								if(client.hasJoined()) {
									broadcastDisconnect(this.getClients(), clientID);
								}
								clients[i] = null;
								client = null;
								continue;
							}
							if(!client.isSetup() && !client.isInitializing()) {
								final Client setupClient = client;
								client.lockForSetup(clientID);
								setupService.execute(() -> {
									setupClient.handshake();
									try {
										
										//buffer.writeInt(0xbe11c0de);
										//buffer.writeInt(0x2c);
										/*
										GMDataOutputStream buffer = new GMDataOutputStream(new ByteArrayOutputStream());
										
										buffer.writeLong(GMHeader.DATA_BUFFER);
										buffer.writeInt((1<<31));
										//buffer.writeLong(GMHeader.DATA_BUFFER);
										
										setupClient.submitRawPacket(buffer.getByteStream().toByteArray());
										setupClient.dispatchBuffers();*/
										
										
										
										List<Client> clients = this.getClients();
										GMDataOutputStream buffer = setupClient.startBuffer(UTCMessageType.CONFIGURATION);
										buffer.write(setupClient.getID());
										buffer.write(clients.size());
										for(Client c : clients) {
											// list does not contain this client since it's not set up yet
											buffer.write(c.getID());
										}
										
										// TODO: Shared stat configuration
										buffer.write(0); // LV
										buffer.write(0); // G
										buffer.write(0); // KILLS
										setupClient.submitBuffer(buffer);
										
										Player player;
										BattleSOUL soul;
										for(Client c : clients) {
											player = c.getPlayer();
											if(player != null) {
												buffer = setupClient.startBuffer(UTCMessageType.PLAYER_UPDATE);
												buffer.write(c.getID());
												player.serialize(buffer);
												setupClient.submitBuffer(buffer);
											}
											soul = c.getSoul();
											if(soul != null) {
												buffer = setupClient.startBuffer(UTCMessageType.BATTLE_UPDATE);
												buffer.write(c.getID());
												soul.serialize(buffer);
												setupClient.submitBuffer(buffer);
											}
										}
										
										setupClient.finishSetup();
									}
									catch(Exception e) {
										e.printStackTrace();
										setupClient.close();
									}
								});
							}
							else if(client.isSetup()) {
								if(!client.hasJoined()) {
									broadcastJoin(this.getClients(), client.getID());
									client.markJoined();
								}
								try {
									client.tick();
									if(client.isActive()) {
										while(client.hasNextBuffer()) {
											processBuffer(clientID, client.nextBuffer());
										}
									}
									client.endTick();
								}
								catch(Exception e) {
									e.printStackTrace();
									client.close();
								}
							}
						}
					}
					for(Client c : this.getClients()) {
						try {
							c.dispatchBuffers();
						}
						catch(Exception e) {
							e.printStackTrace();
							c.close();
						}
					}
				}
				end = System.currentTimeMillis();
				diff = end - start;
				if(diff < 10) {
					Thread.sleep(10 - diff);
				}
			}
			catch(Exception e) {
				System.err.printf("Lobby %d encountered an unexpected error\n", id);
				e.printStackTrace();
				
				// Lobby is in unknown state, it's better to just kick everyone out
				// and reset everything.
				for(int i = 0; i < clients.length; i++) {
					client = clients[i];
					if(client != null && client.isActive()) {
						client.close();
					}
					clients[i] = null;
				}
			}
		}
	}
	
	private void broadcastJoin(List<Client> clients, int newID) {
		GMDataOutputStream buffer;
		System.out.println("Sending joins from client " + newID);
		for(Client client : clients) {
			if(client.getID() == newID) continue;
			try {
				System.out.println("Sending join from client " + newID + " to client " + client.getID());
				buffer = client.startBuffer(UTCMessageType.PLAYER_JOIN);
				buffer.write(newID);
				client.submitBuffer(buffer);
			}
			catch(Exception e) {
				e.printStackTrace();
				client.close();
			}
		}
		System.out.println("Done sending joins");
	}
	
	private void broadcastDisconnect(List<Client> clients, int deadID) {
		GMDataOutputStream buffer;
		System.out.println("Sending disconnects from client " + deadID);
		for(Client client : clients) {
			if(client.getID() == deadID) continue;
			try {
				System.out.println("Sending disconnect from client " + deadID + " to client " + client.getID());
				buffer = client.startBuffer(UTCMessageType.PLAYER_LEAVE);
				buffer.write(deadID);
				client.submitBuffer(buffer);
			}
			catch(Exception e) {
				e.printStackTrace();
				client.close();
			}
		}
		System.out.println("Done sending disconnects");
	}
	
	private void processBuffer(int clientID, ByteBuffer buff) throws IOException {
		//System.out.printf("Client %d: %s\n", clientID, Util.parseUTCMessage(buff));
		
		int typeIndex = buff.get() & 0xff;
		UTCMessageType[] types = UTCMessageType.values();
		
		if(types.length <= typeIndex) {
			InvalidPacketException.throwFormatted("Client %d sent invalid packet type %d", clientID, typeIndex);
		}
		
		GMDataOutputStream stream;
		UTCMessageType type = types[typeIndex];
		switch(type) {
		
		case PLAYER_UPDATE: {
			int updatedClient = buff.get() & 0xff;
			if(updatedClient != clientID) {
				InvalidPacketException.throwFormatted("Client %d sent update packet for different client %d", clientID, updatedClient);
			}
			Player player = this.getClientByID(clientID).getPlayer();
			player.updateFromBuffer(buff);
			
			if(player.updated()) {
				Client client;
				for(int i = 0; i < clients.length; i++) {
					client = clients[i];
					if(client != null && client.isActive() && client.isSetup() && client.getID() != clientID) {
						stream = client.startBuffer(type);
						stream.write(clientID);
						player.serialize(stream);
						client.submitBuffer(stream);
					}
				}
			}
			
		} break;
		case BATTLE_UPDATE: {
			int updatedClient = buff.get() & 0xff;
			if(updatedClient != clientID) {
				InvalidPacketException.throwFormatted("Client %d sent update packet for different client %d", clientID, updatedClient);
			}
			BattleSOUL soul = this.getClientByID(clientID).getSoul();
			soul.updateFromBuffer(buff);
			
			if(soul.updated()) {
				Client client;
				for(int i = 0; i < clients.length; i++) {
					client = clients[i];
					if(client != null && client.isActive() && client.isSetup() && client.getID() != clientID) {
						stream = client.startBuffer(type);
						stream.write(clientID);
						soul.serialize(stream);
						client.submitBuffer(stream);
					}
				}
			}
			
		} break;
		
		// TODO
		case FLAGS_UPDATE: break;
		
		default: InvalidPacketException.throwFormatted("Client %d sent illegal packet of type %d", clientID, type);
		}
	}
}
