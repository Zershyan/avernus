package io.zershyan.avernus.registry.attachment;

import java.util.Set;
import java.util.UUID;

/** 幻化领域内，该生物被哪些玩家先行攻击而获得的反击许可；仅对记录时的领域有效。 */
public record IllusionPermits(UUID realm, Set<UUID> players) {
    public boolean allows(UUID realmId, UUID player) {
        return realm.equals(realmId) && players.contains(player);
    }
}
