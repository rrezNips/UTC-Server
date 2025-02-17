package net.benjaminurquhart.utcserver;

import static net.benjaminurquhart.utcserver.Util.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

public class SpriteTable {

	private static final SpriteEntry[] SPRITES;
	
	static {
		SpriteEntry[] sprites = null;
		File file = new File("data.win");
		if(file.exists()) {
			try {
				ByteBuffer buff = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
				buff.order(ByteOrder.LITTLE_ENDIAN);
				
				expect(readGMString(buff, 4), "FORM", "Invalid file header");
				expect((long)buff.getInt(), Files.size(file.toPath()) - 8, "Invalid file size");
				
				boolean found = false;
				String chunkName = null;
				while(!found && buff.position() < buff.limit()) {
					chunkName = readGMString(buff, 4);
					System.out.printf("%s @ 0x%x\n", chunkName, buff.position() - 4);
					if("SPRT".equals(chunkName)) {
						found = true;
						break;
					}
					buff.position(buff.getInt() + buff.position());
				}
				if(!found) {
					throw new IllegalStateException("SPRT chunk not found");
				}
				
				// SPRT chunk size
				buff.getInt();
				
				sprites = new SpriteEntry[buff.getInt()];
				
				String name;
				int pos, numFrames, ptr;
				for(int i = 0; i < sprites.length; i++) {
					pos = buff.position();
					ptr = buff.getInt();
					buff.position(ptr);
					buff.position(buff.getInt());
					name = readGMString(buff);
					buff.position(ptr + 56);
					numFrames = buff.getInt();
					
					sprites[i] = new SpriteEntry(name, numFrames);
					
					//System.out.printf("%d: %s @ 0x%x\n", i, sprites[i], ptr);
					
					buff.position(pos + 4);
				}
			}
			catch(RuntimeException e) {
				throw e;
			}
			catch(Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				SPRITES = sprites;
			}
		}
		else {
			SPRITES = new SpriteEntry[0];
		}
	}
	
	protected static void init() {}
	
	public static int getCount() {
		return SPRITES == null ? 0 : SPRITES.length;
	}
	
	public static SpriteEntry[] get() {
		return SPRITES;
	}
	
	public static SpriteEntry get(int index) {
		if(SPRITES == null || index < 0 || index >= SPRITES.length) {
			return null;
		}
		return SPRITES[index];
	}
	
	public static int indexOf(String name) {
		for(int i = getCount() - 1; i >= 0; i--) {
			if(SPRITES[i].name().equals(name)) {
				return i;
			}
		}
		return -1;
	}
}
