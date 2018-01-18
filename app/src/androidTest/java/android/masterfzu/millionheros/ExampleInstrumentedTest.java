package android.masterfzu.millionheros;

import android.content.Context;
import android.masterfzu.millionheros.hint.QandA;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("android.masterfzu.millionheros", appContext.getPackageName());
    }

    @Test
    public void testJson() throws Exception {
        String result = "{\"log_id\": 8816071367637002938, \"words_result_num\": 5, \"words_result\": [{\"words\": \"《火星情报局》第三季主题\"}, {\"words\": \"曲是?\"}, {\"words\": \"《再见18岁》\"}, {\"words\": \"《火星人来过》\"}, {\"words\": \"《火星情报局》\"}]}";
        JSONObject jsonObject = new JSONObject(result);
        System.out.println(jsonObject.getJSONArray("words_result").length());
    }

    @Test
    public void testQandA() throws Exception {
        String result = "{\"log_id\": 8816071367637002938, \"words_result_num\": 5, \"words_result\": [{\"words\": \"《火星情报局》第三季主题\"}, {\"words\": \"曲是?\"}, {\"words\": \"《再见18岁》\"}, {\"words\": \"《火星人来过》\"}, {\"words\": \"《火星情报局》\"}]}";
        QandA qa = QandA.format(result);
        Log.w("TEST", qa.toString());
        assertNotNull(qa);
    }


}

