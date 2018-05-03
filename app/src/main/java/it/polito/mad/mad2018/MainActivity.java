package it.polito.mad.mad2018;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

import it.polito.mad.mad2018.data.UserProfile;
import it.polito.mad.mad2018.library.LibraryFragment;
import it.polito.mad.mad2018.utils.AppCompatActivityDialog;
import it.polito.mad.mad2018.utils.GlideApp;
import it.polito.mad.mad2018.utils.GlideRequest;
import it.polito.mad.mad2018.utils.Utilities;

public class MainActivity extends AppCompatActivityDialog<MainActivity.DialogID>
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int RC_SIGN_IN = 1;
    private static final int RC_EDIT_PROFILE = 5;
    private static final int RC_EDIT_PROFILE_WELCOME = 6;

    private FirebaseAuth firebaseAuth;
    private ValueEventListener profileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null && !Utilities.isNetworkConnected(this)) {
            openDialog(DialogID.DIALOG_NO_CONNECTION, true);
        }

        firebaseAuth = FirebaseAuth.getInstance();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // User not signed-in
        if (firebaseAuth.getCurrentUser() == null) {
            this.signIn();
        }

        if (savedInstanceState == null) {
            showDefaultFragment();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (firebaseAuth.getCurrentUser() != null) {
            if (UserProfile.localInstance == null) {
                setOnProfileLoadedListener();
            } else {
                updateNavigationView();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        this.unsetOnProfileLoadedListener();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.nav_explore:
                this.replaceFragment(ExploreFragment.newInstance());
                break;

            case R.id.nav_add_book:
                this.replaceFragment(LibraryFragment.newInstance());
                break;

            case R.id.nav_profile:
                this.replaceFragment(ShowProfileFragment.newInstance(UserProfile.localInstance, true));
                break;

            case R.id.nav_sign_out:
                signOut();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sp_edit_profile:
                this.showEditProfileActivity(RC_EDIT_PROFILE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_SIGN_IN:
                IdpResponse response = IdpResponse.fromResultIntent(data);

                // Successfully signed in
                if (resultCode == RESULT_OK && firebaseAuth.getCurrentUser() != null) {
                    setOnProfileLoadedListener();
                    return;
                }

                if (response != null) {
                    showToast(R.string.sign_in_unknown_error);
                }

                finish();
                return;

            case RC_EDIT_PROFILE:
                if (resultCode == RESULT_OK) {
                    UserProfile.localInstance = (UserProfile) data.getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
                    UserProfile.localInstance.postCommit();
                    updateNavigationView(); // Need to update the drawer information
                    this.replaceFragment(ShowProfileFragment.newInstance(UserProfile.localInstance, true), true);
                }
                break;

            case RC_EDIT_PROFILE_WELCOME:
                if (resultCode == RESULT_OK) {
                    UserProfile.localInstance = (UserProfile) data.getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
                    UserProfile.localInstance.postCommit();
                    updateNavigationView(); // Need to update the drawer information
                }
                break;

            default:
                break;
        }
    }

    private void updateNavigationView() {

        if (this.isDestroyed()) {
            return;
        }

        NavigationView drawer = findViewById(R.id.nav_view);
        View header = drawer.getHeaderView(0);

        ImageView profilePicture = header.findViewById(R.id.nh_profile_picture);
        TextView username = header.findViewById(R.id.nh_username);
        TextView email = header.findViewById(R.id.nh_email);

        UserProfile localProfile = UserProfile.localInstance;

        username.setText(localProfile.getUsername());
        email.setText(localProfile.getEmail());

        GlideRequest<Drawable> thumbnail = GlideApp
                .with(this)
                .load(localProfile.getProfilePictureThumbnail())
                .apply(RequestOptions.circleCropTransform());

        GlideApp.with(this)
                .load(localProfile.getProfilePictureReference())
                .signature(new ObjectKey(localProfile.getProfilePictureLastModified()))
                .thumbnail(thumbnail)
                .fallback(R.mipmap.ic_drawer_picture_round)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(profilePicture);

        profilePicture
                .setOnClickListener(v -> {
                    replaceFragment(ShowProfileFragment.newInstance(localProfile, true));
                    DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    drawer.getMenu().findItem(R.id.nav_profile).setChecked(true);
                });
    }

    private void signIn() {

        if (!Utilities.isNetworkConnected(this)) {
            openDialog(DialogID.DIALOG_NO_CONNECTION, true);
            return;
        }

        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build(),
                                new AuthUI.IdpConfig.GoogleBuilder().build(),
                                new AuthUI.IdpConfig.FacebookBuilder().build()))
                        .build(),
                RC_SIGN_IN);
    }

    private void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnSuccessListener(this, t -> onSignOut())
                .addOnFailureListener(this, t -> showToast(R.string.sign_out_failed));
    }

    private void onSignOut() {
        UserProfile.localInstance = null;
        showDefaultFragment();
        signIn();
    }

    private void showToast(@StringRes int message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setOnProfileLoadedListener() {
        this.openDialog(DialogID.DIALOG_LOADING, false);

        this.profileListener = UserProfile.setOnProfileLoadedListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!unsetOnProfileLoadedListener()) {
                    return;
                }
                closeDialog();

                UserProfile.Data data = dataSnapshot.getValue(UserProfile.Data.class);
                if (data == null) {
                    completeRegistration();
                } else {
                    UserProfile.localInstance = new UserProfile(data, getResources());
                    updateNavigationView();
                    showToast(getString(R.string.sign_in_welcome_back) + " " + UserProfile.localInstance.getUsername());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (!unsetOnProfileLoadedListener()) {
                    return;
                }
                openDialog(DialogID.DIALOG_ERROR_RETRIEVE_DIALOG, true);
            }
        });
    }

    private boolean unsetOnProfileLoadedListener() {
        if (this.profileListener != null) {
            UserProfile.unsetOnProfileLoadedListener(this.profileListener);
            this.profileListener = null;
            return true;
        }
        return false;
    }

    private void completeRegistration() {

        assert firebaseAuth.getCurrentUser() != null;
        UserProfile.localInstance = new UserProfile(firebaseAuth.getCurrentUser());
        UserProfile.localInstance.saveToFirebase(this.getResources());

        String message = getString(R.string.sign_in_welcome) + " " + UserProfile.localInstance.getUsername();
        Snackbar.make(findViewById(R.id.main_coordinator_layout), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.edit_profile, v -> showEditProfileActivity(RC_EDIT_PROFILE_WELCOME))
                .show();

        updateNavigationView();
    }

    private void replaceFragment(@NonNull Fragment instance) {
        replaceFragment(instance, false);
    }

    private void replaceFragment(@NonNull Fragment instance, boolean force) {

        final String fragmentTag = "main_fragment";
        Fragment oldInstance = getSupportFragmentManager()
                .findFragmentByTag(fragmentTag);

        if (force || oldInstance == null || !oldInstance.getClass().equals(instance.getClass())) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, instance, fragmentTag)
                    .commit();

            hideSoftKeyboard();
        }
    }

    private void showDefaultFragment() {
        NavigationView drawer = findViewById(R.id.nav_view);
        drawer.getMenu().findItem(R.id.nav_explore).setChecked(true);
        replaceFragment(ExploreFragment.newInstance());
    }

    private void hideSoftKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void showEditProfileActivity(int code) {
        Intent toEditProfile = new Intent(getApplicationContext(), EditProfile.class);
        toEditProfile.putExtra(UserProfile.PROFILE_INFO_KEY, UserProfile.localInstance);
        startActivityForResult(toEditProfile, code);
    }

    @Override
    protected void openDialog(@NonNull DialogID dialogId, boolean dialogPersist) {
        super.openDialog(dialogId, dialogPersist);

        Dialog dialog = null;
        switch (dialogId) {
            case DIALOG_LOADING:
                dialog = ProgressDialog.show(this, null,
                        getString(R.string.fui_progress_dialog_loading), true);
                break;
            case DIALOG_ERROR_RETRIEVE_DIALOG:
                dialog = Utilities.openErrorDialog(this,
                        R.string.failed_load_data,
                        (dlg, which) -> signOut());
                break;
            case DIALOG_NO_CONNECTION:
                dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.no_internet_connection)
                        .setMessage(R.string.internet_needed)
                        .setPositiveButton(R.string.exit, (dlg, which) -> finish())
                        .setCancelable(false)
                        .show();
                break;
        }

        if (dialog != null) {
            setDialogInstance(dialog);
        }
    }

    public enum DialogID {
        DIALOG_LOADING,
        DIALOG_ERROR_RETRIEVE_DIALOG,
        DIALOG_NO_CONNECTION,
    }
}
