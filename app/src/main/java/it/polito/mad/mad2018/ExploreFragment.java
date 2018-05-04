package it.polito.mad.mad2018;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;

import it.polito.mad.mad2018.data.Book;
import it.polito.mad.mad2018.data.Constants;
import it.polito.mad.mad2018.widgets.MapWidget;

public class ExploreFragment extends Fragment {

    private final static String MAP_FRAGMENT_TAG = "map_fragment";

    private Searcher searcher;
    private FilterResultsFragment filterResultsFragment;
    private SearchResultsTextFragment searchResultsTextFragment;

    private MapWidget mapWidget;
    private SupportMapFragment mapFragment;

    private ViewPager mPager;

    private AppBarLayout appBarLayout;
    private View algoliaLogoLayout;
    private GoogleApiClient mGoogleApiClient;

    public ExploreFragment() { /* Required empty public constructor */ }

    public static ExploreFragment newInstance() {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupGoogleAPI();

        searcher = Searcher.create(Constants.ALGOLIA_APP_ID, Constants.ALGOLIA_SEARCH_API_KEY,
                Constants.ALGOLIA_INDEX_NAME);

        searchResultsTextFragment = SearchResultsTextFragment.newInstance(searcher);
        mapFragment = SupportMapFragment.newInstance();
        filterResultsFragment = FilterResultsFragment.getInstance(searcher);
        filterResultsFragment.addSeekBar("bookConditions.value", "conditions", 10.0, 40.0, 3)
                .addSeekBar("distance", 0.0, 1000000.0, 50);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        algoliaLogoLayout = inflater.inflate(R.layout.algolia_logo_layout, null);

        this.mapWidget = new MapWidget(mapFragment, bookId -> {
            Intent toBookInfo = new Intent(getActivity(), BookInfoActivity.class);
            toBookInfo.putExtra(Book.BOOK_ID_KEY, bookId);
            startActivity(toBookInfo);
        });

        mPager = view.findViewById(R.id.search_pager);
        PagerAdapter mPagerAdapter = new SearchResultsPagerAdapter(getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        return view;
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
        searcher.registerResultListener(mapWidget);

        InstantSearch helper = new InstantSearch(getActivity(), menu, R.id.menu_action_search, searcher);
        helper.search();

        MenuItem itemSearch = menu.findItem(R.id.menu_action_search);
        ImageView algoliaLogo = algoliaLogoLayout.findViewById(R.id.algolia_logo);
        algoliaLogo.setOnClickListener(v -> itemSearch.expandActionView());

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
    }

    public int onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            return 0;
        } else {
            mPager.setCurrentItem(0);
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
        SearchResultsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return (position == 0 ? searchResultsTextFragment : mapFragment);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
