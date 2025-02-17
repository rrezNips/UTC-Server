package net.benjaminurquhart.utcserver;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class Util {

	public static String byteArrayToHex(byte[] bytes) {
		return byteArrayToHex(bytes, 0);
	}
	
	public static String byteArrayToHex(byte[] bytes, int offset) {
		StringBuilder sb = new StringBuilder();
		for(byte b : bytes) {
			sb.append(String.format("%02x ", b));
		}
		return sb.toString();
	}
	
	public static String parseUTCMessage(ByteBuffer buff) {
		return parseUTCMessage(buff, 0);
	}
	
	public static String parseUTCMessage(ByteBuffer buff, int offset) {
		StringBuilder sb = new StringBuilder();
		buff.order(ByteOrder.LITTLE_ENDIAN);
		buff.position(0);
		
		Map<String, Object> values = new LinkedHashMap<>();
		if(buff.limit() > offset) {
			try {
				buff.position(offset);
				
				UTCMessageType[] types = UTCMessageType.values();
				int typeIndex = buff.get() & 0xff;
				
				// guaranteed to be unsigned
				if(typeIndex >= types.length) {
					sb.append("INVALID_");
					sb.append(typeIndex);
					sb.append(" ");
				}
				else {
					UTCMessageType type = types[typeIndex];
					sb.append(type);
					sb.append(" ");
					
					if(type != UTCMessageType.FLAGS_UPDATE && type != UTCMessageType.HEARTBEAT) {
						values.put("client", buff.get() & 0xff);
					}
					
					switch(type) {

					case CONFIGURATION: {
						int[] clients = new int[buff.get() & 0xff];
						for(int i = 0; i < clients.length; i++) {
							clients[i] = buff.get() & 0xff;
						}
						values.put("others", Arrays.toString(clients));
						
						boolean[] config = {
								buff.get() != 0,
								buff.get() != 0,
								buff.get() != 0
						};
						values.put("config", Arrays.toString(config));
					} break;

					case PLAYER_UPDATE: {
						values.put("name", readGMString(buff));
						values.put("lv", buff.get() & 0xff);
						values.put("hp", buff.get() & 0xff);
						values.put("maxhp", buff.get() & 0xff);
						values.put("km", buff.get() & 0xff);
						values.put("x", buff.getShort());
						values.put("y", buff.getShort());
						
						int spriteIndex = buff.getShort() & 0xffff;
						SpriteEntry sprite = SpriteTable.get(spriteIndex);
						values.put("sprite_index", String.format("%d (%s)", spriteIndex, sprite == null ? "<invalid>" : sprite.name()));
						
						values.put("image_index", buff.get() & 0xff);
						values.put("image_alpha", buff.getFloat());
						values.put("room", buff.getShort() & 0xffff);
						values.put("state", buff.get() & 0xff);
						values.put("draw", buff.get() & 0xff);
					} break;
					case PLAYER_JOIN:
						break;
					case PLAYER_LEAVE:
						break;
					case BATTLE_UPDATE: {
						values.put("x", buff.getShort());
						values.put("y", buff.getShort());
						
						int spriteIndex = buff.getShort() & 0xffff;
						SpriteEntry sprite = SpriteTable.get(spriteIndex);
						values.put("sprite_index", String.format("%d (%s)", spriteIndex, sprite == null ? "<invalid>" : sprite.name()));
						
						values.put("image_index", buff.get() & 0xff);
						values.put("battlegroup", buff.getShort() & 0xffff);
					} break;
					case FLAGS_UPDATE:
						break;
						
					case HEARTBEAT: break;
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
				sb.append("<");
				sb.append(e);
				sb.append("> ");
			}
			sb.append(values);
		}
		else {
			sb.append("<INVALID>");
		}
		
		while(buff.position() < buff.limit()) {
			sb.append(String.format("%02x ", buff.get()));
		}
		
		buff.position(0);
		return sb.toString();
	}
	
	public static String readGMString(ByteBuffer buff) {
		int pos = buff.position();
		int length = 0;
		while(buff.get() != 0) {
			length++;
		}
		if(length > 0) {
			buff.position(pos);
			
			byte[] bytes = new byte[length];
			buff.get(bytes);
			verify(buff.get() == 0, "Unexpected non-zero byte when reading string");
			try {
				return new String(bytes, "utf-8");
			} 
			catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				StringBuilder sb = new StringBuilder();
				for(byte b : bytes) {
					sb.append((char)b);
				}
				return sb.toString();
			}
		}
		return "";
	}
	
	public static String readGMString(ByteBuffer buff, int len) {
		byte[] bytes = new byte[len];
		buff.get(bytes);
		
		return new String(bytes, Charset.forName("utf-8", Charset.defaultCharset()));
	}
	
	public static void verify(boolean condition, String message) {
		verify(condition, message, IllegalStateException.class);
	}
	
	public static void verify(boolean condition, String message, Class<? extends Throwable> ex) {
		if(!condition) {
			doThrow(ex, message);
		}
	}
	
	public static void expect(Object value, Object expected, String message) {
		expect(value, expected, message, IllegalStateException.class);
	}
	
	public static void expect(Object value, Object expected, String message, Class<? extends Throwable> ex) {
		if(value != expected && (value == null || !value.equals(expected))) {
			doThrow(ex, String.format("%s: expected %s, got %s", message, expected, value));
		}
	}
	
	private static <T extends Throwable> void doThrow(Class<T> clazz, String msg) {
		Throwable inst;
		try {
			Constructor<T> constructor = clazz.getConstructor(String.class);
			inst = constructor.newInstance(msg);
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(msg);
		}
		throwUnchecked(inst);
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
	  throw (T) t;
	}
}
