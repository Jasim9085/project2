package com.chaquo.python.console;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    Button openNotebookBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            openNotebookBtn = findViewById(R.id.btn_open_notebook);
            openNotebookBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent(MainActivity.this, NotebookActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Failed to open notebook: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error in MainActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
