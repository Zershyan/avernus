package io.zershyan.avernus.util.mixin.avernus.common;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface IMixinPathNavigationRegion {
    Level avernus$level();
    @Nullable Mob avernus$subject();
    void avernus$setSubject(Mob mob);
}
