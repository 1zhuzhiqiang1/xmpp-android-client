package com.yyquan.jzh.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.yyquan.jzh.util.SLog;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * Created by admin on 2017/7/30.
 */

public class XmppActivity extends Activity {

    protected XMPPConnection xmppConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String HOST = "47.93.197.48";
        int PORT = 5222;
        ConnectionConfiguration connConfig = new ConnectionConfiguration(HOST, PORT);
        connConfig.setReconnectionAllowed(true);
        connConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        xmppConnection = new XMPPConnection(connConfig);
        XMPPConnection.DEBUG_ENABLED = true;
        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
        final String tag = getLocalClassName();
        if (xmppConnection.isConnected()) {// 首先判断是否还连接着服务器，需要先断开
            try {
                xmppConnection.disconnect();
            } catch (Exception e) {
                SLog.i(tag, "conn.disconnect() failed: " + e);
            }
        }
        SmackConfiguration.setPacketReplyTimeout(30000);// 设置超时时间
        SmackConfiguration.setKeepAliveInterval(-1);
        SmackConfiguration.setDefaultPingInterval(0);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    xmppConnection.connect();
                    xmppConnection.addConnectionListener(new ConnectionListener() {

                        @Override
                        public void reconnectionSuccessful() {
                            // TODO Auto-generated method stub
                            SLog.i(tag, "重连成功");
                        }

                        @Override
                        public void reconnectionFailed(Exception arg0) {
                            // TODO Auto-generated method stub
                            SLog.i(tag, "重连失败");
                            Toast.makeText(XmppActivity.this, "连接服务器失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void reconnectingIn(int arg0) {
                            // TODO Auto-generated method stub
                            SLog.i(tag, "重连中");
                        }

                        @Override
                        public void connectionClosedOnError(Exception e) {
                            // TODO Auto-generated method stub
                            SLog.i(tag, "连接出错");
                            if (e.getMessage().contains("conflict")) {
                                SLog.i(tag, "被挤掉了");
                            }
                        }

                        @Override
                        public void connectionClosed() {
                            // TODO Auto-generated method stub
                            SLog.i(tag, "连接关闭");
                        }
                    });
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
