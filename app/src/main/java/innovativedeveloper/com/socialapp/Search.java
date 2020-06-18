package innovativedeveloper.com.socialapp;

import android.annotation.TargetApi;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionInflater;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import innovativedeveloper.com.socialapp.adapter.PageFragmentAdapter;
import innovativedeveloper.com.socialapp.config.Config;
import innovativedeveloper.com.socialapp.fragment.SearchHashtag;
import innovativedeveloper.com.socialapp.fragment.SearchNearby;
import innovativedeveloper.com.socialapp.fragment.SearchPeople;

public class Search extends AppCompatActivity {

    Toolbar toolbar;
    TabLayout tabLayout;
    ViewPager viewPager;
    PageFragmentAdapter adapter;
    SearchPeople searchPeople;
    SearchHashtag searchHashtag;
    SearchNearby searchNearby;
    EditText txtSearch;
    AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        mAdView = (AdView) findViewById(R.id.adView);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(4);
        setupViewPager(viewPager);

        txtSearch = (EditText) findViewById(R.id.txtSearch);
        txtSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    updateSearch(txtSearch.getText().toString());
                    return true;
                }
                return false;
            }
        });
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        txtSearch.setEnabled(true);
                        txtSearch.setHint("Search peoples");
                        if (searchPeople != null) {
                            txtSearch.setText(searchPeople.getLastSearch());
                        }
                    } else if (tab.getPosition() == 1) {
                        txtSearch.setEnabled(true);
                        txtSearch.setHint("Search hashtag");
                        if (searchHashtag != null) {
                            txtSearch.setText(searchHashtag.getLastSearch());
                        }
                    } else if (tab.getPosition() == 2) {
                        txtSearch.setEnabled(false);
                        txtSearch.setHint("Nearby users from 25 miles");
                    }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        setupTabsTitle();

        if (Config.ENABLE_ACTIVITY_SEARCH_BANNER) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.VISIBLE);
        }
    }

    public void updateSearch(String query) {
        hideSoftKeyboard();
        if (viewPager.getCurrentItem() == 0) {
            searchPeople.updateSearch(query);
        } else {
            searchHashtag.updateSearch(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new PageFragmentAdapter(getSupportFragmentManager());
        if (searchPeople == null) { searchPeople = new SearchPeople(); }
        if (searchHashtag == null) { searchHashtag = new SearchHashtag(); }
        if (searchNearby == null) { searchNearby = new SearchNearby(); }
        adapter.addFragment(searchPeople, getString(R.string.fragment_people));
        adapter.addFragment(searchHashtag, getString(R.string.fragment_hashtag));
        adapter.addFragment(searchNearby, getString(R.string.fragment_nearby));
        viewPager.setAdapter(adapter);
    }

    private void setupTabsTitle() {
        tabLayout.getTabAt(0).setText(getString(R.string.fragment_people));
        tabLayout.getTabAt(1).setText(getString(R.string.fragment_hashtag));
        tabLayout.getTabAt(2).setText(getString(R.string.fragment_nearby));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {

        }
        return super.onOptionsItemSelected(item);
    }

    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}
