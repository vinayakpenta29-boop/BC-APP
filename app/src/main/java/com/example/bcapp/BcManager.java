package com.example.bcapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import java.io.OutputStream;
import androidx.core.content.ContextCompat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import androidx.cardview.widget.CardView;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;  // ← ADD THIS LINE
import android.widget.ScrollView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bcapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import android.util.Log;

public class BcManager {

// 🔹 Backup for Undo Delete Member
private static class DeletedMemberBackup {
    Bc bc;
    String memberName;
    HashMap<String, Boolean> paidMapBackup = new HashMap<>();
    HashMap<String, Double> paidAmountBackup = new HashMap<>();
    HashMap<String, Double> paidBcAmountBackup = new HashMap<>();
    List<PaymentEntry> paymentEntriesBackup = new ArrayList<>();
}

// Stores last deleted members
private final List<DeletedMemberBackup> deletedMembersBackup = new ArrayList<>();

// 🔥 UNDO / REDO SYSTEM (MAX 10 HISTORY)
private static class HistoryAction {
    Runnable undoAction;
    Runnable redoAction;

    HistoryAction(Runnable undoAction, Runnable redoAction) {
        this.undoAction = undoAction;
        this.redoAction = redoAction;
    }
}

private final List<HistoryAction> undoStack = new ArrayList<>();
private final List<HistoryAction> redoStack = new ArrayList<>();
private static final int MAX_HISTORY = 10;
  
private final AppCompatActivity activity;  
private final Context context;  

// UI  
private final TextView menuButton;  
private final ImageButton btnUndo;
private final ImageButton btnRedo;
private final Spinner spinnerBc, spinnerMember;  
private final EditText editPayDate, editPayAmount;  
private final Button buttonAdd;  
private final LinearLayout tableContainer;  

// Data  
private final List<Bc> bcData;  
private ArrayAdapter<String> bcAdapter;  
private ArrayAdapter<String> memberAdapter;  

// Date formats  
private final SimpleDateFormat isoFormat;  
private final SimpleDateFormat displayFormat;  

// Room  
private final AppDatabase db;  
private final BcDao bcDao;  

public BcManager(AppCompatActivity activity,  
                 TextView menuButton,  
                 Spinner spinnerBc,  
                 Spinner spinnerMember,  
                 EditText editPayDate,  
                 EditText editPayAmount,  
                 Button buttonAdd,  
                 LinearLayout tableContainer,  
                 ImageButton btnUndo,
                 ImageButton btnRedo,
                 List<Bc> bcData,  
                 SimpleDateFormat isoFormat,  
                 SimpleDateFormat displayFormat) {  

    this.activity = activity;  
    this.context = activity;  
    this.menuButton = menuButton;  
    this.spinnerBc = spinnerBc;  
    this.spinnerMember = spinnerMember;  
    this.editPayDate = editPayDate;  
    this.editPayAmount = editPayAmount;  
    this.buttonAdd = buttonAdd;  
    this.tableContainer = tableContainer;  
    this.btnUndo = btnUndo;
    this.btnRedo = btnRedo;
    this.bcData = bcData;  
    this.isoFormat = isoFormat;  
    this.displayFormat = displayFormat;  

    this.db = AppDatabase.getDatabase(context);  
    this.bcDao = db.bcDao();  
}  

public void init() {  
    // Adapters  
    bcAdapter = new ArrayAdapter<>(context,  
            R.layout.spinner_item, new ArrayList<>());  
    bcAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);  
    bcAdapter.add("Select BC");  
    spinnerBc.setAdapter(bcAdapter);  
    spinnerBc.setSelection(0);  

    memberAdapter = new ArrayAdapter<>(context,  
            R.layout.spinner_item, new ArrayList<>());  
    memberAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);  
    memberAdapter.add("Select Member");  
    spinnerMember.setAdapter(memberAdapter);  
    spinnerMember.setSelection(0);  

    btnUndo.setOnClickListener(v -> undoLastAction());
    btnRedo.setOnClickListener(v -> redoLastAction());
    updateUndoRedoButtons();

    setupMenu();  
    setupDatePickers();  
    setupListeners();  

    // Load saved data from Room  
    loadFromRoomAndRefreshUi();  
}  

private void updateUndoRedoButtons() {
    if (btnUndo != null) btnUndo.setEnabled(!undoStack.isEmpty());
    if (btnRedo != null) btnRedo.setEnabled(!redoStack.isEmpty());
}

/* -------------------- ROOM: load/save -------------------- */  

private void loadFromRoomAndRefreshUi() {
    new Thread(() -> {
        List<BcEntity> entities = bcDao.getAll();
        List<Bc> loaded = new ArrayList<>();

        for (BcEntity e : entities) {

            Bc bc = new Bc(e.name, e.months, e.startDateIso);

            // ---------- BASIC FIELDS ----------
            bc.afterTaken = e.afterTaken;
            bc.afterTakenAmount = e.afterTakenAmount;

            // ---------- LISTS ----------
            if (e.members != null) bc.members = e.members;
            if (e.amounts != null) bc.amounts = e.amounts;

            // ---------- MAPS ----------
            bc.paid = (e.paid != null) ? e.paid : new HashMap<>();
            bc.paidAmount = (e.paidAmount != null) ? e.paidAmount : new HashMap<>();
            bc.paidBcAmount = (e.paidBcAmount != null) ? e.paidBcAmount : new HashMap<>();
            if (e.receiveAmounts != null) bc.receiveAmounts = e.receiveAmounts;
                bc.isReceiveAmountFixed = e.isReceiveAmountFixed;

            // ---------- 🔹 STEP 2 FIX (IMPORTANT) ----------
            // LOAD full payment history from Room
            bc.payments = (e.payments != null) ? e.payments : new ArrayList<>();

            // REBUILD paymentEntries for UI + popup
            bc.paymentEntries = new HashMap<>();
            for (PaymentEntry pe : bc.payments) {
                String key = bc.getPaidKey(pe.member, pe.monthIndex);
                List<PaymentEntry> list = bc.paymentEntries.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    bc.paymentEntries.put(key, list);
                }
                list.add(pe);
            }

            loaded.add(bc);
        }

        activity.runOnUiThread(() -> {
            bcData.clear();
            bcData.addAll(loaded);

            bcAdapter.clear();
            bcAdapter.add("Select BC");
            for (Bc bc : bcData) {
                bcAdapter.add(bc.name);
            }
            bcAdapter.notifyDataSetChanged();
            spinnerBc.setSelection(0);

            updateMembersDropdown();
        });

    }).start();
}


private void saveAllToRoom() {
    new Thread(() -> {
        bcDao.deleteAll();
        for (Bc bc : bcData) {

            BcEntity e = new BcEntity(
                    bc.name,
                    bc.months,
                    bc.startDateIso,
                    bc.afterTaken
            );

            // Existing fields
            e.afterTakenAmount = bc.afterTakenAmount;
            e.members = new ArrayList<>(bc.members);
            e.amounts = new ArrayList<>(bc.amounts);
            e.paid = new HashMap<>(bc.paid);
            e.paidAmount = new HashMap<>(bc.paidAmount);
            e.payments = new ArrayList<>(bc.payments);
            e.paidBcAmount = new HashMap<>(bc.paidBcAmount);

            // 🔴 🔴 🔴 MISSING RECEIVE AMOUNT (ROOT CAUSE FIX)
            e.isReceiveAmountFixed = bc.isReceiveAmountFixed;
            e.receiveAmounts = new ArrayList<>(bc.receiveAmounts);

            bcDao.insert(e);
        }
    }).start();
}

/* ---------- Menu ---------- */  

private void setupMenu() {  
    menuButton.setOnClickListener(v -> {  

        PopupMenu popup = new PopupMenu(context, menuButton);  

        // Add menu items  
        popup.getMenu().add(0, 1, 0, "Create New BC");  
        popup.getMenu().add(0, 2, 1, "Show BC List");  
        popup.getMenu().add(0, 3, 2, "Paid BC");
        popup.getMenu().add(0, 4, 3, "Summary");
        popup.getMenu().add(0, 5, 4, "Delete BC");
        popup.getMenu().add(0, 6, 5, "Delete A Member");

        popup.setOnMenuItemClickListener(item -> onMenuItemClick(item));  

        // OPTIONAL: Force icons to show (reflection – safe wrapped)  
        try {  
            Field field = PopupMenu.class.getDeclaredField("mPopup");  
            field.setAccessible(true);  
            Object menuPopupHelper = field.get(popup);  

            Method setForceShowIcon =  
                    menuPopupHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class);  
            setForceShowIcon.invoke(menuPopupHelper, true);  

            Method setBackground =  
                    menuPopupHelper.getClass().getDeclaredMethod(  
                            "setPopupBackgroundDrawable", Drawable.class);  

            setBackground.invoke(  
                    menuPopupHelper,  
                    ContextCompat.getDrawable(context, R.drawable.bg_popup_menu)  
            );  

        } catch (Exception e) {  
            e.printStackTrace();  
        }  

        popup.show();  
    });  
}  

private boolean onMenuItemClick(@NonNull MenuItem item) {  
    if (item.getItemId() == 1) {  
        openCreateBcDialog();  
        return true;  
    } else if (item.getItemId() == 2) {  
        showBcListTable();  
        return true;  
    }  else if (item.getItemId() == 3) {
        showPaidBcDialog();
        return true;
    }
    else if (item.getItemId() == 4) {
    showSummaryDialog();
    return true;
    }
    else if (item.getItemId() == 5) {
    showDeleteBcDialog();
    return true;
    }
    else if (item.getItemId() == 6) {
    showDeleteMemberDialog();
    return true;
    }
    return false;  
}  

/* ---------- Date pickers ---------- */  

private void setupDatePickers() {  
    View.OnClickListener dateClick = v -> {  
        final Calendar c = Calendar.getInstance();  
        int year = c.get(Calendar.YEAR);  
        int month = c.get(Calendar.MONTH);  
        int day = c.get(Calendar.DAY_OF_MONTH);  

        DatePickerDialog dp = new DatePickerDialog(context,  
                (view, year1, month1, dayOfMonth) -> {  
                    Calendar cal = Calendar.getInstance();  
                    cal.set(year1, month1, dayOfMonth, 0, 0, 0);  
                    String iso = isoFormat.format(cal.getTime());  
                    ((EditText) v).setText(iso);  
                },  
                year, month, day);  
        dp.show();  
    };  
    editPayDate.setOnClickListener(dateClick);  
}  

/* ---------- Listeners ---------- */  

private void setupListeners() {  
    spinnerBc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {  
        @Override  
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {  
            updateMembersDropdown();  
        }  

        @Override  
        public void onNothingSelected(AdapterView<?> parent) { }  
    });  

    buttonAdd.setOnClickListener(v -> markInstallment());  
}  

/* ---------- After Taken amount dialog ---------- */  

private void askAfterTakenAmount(AfterTakenCallback callback) {  
    final EditText input = new EditText(context);  
    input.setHint("Enter amount");  
    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);  

    new AlertDialog.Builder(context)  
            .setTitle("After Taken BC Amount")  
            .setView(input)  
            .setCancelable(false)  
            .setPositiveButton("OK", (d, which) -> {  
                String s = input.getText().toString().trim();  
                double amount = 0.0;  
                try {  
                    if (!s.isEmpty()) amount = Double.parseDouble(s);  
                } catch (Exception ignored) { }  
                callback.onValue(amount);  
            })  
            .setNegativeButton("Cancel", (d, which) -> callback.onCancelled())  
            .show();  
}  

private interface AfterTakenCallback {  
    void onValue(double amount);  
    void onCancelled();  
}  

/* ---------- Create BC dialog ---------- */  

private void openCreateBcDialog() {

    View dialogView = activity.getLayoutInflater()
            .inflate(R.layout.dialog_create_bc, null);

    EditText editBcName = dialogView.findViewById(R.id.editBcName);
    EditText editMonths = dialogView.findViewById(R.id.editMonths);
    EditText editStartDate = dialogView.findViewById(R.id.editStartDate);
    // 🔹 RECEIVE AMOUNT views (FROM XML — DO NOT CREATE MANUALLY)
    Spinner spinnerReceiveType = dialogView.findViewById(R.id.spinnerReceiveType);
    LinearLayout layoutReceiveAmounts = dialogView.findViewById(R.id.layoutReceiveAmounts);
    LinearLayout layoutMembers = dialogView.findViewById(R.id.layoutMembers);
    Spinner spinnerAmountType = dialogView.findViewById(R.id.spinnerAmountType);
    LinearLayout layoutAmounts = dialogView.findViewById(R.id.layoutAmounts);
    CheckBox checkAfterTaken = dialogView.findViewById(R.id.checkAfterTaken);
    TextView buttonSaveBc = dialogView.findViewById(R.id.buttonSaveBc);
    TextView buttonCancelBc = dialogView.findViewById(R.id.buttonCancelBc);

    
    /* ---------- AFTER TAKEN ---------- */

    final double[] afterTakenAmountHolder = new double[]{0.0};

    checkAfterTaken.setOnCheckedChangeListener((btn, isChecked) -> {
        if (isChecked) {
            askAfterTakenAmount(new AfterTakenCallback() {
                @Override
                public void onValue(double amount) {
                    afterTakenAmountHolder[0] = amount;
                }

                @Override
                public void onCancelled() {
                    checkAfterTaken.setChecked(false);
                    afterTakenAmountHolder[0] = 0.0;
                }
            });
        } else {
            afterTakenAmountHolder[0] = 0.0;
        }
    });

    /* ---------- DATE PICKER ---------- */

    editStartDate.setOnClickListener(v -> {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(context,
                (view, y, m, d) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(y, m, d, 0, 0, 0);
                    editStartDate.setText(isoFormat.format(cal.getTime()));
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    });

    /* ---------- AMOUNT TYPE ---------- */

    ArrayAdapter<String> amountTypeAdapter = new ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            Arrays.asList("Select Amount Type", "Fixed", "Random")
    );
    amountTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerAmountType.setAdapter(amountTypeAdapter);

    // 🔹 Receive Amount type spinner
    ArrayAdapter<String> receiveTypeAdapter = new ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            Arrays.asList("Select Receive Amount Type", "Fixed", "Random")
    );
    receiveTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerReceiveType.setAdapter(receiveTypeAdapter);

    editMonths.addTextChangedListener(new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        @Override
        public void afterTextChanged(Editable s) {
            createMemberInputs(editMonths, layoutMembers);
            amountTypeChange(editMonths, spinnerAmountType, layoutAmounts);
        }
    });

    spinnerAmountType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            amountTypeChange(editMonths, spinnerAmountType, layoutAmounts);
        }
        @Override public void onNothingSelected(AdapterView<?> p) {}
    });

    /* ---------- RECEIVE AMOUNT LISTENER ---------- */

    spinnerReceiveType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            layoutReceiveAmounts.removeAllViews();
            int months = safeParseInt(editMonths.getText().toString());

            if (position == 1) { // FIXED
                EditText e = new EditText(context);
                e.setHint("Receive Amount");
                e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                layoutReceiveAmounts.addView(e);

            } else if (position == 2) { // RANDOM
                for (int i = 0; i < months; i++) {
                    EditText e = new EditText(context);
                    e.setHint("Receive Amount - Month " + (i + 1));
                    e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    layoutReceiveAmounts.addView(e);
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    });

    /* ---------- DIALOG ---------- */

    AlertDialog dialog = new AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create();

    buttonSaveBc.setOnClickListener(v -> {

        String name = editBcName.getText().toString().trim();
        int months = safeParseInt(editMonths.getText().toString());
        String startDate = editStartDate.getText().toString().trim();

        if (name.isEmpty() || months <= 0 || startDate.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Bc bc = new Bc(name, months, startDate);

        // Members
        for (int i = 0; i < layoutMembers.getChildCount(); i++) {
            View v1 = layoutMembers.getChildAt(i);
            if (v1 instanceof EditText) {
                String m = ((EditText) v1).getText().toString().trim();
                if (!m.isEmpty()) bc.members.add(m);
            }
        }

        // Amounts
        for (int i = 0; i < layoutAmounts.getChildCount(); i++) {
            View v1 = layoutAmounts.getChildAt(i);
            if (v1 instanceof EditText) {
                String a = ((EditText) v1).getText().toString().trim();
                bc.amounts.add(a.isEmpty() ? 0.0 : Double.parseDouble(a));
            }
        }

        // 🔹 RECEIVE AMOUNTS SAVE
        bc.isReceiveAmountFixed = spinnerReceiveType.getSelectedItemPosition() == 1;
        bc.receiveAmounts.clear();

        for (int i = 0; i < layoutReceiveAmounts.getChildCount(); i++) {
            View v1 = layoutReceiveAmounts.getChildAt(i);
            if (v1 instanceof EditText) {
                String r = ((EditText) v1).getText().toString().trim();
                bc.receiveAmounts.add(r.isEmpty() ? 0.0 : Double.parseDouble(r));
            }
        }

        bc.afterTaken = checkAfterTaken.isChecked();
        bc.afterTakenAmount = bc.afterTaken ? afterTakenAmountHolder[0] : 0.0;

        bcData.add(bc);
        bcAdapter.add(bc.name);
        bcAdapter.notifyDataSetChanged();

        saveAllToRoom();
        dialog.dismiss();
    });

    buttonCancelBc.setOnClickListener(v -> dialog.dismiss());
    dialog.show();
}

private void createMemberInputs(EditText editMonths, LinearLayout layoutMembers) {  
    layoutMembers.removeAllViews();  
    int m = safeParseInt(editMonths.getText().toString());  
    for (int i = 0; i < m; i++) {  
        EditText e = new EditText(context);  
        e.setHint("Member " + (i + 1));  
        layoutMembers.addView(e);  
    }  
}  

private void amountTypeChange(EditText editMonths,  
                              Spinner spinnerAmountType,  
                              LinearLayout layoutAmounts) {  
    layoutAmounts.removeAllViews();  
    int m = safeParseInt(editMonths.getText().toString());  
    String selected = spinnerAmountType.getSelectedItem() != null  
            ? spinnerAmountType.getSelectedItem().toString().toLowerCase(Locale.ROOT)  
            : "";  

    if (selected.contains("fixed")) {  
        EditText e = new EditText(context);  
        e.setHint("Amount");  
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);  
        layoutAmounts.addView(e);  
    } else if (selected.contains("random")) {  
        for (int i = 0; i < m; i++) {  
            EditText e = new EditText(context);  
            e.setHint("Amount Month " + (i + 1));  
            e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);  
            layoutAmounts.addView(e);  
        }  
    }  
}  

/* ---------- Dropdowns & tables ---------- */  

private void updateMembersDropdown() {  
    memberAdapter.clear();  
    memberAdapter.add("Select Member");  
    int index = spinnerBc.getSelectedItemPosition();  
    if (index <= 0 || index > bcData.size()) {  
        memberAdapter.notifyDataSetChanged();  
        renderMainTable(null);  
        return;  
    }  
    Bc bc = bcData.get(index -1);  
    memberAdapter.addAll(bc.members);  
    memberAdapter.notifyDataSetChanged();  
    spinnerMember.setSelection(0);  
    renderMainTable(bc);  
}  

private void showBcListTable() {
    tableContainer.removeAllViews();

    for (Bc bc : bcData) {

        // 🔷 ===== BC TITLE ROW =====
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(16, 16, 16, 8);

        // BC Name (Premium Style)
        TextView title = new TextView(context);
        title.setText(bc.name);
        title.setTextSize(18f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setTextColor(Color.parseColor("#0D47A1")); // Premium blue
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // 🖨 Print Button (Elevated Style)
        Button btnPrint = new Button(context);
        btnPrint.setText("Print");
        btnPrint.setTextSize(12f);
        btnPrint.setTextColor(Color.WHITE);
        btnPrint.setAllCaps(false);
        btnPrint.setPadding(20, 8, 20, 8);
        btnPrint.setBackgroundResource(R.drawable.bg_print_button);
        btnPrint.setElevation(8f);

        LinearLayout.LayoutParams printParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        printParams.setMargins(12, 0, 0, 0);
        btnPrint.setLayoutParams(printParams);

        titleRow.addView(title);
        titleRow.addView(btnPrint);
        tableContainer.addView(titleRow);

        // 🔷 ===== CARD WRAPPER (PREMIUM LOOK) =====
        CardView card = new CardView(context);
        card.setRadius(26f);
        card.setCardElevation(12f);
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 8, 16, 24);
        card.setLayoutParams(cardParams);

        LinearLayout tableCaptureLayout = new LinearLayout(context);
        tableCaptureLayout.setOrientation(LinearLayout.VERTICAL);
        card.addView(tableCaptureLayout);

        // 🔷 ===== HORIZONTAL SCROLL + TABLE =====
        HorizontalScrollView hScroll = new HorizontalScrollView(context);
        hScroll.setHorizontalScrollBarEnabled(true);
        hScroll.setFillViewport(false);
        hScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        hScroll.setFadingEdgeLength(50);
        hScroll.setHorizontalFadingEdgeEnabled(true);

        TableLayout table = new TableLayout(context);
        table.setStretchAllColumns(false);
        table.setShrinkAllColumns(false);
        table.setPadding(8, 8, 8, 8);
        table.setBackgroundColor(Color.parseColor("#ECEFF1")); // Soft outer tint

        // ========== HEADER ==========
        TableRow header = new TableRow(context);
        header.setElevation(6f); // Floating header effect

        addCellFixedWidth(header, "Sr", true, 20);
        addCellFixedWidth(header, "Date", true, 110);
        addCellFixedWidth(header, "Installment\nAmount", true, 100);
        addCellFixedWidth(header, "Receive\nAmount", true, 90);

        if (bc.afterTaken) {
            addCellFixedWidth(header, "After\nTaken", true, 90);
        }

        table.addView(header);

        // ========== ROWS ==========
        for (int i = 0; i < bc.months; i++) {

            TableRow row = new TableRow(context);

            // Zebra striping
            if (i % 2 == 0) {
                row.setBackgroundColor(Color.parseColor("#F8FAFC"));
            } else {
                row.setBackgroundColor(Color.WHITE);
            }

            addCellFixedWidth(row, String.valueOf(i + 1), false, 80);

            Calendar cal = parseIsoDate(bc.startDateIso);
            if (cal != null) cal.add(Calendar.MONTH, i);
            String dateStr = cal != null ? displayFormat.format(cal.getTime()) : "-";
            addCellFixedWidth(row, dateStr, false, 120);

            // Installment Amount
            double amount = 0.0;
            if (!bc.amounts.isEmpty()) {
                if (bc.amounts.size() == 1) amount = bc.amounts.get(0);
                else if (bc.amounts.size() > i) amount = bc.amounts.get(i);
            }
            addCellFixedWidth(row, "₹" + String.format("%.0f", amount), false, 130);

            // Receive Amount
            double receiveAmount = 0.0;
            if (!bc.receiveAmounts.isEmpty()) {
                if (bc.isReceiveAmountFixed) receiveAmount = bc.receiveAmounts.get(0);
                else if (bc.receiveAmounts.size() > i) receiveAmount = bc.receiveAmounts.get(i);
            }
            addCellFixedWidth(row, "₹" + String.format("%.0f", receiveAmount), false, 130);

            if (bc.afterTaken) {
                addCellFixedWidth(row, "₹" + String.format("%.0f", bc.afterTakenAmount), false, 130);
            }

            table.addView(row);
        }

        hScroll.addView(table);
        tableCaptureLayout.addView(hScroll);

        tableContainer.addView(card);

        btnPrint.setOnClickListener(v -> captureAndSaveTable(tableCaptureLayout, bc.name));
    }
}

private void captureAndSaveTable(View view, String bcName) {
    try {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        String filename = "BC_" + bcName + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BCApp");

        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        OutputStream out = context.getContentResolver().openOutputStream(uri);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();

        Toast.makeText(context, "Image Saved to Gallery", Toast.LENGTH_SHORT).show();

    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show();
    }
}

private void renderMainTable(Bc bc) {
    tableContainer.removeAllViews();
    if (bc == null) return;

    TextView title = new TextView(context);
    title.setText("Main BC Table");
    title.setTextColor(Color.parseColor("#000000"));
    title.setTextSize(16f);
    title.setTypeface(null, Typeface.BOLD);
    title.setPadding(0, 8, 0, 4);
    tableContainer.addView(title);

    TableLayout table = new TableLayout(context);
    table.setPadding(8, 8, 8, 8);
    table.setBackgroundColor(Color.parseColor("#ECEFF1")); // soft outer background
    table.setStretchAllColumns(false);

    TableRow header = new TableRow(context);
    header.setElevation(6f); // floating header feel
    header.setBackgroundColor(Color.parseColor("#E3F2FD")); // light premium blue
    addCell(header, "Sr", true);
    addCell(header, "Date", true);
    addCell(header, "Amount", true);
    addCell(header, "Member", true);

    Calendar headerCal = parseIsoDate(bc.startDateIso);
    for (int i = 0; i < bc.months; i++) {
        String monthName = "M" + (i + 1); // fallback

        if (headerCal != null) {
            monthName = new SimpleDateFormat("MMM-yy", Locale.getDefault())
                    .format(headerCal.getTime());
            headerCal.add(Calendar.MONTH, 1);
        }

        addCell(header, monthName, true);
    }
    addCell(header, "Total", true);
    addCell(header, "Paid BC", true);
    table.addView(header);

    for (int r = 0; r < bc.members.size(); r++) {
        String member = bc.members.get(r);
        TableRow row = new TableRow(context);
        if (r % 2 == 0) {
        row.setBackgroundColor(Color.parseColor("#F7F9FC"));
        } else {
        row.setBackgroundColor(Color.WHITE);
        }

        addCell(row, String.valueOf(r + 1), false);

        Calendar cal = parseIsoDate(bc.startDateIso);
        if (cal != null) cal.add(Calendar.MONTH, r);
        String dateStr = cal != null ? displayFormat.format(cal.getTime()) : "-";
        addCell(row, dateStr, false);

        double amount = bc.amounts.size() > r
                ? bc.amounts.get(r)
                : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);
        addCell(row, String.valueOf(amount), false);

        // 🔹 MEMBER CELL WITH UNPAID HIGHLIGHT
        TextView memberCell = new TextView(context);
        memberCell.setText(member);
        memberCell.setPadding(16, 12, 16, 12);
        memberCell.setGravity(Gravity.CENTER);
        memberCell.setMinHeight(64);
        memberCell.setTextSize(15f);
        memberCell.setLetterSpacing(0.05f);

        // 🔴 CHECK CURRENT MONTH (IMPORTANT)
        // Use CURRENT month index based on today
        Calendar startCal = parseIsoDate(bc.startDateIso);
        Calendar todayCal = Calendar.getInstance();

        int currentMonthIndex = -1;
        if (startCal != null) {
            currentMonthIndex =
                    (todayCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
                    (todayCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH));
        }

        // Apply highlight only for valid month
        // default
        memberCell.setTextColor(Color.parseColor("#000000"));
        memberCell.setTypeface(null, Typeface.BOLD);
        memberCell.setBackgroundResource(R.drawable.table_cell_border);

        // Apply highlight only for valid month
        if (currentMonthIndex >= 0 && currentMonthIndex < bc.months) {

            // 🔴 UNPAID → bold red text
            if (shouldHighlightUnpaid(bc, member, currentMonthIndex)) {
                memberCell.setTextColor(Color.parseColor("#D32F2F"));
                memberCell.setTypeface(null, Typeface.BOLD);
            }

            // 🔴 OVERDUE → bold red text + light red background
            if (isOverDue(bc, member, currentMonthIndex)) {
                memberCell.setTextColor(Color.parseColor("#D32F2F"));
                memberCell.setTypeface(null, Typeface.BOLD);
                memberCell.setBackgroundResource(R.drawable.table_cell_border_overdue);
            }
        }
        TableRow.LayoutParams memberLp =
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.MATCH_PARENT
                );
        memberLp.setMargins(1, 1, 1, 1);
        memberCell.setLayoutParams(memberLp);

        row.addView(memberCell);

        double totalPaid = 0.0;
        boolean hasPartial = false;

        for (int m = 0; m < bc.months; m++) {

            LinearLayout cellContainer = new LinearLayout(context);
            cellContainer.setOrientation(LinearLayout.VERTICAL);
            cellContainer.setGravity(Gravity.CENTER);
            cellContainer.setPadding(8, 10, 8, 10);
            cellContainer.setMinimumHeight(84);
            cellContainer.setElevation(2f);
            cellContainer.setBackgroundResource(R.drawable.table_cell_border);

            String key = bc.getPaidKey(member, m);
            Double paidAmtObj = bc.paidAmount.get(key);
            double paidAmt = paidAmtObj != null ? paidAmtObj : 0.0;
            
            boolean hasAnyPayment = paidAmt > 0;
          
            double expectedAmt = bc.amounts.size() > m
                    ? bc.amounts.get(m)
                    : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);

            if (paidAmt > 0) {
                totalPaid += paidAmt;

                if (paidAmt < expectedAmt) {
                    hasPartial = true;
                }
            } else {
                hasPartial = true;
            }
                  
            if (hasAnyPayment) {
                if (paidAmt >= expectedAmt) {
                // 🟢 FULLY PAID
                cellContainer.setBackgroundResource(
                R.drawable.table_cell_border_paid
                );
            } else {
                // 🔴 PARTIALLY PAID
               cellContainer.setBackgroundResource(
               R.drawable.table_cell_border_partialy_paid // light red
                );
            }

                // ✅ Tick (same for full & partial)
                TextView tick = new TextView(context);
                tick.setText("✅");
                tick.setTextSize(18f);
                tick.setTextColor(Color.parseColor("#2E7D32"));
                tick.setGravity(Gravity.CENTER);
                cellContainer.addView(tick);

                // Amount badge
                TextView amountBadge = new TextView(context);
                amountBadge.setText("₹" + String.format("%.0f", paidAmt));
                amountBadge.setTextSize(8f);
                amountBadge.setTypeface(null, Typeface.BOLD);
                amountBadge.setGravity(Gravity.CENTER);
                amountBadge.setPadding(10, 4, 10, 4);

                // 🔴 PARTIAL vs 🟢 FULL
                if (paidAmt < expectedAmt) {
                    // PARTIAL PAYMENT
                    amountBadge.setBackgroundResource(
                            R.drawable.amount_badge_red
                    );
                    amountBadge.setTextColor(Color.parseColor("#D32F2F"));
                } else {
                    // FULL PAYMENT
                    amountBadge.setBackgroundResource(
                            R.drawable.amount_badge_green
                    );
                    amountBadge.setTextColor(Color.parseColor("#FFFFFF"));
                }

                LinearLayout.LayoutParams badgeLp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                badgeLp.topMargin = 4;
                amountBadge.setLayoutParams(badgeLp);

                cellContainer.addView(amountBadge);

            } else {
                // ☐ UNPAID ONLY
                TextView checkbox = new TextView(context);
                checkbox.setText("☐");
                checkbox.setTextSize(18f);
                checkbox.setTextColor(Color.BLACK);
                checkbox.setGravity(Gravity.CENTER);
                cellContainer.addView(checkbox);
            }

            TableRow.LayoutParams lp =
                    new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.MATCH_PARENT
                    );
            lp.setMargins(1, 1, 1, 1);
            cellContainer.setLayoutParams(lp);

            row.addView(cellContainer);
        }
        TextView totalCell = new TextView(context);
        totalCell.setText("₹" + String.format("%.0f", totalPaid));
        totalCell.setGravity(Gravity.CENTER);
        totalCell.setTextSize(15f);
        totalCell.setLetterSpacing(0.04f);
        totalCell.setTypeface(null, Typeface.BOLD);
        totalCell.setPadding(16, 12, 16, 12);
        totalCell.setMinHeight(64);
        totalCell.setBackgroundResource(R.drawable.table_cell_border);

        final String clickedMember = member;
        final double clickedTotalPaid = totalPaid;

        totalCell.setOnClickListener(v -> {
            showTotalBreakdownDialog(bc, clickedMember);
        });

        if (hasPartial) {
            totalCell.setTextColor(Color.parseColor("#D32F2F")); // 🔴 PARTIAL
        } else {
            totalCell.setTextColor(Color.parseColor("#1B5E20")); // 🟢 FULL
        }

        TableRow.LayoutParams totalLp =
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.MATCH_PARENT
                );
        totalLp.setMargins(1, 1, 1, 1);
        totalCell.setLayoutParams(totalLp);

        row.addView(totalCell);

        Double paidBc = bc.paidBcAmount.get(member);

        TextView paidBcCell = new TextView(context);
        paidBcCell.setText(paidBc != null ? "₹" + String.format("%.0f", paidBc) : "-");
        paidBcCell.setGravity(Gravity.CENTER);
        paidBcCell.setTextSize(15f);
        paidBcCell.setLetterSpacing(0.04f);
        paidBcCell.setTypeface(null, Typeface.BOLD);
        paidBcCell.setTextColor(Color.BLACK);
        paidBcCell.setPadding(16, 12, 16, 12);
        paidBcCell.setMinHeight(64);
        paidBcCell.setBackgroundResource(R.drawable.table_cell_border);

        TableRow.LayoutParams paidBcLp =
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.MATCH_PARENT
                );
        paidBcLp.setMargins(1, 1, 1, 1);
        paidBcCell.setLayoutParams(paidBcLp);

        row.addView(paidBcCell);

        table.addView(row);
    }

    CardView card = new CardView(context);
    card.setRadius(28f);
    card.setCardElevation(14f);
    card.setUseCompatPadding(true);
    card.setCardBackgroundColor(Color.WHITE);

    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    cardParams.setMargins(16, 12, 16, 24);
    card.setLayoutParams(cardParams);

    HorizontalScrollView scrollWrap = new HorizontalScrollView(context);
    scrollWrap.setHorizontalScrollBarEnabled(true);
    scrollWrap.addView(table);

    card.addView(scrollWrap);
    tableContainer.addView(card);
    }

/* ---------- Installments ---------- */  

private void markInstallment() {
    int bcIndex = spinnerBc.getSelectedItemPosition();
    int memberIndex = spinnerMember.getSelectedItemPosition();
    String dateVal = editPayDate.getText().toString().trim();
    String amountVal = editPayAmount.getText().toString().trim();

    if (bcIndex <= 0 || memberIndex <= 0 || dateVal.isEmpty() || amountVal.isEmpty()) {
        Toast.makeText(context, "Please select BC, Member, Date and Amount", Toast.LENGTH_SHORT).show();
        return;
    }

    double enteredAmount;
    try {
        enteredAmount = Double.parseDouble(amountVal);
        if (enteredAmount <= 0) throw new NumberFormatException();
    } catch (Exception e) {
        Toast.makeText(context, "Enter valid amount", Toast.LENGTH_SHORT).show();
        return;
    }

    Bc bc = bcData.get(bcIndex - 1);

    Calendar start = parseIsoDate(bc.startDateIso);
    Calendar paid = parseIsoDate(dateVal);

    if (start == null || paid == null) {
        Toast.makeText(context, "Invalid date", Toast.LENGTH_SHORT).show();
        return;
    }

    int monthIndex =
            (paid.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 +
            (paid.get(Calendar.MONTH) - start.get(Calendar.MONTH));

    if (monthIndex < 0 || monthIndex >= bc.months) {
        Toast.makeText(context, "Selected date is outside BC duration", Toast.LENGTH_SHORT).show();
        return;
    }

    String member = bc.members.get(memberIndex - 1);
    String key = bc.getPaidKey(member, monthIndex);

    if (bc.payments == null) bc.payments = new ArrayList<>();
    if (bc.paymentEntries == null) bc.paymentEntries = new HashMap<>();

    // 🔹 BACKUP OLD VALUES BEFORE CHANGE
    double previousPaid = bc.paidAmount.getOrDefault(key, 0.0);
    boolean previousPaidStatus = bc.paid.getOrDefault(key, false);

    double newPaid = previousPaid + enteredAmount;

    PaymentEntry pe = new PaymentEntry(member, monthIndex, enteredAmount, dateVal);

    // 🔥 APPLY PAYMENT
    bc.payments.add(pe);
    rebuildPaymentEntries(bc);

    bc.paidAmount.put(key, newPaid);

    double expectedAmount =
            bc.amounts.size() > monthIndex
                    ? bc.amounts.get(monthIndex)
                    : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);

    bc.paid.put(key, newPaid >= expectedAmount);

    // 🧠 UNDO / REDO SUPPORT
    pushToUndoStack(
        () -> { // UNDO
            bc.payments.remove(pe);
            rebuildPaymentEntries(bc);

            if (previousPaid == 0.0)
                bc.paidAmount.remove(key);
            else
                bc.paidAmount.put(key, previousPaid);

            bc.paid.put(key, previousPaidStatus);

            saveAllToRoom();
            renderMainTable(bc);
        },
        () -> { // REDO
            bc.payments.add(pe);
            rebuildPaymentEntries(bc);

            bc.paidAmount.put(key, newPaid);
            bc.paid.put(key, newPaid >= expectedAmount);

            saveAllToRoom();
            renderMainTable(bc);
        }
    );

    saveAllToRoom();
    activity.runOnUiThread(() -> renderMainTable(bc));

    editPayDate.setText("");
    editPayAmount.setText("");
}

private void rebuildPaymentEntries(Bc bc) {
    bc.paymentEntries.clear();
    for (PaymentEntry p : bc.payments) {
        String k = bc.getPaidKey(p.member, p.monthIndex);
        List<PaymentEntry> list = bc.paymentEntries.get(k);
        if (list == null) list = new ArrayList<>();
        list.add(p);
        bc.paymentEntries.put(k, list);
    }
}

// 🔴 Highlight unpaid member name until due date
private boolean shouldHighlightUnpaid(Bc bc, String member, int monthIndex) {

    Calendar today = Calendar.getInstance();

    // Due date = BC start date + monthIndex
    Calendar due = parseIsoDate(bc.startDateIso);
    if (due == null) return false;

    due.add(Calendar.MONTH, monthIndex);

    String key = bc.getPaidKey(member, monthIndex);

    boolean isPaid =
            bc.paid.containsKey(key)
                    && Boolean.TRUE.equals(bc.paid.get(key));

    // Highlight only if:
    // 1) not paid
    // 2) today is BEFORE or SAME as due date
    return !isPaid && !today.after(due);
      }

// 🔴 Overdue = unpaid AND today AFTER due date
private boolean isOverDue(Bc bc, String member, int monthIndex) {

    Calendar today = Calendar.getInstance();

    Calendar due = parseIsoDate(bc.startDateIso);
    if (due == null) return false;

    due.add(Calendar.MONTH, monthIndex);

    String key = bc.getPaidKey(member, monthIndex);

    boolean isPaid =
            bc.paid.containsKey(key)
                    && Boolean.TRUE.equals(bc.paid.get(key));

    return !isPaid && today.after(due);
}

/* ---------- Helpers ---------- */  

private void addCell(TableRow row, String text, boolean header) {

    TextView tv = new TextView(context);
    tv.setText(text);
    tv.setPadding(20, 16, 20, 16);   // More breathing space
    tv.setGravity(Gravity.CENTER);
    tv.setMinHeight(72);

    if (header) {
        // 🔷 PREMIUM HEADER STYLE
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextSize(15f);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.parseColor("#1565C0")); // Premium blue header
    } else {

        tv.setTextSize(14f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setTextColor(Color.parseColor("#212121")); // Dark premium text

        boolean isSrNumber = text.matches("\\d+");
        boolean isDate = text.matches("\\d{2}/\\d{2}/\\d{4}");
        boolean isAmount = text.matches("\\d+(\\.\\d+)?");

        // 🔹 Highlight important columns
        if (isSrNumber || isDate) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.BLACK);
        }

        // 💰 Premium money style
        if (isAmount) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.parseColor("#2E7D32")); // Rich green
        }

        // ✅ Paid tick style
        if ("✅".equals(text)) {
            tv.setTextSize(16f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.parseColor("#1B5E20"));
            tv.setBackgroundResource(R.drawable.premium_paid_bg);
        } else {
            tv.setBackgroundResource(R.drawable.premium_cell_bg);
        }
    }

    TableRow.LayoutParams lp = new TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.MATCH_PARENT
    );
    lp.setMargins(2, 2, 2, 2); // Softer spacing
    tv.setLayoutParams(lp);

    row.addView(tv);
}

private void addCellFixedWidth(TableRow row, String text, boolean header, int widthDp) {

    TextView tv = new TextView(context);
    tv.setText(text);
    tv.setPadding(12, 12, 12, 12);
    tv.setMinHeight(dpToPx(48));

    if (header) {
        tv.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.parseColor("#1565C0")); // Same premium header
        tv.setTextSize(14f);
    } else {
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setTextColor(Color.parseColor("#212121"));
        tv.setBackgroundResource(R.drawable.premium_cell_bg);
    }

    TableRow.LayoutParams params =
            new TableRow.LayoutParams(dpToPx(widthDp), TableRow.LayoutParams.WRAP_CONTENT);
    params.setMargins(2, 2, 2, 2);
    tv.setLayoutParams(params);

    row.addView(tv);
}

// 🔹 Measure text width for auto-fit
private int measureTextWidth(String text, float textSize) {
    TextView temp = new TextView(context);
    temp.setText(text);
    temp.setTextSize(textSize / context.getResources().getDisplayMetrics().scaledDensity);
    temp.measure(0, 0);
    return temp.getMeasuredWidth();
}

private int dpToPx(int dp) {
    return (int) (dp * context.getResources().getDisplayMetrics().density);
}

private int safeParseInt(String s) {  
    try {  
        return Integer.parseInt(s.trim());  
    } catch (Exception e) {  
        return 0;  
    }  
}  

private Calendar parseIsoDate(String iso) {  
    try {  
        Date d = isoFormat.parse(iso);  
        Calendar c = Calendar.getInstance();  
        c.setTime(d);  
        return c;  
    } catch (ParseException e) {  
        return null;  
    }  
}

private void addDivider(LinearLayout root) {
    View v = new View(context);
    LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2);
    lp.setMargins(0, 16, 0, 16);
    v.setLayoutParams(lp);
    v.setBackgroundColor(Color.LTGRAY);
    root.addView(v);
}

private void showTotalBreakdownDialog(Bc bc, String member) {

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Payment Breakdown");

    ScrollView scrollView = new ScrollView(context);
    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(32, 24, 32, 24);
    scrollView.addView(root);

    double totalPaid = 0.0;

    // Member title
    TextView tvMember = new TextView(context);
    tvMember.setText("Member: " + member);
    tvMember.setTextSize(18f);
    tvMember.setTypeface(null, Typeface.BOLD);
    root.addView(tvMember);

    addDivider(root);

    // Month-wise entries
    for (int m = 0; m < bc.months; m++) {

        String key = bc.getPaidKey(member, m);
        List<PaymentEntry> list =
        bc.paymentEntries != null ? bc.paymentEntries.get(key) : null;

        if (list == null || list.isEmpty()) continue;

        for (PaymentEntry e : list) {

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tvMonth = new TextView(context);
            tvMonth.setText("M" + (m + 1));
            tvMonth.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView tvAmt = new TextView(context);
            tvAmt.setText("₹" + String.format("%.0f", e.amount));
            tvAmt.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView tvDate = new TextView(context);
            tvDate.setText(e.paidDateIso);
            tvDate.setGravity(Gravity.END);
            tvDate.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            row.addView(tvMonth);
            row.addView(tvAmt);
            row.addView(tvDate);

            root.addView(row);

            totalPaid += e.amount;
        }
    }

    addDivider(root);

    // Expected total
    double expectedTotal = 0.0;
    for (double a : bc.amounts) expectedTotal += a;

    TextView tvExpected = new TextView(context);
    tvExpected.setText("Expected Total: ₹" + String.format("%.0f", expectedTotal));
    tvExpected.setTypeface(null, Typeface.BOLD);
    root.addView(tvExpected);

    // Total paid
    TextView tvPaid = new TextView(context);
    tvPaid.setText("Total Paid: ₹" + String.format("%.0f", totalPaid));
    tvPaid.setTypeface(null, Typeface.BOLD);
    root.addView(tvPaid);

    // Balance
    double balance = expectedTotal - totalPaid;
    TextView tvBalance = new TextView(context);
    tvBalance.setText("Balance: ₹" + String.format("%.0f", balance));
    tvBalance.setTypeface(null, Typeface.BOLD);
    tvBalance.setTextColor(balance > 0
            ? Color.parseColor("#D32F2F")
            : Color.parseColor("#1B5E20"));
    root.addView(tvBalance);

    builder.setView(scrollView);
    builder.setPositiveButton("OK", null);
    builder.show();
}

private void showPaidBcDialog() {

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    LayoutInflater inflater = LayoutInflater.from(context);
    View view = inflater.inflate(R.layout.dialog_bc_paid, null);
    builder.setView(view);

    AlertDialog dialog = builder.create();
    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    dialog.show();

    // Views from custom layout
    Spinner bcSpinner = view.findViewById(R.id.spinnerBC);
    Spinner memberSpinner = view.findViewById(R.id.spinnerMember);
    EditText amountInput = view.findViewById(R.id.etPaidAmount);
    TextView btnCancel = view.findViewById(R.id.btnCancel);
    TextView btnOk = view.findViewById(R.id.btnOk);

    // BC Spinner Adapter
    ArrayAdapter<String> bcSpinAdapter =
            new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
    bcSpinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    bcSpinAdapter.add("Select BC");
    for (Bc bc : bcData) bcSpinAdapter.add(bc.name);
    bcSpinner.setAdapter(bcSpinAdapter);

    // Member Adapter
    ArrayAdapter<String> memAdapter =
            new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
    memAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    memberSpinner.setAdapter(memAdapter);

    // BC selection → load members
    bcSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            memAdapter.clear();
            if (pos > 0) {
                memAdapter.addAll(bcData.get(pos - 1).members);
            }
            memAdapter.notifyDataSetChanged();
        }
        @Override public void onNothingSelected(AdapterView<?> p) {}
    });

    // Cancel button
    btnCancel.setOnClickListener(v -> dialog.dismiss());

    // OK button (YOUR SAME LOGIC)
    btnOk.setOnClickListener(v -> {

        int bcPos = bcSpinner.getSelectedItemPosition();
        int memPos = memberSpinner.getSelectedItemPosition();
        String amtStr = amountInput.getText().toString().trim();

        if (bcPos <= 0 || memPos < 0 || amtStr.isEmpty()) {
            Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amt = Double.parseDouble(amtStr);

        Bc bc = bcData.get(bcPos - 1);
        String member = bc.members.get(memPos);

        Double oldAmount = bc.paidBcAmount.get(member);

        bc.paidBcAmount.put(member, amt);
        saveAllToRoom();
        renderMainTable(bc);

        pushToUndoStack(
            () -> { // UNDO
                if (oldAmount == null) bc.paidBcAmount.remove(member);
                else bc.paidBcAmount.put(member, oldAmount);

                saveAllToRoom();
                renderMainTable(bc);
            },
            () -> { // REDO
                bc.paidBcAmount.put(member, amt);
                saveAllToRoom();
                renderMainTable(bc);
            }
        );

        dialog.dismiss();
    });
}

private void showSummaryDialog() {

    ScrollView scrollView = new ScrollView(context);
    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(32, 32, 32, 32);
    scrollView.addView(root);

    double grandCollected = 0;
    double grandPaid = 0;

    for (Bc bc : bcData) {

        double collected = 0;
        double paid = 0;

        // 🔹 TOTAL COLLECTED = sum of all member installment payments
        for (double amt : bc.paidAmount.values()) {
            collected += amt;
        }

        // 🔹 TOTAL PAID BC
        for (double v : bc.paidBcAmount.values()) {
            paid += v;
        }

        grandCollected += collected;
        grandPaid += paid;

        // 🔹 BC NAME
        TextView title = new TextView(context);
        title.setText(bc.name);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 24, 0, 12);
        root.addView(title);

        root.addView(createSummaryBox("Total Collected", collected, "#E3F2FD"));
        root.addView(createSummaryBox("Total BC Paid", paid, "#E8F5E9"));

        double balance = collected - paid;
        root.addView(createSummaryBox("Balance", balance, "#FFF3E0"));
    }

    // 🔷 OVERALL TOTAL
    TextView grandTitle = new TextView(context);
    grandTitle.setText("Overall Summary");
    grandTitle.setTextSize(20f);
    grandTitle.setTypeface(null, Typeface.BOLD);
    grandTitle.setPadding(0, 32, 0, 16);
    root.addView(grandTitle);

    root.addView(createSummaryBox("All BC Collected", grandCollected, "#BBDEFB"));
    root.addView(createSummaryBox("All BC Paid", grandPaid, "#C8E6C9"));
    root.addView(createSummaryBox("Final Balance", grandCollected - grandPaid, "#FFE0B2"));

    new AlertDialog.Builder(context)
            .setTitle("BC Summary")
            .setView(scrollView)
            .setPositiveButton("OK", null)
            .show();
}

private View createSummaryBox(String label, double amount, String color) {

    LinearLayout box = new LinearLayout(context);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(24, 24, 24, 24);

    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.parseColor(color));
    bg.setCornerRadius(30);
    box.setBackground(bg);

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    params.setMargins(0, 12, 0, 12);
    box.setLayoutParams(params);

    TextView tvLabel = new TextView(context);
    tvLabel.setText(label);
    tvLabel.setTextSize(14f);
    tvLabel.setTextColor(Color.DKGRAY);

    TextView tvAmount = new TextView(context);
    tvAmount.setText("₹ " + String.format("%,.0f", amount));
    tvAmount.setTextSize(18f);
    tvAmount.setTypeface(null, Typeface.BOLD);
    tvAmount.setTextColor(Color.BLACK);

    box.addView(tvLabel);
    box.addView(tvAmount);

    return box;
}

private void showDeleteBcDialog() {
    if (bcData.isEmpty()) {
        Toast.makeText(context, "No BC available to delete", Toast.LENGTH_SHORT).show();
        return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Select BCs to Delete");

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(32, 16, 32, 16);

    // Create a checkbox for each BC
    List<CheckBox> checkBoxes = new ArrayList<>();
    for (Bc bc : bcData) {
        CheckBox cb = new CheckBox(context);
        cb.setText(bc.name);
        layout.addView(cb);
        checkBoxes.add(cb);
    }

    builder.setView(layout);
    builder.setPositiveButton("Delete", (dialog, which) -> {

        List<Bc> toRemove = new ArrayList<>();
        List<Integer> removedIndexes = new ArrayList<>();

        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isChecked()) {
                toRemove.add(bcData.get(i));
                removedIndexes.add(i);
            }
        }

        if (toRemove.isEmpty()) {
            Toast.makeText(context, "No BC selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Backup for Undo
        List<Bc> backupList = new ArrayList<>(toRemove);
        List<Integer> backupIndexes = new ArrayList<>(removedIndexes);

        // 🔥 DELETE SELECTED BCs
        bcData.removeAll(toRemove);
        saveAllToRoom();

        // Refresh UI
        bcAdapter.clear();
        bcAdapter.add("Select BC");
        for (Bc bc : bcData) {
            bcAdapter.add(bc.name);
        }
        bcAdapter.notifyDataSetChanged();
        spinnerBc.setSelection(0);
        tableContainer.removeAllViews();

        Toast.makeText(context, "Selected BC(s) deleted", Toast.LENGTH_SHORT).show();

        // 🧠 ADD TO UNDO STACK
        pushToUndoStack(
            () -> { // UNDO
                for (int i = 0; i < backupList.size(); i++) {
                    int index = Math.min(backupIndexes.get(i), bcData.size());
                    bcData.add(index, backupList.get(i));
                }

                saveAllToRoom();

                bcAdapter.clear();
                bcAdapter.add("Select BC");
                for (Bc bc : bcData) bcAdapter.add(bc.name);
                bcAdapter.notifyDataSetChanged();
                spinnerBc.setSelection(0);
            },
            () -> { // REDO
                bcData.removeAll(backupList);
                saveAllToRoom();

                bcAdapter.clear();
                bcAdapter.add("Select BC");
                for (Bc bc : bcData) bcAdapter.add(bc.name);
                bcAdapter.notifyDataSetChanged();
                spinnerBc.setSelection(0);
                tableContainer.removeAllViews();
            }
        );

    });

    builder.setNegativeButton("Cancel", null);
    builder.show();
}

private void showDeleteMemberDialog() {

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Delete Members");

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(40, 30, 40, 10);

    // 🔹 BC Selection Spinner
    Spinner bcSpinner = new Spinner(context);
    ArrayAdapter<String> bcAdapter = new ArrayAdapter<>(context,
            android.R.layout.simple_spinner_item);
    bcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    bcAdapter.add("Select BC");
    for (Bc bc : bcData) bcAdapter.add(bc.name);
    bcSpinner.setAdapter(bcAdapter);
    root.addView(bcSpinner);

    // 🔹 Members Multi Select Layout
    LinearLayout membersLayout = new LinearLayout(context);
    membersLayout.setOrientation(LinearLayout.VERTICAL);
    membersLayout.setPadding(0, 20, 0, 10);
    root.addView(membersLayout);

    List<CheckBox> memberCheckBoxes = new ArrayList<>();

    bcSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            membersLayout.removeAllViews();
            memberCheckBoxes.clear();

            if (pos > 0) {
                Bc selectedBc = bcData.get(pos - 1);

                for (String member : selectedBc.members) {
                    CheckBox cb = new CheckBox(context);
                    cb.setText(member);
                    membersLayout.addView(cb);
                    memberCheckBoxes.add(cb);
                }
            }
        }

        @Override public void onNothingSelected(AdapterView<?> parent) {}
    });

    builder.setView(root);

    builder.setPositiveButton("Delete", (d, w) -> {

        int bcPos = bcSpinner.getSelectedItemPosition();
        if (bcPos <= 0) {
            Toast.makeText(context, "Select BC", Toast.LENGTH_SHORT).show();
            return;
        }

        Bc bc = bcData.get(bcPos - 1);

        for (CheckBox cb : memberCheckBoxes) {
            if (cb.isChecked()) {

                String memberName = cb.getText().toString();

                // ================= BACKUP DATA FOR UNDO =================
                DeletedMemberBackup backup = new DeletedMemberBackup();
                backup.bc = bc;
                backup.memberName = memberName;

                // Backup paid installment map
                for (String key : bc.paid.keySet()) {
                    if (key.startsWith(memberName + "_")) {
                        backup.paidMapBackup.put(key, bc.paid.get(key));
                    }
                }

                // Backup paid installment amounts
                for (String key : bc.paidAmount.keySet()) {
                    if (key.startsWith(memberName + "_")) {
                        backup.paidAmountBackup.put(key, bc.paidAmount.get(key));
                    }
                }

                // Backup BC paid amount
                if (bc.paidBcAmount.containsKey(memberName)) {
                    backup.paidBcAmountBackup.put(memberName, bc.paidBcAmount.get(memberName));
                }

                // Backup payment history entries
                for (PaymentEntry pe : bc.payments) {
                    if (pe.member.equals(memberName)) {
                        backup.paymentEntriesBackup.add(pe);
                    }
                }

                deletedMembersBackup.add(backup);

                // ================= DELETE MEMBER DATA =================

                bc.members.remove(memberName);

                bc.paid.keySet().removeIf(k -> k.startsWith(memberName + "_"));
                bc.paidAmount.keySet().removeIf(k -> k.startsWith(memberName + "_"));
                bc.paidBcAmount.remove(memberName);

                bc.payments.removeIf(pe -> pe.member.equals(memberName));
            }
        }

        List<DeletedMemberBackup> actionBackup = new ArrayList<>(deletedMembersBackup);

        pushToHistory(
            // 🔙 UNDO
            () -> {
                for (DeletedMemberBackup backup : actionBackup) {
                    Bc bc1 = backup.bc;
                    String member = backup.memberName;

                    if (!bc1.members.contains(member)) bc1.members.add(member);
                    bc1.paid.putAll(backup.paidMapBackup);
                    bc1.paidAmount.putAll(backup.paidAmountBackup);
                    bc1.paidBcAmount.putAll(backup.paidBcAmountBackup);
                    bc1.payments.addAll(backup.paymentEntriesBackup);
                }
            },
            // 🔁 REDO
            () -> {
                for (DeletedMemberBackup backup : actionBackup) {
                    Bc bc1 = backup.bc;
                    String member = backup.memberName;

                    bc1.members.remove(member);
                    bc1.paid.keySet().removeIf(k -> k.startsWith(member + "_"));
                    bc1.paidAmount.keySet().removeIf(k -> k.startsWith(member + "_"));
                    bc1.paidBcAmount.remove(member);
                    bc1.payments.removeIf(pe -> pe.member.equals(member));
                }
            }
        );

        saveAllToRoom();       // persist changes

        // 🔥 SHOW UNDO SNACKBAR
        Snackbar.make(tableContainer, "Member(s) deleted", Snackbar.LENGTH_LONG).show();
    });

    builder.setNegativeButton("Cancel", null);
    builder.show();
}

private void pushToHistory(Runnable undo, Runnable redo) {
    if (undoStack.size() == MAX_HISTORY) {
        undoStack.remove(0);
    }
    undoStack.add(new HistoryAction(undo, redo));
    redoStack.clear(); // once new action happens, redo history resets
    updateUndoRedoButtons();
}

public void undoLastAction() {
    if (undoStack.isEmpty()) {
        Toast.makeText(context, "Nothing to undo", Toast.LENGTH_SHORT).show();
        return;
    }

    HistoryAction action = undoStack.remove(undoStack.size() - 1);
    action.undoAction.run();
    redoStack.add(action);
    saveAllToRoom();
    showBcListTable();
    updateUndoRedoButtons();
}

public void redoLastAction() {
    if (redoStack.isEmpty()) {
        Toast.makeText(context, "Nothing to redo", Toast.LENGTH_SHORT).show();
        return;
    }

    HistoryAction action = redoStack.remove(redoStack.size() - 1);
    action.redoAction.run();
    undoStack.add(action);
    saveAllToRoom();
    showBcListTable();
    updateUndoRedoButtons();
}

// 🔥 ADD ACTION TO UNDO STACK
private void pushToUndoStack(Runnable undo, Runnable redo) {

    if (undoStack.size() >= MAX_HISTORY) {
        undoStack.remove(0); // remove oldest
    }

    undoStack.add(new HistoryAction(undo, redo));
    redoStack.clear(); // once new action happens, redo history resets

    updateUndoRedoButtons();
}
}
