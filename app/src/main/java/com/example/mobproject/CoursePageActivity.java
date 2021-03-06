package com.example.mobproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobproject.adapters.CommentAdapter;
import com.example.mobproject.constants.DatabaseCollections;
import com.example.mobproject.constants.Intents;
import com.example.mobproject.constants.UserInfo;
import com.example.mobproject.controllers.CourseController;
import com.example.mobproject.db.CourseDatabase;
import com.example.mobproject.db.Database;
import com.example.mobproject.db.EnrolledCoursesDatabase;
import com.example.mobproject.db.FavouriteCoursesDatabase;
import com.example.mobproject.interfaces.Callback;
import com.example.mobproject.models.Course;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CoursePageActivity extends AppCompatActivity {

    private RatingBar finalRating;
    private TextView ratingScore, finalRatingScore, courseEnroll, courseDescription, courseName, coursePrice,
    courseDifficulty, coursePeriod, courseMeetingDays, numberOfComments, commentTextView;
    private FloatingActionButton addToFav;
    private String courseId;
    private Course courseInfo;
    private UserInfo userInfo;
    private String userId;
    private boolean isFavourite = false;
    private boolean isEnrolled = false;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.course_page);

        RatingBar rateEdit = findViewById(R.id.rating_edit);
        finalRating = findViewById(R.id.final_rating);
        ratingScore = findViewById(R.id.rating_score);
        finalRatingScore = findViewById(R.id.final_rating_score);
        ImageButton editCourse = findViewById(R.id.edit_btn);
        addToFav = findViewById(R.id.add_to_fav);
        courseEnroll = findViewById(R.id.open_to_enroll_course_page);
        courseDescription = findViewById(R.id.course_descr);
        ImageButton backToMain = findViewById(R.id.back_btn);
        courseName = findViewById(R.id.course_name);
        coursePrice = findViewById(R.id.course_price);
        courseDifficulty = findViewById(R.id.difficulty_course_page);
        coursePeriod = findViewById(R.id.course_period);
        courseMeetingDays = findViewById(R.id.meeting_days);
        Button enrollMe = findViewById(R.id.enroll_me_btn);
        numberOfComments = findViewById(R.id.no_comments);
        Button postCommentButton = findViewById(R.id.post_btn);
        commentTextView = findViewById(R.id.comment_input);
        userInfo = new UserInfo(this);
        userId = userInfo.getUserId();

        backToMain.setOnClickListener(switchToMain);
        addToFav.setOnClickListener(onFavouriteHandler);
        editCourse.setOnClickListener(onEditHandler);
        rateEdit.setOnRatingBarChangeListener(onVoteHandler);
        postCommentButton.setOnClickListener(postCommentHandler);
        enrollMe.setOnClickListener(onEnrollHandler);


        // get course ID
        Intent intent = getIntent();
        courseId = intent.getStringExtra(Intents.COURSE_ID);

        // set values for all fields
        Database<Course> database = new CourseDatabase();
        database.getItem(courseId, initValuesCallback);

        //check course availability - open to enroll?
        //if user is already enrolled or currentDate > startDate
        //enrollMe.setEnabled(false);

        //make button invisible for Student User
        UserInfo userInfo = new UserInfo(this);
        String userTypeString = userInfo.getUserType();
        if (userTypeString.equals("0"))
            editCourse.setVisibility(View.GONE);

        initFavouriteButton();
        initEnrolledButton();
    }

    private void initFavouriteButton() {
        FavouriteCoursesDatabase favouriteDatabase = new FavouriteCoursesDatabase();
        DocumentReference courseReference = db.collection(DatabaseCollections
                .COURSES_COLLECTION).document(courseId);

        final Callback<DocumentReference> favouriteCallback = new Callback<DocumentReference>() {
            @Override
            public void OnFinish(ArrayList<DocumentReference> favouriteReferences) {
                for (DocumentReference docRef : favouriteReferences)
                    if (courseReference.equals(docRef)) {
                        isFavourite = true;
                        break;
                    }
                if (isFavourite) {
                    addToFav.setImageResource(R.drawable.ic_favourite_red);
//                    favouriteDatabase.insertItem(userId,courseId);
                }
                else {
                    addToFav.setImageResource(R.drawable.ic_favourite_purple);
//                    favouriteDatabase.removeItem(userId,courseId);
                }
            }
        };

        favouriteDatabase.getItems(userId, favouriteCallback);
    }

    private void initEnrolledButton(){
        EnrolledCoursesDatabase enrolledDatabase = new EnrolledCoursesDatabase();
        DocumentReference courseRef = db.collection(DatabaseCollections.ENROLLED_COLLECTION)
                .document(courseId);
        final Callback<DocumentReference> enrolledCallback = new Callback<DocumentReference>() {
            @Override
            public void OnFinish(ArrayList<DocumentReference> enrolledReferences) {
                for(DocumentReference docRef : enrolledReferences)
                    if(courseRef.equals(docRef)){
                        isEnrolled = true;
                        break;
                    }
//                if(isEnrolled)
//                    //TODO Front-end setting the Enroll Button disabled
            }
        };

        enrolledDatabase.getItems(userId,enrolledCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        courseId = intent.getStringExtra(Intents.COURSE_ID);

        // set values for all fields
        Database<Course> database = new CourseDatabase();
        database.getItem(courseId, initValuesCallback);

    }

    private final RatingBar.OnRatingBarChangeListener onVoteHandler = (ratingBar, givenScore, b) -> {
        int rating = (int) givenScore; // to be sent
        String ratingText = rating + this.getString(R.string.ratingOutOf);
        ratingScore.setText(ratingText);

        CourseController courseController = new CourseController();
        courseInfo = courseController.addRating(courseInfo, givenScore);
        updateTotalRating();
    };
    //TODO add rating to total rating

    private final View.OnClickListener onEditHandler = view -> {
        int isEdit = 1;
        Intent toEditCourse = new Intent(CoursePageActivity.this,
                CreateCourseActivity.class);
        toEditCourse.putExtra("EDIT_COURSE", isEdit);
        toEditCourse.putExtra("COURSE_ID", courseId);
        startActivity(toEditCourse);
    };

    private final View.OnClickListener onFavouriteHandler = view -> {
        FavouriteCoursesDatabase favouriteDatabase = new FavouriteCoursesDatabase();
        if (isFavourite) {
            addToFav.setImageResource(R.drawable.ic_favourite_red);
            favouriteDatabase.insertItem(userId,courseId);
        }
        else {
            addToFav.setImageResource(R.drawable.ic_favourite_purple);
            favouriteDatabase.removeItem(userId,courseId);
        }
        isFavourite = !isFavourite;
    };

    private final View.OnClickListener onEnrollHandler = view -> {
        EnrolledCoursesDatabase enrolledDatabase = new EnrolledCoursesDatabase();
        if(!isEnrolled){
            //TODO Front-end set Enroll Button disabled  + Toast/Dummy Payment
            enrolledDatabase.insertItem(userId,courseId);
        }
    };

    private final Callback<Course> initValuesCallback = new Callback<Course>() {
        @Override
        public void OnFinish(ArrayList<Course> arrayList) {
            courseInfo = arrayList.get(0);
            courseName.setText(courseInfo.getName());

            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
            String currency = format.format(courseInfo.getPrice());
            coursePrice.setText(currency);

            int difficulty = courseInfo.getDifficulty();
            courseDifficulty.setText(getResources().getStringArray(R.array.difficulties)[difficulty]);

            //set difficulty color
            courseDifficulty.setTextColor(getResources().getIntArray(R.array.difficultyColors)[difficulty]);

            if(courseInfo.isOpenEnroll())
            {
                courseEnroll.setText(R.string.open_to_enroll);
                courseEnroll.setTextColor(getResources().getColor(R.color.beginner_green));
            }
            else {
                courseEnroll.setText(R.string.cannot_enroll);
                courseEnroll.setTextColor(getResources().getColor(R.color.advanced_red));
            }

            courseDescription.setText(courseInfo.getDescription());

            String dateString = SimpleDateFormat.getDateInstance().format(courseInfo.getStartDate()) + " - " + SimpleDateFormat.getDateInstance().format(courseInfo.getEndDate());
            coursePeriod.setText(dateString);

            StringBuilder meetingDaysString = new StringBuilder();
            for(int day : courseInfo.getMeetDays()){
                meetingDaysString.append(" ").append(getResources().getStringArray(R.array.meeting_days)[day]);
            }
            courseMeetingDays.setText(meetingDaysString.toString());

            updateComments();

            updateTotalRating();
        }
    };

    private void updateComments() {
        if (courseInfo.getCommentsReferences() != null) {
            RecyclerView commentsList = findViewById(R.id.comments_list);
            commentsList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            CommentAdapter adapter = new CommentAdapter(getApplicationContext(), courseInfo.getCommentsReferences());
            commentsList.setAdapter(adapter);
            numberOfComments.setText(String.valueOf(courseInfo.getCommentsReferences().size()));
        }
    }

    private void updateTotalRating() {
        float finalRatingValue = (float)(Math.round(courseInfo.getRating() * 100.0) / 100.0);
        String finalRatingString = String.valueOf(finalRatingValue);
        String finalRatingText = finalRatingString + getString(R.string.ratingOutOf);

        finalRatingScore.setText(finalRatingText);
        finalRating.setRating(finalRatingValue);
    }

    private final View.OnClickListener switchToMain = view -> finish();

    private final View.OnClickListener postCommentHandler = view -> {
        String commentMessage = commentTextView.getText().toString();

        if (!commentMessage.trim().isEmpty()) {
            CourseController courseController = new CourseController();
            UserInfo userInfo = new UserInfo(this);
            courseInfo = courseController.addComment(courseInfo, userInfo.getUserId(), commentMessage);
            commentTextView.setText("");
            updateComments();
        } else {
            Toast.makeText(this, R.string.comment_error, Toast.LENGTH_SHORT);
        }
    };
}
