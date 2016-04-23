package tlb.service.http;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;


/**
 * @understands talking http
 */
public class DefaultHttpAction implements HttpAction {
    public static final String HTTPS = "https";
    private final HttpClient client;
    private static final Logger logger = Logger.getLogger(DefaultHttpAction.class.getName());
    private SSLSocketFactory socketFactory;
    private HttpContext context;

    public DefaultHttpAction() {
        this(createClient());
    }

    DefaultHttpAction(HttpClient client) {
        this(client, new BasicHttpContext());
    }

    public DefaultHttpAction(HttpClient client, HttpContext context) {
        this.client = client;
        this.socketFactory = sslSocketFactory();
        this.context = context;
    }

    public static DefaultHttpClient createClient() {
        return new DefaultHttpClient();
    }

    private SSLSocketFactory sslSocketFactory() {
        try {
            return new SSLSocketFactory(new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            }, new AllowAllHostnameVerifier());
        } catch (Exception e) {
            logger.fatal("failed to create ssl socket factory", e);
            throw new RuntimeException(e);
        }
    }

    private String executeMethod(HttpRequestBase req) {
        return responseString(req.getURI().toString(), execute(req));
    }

    private String responseString(String uri, HttpResponse res) {
        HttpEntity entity = res.getEntity();
        try {
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            logger.fatal(String.format("Could not de-reference response from [%s].", uri), e);
            throw new RuntimeException(e);
        }
    }

    private synchronized HttpResponse execute(HttpRequestBase req) {
        URI uri = req.getURI();
        HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        if (HTTPS.equals(uri.getScheme())) {
            Scheme sch = new Scheme(HTTPS, uri.getPort(), socketFactory);
            client.getConnectionManager().getSchemeRegistry().register(sch);
        }

        HttpResponse response = null;

        try {
            response = client.execute(targetHost, req, new BasicHttpContext(context));
        } catch (IOException e) {
            logger.fatal(String.format("Request to [%s] failed.", uri), e);
            throw new RuntimeException(e);
        }
        // response.getStatusLine().getStatusCode();//TODO: check me and bomb if need be
        return response;
    }

    public String get(String url) {
        return responseString(url, doGet(url));
    }

    public HttpResponse doGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        return execute(httpGet);
    }

    public String post(String url, Map<String,String> data) {
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(parameters));
        } catch (UnsupportedEncodingException e) {
            UUID logId = UUID.randomUUID();
            logger.fatal(String.format("log group id: %s => could not create valid form entity from given data.", logId));
            logger.fatal(String.format("log group id: %s => data given for form entity was: %s", logId, data.toString()));
        }
        return executeMethod(httpPost);
    }

    public String post(String url, String data) {
        return responseString(url, doPost(url, data));
    }

    public HttpResponse doPost(String url, String data) {
        HttpPost httpPost = new HttpPost(url);
        setStringEntity(data, httpPost);
        return execute(httpPost);
    }


    public String put(String url, String data) {
        HttpPut httpPut = new HttpPut(url);
        setStringEntity(data, httpPut);
        return executeMethod(httpPut);
    }

    private void setStringEntity(String data, HttpEntityEnclosingRequest httpPut) {
        try {
            httpPut.setEntity(new StringEntity(data));
        } catch (UnsupportedEncodingException e) {
            UUID logId = UUID.randomUUID();
            logger.fatal(String.format("log group id: %s => could not create valid string entity from given data.", logId));
            logger.fatal(String.format("log group id: %s => data given for string entity was: %s", logId, data));
        }
    }
}
