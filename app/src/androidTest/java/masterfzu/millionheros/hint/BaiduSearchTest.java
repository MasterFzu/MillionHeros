package masterfzu.millionheros.hint;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

/**
 * Created by zhengsiyuan on 2018/1/16.
 */
@RunWith(AndroidJUnit4.class)
public class BaiduSearchTest {

    @Test
    public void testQandA() throws Exception {
        String result = "{\"log_id\": 8816071367637002938, \"words_result_num\": 5, \"words_result\": [{\"words\": \"《火星情报局》第三季主题\"}, {\"words\": \"曲是?\"}, {\"words\": \"《再见18岁》\"}, {\"words\": \"《火星人来过》\"}, {\"words\": \"《火星情报局》\"}]}";
        QandA qa = QandA.format(result);
        Log.w("TEST", qa.toString());
        assertNotNull(qa);

        BaiduSearch.ResultSum rs = BaiduSearch.searchResult(qa);
        Log.w("TEST", rs.toString());

        String s = BaiduSearch.prettyOut(qa, rs);
        Log.w("TEST", s);
    }

    @Test
    public void testSearchResult() throws Exception {

        String result = "{\"log_id\": 3842398351031184515, \"words_result_num\": 5, \"words_result\": [{\"words\": \"3歌曲《老大》里,布瑞吉想要\"}, {\"words\": \"什么车\"}, {\"words\": \"法拉利\"}, {\"words\": \"宾利\"}, {\"words\": \"保时捷\"}]}";
        QandA qa = QandA.format(result);

        BaiduSearch.ResultSum rs = null;
        rs = BaiduSearch.searchResult(qa);
        Log.w("TEST", rs.toString());

        String s = BaiduSearch.prettyOut(qa, rs);
        Log.w("TEST", s);
    }
}
