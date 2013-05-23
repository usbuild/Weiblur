package com.lecoding.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.lecoding.R;
import com.lecoding.data.User;
import com.lecoding.util.JSONParser;
import com.loopj.android.image.SmartImage;
import com.loopj.android.image.SmartImageView;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.api.UsersAPI;
import com.weibo.sdk.android.net.RequestListener;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by usbuild on 13-5-23.
 */
public class AccountActivity extends SherlockActivity {

    SmartImageView profileImg;
    Handler handler;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("主页");
        profileImg = (SmartImageView) findViewById(R.id.profile_img);

        Intent intent = getIntent();
        long uid = intent.getLongExtra("uid", 0);
        if (uid == 0) {
            Toast.makeText(getApplicationContext(), "用户不存在", Toast.LENGTH_LONG).show();
            this.finish();
        }

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                setData((User) message.obj);
                return true;
            }
        });


        UsersAPI usersAPI = new UsersAPI(BaseActivity.token);
        usersAPI.show(uid, new RequestListener() {
            @Override
            public void onComplete(String response) {
                try {

                    User user = JSONParser.parseUser(new JSONObject(response));
                    Message message = new Message();
                    message.obj = user;
                    handler.sendMessage(message);
                } catch (JSONException e) {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), "获取用户信息失败", Toast.LENGTH_LONG).show();
                    AccountActivity.this.finish();
                    Looper.loop();
                }
            }

            @Override
            public void onIOException(IOException e) {
                Looper.prepare();
                Toast.makeText(getApplicationContext(), "获取用户信息失败", Toast.LENGTH_LONG).show();
                AccountActivity.this.finish();
                Looper.loop();
            }

            @Override
            public void onError(WeiboException e) {
                Looper.prepare();
                Toast.makeText(getApplicationContext(), "获取用户信息失败", Toast.LENGTH_LONG).show();
                AccountActivity.this.finish();
                Looper.loop();
            }
        });
    }

    public void setData(User user) {
        profileImg.setImageUrl(user.getProfileImageUrl());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}