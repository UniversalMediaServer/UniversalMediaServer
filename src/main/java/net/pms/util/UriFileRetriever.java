package net.pms.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;

public class UriFileRetriever {
	
	private URI uri;
	private File target;
	private UriRetrieverCallback callback;
	
	public UriFileRetriever(URI uri, File target, UriRetrieverCallback callback) {
		this.uri = uri;
		this.target = target;
		this.callback = callback;
	}
	
	public File executeGetFile() throws Exception {
		CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
		File result;
		try  {
			httpclient.start();
			ZeroCopyConsumer<File> consumer = new ZeroCopyConsumer<File>(target) {

				@Override
				protected File process(
					final HttpResponse response,
					final File file,
					final ContentType contentType) throws Exception {
					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
						throw new ClientProtocolException("Connection to host failed: " + response.getStatusLine());
					}

					return file;
				}

				@Override
			    protected void onContentReceived(
			    	final ContentDecoder decoder, final IOControl ioctrl) throws IOException {
					super.onContentReceived(decoder, ioctrl);
					UriRetriever.invokeCallback(uri.toString(), callback, 700000, 1000);
				}
			};

			Future<File> future = httpclient.execute(HttpAsyncMethods.createGet(uri), consumer, null, null);
			result = future.get();
			System.out.println("Response file length: " + result.length());
		} finally {
			httpclient.close();
		}

		return result;
	}
}
