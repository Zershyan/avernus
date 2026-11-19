#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec3 viewPos;
out float shade;
out vec4 glow;
out vec2 texCoord0;
out vec3 bary;
flat out ivec2 state;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    viewPos = view.xyz;
    vertexDistance = fog_distance(Position, FogShape);
    shade = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.0)).r;
    glow = Color;
    texCoord0 = UV0;
    vec2 b = vec2(UV2) / 255.0;
    bary = vec3(b, 1.0 - b.x - b.y);
    state = UV1;
}
