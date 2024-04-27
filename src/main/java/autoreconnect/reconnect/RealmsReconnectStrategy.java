package autoreconnect.reconnect;

import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public class RealmsReconnectStrategy extends ReconnectStrategy {
    private final RealmsServer realmsServer;

    public RealmsReconnectStrategy(RealmsServer realmsServer) {
        this.realmsServer = realmsServer;
    }

    @Override
    public String getName() {
        return realmsServer.getName();
    }

    /**
     * @see net.minecraft.client.QuickPlay#joinRealmsWorld(net.minecraft.client.Minecraft, com.mojang.realmsclient.client.RealmsClient, String)
     */
    @Override
    public void reconnect() {
        var titlescreen = new TitleScreen();
        var getserverdetailstask = new GetServerDetailsTask(titlescreen, realmsServer);
        Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(titlescreen, getserverdetailstask));
    }
}
