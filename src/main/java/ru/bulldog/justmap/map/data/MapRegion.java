package ru.bulldog.justmap.map.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.ChunkPos;

import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.util.Colors;
import ru.bulldog.justmap.util.ImageUtil;
import ru.bulldog.justmap.util.StorageUtil;

public class MapRegion {
	
	private final RegionPos pos;

	private ConcurrentMap<Layers, RegionLayer> layers = new ConcurrentHashMap<>();
	private Layers currentLayer;
	private int currentLevel;
	
	public long updated = 0;
	public long saved = 0;
	
	public MapRegion(ChunkPos pos) {
		this(pos.getRegionX(),
			 pos.getRegionZ());
	}
	
	public MapRegion(int x, int z) {
		this.pos = new RegionPos(x, z);
	}
	
	public void setLevel(int level) {
		this.currentLevel = level > 0 ? level : 0;
	}
	
	public void setLayer(Layers layer) {
		this.currentLayer = layer;
	}
	
	public NativeImage getChunkImage(ChunkPos chunkPos) {
		return getChunkImage(chunkPos, currentLayer, currentLevel);
	}
	
	public NativeImage getChunkImage(ChunkPos chunkPos, Layers layer, int level) {
		if (chunkPos.getRegionX() != this.pos.x ||
			chunkPos.getRegionZ() != this.pos.z) {
			
			return null;
		}
		
		int imgX = (chunkPos.x - (this.pos.x << 5)) << 4;
		int imgY = (chunkPos.z - (this.pos.z << 5)) << 4;
		
		return ImageUtil.readTile(getImage(layer, level), imgX, imgY, 16, 16, false);
	}
	
	public NativeImage getImage(Layers layer, int level) {		
		return getLayer(layer).getImage(level).image();
	}
	
	public void saveImage() {
		if (getLayer().isSaved()) return;
		
		File png = new File(imagesDir(), String.format("%d.%d.png", this.pos.x, this.pos.z));		
		getLayer().saveImage(png);
		
		this.saved = System.currentTimeMillis();
	}
	
	public RegionLayer getLayer() {
		return getLayer(currentLayer);
	}
	
	public RegionLayer getLayer(Layers layer) {
		if (layers.containsKey(layer)) {
			return layers.get(layer);
		}
		
		RegionLayer regionLayer = new RegionLayer(layer);
		layers.put(layer, regionLayer);
		
		return regionLayer;
	}
	
	public void storeChunk(MapChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		if (chunkPos.getRegionX() != this.pos.x || chunkPos.getRegionZ() != this.pos.z) return;
		
		int imgX = (chunkPos.x - (this.pos.x << 5)) << 4;
		int imgY = (chunkPos.z - (this.pos.z << 5)) << 4;			
		
		getLayer().writeImage(chunk.getImage(), imgX, imgY);
		
		this.updated = System.currentTimeMillis();
	}
	
	public int getX() {
		return this.pos.x;
	}
	
	public int getZ() {
		return this.pos.z;
	}
	
	private File imagesDir() {
		return imagesDir(currentLayer, currentLevel);
	}
	
	private File imagesDir(Layers layer, int level) {
		File cacheDir;
		if (layer.equals(Layers.Type.SURFACE.value)) {
			cacheDir = layerDir(layer);
		} else {
			cacheDir = new File(layerDir(layer), String.format("%d/", level));
		}
		
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		
		return cacheDir;
	}
	
	private File layerDir(Layers layer) {
		return new File(StorageUtil.cacheDir(), String.format("%s/", layer.name));
	}
	
	public class RegionLayer {
		private ConcurrentMap<Integer, RegionImage> images;
		private final Layers layer;
		
		private RegionLayer(Layers layer) {
			this.layer = layer;
			this.images = new ConcurrentHashMap<>();
		}
		
		private RegionImage getImage(int level) {
			if (images.containsKey(level)) {
				return images.get(level);
			}			
			
			RegionImage regionImage = new RegionImage(layer, level);
			images.put(level, regionImage);
			
			return regionImage;
		}
		
		private RegionImage currentImage() {
			return getImage(currentLevel);
		}
		
		public boolean isSaved() {
			return currentImage().saved;
		}
		
		public void writeImage(NativeImage tile, int x, int y) {
			currentImage().write(tile, x, y);
		}
		
		public void saveImage(File png) {
			currentImage().save(png);
		}
		
		@Override
		public String toString() {
			return this.layer.name;
		}
	}
	
	private class RegionImage {
		private volatile NativeImage image;
		private boolean saved = true;
		
		private RegionImage(Layers layer, int level) {
			this.image = loadImage(layer, level);
		}
		
		private NativeImage loadImage(Layers layer, int level) {
			File png = new File(imagesDir(layer, level), String.format("%d.%d.png", pos.x, pos.z));
			if (png.exists()) {
				try (FileInputStream fis = new FileInputStream(png)) {
					return NativeImage.read(fis);
				} catch (IOException ex) {
					JustMap.LOGGER.catching(ex);
				}
			}
			
			NativeImage image = new NativeImage(512, 512, false);
			ImageUtil.fillImage(image, Colors.BLACK);
			
			return image;
		}
		
		public NativeImage image() {
			synchronized (image) {
				return this.image;
			}			
		}
		
		public void write(NativeImage tile, int x, int y) {
			ImageUtil.writeTile(image(), tile, x, y);
			this.saved = false;
		}
		
		public void save(File png) {
			try {
				image().writeFile(png);
				this.saved = true;
			} catch (IOException ex) {
				JustMap.LOGGER.catching(ex);
			}
		}
	}
}
