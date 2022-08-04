package com.shengbo.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.shengbo.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public String app_id = "2021000121631677";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCrKNG0kIy9dl7SYEHb399b1joP2LyA0+2iLmTEroOZo7VcJT4uU5HDKxPfENSoyR6K4KzokS6og6+FHC7AVXn1A/q3kWXT+oZ4/FBlLeObQ2uOvQ9WRn8DmnIMxIMUNNMMBB7MmCoZmfNKLiLJ9XKVs2P+OA4HM4trG+N6s9FtRmc+ql3S1mesvbAglO65LKiEAGGgfeMJwuAt3ilh/MYK//Y5jMrRhtHLpczVBw/po4uWF1Wz928mQLvIJmP2VG1hnV9gzDISI9Se243Z15b1LQ5iMU09SVC4lNtTaoR4z/gOM1JkL28aCM9+b5r+wBoQMIeqSkEjmCF/jZc+3ecfAgMBAAECggEAP4c+FtwUMPpvhXxO1ZKJr+ea02jpxpNiv3Ci3FG97NfTp+j25HEGSD+D96aimCdWG8v6wbOpcsi17B0iySawxp18gIgKGJ0DLoLQQE3BDk7+7BMGT0qJOEgqDs0drfFRaENbPys3bRs0LxNivD1LE39rHN+nbUrXc6D9VPzLPdhoVT1hg1FSDoglodEICGNeO4MKrElGMgWsSnlzWHa4LVYjyZgIcCqgdDcfMVdvIXphYN8MVqLcJRs0ZV8JXaG5sOitJA2SwcfoZouJ8ONSl4I47isLCohz1VAUtbOZGZ7mENZtmJG885QuiQ1/b+k35eb4mIYQLASGmUz/LuEGyQKBgQDsA07xQY5TOVSdyAcwqWV8r5FvASyOh8xHML0t9ZEaV30hoNCASZKMOfreGpxlGLX3b2C4KLTF+T4HuSvpRxlqmA9wyE0xilv5CzId1MGUdE33UfR1Dk1ZdL89FWc/yeQI9Wom9RLfGCAnRD6Moy/jr7OhbAGRcbUqy+7XKOUVIwKBgQC5p4GebHAxqtwsLt79j1j6D/cztfSo7eZ+C5+RinoVAVrZD8lfUG2WcDHhIvphTkwAv08eYkxdzV0G4KWFQRVyYPYm9JMtyoHhdy8kTfvnoxOvaC2CMfUW+go9GJieXijHA1p7iz02p3EaAF8gK7aw0vtEpndScIODft3mku771QKBgQDBIhvxtVHArQOfclbf7V9NX2bFzdImeN4jy7Cj8XZidgHCCRVWRd6UvcWbB2/AEy6lYxFk4nq8HzAxpchXR4V3AyRviJoS1kRZUl4ap0YaM+qxXvGK8L8/Wxg75k3tl0ryQdIyOw7MXbPCLkh5UfkYe2mlyPqniMNWWDgE/kQovQKBgBqjEPLlBhfqNtrUAsSIm7CNN9+gfLD8KTTuf/+GyctXvWwlWrQlOwP5pv5xYVEyxa7ZxAVM/z36KB3Df5wl4WjexpKCRtesFYR+8DGaSslKWZmyLc1fU5XvXCa177fRNLKRJZtSN/8ueQjNAtj/zJ+ENe78n2QhpozoV/gsHsPdAoGAOsKCpS0A9MZYWidl/YCkqjhyrXp3A8SRzHza/ZLQM5HHbwGFiTtLwjkkpMpaGYqolJnypmAtY5gjgyPz8oxmu4hyFHljpF6Nj7+1m3iHejgZKXF0QfsCW4zhQk7a6dr1DiVWiUArFL3OLnWwqkGbhy6wcO8xiNGQ0Le8tLA0CoI=";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqyjRtJCMvXZe0mBB29/fW9Y6D9i8gNPtoi5kxK6DmaO1XCU+LlORwysT3xDUqMkeiuCs6JEuqIOvhRwuwFV59QP6t5Fl0/qGePxQZS3jm0Nrjr0PVkZ/A5pyDMSDFDTTDAQezJgqGZnzSi4iyfVylbNj/jgOBzOLaxvjerPRbUZnPqpd0tZnrL2wIJTuuSyohABhoH3jCcLgLd4pYfzGCv/2OYzK0YbRy6XM1QcP6aOLlhdVs/dvJkC7yCZj9lRtYZ1fYMwyEiPUntuN2deW9S0OYjFNPUlQuJTbU2qEeM/4DjNSZC9vGgjPfm+a/sAaEDCHqkpBI5ghf42XPt3nHwIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息

    //TODO 注意！！！！！ 在nginx里面也要改这个地址！！！！！
    public String notify_url = "http://erwzcf.natappfree.cc/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    public String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type= "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    //订单超时时间
    private String timeout = "30m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    public String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
