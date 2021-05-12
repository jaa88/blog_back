package test.controller;

import com.sun.mail.util.MailSSLSocketFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.net.URL;
import java.util.Properties;

/**
 * Created by jianan on 2018/9/29.
 */
@RestController
public class HelloWorldController {
    @RequestMapping("/helloWorld")
    public String sayHelloWorld(){
        return "hello world";
    }

    /**
     * 助教公司微信号同步
     * @return
     */
    @RequestMapping(value="/test_email")
    @ResponseBody
    public void testEmail() {
        //创建一个配置文件并保存
        Properties properties = new Properties();

        properties.setProperty("mail.host","smtp.qq.com");

        properties.setProperty("mail.transport.protocol","smtp");

        properties.setProperty("mail.smtp.auth","true");

        try{
//QQ存在一个特性设置SSL加密
            MailSSLSocketFactory sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.ssl.socketFactory", sf);

            //创建一个session对象
            Session session = Session.getDefaultInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("1170271773@qq.com","haahzrihjrtihfaf");
                }
            });

            //开启debug模式
            session.setDebug(true);
            //获取连接对象
            Transport transport = session.getTransport();

            //连接服务器
            transport.connect("smtp.qq.com","1170271773@qq.com","haahzrihjrtihfaf");

            //创建邮件对象
            MimeMessage mimeMessage = new MimeMessage(session);

            //邮件发送人
            mimeMessage.setFrom(new InternetAddress("1170271773@qq.com"));

            //邮件接收人
            Address[] sendAddress=new Address[]{
                    new InternetAddress("1170271773@qq.com"),
                    new InternetAddress("1169788454@qq.com"),
            };
            mimeMessage.addRecipients(Message.RecipientType.TO,sendAddress);

            //邮件标题
            mimeMessage.setSubject("公示");

            //邮件内容
            mimeMessage.setContent("hai mei you gong shi","text/html;charset=UTF-8");

            //发送邮件
            transport.sendMessage(mimeMessage,mimeMessage.getAllRecipients());

        }catch (Exception e){
            System.out.println("错误");
        }

    }
}
