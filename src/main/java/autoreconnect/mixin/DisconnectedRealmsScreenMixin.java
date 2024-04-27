package autoreconnect.mixin;

import autoreconnect.AutoReconnect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.DisconnectedRealmsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Mixin(DisconnectedRealmsScreen.class)
public class DisconnectedRealmsScreenMixin extends Screen {
    @Unique
    private Button reconnectButton, cancelButton, backButton;
    @Unique
    private boolean shouldAutoReconnect;

    protected DisconnectedRealmsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo info) {
        backButton = AutoReconnect.findBackButton(this)
                .orElseThrow(() -> new NoSuchElementException("Couldn't find the back button on the realms disconnect screen"));

        shouldAutoReconnect = AutoReconnect.getConfig().hasAttempts();

        reconnectButton = Button.builder(
                        Component.translatable("text.autoreconnect.disconnect.reconnect"),
                        btn -> AutoReconnect.schedule(() -> Minecraft.getInstance().execute(this::manualReconnect), 100, TimeUnit.MILLISECONDS))
                .bounds(0, 0, 0, 20).build();

        // put reconnect (and cancel button) where back button is and push that down
        reconnectButton.setX(backButton.getX());
        reconnectButton.setY(backButton.getY());
        if (shouldAutoReconnect) {
            reconnectButton.setWidth(backButton.getWidth() - backButton.getHeight() - 4);

            cancelButton = Button.builder(
                            Component.literal("✕")
                                    .withStyle(s -> s.withColor(ChatFormatting.RED)),
                            btn -> cancelCountdown())
                    .bounds(
                            backButton.getX() + backButton.getWidth() - backButton.getHeight(),
                            backButton.getY(),
                            backButton.getHeight(),
                            backButton.getHeight())
                    .build();

            addRenderableWidget(cancelButton);
        } else {
            reconnectButton.setWidth(backButton.getWidth());
        }
        addRenderableWidget(reconnectButton);
        backButton.setY(backButton.getY() + backButton.getHeight() + 4);

        if (shouldAutoReconnect) {
            AutoReconnect.getInstance().startCountdown(this::countdownCallback);
        }
    }

    @Unique
    private void manualReconnect() {
        AutoReconnect.getInstance().cancelAutoReconnect();
        AutoReconnect.getInstance().reconnect();
    }

    @Unique
    private void cancelCountdown() {
        AutoReconnect.getInstance().cancelAutoReconnect();
        shouldAutoReconnect = false;
        removeWidget(cancelButton);
        reconnectButton.active = true; // in case it was deactivated after running out of attempts
        reconnectButton.setMessage(Component.translatable("text.autoreconnect.disconnect.reconnect"));
        reconnectButton.setWidth(backButton.getWidth()); // reset to full width
    }

    @Unique
    private void countdownCallback(int seconds) {
        if (seconds < 0) {
            // indicates that we're out of attempts
            reconnectButton.setMessage(Component.translatable("text.autoreconnect.disconnect.reconnect_failed")
                    .withStyle(s -> s.withColor(ChatFormatting.RED)));
            reconnectButton.active = false;
        } else {
            reconnectButton.setMessage(Component.translatable("text.autoreconnect.disconnect.reconnect_in", seconds)
                    .withStyle(s -> s.withColor(ChatFormatting.GREEN)));
        }
    }

    // cancel auto reconnect when pressing escape, higher priority than exiting the screen
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && shouldAutoReconnect) {
            cancelCountdown();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
