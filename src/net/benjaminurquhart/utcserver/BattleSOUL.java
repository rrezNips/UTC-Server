package net.benjaminurquhart.utcserver;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BattleSOUL extends Entity {

	private short x, y;
	
	private int spriteIndex, imageIndex, battlegroup;

	@Override
	protected boolean updateFromBufferImpl(ByteBuffer buff) {
		short newX = buff.getShort();
		short newY = buff.getShort();
		int newSpriteIndex = buff.getShort() & 0xffff;
		int newImageIndex = buff.get() & 0xff;
		int newBattleGroup = buff.getShort() & 0xffff;
		
		SpriteEntry sprite = SpriteTable.get(newSpriteIndex);
		
		if(sprite == null) {
			throw new IllegalStateException("Sprite " + newSpriteIndex + " does not exist");
		}
		if(!sprite.name().contains("heart")) {
			throw new IllegalStateException("Illegal sprite: " + sprite.name());
		}
		
		if (x != newX || y != newY || spriteIndex != newSpriteIndex || imageIndex != newImageIndex || battlegroup != newBattleGroup) {
			synchronized(DATA_LOCK) {
				x = newX;
				y = newY;
				spriteIndex = newSpriteIndex;
				imageIndex = newImageIndex;
				battlegroup = newBattleGroup;
			}
			return true;
		}
		
		return false;
	}

	@Override
	public void serialize(GMDataOutputStream stream) throws IOException {
		synchronized(DATA_LOCK) {
			stream.writeShort(x);
			stream.writeShort(y);
			stream.writeShort(spriteIndex);
			stream.write(imageIndex);
			stream.writeShort(battlegroup);
		}
	}
}
