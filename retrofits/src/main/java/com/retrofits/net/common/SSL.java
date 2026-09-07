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
import javax.net.ssl.HttpsURLConnection;
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
 * HTTPS 证书配置用法：
 * <pre>
 * // 推荐用法：在 BaseUrl 的实现类（例如 UrlManger）中配置开关。
 * // BaseNetSource 创建 OkHttpClient 时会自动读取该配置，无需业务代码再次调用 SSL。
 * {@literal @}Override
 * public boolean isTrustAllCertificates() {
 *     return true; // true：信任所有证书和主机名；false：正常校验证书（默认值）
 * }
 *
 * // BaseNetSource 内部的等价调用：
 * new SSL().setSSL(builder, context, certificatePaths, hostNames,
 *         baseUrl.isTrustAllCertificates());
 * </pre>
 *
 * <p>警告：信任所有证书会关闭证书链和主机名校验，只能用于受控测试环境，
 * 正式环境必须设置为 {@code false}。</p>
 *
 * <p>证书设置（cer 证书单向验证）。</p>
 * Created by 郭敏 on 2018/4/20 0020.
 */

public class SSL {
    /**
     * @param builder         OkHttpClient.Builder
     * @param context         上下文
     * @param certificatePath 证书位置（在asset）如：zs.cer
     *                        [0] 服务端证书路径     //App 用来验证服务器的证书/信任库
     *                        [1] 服务端 BKS 密码   // 对应密码
     *                        [2] 客户端证书路径    // App 向服务器出示的客户端证书
     *                        [3] 客户端 BKS 密码   // 对应密码
     * @param hostName        信任的主机名
     * @return
     */
    public OkHttpClient.Builder setSSL(OkHttpClient.Builder builder, Context context,
                                       String[] certificatePath, String[] hostName) {
        return setSSL(builder, context, certificatePath, hostName, false);
    }

    /**
     * 配置 HTTPS 证书校验。
     *
     * @param trustAllCertificates true 时信任所有证书和主机名，仅允许在受控测试环境使用
     */
    public OkHttpClient.Builder setSSL(OkHttpClient.Builder builder, Context context,
                                       String[] certificatePath, String[] hostName,
                                       boolean trustAllCertificates) {
        if (trustAllCertificates) {
            // 明确开启开关时才进入不安全模式，正式环境必须保持关闭。
            return setTrustAllCertificates(builder);
        }
        if (context == null) {
            return builder;
        }
        //
        String[] temps = new String[4];
        if (certificatePath != null && certificatePath.length > 0) {
            int length = certificatePath.length;
            for (int i = 0; i < 4; i++) {
                if (i >= length) {
                    continue;
                }
                temps[i] = certificatePath[i];
            }
        }
        builder = setCertificate(builder, context, temps);
        builder.hostnameVerifier(getHostnameVerifier(hostName));
        return builder;
    }


    // =============================================打印证书信息========================================
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
    // =============================================验证证书========================================

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
            // 没有配置自定义证书时，必须使用系统默认信任库，禁止信任所有证书。
            X509TrustManager x509TrustManager;
            if (trustManager == null) {
                // 没有配置自定义证书
                x509TrustManager = getDefaultTrustManager();
            } else {
                // 已根据自定义 CER/BKS 创建了 TrustManager
                x509TrustManager = chooseTrustManager(trustManager);
            }
            if (x509TrustManager == null) {
                throw new CertificateException("没有可用的 X509TrustManager");
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



    // 获取系统默认的证书信任管理器。
    private X509TrustManager getDefaultTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        return chooseTrustManager(trustManagerFactory.getTrustManagers());
    }
    //找到第一个负责 X.509 证书校验的管理器。
    private X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            // 判断当前管理器是否支持 X.509 证书校验
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
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

    //设置域名
    class Hostname implements HostnameVerifier {
        //信任服务器地址
        private String[] hostName;

        public Hostname(String[] hostName) {
            this.hostName = hostName;
        }

        @Override
        public boolean verify(String hostname, SSLSession session) {
            RLog.e("主机", hostname);
            if (hostName == null || hostName.length == 0) {
                // 未配置主机名时使用系统校验，不能默认信任任意域名。
                return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session);
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
    // =============================================信任所有证书========================================
    // 信任所有证书并跳过主机名校验，仅供受控测试环境排查证书问题。
    private OkHttpClient.Builder setTrustAllCertificates(OkHttpClient.Builder builder) {
        try {
            X509TrustManager trustManager = new UnSafeTrustManager();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // 信任所有主机名，否则自签名证书仍可能因域名不匹配而失败。
                    return true;
                }
            });
            RLog.e("SSL", "警告：当前已开启信任所有 HTTPS 证书模式");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("开启信任所有证书模式失败", e);
        }
        return builder;
    }
    class UnSafeTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // 接受任意客户端证书
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // 接受任意服务端证书
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }
}

