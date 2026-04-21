package autoreconnect.mixin;

import autoreconnect.AutoReconnect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen {
    @Shadow
    @Final
    @Mutable
    private Screen parent;
    @Shadow
    @Final
    private LinearLayout layout;
    @Unique
    private Button reconnectButton, cancelButton, backButton;
    @Unique
    private boolean shouldAutoReconnect;
    @Unique
    private Integer baseButtonY;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V")
    private void constructor(Screen parent, Component title, Component reason, Component buttonLabel, CallbackInfo info) {
        if (AutoReconnect.getInstance().isPlayingSingleplayer()) {
            // make back button redirect to SelectWorldScreen instead of MultiPlayerScreen (https://bugs.mojang.com/browse/MC-45602)
            this.parent = new SelectWorldScreen(new TitleScreen());
        }
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/DisconnectedScreen;repositionElements()V"))
    private void init(CallbackInfo info) {
        backButton = AutoReconnect.findBackButton(this)
                .orElseThrow(() -> new NoSuchElementException("Couldn't find the back button on the disconnect screen"));

        if (AutoReconnect.getInstance().isPlayingSingleplayer()) {
            // change back button text to "Back" instead of "Back to World List" bcs of bug fix above
            backButton.setMessage(Component.translatable("gui.toWorld"));
        }

        shouldAutoReconnect = AutoReconnect.getConfig().hasAttempts();
        baseButtonY = null;

        reconnectButton = Button.builder(
                        Component.translatable("text.autoreconnect.disconnect.reconnect"),
                        btn -> AutoReconnect.schedule(() -> Minecraft.getInstance().execute(this::manualReconnect), 100, TimeUnit.MILLISECONDS)).build();
        addRenderableWidget(reconnectButton);

        if (shouldAutoReconnect) {
            cancelButton = Button.builder(
                            Component.literal("✕")
                                    .withStyle(s -> s.withColor(ChatFormatting.RED)),
                            btn -> cancelCountdown()).build();
            addRenderableWidget(cancelButton);
        }

        if (shouldAutoReconnect) {
            AutoReconnect.getInstance().startCountdown(this::countdownCallback);
        }
    }

    // make this screen closable by pressing escape
    @Inject(at = @At("RETURN"), method = "shouldCloseOnEsc", cancellable = true)
    private void shouldCloseOnEsc(CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(true);
    }

    // actually return to parent screen and not to the title screen
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // cancel auto reconnect when pressing escape, higher priority than exiting the screen
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 && shouldAutoReconnect) {
            cancelCountdown();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public void repositionElements() {
        if (baseButtonY != null) {
            backButton.setY(baseButtonY);
        }
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
        baseButtonY = backButton.getY();

        // Update the positions of all buttons
        reconnectButton.setX(backButton.getX());
        reconnectButton.setY(backButton.getY());
        reconnectButton.setHeight(20);
        if (shouldAutoReconnect) {
            reconnectButton.setWidth(backButton.getWidth() - backButton.getHeight() - 4);
        } else {
            reconnectButton.setWidth(backButton.getWidth());
        }
        cancelButton.setX(backButton.getX() + backButton.getWidth() - backButton.getHeight());
        cancelButton.setY(backButton.getY());
        cancelButton.setWidth(backButton.getHeight());
        cancelButton.setHeight(backButton.getHeight());

        backButton.setY(backButton.getY() + backButton.getHeight() + 4);
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

    @Unique
    private void manualReconnect() {
        AutoReconnect.getInstance().cancelAutoReconnect();
        AutoReconnect.getInstance().reconnect();
    }
}
