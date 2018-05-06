package it.polito.mad.mad2018.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Conversation;

public class ArchivedChatsFragment extends Fragment {
    private FirebaseRecyclerAdapter<Conversation, ChatAdapter.ChatHolder> adapter;
    private ChatAdapter.OnItemCountChangedListener onItemCountChangedListener;

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
        View view = inflater.inflate(R.layout.fragment_archived_chats, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.ac_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        View noChatsView = view.findViewById(R.id.ac_no_archived_chats);

        onItemCountChangedListener = (count) -> {
            noChatsView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        };

        FirebaseRecyclerOptions<Conversation> options = Conversation.getArchivedConversations();
        adapter = new ChatAdapter(options, (v, model) -> {
            Intent toChat = new Intent(getActivity(), SingleChatActivity.class);
            toChat.putExtra(Conversation.CONVERSATION_KEY, model);
            startActivity(toChat);
        }, onItemCountChangedListener);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
