package it.polito.mad.mad2018.explore;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.algolia.instantsearch.helpers.Searcher;
import com.algolia.instantsearch.ui.views.Hits;

import org.json.JSONException;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Book;
import it.polito.mad.mad2018.library.BookInfoActivity;

public class SearchResultsTextFragment extends Fragment {

    private Searcher searcher;

    public SearchResultsTextFragment() { /* Required empty public constructor */ }

    public static SearchResultsTextFragment newInstance() {
        return new SearchResultsTextFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.search_results_text_layout, container, false);

        if (searcher != null) {
            Hits hits = view.findViewById(R.id.algolia_hits);
            setHitsOnClickListener(hits);
            hits.initWithSearcher(searcher);
            searcher.registerResultListener(hits);
        }

        return view;
    }

    public void setSearcher(@NonNull Searcher searcher) {
        this.searcher = searcher;
    }

    private void setHitsOnClickListener(Hits hits) {

        hits.setOnItemClickListener((recyclerView, position, v) -> {

            try {
                String bookId = hits.get(position).getString(Book.ALGOLIA_BOOK_ID_KEY);
                if (bookId != null) {
                    Intent toBookInfo = new Intent(getActivity(), BookInfoActivity.class);
                    toBookInfo.putExtra(Book.BOOK_ID_KEY, bookId);
                    startActivity(toBookInfo);
                    return;
                }
            } catch (JSONException e) { /* Do nothing */ }

            Toast.makeText(getContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
        });
    }
}
