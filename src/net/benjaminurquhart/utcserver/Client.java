package net.benjaminurquhart.utcserver;

import static net.benjaminurquhart.utcserver.GMHeader.*;
import static net.benjaminurquhart.utcserver.Util.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;


public class Client {
	
	private Socket socket;
	private GMDataInputStream input;
	private GMDataOutputStream output;
	
	private OutputStream underlyingStream;
	
	private int remainder, clientID;
	
	private byte[] current;
	private Queue<byte[]> outgoing;
	private Queue<ByteBuffer> buffers;
	
	private Player player;
	private BattleSOUL soul;
	
	private long nextHeartbeat;
	
	private volatile boolean setup, initializing, joined, closed;
	
	private final Object OUTPUT_LOCK = new Object();
	
	public Client(Socket socket) throws IOException {
		this.socket = socket;
		this.input = new GMDataInputStream(socket.getInputStream());
		this.output = new GMDataOutputStream(underlyingStream = socket.getOutputStream());
		
		this.outgoing = new ArrayDeque<>();
		this.buffers = new ArrayDeque<>();
		
		this.clientID = -1;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public GMDataInputStream getInputStream() {
		return input;
	}
	
	public GMDataOutputStream getOutputStream() {
		return output;
	}
	
	public boolean isActive() {
		return !closed && socket.isConnected() && !socket.isClosed();
	}
	
	public boolean isSetup() {
		return setup;
	}
	
	public boolean isInitializing() {
		return initializing;
	}
	
	public boolean hasJoined() {
		return joined;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	public int getID() {
		return clientID;
	}
	
	protected void markJoined() {
		joined = true;
	}
	
	public BattleSOUL getSoul() {
		return soul;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	protected void lockForSetup(int clientID) {
		if(setup || initializing) return;
		
		verify(clientID > 0 && clientID < 256, "Invalid client ID: " + clientID);
		this.clientID = clientID & 0xff;
		initializing = true;
	}
	
	protected synchronized void handshake() {
		if(setup) return;
		
		if(!initializing) {
			throw new IllegalStateException("Must call lockForSetup with a valid client ID before attempting to handshake");
		}
		
		try {
			output.writeUTF("GM:Studio-Connect");
			output.flush();
			
			long header = input.readLong();
			int unknown = input.readInt();
			
			// This is literally just an out of bounds read in the client.
			// It's sending 16 bytes but only writes 12, so we get a 
			// random* stack pointer. Good engine.
			// (Seems to always be 0x19f240 for my runtime.)
			int garbage = input.readInt(); 
			
			expect(header,  HANDSHAKE_CLIENT, "Invalid handshake header");
			expect(unknown, 0x10,             "Invalid unknown value");
			
			System.out.printf("Client %d garbage value is %d\n", clientID, garbage);
			//expect(garbage, 0x19f240,         "Invalid garbage value");
			
			output.writeLong(HANDSHAKE_RESPONSE);
			output.writeInt(0x0c);
			output.flush();
		}
		catch(Exception e) {
			this.close();
			throw new RuntimeException("Failed to setup client " + socket.getRemoteSocketAddress(), e);
		}
	}
	
	protected void finishSetup() {
		player = new Player();
		soul = new BattleSOUL();
		
		nextHeartbeat = System.currentTimeMillis() + 5000;
		initializing = false;
		setup = true;
	}
	
	public boolean hasNextBuffer() {
		return !buffers.isEmpty();
	}
	
	public ByteBuffer nextBuffer() {
		return buffers.poll();
	}
	
	public GMDataOutputStream startBuffer(UTCMessageType type) throws IOException {
		verify(this.isActive(), "Client socket is closed");
		
		GMDataOutputStream buffer = new GMDataOutputStream(new ByteArrayOutputStream());
		
		buffer.writeLong(DATA_BUFFER);
		//buffer.writeInt(0xC);
		buffer.writeInt(0xffffffff); // length
		buffer.write(type.ordinal());
		
		return buffer;
	}
	
	public void submitEmptyBuffer(UTCMessageType type) throws IOException {
		this.submitBuffer(this.startBuffer(type));
	}
	
	public void submitBuffer(GMDataOutputStream buff) throws IOException {
		if(!this.isActive()) {
			throw new IOException("Client socket is closed");
		}
		
		byte[] out = buff.getByteStream().toByteArray();
		
		verify(out.length > 12, "Gamemaker Studio 1 does not support empty buffers.", IllegalArgumentException.class);
		
		ByteBuffer tmp = ByteBuffer.wrap(out);
		tmp.order(ByteOrder.LITTLE_ENDIAN);
		tmp.putInt(8, out.length - 12);
		
		synchronized(OUTPUT_LOCK) {
			outgoing.add(out);
		}
		
		nextHeartbeat = System.currentTimeMillis() + 5000;
	}
	
	protected void submitRawPacket(byte[] bytes) {
		synchronized(OUTPUT_LOCK) {
			outgoing.add(bytes);
		}
	}
	
	public synchronized void tick() throws IOException {
		int available = input.available();
		if(current == null) {
			if(available >= 12) {
				long header = input.readLong();
				
				expect(header,  DATA_BUFFER, "Invalid header", InvalidPacketException.class);
				
				// Max packet buffer size is set to 65536 (see GMDataOutputStream), need 12 bytes for the packet header.
				int length = input.readInt();
				verify(length >= 0 && length < (1<<16) - 12, "Invalid buffer size: " + length, InvalidPacketException.class);
				current = new byte[length];
				remainder = length;
				available -= 12;
			}
			else {
				remainder = -1;
			}
		}
		if(available > 0) {
			if(remainder > 0) {
				int read = input.read(current, current.length - remainder, Math.min(remainder, available));
				remainder -= read;
				
				// shouldn't ever happen
				if(remainder < 0) {
					System.err.printf("Underflow of %d bytes while reading packet from client %d\n", -remainder, this.getID());
					remainder = 0;
				}
			}
			if(remainder == 0) {
				ByteBuffer wrapper = ByteBuffer.wrap(current);
				wrapper.order(ByteOrder.LITTLE_ENDIAN);
				buffers.add(wrapper);
				current = null;
				remainder = -1;
			}
		}
	}
	
	public void endTick() throws IOException {
		if(nextHeartbeat <= System.currentTimeMillis()) {
			this.submitEmptyBuffer(UTCMessageType.HEARTBEAT);
		}
	}
	
	public void dispatchBuffers() throws IOException {
		synchronized(OUTPUT_LOCK) {
			byte[] next = outgoing.poll();
			if(next != null) {
				while(next != null) {
					System.out.printf("Sending to client %d: %s %s\n", clientID, Util.parseUTCMessage(ByteBuffer.wrap(next), 12), Arrays.toString(next));
					underlyingStream.write(next);
					underlyingStream.flush();
					next = outgoing.poll();
				}
				nextHeartbeat = System.currentTimeMillis() + 5000;
			}
		}
	}
	
	public void close() {
		try {
			if(this.isActive()) {
				try {
					output.writeUTF("GM:BYE");
					output.flush();
				}
				catch(IOException e) {
					System.out.printf("Failed to send goodbye to client %d\n", clientID == -1 ? "<unknown>" : clientID);
				}
			}
			socket.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		System.out.printf("Client %s disconnected\n", clientID == -1 ? "<unknown>" : clientID);
		closed = true;
	}
}
