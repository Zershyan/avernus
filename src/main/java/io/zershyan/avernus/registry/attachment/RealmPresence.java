package io.zershyan.avernus.registry.attachment;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 实体当前受影响的领域，以及本次经历中尚未消耗的首次落地保护。 */
public record RealmPresence(@Nullable UUID realm, boolean fallGuard, long checkedGeometry) {
    public static final RealmPresence NONE = new RealmPresence(null, false, 0L);
}
