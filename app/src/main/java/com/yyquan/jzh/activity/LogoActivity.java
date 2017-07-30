package com.yyquan.jzh.activity;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushManager;

import com.yyquan.jzh.R;
import com.yyquan.jzh.entity.Ip;
import com.yyquan.jzh.entity.User;
import com.yyquan.jzh.util.SaveUserUtil;
import com.yyquan.jzh.util.SharedPreferencesUtil;

import com.yyquan.jzh.xmpp.XmppService;
import com.yyquan.jzh.xmpp.XmppTool;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;


public class LogoActivity extends Activity {
    boolean is_login;
    boolean is_lock;

    private String url = Ip.ip + "/YfriendService/DoGetUser";

    private final int AUTO_LOGIN = 1;
    private final int XMPP_LOGIN = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);
        is_login = SharedPreferencesUtil.getBoolean(this, "user_message", "login");
        is_lock = SharedPreferencesUtil.getBoolean(this, "user_message", "lock");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(LogoActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        }, 2000);

//        new Thread() {
//            @Override
//            public void run() {
//                if (is_lock) {
//                    Intent intent = new Intent(LogoActivity.this, LockActivity.class);
//                    startActivity(intent);
//                    finish();
//                } else {
//                    Message m = handler.obtainMessage(AUTO_LOGIN);
//                    handler.sendMessage(m);
//                }
//            }
//        };
    }

    //
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case AUTO_LOGIN:
                    is_auto_login();
                    break;
                case XMPP_LOGIN:
                    xmppLogin(msg);
                    break;
            }
        }
    };

    /**
     * 登陆聊天服务器
     *
     * @param msg
     */
    private void xmppLogin(Message msg) {
        final User users = (User) msg.obj;
        new Thread() {
            @Override
            public void run() {
                boolean result = XmppTool.getInstance(LogoActivity.this).login(users.getUser(), users.getPassword(), LogoActivity.this);
                if (result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startService(new Intent(LogoActivity.this, XmppService.class));
                            Intent intent = new Intent(LogoActivity.this, MainActivity.class);
                            intent.putExtra("user", users);
                            regster_push(users.getUser());
                            SaveUserUtil.saveAccount(LogoActivity.this, users);
                            SharedPreferencesUtil.setBoolean(LogoActivity.this, "user_message", "login", true);
                            startActivity(intent);
                            finish();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LogoActivity.this, "登陆失败,请重试", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }.start();
    }


    /**
     * 先判断有没有自动登录
     */
    private void is_auto_login() {
        if (is_login) {
            User user = SaveUserUtil.loadAccount(LogoActivity.this);
            login(user.getUser(), user.getPassword());
        } else {
            startActivity(new Intent(LogoActivity.this, LoginActivity.class));
            finish();
        }
    }


    /**
     * 登录自己的服务器
     */
    private void login(String user, String password) {
        RequestParams params = new RequestParams();
        params.put("user", user);
        params.put("password", password);
        params.put("action", "login");
        AsyncHttpClient client = new AsyncHttpClient();

        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String str = new String(responseBody);
                if (str != null) {
                    try {
                        JSONObject object = new JSONObject(str);
                        if (object.getString("code").equals("success")) {


                            object = object.getJSONObject("data");
                            User user = new User();
                            user.setUser(object.getString("user"));
                            user.setPassword(object.getString("password"));
                            user.setQq(object.getString("qq"));
                            user.setIcon(object.getString("icon"));
                            user.setNickname(object.getString("nickname"));
                            user.setCity(object.getString("city"));
                            user.setSex(object.getString("sex"));
                            user.setYears(object.getString("years"));
                            user.setQianming(object.getString("qianming"));
                            regster_push(user.getUser());
//                            if (!SharedPreferencesUtil.getBoolean(LogoActivity.this, "xmpp", "create" + user.getUser())) {
//                                Message m = handler.obtainMessage(IS_CREATE);
//                                m.obj = user;
//                                handler.sendMessage(m);
//
//                            } else {

                            Message m = handler.obtainMessage(XMPP_LOGIN);
                            m.obj = user;
                            handler.sendMessage(m);

//                            }


                        } else {
                            startActivity(new Intent(LogoActivity.this, LoginActivity.class));
                            finish();
                            Toast.makeText(LogoActivity.this, "账号或密码有误，请重新输入", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        startActivity(new Intent(LogoActivity.this, LoginActivity.class));
                        finish();
                    }
                } else {
                    startActivity(new Intent(LogoActivity.this, LoginActivity.class));
                    finish();
                }
            }


            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(LogoActivity.this, "网络连接失败，请查看网络设置", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LogoActivity.this, LoginActivity.class));
                finish();
            }
        });
    }


    /**
     * 注册信鸽
     */
    private void regster_push(String user) {
        XGPushManager.registerPush(getApplicationContext(), user, new XGIOperateCallback() {
            @Override
            public void onSuccess(Object o, int i) {
                // Toast.makeText(LogoActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(Object o, int i, String s) {
                //  Toast.makeText(LogoActivity.this, "注册失败" + i + "\n" + s, Toast.LENGTH_SHORT).show();
            }
        });
    }


}
