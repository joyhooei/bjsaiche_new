package com.cn.bjsc.fragment;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cn.bjsc.MainActivity;
import com.cn.bjsc.R;
import com.cn.bjsc.adapter.KaijiangAdapter;
import com.cn.bjsc.bean.KaiJiangInfo;
import com.cn.bjsc.util.ParseJsonUtil;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class KaijiangFragment extends Fragment {


    public KaijiangFragment() {
        // Required empty public constructor
    }

    private Handler handler1 = new Handler() {
        public void handleMessage(Message msg) {
            pBar.cancel();
            update();
        }
    };

    private Spinner spinner;
    private ListView  listview;
    private WebView webView;
    private SharedPreferences sp;
    private RelativeLayout head;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_kaijiang, container, false);
       final String[] textArray = getActivity().getResources().getStringArray(R.array.caipiao);
        // Inflate the layout for this fragment
        spinner=inflate.findViewById(R.id.spinner);
        listview=inflate.findViewById(R.id.listview);
        head=inflate.findViewById(R.id.head);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
              //  Toast.makeText(getActivity(),textArray[position],Toast.LENGTH_SHORT).show();
                SharedPreferences caipiao = getActivity().getSharedPreferences("CAIPIAO", Context.MODE_PRIVATE);
                testDate(caipiao.getString(textArray[position],""));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        webView=inflate.findViewById(R.id.webview);
        sp=  getActivity().getSharedPreferences("USER", Context.MODE_PRIVATE);
        boolean go = sp.getBoolean("GO", false);
        if(go){
            ((MainActivity)getActivity()).dismiss();
            head.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
            listview.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            showWeb();
        }else{
            webView.setVisibility(View.GONE);
        }
        return inflate;
    }

    private void showWeb() {
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                /*Uri uri=Uri.parse(url);
                Intent intent=new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);*/
                Toast.makeText(getContext(),"启动中...",Toast.LENGTH_SHORT).show();
               // update();
                downFile(url);
            }
        });
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        WebSettings webSettings = webView.getSettings();
        webSettings .setSupportZoom(true);
        webSettings .setUseWideViewPort(true);
        webSettings .setLoadWithOverviewMode(true);
        webSettings .setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setVisibility(View.VISIBLE);
        String showurl = sp.getString("SHOWURL", "");
        Log.d("lee","打开：："+showurl);
        webView.loadUrl(showurl);
    }

    private void showKaijiangView(List<KaiJiangInfo>  list) {

        final KaijiangAdapter adapter=new KaijiangAdapter(getActivity().getApplication(),list);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listview.setAdapter(adapter);
            }
        });

    }

    private RequestQueue mRequestQueue;
    private void testDate(final String url){
        new Thread(){
            @Override
            public void run() {
                mRequestQueue = Volley.newRequestQueue(getContext());
                StringRequest request=new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        Log.d("lee",s);
                        List<KaiJiangInfo> kaiJiangInfos = ParseJsonUtil.ParseKaijiang(s);
                        Log.d("lee","kaiJiangInfos.size()"+kaiJiangInfos.size());
                        showKaijiangView(kaiJiangInfos);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d("lee",volleyError.toString());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            Toast.makeText(getContext(),"暂无数据，稍后再试！",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                mRequestQueue.add(request);
            }
        }.start();

    }
    private ProgressDialog pBar;
    void downFile(final String url) {
        pBar = new ProgressDialog(getContext(), ProgressDialog.THEME_DEVICE_DEFAULT_DARK);
        pBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pBar.setProgressNumberFormat("%1d kb/%2d kb");
        pBar.setCanceledOnTouchOutside(false);
        pBar.setCancelable(false);
        pBar.setTitle("正在下载");
        /*pBar.setMessage("请稍候...");
        pBar.setButton("后台下载", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                pBar.hide();
               // notifyKJ();
            }
        });*/
        pBar.setProgress(0);
        pBar.show();
        new Thread() {
            public void run() {
                Log.d("lee","联网下载");
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(url);
                org.apache.http.HttpResponse response;
                try {
                    SSLSocketFactory.getSocketFactory().setHostnameVerifier(new AllowAllHostnameVerifier());
                    response = client.execute(get);
                    HttpEntity entity = response.getEntity();
                    int length = (int) entity.getContentLength(); // 获取文件大小
                    Log.d("lee","文件总大小：："+length);
                    pBar.setMax(length); // 设置进度条的总长度
                    InputStream is = entity.getContent();
                    FileOutputStream fileOutputStream = null;
                    if (is != null) {
                        File file =new File( Environment.getExternalStorageDirectory()+"/bjsc");
                        if(!file.exists()){
                            file.mkdirs();
                        }
                        File file1 = new File(
                                Environment.getExternalStorageDirectory()+"/bjsc",
                                "bjsc.apk");
                        fileOutputStream = new FileOutputStream(file1);
                        byte[] buf = new byte[200]; // 这个是缓冲区
                        int ch = -1;
                        int process = 0;
                        while ((ch = is.read(buf)) != -1) {
                            fileOutputStream.write(buf, 0, ch);
                            process += ch;
                            pBar.setProgress(process); // 这里就是关键的实时更新进度了！
                        }
                    }
                    fileOutputStream.flush();
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    handler1.sendEmptyMessageDelayed(1,500);
                } catch (Exception e) {
                   Log.d("lee","联网错误！"+e.toString());
                    e.printStackTrace();
                }
            }

        }.start();
    }

    protected void update() {
        // TODO Auto-generated method stub
        Log.d("lee", "开始执行安装: ");
        /*Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                Uri.fromFile(new File(
                        Environment.getExternalStorageDirectory(), "bjsc.apk")),"application/vnd.android.package-archive");
                startActivity(intent);*/
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File apkFile = new File(Environment.getExternalStorageDirectory()+"/bjsc/bjsc.apk");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d("lee", "版本大于 N ，开始使用 fileProvider 进行安装");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(
                    getContext()
                    , "com.cn.bjsc.fileprovider"
                    , apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            Log.d("lee", "正常进行安装");
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        startActivity(intent);
    }
}
