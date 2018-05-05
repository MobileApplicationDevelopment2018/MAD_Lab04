package it.polito.mad.mad2018.chat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.polito.mad.mad2018.R;

public class ArchivedChatsFragment extends Fragment {
    public ArchivedChatsFragment() {
        // Required empty public constructor
    }

    public static ArchivedChatsFragment newInstance() {
        ArchivedChatsFragment fragment = new ArchivedChatsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_archived_chats, container, false);
    }
}
