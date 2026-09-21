package com.example.cs360projecttwo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private EditText itemNameEditText;
    private EditText itemQuantityEditText;
    private EditText phoneNumberEditText;
    private LinearLayout inventoryContainer;
    private Button addItemButton;

    private DatabaseHelper databaseHelper;

    private int userId;
    private int editingItemId = -1;

    private final ActivityResultLauncher<String> smsPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {

                        if (granted) {
                            showMessage("SMS notifications enabled.");
                        } else {
                            showMessage(
                                    "SMS permission denied. Inventory features still work."
                            );
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        userId = getIntent().getIntExtra(
                "USER_ID",
                -1
        );

        if (userId == -1) {
            showMessage("Unable to load user account.");
            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        itemNameEditText =
                findViewById(R.id.itemNameEditText);

        itemQuantityEditText =
                findViewById(R.id.itemQuantityEditText);

        phoneNumberEditText =
                findViewById(R.id.phoneNumberEditText);

        inventoryContainer =
                findViewById(R.id.inventoryContainer);

        addItemButton =
                findViewById(R.id.addItemButton);

        Button smsPermissionButton =
                findViewById(R.id.smsPermissionButton);

        addItemButton.setOnClickListener(
                v -> saveInventoryItem()
        );

        smsPermissionButton.setOnClickListener(
                v -> requestSmsPermission()
        );

        displayInventoryItems();
    }

    private void saveInventoryItem() {

        String itemName =
                itemNameEditText
                        .getText()
                        .toString()
                        .trim();

        String quantityText =
                itemQuantityEditText
                        .getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(itemName)
                || TextUtils.isEmpty(quantityText)) {

            showMessage(
                    "Enter an item name and quantity."
            );
            return;
        }

        if (itemName.length() > 50) {
            showMessage(
                    "Item name must be 50 characters or less."
            );
            return;
        }

        int quantity;

        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            showMessage(
                    "Quantity must be a valid number."
            );
            return;
        }

        if (quantity < 0) {
            showMessage(
                    "Quantity cannot be negative."
            );
            return;
        }

        boolean success;

        if (editingItemId == -1) {

            success = databaseHelper.addInventoryItem(
                    userId,
                    itemName,
                    quantity
            );

            if (success) {
                showMessage("Item added.");
            } else {
                showMessage("Unable to add item.");
                return;
            }

        } else {

            success = databaseHelper.updateInventoryItem(
                    editingItemId,
                    itemName,
                    quantity
            );

            if (success) {
                showMessage("Item updated.");
                editingItemId = -1;
                addItemButton.setText("Add Item");
            } else {
                showMessage("Unable to update item.");
                return;
            }
        }

        if (quantity <= 5) {
            sendLowStockAlert(
                    itemName,
                    quantity
            );
        }

        itemNameEditText.setText("");
        itemQuantityEditText.setText("");

        itemNameEditText.requestFocus();

        displayInventoryItems();
    }

    private void displayInventoryItems() {

        inventoryContainer.removeAllViews();

        List<DatabaseHelper.InventoryItem> items =
                databaseHelper.getInventoryItems(userId);

        for (DatabaseHelper.InventoryItem item : items) {
            addInventoryRow(item);
        }
    }

    private void addInventoryRow(
            DatabaseHelper.InventoryItem item
    ) {

        LinearLayout itemRow =
                new LinearLayout(this);

        itemRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        itemRow.setPadding(
                8,
                12,
                8,
                12
        );

        itemRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView itemNameTextView =
                new TextView(this);

        itemNameTextView.setText(item.name);

        itemNameTextView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        2
                )
        );

        TextView quantityTextView =
                new TextView(this);

        quantityTextView.setText(
                String.valueOf(item.quantity)
        );

        quantityTextView.setGravity(
                Gravity.CENTER
        );

        quantityTextView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button editButton =
                new Button(this);

        editButton.setText("Edit");
        editButton.setTextSize(11);

        editButton.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        editButton.setOnClickListener(v -> {

            editingItemId = item.id;

            itemNameEditText.setText(
                    item.name
            );

            itemQuantityEditText.setText(
                    String.valueOf(item.quantity)
            );

            addItemButton.setText(
                    "Save Update"
            );

            itemNameEditText.requestFocus();
        });

        Button removeButton =
                new Button(this);

        removeButton.setText("Remove");
        removeButton.setTextSize(11);

        removeButton.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        removeButton.setOnClickListener(v -> {

            boolean deleted =
                    databaseHelper.deleteInventoryItem(
                            item.id
                    );

            if (deleted) {
                showMessage("Item removed.");
                displayInventoryItems();
            } else {
                showMessage(
                        "Unable to remove item."
                );
            }
        });

        itemRow.addView(itemNameTextView);
        itemRow.addView(quantityTextView);
        itemRow.addView(editButton);
        itemRow.addView(removeButton);

        inventoryContainer.addView(itemRow);
    }

    private void requestSmsPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED) {

            showMessage(
                    "SMS notifications are already enabled."
            );

        } else {

            smsPermissionLauncher.launch(
                    Manifest.permission.SEND_SMS
            );
        }
    }

    private void sendLowStockAlert(
            String itemName,
            int quantity
    ) {

        String phoneNumber =
                phoneNumberEditText
                        .getText()
                        .toString()
                        .trim();

        if (phoneNumber.isEmpty()) {
            return;
        }

        if (!Patterns.PHONE
                .matcher(phoneNumber)
                .matches()) {

            showMessage(
                    "Enter a valid phone number."
            );
            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED) {

            showMessage(
                    "Low stock detected. Enable SMS to receive an alert."
            );
            return;
        }

        try {

            String message =
                    "Inventory alert: "
                            + itemName
                            + " is low. Remaining quantity: "
                            + quantity
                            + ".";

            SmsManager.getDefault()
                    .sendTextMessage(
                            phoneNumber,
                            null,
                            message,
                            null,
                            null
                    );

            showMessage(
                    "Low-stock SMS alert sent."
            );

        } catch (Exception exception) {

            showMessage(
                    "Unable to send SMS on this device."
            );
        }
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}