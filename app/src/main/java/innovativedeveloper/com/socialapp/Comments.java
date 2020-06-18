package innovativedeveloper.com.socialapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import innovativedeveloper.com.socialapp.adapter.CommentsAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Comment;
import innovativedeveloper.com.socialapp.dataset.Feed;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.services.AppService;

public class Comments extends AppCompatActivity implements CommentsAdapter.OnItemClickListener, AppService.OnCommentStatusChanged {

    RecyclerView recyclerView;
    EditText txtComment;
    ImageView btnSend;
    LinearLayout mainLayout, commentLayout;
    Toolbar toolbar;
    CommentsAdapter cAdapter;
    ArrayList<Comment> commentsList;
    String postId, commentId;
    boolean isDisabled;
    boolean isOwnPost = false;
    boolean isReply = false;
    View bottomView;
    AdView mAdView;
    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.push_up, R.anim.slide_out);
        setContentView(R.layout.activity_comments);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        mAdView = (AdView) findViewById(R.id.adView);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        bottomView = findViewById(R.id.bottomView);
        commentsList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        txtComment = (EditText) findViewById(R.id.txtComment);
        btnSend = (ImageView) findViewById(R.id.btnSend);
        mainLayout = (LinearLayout) findViewById(R.id.layoutMain);
        commentLayout = (LinearLayout) findViewById(R.id.commentLayout);

        // Getting parameters
        Bundle param = getIntent().getExtras();
        if (param != null) {
            postId = param.getString("postId");
            commentId = param.getString("commentId", "0");
            isDisabled = param.getBoolean("isDisabled");
            isOwnPost = param.getBoolean("isOwnPost");
            isReply = param.getBoolean("isReply");
        }

        if (isDisabled) {
            bottomView.setVisibility(View.GONE);
            txtComment.setEnabled(false);
            commentLayout.setVisibility(View.INVISIBLE);
        }

        txtComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (txtComment.length() > 0) {
                    btnSend.setEnabled(true);
                } else {
                    btnSend.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        cAdapter = new CommentsAdapter(this, commentsList, isOwnPost, isReply);
        cAdapter.setOnItemClickListener(this);
        cAdapter.setAnimationsLocked(true);
        recyclerView.setAdapter(cAdapter);
        recyclerView.addItemDecoration(new ItemDivider(this));
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    cAdapter.setAnimationsLocked(true);
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String UTCTime = sdf.format(new Date());
                Comment c = new Comment();
                User u = new User();
                c.setPostId(postId);
                c.setCommentId(commentId);
                u.setUsername(AppHandler.getInstance().getDataManager().getString("username", ""));
                u.setName(AppHandler.getInstance().getDataManager().getString("name", ""));
                u.setProfilePhoto(AppHandler.getInstance().getDataManager().getString("profilePhoto", ""));
                c.setUser(u);
                c.setContent(txtComment.getText().toString());
                c.setContent("#$0" + u.getName() + "#space" + c.getContent());
                c.setCreation(UTCTime);
                commentsList.add(c);
                if (isReply) {
                    addReply(txtComment.getText().toString(), c);
                } else {
                    addComment(txtComment.getText().toString(), c);
                }
                cAdapter.addItem();
                txtComment.setText("");
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(txtComment.getWindowToken(), 0);
            }
        });
        cAdapter.updateItems();
        animateContent();

        if (Config.ENABLE_ACTIVITY_COMMENTS_BANNER) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.VISIBLE);
        }

        if (isReply) {
            setTitle("Replies");
            txtComment.setHint("Add a reply...");
            LoadReplies();
        } else {
            LoadComments();
        }

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                if (intent.getAction().equals("notification")) {
                    if (isReply) {
                        if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_REPLY))) {
                            if (intent.getStringExtra("commentId").equals(commentId)) {
                                LoadReplies();
                            }
                        }
                    } else {
                        if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_NOTIFICATION))) {
                            if (intent.hasExtra("commentId")) {
                                if (intent.getStringExtra("postId").equals(postId)) {
                                    LoadComments();
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    private void addComment(final String comment, final Comment c) {
        class AddComment extends AsyncTask<Void, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
            }

            @Override
            protected String doInBackground(Void... params) {
                AppHandler.getInstance().getAppService().addComment(comment, postId, c);
                return "";
            }
        }
        AppHandler.getInstance().getAppService().setOnCommentStatusChanged(this);
        AddComment addComment = new AddComment();
        addComment.execute();
    }

    private void addReply(final String reply, final Comment c) {
        class AddReply extends AsyncTask<Void, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
            }

            @Override
            protected String doInBackground(Void... params) {
                AppHandler.getInstance().getAppService().addReply(reply, postId, c);
                return "";
            }
        }
        AppHandler.getInstance().getAppService().setOnCommentStatusChanged(this);
        AddReply addReply = new AddReply();
        addReply.execute();
    }

    @Override
    public void onCommentChanged(String response, Comment c) {
        try {
            JSONObject obj = new JSONObject(response);
            if (!obj.getBoolean("error")) {
                commentsList.get(commentsList.indexOf(c)).setCommentId(obj.getString("id"));
                recyclerView.smoothScrollToPosition(commentsList.size());
                cAdapter.notifyItemChanged(commentsList.indexOf(c));
            } else {
                Log.e("Comments", "Server response: " + obj.getString("code"));
                commentsList.remove(c);
                Toast.makeText(Comments.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
            }
        }
        catch (JSONException ex) {
            Log.e("Comments", "Unexpected error: " + ex.getMessage());
            commentsList.remove(c);
            Toast.makeText(Comments.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommentErrorResponse(VolleyError error, Comment c) {
        Log.e("Comments", "Unexpected error: " + error.getMessage());
        commentsList.remove(c);
        Toast.makeText(Comments.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
    }

    private void animateContent() {
        commentLayout.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).setDuration(200).start();
    }

    private void LoadReplies() {
        StringRequest request = new StringRequest(Request.Method.GET, Config.LOAD_REPLIES + commentId, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    commentsList.clear();
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray comments = obj.getJSONArray("comments");
                        if (comments.length() != 0) {
                            for (int i = 0; i < comments.length(); i++) {
                                JSONObject comment = comments.getJSONObject(i);
                                Comment c = new Comment();
                                User u = new User();
                                c.setCommentId(comment.getString("id"));
                                u.setUsername(comment.getString("username"));
                                u.setProfilePhoto(comment.getString("icon"));
                                u.setName(comment.getString("name"));
                                c.setUser(u);
                                c.setContent("#$0" + u.getName() + "#space" + comment.getString("content"));
                                c.setCreation(comment.getString("creation"));
                                commentsList.add(c);
                            }
                            cAdapter.updateItems();
                            recyclerView.smoothScrollToPosition(commentsList.size());
                        }
                    } else {
                        Toast.makeText(Comments.this, "Couldn't load", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (JSONException ex) {
                    Log.e("Comments", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(Comments.this, "Couldn't load", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Comments", "Unexpected error: " + error.getMessage());
                Toast.makeText(Comments.this, "Couldn't load", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    private void LoadComments() {
        StringRequest request = new StringRequest(Request.Method.GET, Config.LOAD_COMMENTS + postId, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    commentsList.clear();
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        JSONArray comments = obj.getJSONArray("comments");
                        isOwnPost = obj.getBoolean("isOwnPost");
                        cAdapter.setOwnPost(isOwnPost);
                        if (comments.length() != 0) {
                            for (int i = 0; i < comments.length(); i++) {
                                JSONObject comment = comments.getJSONObject(i);
                                Comment c = new Comment();
                                User u = new User();
                                c.setCommentId(comment.getString("id"));
                                c.setPostId(comment.getString("post_id"));
                                u.setUsername(comment.getString("username"));
                                u.setProfilePhoto(comment.getString("icon"));
                                u.setName(comment.getString("name"));
                                c.setUser(u);
                                c.setContent("#$0" + u.getName() + "#space" + comment.getString("content"));
                                c.setCreation(comment.getString("creation"));
                                c.setReplies(comment.getInt("replies"));
                                commentsList.add(c);
                            }
                            cAdapter.updateItems();
                            recyclerView.smoothScrollToPosition(commentsList.size());
                        }
                    } else {
                        Toast.makeText(Comments.this, "Couldn't load", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (JSONException ex) {
                    Log.e("Comments", "Unexpected error: " + ex.getMessage());
                    Toast.makeText(Comments.this, "Couldn't load", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Comments", "Unexpected error: " + error.getMessage());
                Toast.makeText(Comments.this, "Couldn't load", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return AppHandler.getInstance().getAuthorization();
            }
        };
        AppHandler.getInstance().addToRequestQueue(request);
    }

    @Override
    public void onReplyClick(View v, int position) {
        final Intent intent = new Intent(Comments.this, Comments.class);
        intent.putExtra("postId", postId);
        intent.putExtra("commentId", commentsList.get(position).getCommentId());
        intent.putExtra("isDisabled", isDisabled);
        intent.putExtra("isOwnPost", isOwnPost);
        intent.putExtra("isReply", true);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onRepliesClick(View v, int position) {
        final Intent intent = new Intent(Comments.this, Comments.class);
        intent.putExtra("postId", postId);
        intent.putExtra("commentId", commentsList.get(position).getCommentId());
        intent.putExtra("isDisabled", isDisabled);
        intent.putExtra("isOwnPost", isOwnPost);
        intent.putExtra("isReply", true);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onProfileClick(View v, int position) {
        UserProfile.startUserProfile(this, commentsList.get(position).getUser().getUsername(), commentsList.get(position).getUser().getName());
    }

    @Override
    public void onMoreClick(View v, final int position) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_delete) {
                    final String cId = commentsList.get(position).getCommentId();
                    commentsList.remove(position);
                    cAdapter.notifyItemRemoved(position);
                    StringRequest request = new StringRequest(Request.Method.PUT, isReply ? Config.DELETE_REPLY.replace(":commentId", commentId) : Config.DELETE_COMMENT.replace(":postId", postId), new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject obj = new JSONObject(response);
                                if (!obj.getBoolean("error")) {
                                    Toast.makeText(Comments.this, "Comment deleted", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e("Comments", "Server response: " + obj.getString("code"));
                                    //cAdapter.notifyItemInserted(position);
                                    Toast.makeText(Comments.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                                }
                            }
                            catch (JSONException ex) {
                                Log.e("Comments", "Unexpected error: " + ex.getMessage());
                                //cAdapter.notifyItemInserted(position);
                                Toast.makeText(Comments.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Comments", "Unexpected error: " + error.getMessage());
                            //cAdapter.notifyItemInserted(position);
                            Toast.makeText(Comments.this, "Your request was not proceed", Toast.LENGTH_SHORT).show();
                        }
                    }) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            return AppHandler.getInstance().getAuthorization();
                        }

                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("id", cId);
                            return params;
                        }
                    };
                    int socketTimeout = 0;
                    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                    request.setRetryPolicy(policy);
                    AppHandler.getInstance().addToRequestQueue(request);
                }
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_comments);
        popupMenu.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("notification"));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }
}
