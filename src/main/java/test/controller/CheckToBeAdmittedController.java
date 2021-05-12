package test.controller;

import com.sun.mail.util.MailSSLSocketFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by jianan on 2021/05/12
 */
@RestController
public class CheckToBeAdmittedController {
    //储存现有的组织部消息
    private static List<String> STATIC_ZZB_NEWS_LIST=new ArrayList<>();
    //储存新出现的组织部消息
    private static List<String> NEW_STATIC_ZZB_NEWS_LIST=new ArrayList<>();
    //发送的邮箱
    private static List<String> SEND_EMAIL_LIST=new ArrayList<>(Arrays.asList("1170271773@qq.com"));

    /**
     * 添加需要接受消息的邮箱
     * @return
     */
    @RequestMapping(value="/add_email")
    @ResponseBody
    public String addEmail(@RequestParam("email")String email) {
        if(email.lastIndexOf("@")!=-1){
            SEND_EMAIL_LIST.add(email);
        }
        return SEND_EMAIL_LIST.toString();
    }

    /**
     * 启动定时任务，定时检查拟录取等
     * @return
     */
    @RequestMapping(value="/start_check_to_be_admitted_task")
    @ResponseBody
    public void startCheckToBeAdmittedTask() {
        checkToBeAdmittedTask();
        System.out.println("start_check_to_be_admitted_task begin");
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Thread-check_to_be_admitted");
            }
        });
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    checkToBeAdmittedTask();
                } catch (Exception e) {
                    System.out.println("start_check_to_be_admitted_task failed");
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void checkToBeAdmittedTask(){
        //首先获取
        Document doc= null;
        try {
            doc = Jsoup.parse(new URL("http://www.szzzb.gov.cn/default.html"),(10000));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(doc!=null){
            //找到组织部官网上class 是news的，有三块内容
            Elements news = doc.getElementsByClass("news");
            //本次检查是否有新消息标志 
            boolean hasNewNews=false;
            //遍历所有的新闻块
            for (Element zzbNew : news){
                Document zzbNewDoc = Jsoup.parse(zzbNew.toString());
                //新闻的标签在li下
                Elements liElements = zzbNewDoc.getElementsByTag("li");
                for(Element li:liElements){
                    //是否是新出现的新闻
                    boolean inStaticZzbNewsListFlag=false;
                    //遍历之前的新闻，看有没有标题和当前的一致，若一致为老新闻，否则新的新闻
                    for(int i=0;i<STATIC_ZZB_NEWS_LIST.size();i++){
                        if(STATIC_ZZB_NEWS_LIST.get(i).equals(li.text())){
                            inStaticZzbNewsListFlag=true;
                        }
                    }
                    //新的新闻。1.置本次检查标志为有新的新闻。2.将该新闻放置于需要发送消息的队列中。3 置于所有的新闻队列中
                    if(!inStaticZzbNewsListFlag){
                        hasNewNews=true;
                        NEW_STATIC_ZZB_NEWS_LIST.add(li.text());
                        STATIC_ZZB_NEWS_LIST.add(li.text());
                    }
                }
            }
            //有新的新闻，触发发送邮件
            if(hasNewNews || NEW_STATIC_ZZB_NEWS_LIST.size()!=0){
                sendEmail(NEW_STATIC_ZZB_NEWS_LIST.get(0));
            }
            System.out.println("check finished");
            NEW_STATIC_ZZB_NEWS_LIST=new ArrayList<>();
        }

    }
    
    private  void sendEmail(String newsTitle){
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
            Address[] sendAddress=new Address[SEND_EMAIL_LIST.size()];
            for(int i=0;i<SEND_EMAIL_LIST.size();i++){
                sendAddress[i]=new InternetAddress(SEND_EMAIL_LIST.get(i));
            }

            mimeMessage.addRecipients(Message.RecipientType.TO,sendAddress);

            //邮件标题
            //mimeMessage.setSubject("官网有新的消息","UTF-8");
            mimeMessage.setSubject(MimeUtility.encodeText("官网有新的消息", "utf-8", "B"));  //生成邮件标题

            //邮件内容
            //mimeMessage.setContent(newsTitle,"UTF-8");
            mimeMessage.setContent(newsTitle, "text/plain;charset=utf-8");   //生成邮件正文

            //发送邮件
            transport.sendMessage(mimeMessage,mimeMessage.getAllRecipients());

        }catch (Exception e){
            e.printStackTrace();
            System.out.println("错误");
        }
    }
}
