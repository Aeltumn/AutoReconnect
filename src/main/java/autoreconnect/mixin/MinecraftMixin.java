package autoreconnect.mixin;

import autoreconnect.AutoReconnect;
import autoreconnect.reconnect.SingleplayerReconnectStrategy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.objectweb.asm.Opcodes.PUTFIELD;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public Screen screen;

    @Inject(method = "doWorldLoad", at = @At("HEAD"))
    private void doWorldLoad(LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, boolean bl, CallbackInfo ci) {
        AutoReconnect.getInstance().setReconnectHandler(new SingleplayerReconnectStrategy(worldStem.worldData().getLevelName()));
    }

    @Inject(method = "setScreen", at = @At(value = "FIELD", opcode = PUTFIELD,
            target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private void setScreen(Screen newScreen, CallbackInfo info) {
        AutoReconnect.getInstance().onScreenChanged(screen, newScreen);
    }
}
