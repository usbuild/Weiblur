package com.lecoding.view;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.lecoding.R;
import com.lecoding.activity.ViewImageActivity;
import com.lecoding.data.Status;
import com.loopj.android.image.SmartImageView;

/**
 * Created with IntelliJ IDEA.
 * User: usbuild
 * DateTime: 13-4-21 上午9:25
 */
public class Retweet extends LinearLayout {

    public Retweet(Context context) {
        super(context);
    }

    public Retweet(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.retweet, this, true);
    }

    public Retweet(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setData(final Status status, boolean origin) {
        TextView username = (TextView) findViewById(R.id.retweet_user);
        TextView text = (TextView) findViewById(R.id.retweet_text);
        SmartImageView thumbnail = (SmartImageView) findViewById(R.id.retweet_thumbnail);
        TextView cmtCount = (TextView) findViewById(R.id.retweet_comment_cnt);
        TextView rpCount = (TextView) findViewById(R.id.retweet_repost_cnt);
        TextView attCount = (TextView) findViewById(R.id.retweet_attitude_cnt);
        TextView source = (TextView) findViewById(R.id.weibo_item_rtsrc);
        if (status.getUser() != null)
            username.setText(status.getUser().getName());
        text.setText(status.getText());
        if (status.getThumbnailPic() != null) {
            if (origin) {
                thumbnail.setImageUrl(status.getBmiddlePic());
            } else {

                thumbnail.setImageUrl(status.getThumbnailPic());
            }
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getContext(), ViewImageActivity.class);
                    intent.putExtra("uri", status.getOriginalPic());
                    getContext().startActivity(intent);
                }
            });
        } else {
            thumbnail.setVisibility(View.GONE);
        }

        if (status.getPicDetails().size() != 0) {

        } else {
            thumbnail.setVisibility(View.GONE);
        }
        attCount.setText("赞(" + String.valueOf(status.getAttitudesCount()) + ")");
        cmtCount.setText("评论(" + String.valueOf(status.getCommentsCount()) + ")");
        rpCount.setText("转发(" + String.valueOf(status.getRepostsCount()) + ")");
        source.setText(Html.fromHtml("来自: " + status.getSource()));
    }
}
