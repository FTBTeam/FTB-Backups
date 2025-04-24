package dev.ftb.mods.ftbbackups.config;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.BaseValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ArchivalPluginValue extends BaseValue<ResourceLocation> {
    public ArchivalPluginValue(@Nullable SNBTConfig config, String key, ResourceLocation def) {
        super(config, key, def);
    }

    @Override
    public void write(SNBTCompoundTag tag) {
        List<String> s = new ArrayList<>(comment);
        s.add("Default: " + defaultValue);
        tag.comment(key, String.join("\n", s));
        tag.putString(key, get().toString());
    }

    @Override
    public void read(SNBTCompoundTag snbtCompoundTag) {
        set(ResourceLocation.parse(snbtCompoundTag.getString(key)));
    }

    @Override
    public void createClientConfig(ConfigGroup group) {
        group.add(key, new ArchivalPluginConfig(), get(), this::set, defaultValue);
    }
}
