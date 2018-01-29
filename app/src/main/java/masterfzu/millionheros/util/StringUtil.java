package masterfzu.millionheros.util;

/**
 * Created by zhengsiyuan on 2018/1/16.
 */

public class StringUtil {
    public static boolean isEmpty(String s) {
        if (s == null || s.isEmpty() || s.trim().isEmpty())
            return true;

        return false;
    }
}
