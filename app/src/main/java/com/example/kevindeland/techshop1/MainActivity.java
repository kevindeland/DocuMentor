package com.example.kevindeland.techshop1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    private String TAG = "main";


    private final static String[] wk1 = {"Aidan", "Andriy", "Benny", "Chazz", "Jakob", "Owen", "Patrick", "Tili", "Zack"};
    private final static String[] students = {"Ben", "David", "Ignacio", "Kyra", "Khantrell",  "Michael", "DEFAULT"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "starting up");

        for (int i=0; i<students.length; i++) {
            Button myButton = new Button(this);
            final String studentName = students[i];
            myButton.setText(studentName);
            myButton.setHeight(120);
            myButton.setTextSize(20);


            //myButton.setId("button" + studentName);

            myButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {

                    Intent activityChangeIntent = new Intent(MainActivity.this, Camera2Activity.class);

                    Bundle b = new Bundle();
                    b.putString("studentName", studentName);
                    activityChangeIntent.putExtras(b);
                    startActivity(activityChangeIntent);
                }
            });

            LinearLayout ll = (LinearLayout) findViewById(R.id.buttonLayout);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);


            ll.addView(myButton, lp);

        }
    }

}
