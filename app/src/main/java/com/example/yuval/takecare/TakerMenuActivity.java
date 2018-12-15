package com.example.yuval.takecare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TakerMenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static String TAG = "TAKER";

    private FeedRecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ImageView userProfilePicture;
    private MenuItem currentDrawerChecked;

    private ConstraintLayout filterPopupMenu;
    private AppCompatImageButton chosenPickupMethod;

    private boolean emptyFeed = true;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taker_menu);

        //Set the toolbar as the AppBar for this activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set up the onClick listener for the giver form button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TakerMenuActivity.this, GiverMenuActivity.class);
                startActivity(intent);
            }
        });

        Log.d("TAG", "Temp");

        //Set up the navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        currentDrawerChecked = (MenuItem) navigationView.getMenu().findItem(R.id.nav_show_all);
        currentDrawerChecked.setEnabled(true);
        currentDrawerChecked.setChecked(true);

        filterPopupMenu = (ConstraintLayout) findViewById(R.id.filter_menu_popup);
        chosenPickupMethod = (AppCompatImageButton) findViewById(R.id.pickup_any_button);
        Log.d("TAG", "TEMP1");
        View header = navigationView.getHeaderView(0);
        userProfilePicture = (ImageView) header.findViewById(R.id.nav_user_picture);
        Log.d("TAG", "Temp2");

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance().getReference();

        final FirebaseUser user = auth.getCurrentUser();
        Log.d("TAG", "Checking for user");
        if (user != null) {
            DocumentReference docRef = db.collection("users").document(user.getUid());
            Log.d("TAG", "User is logged in");
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d("TAG", "Found user");
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                            if (document.getString("profilePicture") != null) {
                                Glide.with(TakerMenuActivity.this)
                                        .load(document.getString("profilePicture"))
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(userProfilePicture);
                            }
                        } else {
                            Log.d("TAG", "No such document");
                        }
                    } else {
                        Log.d("TAG", "get failed with ", task.getException());
                    }
                }
            });
        }

        setUpRecyclerView();
    }


    @Override
    protected void onResume() {
        super.onResume();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
    }

    private void setUpRecyclerView() {
        recyclerView = (FeedRecyclerView) findViewById(R.id.taker_feed_list);
        View emptyFeedView = findViewById(R.id.empty_feed_view);
        //Optimizing recycler view's performance:
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        List<FeedCardInformation> cards = new ArrayList<>();
        adapter = new TakerRVAdapter(cards); //List is still empty
        recyclerView.setAdapter(adapter);
        //Set the view to be displayed when the FeedRecyclerView is empty!
        recyclerView.setEmptyView(emptyFeedView);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.taker_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                toggleFilterMenu();
                break;
            case R.id.action_change_display:
                //TODO; add this in the future
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleFilterMenu() {
        if (emptyFeed) {
            Toast.makeText(getApplicationContext(), "Filter menu is not available when the feed is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (filterPopupMenu.getVisibility() == View.GONE) {
            filterPopupMenu.setVisibility(View.VISIBLE);
        } else {
            filterPopupMenu.setVisibility(View.GONE);
        }
    }

    public void openUserSettings(View view) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        startActivity(intent);
    }


    public void onChoosePickupMethod(View view) {
        if (chosenPickupMethod.equals(view)) {
            return;
        }
        ViewCompat.setBackgroundTintList(chosenPickupMethod, getResources().getColorStateList(R.color.divider));
        ImageViewCompat.setImageTintList(chosenPickupMethod, getResources().getColorStateList(R.color.secondary_text));
        ViewCompat.setBackgroundTintList(view, getResources().getColorStateList(R.color.colorPrimary));
        ImageViewCompat.setImageTintList((ImageView) view, getResources().getColorStateList(R.color.icons));
        chosenPickupMethod = (AppCompatImageButton) view;

        //TODO: handle
        switch (view.getId()) {
            case R.id.pickup_any_button:
                break;
            case R.id.pickup_in_person_button:
                break;
            case R.id.pickup_giveaway_button:
                break;
            case R.id.pickup_race_button:
                break;
        }
        filterPopupMenu.setVisibility(View.GONE);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "onNavigationItemSelected: selected item id: " + id);
        Intent intent;
        if (id == R.id.nav_requested_items) {
            intent = new Intent(this, RequestedItemsActivity.class);
            startActivity(intent);
            item.setChecked(false);
            currentDrawerChecked.setChecked(true);
            return false;
        } else if (id == R.id.nav_my_items) {
            intent = new Intent(this, RequestedItemsActivity.class);
            startActivity(intent);
            item.setChecked(false);
            currentDrawerChecked.setChecked(true);
            return false;
        } else if (id == R.id.nav_manage_favorites) {
            intent = new Intent(this, RequestedItemsActivity.class);
            startActivity(intent);
            item.setChecked(false);
            currentDrawerChecked.setChecked(true);
            return false;
        } else if (id == R.id.nav_chat) {
            intent = new Intent(this, RequestedItemsActivity.class);
            startActivity(intent);
            item.setChecked(false);
            currentDrawerChecked.setChecked(true);
            return false;
        } else if (id == R.id.nav_user_settings) {
            intent = new Intent(this, RequestedItemsActivity.class);
            startActivity(intent);
            item.setChecked(false);
            currentDrawerChecked.setChecked(true);
            return false;
        } else {
            currentDrawerChecked.setChecked(false);
            currentDrawerChecked = item;
            currentDrawerChecked.setChecked(true);
            //TODO: implement handler for each filter option
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        }

        return true;
    }

    public void onItemCategoryPress(View view) {
        String str = "";
        if (view.getId() == R.id.item_category) {
            switch ((int) view.getTag()) {
                case R.drawable.ic_pizza_slice_purple:
                    str = "This item's category is food";
                    break;
                case R.drawable.ic_book_purple:
                    str = "This item's category is study material";
                    break;
                case R.drawable.ic_lamp_purple:
                    str = "This item's category is household objects";
                    break;
                case R.drawable.ic_lost_and_found_purple:
                    str = "This item's category is lost&founds";
                    break;
                case R.drawable.ic_car_purple:
                    str = "This item's category is hitchhiking";
                    break;
                default:
                    str = "This item is in a category of its own";
                    break;
            }
        } else {
            switch ((int) view.getTag()) {
                case R.drawable.ic_in_person_purple:
                    str = "This item is available for pick-up in person";
                    break;
                case R.drawable.ic_giveaway_purple:
                    str = "This item is available in a public giveaway";
                    break;
                case R.drawable.ic_race_purple:
                    str = "Race to get this item before anyone else!";
                    break;
                default:
                    break;
            }
        }
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }

    public void onReportPress(View view) {
        PopupMenu menu = new PopupMenu(this, view);
        menu.getMenuInflater().inflate(R.menu.report_menu, menu.getMenu());
        menu.show();
    }

    public void tempFillItems(View view) {
        List<FeedCardInformation> list = new ArrayList<>();
        String muffinPhotoURL = "https://firebasestorage.googleapis.com/v0/b/takecare-81dab.appspot.com/o/photo_muffin.png?alt=media&token=d52abb7a-1763-4c6b-ac74-89ffab4a8714";
        String nightstandURL = "https://firebasestorage.googleapis.com/v0/b/takecare-81dab.appspot.com/o/photo_nightstand.png?alt=media&token=a3afa089-acaf-4a05-94eb-8cc581121935";
        String pizzaPhotoURL = "https://firebasestorage.googleapis.com/v0/b/takecare-81dab.appspot.com/o/photo_pizza.png?alt=media&token=6c81b8d3-c4ad-4769-9c82-2f03cd4c55d1";
        String booksPhotoURL = "https://firebasestorage.googleapis.com/v0/b/takecare-81dab.appspot.com/o/photo_books.png?alt=media&token=1cb30a65-80cc-4957-9254-a7f52234b2ca";
        String hitchhikerPhotoURL = "https://firebasestorage.googleapis.com/v0/b/takecare-81dab.appspot.com/o/photo_hittchhiker.png?alt=media&token=f4d2f6ea-9590-4297-a1f5-04c8a2673108";
        String umbrellaPhotoURL = "https://firebasestorage.googleapis.com/v0/b/takecare-81dab.appspot.com/o/photo_umbrella.png?alt=media&token=6638b6a1-b1a4-4e26-926b-3af1532be35c";


        for (int i = 0; i < 1e3; i++) {
            list.add(new FeedCardInformation("Yummy Muffins For All!", muffinPhotoURL, R.drawable.photo_mcgiverface, "Giver McGiverFace", R.drawable.ic_pizza_slice_purple, R.drawable.ic_giveaway_purple));
            list.add(new FeedCardInformation("Driving to Tel-Aviv at Approx 7pm", hitchhikerPhotoURL, R.drawable.ic_user_purple, "Israel M. Shalom", R.drawable.ic_car_purple, R.drawable.ic_race_purple));
            list.add(new FeedCardInformation("I Found An Umbrella Near Ullman", umbrellaPhotoURL, R.drawable.photo_mcgiverface, "Giver McGiverFace", R.drawable.ic_lost_and_found_purple, R.drawable.ic_in_person_purple));
            list.add(new FeedCardInformation("FREE PIZZAS IN TAUB'S BALCONY!! GET OVER HERE QUICKLY!!", pizzaPhotoURL, R.drawable.ic_user_purple, "Yuval", R.drawable.ic_pizza_slice_purple, R.drawable.ic_giveaway_purple));
            list.add(new FeedCardInformation("This Cool Nightstand!", nightstandURL, R.drawable.ic_user_purple, "Tzvika", R.drawable.ic_lamp_purple, R.drawable.ic_race_purple));
            list.add(new FeedCardInformation("I have lots of MATAM books", booksPhotoURL, R.drawable.photo_mcgiverface, "Giver McGiverFace", R.drawable.ic_book_purple, R.drawable.ic_race_purple));
        }

        List<FeedCardInformation> cards = list;
        adapter = new TakerRVAdapter(cards);
        recyclerView.setAdapter(adapter);
        //TODO: add logic for this flag
        emptyFeed = false;
    }

    public void onTakerCardSelected(View view) {
        //TODO: add extra information to intent
        Intent intent = new Intent(this, ItemInfoActivity.class);
        startActivity(intent);
    }
}
