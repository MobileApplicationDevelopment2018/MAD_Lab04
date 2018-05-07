package it.polito.mad.mad2018.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.mad2018.MAD2018Application;
import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Book;
import it.polito.mad.mad2018.data.Conversation;
import it.polito.mad.mad2018.data.UserProfile;
import it.polito.mad.mad2018.utils.Utilities;

public class SingleChatActivity extends AppCompatActivity {
    private String peerId;
    private EditText message;
    private TextView noMessages;
    private ImageButton btnSend;
    private RecyclerView messages;
    private Conversation conversation;
    private SingleChatAdapter.OnItemCountChangedListener onItemCountChangedListener;
    private SingleChatAdapter adapter;
    private ValueEventListener profileListener, chatListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Set the toolbar
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Intent intent = getIntent();
        if (intent != null) {
            conversation = (Conversation) intent.getExtras().get(Conversation.CONVERSATION_KEY);

            if(conversation == null){
                conversation = new Conversation((Book) intent.getExtras().get(Book.BOOK_KEY));
            }
        }

        findViews();

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messages.setLayoutManager(linearLayoutManager);

        btnSend.setOnClickListener(v -> onClickButtonSend());


        onItemCountChangedListener = (count) -> {
            noMessages.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            messages.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        };

        FirebaseRecyclerOptions<Conversation.Message> options = conversation.getMessages();
        adapter = new SingleChatAdapter(options, null, onItemCountChangedListener);
        messages.setAdapter(adapter);

        btnSend.setEnabled(false);
        message.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (Utilities.isNullOrWhitespace(s.toString())) {
                    btnSend.setEnabled(false);
                } else{
                    btnSend.setEnabled(true);
                }
            }
        });
    }

    private void onClickButtonSend() {
        String msg = message.getText().toString();
        conversation.sendMessage(msg);
        message.setText("");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
        setOnProfileLoadedListener();
        setChatListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
        unsetOnProfileLoadedListener();
        unsetChatListener();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void findViews(){
        this.message = this.findViewById(R.id.message_send);
        this.btnSend = this.findViewById(R.id.button_send);
        this.noMessages = this.findViewById(R.id.chat_no_messages);
        this.messages = this.findViewById(R.id.chat_messages);
    }

    private void setChatListener(){
        Conversation.setOnConversationLoadedListener(conversation.getConversationId(), new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!unsetChatListener())
                    return;

                Conversation.Data data = dataSnapshot.getValue(Conversation.Data.class);

                if(data != null){
                    adapter.updateMessages(data);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                unsetChatListener();
            }
        });
    }

    private boolean unsetChatListener() {
        if(chatListener != null){
            Conversation.unsetOnConversationLoadedListener(conversation.getConversationId(), chatListener);
            this.chatListener = null;
            return true;
        }
        return false;
    }

    private void setOnProfileLoadedListener() {

        this.profileListener = UserProfile.setOnProfileLoadedListener(
                conversation.getPeerUserId(),
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!unsetOnProfileLoadedListener()) {
                            return;
                        }

                        UserProfile.Data data = dataSnapshot.getValue(UserProfile.Data.class);
                        if (data != null) {
                            UserProfile user = new UserProfile(conversation.getPeerUserId(), data, MAD2018Application.applicationContext.getResources());

                            updateTitle(user.getUsername());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        unsetOnProfileLoadedListener();
                    }
                });
    }

    private void updateTitle(String username) {
        setTitle(username);
    }

    private boolean unsetOnProfileLoadedListener() {
        if (this.profileListener != null) {
            UserProfile.unsetOnProfileLoadedListener(conversation.getPeerUserId(), this.profileListener);
            this.profileListener = null;
            return true;
        }
        return false;
    }

}
