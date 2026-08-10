#version 330 core

in vec2 vUv;
out vec4 FragColor;

// 转场 shader：冻结旧场景(glitch) → 交叉淡入实时新场景。
// 输入：
//   uOld       触发瞬间截取的旧场景（截取一次，见 PostProcessPipeline）
//   uNew       实时新场景（sceneBuffer，逐帧最新）
//   uTime      秒
//   uIntensity 故障强度，0 = 原图（无任何故障）
//   uMix       0 = 旧场景，1 = 新场景（交叉淡入进度）
//   uAlpha     FBO 遮罩：0 = 完全显示实时画面，1 = 完全显示转场画面（用于 FBO 淡入/淡出，掩盖画质差异）
// 移植自 Shadertoy "Screen Glitch"（spectrum_offset / rand / mytrunc 保留原实现）。

uniform sampler2D uOld;
uniform sampler2D uNew;
uniform float uTime;
uniform float uIntensity;
uniform float uMix;
uniform float uAlpha;

float sat(float t) { return clamp(t, 0.0, 1.0); }

float remap(float t, float a, float b) { return sat((t - a) / (b - a)); }

float linterp(float t) { return sat(1.0 - abs(2.0 * t - 1.0)); }

vec3 spectrumOffset(float t) {
    vec3 ret;
    float lo = step(t, 0.5);
    float hi = 1.0 - lo;
    float w = linterp(remap(t, 1.0 / 6.0, 5.0 / 6.0));
    float negW = 1.0 - w;
    ret = vec3(lo, 1.0, hi) * vec3(negW, w, negW);
    return pow(ret, vec3(1.0 / 2.2));
}

float rand(vec2 n) {
    return fract(sin(dot(n.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

float mytrunc(float x, float levels) { return floor(x * levels) / levels; }
vec2 mytrunc(vec2 x, float levels) { return floor(x * levels) / levels; }

/** 对输入纹理做一次故障采样：色差采样(20 级) + 撕裂行偏移 + 垂直抖动；glitch = 0 时等于原图 */
vec4 glitchSample(sampler2D tex, vec2 uv, float time, float glitch) {
    float gnm = sat(glitch);
    float rnd0 = rand(mytrunc(vec2(time, time), 6.0));
    float r0 = sat((1.0 - gnm) * 0.7 + rnd0);
    float rnd1 = rand(vec2(mytrunc(uv.x, max(0.0001, 10.0 * r0)), time));
    float r1 = 0.5 - 0.5 * gnm + rnd1;
    r1 = 1.0 - max(0.0, (r1 < 1.0) ? r1 : 0.9999999);
    float rnd2 = rand(vec2(mytrunc(uv.y, max(0.0001, 40.0 * r1)), time));
    float r2 = sat(rnd2);
    float rnd3 = rand(vec2(mytrunc(uv.y, max(0.0001, 10.0 * r0)), time));
    float r3 = (1.0 - sat(rnd3 + 0.8)) - 0.1;

    float pxrnd = rand(uv + time);
    float ofs = 0.05 * r2 * glitch * (rnd0 > 0.5 ? 1.0 : -1.0);
    ofs += 0.5 * pxrnd * ofs;
    uv.y += 0.1 * r3 * glitch;

    const int NUM_SAMPLES = 20;
    const float RCP = 1.0 / float(NUM_SAMPLES);
    vec4 sum = vec4(0.0);
    vec3 wsum = vec3(0.0);
    for (int i = 0; i < NUM_SAMPLES; ++i) {
        float t = float(i) * RCP;
        vec2 suv = uv;
        suv.x = sat(suv.x + ofs * t);
        vec4 sc = texture(tex, suv);
        vec3 s = spectrumOffset(t);
        sc.rgb *= s;
        sum += sc;
        wsum += s;
    }
    sum.rgb /= wsum;
    sum.a *= RCP;
    return sum;
}

void main() {
    float time = mod(uTime * 100.0, 32.0) / 110.0;

    // 旧场景过故障；新场景保持干净
    vec4 oldG = glitchSample(uOld, vUv, time, uIntensity);
    vec4 base = mix(oldG, texture(uNew, vUv), uMix);
    // FBO 遮罩：0 = 实时画面（无画质差异突兀），1 = 转场画面
    vec4 col = mix(base, texture(uNew, vUv), 1.0 - uAlpha);
    FragColor = vec4(col.rgb, 1.0);
}
