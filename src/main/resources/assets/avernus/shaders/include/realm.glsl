uniform isampler2D RealmColumns;
uniform ivec4 RealmBounds;
uniform ivec2 RealmTableOrigin;
uniform int RealmTableSize;
uniform ivec3 RealmSectionOrigin;
uniform int RealmMeshMode;
uniform int RealmStyle;
uniform vec3 ChunkOffset;
// 境内封闭：xy 为相对摄像机的圆心，z 为半径，z < 0 表示未封闭；A 为领域圆柱，B 为波面圆盘
uniform vec4 RealmEncloseA;
uniform vec4 RealmEncloseB;

ivec4 realm_column(ivec3 cell) {
    ivec2 p = cell.xz;
    if (any(lessThan(p, RealmBounds.xy)) || any(greaterThanEqual(p, RealmBounds.zw))) {
        return ivec4(0);
    }
    ivec2 uv = (p - RealmBounds.xy + RealmTableOrigin) % RealmTableSize;
    return texelFetch(RealmColumns, uv, 0);
}

// column.x 为境面层 H-1；其下一格 H-2 是遮住地下的底板层，真实方块从底板层起隐藏
bool realm_hidden(ivec3 cell, ivec4 column) {
    return column.y != 0 && (column.w != 0 || cell.y >= column.x - 1);
}

bool realm_floor(ivec3 cell, ivec4 column) {
    return column.y != 0 && column.w == 0 && cell.y == column.x - 1;
}

// 摄像机身处覆盖范围时，未被覆盖的方块列一律不画，露出后方的天空；按列中心判定，与境面列表逐列一致
bool realm_enclosed_out(vec3 position, vec3 normal) {
    if (RealmEncloseA.z < 0.0) return false;
    vec2 p = ChunkOffset.xz + floor(position.xz - normal.xz * 0.005) + 0.5;
    return length(p - RealmEncloseA.xy) > RealmEncloseA.z || length(p - RealmEncloseB.xy) > RealmEncloseB.z;
}

bool realm_discard(vec3 position, vec3 normal, float alpha) {
    if (realm_enclosed_out(position, normal)) return true;
    if (RealmMeshMode < 0) return false;
    ivec3 cell = RealmSectionOrigin + ivec3(floor(position - normal * 0.005));
    ivec4 column = realm_column(cell);
    if (RealmMeshMode == 0) return realm_hidden(cell, column);

    if (RealmMeshMode == 2 && (column.y != RealmStyle || (cell.y != column.x && !realm_floor(cell, column)))) return true;
    if (RealmMeshMode == 2 && abs(normal.y) > 0.5) return false;

    ivec3 side = RealmSectionOrigin + ivec3(floor(position + normal * 0.005));
    ivec4 adjacent = realm_column(side);
    if (RealmMeshMode == 1) {
        return realm_hidden(cell, column) || !realm_hidden(side, adjacent) ||
               (side.y == adjacent.x && adjacent.z == 1) || realm_floor(side, adjacent);
    }
    if (!realm_hidden(side, adjacent)) return alpha < 0.5;
    return realm_floor(side, adjacent) || side.y == adjacent.x && (adjacent.z == 1 || (column.z == 2 && adjacent.z == 2));
}
