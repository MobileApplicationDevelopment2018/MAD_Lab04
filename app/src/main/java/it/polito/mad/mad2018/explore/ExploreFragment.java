package it.polito.mad.mad2018.explore;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;
import com.algolia.instantsearch.ui.views.SearchBox;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.List;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Book;
import it.polito.mad.mad2018.data.Constants;
import it.polito.mad.mad2018.library.BookInfoActivity;
import it.polito.mad.mad2018.widgets.MapWidget;

public class ExploreFragment extends Fragment {

    private final Searcher searcher;
    private FilterResultsFragment filterResultsFragment;

    private ViewPager pager;

    private AppBarLayout appBarLayout;
    private View algoliaLogoLayout;
    private GoogleApiClient mGoogleApiClient;

    private String searchQuery;
    private final static String SEARCH_QUERY_STRING = "searchQuery";

    public ExploreFragment() {
        searcher = Searcher.create(Constants.ALGOLIA_APP_ID, Constants.ALGOLIA_SEARCH_API_KEY,
                Constants.ALGOLIA_INDEX_NAME);
    }

    public static ExploreFragment newInstance() {
        return new ExploreFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            searchQuery = savedInstanceState.getString(SEARCH_QUERY_STRING);
        }

        setupGoogleAPI();

        filterResultsFragment = FilterResultsFragment.getInstance(searcher);
        List<Book.BookConditions> bookConditions = Book.BookConditions.values();
        filterResultsFragment
                .addSeekBar(Book.ALGOLIA_CONDITIONS_KEY,
                        FilterResultsFragment.CONDITIONS_NAME,
                        (double) bookConditions.get(0).value,
                        (double) bookConditions.get(bookConditions.size() - 1).value,
                        bookConditions.size() - 1)
                .addSeekBar(FilterResultsFragment.DISTANCE_NAME,
                        0.0, 1000000.0, 50);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        algoliaLogoLayout = inflater.inflate(R.layout.algolia_logo_layout, null);

        pager = view.findViewById(R.id.search_pager);
        SearchResultsPagerAdapter pagerAdapter = new SearchResultsPagerAdapter(getChildFragmentManager());
        pager.setAdapter(pagerAdapter);

        return view;
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        super.onAttachFragment(childFragment);

        if (childFragment instanceof SearchResultsTextFragment) {
            SearchResultsTextFragment instance = (SearchResultsTextFragment) childFragment;
            instance.setSearcher(searcher);
        }

        if (childFragment instanceof SupportMapFragment) {
            SupportMapFragment instance = (SupportMapFragment) childFragment;
            MapWidget mapWidget = new MapWidget(instance, bookId -> {
                Intent toBookInfo = new Intent(getActivity(), BookInfoActivity.class);
                toBookInfo.putExtra(Book.BOOK_ID_KEY, bookId);
                startActivity(toBookInfo);
            });
            searcher.registerResultListener(mapWidget);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        assert getActivity() != null;
        getActivity().setTitle(R.string.explore);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDetach() {
        if (appBarLayout != null) {
            appBarLayout.removeView(algoliaLogoLayout);
        }
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        searcher.destroy();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();

        assert getActivity() != null;

        inflater.inflate(R.menu.menu_explore, menu);
        InstantSearch helper = new InstantSearch(searcher);
        helper.registerSearchView(getActivity(), menu, R.id.menu_action_search);
        if(searchQuery != null) {
            helper.search(searchQuery);
        } else {
            helper.search();
        }

        MenuItem itemSearch = menu.findItem(R.id.menu_action_search);
        ImageView algoliaLogo = algoliaLogoLayout.findViewById(R.id.algolia_logo);
        algoliaLogo.setOnClickListener(v -> itemSearch.expandActionView());

        final SearchBox searchBox = (SearchBox) itemSearch.getActionView();
        itemSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if(searchQuery != null) {
                    searchBox.post(() -> searchBox.setQuery(searchQuery, false));
                }
                helper.setSearchOnEmptyString(true);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchQuery = searchBox.getQuery().toString();
                helper.setSearchOnEmptyString(false);
                return true;
            }
        });

        appBarLayout = getActivity().findViewById(R.id.app_bar_layout);
        appBarLayout.addView(algoliaLogoLayout);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_filter:
                filterResultsFragment.show(getChildFragmentManager(), FilterResultsFragment.TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SEARCH_QUERY_STRING, searchQuery);
    }

    public int onBackPressed() {
        if (pager.getCurrentItem() == 0) {
            return 0;
        } else {
            pager.setCurrentItem(0);
            return 1;
        }
    }

    private void setupGoogleAPI() {
        assert getContext() != null;
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addApi(LocationServices.API)
                .build();
    }

    private class SearchResultsPagerAdapter extends FragmentStatePagerAdapter {
        SearchResultsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return (position == 0
                    ? SearchResultsTextFragment.newInstance()
                    : SupportMapFragment.newInstance());
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
