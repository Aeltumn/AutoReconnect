package autoreconnect.reconnect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public class SingleplayerReconnectStrategy extends ReconnectStrategy {
    private final String worldName;

    public SingleplayerReconnectStrategy(String worldName) {
        this.worldName = worldName;
    }

    @Override
    public String getName() {
        return worldName;
    }

    /**
     * @see net.minecraft.client.QuickPlay#joinSingleplayerWorld(net.minecraft.client.Minecraft, String)
     */
    @Override
    public void reconnect() {
        var minecraft = Minecraft.getInstance();
        if (!minecraft.getLevelSource().levelExists(worldName)) return;
        minecraft.createWorldOpenFlows().openWorld(worldName, () -> {
            minecraft.setScreen(new TitleScreen());
        });
    }
}
