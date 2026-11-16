// 领域覆盖判定，与服务端 Realm#covers 逐格一致。坐标均相对相机所在方块格。
// RealmData 每个领域 8 项：波源 xz、波面半径、高度 H、中心 xz、半径、网格标签；按开启序号从新到旧。
// 顶点标记：255 真实网格，254 边界补面，其余为虚拟网格所属领域标签。
uniform int RealmCount;
uniform float RealmData[64];
uniform vec3 CameraFrac;

vec2 realm_cover(vec3 cell) {
    vec2 c = cell.xz + 0.5;
    for (int i = 0; i < RealmCount; i++) {
        int b = i * 8;
        float r = RealmData[b + 6];
        vec2 dc = c - vec2(RealmData[b + 4], RealmData[b + 5]);
        if (dot(dc, dc) > r * r) continue;
        float w = RealmData[b + 2];
        vec2 ds = c - vec2(RealmData[b], RealmData[b + 1]);
        if (w <= 0.0 || dot(ds, ds) > w * w) continue;
        return vec2(RealmData[b + 3], RealmData[b + 7]);
    }
    return vec2(1.0e9, -1.0);
}

bool realm_hidden(vec3 cell) {
    return cell.y >= realm_cover(cell).x - 1.0;
}

bool realm_discard(vec3 relPos, vec3 normal, float marker) {
    vec3 p = relPos + CameraFrac;
    vec3 cell = floor(p - normal * 0.01);
    if (marker > 254.5) return realm_hidden(cell);
    if (marker > 253.5) return realm_hidden(cell) || !realm_hidden(floor(p + normal * 0.01));
    vec2 cover = realm_cover(cell);
    return abs(cover.y - marker) > 0.5 || abs(cell.y - (cover.x - 1.0)) > 0.5;
}
