package thedark.example.com.thehousefood_sever.Food;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import thedark.example.com.thehousefood_sever.Common.Common;
import thedark.example.com.thehousefood_sever.Interface.ItemClickListener;
import thedark.example.com.thehousefood_sever.Model.Food;
import thedark.example.com.thehousefood_sever.R;
import thedark.example.com.thehousefood_sever.ViewHolder.FoodViewHolder;

public class FoodListActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 2;
    RecyclerView recycler_food;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference foodList;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchAdapter;
    ArrayList<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;
    FloatingActionButton fab;

    MaterialEditText edtNameFood, edtDescriptionFood, edtPriceFood, edtDiscountFood;
    Button btnUpload, btnSelect;
    private String categoryId;
    private Uri saveUri;
    private String newUriImage = "";
    private Food newFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        fab = (FloatingActionButton) findViewById(R.id.fabFoodList);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddFoodDialog();
            }
        });

        //Init Firebase:
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference("images/");

        recycler_food = (RecyclerView) findViewById(R.id.recycler_foods);
        recycler_food.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_food.setLayoutManager(layoutManager);

        //Get Intent (CategoryID) From Home.java:
        if (getIntent() != null) {
            categoryId = getIntent().getStringExtra("CategoryID");
        }

        if (!categoryId.isEmpty()) {
            loadFoodList(categoryId);
        } else {
            Toast.makeText(this, "CategoryID is empty", Toast.LENGTH_SHORT).show();
        }

        //Search
        materialSearchBar = findViewById(R.id.searchBar);
        materialSearchBar.setHint("Enter Your Food");
        loadSuggest(); //write function loadSuggest from Firebase:
        materialSearchBar.setLastSuggestions(suggestList);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //When user type their text, we will change suggest list:
                List<String> suggest = new ArrayList<>();
                for (String search : suggestList) {
                    if (search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                        suggest.add(search);
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                //When SearchBar is close
                //Restore original adapter
                if (!enabled) {
                    recycler_food.setAdapter(adapter);
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                //When search finish
                //Show result of search adapter
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });
    }

    private void showAddFoodDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodListActivity.this);
        alertDialog.setTitle("Add new Category");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_food_layout, null);

        edtNameFood = add_menu_layout.findViewById(R.id.edtNameFood);
        edtDescriptionFood = add_menu_layout.findViewById(R.id.edtDescriptionFood);
        edtDiscountFood = add_menu_layout.findViewById(R.id.edtDiscountFood);
        edtPriceFood = add_menu_layout.findViewById(R.id.edtPriceFood);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        //Event for button:
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);
        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }

    private void uploadImage() {
        if (edtNameFood.getText().toString().equals("")
                || edtPriceFood.getText().toString().equals("")
                || edtDiscountFood.getText().toString().equals("")
                || edtDescriptionFood.getText().toString().equals("")
                ) {
            Toast.makeText(FoodListActivity.this, "Please enter full information", Toast.LENGTH_SHORT).show();
        } else {
            if (saveUri != null) {
                final ProgressDialog mDialog = new ProgressDialog(this);
                mDialog.setMessage("Uploading...");
                mDialog.show();

                String imageName = UUID.randomUUID().toString();
                final StorageReference imageFolder = storageReference.child("images/" + imageName);
                imageFolder.putFile(saveUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        //Set value new Category if image upload and we can download link:
                                        newUriImage = uri.toString();
                                        mDialog.dismiss();
                                        submitData();
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(FoodListActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                mDialog.dismiss();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                mDialog.setMessage("Upload " + progress + "%");
                                Toast.makeText(FoodListActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Please Select Image", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void submitData() {
        if (edtNameFood.getText().toString().equals("")
                || edtPriceFood.getText().toString().equals("")
                || edtDiscountFood.getText().toString().equals("")
                || edtDescriptionFood.getText().toString().equals("")) {
            Toast.makeText(FoodListActivity.this, "Please enter full information", Toast.LENGTH_SHORT).show();
        } else {
            newFood = new Food(
                    edtDescriptionFood.getText().toString(),
                    edtDiscountFood.getText().toString(),
                    newUriImage,
                    categoryId,
                    edtNameFood.getText().toString(),
                    edtPriceFood.getText().toString());
            foodList.push().setValue(newFood);

            final Snackbar snackbar = Snackbar
                    .make(getCurrentFocus(), "New category " + newFood.getName() + " was added", Snackbar.LENGTH_LONG);
            View sbView = snackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(Color.WHITE);
            snackbar.setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    snackbar.dismiss();
                }
            })
                    .setActionTextColor(Color.GREEN);
            sbView.setBackgroundColor(getApplication().getResources().getColor(R.color.backroundSnackbar));
            snackbar.show();
            newUriImage = "";
            saveUri = null;
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            saveUri = data.getData();
            btnSelect.setText("IMAGE SELECTED");
        }
    }

    private void startSearch(CharSequence text) {
        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(
                Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                foodList.orderByChild("name").equalTo(text.toString()) //Compare name;
        ) {
            @Override
            protected void populateViewHolder(FoodViewHolder viewHolder, final Food model, final int position) {
                viewHolder.txtFoodName.setText(model.getName());
                Picasso.get()
                        .load(model.getImage())
                        .into(viewHolder.imageView);

                Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int positon, boolean isLongClick) {
//                        //Start Activity FoodDetails
//                        Intent moveToFoodDetails = new Intent(FoodListActivity.this, FoodDetailsActivity.class);
//                        moveToFoodDetails.putExtra("FoodId", searchAdapter.getRef(position).getKey());
//                        // Send FoodId to FoodDetailsActivity
//                        moveToFoodDetails.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                        startActivity(moveToFoodDetails);
                        Toast.makeText(FoodListActivity.this, model.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        searchAdapter.notifyDataSetChanged();
        recycler_food.setAdapter(searchAdapter);
    }

    private void loadSuggest() {
        foodList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            Food item = postSnapshot.getValue(Food.class);
                            suggestList.add(item.getName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadFoodList(String categoryId) {
        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(
                Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                //like: SELECT * FROM Foods WHERE MenuID = categoryID;
                foodList.orderByChild("menuId").equalTo(categoryId)) {
            @Override
            protected void populateViewHolder(FoodViewHolder viewHolder, final Food model, final int position) {
                viewHolder.txtFoodName.setText(model.getName());
                Picasso.get()
                        .load(model.getImage())
                        .into(viewHolder.imageView);

                Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int positon, boolean isLongClick) {
//                        //Start Activity FoodDetails
//                        Intent moveToFoodDetails = new Intent(getApplicationContext(), FoodDetailsActivity.class);
//                        moveToFoodDetails.putExtra("FoodId", adapter.getRef(position).getKey());
//                        // Send FoodId to FoodDetailsActivity
//                        startActivity(moveToFoodDetails);
                        Toast.makeText(FoodListActivity.this, model.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        adapter.notifyDataSetChanged();
        recycler_food.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishActivity(5);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.UPDATE)) {
            showUpdateDialog(adapter.getRef(item.getOrder()).getKey(), adapter.getItem(item.getOrder()));
        } else {
            deleteCategory(adapter.getRef(item.getOrder()).getKey());
        }
        return super.onContextItemSelected(item);
    }

    private void deleteCategory(String key) {
        foodList.child(key).removeValue();
    }

    private void showUpdateDialog(final String key, final Food item) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodListActivity.this);
        alertDialog.setTitle("Update Food");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_food_layout, null);

        edtNameFood = add_menu_layout.findViewById(R.id.edtNameFood);
        edtDescriptionFood = add_menu_layout.findViewById(R.id.edtDescriptionFood);
        edtDiscountFood = add_menu_layout.findViewById(R.id.edtDiscountFood);
        edtPriceFood = add_menu_layout.findViewById(R.id.edtPriceFood);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        //set default data:
        edtNameFood.setText(item.getName());
        edtDescriptionFood.setText(item.getDescription());
        edtDiscountFood.setText(item.getDiscount());
        edtPriceFood.setText(item.getPrice());

        //Event for button:
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeImage(item);
            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);
        alertDialog.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                item.setName(edtNameFood.getText().toString());
                item.setDescription(edtDescriptionFood.getText().toString());
                item.setDiscount(edtDiscountFood.getText().toString());
                item.setPrice(edtPriceFood.getText().toString());
                foodList.child(key).setValue(item);

                Toast.makeText(FoodListActivity.this, "Update Successfully", Toast.LENGTH_SHORT).show();
                saveUri = null;
                newUriImage = "";
            }
        });
        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }

    private void changeImage(final Food item) {
        if (edtNameFood.getText().toString().equals("")
                || edtPriceFood.getText().toString().equals("")
                || edtDiscountFood.getText().toString().equals("")
                || edtDescriptionFood.getText().toString().equals("")) {
            Toast.makeText(FoodListActivity.this, "Please enter full information", Toast.LENGTH_SHORT).show();
        } else {
            if (saveUri != null) {
                final ProgressDialog mDialog = new ProgressDialog(this);
                mDialog.setMessage("Uploading...");
                mDialog.show();

                String imageName = UUID.randomUUID().toString();
                final StorageReference imageFolder = storageReference.child("images/" + imageName);
                imageFolder.putFile(saveUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        item.setImage(uri.toString());
                                        mDialog.dismiss();
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(FoodListActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                mDialog.dismiss();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                mDialog.setMessage("Upload " + progress + "%");
                                Toast.makeText(FoodListActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Please select image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
