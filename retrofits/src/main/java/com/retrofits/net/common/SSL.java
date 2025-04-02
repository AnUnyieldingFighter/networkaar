package com.retrofits.net.common;

import android.content.Context;
import android.text.TextUtils;

import com.retrofits.utiles.RLog;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * 证书设置（cer证书 单向验证  已测）
 * Created by 郭敏 on 2018/4/20 0020.
 */

public class SSL {
    /**
     * @param builder         OkHttpClient.Builder
     * @param context         上下文
     * @param certificatePath 证书位置（在asset）如：zs.cer
     * @param hostName        信任的主机名
     * @return
     */
    public OkHttpClient.Builder setSSL(OkHttpClient.Builder builder, Context context,
                                       String[] certificatePath, String[] hostName) {
        if (context == null) {
            return builder;
        }
        String[] certificatePaths = new String[4];
        if (certificatePath != null && certificatePath.length > 0) {
            int length = certificatePath.length;
            for (int i = 0; i < 4; i++) {
                if (i >= length) {
                    continue;
                }
                certificatePaths[i] = certificatePath[i];
            }
        }
        builder = setCertificate(builder, context, certificatePaths);
        builder.hostnameVerifier(getHostnameVerifier(hostName));
        return builder;
    }

    /**
     * 打印证书信息
     *
     * @param context
     * @param crePath 证书位置（在asset）如：zs.cer
     */
    public void logCertificate(Context context, String crePath) {
        try {
            InputStream inputStream = context.getAssets().open(crePath);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate certificate = certificateFactory.generateCertificate(inputStream);
            certificateMsg(certificate);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }

    }

    //打印证书信息
    private void certificateMsg(Certificate certificate) {
        X509Certificate oCert = null;
        if (certificate instanceof X509Certificate) {
            oCert = (X509Certificate) certificate;
        }
        if (oCert == null) {
            return;
        }
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
        //获得证书版本
        String info = "证书版本:" + oCert.getVersion();

        //获得证书序列号
        info += "\n证书序列号:" + oCert.getSerialNumber().toString(16);
        //获得证书有效期
        Date beforedate = oCert.getNotBefore();
        info += "\n证书生效日期:" + dateformat.format(beforedate);
        Date afterdate = oCert.getNotAfter();
        info += "\n证书失效日期:" + dateformat.format(afterdate);
        //获得证书主体信息
        info += "\n证书拥有者:" + oCert.getSubjectDN().getName();
        //获得证书颁发者信息
        info += "\n证书颁发者:" + oCert.getIssuerDN().getName();

        //获得证书签名算法名称
        info += "\n证书签名算法:" + oCert.getSigAlgName();
        PublicKey key = oCert.getPublicKey();
        info += "\n公钥链:" + key.toString();
        byte[] byt = oCert.getExtensionValue("1.2.86.11.7.9");
        if (byt != null) {
            String strExt = new String(byt);
            info += "\n证书扩展域:" + strExt;
        }
        //
        byt = oCert.getExtensionValue("1.2.86.11.7.1.8");
        if (byt != null) {
            String strExt2 = new String(byt);
            info += "\n证书扩展域2:" + strExt2;
        }
        RLog.e("证书信息", info);
    }

    //设置证书  certificates["path","password"]
    private OkHttpClient.Builder setCertificate(OkHttpClient.Builder builder, Context context, String[] certificates) {
        try {
            //信任管理器 服务端证书
            String certificatePath = certificates[0];
            TrustManager[] trustManager = null;
            if (!TextUtils.isEmpty(certificatePath) && certificatePath.endsWith("bks")) {
                //没有测试过，似乎不支持这种格式
                trustManager = getTmf(context, certificatePath, certificates[1]);
            }
            Certificate ca = null;
            //引导创建 trustManager
            if (!TextUtils.isEmpty(certificatePath) && trustManager == null) {
                //一般是cer证书
                ca = getCertificate(context, certificatePath);
                trustManager = getCerTmf(ca);
            }
            if (trustManager != null) {
                RLog.e("TrustManager[] 个数", trustManager.length);
            }
            X509TrustManager x509TrustManager;
            //创建自定义的 trustManager
            if (trustManager != null) {
                x509TrustManager = new UseTrustManager(chooseTrustManager(trustManager), ca);
            } else {
                x509TrustManager = new UnSafeTrustManager();
            }
            //密钥管理器 客户端bks
            KeyManager[] keyManager = getkmf(context, certificates[2], certificates[3]);
            //
            SSLContext sslContext = SSLContext.getInstance("TLS");
            //第一个参数是授权的密钥管理器，用来授权验证。
            //第二个是被授权的证书管理器，用来验证服务器端的证书。
            //第三个参数是一个随机数值，可以填写null
            sslContext.init(keyManager, new TrustManager[]{x509TrustManager}, new SecureRandom());
            //trustManager 不为null 的时候可以这样
            //sslContext.init(keyManager, trustManager, new SecureRandom());
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            //builder.sslSocketFactory(ssf);
            builder.sslSocketFactory(ssf, x509TrustManager);
        } catch (KeyStoreException e) {
            e.printStackTrace();
            RLog.e("KeyStoreException", e.getMessage());
        } catch (CertificateException e) {
            e.printStackTrace();
            RLog.e("CertificateException", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            RLog.e("NoSuchAlgorithmException", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            RLog.e("IOException", e.getMessage());
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
            RLog.e("UnrecoverableKeyException", e.getMessage());
        } catch (KeyManagementException e) {
            e.printStackTrace();
            RLog.e("KeyManagementException", e.getMessage());
        }
        return builder;
    }

    private X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }

    //证书校验
    class UseTrustManager implements X509TrustManager {

        private X509TrustManager localTrustManager;
        private Certificate ca;

        public UseTrustManager(X509TrustManager localTrustManager, Certificate ca) throws NoSuchAlgorithmException, KeyStoreException {
            this.ca = ca;
            this.localTrustManager = localTrustManager;
        }


        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            //该方法检查客户端的证书，若不信任该证书则抛出异常。
            //由于不需要对客户端进行认证，因此只需要执行默认的信任管理器的这个方法。
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            //该方法检查服务器的证书，若不信任该证书同样抛出异常。
            //通过自己实现该方法，可以使之信任我们指定的任何证书。
            //在实现该方法时，也可以简单的不做任何处理，
            //即一个空的函数体，由于不会抛出异常，它就会信任任何证书。
            try {
                for (X509Certificate cert : chain) {
                    //确保它没有过期
                    cert.checkValidity();
                    RLog.e("证书过期验证", "通过");
                }
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(chain, authType);
                RLog.e("证书验证", "通过");
            }
            //throw new CertificateException();
        }


        @Override
        public X509Certificate[] getAcceptedIssuers() {
            //返回受信任的X509证书数组
            return new X509Certificate[0];
        }
    }

    //信任证书(不验证)
    class UnSafeTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // 接受任意客户端证书
            RLog.e("信任证书", "接受任意客户端证书");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // 接受任意服务端证书
            RLog.e("信任证书", "接受任意服务端证书");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }

    //获取证书
    private Certificate getCertificate(Context context, String crePath) throws IOException, CertificateException {
        InputStream inputStream = context.getAssets().open(crePath);
        //创建X509工厂类
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate = certificateFactory.generateCertificate(inputStream);
        closeInputStream(inputStream);
        //certificateMsg(certificate);
        return certificate;
    }

    //信任服务端证书 cer证书
    private TrustManager[] getCerTmf(Certificate certificate) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        //服务端公钥
        KeyStore serviceKey = KeyStore.getInstance(KeyStore.getDefaultType());
        serviceKey.load(null);
        //证书别名
        String alias = String.valueOf(0);
        //设置证书
        serviceKey.setCertificateEntry(alias, certificate);
        //
        //信任管理器
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(serviceKey);
        //
        TrustManager[] trustManager = trustManagerFactory.getTrustManagers();
        return trustManager;
    }

    //信任服务端证书 必须是bks 否则报：java.io.IOException: Wrong version of key store.
    private TrustManager[] getTmf(Context context, String bksPath, String bksPassowd) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        //服务端bks
        InputStream serviceInput = context.getAssets().open(bksPath);
        //keyStoreType默认是BKS
        KeyStore serviceKey = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] sp = null;
        if (!TextUtils.isEmpty(bksPassowd)) {
            sp = bksPassowd.toCharArray();
        }
        serviceKey.load(serviceInput, sp);
        closeInputStream(serviceInput);
        //信任管理器
        String type = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(type);
        tmf.init(serviceKey);
        TrustManager[] trustManager = tmf.getTrustManagers();
        return trustManager;
    }

    //客户端密钥管理器 必须是bks 否则报：java.io.IOException: Wrong version of key store.
    private KeyManager[] getkmf(Context context, String bksPath, String bksPassowd) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (TextUtils.isEmpty(bksPath)) {
            return null;
        }
        //客户端bks
        InputStream clientInput = context.getAssets().open(bksPath);
        KeyStore clientKey = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] cp = null;
        if (!TextUtils.isEmpty(bksPassowd)) {
            cp = bksPassowd.toCharArray();
        }
        clientKey.load(clientInput, cp);
        closeInputStream(clientInput);
        //密钥管理器
        String type = KeyManagerFactory.getDefaultAlgorithm();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(type);
        kmf.init(clientKey, cp);
        KeyManager[] keyManager = kmf.getKeyManagers();
        return keyManager;
    }

    //关闭 流
    private void closeInputStream(InputStream keyStoreInput) {
        if (keyStoreInput == null) {
            return;
        }
        try {
            keyStoreInput.close();
        } catch (IOException e) {
            e.printStackTrace();
            keyStoreInput = null;
        }
    }

    //设置证书域名验证
    public HostnameVerifier getHostnameVerifier(String[] hostName) {
        return new Hostname(hostName);
    }


    class Hostname implements HostnameVerifier {
        //信任服务器地址
        private String[] hostName;

        public Hostname(String[] hostName) {
            this.hostName = hostName;
        }

        @Override
        public boolean verify(String hostname, SSLSession session) {
            RLog.e("主机", hostname);
            if (hostName == null) {
                //信任任意主机
                return true;
            }
            boolean ret = false;
            for (String host : hostName) {
                if (TextUtils.isEmpty(host)) {
                    continue;
                }
                ret = host.equalsIgnoreCase(hostname);
                if (ret) {
                    break;
                }
            }
            return ret;
        }
    }
}

