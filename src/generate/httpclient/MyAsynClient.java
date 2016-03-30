package generate.httpclient;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.reactor.IOReactorException;
////参考文章:http://www.cnblogs.com/wasp520/archive/2012/06/28/2568897.html
public class MyAsynClient{
  
    private List<String> urls;
    private HandlerFailThread failHandler;
    public MyAsynClient(List<String> list){
        failHandler=new HandlerFailThread();
        urls=list;
    }
    public Map<String,String> asynGet() throws IOReactorException,
            InterruptedException {
        final HttpAsyncClient httpclient = new DefaultHttpAsyncClient();
        
        httpclient.start();
        
        HttpGet[] requests = new HttpGet[urls.size()];
        int i=0;
        for(String url : urls){
            requests[i]=new HttpGet(url);
            i++;
        }
        final CountDownLatch latch = new CountDownLatch(requests.length);
        final Map<String, String> responseMap=new HashMap<String, String>();
        try {
        	 final AtomicInteger count = new AtomicInteger(0);
            
        	//匿名内部类和局部内部类只能访问final变量 :http://blog.csdn.net/salahg/article/details/7529091
            for ( final HttpGet request : requests) {
            	 
                httpclient.execute(request, new FutureCallback<HttpResponse>() {
                	
                    public void completed(final HttpResponse response) {
                        latch.countDown();
                        responseMap.put(request.getURI().toString(), response.getStatusLine().toString());
                        try {
                            System.out.println(request.getRequestLine() + "->" + response.getStatusLine()+"->");
                            
                            System.out.println(count.addAndGet(1) + ".response: threadId="
                                    + Thread.currentThread().getId()); 
                        } catch (Exception e) {
                            failHandler.putFailUrl(request.getURI().toString(),
                                    response.getStatusLine().toString());
                            e.printStackTrace();
                        }
                    }

                    public void failed( final Exception e) {
                        latch.countDown();
                        e.printStackTrace();
                        failHandler.putFailUrl(request.getURI().toString(),
                                e.getMessage());
                    }

                    public void cancelled() {
                    //计数器减一 
                    	latch.countDown();
                    }

                });
            }
            System.out.println("Doing...");
        } finally {
        	//等待所有工人完成工作 
            latch.await();
            httpclient.shutdown();
        }
        
        
        System.out.println("Done");
        failHandler.printFailUrl();
       
        return responseMap;
    }
    
    
    
    public static void main(String[] args) {
        List<String> urls=new ArrayList<String>();
        urls.add("http://blog.csdn.net/binyao02123202/article/details/18361755");
        urls.add("http://www.baidu.com");
        for(int i=0;i<5;i++){
            urls.addAll(urls);
        }
        System.out.println(urls.size());
        MyAsynClient client=new MyAsynClient(urls);
        try {
            client.asynGet();
        }  catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}