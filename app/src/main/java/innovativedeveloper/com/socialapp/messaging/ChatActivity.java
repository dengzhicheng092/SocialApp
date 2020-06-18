package innovativedeveloper.com.socialapp.messaging;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import innovativedeveloper.com.socialapp.ActivityPost;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.UserProfile;
import innovativedeveloper.com.socialapp.adapter.ChatAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Message;
import innovativedeveloper.com.socialapp.dataset.User;
import innovativedeveloper.com.socialapp.services.AppService;

public class ChatActivity extends AppCompatActivity implements AppService.OnMessagingStatusChanged, ChatAdapter.OnItemClickListener,
        EmojiconGridFragment.OnEmojiconClickedListener, EmojiconsFragment.OnEmojiconBackspaceClickedListener{

    Toolbar toolbar;
    RecyclerView recyclerView;
    String username, name;
    User u;
    EditText txtMessage;
    ImageButton btnEmoticon, btnSpeech, btnSend, btnCancel;
    BroadcastReceiver broadcastReceiver;
    View chat_main, viewEmoticons;
    ChatAdapter chatAdapter;
    ArrayList<Message> messagesArrayList = new ArrayList<>();
    int type = 1;
    SimpleDateFormat crTime;
    Calendar c;
    boolean isShare;
    boolean isEmoticonsVisible = false;

    public static void startActivity(Activity startingActivity, String username, String name) {
        Intent intent = new Intent(startingActivity, ChatActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("name", name);
        startingActivity.startActivity(intent);
    }

    public static void startActivity(Activity startingActivity, String username, String name, boolean isShare, String message) {
        Intent intent = new Intent(startingActivity, ChatActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("name", name);
        intent.putExtra("isShare", isShare);
        intent.putExtra("message", message);
        startingActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        setContentView(R.layout.activity_chat);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(name);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        chatAdapter = new ChatAdapter(this, messagesArrayList);
        chatAdapter.setOnItemClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        getSupportFragmentManager().beginTransaction().replace(R.id.viewEmoticons, EmojiconsFragment.newInstance(Build.VERSION.SDK_INT >= 20)).commit();
        btnEmoticon = (ImageButton) findViewById(R.id.btnEmoticons);
        btnEmoticon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowEmoticons();
            }
        });
        btnSpeech = (ImageButton) findViewById(R.id.btnSpeech);
        btnSend = (ImageButton) findViewById(R.id.btnSend);
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        btnCancel = (ImageButton) findViewById(R.id.btnCancel);
        chat_main = findViewById(R.id.chat_main);
        viewEmoticons = findViewById(R.id.viewEmoticons);
        c = Calendar.getInstance();
        crTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        crTime.setTimeZone(TimeZone.getTimeZone("UTC"));
        btnSpeech = (ImageButton) findViewById(R.id.btnSpeech);
        txtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
               btnSend.setVisibility(txtMessage.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    btnSend.setVisibility(View.GONE);
                    String strCreation = crTime.format(c.getTime());
                    Message m = new Message();
                    m.setUsername(username);
                    m.setMessage(isShare ? getIntent().getStringExtra("message") : txtMessage.getText().toString());
                    m.setType(type);
                    m.setIsOwn(1);
                    m.setCreation(strCreation);
                    m.setChecked(1);
                    AppHandler.getInstance().getDBHandler().addMessage(m);
                    AppHandler.getInstance().getAppService().sendMessage(m);
                    txtMessage.setText("");
                    type = 1;
                    LoadMessages();
                    if (isShare) {
                        isShare = false;
                        txtMessage.setText("");
                        type = 1;
                        txtMessage.setEnabled(true);
                        btnEmoticon.setVisibility(View.VISIBLE);
                        btnSpeech.setVisibility(View.VISIBLE);
                        btnCancel.setVisibility(View.GONE);
                    }
                    if (!AppHandler.getInstance().getDBHandler().isUserExists(username)) {
                        AppHandler.getInstance().getDBHandler().addUser(u);
                        setTitle();
                    }
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);
        username = getIntent().getStringExtra("username");
        name = getIntent().getStringExtra("name");
        isShare = getIntent().getBooleanExtra("isShare", false);
        u = AppHandler.getInstance().getDBHandler().getUser(username);
        if (!AppHandler.getInstance().getDBHandler().isUserExists(username)) {
            u.setUsername(getIntent().getStringExtra("username"));
            u.setName(getIntent().getStringExtra("name"));
            u.setEmail(getIntent().getStringExtra("email"));
            u.setProfilePhoto(getIntent().getStringExtra("icon"));
            u.setCreation(getIntent().getStringExtra("creation"));
        }
        setTitle();
        LoadMessages();
        btnSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
                try {
                    startActivityForResult(intent, 3);
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(), "Your device doesn't support Speech to Text.", Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                if (intent.getAction().equals("notification")) {
                    if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_MESSAGE))) {
                        if (intent.getStringExtra("username").equals(username)) {
                            LoadMessages();
                        } else {
                            showSnackBar(intent.getStringExtra("messageData"), intent.getStringExtra("username"), intent.getStringExtra("name"));
                        }
                    }
                }
            }
        };
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShare) {
                    isShare = false;
                    txtMessage.setText("");
                    type = 1;
                    txtMessage.setEnabled(true);
                    btnEmoticon.setVisibility(View.VISIBLE);
                    btnSpeech.setVisibility(View.VISIBLE);
                    btnCancel.setVisibility(View.GONE);
                } else {
                    btnCancel.setVisibility(View.GONE);
                }
            }
        });
        if (isShare) {
            String[] message = getIntent().getStringExtra("message").split(":");
            txtMessage.setText("" + message[0] + "'s Post");
            type = 2;
            txtMessage.setEnabled(false);
            btnEmoticon.setVisibility(View.GONE);
            btnSpeech.setVisibility(View.GONE);
            btnCancel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMessageStatusChanged(JSONObject obj) {

    }

    private void LoadMessages() {
        messagesArrayList.clear();
        messagesArrayList.addAll(AppHandler.getInstance().getDBHandler().getAllMessages(username));
        chatAdapter.notifyDataSetChanged();
        scrollToLast();
    }

    void scrollToLast()
    {
        recyclerView.scrollToPosition(messagesArrayList.size() - 1);
    }

    private void showSnackBar(String message, final String username, final String name) {
        final Snackbar snackbar = Snackbar.make(chat_main, message, Snackbar.LENGTH_LONG);
        snackbar.setAction("Open", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatActivity.startActivity(ChatActivity.this, username, name);
                finish();
            }
        });
        snackbar.setActionTextColor(Color.WHITE);
        View view = snackbar.getView();
        CoordinatorLayout.LayoutParams params=(CoordinatorLayout.LayoutParams)view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        params.height = 200;
        snackbar.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("notification"));
        AppHandler.getInstance().getAppService().setOnMessagingStatusChanged(this);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    private void setTitle() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_chat) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete conversation")
                    .setMessage("Are you sure you want to delete whole conversation? This action cannot be undone.")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppHandler.getInstance().getDBHandler().deleteConversation(username);
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        } else if (item.getItemId() == R.id.action_profile) {
            UserProfile.startUserProfile(this, username, name);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isEmoticonsVisible) {
            ShowEmoticons();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onAttachmentItemClick(View v, int position, int id) {
        ActivityPost.startActivityPost(ChatActivity.this, messagesArrayList.get(position).getUsername(),
                messagesArrayList.get(position).getUsername(), String.valueOf(id));
    }

    private void ShowEmoticons() {
        if (!isEmoticonsVisible) {
            viewEmoticons.setVisibility(View.VISIBLE);
            txtMessage.requestFocus();
            isEmoticonsVisible = true;
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(txtMessage.getWindowToken(), 0);
        } else {
            viewEmoticons.setVisibility(View.GONE);
            txtMessage.requestFocus();
            isEmoticonsVisible = false;
        }
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(txtMessage, emojicon);
    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {
        EmojiconsFragment.backspace(txtMessage);
    }


}
