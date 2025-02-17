package net.benjaminurquhart.utcserver;

import static net.benjaminurquhart.utcserver.Util.readGMString;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Player extends Entity {

	private String name;
	private int lv, hp, maxHp, km;
	
	private short x, y;
	
	private int spriteIndex, imageIndex;
	
	private float imageAlpha;
	
	private short room;
	private byte state, draw;
	
	public Player() {
		this("CHARA");
	}
	
	public Player(String name) {
		this(name, 1);
	}
	
	public Player(String name, int lv) {
		this.name = name;
		this.lv = lv;
		
		this.hp = 16 + (lv * 4);
		this.maxHp = this.hp;
		
		this.spriteIndex = SpriteTable.indexOf("spr_maincharad");
	}
	
	@Override
	protected boolean updateFromBufferImpl(ByteBuffer buff) {
		String newName = readGMString(buff);
		int newLv = buff.get();
		int newHp = buff.get();
		int newMaxHp = buff.get();
		int newKm = buff.get();
		short newX = buff.getShort();
		short newY = buff.getShort();
		int newSpriteIndex = buff.getShort() & 0xffff;
		int newImageIndex = buff.get() & 0xff;
		float newImageAlpha = buff.getFloat();
		short newRoom = buff.getShort();
		byte newState = buff.get();
		byte newDraw = buff.get();
		
		SpriteEntry sprite = SpriteTable.get(newSpriteIndex);
		if(sprite == null) {
			throw new IllegalStateException("Sprite " + newSpriteIndex + " does not exist");
		}
		if(!sprite.name().contains("mainchara")) {
			throw new IllegalStateException("Illegal sprite: " + sprite.name());
		}
		
		// TODO: room validation
		
		if(!newName.equals(name) ||
				  newLv != lv ||
				  newHp != hp ||
				  newMaxHp != maxHp ||
				  newKm != km ||
				  newX != x ||
				  newY != y ||
				  newSpriteIndex != spriteIndex ||
				  newImageIndex != imageIndex ||
				  newImageAlpha != imageAlpha ||
				  newRoom != room ||
				  newState != state ||
				  newDraw != draw) {
			synchronized(DATA_LOCK) {
				name = newName;
				lv = newLv;
				hp = newHp;
				maxHp = newMaxHp;
				km = newKm;
				x = newX;
				y = newY;
				spriteIndex = newSpriteIndex;
				imageIndex = newImageIndex;
				imageAlpha = newImageAlpha;
				room = newRoom;
				state = newState;
				draw = newDraw;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void serialize(GMDataOutputStream stream) throws IOException {
		synchronized(DATA_LOCK) {
			stream.writeUTF(name);
			stream.write(lv);
			stream.write(hp);
			stream.write(maxHp);
			stream.write(km);
			stream.writeShort(x);
			stream.writeShort(y);
			stream.writeShort(spriteIndex);
			stream.write(imageIndex);
			stream.writeFloat(imageAlpha);
			stream.writeShort(room);
			stream.write(state);
			stream.write(draw);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public int getLV() {
		return lv;
	}
	
	public int getHP() {
		return hp;
	}
	
	public int getMaxHP() {
		return maxHp;
	}
	
	public int getKM() {
		return km;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getSpriteIndex() {
		return spriteIndex;
	}
	
	public int getImageIndex() {
		return imageIndex;
	}
	
	public float getImageAlpha() {
		return imageAlpha;
	}
	
	public short getRoom() {
		return room;
	}
	
	public byte getState() {
		return state;
	}
	
	public byte getDraw() {
		return draw;
	}
}
