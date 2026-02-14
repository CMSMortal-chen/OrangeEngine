package cms.mortalchen.orange.utils.math;

import java.util.*;

public class RandomUtil {
    public static String string = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String randomString(final int length) {
        return ">"+(random(length, string))+"<";
    }

    public static String randomStringRaw(final int length) {
        return (random(length, string));
    }

    public static String random(final int length, final String chars) {
        return random(length, chars.toCharArray());
    }

    public static String random(final int length, final char[] chars) {
        final StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < length; i++)
            stringBuilder.append(chars[new Random().nextInt(chars.length)]);
        return stringBuilder.toString();
    }

    public static long randomDelay(final int minDelay, final int maxDelay) {
        return nextInt(minDelay, maxDelay);
    }

    public static int nextInt(int startInclusive, int endExclusive) {
        return endExclusive - startInclusive <= 0 ? startInclusive : startInclusive + new Random().nextInt(endExclusive - startInclusive);
    }

    public static float randFloat(float min, float max) {
        Random random = new Random();
        float range = max - min;
        float scaled = random.nextFloat() * range;
        if (scaled > max) {
            scaled = max;
        }
        float shifted = scaled + min;

        if (shifted > max) {
            shifted = max;
        }
        return shifted;
    }

    public static boolean nextBoolean() {
        return new Random().nextInt(100) >= 50;
    }

    /**
     * 随机打乱 Deque 中元素的顺序（原地修改）
     * @param deque 需要打乱顺序的双端队列，不可为 null
     * @throws NullPointerException 如果 deque 为 null
     */
    public static void shuffle(Deque<?> deque) {
        // 将 Deque 转换为 List 以便使用 Collections.shuffle()
        List<Object> list = new ArrayList<>(deque);
        Collections.shuffle(list);

        // 清空原队列并重新添加打乱后的元素
        deque.clear();
        for (Object element : list) {
            if (element != null) {
                // 类型安全转换（通过泛型擦除检查）
                @SuppressWarnings("unchecked")
                Deque<Object> typedDeque = (Deque<Object>) deque;
                typedDeque.add(element);
            }
        }
    }
}
