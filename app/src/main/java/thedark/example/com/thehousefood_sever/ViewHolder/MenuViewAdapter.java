package thedark.example.com.thehousefood_sever.ViewHolder;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import thedark.example.com.thehousefood_sever.Food.FoodListActivity;
import thedark.example.com.thehousefood_sever.Model.Category;
import thedark.example.com.thehousefood_sever.R;

public class MenuViewAdapter extends RecyclerView.Adapter<MenuViewAdapter.ViewHolder> {
    ArrayList<Category> categoryArrayList = new ArrayList<>();
    Context context;

    public MenuViewAdapter(ArrayList<Category> categoryArrayList, Context context) {
        this.categoryArrayList = categoryArrayList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.menu_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final ArrayList<String> arrCategoryID = new ArrayList<>();
        holder.name.setText(categoryArrayList.get(position).getName());
        Picasso.get().load(categoryArrayList.get(position).getImage()).into(holder.img);

        holder.cardViewMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.table_category.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            String categoryID = messageSnapshot.getRef().getKey();
                            arrCategoryID.add(categoryID);
                        }
                        // Get CategoryID and send to new Activity:
                        Intent moveToFoodList = new Intent(context, FoodListActivity.class);
                        //Because CategoryID is Key, so we just get key of this item:
                        moveToFoodList.putExtra("CategoryID", arrCategoryID.get(position));
                        context.startActivity(moveToFoodList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

    }


    @Override
    public int getItemCount() {
        return categoryArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public ImageView img;
        public View cardViewMenu;

        //FIREBASE:
        FirebaseDatabase database;
        DatabaseReference table_category;

        public ViewHolder(View itemView) {
            super(itemView);
            database = FirebaseDatabase.getInstance();
            table_category = database.getReference("Category");

            name = (TextView) itemView.findViewById(R.id.menu_name);
            img = (ImageView) itemView.findViewById(R.id.menu_image);
            cardViewMenu = (CardView) itemView.findViewById(R.id.cardViewMenu);
        }
    }
}
