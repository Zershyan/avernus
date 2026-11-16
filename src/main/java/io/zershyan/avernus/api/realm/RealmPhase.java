package io.zershyan.avernus.api.realm;

/** 领域演出阶段，由服务端时间线推导。 */
public enum RealmPhase {
    /** 镜子飞出或升起，尚未碎裂，世界未改变。 */
    OPENING,
    /** 镜子已碎，波纹向外展开。 */
    EXPANDING,
    /** 波纹已覆盖完整领域。 */
    ACTIVE,
    /** 收到关闭请求后，世界随波纹反向恢复，镜子随后重组飞回。 */
    CLOSING
}
