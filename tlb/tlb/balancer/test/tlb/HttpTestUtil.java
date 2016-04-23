package tlb;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import sun.security.rsa.RSAKeyPairGenerator;
import tlb.utils.FileUtil;

import javax.security.auth.x500.X500Principal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import static tlb.TestUtil.createTmpDir;
import static tlb.TestUtil.deref;


/**
 * @understands test http server that is used to test http client code end-to-end
 */
public class HttpTestUtil {

    private static final String STORE_PASSWORD = "tlb";

    private Server server;
    private Thread blocker;
    private File serverKeyStore;
    private File tmpDir;

    public HttpTestUtil() {
        Security.addProvider(new BouncyCastleProvider());
        tmpDir = createTmpDir();
        serverKeyStore = new File(tmpDir, "server.jks");
        prepareCertStore(serverKeyStore);
        server = new Server();
        server.addHandler(echoHandler());
        server.addHandler(redirectHandler());
    }

    public void destroy() {
        FileUtils.deleteQuietly(tmpDir);
    }

    public void httpConnector(final int port) {
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        server.addConnector(connector);
    }

    public void httpsConnector(final int port) {
        SslSocketConnector socketConnector = new SslSocketConnector();
        socketConnector.setPort(port);
        socketConnector.setMaxIdleTime(30000);
        socketConnector.setKeystore(serverKeyStore.getAbsolutePath());
        socketConnector.setPassword(STORE_PASSWORD);
        socketConnector.setKeyPassword(STORE_PASSWORD);
        socketConnector.setWantClientAuth(false);
        server.addConnector(socketConnector);
    }

    public synchronized void start() throws InterruptedException {
        if (blocker != null)
            throw new IllegalStateException("Aborting server start, it seems server is already running.");

        blocker = new Thread(new Runnable() {
            public void run() {
                try {
                    server.start();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        });
        blocker.start();
        while (!server.isStarted()) {
            Thread.sleep(50);
        }
    }

    public synchronized void stop() {
        if (blocker == null)
            throw new IllegalStateException("Aborting server stop, it seems there is no server running.");

        try {
            server.stop();
            blocker.interrupt();
            blocker.join();
            blocker = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareCertStore(File serverKeyStore) {
        KeyPair keyPair = generateKeyPair();
        X509Certificate cert = generateCert(keyPair);
        FileOutputStream os = null;
        try {
            KeyStore store = KeyStore.getInstance("JKS");
            store.load(null, null);
            store.setKeyEntry("test", keyPair.getPrivate(), STORE_PASSWORD.toCharArray(), new Certificate[]{cert});
            os = new FileOutputStream(serverKeyStore);
            store.store(os, STORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (os != null) {
                IOUtils.closeQuietly(os);
            }
        }
    }

    private X509Certificate generateCert(final KeyPair keyPair) {
        Date startDate = day(-1);
        Date expiryDate = day(+1);
        BigInteger serialNumber = new BigInteger("1000200030004000");

        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=Test CA Certificate");

        certGen.setSerialNumber(serialNumber);
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(startDate);
        certGen.setNotAfter(expiryDate);
        certGen.setSubjectDN(dnName);                       // note: same as issuer
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA1WITHRSA");

        try {
            return certGen.generate(keyPair.getPrivate());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Date day(final int offset) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.add(GregorianCalendar.DAY_OF_MONTH, offset);
        return gregorianCalendar.getTime();
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPair seed = new RSAKeyPairGenerator().generateKeyPair();
            RSAPrivateKey privateSeed = (RSAPrivateKey) seed.getPrivate();
            RSAPublicKey publicSeed = (RSAPublicKey) seed.getPublic();
            KeyFactory fact = KeyFactory.getInstance("RSA", "BC");
            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(privateSeed.getModulus(), privateSeed.getPrivateExponent());
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(publicSeed.getModulus(), publicSeed.getPublicExponent());
            return new KeyPair(fact.generatePublic(publicKeySpec), fact.generatePrivate(privateKeySpec));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Handler echoHandler() {
        return new ContextHandler("/echo") {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
                if (request.getPathInfo().startsWith("/echo")) {
                    response.setStatus(200);
                    response.setHeader("Content-Type", "text/plain");
                    PrintWriter writer = response.getWriter();
                    writer.write(String.format("%s(%s): %s", request.getMethod(), request.getLocalPort(), request.getPathInfo()));
                    String query = request.getQueryString();
                    if (query != null) {
                        writer.write("?");
                        writer.write(query);
                    }
                    Enumeration paramKeys = request.getParameterNames();
                    if (paramKeys.hasMoreElements()) {
                        writer.write("\nparams: ");
                        while (paramKeys.hasMoreElements()) {
                            Object key = paramKeys.nextElement();
                            writer.write(key.toString());
                            writer.write("=");
                            writer.write(Arrays.asList(request.getParameterValues((String) key)).toString());
                        }
                    }
                    writer.write("\n");
                    writer.write(deref(request.getInputStream()));
                    writer.close();
                }
            }
        };
    }

    private Handler redirectHandler() {
        return new ContextHandler("/redirect") {
            @Override
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
                if (request.getPathInfo().startsWith("/redirect")) {
                    response.setStatus(302);
                    response.setHeader("Location", String.format("%s://%s:%s/echo", request.getScheme(), request.getLocalName(), String.valueOf(request.getLocalPort())));
                    PrintWriter writer = response.getWriter();
                    writer.write("fly. huuuuiiiii!!!!");
                    writer.close();
                }
            }
        };
    }
}
