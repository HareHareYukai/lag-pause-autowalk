package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWalk;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class LagPauseAutoWalk extends Module {


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private long lagBackTime = 0;
    private final Setting<AutoWalk.Mode> mode = sgGeneral.add(new EnumSetting.Builder<AutoWalk.Mode>()
        .name("mode")
        .description("Walking mode.")
        .defaultValue(AutoWalk.Mode.Simple)
        .onChanged(mode1 -> {
            if (isActive()) {
                if (mode1 == AutoWalk.Mode.Simple) {
                    PathManagers.get().stop();
                } else {
                    createGoal();
                }

                unpress();
            }
        })
        .build()
    );

    private final Setting<AutoWalk.Direction> direction = sgGeneral.add(new EnumSetting.Builder<AutoWalk.Direction>()
        .name("simple-direction")
        .description("The direction to walk in simple mode.")
        .defaultValue(AutoWalk.Direction.Forwards)
        .onChanged(direction1 -> {
            if (isActive()) unpress();
        })
        .visible(() -> mode.get() == AutoWalk.Mode.Simple)
        .build()
    );

    public LagPauseAutoWalk() {
        super(Addon.CATEGORY, "lag-pause-auto-walk", "Automatically walks forward.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == AutoWalk.Mode.Smart) createGoal();
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == AutoWalk.Mode.Simple) unpress();
        else PathManagers.get().stop();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (lagBackTime + 1_000_000_000 > System.nanoTime()) {
            return;
        }

        if (mode.get() == AutoWalk.Mode.Simple) {
            switch (direction.get()) {
                case Forwards -> setPressed(mc.options.forwardKey, true);
                case Backwards -> setPressed(mc.options.backKey, true);
                case Left -> setPressed(mc.options.leftKey, true);
                case Right -> setPressed(mc.options.rightKey, true);
            }
        } else {
            if (PathManagers.get() instanceof NopPathManager) {
                info("Smart mode requires Baritone");
                toggle();
            }
        }
    }

    private void unpress() {
        setPressed(mc.options.forwardKey, false);
        setPressed(mc.options.backKey, false);
        setPressed(mc.options.leftKey, false);
        setPressed(mc.options.rightKey, false);
    }

    private void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    private void createGoal() {
        PathManagers.get().moveInDirection(mc.player.getYaw());
    }

    public enum Mode {
        Simple,
        Smart
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            lagBackTime = System.nanoTime();
            unpress();
        }
    }

    public enum Direction {
        Forwards,
        Backwards,
        Left,
        Right
    }
}
