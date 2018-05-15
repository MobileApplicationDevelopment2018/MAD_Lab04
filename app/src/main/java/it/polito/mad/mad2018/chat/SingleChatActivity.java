package it.polito.mad.mad2018.chat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.data.Book;
import it.polito.mad.mad2018.data.Conversation;
import it.polito.mad.mad2018.data.LocalUserProfile;
import it.polito.mad.mad2018.data.UserProfile;
import it.polito.mad.mad2018.utils.AppCompatActivityDialog;
import it.polito.mad.mad2018.utils.TextWatcherUtilities;
import it.polito.mad.mad2018.utils.Utilities;

public class SingleChatActivity extends AppCompatActivityDialog<SingleChatActivity.DialogID> {

    private Conversation conversation;
    private UserProfile peer;
    private Book book;
    private String conversationId;

    private EditText editTextMessage;
    private TextView viewNoMessages;
    private ImageButton buttonSend;
    private RecyclerView recyclerViewMessages;

    private SingleChatAdapter adapter;
    private ValueEventListener conversationListener, profileListener, bookListener;
    private ValueEventListener localProfileListener;

    private Handler handlerUpdateMessageTime;
    private Runnable runnableUpdateMessageTime;

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

        findViews();
        setTitle(peer != null ? peer.getUsername() : getString(R.string.app_name));

        buttonSend.setEnabled(false);
        buttonSend.setOnClickListener(v -> {
            boolean wasNewConversation = conversation.isNew();
            conversation.sendMessage(editTextMessage.getText().toString());
            editTextMessage.setText(null);

            if (wasNewConversation) {
                setupMessages();
            }
        });

        editTextMessage.addTextChangedListener(new TextWatcherUtilities.GenericTextWatcher(
                editable -> buttonSend.setEnabled(!Utilities.isNullOrWhitespace(editable.toString()))
        ));
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
        recyclerViewMessages.setLayoutManager(linearLayoutManager);

        adapter = new SingleChatAdapter(conversation.getMessages(), (count) -> {
            viewNoMessages.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            recyclerViewMessages.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
            if (count > 0) {
                linearLayoutManager.scrollToPosition(count - 1 - conversation.getUnreadMessagesCount());
            }
            conversation.setMessagesAllRead();
        });
        recyclerViewMessages.setAdapter(adapter);
        adapter.startListening();
        editTextMessage.setEnabled(true);

        conversation.startArchivedListener(() -> openDialog(DialogID.DIALOG_ARCHIVED, true));

        runnableUpdateMessageTime = () -> {
            adapter.notifyDataSetChanged();
            handlerUpdateMessageTime.postDelayed(runnableUpdateMessageTime, Conversation.UPDATE_TIME);
        };
        handlerUpdateMessageTime = new Handler();
        handlerUpdateMessageTime.postDelayed(runnableUpdateMessageTime, Conversation.UPDATE_TIME);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_single_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (LocalUserProfile.getInstance() != null) {
            init();
        } else {
            setOnLocalProfileLoadedListener();
        }

        if (adapter != null) {
            adapter.startListening();
        }

        updateViewsVisibility();
        editTextMessage.setEnabled(conversation != null);
        if (conversation != null && !conversation.isNew()) {
            setupMessages();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (adapter != null) {
            adapter.stopListening();
        }
        if (conversation != null) {
            conversation.stopArchivedListener();
        }
        if (handlerUpdateMessageTime != null && runnableUpdateMessageTime != null) {
            handlerUpdateMessageTime.removeCallbacks(runnableUpdateMessageTime);
        }

        unsetOnConversationLoadedListener();
        unsetOnProfileLoadedListener();
        unsetOnLocalProfileLoadedListener();
        unsetOnBookLoadedListener();
    }

    private void findViews() {
        editTextMessage = findViewById(R.id.message_send);
        buttonSend = findViewById(R.id.button_send);
        viewNoMessages = findViewById(R.id.chat_no_messages);
        recyclerViewMessages = findViewById(R.id.chat_messages);
    }

    private void updateViewsVisibility() {
        findViewById(R.id.chat_main_layout).setVisibility(conversation == null ? View.GONE : View.VISIBLE);
        findViewById(R.id.chat_loading).setVisibility(conversation == null ? View.VISIBLE : View.GONE);

        if (conversation != null) {
            findViewById(R.id.chat_line).setVisibility(conversation.isArchived() ? View.GONE : View.VISIBLE);
            editTextMessage.setVisibility(conversation.isArchived() ? View.GONE : View.VISIBLE);
            buttonSend.setVisibility(conversation.isArchived() ? View.GONE : View.VISIBLE);
        }
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

                    updateViewsVisibility();
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

        this.localProfileListener = LocalUserProfile.setOnProfileLoadedListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!unsetOnLocalProfileLoadedListener()) {
                            return;
                        }

                        UserProfile.Data data = dataSnapshot.getValue(UserProfile.Data.class);
                        if (data != null) {
                            LocalUserProfile.setInstance(new LocalUserProfile(data));
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
                conversationId = LocalUserProfile.getInstance().findConversationByBookId(book.getBookId());
                if (conversationId == null) {
                    conversation = new Conversation(book);
                    conversationId = conversation.getConversationId();
                } else {
                    setOnConversationLoadedListener();
                }
            }
        } else {
            if (peer == null) {
                setOnProfileLoadedListener();
            }
            if (book == null) {
                setOnBookLoadedListener();
            }
        }
    }

    private boolean unsetOnLocalProfileLoadedListener() {
        if (this.localProfileListener != null) {
            LocalUserProfile.unsetOnProfileLoadedListener(this.localProfileListener);
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

    @Override
    protected void openDialog(@NonNull DialogID dialogId, boolean dialogPersist) {
        super.openDialog(dialogId, dialogPersist);

        Dialog dialogInstance = null;
        switch (dialogId) {
            case DIALOG_ARCHIVED:
                dialogInstance = new AlertDialog.Builder(this)
                        .setTitle(R.string.conversation_archived)
                        .setMessage(R.string.conversation_archived_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
                break;
        }

        if (dialogInstance != null) {
            this.setDialogInstance(dialogInstance);
        }
    }

    public enum DialogID {
        DIALOG_ARCHIVED,
    }
}
