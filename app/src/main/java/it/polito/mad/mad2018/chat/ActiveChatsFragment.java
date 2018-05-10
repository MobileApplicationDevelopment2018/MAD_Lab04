package it.polito.mad.mad2018.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.firebase.ui.database.FirebaseRecyclerOptions;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Book;
import it.polito.mad.mad2018.data.Conversation;
import it.polito.mad.mad2018.data.UserProfile;

public class ActiveChatsFragment extends Fragment {

    private ChatAdapter adapter;

    public ActiveChatsFragment() { /* Required empty public constructor */ }

    public static ActiveChatsFragment newInstance() {
        return new ActiveChatsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_chats, container, false);

        View noChatsView = view.findViewById(R.id.ac_no_active_chats);
        RecyclerView recyclerView = view.findViewById(R.id.ac_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ProgressBar loading = view.findViewById(R.id.active_chats_loading);

        FirebaseRecyclerOptions<Conversation> options = Conversation.getActiveConversations();
        adapter = new ChatAdapter(options, (v, conversation, peer, book) -> {
            Intent toChat = new Intent(getActivity(), SingleChatActivity.class);
            toChat.putExtra(Conversation.CONVERSATION_KEY, conversation);
            toChat.putExtra(UserProfile.PROFILE_INFO_KEY, peer);
            toChat.putExtra(Book.BOOK_KEY, book);
            startActivity(toChat);
        }, (count) -> {
            loading.setVisibility(View.GONE);
            noChatsView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        });

        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}



