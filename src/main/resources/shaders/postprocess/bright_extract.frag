#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uScene;
uniform float uThreshold;

void main() {
    vec4 color = texture(uScene, vUv);
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    float mask = smoothstep(uThreshold, uThreshold + 0.15, luma);
    FragColor = vec4(color.rgb * mask, color.a * mask);
}
