package cn.moy84.horse.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HorseClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    private static KeyBinding keyBinding;
    public static final Identifier PACKET_ID = new Identifier("horse", "horse_sprint");

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.horse.sprint",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.horse.sprint"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()){
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(1);
                ClientPlayNetworking.send(PACKET_ID, buf);
            }
        });

    }
}
