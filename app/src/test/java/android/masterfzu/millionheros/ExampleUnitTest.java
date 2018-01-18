package android.masterfzu.millionheros;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testPureQ() {
        String s = "19. \"梨花带雨\"是用来形容哪个美女的？";
        String r = "";

        if ((isNum(s.charAt(0)) && s.charAt(1) == '.'))
            r = s.substring(3);
        else if (isNum(s.charAt(1)) && s.charAt(2) == '.')
            r = s.substring(4);

        System.out.println(r);
    }

    private boolean isNum(char c) {
        if (c >='0'&& c <='9')
            return true;

        return false;
    }
}