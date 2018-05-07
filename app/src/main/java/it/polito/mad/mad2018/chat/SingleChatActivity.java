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
    private String conversationId;
    private Book book;
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
                conversationId = (String) intent.getExtras().get(Conversation.CONVERSATION_ID_KEY);
                book = (Book) intent.getExtras().get(Book.BOOK_KEY);
                if (conversationId == null) {
                    conversation = new Conversation(book);
                    conversationId = conversation.getConversationId();
                    peerId = book.getOwnerId();
                }
            } else {
                conversationId = conversation.getConversationId();
                peerId = conversation.getPeerUserId();
            }
        }

        findViews();

        btnSend.setOnClickListener(v -> onClickButtonSend());

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        messages.setLayoutManager(linearLayoutManager);

        onItemCountChangedListener = (count) -> {
            noMessages.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            messages.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
            if(count > 0) {
                linearLayoutManager.scrollToPosition(count - 1);
            }
        };

        FirebaseRecyclerOptions<Conversation.Message> options = Conversation.getMessages(conversationId);
        adapter = new SingleChatAdapter(options, null, onItemCountChangedListener);
        messages.setAdapter(adapter);

        btnSend.setEnabled(false);
        message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
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
        if (conversation == null)
            return;
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
        Conversation.setOnConversationLoadedListener(conversationId, new ValueEventListener() {
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
            Conversation.unsetOnConversationLoadedListener(conversationId, chatListener);
            this.chatListener = null;
            return true;
        }
        return false;
    }

    private void setOnProfileLoadedListener() {
        if (peerId == null)
            return;

        this.profileListener = UserProfile.setOnProfileLoadedListener(
                peerId,
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!unsetOnProfileLoadedListener()) {
                            return;
                        }

                        UserProfile.Data data = dataSnapshot.getValue(UserProfile.Data.class);
                        if (data != null) {
                            UserProfile user = new UserProfile(peerId, data, MAD2018Application.applicationContext.getResources());

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
            UserProfile.unsetOnProfileLoadedListener(peerId, this.profileListener);
            this.profileListener = null;
            return true;
        }
        return false;
    }

}
