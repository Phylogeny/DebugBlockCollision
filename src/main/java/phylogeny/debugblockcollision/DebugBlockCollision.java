package phylogeny.debugblockcollision;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToAccessFieldException;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindMethodException;
import net.minecraftforge.fml.relauncher.Side;
import phylogeny.debugblockcollision.ConfigMod.Mode;
import phylogeny.debugblockcollision.ConfigMod.ModeNames;

@EventBusSubscriber(Side.CLIENT)
@Mod(modid = DebugBlockCollision.MOD_ID,
	 version = DebugBlockCollision.VERSION,
	 acceptedMinecraftVersions = DebugBlockCollision.MC_VERSIONS_ACCEPTED,
	 updateJSON = DebugBlockCollision.UPDATE_JSON,
	 clientSideOnly = DebugBlockCollision.CLIENT_OLNY)
public class DebugBlockCollision
{
	public static final String MOD_ID = "debugblockcollision";
	public static final String MOD_NAME = "Debug Block Collision";
	public static final String MOD_PATH = "phylogeny." + MOD_ID;
	public static final String VERSION = "@VERSION@";
	public static final String UPDATE_JSON = "@UPDATE@";
	public static final String MC_VERSIONS_ACCEPTED = "[1.12.2,)";
	public static final boolean CLIENT_OLNY = true;
	public static final String MODE_COMMENT = "If set to 'Blocks In Radius', all collision/bounding boxes that are not single full-blocks will render within a "
			+ "radius around the player. If set to 'Boxes Collided', only collision boxes that the player is currently colliding with will render. If set to "
			+ "'Blocks Hovered', only the collision/bounding of the block the player is looking at will render (sneaking will cause only the single box looked "
			+ "at to render). [full collision boxes = green; non-full collision boxes = blue; bounding boxes (for blocks with no collision boxes) = red]";

	public static Logger logger;

	private static boolean enabled;
	private static Configuration configFile;

	public static Configuration getConfigFile() throws Exception
	{
		if (configFile == null)
		{
			try
			{
				Method getConfiguration = ReflectionHelper.findMethod(ConfigManager.class, "getConfiguration", null, String.class, String.class);
				configFile = (Configuration) getConfiguration.invoke(new ConfigManager(), DebugBlockCollision.MOD_ID, null);
			}
			catch (UnableToFindMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				logger.error("Failed to get config file instance", e);
				throw(e);
			}
		}
		return configFile;
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
	}

	@EventHandler
	public void init(@SuppressWarnings("unused") FMLInitializationEvent event)
	{
		KeyBindingsMod.init();
	}

	@SubscribeEvent
	public static void toggleEnabled(@SuppressWarnings("unused") KeyInputEvent event) throws Exception
	{
//		System.out.println(KeyBindingsMod.MODE.isPressed());
		if (Keyboard.isKeyDown(Keyboard.KEY_F3))
		{
			if (!KeyBindingsMod.MODE.isPressed())
				return;

			// Toggle enabled and prevent debug screen from toggling on/off
			try
			{
				ReflectionHelper.setPrivateValue(Minecraft.class, Minecraft.getMinecraft(), true, "actionKeyF3", "field_184129_aV");
				enabled ^= true;
			}
			catch (UnableToAccessFieldException e)
			{
				logger.error("Failed to toggle debug block collision box visibility", e);
				throw(e);
			}

			// Send chat message
			debugFeedbackTranslated(enabled ? "on" : "off");

			// Display color key
			if (enabled)
				Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation("debug."
						+ DebugBlockCollision.MOD_ID + ".color_key").setStyle(new Style().setColor(TextFormatting.DARK_GREEN)));
		}
		else if (enabled && KeyBindingsMod.MODE.isPressed())
		{
			// Cycle mode
			Mode mode = ConfigMod.CLIENT.mode;
			ConfigMod.CLIENT.mode = mode.values()[(mode.ordinal() + 1) % mode.values().length];

			// Update config file
			Configuration configFile = getConfigFile();
			Property prop = configFile.get("client", "Mode", ModeNames.BLOCK_HOVERED.toString());
			ModeNames name = ModeNames.values()[ConfigMod.CLIENT.mode.ordinal()];
			prop.setValue(name.toString());
			prop.setComment(MODE_COMMENT);
			configFile.save();

			// Send chat message
			debugFeedbackTranslated(name.getChatKey());
		}
	}

	private static void debugFeedbackTranslated(String keySuffix) throws Exception
	{
		try
		{
			Method debugFeedbackTranslated = ReflectionHelper.findMethod(Minecraft.class, "debugFeedbackTranslated", "func_190521_a", String.class, Object[].class);
			String debug = "debug." + DebugBlockCollision.MOD_ID;
			debugFeedbackTranslated.invoke(Minecraft.getMinecraft(), debug,
					new Object[]{new TextComponentTranslation(debug + "." + keySuffix).setStyle(new Style().setColor(TextFormatting.DARK_AQUA))});
		}
		catch (UnableToFindMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			logger.error("Failed to send feedback for toggling of debug block collision box visibility", e);
			throw(e);
		}
	}

	@SubscribeEvent
	public static void renderCollisionBoxes(RenderWorldLastEvent event)
	{
		if (!enabled)
			return;

		EntityPlayer player = Minecraft.getMinecraft().player;
		World world = player.world;
		float ticks = event.getPartialTicks();
		List<ColoredBox> boxes = new ArrayList<>();
		if (ConfigMod.CLIENT.mode.ordinal() == ModeNames.BLOCK_HOVERED.ordinal())
		{
			// Add all collision/bounding boxes for block looked at
			RayTraceResult target = Minecraft.getMinecraft().objectMouseOver;
			if (target == null || !target.typeOfHit.equals(RayTraceResult.Type.BLOCK) || addBoxesToList(world, target.getBlockPos(), true, boxes))
				return;
			else if (player.isSneaking())
			{
				// Get the box looked at via a raytrace
				double distanceSq;
				double distanceSqShortest = Double.POSITIVE_INFINITY;
				ColoredBox boxClosest = null;
				RayTraceResult result;
				Vec3d eyes = player.getPositionEyes(ticks);
				Vec3d look = eyes.add(player.getLook(ticks).scale(Minecraft.getMinecraft().playerController.getBlockReachDistance()));
				for (ColoredBox box : boxes)
				{
					result = box.calculateIntercept(eyes, look);
					if (result != null)
					{
						distanceSq = result.hitVec.squareDistanceTo(eyes);
						if (distanceSq < distanceSqShortest)
						{
							distanceSqShortest = distanceSq;
							boxClosest = box;
						}
					}
				}
				if (boxClosest == null)
					return;

				// Remove all but the box looked at
				boxes.clear();
				boxes.add(boxClosest);
			}
		}
		else
		{
			double x, y, z;
			if (ConfigMod.CLIENT.mode.ordinal() == ModeNames.BOXES_COLLIDED.ordinal())
			{
				// Add all collision boxes that the player is collided with
				AxisAlignedBB playerBoundingBox = player.getEntityBoundingBox().grow(0.001);
				world.getCollisionBoxes(player, playerBoundingBox).forEach(box -> boxes.add(new ColoredBox(box, false)));

				// Add all bounding boxes that the player is collided with
				AxisAlignedBB box = player.getEntityBoundingBox();
				box = new AxisAlignedBB(Math.floor(box.minX), Math.floor(box.minY), Math.floor(box.minZ),
						Math.ceil(box.maxX), Math.ceil(box.maxY), Math.ceil(box.maxZ)).grow(1);
				for (x = box.minX; x < box.maxX; x++)
				{
					for (y = box.minY; y < box.maxY; y++)
					{
						for (z = box.minZ; z < box.maxZ; z++)
						{
							addBoxesToList(world, new BlockPos(x, y, z), false, playerBoundingBox, boxes);
						}
					}
				}
			}
			else
			{
				// Add all collision/bounding boxes within a radius of the player's eyes
				double radius = ConfigMod.CLIENT.renderRadius;
				Vec3d eyes = player.getPositionEyes(ticks);
				AxisAlignedBB box = new AxisAlignedBB(new BlockPos(eyes)).grow(radius).offset(0.5, 0.5, 0.5);
				double radiusSq = radius * radius;
				for (x = box.minX; x < box.maxX; x++)
				{
					for (y = box.minY; y < box.maxY; y++)
					{
						for (z = box.minZ; z < box.maxZ; z++)
						{
							if (eyes.squareDistanceTo(x, y, z) <= radiusSq)
								addBoxesToList(world, new BlockPos(x, y, z), false, boxes);
						}
					}
				}
			}
		}

		// Render all boxes
		double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * ticks;
		double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * ticks;
		double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * ticks;
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);
		GlStateManager.glLineWidth(ConfigMod.CLIENT.lineWidth);
		for (ColoredBox box : boxes)
			box.render(playerX, playerY, playerZ);

		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.glLineWidth(2.0F);
	}

	private static boolean addBoxesToList(World world, BlockPos pos, boolean renderSolitaryFullBoxes, List<ColoredBox> boxes)
	{
		return addBoxesToList(world, pos, renderSolitaryFullBoxes, null, boxes);
	}

	private static boolean addBoxesToList(World world, BlockPos pos, boolean renderSolitaryFullBoxes, @Nullable AxisAlignedBB playerBoundingBox, List<ColoredBox> boxes)
	{
		IBlockState state = world.getBlockState(pos).getActualState(world, pos);
		if (state.getBlock().isAir(state, world, pos))
			return true;

		// Attempt to collect all collision boxes
		List<AxisAlignedBB> boxesCollision = new ArrayList<>();
		state.addCollisionBoxToList(world, pos, TileEntity.INFINITE_EXTENT_AABB, boxesCollision, null, true);

		// Remove null boxes
		boxesCollision.removeIf(box -> box == null);

		// Ignore solitary full-blocks
		if (!renderSolitaryFullBoxes && boxesCollision.size() == 1 && boxesCollision.get(0).equals(Block.FULL_BLOCK_AABB.offset(pos)))
			return true;

		if (boxesCollision.isEmpty())
		{
			Material material = state.getMaterial();
			if (material.isLiquid())
				return true;

			// Add bounding box for blocks without collision
			AxisAlignedBB bounds = state.getBoundingBox(world, pos);
			if (bounds == null)
				return true;

			bounds = bounds.offset(pos);
			if (playerBoundingBox == null || playerBoundingBox.intersects(bounds))
				boxes.add(new ColoredBox(bounds, true));
		}
		else if (playerBoundingBox == null)
		{
			// Add all collision boxes (if not in collision mode, since they would have already been added)
			boxesCollision.forEach(box -> boxes.add(new ColoredBox(box, false)));
		}
		return false;
	}

	private static class ColoredBox extends AxisAlignedBB
	{
		private Color color;

		public ColoredBox(AxisAlignedBB box, boolean isBoundingBox)
		{
			super(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
			color = isBoundingBox ? Color.RED : (box.offset(-box.minX, -box.minY, -box.minZ).equals(Block.FULL_BLOCK_AABB) ? Color.GREEN : Color.BLUE);
		}

		public void render(double playerX, double playerY, double playerZ)
		{
			color.renderBox(this, playerX, playerY, playerZ);
		}
	}

	private static enum Color
	{
		RED(1, 0, 0),
		GREEN(0, 1, 0),
		BLUE(0, 0, 1);

		private final float red, green, blue;
		private Color(float red, float green, float blue)
		{
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		public void renderBox(AxisAlignedBB box, double playerX, double playerY, double playerZ)
		{
			box = box.offset(-playerX, -playerY, -playerZ);
			RenderGlobal.drawSelectionBoundingBox(box, red, green, blue, 155 / 255.0F);
			if (ConfigMod.CLIENT.renderObscuredLines)
			{
				GlStateManager.depthFunc(GL11.GL_GREATER);
				RenderGlobal.drawSelectionBoundingBox(box, red, green, blue, 28 / 255.0F);
				GlStateManager.depthFunc(GL11.GL_LEQUAL);
			}
		}
	}
}