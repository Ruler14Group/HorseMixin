package cn.moy84.horse;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.util.Identifier;

public class Horse implements ModInitializer {
    /**
     * Runs the mod initializer.
     */
    public static final Identifier PACKET_ID = new Identifier("horse", "horse_sprint");

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, (server, player, handler, buf, responseSender) -> {
            if (buf.readInt() == 1 && player.hasVehicle() && player.getVehicle() instanceof HorseBaseEntity vehicle){
                vehicle.onSprint();
            }
        });
    }
}
