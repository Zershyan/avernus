uniform isampler2D RealmColumns;
uniform ivec4 RealmBounds;
uniform ivec2 RealmTableOrigin;
uniform int RealmTableSize;
uniform ivec3 RealmSectionOrigin;
uniform int RealmMeshMode;
uniform int RealmStyle;

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

bool realm_discard(vec3 position, vec3 normal, float alpha) {
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
