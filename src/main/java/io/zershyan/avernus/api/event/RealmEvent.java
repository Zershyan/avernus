package io.zershyan.avernus.api.event;

import io.zershyan.avernus.api.realm.RealmView;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/** 领域事件基类，于服务端主线程在 NeoForge 游戏总线发布。 */
public abstract class RealmEvent extends Event {
    private final ServerLevel level;
    private final RealmView realm;

    protected RealmEvent(ServerLevel level, RealmView realm) {
        this.level = level;
        this.realm = realm;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public RealmView getRealm() {
        return realm;
    }

    /** 领域已创建，开始开场演出。 */
    public static class Opened extends RealmEvent {
        public Opened(ServerLevel level, RealmView realm) {
            super(level, realm);
        }
    }

    /** 领域已接受关闭请求，开始收束；冷却已结算。 */
    public static class Closing extends RealmEvent {
        public Closing(ServerLevel level, RealmView realm) {
            super(level, realm);
        }
    }

    /** 领域已彻底移除；emergency 为真表示死亡、下线、跨维度等立即撤销。 */
    public static class Removed extends RealmEvent {
        private final boolean emergency;

        public Removed(ServerLevel level, RealmView realm, boolean emergency) {
            super(level, realm);
            this.emergency = emergency;
        }

        public boolean isEmergency() {
            return emergency;
        }
    }
}
