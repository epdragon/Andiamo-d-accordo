package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private MaterialEditText username, email, password;
    private Button btn_register;
    private RadioGroup radioGroup;
    private MaterialRadioButton radioButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Register");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btn_register = findViewById(R.id.btn_register);
        radioGroup = findViewById(R.id.radioGroup);

        firebaseAuth = FirebaseAuth.getInstance();

        // When register is clicked check if fields are empty and if password is longer than 6 characters and call register method
        btn_register.setOnClickListener(v -> {
            String txt_username = username.getText().toString();
            String txt_email = email.getText().toString();
            String txt_password = password.getText().toString();
            String txt_gender = null;

            int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();
            if (selectedRadioButtonId != -1) {
                radioButton = findViewById(selectedRadioButtonId);
            }

            if (radioButton != null) {
                txt_gender = radioButton.getText().toString();
            }

            if (TextUtils.isEmpty(txt_username) || TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password)) {
                Toast.makeText(RegisterActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
            }
            else if (txt_password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            }
            else {
                register(txt_username, txt_email, txt_password, txt_gender);
            }
        });
    }

    private void register(final String username, final String email, final String password, final String gender) {
        // If register task is successful add a reference to Users
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        assert firebaseUser != null;
                        String userid = firebaseUser.getUid();

                        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put("id", userid);
                        hashMap.put("username", username);
                        hashMap.put("imageURL", "default");
                        hashMap.put("status", "offline");
                        hashMap.put("search", username.toLowerCase());
                        hashMap.put("address", "To be added");
                        hashMap.put("gender", gender);

                        reference.setValue(hashMap).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                    else {
                        Log.e("Auth", "Unable to create user", task.getException());
                        Toast.makeText(RegisterActivity.this, "You can't register with this email or password", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}