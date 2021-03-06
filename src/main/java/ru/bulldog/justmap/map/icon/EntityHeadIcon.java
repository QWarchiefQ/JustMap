package ru.bulldog.justmap.map.icon;

import java.util.HashMap;
import java.util.Map;

import ru.bulldog.justmap.client.config.ClientParams;
import ru.bulldog.justmap.util.ImageUtil;
import ru.bulldog.justmap.util.SpriteAtlas;
import ru.bulldog.justmap.util.DrawHelper;

import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

public class EntityHeadIcon extends AbstractIcon {
	
	private final static Map<Identifier, EntityHeadIcon> ICONS = new HashMap<>();
	
	private EntityHeadIcon(Identifier texture, int w, int h) {
		super(SpriteAtlas.ENTITY_HEAD_ICONS, new Sprite.Info(texture, w, h, AnimationResourceMetadata.EMPTY), 0, w, h, 0, 0, ImageUtil.loadImage(texture, w, h));
	}
	
	public static EntityHeadIcon getIcon(Entity entity) {
		Identifier id = EntityType.getId(entity.getType());
		if (ICONS.containsKey(id)) {
			return ICONS.get(id);
		} else {
			Identifier iconId = iconId(id);
			if (ImageUtil.imageExists(iconId)) {
				return registerIcon(id, iconId);
			}
		}
		
		return null;
	}
	
	@Override
	public void draw(double x, double y, int w, int h) {
		if (ClientParams.showIconsOutline) {
			DrawHelper.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF444444);
		}
		textureManager.bindTexture(this.getId());
		
		this.draw(x, y, (float) w, (float) h);
	}
	
	private static Identifier iconId(Identifier id) {
		String path = String.format("textures/minimap/entities/%s.png", id.getPath());
		return new Identifier(id.getNamespace(), path);
	}
	
	private static EntityHeadIcon registerIcon(Identifier entityId, Identifier iconId) {
		EntityHeadIcon icon = new EntityHeadIcon(iconId, 32, 32);
		ICONS.put(entityId, icon);
		
		return icon;
	}
}
