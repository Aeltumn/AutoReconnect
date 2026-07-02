package autoreconnect.mixin;

import autoreconnect.AutoReconnect;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.objectweb.asm.Opcodes.PUTFIELD;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow
    private @Nullable Screen screen;

    @Inject(method = "setScreen", at = @At(value = "FIELD", opcode = PUTFIELD,
            target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private void setScreen(Screen newScreen, CallbackInfo info) {
        AutoReconnect.getInstance().onScreenChanged(screen, newScreen);
    }
}
