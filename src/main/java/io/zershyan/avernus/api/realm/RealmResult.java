package io.zershyan.avernus.api.realm;

/** 服务端开关领域的结果；失败时不会创建新领域。 */
public enum RealmResult {
    OPENED,
    CLOSING,
    ALREADY_CLOSING,
    COOLDOWN,
    INVALID_FORM,
    INVALID_SUBJECT
}
