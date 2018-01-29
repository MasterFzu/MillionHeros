package masterfzu.millionheros.baiduocr;

import masterfzu.millionheros.util.Counter;

import java.net.URLEncoder;

/**
 * OCR 通用识别
 */
public class BaiduOCR {

    /**
     * 重要提示代码中所需工具类
     * FileUtil,Base64Util,HttpUtil,GsonUtils请从
     * https://ai.baidu.com/file/658A35ABAB2D404FBF903F64D47C1F72
     * https://ai.baidu.com/file/C8D81F3301E24D2892968F09AE1AD6E2
     * https://ai.baidu.com/file/544D677F5D4E4F17B4122FBD60DB82B3
     * https://ai.baidu.com/file/470B3ACCA3FE43788B5A963BF0B625F3
     * 下载
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        System.out.println("begin...");
        // 通用识别url
        String otherHost = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
        // 本地图片路径
        String filePath = "g:\\testocr\\微信截图_20180114231559.png";
        try {
            byte[] imgData = FileUtil.readFileByBytes(filePath);
            String imgStr = Base64Util.encode(imgData);
            String params = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(imgStr, "UTF-8");
            /**
             * 线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
             */
            String accessToken = "24.b452843489999c2812d18a10cf1ab3d0.2592000.1518661565.282335-10690761";
            String result = HttpUtil.post(otherHost, accessToken, params);
            System.out.println(result);

            System.out.println("spend:" + (System.currentTimeMillis() - start) / 1000.0 + "s");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String doOCR(final byte[] imgData) {
        if (imgData == null)
            return "";

        Counter.spendS("doOCR");
        // 通用识别url
        String otherHost = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
        // 本地图片路径
        try {
            String imgStr = Base64Util.encode(imgData);
            String params = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(imgStr, "UTF-8");
            /**
             * 线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
             * TODO 输入你的accessToken
             */
            String accessToken = "24.b452843489999c2812d18a10cf1ab3d0.2592000.1518661565.282335-10690761";
            String result = HttpUtil.post(otherHost, accessToken, params);
            System.out.println(result);

            Counter.spendS("doOCR");

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
