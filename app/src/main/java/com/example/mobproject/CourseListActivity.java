package com.example.mobproject;


import static com.example.mobproject.navigation.MenuDrawer.setupDrawerContent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobproject.adapters.CourseAdapter;
import com.example.mobproject.constants.UserInfo;
import com.example.mobproject.db.CourseDatabase;
import com.example.mobproject.db.Database;
import com.example.mobproject.db.FavouriteCoursesDatabase;
import com.example.mobproject.interfaces.Callback;
import com.example.mobproject.models.Course;
import com.example.mobproject.navigation.MenuDrawer;
import com.example.mobproject.navigation.MenuDrawer;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.firestore.DocumentReference;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

public class CourseListActivity extends AppCompatActivity {
    private Switch enrollSwitch;
    private Spinner categoryFilter;
    private final ArrayList<Integer> difficultyChecked = new ArrayList<>();
    private CheckBox Beginner, Intermediate, Advanced;
    private RangeSlider priceRange;
    private SharedPreferences sharedPref;
    private String userId;

    @SuppressLint("ResourceAsColor")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.course_list);

        MenuDrawer.actionBarInit(this);
        UserInfo userInfo = new UserInfo(this);
        userId = userInfo.getUserId();
        Log.d("prefCheck", userInfo.getUserType());
//        sharedPref = getApplicationContext().getSharedPreferences("com.example.mobproject", Context.MODE_PRIVATE);
//        Log.d("prefCheck", sharedPref.getString("userType","NoIdFound"));
        MenuDrawer.actionBarInit(this);
        sortBarInit();

        /*View v = sortingCategory.getSelectedView();
        ((TextView)v).setTextColor(Integer.parseInt("4E0D3A"));*/


        FloatingActionButton filterBtn = findViewById(R.id.filter_fab);
        filterBtn.setOnClickListener(view -> showFilterDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        fillCourses();
    }

    private void fillCourses() {
        Context context = this;
        Callback<Course> recyclerViewCallback = new Callback<Course>() {
            @Override
            public void OnFinish(ArrayList<Course> coursesList) {
                RecyclerView courseListRecyclerView = findViewById(R.id.course_list);
                courseListRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                FavouriteCoursesDatabase favouriteCoursesDatabase = new FavouriteCoursesDatabase();
                favouriteCoursesDatabase.getItems(userId, new Callback<DocumentReference>() {
                    @Override
                    public void OnFinish(ArrayList<DocumentReference> arrayList) {
                        CourseAdapter adapter = new CourseAdapter(context, coursesList, arrayList, userId);
                        courseListRecyclerView.setAdapter(adapter);
                    }
                });
            }
        };

        Database<Course> database = new CourseDatabase();
        database.getItems(recyclerViewCallback);
    }

    private void sortBarInit() {
        Spinner sortingCategory = (Spinner) findViewById(R.id.sort_spn);
        sortingCategory.getBackground().setColorFilter(getResources().getColor(R.color.primary_dark_purple), PorterDuff.Mode.SRC_ATOP);

        ArrayAdapter<CharSequence> categoriesAdapter = ArrayAdapter.createFromResource(CourseListActivity.this,
                R.array.sorting_criteria,
                R.layout.spinner_dropdwon_layout);
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sortingCategory.setAdapter(categoriesAdapter);
    }

    //generate filter drawer
    private void showFilterDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.filter_drawer);

        //switch button
        enrollSwitch = bottomSheetDialog.findViewById(R.id.enroll_switch);

        //category Spinner
        categoryFilter = bottomSheetDialog.findViewById(R.id.category_spn);
        //after APPLY button


        //take difficulty - CheckBox
        Beginner = (CheckBox) bottomSheetDialog.findViewById(R.id.beginner_checkbox);
        Intermediate = (CheckBox) bottomSheetDialog.findViewById(R.id.intermediate_checkbox);
        Advanced = (CheckBox) bottomSheetDialog.findViewById(R.id.advanced_checkbox);

        //price Range
        priceRange = bottomSheetDialog.findViewById(R.id.price_range);

        //add "$" label
        assert priceRange != null;
        priceRange.setLabelFormatter(value -> {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            currencyFormat.setCurrency(Currency.getInstance("USD"));

            return currencyFormat.format(value);
        });

        //APPLY changes button
        Button applyFilters = bottomSheetDialog.findViewById(R.id.apply_btn);
        assert applyFilters != null;
        applyFilters.setOnClickListener(view -> filterCourses());

        //RESET button
        Button resetFilters = bottomSheetDialog.findViewById(R.id.reset_btn);
        assert resetFilters != null;
        resetFilters.setOnClickListener(view -> resetFilters());

        bottomSheetDialog.show();
    }

    private void resetFilters() {
        //reset priceRange
        priceRange.setValues(0.0F, 6000.0F);

        //reset Category spinner
        categoryFilter.setSelection(0);

        //reset Difficulty checkbox
        Beginner.setChecked(false);
        Intermediate.setChecked(false);
        Advanced.setChecked(false);

        //reset Enroll Switch
        enrollSwitch.setChecked(false);
    }

    private void filterCourses() {
        //PRICE RANGE
        List<Float> priceValues = priceRange.getValues();
        float minPrice = Collections.min(priceValues);
        float maxPrice = Collections.max(priceValues);
        //send price values to DB

        //CATEGORY SPINNER
        String chosenCategory = categoryFilter.getSelectedItem().toString();
        //send to DB - when "ALL" => no filter

        //DIFFICULTY CHECKBOX
        int diffCheckedID;
        if(Beginner.isChecked()) {
            diffCheckedID = 1;
            difficultyChecked.add(diffCheckedID);
        }
        else if(Intermediate.isChecked()){
            diffCheckedID = 2;
            difficultyChecked.add(diffCheckedID);
        }
        else if(Advanced.isChecked()){
            diffCheckedID = 3;
            difficultyChecked.add(diffCheckedID);
        }
        //send  difficultyChecked array to DB

        //ENROLL SWITCH
        enrollSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {

            if(isChecked){
                //send "open to enroll courses"
                Toast.makeText(getApplicationContext(),
                        "SWITCH ON", Toast.LENGTH_SHORT).show();
            }
            else{
                //send "all courses""
                Toast.makeText(getApplicationContext(),
                        "SWITCH OFF", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void onBackPressed(){
        MenuDrawer.onBackHandler();
    }
}
