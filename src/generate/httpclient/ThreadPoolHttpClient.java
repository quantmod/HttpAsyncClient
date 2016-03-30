package generate.httpclient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
//参考文章:http://www.cnblogs.com/wasp520/archive/2012/06/28/2568897.html
public class ThreadPoolHttpClient {
	// 线程池
	private static ExecutorService exe = null;
	// 线程池的容量
	private static final int POOL_SIZE = 20;
	private HttpClient client = null;
	static String[] urls = { "http://blog.csdn.net/binyao02123202/article/details/18361755",
  "http://hc.apache.org/httpcomponents-asyncclient-4.1.x/httpasyncclient/dependency-info.html" };
	
	public static void main(String[] args) {
		
		exe = Executors.newFixedThreadPool(POOL_SIZE);
		HttpParams params = new BasicHttpParams();
		/* 从连接池中取连接的超时时间 */
		ConnManagerParams.setTimeout(params, 1000);
		/* 连接超时 */
		HttpConnectionParams.setConnectionTimeout(params, 2000);
		/* 请求超时 */
		HttpConnectionParams.setSoTimeout(params, 4000);
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
				.getSocketFactory()));

		PoolingClientConnectionManager cm = new PoolingClientConnectionManager(
				schemeRegistry);
		cm.setMaxTotal(10);
		//线程安全的,多线程可以共用一个
		final HttpClient httpClient = new DefaultHttpClient(cm, params);
		// URIs to perform GETs on
		final String[] urisToGet = urls;

		/*
		 * join方法的作用： 解释一下，是主线程等待子线程的终止。也就是在子线程调用了join()方法后面的代码，
		 * 只有等到子线程结束了主线程才能执行。(Waits for this thread to die.)
		 */

		// 使用线程池
		for (int i = 0; i < urisToGet.length; i++) {
			final int j = i;
			System.out.println(j);
			HttpGet httpget = new HttpGet(urisToGet[i]);
			GetThread getThread = new GetThread(httpClient, httpget);
			// 线程池里面给线程命名无效
			getThread.setName("threadsPoolClient");
			System.out.println(getThread.getName());
			exe.execute(getThread);
		}
		// 关闭线程池
		exe.shutdown();
		System.out.println("Main Thread Done");
	}

	static class GetThread extends Thread {

		private final HttpClient httpClient;
		private final HttpContext context;
		private final HttpGet httpget;

		public GetThread(HttpClient httpClient, HttpGet httpget) {
			this.httpClient = httpClient;
			this.context = new BasicHttpContext();
			this.httpget = httpget;
		}

		@Override
		public void run() {
			System.out.println(Thread.currentThread().getName());
			get();
		}

		public void get() {
			try {
				HttpResponse response = this.httpClient.execute(httpget,
						context);
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					System.out.println(httpget.getURI() + ": status"
							+ response.getStatusLine().toString());
				}
				// ensure the connection gets released to the manager
				System.out.println(EntityUtils.toString(entity));
			} catch (Exception e) {
				e.printStackTrace();
				httpget.abort();
			} finally {
				httpget.releaseConnection();
			}
		}
	}
}