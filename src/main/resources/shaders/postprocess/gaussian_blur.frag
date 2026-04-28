#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uScene;
uniform vec2 uTexelSize;
uniform vec2 uDirection;
uniform vec2 uRegionPosPx;
uniform vec2 uRegionSizePx;
uniform float uRadius;

float sCurve(float x) {
    x = x * 2.0 - 1.0;
    return -x * abs(x) * 0.5 + x + 0.5;
}

bool inRegion(vec2 pixelPos, vec2 regionPos, vec2 regionSize) {
    return pixelPos.x >= regionPos.x &&
           pixelPos.y >= regionPos.y &&
           pixelPos.x <= regionPos.x + regionSize.x &&
           pixelPos.y <= regionPos.y + regionSize.y;
}

void main() {
    vec4 original = texture(uScene, vUv);
    vec2 pixelPos = vUv / uTexelSize;

    if (!inRegion(pixelPos, uRegionPosPx, uRegionSizePx)) {
        FragColor = original;
        return;
    }

    const int MAX_RADIUS = 32;
    int radius = int(clamp(uRadius, 1.0, float(MAX_RADIUS)));
    float radiusF = float(radius);

    vec3 acc = vec3(0.0);
    float divisor = 0.0;

    for (int i = -MAX_RADIUS; i <= MAX_RADIUS; i++) {
        if (abs(i) > radius) {
            continue;
        }

        float w = sCurve(1.0 - (abs(float(i)) / radiusF));
        vec2 sampleUv = vUv + (uDirection * uTexelSize * float(i));
        acc += texture(uScene, sampleUv).rgb * w;
        divisor += w;
    }

    if (divisor <= 0.00001) {
        FragColor = original;
        return;
    }

    FragColor = vec4(acc / divisor, original.a);
}
