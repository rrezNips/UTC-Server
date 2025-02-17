package net.benjaminurquhart.utcserver;

public record SpriteEntry(String name, int frames) {
	
	@Override
	public String toString() {
		return String.format("%s (%d frame%s)", name, frames, frames == 1 ? "" : "s");
	}
}
