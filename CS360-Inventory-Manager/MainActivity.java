package com.example.cs360projecttwo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        Button loginButton = findViewById(R.id.loginButton);
        Button createAccountButton = findViewById(R.id.createAccountButton);

        databaseHelper = new DatabaseHelper(this);

        loginButton.setOnClickListener(v -> loginUser());
        createAccountButton.setOnClickListener(v -> createAccount());
    }

    private void loginUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Enter a username and password.");
            return;
        }

        if (databaseHelper.isValidUser(username, password)) {
            openInventory(username);
        } else {
            showMessage("Username or password is incorrect.");
        }
    }

    private void createAccount() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Enter a username and password.");
            return;
        }

        if (!isStrongPassword(password)) {
            showMessage("Password must be 8+ characters with upper, lower, number, and special character.");
            return;
        }

        if (databaseHelper.createUser(username, password)) {
            showMessage("Account created. Please log in.");
            passwordEditText.setText("");
            passwordEditText.requestFocus();
        } else {
            showMessage("That username is already in use.");
        }
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*[0-9].*")
                && password.matches(".*[^A-Za-z0-9].*");
    }

    private void openInventory(String username) {
        int userId = databaseHelper.getUserId(username);

        if (userId == -1) {
            showMessage("Unable to load account.");
            return;
        }

        Intent intent = new Intent(this, InventoryActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}