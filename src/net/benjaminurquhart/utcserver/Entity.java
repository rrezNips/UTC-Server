package net.benjaminurquhart.utcserver;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class Entity {

	private boolean updated;
	
	protected final Object DATA_LOCK = new Object();
	
	protected final boolean updated() {
		return updated;
	}
	
	protected final void clearUpdate() {
		updated = false;
	}
	
	protected final void updateFromBuffer(ByteBuffer buff) {
		updated = updateFromBufferImpl(buff);
	}
	
	protected abstract boolean updateFromBufferImpl(ByteBuffer buff);
	
	public abstract void serialize(GMDataOutputStream stream) throws IOException;
}
