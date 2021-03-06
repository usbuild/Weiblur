package com.lecoding.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.lecoding.R;
import com.lecoding.data.Comment;
import com.lecoding.data.PicDetail;
import com.lecoding.data.Status;
import com.lecoding.util.CommentAdapter;
import com.lecoding.util.JSONParser;
import com.lecoding.util.RepostAdapter;
import com.lecoding.view.PicList;
import com.lecoding.view.Retweet;
import com.loopj.android.image.SmartImageView;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.api.CommentsAPI;
import com.weibo.sdk.android.api.FavoritesAPI;
import com.weibo.sdk.android.api.StatusesAPI;
import com.weibo.sdk.android.api.WeiboAPI;
import com.weibo.sdk.android.net.RequestListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: usbuild
 * DateTime: 13-4-18 上午9:38
 */
public class ViewWeiboActivity extends SherlockActivity {
    private final int UPDATE_CMT = 0;
    private final int UPDATE_REPO = 1;
    public final int LOVE = 2;
    public final int UNLOVE = 3;

    private TextView weiboText;
    private SmartImageView profileImg;
    private SmartImageView thumbnail;
    private TextView weiboUser;
    private PicList picList;
    private Retweet retweet;
    private TextView commentCount;
    private TextView repostCount;
    private TextView attitudeCount;
    private ListView commentList;
    private TextView source;
    private CommentAdapter commentAdapter;
    private RepostAdapter repostAdapter;
    private List<Comment> comments;
    private List<Status> reposts;
    private Status status;
    public final static int COMMENT = 0;
    public final static int REPOST = 1;

    private int type = COMMENT;
    private MenuItem loveItem;


    private Handler handler;


    public void updateCommentList(List<Comment> comments) {
        commentList.setAdapter(new CommentAdapter(this, comments));
        commentCount.setTextColor(getResources().getColor(R.color.lightblue));
        repostCount.setTextColor(getResources().getColor(android.R.color.black));
        type = COMMENT;
    }

    public void updateRepostList(List<Status> statuses) {
        commentList.setAdapter(new RepostAdapter(this, statuses));
        repostCount.setTextColor(getResources().getColor(R.color.lightblue));
        commentCount.setTextColor(getResources().getColor(android.R.color.black));
        type = REPOST;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        commentList = new ListView(this);
        //set list options
        commentList.setHeaderDividersEnabled(false);
        commentList.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View view = getLayoutInflater().inflate(R.layout.view_weibo, null);
        commentList.addHeaderView(view, null, true);

        setContentView(commentList);

        repostAdapter = new RepostAdapter(this, reposts);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        comments = new ArrayList<Comment>();
        reposts = new ArrayList<Status>();

        weiboText = (TextView) findViewById(R.id.weibo_item_text);
        weiboText.setMovementMethod(LinkMovementMethod.getInstance());

        profileImg = (SmartImageView) findViewById(R.id.profile_img);
        weiboUser = (TextView) findViewById(R.id.weibo_item_user);


        thumbnail = (SmartImageView) findViewById(R.id.thumbnail);
        picList = (PicList) findViewById(R.id.piclist);
        retweet = (Retweet) findViewById(R.id.retweet);

        attitudeCount = (TextView) findViewById(R.id.weibo_item_attitude_cnt);
        commentCount = (TextView) findViewById(R.id.weibo_item_comment_cnt);
        repostCount = (TextView) findViewById(R.id.weibo_item_repost_cnt);

        source = (TextView) findViewById(R.id.weibo_item_src);
        source.setMovementMethod(LinkMovementMethod.getInstance());



        status = (Status) getIntent().getSerializableExtra("status");

        weiboUser.setText(status.getUser().getScreenName());

        attitudeCount.setText("赞(" + status.getAttitudesCount() + ")");
        commentCount.setText("评论(" + status.getCommentsCount() + ")");
        repostCount.setText("转发(" + status.getRepostsCount() + ")");
        source.setText(Html.fromHtml("来自: " + status.getSource()));

        weiboText.setText(status.getText());
        profileImg.setImageUrl(status.getUser().getProfileImageUrl());

        List<PicDetail> picDetails = status.getPicDetails();

        if (status.getRetweetedStatus() != null) {
            retweet.setVisibility(View.VISIBLE);
            retweet.setData(status.getRetweetedStatus(), true);
            retweet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ViewWeiboActivity.this, ViewWeiboActivity.class);
                    intent.putExtra("status", status.getRetweetedStatus());
                    startActivity(intent);
                }
            });
        } else {
            if (picDetails.size() <= 1) {
                thumbnail.setImageUrl(status.getBmiddlePic());
                thumbnail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(ViewWeiboActivity.this, ViewImageActivity.class);
                        intent.putExtra("uri", status.getOriginalPic());
                        startActivity(intent);
                    }
                });
            } else {
                List<String> urls = new ArrayList<String>();
                for (PicDetail picDetail : picDetails) {
                    urls.add(picDetail.getThumbnailPic());
                }
                picList.setImages(urls);
            }
        }

        handler = new Handler(new Handler.Callback() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case UPDATE_CMT:
                        updateCommentList((List<Comment>) message.obj);
                        break;
                    case UPDATE_REPO:
                        updateRepostList((List<Status>) message.obj);
                        break;
                    case LOVE:
                        loveItem.setIcon(R.drawable.rating_favorite_r);
                        Toast.makeText(ViewWeiboActivity.this, loveItem.getTitle(), Toast.LENGTH_LONG).show();
                        status.setFavorited(true);
                        break;
                    case UNLOVE:
                        loveItem.setIcon(R.drawable.rating_favorite);
                        status.setFavorited(false);
                        break;

                }
                return false;
            }
        });

        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ViewWeiboActivity.this, AccountActivity.class);
                intent.putExtra("uid", status.getUser().getId());
                startActivity(intent);
            }
        });

        repostCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadReposts();
            }
        });

        commentCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadComments();
            }
        });

        attitudeCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });


        commentAdapter = new CommentAdapter(this, comments);
        commentList.setAdapter(commentAdapter);

        loadComments();

        /*
        commentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (type == COMMENT) {
                    Comment comment = (Comment) commentList.getAdapter().getItem(i);
                    Intent intent = new Intent(ViewWeiboActivity.this, AccountActivity.class);
                    intent.putExtra("uid", comment.getUser().getId());
                    startActivity(intent);
                } else {
                    Status status = (Status) commentList.getAdapter().getItem(i);
                    Intent intent = new Intent(ViewWeiboActivity.this, ViewWeiboActivity.class);
                    intent.putExtra("status", status);
                    startActivity(intent);
                }
            }
        });
        */
    }

    public void loadReposts() {
        StatusesAPI statusesAPI = new StatusesAPI(BaseActivity.token);
        statusesAPI.repostTimeline(status.getId(), 0, 0, 20, 1, WeiboAPI.AUTHOR_FILTER.ALL, new RequestListener() {
            @Override
            public void onComplete(String response) {
                JSONTokener tokener = new JSONTokener(response);
                try {
                    JSONArray jsonArray = ((JSONObject) tokener.nextValue()).getJSONArray("reposts");
                    List<Status> statuses = new ArrayList<Status>();
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        statuses.add(JSONParser.parseStatus(jsonObject));
                    }

                    Message message = new Message();
                    message.obj = statuses;
                    message.what = UPDATE_REPO;
                    handler.sendMessage(message);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIOException(IOException e) {

            }

            @Override
            public void onError(WeiboException e) {

            }
        });
    }

    public void loadComments() {
        CommentsAPI commentsAPI = new CommentsAPI(BaseActivity.token);
        commentsAPI.show(status.getId(), 0, 0, 20, 1,
                WeiboAPI.AUTHOR_FILTER.ALL
                , new RequestListener() {
            @Override
            public void onComplete(String response) {
                JSONTokener tokener = new JSONTokener(response);
                try {
                    JSONArray jsonArray = ((JSONObject) tokener.nextValue()).getJSONArray("comments");
                    List<Comment> comments = new ArrayList<Comment>();
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        comments.add(JSONParser.parseComment(jsonObject));
                    }

                    Message message = new Message();
                    message.obj = comments;
                    message.what = UPDATE_CMT;
                    handler.sendMessage(message);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIOException(IOException e) {

            }

            @Override
            public void onError(WeiboException e) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(ViewWeiboActivity.this, PostActivity.class);
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;

            case R.id.menu_comment:
                intent.putExtra("type", PostActivity.COMMENT);
                intent.putExtra("status", status);
                startActivityForResult(intent, 0);
                return true;

            case R.id.menu_forward:
                intent.putExtra("type", PostActivity.FORWARD);
                intent.putExtra("status", status);
                startActivityForResult(intent, 1);
                return true;

            case R.id.menu_favorite:
                love();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void love() {
        FavoritesAPI favoritesAPI = new FavoritesAPI(BaseActivity.token);
        if (status.isFavorited()) {
            favoritesAPI.destroy(status.getId(), new RequestListener() {
                @Override
                public void onComplete(String response) {
                    Message message = new Message();
                    message.what = UNLOVE;
                    handler.sendMessage(message);
                }

                @Override
                public void onIOException(IOException e) {
                    Log.e("abc", e.toString());
                }

                @Override
                public void onError(WeiboException e) {
                    Log.e("abc", e.toString());
                }
            });
        } else {
            favoritesAPI.create(status.getId(), new RequestListener() {
                @Override
                public void onComplete(String response) {
                    Message message = new Message();
                    message.what = LOVE;
                    handler.sendMessage(message);
                }

                @Override
                public void onIOException(IOException e) {
                    Log.e("abc", e.toString());
                }

                @Override
                public void onError(WeiboException e) {
                    Log.e("abc", e.toString());
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.weibo, menu);
        loveItem = menu.findItem(R.id.menu_favorite);
        if (status.isFavorited()) {
            loveItem.setIcon(R.drawable.rating_favorite_r);
        }
        loveItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        loadComments();
    }
}