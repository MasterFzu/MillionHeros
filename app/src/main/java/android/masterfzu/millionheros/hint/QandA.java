package android.masterfzu.millionheros.hint;

import android.masterfzu.millionheros.TheApp;
import android.masterfzu.millionheros.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by zhengsiyuan on 2018/1/16.
 * 问题与答案
 */
public class QandA {
    private String question;
    private String [] ans = new String[3];

    @Override
    public String toString() {
        return "QandA{" +
                "question='" + question + '\'' +
                ", ans=" + Arrays.toString(ans) +
                '}';
    }

    public static QandA format(String j) {
        if (StringUtil.isEmpty(j))
            return null;

        QandA result = new QandA();

        try {
            JSONObject json = new JSONObject(j);
            if (json.getInt("words_result_num") <= 3) //问题加答案至少大于3
                return null;

            JSONArray ja = json.getJSONArray("words_result");
            int num = ja.length() - 1;
            int a = 2;
            for (; num >= ja.length() - 3; num--) {
                result.ans[a--] = pureA(ja.getJSONObject(num).getString("words"));
            }

            StringBuffer qsb = new StringBuffer();
            for (int i = 0; i <= num; i++){
                String s = ja.getJSONObject(i).getString("words");
                if (s.contains("镖姬线"))
                    continue;

                qsb.append(s);
            }

            String q = qsb.toString();
            if (StringUtil.isEmpty(q))
                return null;

            result.question = pureQ(q);

            TheApp.LogW(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return result;
    }

    private static String pureQ(String s) {
        String result = s;

        if ((isNum(s.charAt(0)) && s.charAt(1) == '.'))
            result = s.substring(2);
        else if (isNum(s.charAt(1)) && s.charAt(2) == '.')
            result = s.substring(3);

//        if (s.indexOf("?") == -1)
//            return s;
//        result = result.replaceAll("\\?", "");
        return result;
    }

    private static boolean isNum(char c) {
        if (c >='0'&& c <='9')
            return true;

        return false;
    }

    private static String pureA(String s) {
        if (s.indexOf("《") == -1 && s.indexOf("》") == -1)
            return s;

        String result = s.replaceAll("《", "").replaceAll("》", "");
        return result;
    }

    public static void main(String [] args) {
        String result = "{\"log_id\": 8816071367637002938, \"words_result_num\": 5, \"words_result\": [{\"words\": \"《火星情报局》第三季主题\"}, {\"words\": \"曲是?\"}, {\"words\": \"《再见18岁》\"}, {\"words\": \"《火星人来过》\"}, {\"words\": \"《火星情报局》\"}]}";
        QandA.format(result);
    }

    public String getQuestion() {
        return question;
    }

    public String[] getAns() {
        return ans;
    }
}
