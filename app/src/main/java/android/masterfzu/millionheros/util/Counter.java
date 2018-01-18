package android.masterfzu.millionheros.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhengsiyuan on 2018/1/15.
 * 耗时记录器
 */
public class Counter {
    private static Map<String, Long> counter = new HashMap<String, Long>();

    public static void letsgo(String go) {
        counter.put(go, System.currentTimeMillis());
    }

    public static float spendS(String now) {
        if (counter.get(now) == null)
            return -1;

        long begin = counter.get(now);
        return (System.currentTimeMillis() - begin) / 1000.0f;
    }
}
