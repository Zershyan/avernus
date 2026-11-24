package io.zershyan.avernus.client.sound;

import io.zershyan.avernus.client.mirror.MirrorAnchors;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 开境（碎裂与展开）与收拢的音效由客户端按领域范围播放：境内不随距离衰减，声像仍指向镜子；出境后向外渐弱。
 */
public final class RealmSounds {
    /** 出境后渐弱到无声的距离。 */
    private static final double FADE = 32.0;
    /** 晚于演出该 tick 数才得知的领域（如刚进入视距）不再补放。 */
    private static final long LATE = 10L;
    private static final Set<UUID> OPENED = new HashSet<>();
    private static final Set<UUID> CLOSED = new HashSet<>();

    private RealmSounds() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        long time = level.getGameTime();
        Set<UUID> live = new HashSet<>();
        for (Realm realm : RealmSets.of(level).all()) {
            live.add(realm.id());
            if (!realm.shatters()) continue;
            if (due(time, realm.shatterTime()) && OPENED.add(realm.id())) {
                play(level, realm, AvernusSounds.SHATTER.get());
                play(level, realm, AvernusSounds.EXPAND.get());
            }
            if (realm.closeTime() >= 0L && due(time, realm.closeTime()) && CLOSED.add(realm.id())) {
                play(level, realm, AvernusSounds.CONTRACT.get());
            }
        }
        OPENED.retainAll(live);
        CLOSED.retainAll(live);
    }

    public static void reset() {
        OPENED.clear();
        CLOSED.clear();
    }

    private static boolean due(long time, long at) {
        return time >= at && time - at <= LATE;
    }

    private static void play(ClientLevel level, Realm realm, SoundEvent sound) {
        Cue cue = new Cue(level, realm, sound);
        if (cue.audible()) Minecraft.getInstance().getSoundManager().play(cue);
    }

    /** 跟随镜子位置、按听者与领域边界的距离调整音量。 */
    private static final class Cue extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final Realm realm;

        Cue(ClientLevel level, Realm realm, SoundEvent sound) {
            super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.level = level;
            this.realm = realm;
            attenuation = Attenuation.NONE;
            tick();
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != level) {
                stop();
                return;
            }
            Vec3 at = MirrorAnchors.shatterPoint(realm, 1.0F);
            x = at.x;
            y = at.y;
            z = at.z;
            Vec3 ear = mc.gameRenderer.getMainCamera().getPosition();
            double outside = Math.hypot(ear.x - realm.exactCenterX(), ear.z - realm.exactCenterZ()) - realm.radius();
            volume = (float) Mth.clamp(1.0 - outside / FADE, 0.0, 1.0);
        }

        /** 声音资源解析前 getVolume 不可用，按自身音量判断。 */
        boolean audible() {
            return volume > 0.0F;
        }
    }
}
