package com.lecoding.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.*;
import android.widget.AdapterView;
import android.widget.Toast;
import com.lecoding.R;
import com.lecoding.data.KeywordProvider;
import com.lecoding.data.SourceProvider;
import com.lecoding.data.Status;
import com.lecoding.util.BlockValidator;
import com.lecoding.util.DuplicateUtil;
import com.lecoding.util.JSONParser;
import com.lecoding.util.WeiboAdapter;
import com.lecoding.view.PullToRefreshListView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by usbuild on 13-6-17.
 */
public abstract class AbstractTimelineFragment extends Fragment {

    /**
     * Called when the activity is first created.
     */
    protected final int WEIBO_ERROR = 0;
    protected final int PUBLIC_LINE = 1;
    protected final int RESET = 2;
    protected final int PAGE_SIZE = 20;
    protected final int UP_LOAD = 0;
    protected final int DOWN_LOAD = 1;
    Handler handler = null;
    protected PullToRefreshListView listView;
    protected long sinceId = 0;
    protected long maxId = 0;
    protected List<Status> oldStatuses = new ArrayList<Status>();
    protected List<Status> allStatues = new ArrayList<Status>();
    protected WeiboAdapter adapter;
    protected BlockValidator validator;
    protected SharedPreferences preferences;

    public List<Status> filterStatus(List<Status> statuses) {
        List<Status> sts = new ArrayList<Status>();
        for (Status st : statuses) {
            if (validator.validate(st)) {
                sts.add(st);
            }
        }
        return sts;
    }

    public void updateListView(List<Status> statuses, int direction) {

        if (statuses.size() > 0) {
            if (direction == 0) {
                if (oldStatuses.size() == 0) maxId = statuses.get(statuses.size() - 1).getId() - 1;
                this.oldStatuses.addAll(0, filterStatus(statuses));
                this.allStatues.addAll(0, statuses);
                if (statuses.size() > 0)
                    sinceId = statuses.get(0).getId();
            } else {
                this.oldStatuses.addAll(filterStatus(statuses));
                this.allStatues.addAll(statuses);

                maxId = statuses.get(statuses.size() - 1).getId() - 1;
            }
            Toast.makeText(getActivity(), "共更新" + statuses.size() + "条微博", Toast.LENGTH_SHORT).show();

            adapter.notifyDataSetChanged();
            if (oldStatuses.size() - statuses.size() == 0) {
                listView.setSelection(1);
                return;
            }
            if (direction == 0) {
                listView.setSelection(statuses.size());
            } else {
                listView.setSelection(oldStatuses.size() - statuses.size());
            }
        }
    }

    public void loadData() {
        listView.prepareForRefresh();
        loadUp();
    }

    public void refreshAll() {
        oldStatuses.clear();
        oldStatuses.addAll(filterStatus(allStatues));
        adapter.notifyDataSetChanged();
        listView.setSelection(1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ground, container, false);
        listView = (PullToRefreshListView) view.findViewById(R.id.ground_list);


        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Status status = (Status) listView.getAdapter().getItem(pos);
                Intent intent = new Intent(getActivity(), ViewWeiboActivity.class);
                intent.putExtra("status", status);
                startActivity(intent);
            }
        });


        listView.setLoadMore(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listView.getmLoadMore().setText("正在加载");
                listView.getmLoadMore().setClickable(false);
                loadDown();

            }
        });

        listView.setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadUp();
            }
        });
        registerForContextMenu(listView);

        if (BaseActivity.token != null) loadData();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new WeiboAdapter(this.getActivity(), oldStatuses);

        handler = new Handler(new Handler.Callback() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean handleMessage(Message message) {
                listView.onRefreshComplete();
                switch (message.what) {
                    case WEIBO_ERROR:
                        Toast.makeText(getActivity(), "拉取微博信息出错！", Toast.LENGTH_LONG).show();
                        break;
                    case PUBLIC_LINE:
                        updateListView((List<Status>) message.obj, message.arg1);
                        break;
                    case RESET:
                        refreshAll();
                        break;
                }
                return true;
            }
        });
        preferences = getActivity().getSharedPreferences(BlockActivity.DOMAIN, Context.MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                handler.sendEmptyMessage(RESET);
            }
        });

        getActivity().getContentResolver().registerContentObserver(SourceProvider.CONTENT_URI, false, new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                handler.sendEmptyMessage(RESET);
            }
        });

        getActivity().getContentResolver().registerContentObserver(KeywordProvider.CONTENT_URI, false, new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                handler.sendEmptyMessage(RESET);
            }
        });


        validator = BlockValidator.getInstance(getActivity());
    }

    private List<Status> parseJSON(String resp) {
        JSONTokener tokener = new JSONTokener(resp);
        try {
            JSONArray array = (JSONArray) ((JSONObject) tokener.nextValue()).get("statuses");
            List<com.lecoding.data.Status> statuses = new ArrayList<com.lecoding.data.Status>();
            for (int i = 0; i < array.length(); ++i) {
                JSONObject object = array.getJSONObject(i);
                Status st = JSONParser.parseStatus(object);
                statuses.add(st);
            }
            return statuses;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.weibo_ctx, menu);
        int postion = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
        Status status = (Status) adapter.getItem(postion - 1);
        menu.findItem(R.id.block).setTitle(Html.fromHtml("屏蔽来自： " + status.getSource()).toString());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int postion = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        Status status = (Status) adapter.getItem(postion - 1);

        Intent intent;
        switch (item.getItemId()) {
            case R.id.reply:
                intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra("type", PostActivity.COMMENT);
                intent.putExtra("status", status);
                startActivity(intent);
                break;
            case R.id.forward:
                intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra("type", PostActivity.FORWARD);
                intent.putExtra("status", status);
                startActivity(intent);
                break;
            case R.id.share:
                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                String text = status.getText();
                if (status.getRetweetedStatus() != null) {
                    text += "//@" + status.getRetweetedStatus().getUser().getScreenName() + ": " + status.getRetweetedStatus().getText();
                }
                intent.putExtra(Intent.EXTRA_TEXT, text);
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, "分享到"));
                break;
            case R.id.block:
                String source = Html.fromHtml(status.getSource()).toString();
                if (new DuplicateUtil(getActivity(), SourceProvider.traits).hasKey(source)) {
                    Toast.makeText(getActivity(), "已加入屏蔽列表", Toast.LENGTH_SHORT).show();
                }
                if (TextUtils.isEmpty(source)) {
                    Toast.makeText(getActivity(), "来源为空，无法添加", Toast.LENGTH_SHORT).show();
                }
                ContentValues values = new ContentValues();
                values.put(SourceProvider.NAME, source);
                getActivity().getContentResolver().insert(SourceProvider.CONTENT_URI, values);
                Toast.makeText(getActivity(), "添加成功", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    protected void setUp(String response) {
        Message message = new Message();
        message.what = PUBLIC_LINE;
        message.arg1 = UP_LOAD;
        message.obj = parseJSON(response);
        handler.sendMessage(message);
    }

    protected void setDown(String response) {
        Message message = new Message();
        message.what = PUBLIC_LINE;
        message.arg1 = DOWN_LOAD;
        message.obj = parseJSON(response);
        handler.sendMessage(message);
    }

    protected void loadError() {
        Message message = new Message();
        message.what = WEIBO_ERROR;
        handler.sendMessage(message);
    }

    protected abstract void loadUp();

    protected abstract void loadDown();
}
