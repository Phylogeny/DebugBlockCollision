package phylogeny.debugblockcollision;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public enum KeyBindingsMod implements IKeyConflictContext
{
	MODE("mode", Keyboard.KEY_O);
	
	protected KeyBinding keyBinding;
	protected String description;
	private int defaultKeyCode;
	private KeyModifier defaultModifier;
	
	private KeyBindingsMod(String description, int defaultKeyCode)
	{
		this(description, defaultKeyCode, KeyModifier.NONE);
	}
	
	private KeyBindingsMod(String description, int defaultKeyCode, KeyModifier defaultModifier)
	{
		this.description = description;
		this.defaultKeyCode = defaultKeyCode;
		this.defaultModifier = defaultModifier;
	}
	
	public boolean isPressed()
	{
		return getKeyBinding().isPressed();
	}
	
	public boolean isKeyDown()
	{
		return getKeyBinding().isKeyDown();
	}
	
	public static void init()
	{
		for (KeyBindingsMod keyBinding : values())
			keyBinding.registerKeyBinding();
	}
	
	private void registerKeyBinding()
	{
		keyBinding = new KeyBinding("keybinding." + DebugBlockCollision.MOD_ID + "." + description.toLowerCase(),
				this, defaultModifier, defaultKeyCode, "itemGroup." + DebugBlockCollision.MOD_ID);
		ClientRegistry.registerKeyBinding(keyBinding);
	}
	
	public String getText()
	{
		return keyBinding.isSetToDefaultValue() ? description.toUpperCase() : ("[" + keyBinding.getDisplayName() + "]");
	}
	
	public KeyBinding getKeyBinding()
	{
		return keyBinding;
	}
	
	@Override
	public boolean conflicts(IKeyConflictContext other)
	{
		return conflictsInGame(other);
	}
	
	protected boolean conflictsInGame(IKeyConflictContext other)
	{
		return other == this || other == KeyConflictContext.IN_GAME;
	}
	
	@Override
	public boolean isActive()
	{
		return true;
	}
	
}