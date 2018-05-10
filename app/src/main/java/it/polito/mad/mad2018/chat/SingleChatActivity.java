package it.polito.mad.mad2018.chat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import it.polito.mad.mad2018.MainActivity;
import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Book;
import it.polito.mad.mad2018.data.Conversation;
import it.polito.mad.mad2018.data.UserProfile;
import it.polito.mad.mad2018.utils.TextWatcherUtilities;
import it.polito.mad.mad2018.utils.Utilities;

public class SingleChatActivity extends AppCompatActivity {

    private Conversation conversation;
    private UserProfile peer;
    private Book book;
    private String conversationId;

    private EditText message;
    private TextView noMessages;
    private ProgressBar loading;
    private ImageButton btnSend;
    private RecyclerView messages;

    private SingleChatAdapter adapter;
    private ValueEventListener conversationListener, profileListener, bookListener;
    private ValueEventListener localProfileListener;

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
            getSupportActionBar().setDisplayUseLogoEnabled(true);
        }

        if (savedInstanceState != null) {
            conversation = (Conversation) savedInstanceState.getSerializable(Conversation.CONVERSATION_KEY);
            peer = (UserProfile) savedInstanceState.getSerializable(UserProfile.PROFILE_INFO_KEY);
            book = (Book) savedInstanceState.getSerializable(Book.BOOK_KEY);
            conversationId = savedInstanceState.getString(Conversation.CONVERSATION_ID_KEY);
        } else {
            conversation = (Conversation) getIntent().getSerializableExtra(Conversation.CONVERSATION_KEY);
            peer = (UserProfile) getIntent().getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
            book = (Book) getIntent().getSerializableExtra(Book.BOOK_KEY);
            conversationId = getIntent().getStringExtra(Conversation.CONVERSATION_ID_KEY);
        }

        if(UserProfile.localInstance != null) {
            init();
        } else {
            setOnLocalProfileLoadedListener();
        }

        findViews();
        setTitle(peer != null ? peer.getUsername() : getString(R.string.app_name));

        btnSend.setEnabled(false);
        btnSend.setOnClickListener(v -> {
            boolean wasNewConversation = conversation.isNew();
            conversation.sendMessage(message.getText().toString());
            message.setText(null);

            if (wasNewConversation) {
                setupMessages();
            }
        });

        message.setEnabled(conversation != null);
        message.addTextChangedListener(new TextWatcherUtilities.GenericTextWatcher(
                editable -> btnSend.setEnabled(!Utilities.isNullOrWhitespace(editable.toString()))
        ));

        if (conversation != null && !conversation.isNew()) {
            setupMessages();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(Conversation.CONVERSATION_KEY, conversation);
        outState.putSerializable(UserProfile.PROFILE_INFO_KEY, peer);
        outState.putSerializable(Book.BOOK_KEY, book);
        outState.putSerializable(Conversation.CONVERSATION_ID_KEY, conversationId);
    }

    private void setupMessages() {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        messages.setLayoutManager(linearLayoutManager);

        adapter = new SingleChatAdapter(conversation.getMessages(), (count) -> {
            loading.setVisibility(View.GONE);
            noMessages.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            messages.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
            if (count > 0) {
                linearLayoutManager.scrollToPosition(count - 1);
            }
        });
        messages.setAdapter(adapter);
        adapter.startListening();
        message.setEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (adapter != null) {
            adapter.stopListening();
        }
        unsetOnConversationLoadedListener();
        unsetOnProfileLoadedListener();
        unsetOnLocalProfileLoadedListener();
        unsetOnBookLoadedListener();
    }

    private void findViews() {
        message = findViewById(R.id.message_send);
        btnSend = findViewById(R.id.button_send);
        noMessages = findViewById(R.id.chat_no_messages);
        messages = findViewById(R.id.chat_messages);
        loading = findViewById(R.id.chat_loading);
    }

    private void setOnConversationLoadedListener() {

        conversationListener = Conversation.setOnConversationLoadedListener(conversationId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!unsetOnConversationLoadedListener())
                    return;

                Conversation.Data data = dataSnapshot.getValue(Conversation.Data.class);
                if (data != null) {
                    conversation = new Conversation(conversationId, data);
                    if (peer == null) {
                        setOnProfileLoadedListener();
                    }
                    if (book == null) {
                        setOnBookLoadedListener();
                    }
                    setupMessages();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                unsetOnConversationLoadedListener();
            }
        });
    }

    private boolean unsetOnConversationLoadedListener() {
        if (conversationListener != null) {

            Conversation.unsetOnConversationLoadedListener(conversationId, conversationListener);
            this.conversationListener = null;
            return true;
        }
        return false;
    }

    private void setOnLocalProfileLoadedListener() {

        this.localProfileListener = UserProfile.setOnProfileLoadedListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!unsetOnLocalProfileLoadedListener()) {
                            return;
                        }

                        UserProfile.Data data = dataSnapshot.getValue(UserProfile.Data.class);
                        if (data != null) {
                            UserProfile.localInstance = new UserProfile(data);
                            init();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        unsetOnLocalProfileLoadedListener();
                    }
                });
    }

    private void init() {
        if (conversation == null) {
            if (conversationId == null) {
                conversationId = UserProfile.localInstance.findConversationByBookId(book.getBookId());
                if (conversationId == null) {
                    conversation = new Conversation(book);
                    conversationId = conversation.getConversationId();
                }
            } else {
                setOnConversationLoadedListener();
            }
        }
    }

    private boolean unsetOnLocalProfileLoadedListener() {
        if (this.localProfileListener != null) {
            UserProfile.unsetOnProfileLoadedListener(this.localProfileListener);
            this.localProfileListener = null;
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
                            peer = new UserProfile(conversation.getPeerUserId(), data);
                            setTitle(peer.getUsername());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        unsetOnProfileLoadedListener();
                    }
                });
    }

    private boolean unsetOnProfileLoadedListener() {
        if (this.profileListener != null) {
            UserProfile.unsetOnProfileLoadedListener(conversation.getPeerUserId(), this.profileListener);
            this.profileListener = null;
            return true;
        }
        return false;
    }

    private void setOnBookLoadedListener() {

        this.bookListener = Book.setOnBookLoadedListener(
                conversation.getBookId(),
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!unsetOnBookLoadedListener()) {
                            return;
                        }

                        Book.Data data = dataSnapshot.getValue(Book.Data.class);
                        if (data != null) {
                            book = new Book(conversation.getBookId(), data);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        unsetOnProfileLoadedListener();
                    }
                });
    }

    private boolean unsetOnBookLoadedListener() {
        if (this.bookListener != null) {
            Book.unsetOnBookLoadedListener(conversation.getBookId(), this.bookListener);
            this.bookListener = null;
            return true;
        }
        return false;
    }
}
