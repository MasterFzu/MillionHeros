package masterfzu.millionheros.hint;

import masterfzu.millionheros.TheApp;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhengsiyuan on 2018/1/16.
 * 识图-解析问题与答案-搜索-分析搜索结果
 */
public class BaiduSearch {

    public static boolean allZero(int [] sum) {
        boolean allzero = true; //是否无精确匹配
        for (int i : sum) {
            if (i != 0) {
                allzero = false;
                break;
            }
        }

        return allzero;
    }

    public static String getHintIfCatch(ResultSum rs) {
        boolean allzero = allZero(rs.sum);

        if (allzero) {
            return "无辅助提示！";
        }

        int index = getZeroone(rs.sum);
        if (index != -1) {
            return "唯一未出现：" + rs.ans[index];
        }

        index = getBigone(rs.sum);
        if (index != -1) {
            return "命中最多：" + rs.ans[index];
        }

        return "无辅助提示！";
    }


    public static int getZeroone(int[] sum) {
        int index = -1, count = -1;
        for (int i = 0; i < sum.length; i ++) {
            if (sum[i] <= 0 ) {
                index = i;
                count++;
            }
            if (count >= 1)
                return -1;
        }

        return index;
    }


    public static ResultSum searchResult(QandA qa) throws IOException {
        ResultSum rs = new ResultSum(qa);
        String path = "http://m.baidu.com/s?word=" + URLEncoder.encode(qa.getQuestion(), "UTF-8");
        rs.path = path;
        String line = "";
        Log.w("BaiduSearch", path);
        URL url = new URL(path);
        BufferedReader breaded = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuffer sb = new StringBuffer();
        while ((line = breaded.readLine()) != null) {
            sb.append(getHanZi(line));
        }

        for (int i = 0; i < rs.sum.length; i++) {
            rs.sum[i] += getCount(sb, qa.getAns()[i]);

            for (int y = 0; y < qa.getAns()[i].length(); y++) {
                int count = getCount(sb, qa.getAns()[i].substring(y, y+1));
                if (count <= 0)
                    continue;

                rs.dumpsum[i][y] += count;
                rs.allsum[i] += count;
            }
        }

        TheApp.LogW(rs.toString());
        return  rs;
    }


    static String prettyOut(QandA qa, ResultSum r) {
        StringBuffer rsb = new StringBuffer();
        long startTime = System.currentTimeMillis();

        boolean allzero = true; //是否无精确匹配
        for (int i : r.sum) {
            if (i != 0) {
                allzero = false;
                break;
            }
        }

        int index = getBigone(r.sum);

        if (!allzero) {
            for (int i = 0; i < r.sum.length; i++) {
                if (r.sum[i] <= 0) {
                    rsb.append("无匹配：" + qa.getAns()[i]);
                    rsb.append("\n");
                    continue;
                }

                System.out.println("命中：" + qa.getAns()[i] + ":" + r.sum[i] + (index == i ? " ,  最多！" : "") + "\t 总和：" + r.allsum[i]);
                rsb.append("命中：" + qa.getAns()[i] + " : " + r.sum[i] + (index == i ? " ,  最多！" : ""));
                rsb.append("\n");
            }

            return rsb.toString();
        }

        index = getBigone(r.allsum);

        for (int i = 0; i < r.sum.length; i++) {
            int zeroCount = 0;
            System.out.print("单字：");
            rsb.append("单字：");
            for (int y = 0; y < r.dumpsum[i].length; y++) {
                if (r.dumpsum[i][y] <= 0) {
                    zeroCount++;
                    continue;
                }

                System.out.print(qa.getAns()[i].substring(y, y + 1) + ":" + r.dumpsum[i][y] + ", ");
                rsb.append(qa.getAns()[i].substring(y, y + 1) + " : " + r.dumpsum[i][y] + " , ");
            }

            rsb.append(" ### 总和:" + r.allsum[i] + (index == i ? " 最大值！" : ""));
//            if (zeroCount > 0) {
//                System.out.print(" \t  未出现:" + zeroCount);
////                rsb.append(" ### " + zeroCount + " 个字无匹配:");
//            } else {
//                System.out.print(" \t ###### 总和:" + r.allsum[i] + (index == i ? "  \t 最大值！" : ""));
//                rsb.append(" ### 总和:" + r.allsum[i] + (index == i ? " 最大值！" : ""));
//            }

            System.out.println();
            rsb.append("\n");
        }

        float excTime = (float) (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("执行时间：" + excTime + "s");
        System.out.println("\n");

        return rsb.toString();
    }

    public static int getBigone(int[] allsum) {
        int index = 0;
        int a = allsum[0];
        for (int i = 1; i < allsum.length; i++) {
            if (allsum[i] == a && allsum[i] != 0)
                return -1;

            if (allsum[i] > a) {
                a = allsum[i];
                index = i;
            }
        }

        return index;
    }

    private static String getHanZi(String s) {
        StringBuffer r = new StringBuffer();
        Pattern pattern = Pattern.compile("[^\\x00-\\xff]");
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            r.append(matcher.group());
        }

        return r.toString();
    }

    private static int getCount(StringBuffer sb, String des) {
        des = des.replaceAll("\\?" , ""); //防止出错
        Pattern pattern = Pattern.compile(des);
        Matcher matcher = pattern.matcher(sb);
        int count=0;
        while(matcher.find()){
            count++;
        }
        return count;
    }

    /**
     * 保存分析结果
     */
    public static class ResultSum {
        public String path; //搜索路径
        public int sum []; //每个答案命中次数
        public int dumpsum [][]; //每个答案中的文字出现的次数
        public int allsum []; //单字出现次数总和
        public String [] ans;

        ResultSum(QandA qa) {
            ans = qa.getAns();
            sum = new int[qa.getAns().length];
            dumpsum = new int[qa.getAns().length][];
            allsum = new int[qa.getAns().length];

            for (int i = 0; i < qa.getAns().length; i++) {
                dumpsum[i] = new int[qa.getAns()[i].length()];
            }
        }

        @Override
        public String toString() {
            return "ResultSum{" +
                    "path='" + path + '\'' +
                    ", sum=" + Arrays.toString(sum) +
                    ", dumpsum=" + Arrays.toString(dumpsum) +
                    ", allsum=" + Arrays.toString(allsum) +
                    '}';
        }
    }
}
