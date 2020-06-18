package innovativedeveloper.com.socialapp.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

import innovativedeveloper.com.socialapp.FriendsList;
import innovativedeveloper.com.socialapp.ItemDivider;
import innovativedeveloper.com.socialapp.R;
import innovativedeveloper.com.socialapp.adapter.InboxAdapter;
import innovativedeveloper.com.socialapp.config.AppHandler;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.dataset.Inbox;

public class Messages extends AppCompatActivity implements InboxAdapter.OnItemClickListener {

    RecyclerView recyclerView;
    View emptyView;
    FloatingActionButton btnConversation;
    Toolbar toolbar;
    ArrayList<Inbox> inboxArrayList = new ArrayList<>();
    InboxAdapter iAdapter;
    BroadcastReceiver broadcastReceiver;
    private Toolbar searchToolbar;
    private boolean isSearch = false;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        setContentView(R.layout.activity_messages);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        searchToolbar = (Toolbar) findViewById(R.id.toolbar_search);
        setSupportActionBar(toolbar);
        mAdView = (AdView) findViewById(R.id.adView);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        iAdapter = new InboxAdapter(this, inboxArrayList);
        iAdapter.setOnItemClickListener(this);
        iAdapter.setAnimationsLocked(true);
        iAdapter.setHasStableIds(true);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        btnConversation = (FloatingActionButton) findViewById(R.id.btnConversation);
        btnConversation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectActivity = new Intent(Messages.this, FriendsList.class);
                startActivity(selectActivity);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new ItemDivider(this));
        recyclerView.setAdapter(iAdapter);
        LoadInbox();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                if (intent.getAction().equals("notification")) {
                    if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_NOTIFICATION))) {
                        Toast.makeText(context, intent.getStringExtra("messageData"), Toast.LENGTH_SHORT).show();
                    } else if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_REQUESTS))) {
                        Toast.makeText(context, intent.getStringExtra("messageData"), Toast.LENGTH_SHORT).show();
                    } else if (intent.getStringExtra("action").equals(String.valueOf(Config.PUSH_TYPE_MESSAGE))) {
                        LoadInbox();
                    }
                }
            }
        };

        if (Config.ENABLE_INBOX_BANNER) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("notification"));
        LoadInbox();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(isSearch ? R.menu.menu_search_toolbar : R.menu.menu_messages, menu);
        if (isSearch) {
            final SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
            search.setIconified(false);
            search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    filter(s);
                    return true;
                }
            });
            search.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    closeSearch();
                    return true;
                }
            });
        }
        return true;
    }

    public void filter(String toSearch)
    {
        if (!toSearch.trim().isEmpty()) {
            isSearch = true;
            ArrayList<Inbox> filteredItems = Filter(AppHandler.getInstance().getDBHandler().getInbox(), toSearch);
            inboxArrayList.clear();
            inboxArrayList.addAll(filteredItems);
            recyclerView.scrollToPosition(0);
            iAdapter.notifyDataSetChanged();
        }
    }

    private ArrayList<Inbox> Filter(ArrayList<Inbox> list, String toSearch) {
        toSearch = toSearch.toLowerCase();
        final ArrayList<Inbox> filteredItems = new ArrayList<>();
        for (Inbox i : list) {
            final String itemName = i.getUser().getName().toLowerCase();
            final String itemMessage = i.getMessage().toLowerCase();
            if (itemName.contains(toSearch) || itemMessage.contains(toSearch)) {
                filteredItems.add(i);
            }
        }
        return filteredItems;
    }

    private void closeSearch() {
        if (isSearch) {
            isSearch = false;
            prepareActionBar(toolbar);
            searchToolbar.setVisibility(View.GONE);
            supportInvalidateOptionsMenu();
            LoadInbox();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            isSearch = true;
            searchToolbar.setVisibility(View.VISIBLE);
            prepareActionBar(searchToolbar);
            supportInvalidateOptionsMenu();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            closeSearch();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareActionBar(Toolbar toolbar) {
        if (isSearch) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        } else {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
        }
    }

    @Override
    public void onItemClick(View v, int position) {
        ChatActivity.startActivity(this, inboxArrayList.get(position).getUser().getUsername(), inboxArrayList.get(position).getUser().getName());
    }

    private void LoadInbox() {
        inboxArrayList.clear();
        inboxArrayList.addAll(AppHandler.getInstance().getDBHandler().getInbox());
        iAdapter.notifyDataSetChanged();

        if(inboxArrayList.size() > 0)
        {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        else
        {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }
}
