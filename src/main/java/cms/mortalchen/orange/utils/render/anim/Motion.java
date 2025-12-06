package cms.mortalchen.orange.utils.render.anim;

/**
 * Motion动画引擎
 * <p>
 * 基于时间戳的精确插值动画系统，提供流畅、可中断、可控制的动画效果。
 * 支持丰富的缓动曲线和灵活的动画控制器，适合游戏、UI动画等场景。
 * </p>
 *
 * @author Mortal
 * @version 1.0
 * @since 2025
 */
public class Motion {

    /**
     * 动画控制器
     * <p>
     * 动画系统的核心组件，管理单个数值的动画状态。
     * 支持动画中断、平滑过渡和多种缓动曲线。
     * </p>
     *
     * <p><b>使用方式：</b></p>
     * <ol>
     *   <li><b>定义</b>: {@code private final Motion.Controller myAnim = new Motion.Controller(初始值);}</li>
     *   <li><b>更新</b>: {@code myAnim.to(目标值, 持续时间ms, 曲线);}</li>
     *   <li><b>获取</b>: {@code float value = myAnim.get();}</li>
     * </ol>
     *
     * <p><b>特性：</b></p>
     * <ul>
     *   <li><b>平滑过渡</b>：当前动画未完成时设置新目标，会从当前位置平滑过渡</li>
     *   <li><b>自动完成</b>：动画结束后自动保持在目标值</li>
     *   <li><b>精确计时</b>：基于系统时间戳，不受帧率波动影响</li>
     *   <li><b>内存友好</b>：轻量级设计，无外部依赖</li>
     * </ul>
     */
    public static class Controller {
        /** 动画起始值 */
        private double startValue;
        /** 动画目标值 */
        private double targetValue;
        /** 动画当前计算值 */
        private double currentValue;

        /** 动画开始时间戳（毫秒） */
        private long startTime;
        /** 动画总持续时间（毫秒），0表示无动画 */
        private long duration;
        /** 当前使用的缓动曲线 */
        private Easing easing;

        /**
         * 创建动画控制器
         *
         * @param initialValue 初始数值，动画将从该值开始
         *
         * <p><b>示例：</b></p>
         * <pre>
         * Motion.Controller anim = new Motion.Controller(0.0); // 创建从0开始的动画控制器
         * <br/>
         * Motion.Controller uiAnim = new Motion.Controller(currentPosition); // 创建从当前位置开始的UI元素动画
         * </pre>
         */
        public Controller(double initialValue) {
            this.startValue = initialValue;
            this.targetValue = initialValue;
            this.currentValue = initialValue;
            this.startTime = System.currentTimeMillis();
            this.duration = 0; // 0 表示无动画直接到位
            this.easing = Easing.LINEAR;
        }

        /**
         * 重置动画到起始状态
         * <p>
         * 将当前值重置为起始值，并重新开始计时。
         * 用于重新开始当前动画或恢复初始状态。
         * </p>
         *
         */
        public void reset() {
            this.currentValue = startValue;
            this.startTime = System.currentTimeMillis();
        }

        /**
         * 设置新的动画目标
         * <p>
         * 从当前位置平滑过渡到指定目标值。
         * 如果当前动画尚未结束，会无缝过渡到新动画，避免数值跳变。
         * </p>
         *
         * @param target 目标数值，动画将移动到的最终值
         * @param duration 动画持续时间，单位：毫秒
         *                 建议范围：16ms（60FPS）到5000ms（长动画）
         * @param easing 缓动曲线，控制动画的加速度变化
         *               例如：{@code Motion.Easing.SINE_OUT}
         */
        public void to(double target, long duration, Easing easing) {
            if (target == this.targetValue) {
                return; // 目标相同，无需改变
            }

            // 记录当前的实际位置作为新的起点，防止跳变
            this.startValue = get();
            this.targetValue = target;
            this.startTime = System.currentTimeMillis();
            this.duration = Math.max(0, duration);
            this.easing = easing;
        }

        /**
         * 获取当前计算出的动画数值
         * <p>
         * 基于当前时间和缓动曲线计算出的插值结果。
         * 应该在渲染循环中每帧调用以获取最新值。
         * </p>
         *
         * @return 当前动画数值（double类型）
         */
        public double get() {
            if (duration == 0) {
                this.currentValue = targetValue;
                return targetValue;
            }

            long timePassed = System.currentTimeMillis() - startTime;

            // 动画已结束
            if (timePassed >= duration) {
                this.currentValue = targetValue;
                return targetValue;
            }

            // 计算进度 (0.0 ~ 1.0)
            double progress = (double) timePassed / duration;

            // 应用缓动函数计算当前值
            this.currentValue = easing.animate(startValue, targetValue, progress);
            return this.currentValue;
        }

        /**
         * 直接设置当前值
         * <p>
         * 立即将动画值设置为指定值，不经过动画过渡。
         * 会中断当前正在进行的动画。
         * </p>
         *
         * @param currentValue 要设置的当前值
         */
        public void set(double currentValue) {
            this.currentValue = currentValue;
            this.startValue = currentValue;
            this.targetValue = currentValue;
            this.startTime = System.currentTimeMillis();
            this.duration = 0;
        }

        /**
         * 获取 float 类型的数值（便捷方法）
         * <p>
         * 与 {@link #get()} 功能相同，但返回float类型。
         * 适用于需要float参数的API或减少类型转换的场景。
         * </p>
         *
         * @return 当前动画数值（float类型）
         */
        public float getF() {
            return (float) get();
        }

        /**
         * 检查动画是否已完成
         * <p>
         * 基于当前时间和动画持续时间判断动画是否已经结束。
         * 动画完成后，{@link #get()} 方法将直接返回目标值。
         * </p>
         *
         * @return true表示动画已完成，false表示动画仍在进行中
         */
        public boolean isFinished() {
            return System.currentTimeMillis() - startTime >= duration;
        }

        /**
         * 获取当前动画进度
         * <p>
         * 返回当前动画的完成进度，范围0.0到1.0。
         * 即使动画已完成，此方法也会返回确切的进度值。
         * </p>
         *
         * @return 动画进度（0.0=刚开始，1.0=已完成）
         */
        public double getProgress() {
            if (duration == 0) return 1.0;
            long timePassed = System.currentTimeMillis() - startTime;
            return Math.min(1.0, (double) timePassed / duration);
        }

        /**
         * 获取动画目标值
         *
         * @return 当前动画的目标值
         */
        public double getTarget() {
            return targetValue;
        }

        /**
         * 获取动画起始值
         *
         * @return 当前动画的起始值
         */
        public double getStart() {
            return startValue;
        }

        /**
         * 获取动画剩余时间
         *
         * @return 动画剩余时间（毫秒），0表示已完成
         */
        public long getRemainingTime() {
            if (duration == 0) return 0;
            long timePassed = System.currentTimeMillis() - startTime;
            return Math.max(0, duration - timePassed);
        }
    }

    /**
     * 缓动曲线枚举
     * <p>
     * 提供10大类30多种缓动函数，控制动画的加速度变化。
     * 每种缓动曲线都有独特的运动特性和视觉效果。
     * </p>
     *
     * <p><b>缓动曲线分类：</b></p>
     * <ol>
     *   <li><b>LINEAR</b> - 线性匀速运动</li>
     *   <li><b>SINE</b> - 正弦曲线，平滑自然</li>
     *   <li><b>QUAD</b> - 二次方曲线，中等加速</li>
     *   <li><b>CUBIC</b> - 三次方曲线，强烈加速</li>
     *   <li><b>QUART</b> - 四次方曲线，非常强烈</li>
     *   <li><b>QUINT</b> - 五次方曲线，极端加速</li>
     *   <li><b>EXPO</b> - 指数曲线，爆发式加速</li>
     *   <li><b>CIRC</b> - 圆形曲线，圆弧运动</li>
     *   <li><b>BACK</b> - 回弹预备，轻微过冲</li>
     *   <li><b>ELASTIC</b> - 弹性曲线，橡皮筋效果</li>
     *   <li><b>BOUNCE</b> - 弹跳曲线，球体弹跳</li>
     * </ol>
     *
     * <p><b>命名规则：</b></p>
     * <ul>
     *   <li><b>IN</b> - 缓入：开始慢，逐渐加速</li>
     *   <li><b>OUT</b> - 缓出：开始快，逐渐减速</li>
     *   <li><b>IN_OUT</b> - 缓入缓出：开始和结束都慢，中间快</li>
     * </ul>
     *
     * @see <a href="https://easings.net">easings.net - 缓动函数可视化</a>
     */
    public enum Easing {
        /**
         * 线性匀速运动
         * <p>最简单的缓动函数，保持恒定速度。</p>
         * <p><b>适用场景：</b>机械运动、匀速移动、进度条</p>
         */
        LINEAR {
            @Override
            public double ease(double t) {
                return t;
            }
        },

        // =================================
        // 1. SINE (正弦曲线)
        // =================================

        /**
         * 正弦缓入
         * <p>基于余弦函数的缓入效果，开始非常平滑。</p>
         * <p><b>数学公式：</b> 1 - cos(t × π ÷ 2)</p>
         * <p><b>适用场景：</b> 柔和启动、自然进入</p>
         */
        SINE_IN {
            @Override
            public double ease(double t) {
                return 1 - Math.cos((t * Math.PI) / 2);
            }
        },

        /**
         * 正弦缓出
         * <p>基于正弦函数的缓出效果，结束非常平滑。</p>
         * <p><b>数学公式：</b> sin(t × π ÷ 2)</p>
         * <p><b>适用场景：</b> UI元素退出、自然停止</p>
         */
        SINE_OUT {
            @Override
            public double ease(double t) {
                return Math.sin((t * Math.PI) / 2);
            }
        },

        /**
         * 正弦缓入缓出
         * <p>完整的正弦波运动，开始和结束都很平滑。</p>
         * <p><b>数学公式：</b> -(cos(π × t) - 1) ÷ 2</p>
         * <p><b>适用场景：</b> 完整动画周期、平滑过渡</p>
         */
        SINE_IN_OUT {
            @Override
            public double ease(double t) {
                return -(Math.cos(Math.PI * t) - 1) / 2;
            }
        },

        // =================================
        // 2. QUAD (二次方 t²)
        // =================================

        /**
         * 二次缓入
         * <p>基于二次方的缓入效果，中等加速。</p>
         * <p><b>数学公式：</b> t²</p>
         * <p><b>适用场景：</b> 中等加速启动、游戏角色移动</p>
         */
        QUAD_IN {
            @Override
            public double ease(double t) {
                return t * t;
            }
        },

        /**
         * 二次缓出
         * <p>基于二次方的缓出效果，中等减速。</p>
         * <p><b>数学公式：</b> 1 - (1 - t)²</p>
         * <p><b>适用场景：</b> 中等减速停止、UI元素运动</p>
         */
        QUAD_OUT {
            @Override
            public double ease(double t) {
                return 1 - (1 - t) * (1 - t);
            }
        },

        /**
         * 二次缓入缓出
         * <p>基于二次方的完整缓动，对称加速减速。</p>
         * <p><b>数学公式：</b> t < 0.5 ? 2t² : 1 - (-2t + 2)² ÷ 2</p>
         * <p><b>适用场景：</b> 对称运动、标准动画</p>
         */
        QUAD_IN_OUT {
            @Override
            public double ease(double t) {
                return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            }
        },

        // =================================
        // 3. CUBIC (三次方 t³)
        // =================================

        /**
         * 三次缓入
         * <p>基于三次方的缓入效果，较强加速。</p>
         * <p><b>数学公式：</b> t³</p>
         * <p><b>适用场景：</b> 强调启动、快速加速</p>
         */
        CUBIC_IN {
            @Override
            public double ease(double t) {
                return t * t * t;
            }
        },

        /**
         * 三次缓出
         * <p>基于三次方的缓出效果，较强减速。</p>
         * <p><b>数学公式：</b> 1 - (1 - t)³</p>
         * <p><b>适用场景：</b> 强调停止、快速减速</p>
         */
        CUBIC_OUT {
            @Override
            public double ease(double t) {
                return 1 - Math.pow(1 - t, 3);
            }
        },

        /**
         * 三次缓入缓出
         * <p>基于三次方的完整缓动，强调加速和减速。</p>
         * <p><b>数学公式：</b> t < 0.5 ? 4t³ : 1 - (-2t + 2)³ ÷ 2</p>
         * <p><b>适用场景：</b> 强调运动、游戏特效</p>
         */
        CUBIC_IN_OUT {
            @Override
            public double ease(double t) {
                return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
            }
        },

        // =================================
        // 4. QUART (四次方 t⁴)
        // =================================

        /**
         * 四次缓入
         * <p>基于四次方的缓入效果，非常强烈的加速。</p>
         * <p><b>数学公式：</b> t⁴</p>
         * <p><b>适用场景：</b> 爆发式启动、强调开始</p>
         */
        QUART_IN {
            @Override
            public double ease(double t) {
                return t * t * t * t;
            }
        },

        /**
         * 四次缓出
         * <p>基于四次方的缓出效果，非常强烈的减速。</p>
         * <p><b>数学公式：</b> 1 - (1 - t)⁴</p>
         * <p><b>适用场景：</b> 紧急停止、强调结束</p>
         */
        QUART_OUT {
            @Override
            public double ease(double t) {
                return 1 - Math.pow(1 - t, 4);
            }
        },

        /**
         * 四次缓入缓出
         * <p>基于四次方的完整缓动，极端加速减速。</p>
         * <p><b>数学公式：</b> t < 0.5 ? 8t⁴ : 1 - (-2t + 2)⁴ ÷ 2</p>
         * <p><b>适用场景：</b> 极端运动、特殊效果</p>
         */
        QUART_IN_OUT {
            @Override
            public double ease(double t) {
                return t < 0.5 ? 8 * t * t * t * t : 1 - Math.pow(-2 * t + 2, 4) / 2;
            }
        },

        // =================================
        // 5. QUINT (五次方 t⁵)
        // =================================

        /**
         * 五次缓入
         * <p>基于五次方的缓入效果，极端加速。</p>
         * <p><b>数学公式：</b> t⁵</p>
         * <p><b>适用场景：</b> 极限启动、瞬间加速</p>
         */
        QUINT_IN {
            @Override
            public double ease(double t) {
                return t * t * t * t * t;
            }
        },

        /**
         * 五次缓出
         * <p>基于五次方的缓出效果，极端减速。</p>
         * <p><b>数学公式：</b> 1 - (1 - t)⁵</p>
         * <p><b>适用场景：</b> 极限停止、瞬间减速</p>
         */
        QUINT_OUT {
            @Override
            public double ease(double t) {
                return 1 - Math.pow(1 - t, 5);
            }
        },

        /**
         * 五次缓入缓出
         * <p>基于五次方的完整缓动，最极端的加速减速。</p>
         * <p><b>数学公式：</b> t < 0.5 ? 16t⁵ : 1 - (-2t + 2)⁵ ÷ 2</p>
         * <p><b>适用场景：</b> 极限动画、夸张效果</p>
         */
        QUINT_IN_OUT {
            @Override
            public double ease(double t) {
                return t < 0.5 ? 16 * t * t * t * t * t : 1 - Math.pow(-2 * t + 2, 5) / 2;
            }
        },

        // =================================
        // 6. EXPO (指数级)
        // =================================

        /**
         * 指数缓入
         * <p>基于指数函数的缓入效果，开始极其缓慢然后爆发。</p>
         * <p><b>数学公式：</b> t = 0 ? 0 : 2^(10t - 10)</p>
         * <p><b>适用场景：</b> 戏剧性入场、爆炸效果</p>
         */
        EXPO_IN {
            @Override
            public double ease(double t) {
                return t == 0 ? 0 : Math.pow(2, 10 * t - 10);
            }
        },

        /**
         * 指数缓出
         * <p>基于指数函数的缓出效果，结束极其缓慢。</p>
         * <p><b>数学公式：</b> t = 1 ? 1 : 1 - 2^(-10t)</p>
         * <p><b>适用场景：</b> 渐隐效果、慢慢消失</p>
         */
        EXPO_OUT {
            @Override
            public double ease(double t) {
                return t == 1 ? 1 : 1 - Math.pow(2, -10 * t);
            }
        },

        /**
         * 指数缓入缓出
         * <p>基于指数函数的完整缓动，开始和结束都极其缓慢。</p>
         * <p><b>数学公式：</b> 分段指数函数</p>
         * <p><b>适用场景：</b> 戏剧性转场、电影效果</p>
         */
        EXPO_IN_OUT {
            @Override
            public double ease(double t) {
                return t == 0 ? 0 : t == 1 ? 1 : t < 0.5
                        ? Math.pow(2, 20 * t - 10) / 2
                        : (2 - Math.pow(2, -20 * t + 10)) / 2;
            }
        },

        // =================================
        // 7. CIRC (圆形 / 圆弧)
        // =================================

        /**
         * 圆形缓入
         * <p>基于圆形方程的缓入效果，开始缓慢加速。</p>
         * <p><b>数学公式：</b> 1 - √(1 - t²)</p>
         * <p><b>适用场景：</b> 弧形进入、圆周运动</p>
         */
        CIRC_IN {
            @Override
            public double ease(double t) {
                return 1 - Math.sqrt(1 - Math.pow(t, 2));
            }
        },

        /**
         * 圆形缓出
         * <p>基于圆形方程的缓出效果，结束缓慢减速。</p>
         * <p><b>数学公式：</b> √(1 - (t - 1)²)</p>
         * <p><b>适用场景：</b> 弧形退出、圆周停止</p>
         */
        CIRC_OUT {
            @Override
            public double ease(double t) {
                return Math.sqrt(1 - Math.pow(t - 1, 2));
            }
        },

        /**
         * 圆形缓入缓出
         * <p>基于圆形方程的完整缓动，圆弧形运动。</p>
         * <p><b>数学公式：</b> 分段圆形函数</p>
         * <p><b>适用场景：</b> 完整圆周运动、弧形轨迹</p>
         */
        CIRC_IN_OUT {
            @Override
            public double ease(double t) {
                return t < 0.5
                        ? (1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2
                        : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
            }
        },

        // =================================
        // 8. BACK (回弹预备)
        // =================================

        /**
         * 回弹缓入
         * <p>轻微过冲然后回弹的缓入效果。</p>
         * <p><b>数学公式：</b> 三次函数带过冲</p>
         * <p><b>适用场景：</b> 过冲入场、预备动作</p>
         */
        BACK_IN {
            @Override
            public double ease(double t) {
                double c1 = 1.70158;
                double c3 = c1 + 1;
                return c3 * t * t * t - c1 * t * t;
            }
        },

        /**
         * 回弹缓出
         * <p>轻微过冲然后回弹的缓出效果。</p>
         * <p><b>数学公式：</b> 三次函数带过冲</p>
         * <p><b>适用场景：</b> 过冲退场、惯性停止</p>
         */
        BACK_OUT {
            @Override
            public double ease(double t) {
                double c1 = 1.70158;
                double c3 = c1 + 1;
                return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
            }
        },

        /**
         * 回弹缓入缓出
         * <p>完整回弹效果，开始和结束都有过冲。</p>
         * <p><b>数学公式：</b> 分段回弹函数</p>
         * <p><b>适用场景：</b> 完整回弹动画、惯性运动</p>
         */
        BACK_IN_OUT {
            @Override
            public double ease(double t) {
                double c1 = 1.70158;
                double c2 = c1 * 1.525;
                return t < 0.5
                        ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                        : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
            }
        },

        // =================================
        // 9. ELASTIC (弹性/橡皮筋)
        // =================================

        /**
         * 弹性缓入
         * <p>橡皮筋拉伸效果的缓入，多次振荡。</p>
         * <p><b>数学公式：</b> 带正弦振荡的指数函数</p>
         * <p><b>适用场景：</b> 弹簧入场、弹性启动</p>
         */
        ELASTIC_IN {
            @Override
            public double ease(double t) {
                double c4 = (2 * Math.PI) / 3;
                return t == 0 ? 0 : t == 1 ? 1 : -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * c4);
            }
        },

        /**
         * 弹性缓出
         * <p>橡皮筋释放效果的缓出，多次振荡。</p>
         * <p><b>数学公式：</b> 带正弦振荡的指数函数</p>
         * <p><b>适用场景：</b> 弹簧退场、弹性停止</p>
         */
        ELASTIC_OUT {
            @Override
            public double ease(double t) {
                double c4 = (2 * Math.PI) / 3;
                return t == 0 ? 0 : t == 1 ? 1 : Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
            }
        },

        /**
         * 弹性缓入缓出
         * <p>完整橡皮筋效果，开始和结束都振荡。</p>
         * <p><b>数学公式：</b> 分段弹性函数</p>
         * <p><b>适用场景：</b> 完整弹性动画、弹簧运动</p>
         */
        ELASTIC_IN_OUT {
            @Override
            public double ease(double t) {
                double c5 = (2 * Math.PI) / 4.5;
                return t == 0 ? 0 : t == 1 ? 1 : t < 0.5
                        ? -(Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * c5)) / 2
                        : (Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * c5)) / 2 + 1;
            }
        },

        // =================================
        // 10. BOUNCE (弹跳)
        // =================================

        /**
         * 弹跳缓入
         * <p>球体落地弹跳效果的缓入。</p>
         * <p><b>数学公式：</b> 1 - BOUNCE_OUT.ease(1 - t)</p>
         * <p><b>适用场景：</b> 弹跳入场、球体进入</p>
         */
        BOUNCE_IN {
            @Override
            public double ease(double t) {
                return 1 - BOUNCE_OUT.ease(1 - t);
            }
        },

        /**
         * 弹跳缓出
         * <p>球体落地弹跳效果的缓出。</p>
         * <p><b>数学公式：</b> 分段抛物线函数</p>
         * <p><b>适用场景：</b> 弹跳退场、球体停止</p>
         */
        BOUNCE_OUT {
            @Override
            public double ease(double t) {
                double n1 = 7.5625;
                double d1 = 2.75;
                if (t < 1 / d1) return n1 * t * t;
                else if (t < 2 / d1) return n1 * (t -= 1.5 / d1) * t + 0.75;
                else if (t < 2.5 / d1) return n1 * (t -= 2.25 / d1) * t + 0.9375;
                else return n1 * (t -= 2.625 / d1) * t + 0.984375;
            }
        },

        /**
         * 弹跳缓入缓出
         * <p>完整球体弹跳效果，开始和结束都弹跳。</p>
         * <p><b>数学公式：</b> 分段弹跳函数</p>
         * <p><b>适用场景：</b> 完整弹跳动画、球体运动</p>
         */
        BOUNCE_IN_OUT {
            @Override
            public double ease(double t) {
                return t < 0.5
                        ? (1 - BOUNCE_OUT.ease(1 - 2 * t)) / 2
                        : (1 + BOUNCE_OUT.ease(2 * t - 1)) / 2;
            }
        };

        // =================================
        // 公共方法
        // =================================

        /**
         * 缓动函数核心计算方法
         * <p>
         * 输入标准化时间（0.0到1.0），返回缓动后的进度值。
         * 子类必须实现此方法来定义具体的缓动曲线。
         * </p>
         *
         * @param t 标准化时间，范围[0.0, 1.0]
         *          0.0表示动画开始，1.0表示动画结束
         * @return 缓动后的进度值，范围[0.0, 1.0]
         */
        public abstract double ease(double t);

        /**
         * 应用缓动函数计算动画值
         * <p>
         * 基于起始值、目标值和进度计算当前动画值。
         * 自动处理进度边界并应用缓动函数。
         * </p>
         *
         * @param start 动画起始值
         * @param target 动画目标值
         * @param progress 动画进度，范围[0.0, 1.0]
         * @return 缓动后的当前值
         *
         * <p><b>数学公式：</b></p>
         * <pre>
         * value = start + (target - start) × ease(progress)
         * </pre>
         *
         * <p><b>示例：</b></p>
         * <pre>
         * // 从0到100，进度0.5，使用线性缓动
         * double value = Easing.LINEAR.animate(0, 100, 0.5);
         * // 结果：50.0
         *
         * // 从0到100，进度0.5，使用二次缓入
         * double value = Easing.QUAD_IN.animate(0, 100, 0.5);
         * // 结果：25.0 (0.5² = 0.25)
         * </pre>
         */
        public double animate(double start, double target, double progress) {
            // 确保进度在有效范围内
            if (progress < 0) progress = 0;
            if (progress > 1) progress = 1;

            // 应用缓动函数计算插值
            return start + (target - start) * ease(progress);
        }

        /**
         * 应用缓动函数计算动画值（float版本）
         * <p>
         * 与{@link #animate(double, double, double)}功能相同，
         * 但使用float类型参数，减少类型转换开销。
         * </p>
         *
         * @param start 动画起始值（float）
         * @param target 动画目标值（float）
         * @param progress 动画进度，范围[0.0f, 1.0f]
         * @return 缓动后的当前值（float）
         */
        public float animate(float start, float target, float progress) {
            return (float) animate(start, target, (double) progress);
        }
    }
}