package com.example.bcapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
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
import android.os.Build;
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
import android.view.WindowManager;
import android.view.Window;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.widget.SwitchCompat;

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
import java.util.Map;

public class BcManager {

//  Backup for Undo Delete Member
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
private final ImageView imgLock;

private Animation shakeAnimation;
private int lockNormalColor;
private int lockWarningColor;
    
private final Spinner spinnerBc, spinnerMember;  
private final EditText editPayDate, editPayAmount;  
private final Button buttonAdd;  
private final LinearLayout tableContainer;  
private SwitchCompat switchVertical;   // ✅ ADD THIS
private View horizontalTableView;
private View verticalTableView;

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
// 🔒 Global Edit Mode (true = can edit, false = view only)
private boolean isEditModeEnabled = false;

// 🔒 Checks whether editing is allowed
private boolean checkEditMode() {
    if (!isEditModeEnabled) {

        Toast.makeText(context, "View Only Mode Enabled", Toast.LENGTH_SHORT).show();

        // 🔒 Shake lock icon with red warning
        if (imgLock != null) {
            imgLock.setVisibility(View.VISIBLE);
            imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
            imgLock.startAnimation(shakeAnimation);

            // Reset color after shake
            imgLock.postDelayed(() -> {
                imgLock.setImageTintList(ColorStateList.valueOf(lockNormalColor));
                imgLock.clearAnimation();
            }, 400);
        }

        return false;
    }
    return true;
}

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
                 ImageView imgLock,   // ✅ ADD THIS
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
    this.switchVertical = activity.findViewById(R.id.switchVertical);
    this.btnUndo = btnUndo;
    this.btnRedo = btnRedo;
    this.imgLock = imgLock;
    this.bcData = bcData;  
    this.isoFormat = isoFormat;  
    this.displayFormat = displayFormat;  

    this.db = AppDatabase.getDatabase(context);  
    this.bcDao = db.bcDao();  

    // 🔒 Lock system setup
    shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake);
    lockNormalColor = ContextCompat.getColor(context, R.color.lock_normal);
    lockWarningColor = ContextCompat.getColor(context, R.color.lock_warning);

    // Show correct lock state at startup
    updateLockIcon();
}  

private void updateLockIcon() {
    if (imgLock == null) return;

    if (isEditModeEnabled) {
        imgLock.setVisibility(View.GONE);
    } else {
        imgLock.setVisibility(View.VISIBLE);
        imgLock.setImageTintList(ColorStateList.valueOf(lockNormalColor));
        imgLock.clearAnimation();
    }
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
            bc.isWeekly = e.isWeekly;   // ✅ ADD THIS

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
            if (e.receiveAmounts != null) {
                bc.receiveAmounts = e.receiveAmounts;
            }
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
            e.isWeekly = bc.isWeekly;   // ✅ ADD THIS
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
        popup.getMenu().add(0, 5, 4, "Delete");
        popup.getMenu().add(0, 6, 5, "Add Member");
        popup.getMenu().add(0, 7, 6, "Edit");
        popup.getMenu().add(0, 8, 7, "Edit Mode");

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
    showDeleteOptionsDialog(); // NEW
    return true;
    }
    else if (item.getItemId() == 6) {
    showAddMemberDialog();
    return true;
    }
    else if (item.getItemId() == 7) {
    showEditOptionsDialog();
    return true;
    }
    else if (item.getItemId() == 8) {
    showEditModeToggleDialog();
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

    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }

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
    // ✅ SELF MODE VIEWS
    CheckBox checkSelf = dialogView.findViewById(R.id.checkSelf);
    Spinner spinnerSelfType = dialogView.findViewById(R.id.spinnerSelfType);

    
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

    // ✅ Self Type Spinner
    ArrayAdapter<String> selfAdapter = new ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            Arrays.asList("Select Type", "Monthly", "Weekly")
    );
    selfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerSelfType.setAdapter(selfAdapter);

    // ✅ ADD SELF CHECK LOGIC HERE
    checkSelf.setOnCheckedChangeListener((buttonView, isChecked) -> {

        if (isChecked) {
            spinnerSelfType.setVisibility(View.VISIBLE);
            layoutMembers.setVisibility(View.GONE);
        } else {
            spinnerSelfType.setVisibility(View.GONE);
            layoutMembers.setVisibility(View.VISIBLE);
            editMonths.setText("");
        }
    });

    spinnerSelfType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

        boolean firstLoad = true;

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            if (!checkSelf.isChecked()) return;

            if (firstLoad) {
                firstLoad = false;
                return;
            }

            if (position == 0) return; // "Select Type"

            String type = parent.getItemAtPosition(position).toString();

            showDurationDialog(type, editMonths);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    });
    
    editMonths.addTextChangedListener(new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        @Override
        public void afterTextChanged(Editable s) {

            if (!checkSelf.isChecked()) {
                createMemberInputs(editMonths, layoutMembers);
            }

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

        if (checkSelf.isChecked()) {

            String type = spinnerSelfType.getSelectedItem().toString();

            if (type.equalsIgnoreCase("Weekly")) {
                bc.isWeekly = true;   // Make sure this field exists in Bc class
            } else {
                bc.isWeekly = false;
            }
        }

        // Members
        // ✅ Members
        if (checkSelf.isChecked()) {

            bc.members.add("Self");

        } else {

            for (int i = 0; i < layoutMembers.getChildCount(); i++) {
                View v1 = layoutMembers.getChildAt(i);
                if (v1 instanceof EditText) {
                    String m = ((EditText) v1).getText().toString().trim();
                    if (!m.isEmpty()) bc.members.add(m);
                }
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
    tableContainer.removeAllViews();
    if (index <= 0 || index > bcData.size()) {  
        memberAdapter.notifyDataSetChanged();  
        return;  
    }  
    Bc bc = bcData.get(index -1);  
    memberAdapter.addAll(bc.members);  
    memberAdapter.notifyDataSetChanged();  
    spinnerMember.setSelection(0);  
    try {
        renderMainTable(bc);
    } catch (Exception e) {
        showCrashDialog(e);
    }  
}  

private void showDurationDialog(String type, EditText editMonths) {

    EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_NUMBER);

    if (type.equalsIgnoreCase("Weekly")) {
        input.setHint("Enter number of Weeks");
    } else {
        input.setHint("Enter number of Months");
    }

    new AlertDialog.Builder(context)
            .setTitle("Duration")
            .setView(input)
            .setPositiveButton("OK", (dialog, which) -> {

                int value = safeParseInt(input.getText().toString());
                if (value <= 0) value = 1;

                editMonths.setText(String.valueOf(value));

            })
            .setNegativeButton("Cancel", null)
            .show();
}

private void showBcListTable() {
    tableContainer.removeAllViews();

    for (Bc bc : bcData) {

        // 🔷 ===== BC TITLE ROW =====
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(16, 16, 16, 8);

        TextView title = new TextView(context);
        title.setText(bc.name);
        title.setTextSize(18f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setTextColor(Color.parseColor("#0D47A1"));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

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

        // 🔷 ===== CARD WRAPPER =====
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

        // 🔷 ===== HORIZONTAL SCROLL =====
        HorizontalScrollView hScroll = new HorizontalScrollView(context);
        hScroll.setHorizontalScrollBarEnabled(true);
        hScroll.setFillViewport(false);

        TableLayout table = new TableLayout(context);
        table.setStretchAllColumns(false);
        table.setShrinkAllColumns(false);
        table.setPadding(8, 8, 8, 8);
        table.setBackgroundColor(Color.parseColor("#ECEFF1"));

        // ========== HEADER ==========
        TableRow header = new TableRow(context);
        header.setBackgroundResource(R.drawable.table_header_border);
        addCompactHeaderCell(header, "Sr");
        addCompactHeaderCell(header, "Date");
        addCompactHeaderCell(header, "Installment\nAmount");
        addCompactHeaderCell(header, "Receive\nAmount");

        if (bc.afterTaken) {
            addCompactHeaderCell(header, "After\nTaken");
        }

        table.addView(header);

        // ========== ROWS ==========
        for (int i = 0; i < bc.months; i++) {

            TableRow row = new TableRow(context);
            row.setBackgroundColor(i % 2 == 0 ? Color.parseColor("#F8FAFC") : Color.WHITE);

            addCompactCell(row, String.valueOf(i + 1));

            Calendar cal = parseIsoDate(bc.startDateIso);

            if (cal != null) {
                if (bc.isWeekly) {
                    cal.add(Calendar.DATE, i * 7);
                } else {
                    cal.add(Calendar.MONTH, i);
                }
            }

            String dateStr = cal != null ? displayFormat.format(cal.getTime()) : "-";
            addCompactCell(row, dateStr);

            double amount = 0.0;
            if (!bc.amounts.isEmpty()) {
                if (bc.amounts.size() == 1) amount = bc.amounts.get(0);
                else if (bc.amounts.size() > i) amount = bc.amounts.get(i);
            }
            addCompactCell(row, "₹" + String.format("%.0f", amount));

            double receiveAmount = 0.0;
            if (!bc.receiveAmounts.isEmpty()) {
                if (bc.isReceiveAmountFixed) receiveAmount = bc.receiveAmounts.get(0);
                else if (bc.receiveAmounts.size() > i) receiveAmount = bc.receiveAmounts.get(i);
            }
            addCompactCell(row, "₹" + String.format("%.0f", receiveAmount));

            if (bc.afterTaken) {
                addCompactCell(row, "₹" + String.format("%.0f", bc.afterTakenAmount));
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

    // 🔷 Title + Toggle Layout
    LinearLayout titleLayout = new LinearLayout(context);
    titleLayout.setOrientation(LinearLayout.HORIZONTAL);
    titleLayout.setGravity(Gravity.CENTER_VERTICAL);
    titleLayout.setPadding(16, 8, 16, 8);

    TextView title = new TextView(context);
    title.setText(bc.name);
    title.setTextColor(Color.BLACK);
    title.setTextSize(16f);
    title.setTypeface(null, Typeface.BOLD);
    title.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

    titleLayout.addView(title);

    // ✅ Show toggle only for Self Weekly
    if (bc.members != null &&
        bc.members.size() == 1 &&
        "Self".equalsIgnoreCase(bc.members.get(0)) &&
        bc.isWeekly)

        if (switchVertical != null) {

            switchVertical.setVisibility(View.VISIBLE);
            switchVertical.setChecked(false);

            switchVertical.setOnCheckedChangeListener((btn, isChecked) -> {

                if (isChecked) {

                    try {
                        renderVerticalSelfWeeklyTable(bc);
                    } catch (Exception e) {
                        showCrashDialog(e);
                    }  

                    if (verticalTableView != null && horizontalTableView != null)
                        animateSwitch(verticalTableView, horizontalTableView);

                } else {

                    if (verticalTableView != null && horizontalTableView != null)
                        animateSwitch(horizontalTableView, verticalTableView);
                }
            });

            if (switchVertical.getParent() != null) {
                ((ViewGroup) switchVertical.getParent()).removeView(switchVertical);
            }

            titleLayout.addView(switchVertical);
    
    } else {
        if (switchVertical != null)
            switchVertical.setVisibility(View.GONE);
    }

    tableContainer.addView(titleLayout);

    TableLayout table = new TableLayout(context);
    table.setPadding(8, 8, 8, 8);
    table.setBackgroundColor(Color.parseColor("#ECEFF1")); // soft outer background
    table.setStretchAllColumns(false);

    TableRow header = new TableRow(context);
    header.setElevation(6f); // floating header feel
    addCell(header, "Sr", true);
    addCell(header, "Date", true);
    addCell(header, "Amount", true);
    addCell(header, "Member", true);

    Calendar headerCal = parseIsoDate(bc.startDateIso);

    for (int i = 0; i < bc.months; i++) {

        String monthName = "M" + (i + 1);

        if (headerCal != null) {

            monthName = displayFormat.format(headerCal.getTime());

            if (bc.isWeekly) {
                headerCal.add(Calendar.DATE, 7);
            } else {
                headerCal.add(Calendar.MONTH, 1);
            }
        }

        addCell(header, monthName, true);
    }
    addCell(header, "Total", true);
    addCell(header, "Paid BC", true);
    table.addView(header);

    if (bc.members == null) return;

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

        if (cal != null) {
            if (bc.isWeekly) {
                cal.add(Calendar.DATE, r * 7);
            } else {
                cal.add(Calendar.MONTH, r);
            }
        }

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

        int currentIndex = -1;

        if (startCal != null) {

            if (bc.isWeekly) {

                long diffMillis =
                        todayCal.getTimeInMillis() - startCal.getTimeInMillis();
                long diffDays = diffMillis / (1000 * 60 * 60 * 24);

                currentIndex = (int) (diffDays / 7);

            } else {

                currentIndex =
                        (todayCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
                        (todayCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH));
            }
        }

        // Apply highlight only for valid month
        // default
        memberCell.setTextColor(Color.parseColor("#000000"));
        memberCell.setTypeface(null, Typeface.BOLD);
        memberCell.setBackgroundResource(R.drawable.table_cell_border);

        // 🔴 MONTHLY: highlight if ANY past month unpaid
        if (!bc.isWeekly && currentIndex > 0) {

            boolean hasPastUnpaid = false;

            int checkUpto = Math.min(currentIndex, bc.months);

            for (int m = 0; m < checkUpto; m++) {

                String key = bc.getPaidKey(member, m);
                double paidAmt = bc.paidAmount.getOrDefault(key, 0.0);

                double expectedAmt = bc.amounts.size() > m
                        ? bc.amounts.get(m)
                        : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);

                if (paidAmt < expectedAmt) {
                    hasPastUnpaid = true;
                    break;
                }
            }

            if (hasPastUnpaid) {
                memberCell.setTextColor(Color.parseColor("#D32F2F"));
                memberCell.setTypeface(null, Typeface.BOLD);
                memberCell.setBackgroundResource(R.drawable.table_cell_border_overdue);
            }
        }

        // 🔴 WEEKLY SELF: highlight if ANY past week unpaid till current week
        if (bc.isWeekly && member.equalsIgnoreCase("Self") && currentIndex >= 0) {

            boolean hasPastUnpaid = false;

            int checkUpto = Math.min(currentIndex + 1, bc.months);

            for (int w = 0; w < checkUpto; w++) {

                String key = bc.getPaidKey(member, w);
                double paidAmt = bc.paidAmount.getOrDefault(key, 0.0);

                double expectedAmt = bc.amounts.size() > w
                        ? bc.amounts.get(w)
                        : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);

                if (paidAmt < expectedAmt) {
                    hasPastUnpaid = true;
                    break;
                }
            }

            if (hasPastUnpaid) {
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

    horizontalTableView = card;   // ⭐ SAVE VIEW
    tableContainer.addView(horizontalTableView);
    }

private void renderVerticalSelfWeeklyTable(Bc bc) {
    
    if (verticalTableView != null) {
        verticalTableView.setVisibility(View.VISIBLE);
        horizontalTableView.setVisibility(View.GONE);
        return;
    }

    // ===== TABLE =====
    TableLayout table = new TableLayout(context);
    table.setPadding(8,8,8,8);
    table.setBackgroundColor(Color.parseColor("#ECEFF1"));
    table.setStretchAllColumns(false);

    // ===== HEADER =====
    TableRow header = new TableRow(context);
    header.setElevation(6f);

    addCell(header,"Week",true);
    addCell(header,"Date",true);
    addCell(header,"Amount",true);
    addCell(header,"Status",true);

    table.addView(header);

    Calendar startCal = parseIsoDate(bc.startDateIso);
    Calendar todayCal = Calendar.getInstance();

    int currentIndex = -1;

    if(startCal!=null){
        long diffMillis =
                todayCal.getTimeInMillis()-startCal.getTimeInMillis();
        long diffDays = diffMillis/(1000*60*60*24);
        currentIndex=(int)(diffDays/7);
    }

    // ===== ROWS =====
    for(int i=0;i<bc.months;i++){

        TableRow row = new TableRow(context);
        row.setBackgroundColor(
                i%2==0 ?
                        Color.parseColor("#F7F9FC"):
                        Color.WHITE
        );

        addCell(row,String.valueOf(i+1),false);

        Calendar cal=parseIsoDate(bc.startDateIso);
        if(cal!=null) cal.add(Calendar.DATE,i*7);

        String dateStr =
                cal!=null ? displayFormat.format(cal.getTime()) : "-";

        addCell(row,dateStr,false);

        double expected =
                bc.amounts.size()>i ?
                        bc.amounts.get(i):
                        (!bc.amounts.isEmpty()?bc.amounts.get(0):0.0);

        addCell(row,"₹"+String.format("%.0f",expected),false);

        // ===== STATUS CELL (SAME STYLE AS MAIN TABLE) =====
        LinearLayout cellContainer = new LinearLayout(context);
        cellContainer.setOrientation(LinearLayout.VERTICAL);
        cellContainer.setGravity(Gravity.CENTER);
        cellContainer.setPadding(8,10,8,10);
        cellContainer.setMinimumHeight(84);
        cellContainer.setElevation(2f);
        cellContainer.setBackgroundResource(R.drawable.table_cell_border);

        String key = bc.getPaidKey("Self",i);
        double paidAmt = bc.paidAmount.getOrDefault(key,0.0);

        boolean hasAnyPayment = paidAmt>0;

        if(hasAnyPayment){

            if(paidAmt>=expected){
                cellContainer.setBackgroundResource(
                        R.drawable.table_cell_border_paid);
            }else{
                cellContainer.setBackgroundResource(
                        R.drawable.table_cell_border_partialy_paid);
            }

            // ✅ tick
            TextView tick=new TextView(context);
            tick.setText("✅");
            tick.setTextSize(18f);
            tick.setTextColor(Color.parseColor("#2E7D32"));
            tick.setGravity(Gravity.CENTER);
            cellContainer.addView(tick);

            // amount badge
            TextView badge=new TextView(context);
            badge.setText("₹"+String.format("%.0f",paidAmt));
            badge.setTextSize(8f);
            badge.setTypeface(null,Typeface.BOLD);
            badge.setPadding(10,4,10,4);

            if(paidAmt<expected){
                badge.setBackgroundResource(
                        R.drawable.amount_badge_red);
                badge.setTextColor(Color.parseColor("#D32F2F"));
            }else{
                badge.setBackgroundResource(
                        R.drawable.amount_badge_green);
                badge.setTextColor(Color.WHITE);
            }

            LinearLayout.LayoutParams badgeLp =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeLp.topMargin=4;
            badge.setLayoutParams(badgeLp);

            cellContainer.addView(badge);

        }else{

            // ☐ unpaid
            TextView box=new TextView(context);
            box.setText("☐");
            box.setTextSize(18f);
            box.setGravity(Gravity.CENTER);
            cellContainer.addView(box);
        }

        // 🔴 overdue highlight (same logic)
        if(i<=currentIndex && paidAmt<expected){
            cellContainer.setBackgroundResource(
                    R.drawable.table_cell_border_overdue);
        }

        TableRow.LayoutParams lp =
                new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.MATCH_PARENT);
        lp.setMargins(1,1,1,1);
        cellContainer.setLayoutParams(lp);

        row.addView(cellContainer);

        table.addView(row);
    }

    // ===== CARD WRAPPER (SAME AS MAIN TABLE) =====
    CardView card = new CardView(context);
    card.setRadius(28f);
    card.setCardElevation(14f);
    card.setUseCompatPadding(true);
    card.setCardBackgroundColor(Color.WHITE);

    LinearLayout.LayoutParams cardParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
    cardParams.setMargins(16,12,16,24);
    card.setLayoutParams(cardParams);

    HorizontalScrollView scrollWrap =
            new HorizontalScrollView(context);
    scrollWrap.addView(table);

    card.addView(scrollWrap);
    verticalTableView = card;

    // safety (prevents future crash)
    if (verticalTableView.getParent() != null) {
        ((ViewGroup) verticalTableView.getParent())
                .removeView(verticalTableView);
    }

    tableContainer.addView(verticalTableView);

    if (horizontalTableView != null)
        horizontalTableView.setVisibility(View.GONE);
}

/* ---------- Installments ---------- */  

private void markInstallment() {

    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }
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

    String member = bc.members.get(memberIndex - 1);

    int monthIndex;

    if (bc.isWeekly && member.equalsIgnoreCase("Self")) {

        long diffMillis = paid.getTimeInMillis() - start.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);

        monthIndex = (int) (diffDays / 7);

    } else {

        monthIndex =
                (paid.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 +
                (paid.get(Calendar.MONTH) - start.get(Calendar.MONTH));
    }

    if (monthIndex < 0 || monthIndex >= bc.months) {
        Toast.makeText(context, "Selected date is outside BC duration", Toast.LENGTH_SHORT).show();
        return;
    }

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
    activity.runOnUiThread(() -> {
    try {
        renderMainTable(bc);
    } catch (Exception e) {
        showCrashDialog(e);
    }
});

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

private void recalculatePaymentsFromEntries(Bc bc) {

    // Safety
    if (bc.payments == null) bc.payments = new ArrayList<>();
    if (bc.paymentEntries == null) bc.paymentEntries = new HashMap<>();
    if (bc.paidAmount == null) bc.paidAmount = new HashMap<>();
    if (bc.paid == null) bc.paid = new HashMap<>();

    // 1️⃣ Rebuild grouped entries
    rebuildPaymentEntries(bc);

    // 2️⃣ Clear totals
    bc.paidAmount.clear();
    bc.paid.clear();

    // 3️⃣ Recalculate totals & tick status
    for (PaymentEntry pe : bc.payments) {

        String key = bc.getPaidKey(pe.member, pe.monthIndex);

        double total = bc.paidAmount.getOrDefault(key, 0.0) + pe.amount;
        bc.paidAmount.put(key, total);

        double expected =
                bc.amounts.size() > pe.monthIndex
                        ? bc.amounts.get(pe.monthIndex)
                        : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);

        bc.paid.put(key, total >= expected);
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

private void showDeleteOptionsDialog() {

    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }

    String[] options = {"Delete BC", "Delete A Member", "Delete Entry"};

    new AlertDialog.Builder(context)
            .setTitle("Delete Options")
            .setItems(options, (dialog, which) -> {

                switch (which) {
                    case 0:
                        showDeleteBcDialog();        // existing
                        break;

                    case 1:
                        showDeleteMemberDialog();    // existing
                        break;

                    case 2:
                        showSelectBcForEntryDelete(); // 🔥 NEW (Entry delete flow)
                        break;
                }

            })
            .show();
}

private void showAddMemberDialog() {

    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }

    if (bcData.isEmpty()) {
        Toast.makeText(context, "No BC available", Toast.LENGTH_SHORT).show();
        return;
    }

    // 🔹 BC Name List
    String[] bcNames = new String[bcData.size()];
    for (int i = 0; i < bcData.size(); i++) {
        bcNames[i] = bcData.get(i).name;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Select BC");

    builder.setItems(bcNames, (dialog, which) -> {

        Bc selectedBc = bcData.get(which);

        // 🔴 LIMIT CHECK
        if (selectedBc.members.size() >= selectedBc.months) {
            Toast.makeText(context,
                    "This BC already has maximum members (" + selectedBc.months + ")",
                    Toast.LENGTH_LONG).show();
            return;
        }

        showMemberNameInputDialog(selectedBc);
    });

    builder.show();
}

private void showMemberNameInputDialog(Bc bc) {

    EditText input = new EditText(context);
    input.setHint("Enter Member Name");
    input.setPadding(40, 20, 40, 20);

    new AlertDialog.Builder(context)
            .setTitle("Add Member to " + bc.name)
            .setView(input)
            .setPositiveButton("Add", (dialog, which) -> {

                String newMember = input.getText().toString().trim();

                if (newMember.isEmpty()) {
                    Toast.makeText(context, "Member name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 🔁 Prevent duplicate names
                if (bc.members.contains(newMember)) {
                    Toast.makeText(context, "Member already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                addMemberToBc(bc, newMember);

            })
            .setNegativeButton("Cancel", null)
            .show();
}

private void addMemberToBc(Bc bc, String memberName) {

    int oldSize = bc.members.size();

    bc.members.add(memberName);

    // 🧠 UNDO / REDO SUPPORT
    pushToUndoStack(
            () -> { // UNDO
                bc.members.remove(memberName);
                saveAllToRoom();
                renderMainTable(bc);
            },
            () -> { // REDO
                bc.members.add(memberName);
                saveAllToRoom();
                renderMainTable(bc);
            }
    );

    saveAllToRoom();
    try {
        renderMainTable(bc);
    } catch (Exception e) {
        showCrashDialog(e);
    }

    Toast.makeText(context,
            "Member added (" + (oldSize + 1) + "/" + bc.months + ")",
            Toast.LENGTH_SHORT).show();
}

private void showEditOptionsDialog() {
    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }
    
    String[] options = {"Edit Member", "Edit Paid BC"};

    new AlertDialog.Builder(context)
            .setTitle("Edit Options")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showSelectBcForMemberEdit();
                } else {
                    showSelectBcForPaidEdit();
                }
            })
            .show();
}

private void showSelectBcForMemberEdit() {

    if (bcData.isEmpty()) {
        Toast.makeText(context, "No BC available", Toast.LENGTH_SHORT).show();
        return;
    }

    String[] bcNames = new String[bcData.size()];
    for (int i = 0; i < bcData.size(); i++) {
        bcNames[i] = bcData.get(i).name;
    }

    new AlertDialog.Builder(context)
            .setTitle("Select BC")
            .setItems(bcNames, (d, which) -> showSelectMemberToEdit(bcData.get(which)))
            .show();
}

private void showSelectMemberToEdit(Bc bc) {

    String[] members = bc.members.toArray(new String[0]);

    new AlertDialog.Builder(context)
            .setTitle("Select Member")
            .setItems(members, (d, which) -> showUpdateMemberDialog(bc, members[which]))
            .show();
}

private void showUpdateMemberDialog(Bc bc, String oldName) {

    final EditText input = new EditText(context);
    input.setText(oldName);
    input.setPadding(40, 20, 40, 20);

    AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("Update Member Name")
            .setView(input)
            .setPositiveButton("Update", null) // we override click to control closing
            .setNegativeButton("Cancel", null)
            .create();

    dialog.show();

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

        String newName = input.getText().toString().trim();

        if (newName.isEmpty()) {
            input.setError("Name cannot be empty");
            return;
        }

        if (newName.equals(oldName)) {
            input.setError("No changes made");
            return;
        }

        // 🚫 Prevent duplicate member names
        if (bc.members.contains(newName)) {
            input.setError("Member already exists");
            return;
        }

        int index = bc.members.indexOf(oldName);
        if (index == -1) return;

        // 🔹 Save old payment data for UNDO
        Boolean oldPaidStatus = bc.paid.get(oldName);
        Double oldInstallmentAmount = bc.paidAmount.get(oldName);
        Double oldPaidBcAmount = bc.paidBcAmount.get(oldName);

        // 🧠 UNDO / REDO SUPPORT
        pushToUndoStack(
                () -> { // UNDO
                    bc.members.set(index, oldName);
                    restoreMapsAfterRename(bc, oldName, newName,
                            oldPaidStatus, oldInstallmentAmount, oldPaidBcAmount);
                    saveAllToRoom();
                    renderMainTable(bc);
                },
                () -> { // REDO
                    bc.members.set(index, newName);
                    moveMapsAfterRename(bc, oldName, newName);
                    saveAllToRoom();
                    renderMainTable(bc);
                }
        );

        // ✅ APPLY CHANGE
        bc.members.set(index, newName);

        // ✅ TRANSFER all payment history to new name
        moveMapsAfterRename(bc, oldName, newName);

        saveAllToRoom();
        try {
            renderMainTable(bc);
        } catch (Exception e) {
            showCrashDialog(e);
        }

        Toast.makeText(context, "Member updated successfully", Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    });
}

private void showSelectBcForPaidEdit() {

    String[] bcNames = new String[bcData.size()];
    for (int i = 0; i < bcData.size(); i++) {
        bcNames[i] = bcData.get(i).name;
    }

    new AlertDialog.Builder(context)
            .setTitle("Select BC")
            .setItems(bcNames, (d, which) -> showSelectMemberForPaidEdit(bcData.get(which)))
            .show();
}

private void showSelectMemberForPaidEdit(Bc bc) {

    String[] members = bc.members.toArray(new String[0]);

    new AlertDialog.Builder(context)
            .setTitle("Select Member")
            .setItems(members, (d, which) -> showUpdatePaidAmountDialog(bc, members[which]))
            .show();
}

private void showUpdatePaidAmountDialog(Bc bc, String memberName) {

    final EditText input = new EditText(context);
    input.setHint("Enter Paid BC Amount");
    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    input.setPadding(40, 20, 40, 20);

    Double oldAmount = bc.paidBcAmount.get(memberName);
    if (oldAmount != null) {
        input.setText(String.valueOf(oldAmount));
    }

    new AlertDialog.Builder(context)
            .setTitle("Update Paid BC Amount")
            .setView(input)
            .setPositiveButton("Update", (d, w) -> {

                double newAmount = 0.0;
                try {
                    newAmount = Double.parseDouble(input.getText().toString().trim());
                } catch (Exception ignored) {}

                double previousAmount = bc.paidBcAmount.getOrDefault(memberName, 0.0);

                double finalNewAmount = newAmount;

                // 🧠 UNDO / REDO SUPPORT
                pushToUndoStack(
                        () -> { // UNDO
                            if (previousAmount == 0.0)
                                bc.paidBcAmount.remove(memberName);
                            else
                                bc.paidBcAmount.put(memberName, previousAmount);

                            saveAllToRoom();
                            renderMainTable(bc);
                        },
                        () -> { // REDO
                            bc.paidBcAmount.put(memberName, finalNewAmount);
                            saveAllToRoom();
                            renderMainTable(bc);
                        }
                );

                // ✅ APPLY CHANGE
                bc.paidBcAmount.put(memberName, newAmount);

                saveAllToRoom();
                try {
                    renderMainTable(bc);
               } catch (Exception e) {
                   showCrashDialog(e);
               }

                Toast.makeText(context, "Paid BC amount updated", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
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
        tv.setBackgroundResource(R.drawable.table_header_border); // Premium blue header
    } else {

        tv.setTextSize(14f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setTextColor(Color.parseColor("#000000")); // Dark premium text

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
        tv.setBackgroundResource(R.drawable.table_header_border); // Same premium header
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

private void addCompactHeaderCell(TableRow row, String text) {
    TextView tv = new TextView(context);
    tv.setText(text);
    tv.setTextSize(13f);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setTextColor(Color.WHITE);
    tv.setGravity(Gravity.CENTER);
    tv.setPadding(16, 12, 16, 12);

    TableRow.LayoutParams params = new TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
    );
    tv.setLayoutParams(params);
    row.addView(tv);
}

private void addCompactCell(TableRow row, String text) {
    TextView tv = new TextView(context);
    tv.setText(text);
    tv.setTextSize(13f);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setTextColor(Color.parseColor("#000000"));
    tv.setGravity(Gravity.CENTER);
    tv.setPadding(16, 10, 16, 10);
    tv.setBackgroundResource(R.drawable.table_cell_border);

    TableRow.LayoutParams lp = new TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
    );
    lp.setMargins(2, 2, 2, 2); // Softer spacing
    tv.setLayoutParams(lp);
    
    row.addView(tv);
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
    try {

    AlertDialog dialog =
        new AlertDialog.Builder(context, R.style.BlurDialogTheme)
                .create();

    CardView glassCard = new CardView(context);
    glassCard.setRadius(40f);
    glassCard.setCardElevation(25f);
    glassCard.setUseCompatPadding(true);
    glassCard.setBackgroundResource(R.drawable.bg_real_glass);
    glassCard.setCardBackgroundColor(Color.parseColor("#CCFFFFFF")); // frosted white
    glassCard.setAlpha(0.96f);
    glassCard.setPreventCornerOverlap(false);
    
    LinearLayout.LayoutParams cardParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

    cardParams.setMargins(40,40,40,40);
    glassCard.setLayoutParams(cardParams);

    ScrollView scrollView = new ScrollView(context);
    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(32, 24, 32, 24);
    scrollView.addView(root);

    double totalPaid = 0.0;

    LinearLayout header = new LinearLayout(context);
    header.setOrientation(LinearLayout.VERTICAL);
    header.setPadding(0,0,0,20);

    TextView title = new TextView(context);
    title.setText("Payment Breakdown");
    title.setTextSize(24f);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(Color.parseColor("#66023C"));
    title.setBackgroundResource(R.drawable.bg_glass_dialog);

    TextView subtitle = new TextView(context);
    subtitle.setText(member);
    subtitle.setTypeface(null, Typeface.BOLD);
    subtitle.setGravity(Gravity.CENTER);
    subtitle.setTextSize(16f);
    subtitle.setTextColor(Color.parseColor("#FA003F"));

    header.addView(title);
    header.addView(subtitle);

    root.addView(header);

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
            tvMonth.setTypeface(null, Typeface.BOLD);
            tvMonth.setTextColor(Color.parseColor("#222222"));
            tvMonth.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView tvAmt = new TextView(context);
            tvAmt.setText("₹" + String.format("%.0f", e.amount));
            tvAmt.setTypeface(null, Typeface.BOLD);
            tvAmt.setTextColor(Color.parseColor("#222222"));
            tvAmt.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView tvDate = new TextView(context);
            tvDate.setText(e.paidDateIso);
            tvDate.setTextColor(Color.parseColor("#222222"));
            tvDate.setTypeface(null, Typeface.BOLD);
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

    LinearLayout summary = new LinearLayout(context);
    summary.setOrientation(LinearLayout.VERTICAL);
    summary.setPadding(24,20,24,20);
    summary.setBackgroundResource(R.drawable.bg_glass_dialog);
    summary.setElevation(8f);

    LinearLayout.LayoutParams summaryParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

    summaryParams.setMargins(0,16,0,0);
    summary.setLayoutParams(summaryParams);

    root.addView(summary);

    // Expected total (Past unpaid + current installment only)
    double expectedTotal = 0.0;

    int currentIndex = bc.getCurrentInstallmentIndex();

    if (currentIndex < 0) {
        currentIndex = 0;
    }

    int checkUpto = Math.min(currentIndex + 1, bc.months);

    for (int i = 0; i < checkUpto; i++) {

        double installmentAmt = bc.amounts.size() > i
                ? bc.amounts.get(i)
                : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);

        // 🔥 Add full installment amount (paid or unpaid)
        expectedTotal += installmentAmt;
    }

    TextView tvExpected = new TextView(context);
    tvExpected.setText("Expected Total : ₹" + String.format("%.0f", expectedTotal));
    tvExpected.setTextColor(Color.BLACK);
    tvExpected.setTypeface(null, Typeface.BOLD);
    summary.addView(tvExpected);

    // Total paid
    TextView tvPaid = new TextView(context);
    tvPaid.setText("Total Paid : ₹" + String.format("%.0f", totalPaid));
    tvPaid.setTextColor(Color.parseColor("#66023C"));
    tvPaid.setTypeface(null, Typeface.BOLD);
    summary.addView(tvPaid);

    // Balance
    double balance = expectedTotal - totalPaid;
    TextView tvBalance = new TextView(context);
    tvBalance.setText("Balance : ₹" + String.format("%.0f", balance));
    tvBalance.setTypeface(null, Typeface.BOLD);
    tvBalance.setTextColor(balance > 0
            ? Color.parseColor("#D32F2F")
            : Color.parseColor("#1B5E20"));
    summary.addView(tvBalance);

    glassCard.addView(scrollView);  
    dialog.setView(glassCard);  
    dialog.show();  
    dialog.getWindow().setDimAmount(0.15f);  

    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);  
    Window window = dialog.getWindow();  

    if (window != null) {  

        // Transparent dialog background  
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));  
  
        // Darken background (important for readability)  
        window.setDimAmount(0.55f);  

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {  

            // REAL BACKGROUND BLUR (Android 12+)  
            window.setBackgroundBlurRadius(120);  

            window.addFlags(  
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND);  
        }  
    }  
  
    if (android.os.Build.VERSION.SDK_INT >= 31) {  
        dialog.getWindow().setBackgroundBlurRadius(90);  
    }  

    glassCard.startAnimation(  
        AnimationUtils.loadAnimation(context, R.anim.dialog_scale_in));  
    } catch (Exception e) {  
        showCrashDialog(e);  
    }
    
}

private void showPaidBcDialog() {
    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }

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
        try {
            renderMainTable(bc);
        } catch (Exception e) {
            showCrashDialog(e);
        }

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

        if (bc.isWeekly) {
        continue;
        }

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

    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }
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

    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }
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

private void showSelectBcForEntryDelete() {

    if (!checkEditMode()) {
    // 🔴 Shake + red lock warning
    if (imgLock != null) {
        imgLock.setImageTintList(ColorStateList.valueOf(lockWarningColor));
        imgLock.startAnimation(shakeAnimation);
    }
    return;
    }

    String[] bcNames = new String[bcData.size()];
    for (int i = 0; i < bcData.size(); i++) {
        bcNames[i] = bcData.get(i).name;
    }

    new AlertDialog.Builder(context)
            .setTitle("Select BC")
            .setItems(bcNames, (d, which) -> showSelectMemberForEntryDelete(bcData.get(which)))
            .show();
}

private void showSelectMemberForEntryDelete(Bc bc) {

    if (bc.members.isEmpty()) {
        Toast.makeText(context, "No members in this BC", Toast.LENGTH_SHORT).show();
        return;
    }

    String[] members = bc.members.toArray(new String[0]);

    new AlertDialog.Builder(context)
            .setTitle("Select Member")
            .setItems(members, (d, which) -> showEntryMultiDeleteDialog(bc, members[which]))
            .show();
}

private void showEntryMultiDeleteDialog(Bc bc, String member) {

    List<PaymentEntry> memberEntries = new ArrayList<>();

    for (PaymentEntry pe : bc.payments) {
        if (pe.member.equals(member)) {
            memberEntries.add(pe);
        }
    }

    if (memberEntries.isEmpty()) {
        Toast.makeText(context, "No entries found", Toast.LENGTH_SHORT).show();
        return;
    }

    String[] entryLabels = new String[memberEntries.size()];
    boolean[] checked = new boolean[memberEntries.size()];

    for (int i = 0; i < memberEntries.size(); i++) {
        PaymentEntry pe = memberEntries.get(i);
        entryLabels[i] = pe.date + "  -  ₹" + pe.amount;
    }

    new AlertDialog.Builder(context)
            .setTitle("Select Entries to Delete")
            .setMultiChoiceItems(entryLabels, checked, (d, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("Delete", (d, w) -> {

                List<PaymentEntry> toRemove = new ArrayList<>();
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) toRemove.add(memberEntries.get(i));
                }

                if (toRemove.isEmpty()) {
                    Toast.makeText(context, "Nothing selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                deleteSelectedEntries(bc, member, toRemove);
            })
            .setNegativeButton("Cancel", null)
            .show();
}

private void deleteSelectedEntries(Bc bc, String member, List<PaymentEntry> entriesToRemove) {

    // Backup for UNDO
    List<PaymentEntry> backup = new ArrayList<>(entriesToRemove);

    pushToUndoStack(
            () -> { // UNDO
                bc.payments.addAll(backup);
                recalculateAfterEntryChange(bc, member);
            },
            () -> { // REDO
                bc.payments.removeAll(backup);
                recalculateAfterEntryChange(bc, member);
            }
    );

    // Apply delete
    bc.payments.removeAll(entriesToRemove);
    recalculateAfterEntryChange(bc, member);

    Toast.makeText(context, "Entries deleted", Toast.LENGTH_SHORT).show();
}

private void recalculateAfterEntryChange(Bc bc, String changedMember) {

    if (bc.payments == null) bc.payments = new ArrayList<>();
    if (bc.paymentEntries == null) bc.paymentEntries = new HashMap<>();
    if (bc.paidAmount == null) bc.paidAmount = new HashMap<>();
    if (bc.paid == null) bc.paid = new HashMap<>();
    if (bc.paidBcAmount == null) bc.paidBcAmount = new HashMap<>(); // DO NOT CLEAR

    // 🔥 CLEAR ONLY INSTALLMENT MAPS
    bc.paymentEntries.clear();
    bc.paidAmount.clear();
    bc.paid.clear();

    // 🔁 REBUILD INSTALLMENT DATA ONLY
    for (PaymentEntry pe : bc.payments) {

        String key = bc.getPaidKey(pe.member, pe.monthIndex);

        // paymentEntries
        List<PaymentEntry> list = bc.paymentEntries.get(key);
        if (list == null) list = new ArrayList<>();
        list.add(pe);
        bc.paymentEntries.put(key, list);

        // paidAmount per month
        double total = bc.paidAmount.getOrDefault(key, 0.0) + pe.amount;
        bc.paidAmount.put(key, total);
    }

    // 🔁 REBUILD TICK STATUS
    for (String memberName : bc.members) {
        for (int m = 0; m < bc.months; m++) {

            String key = bc.getPaidKey(memberName, m);
            double paidAmt = bc.paidAmount.getOrDefault(key, 0.0);

            double expected = 0.0;
            if (bc.amounts != null && m < bc.amounts.size()) {
                expected = bc.amounts.get(m);
            } else if (bc.amounts != null && !bc.amounts.isEmpty()) {
                expected = bc.amounts.get(0);
            }

            bc.paid.put(key, paidAmt >= expected);
        }
    }

    // 🚫 DO NOT TOUCH bc.paidBcAmount HERE

    saveAllToRoom();

    if (spinnerBc.getSelectedItemPosition() > 0) {
        try {
            renderMainTable(bc);
        } catch (Exception e) {
            showCrashDialog(e);
        }
    }
}

private void showCrashDialog(Exception e) {

    String error = e.toString() + "\n\n";

    for (StackTraceElement element : e.getStackTrace()) {
        error += element.toString() + "\n";
    }

    new AlertDialog.Builder(context)
            .setTitle("Crash Details")
            .setMessage(error)
            .setPositiveButton("OK", null)
            .show();
}

private void moveMapsAfterRename(Bc bc, String oldName, String newName) {

    if (oldName.equals(newName)) return;

    Map<String, Boolean> newPaidMap = new HashMap<>();
    Map<String, Double> newPaidAmountMap = new HashMap<>();
    Map<String, List<PaymentEntry>> newPaymentEntriesMap = new HashMap<>();

    // 🔁 MOVE paid + paidAmount month-wise keys
    for (int m = 0; m < bc.months; m++) {

        String oldKey = bc.getPaidKey(oldName, m);
        String newKey = bc.getPaidKey(newName, m);

        if (bc.paid.containsKey(oldKey)) {
            newPaidMap.put(newKey, bc.paid.get(oldKey));
        }

        if (bc.paidAmount.containsKey(oldKey)) {
            newPaidAmountMap.put(newKey, bc.paidAmount.get(oldKey));
        }

        if (bc.paymentEntries.containsKey(oldKey)) {
            newPaymentEntriesMap.put(newKey, bc.paymentEntries.get(oldKey));
        }
    }

    // 🧹 REMOVE OLD KEYS
    for (int m = 0; m < bc.months; m++) {
        String oldKey = bc.getPaidKey(oldName, m);
        bc.paid.remove(oldKey);
        bc.paidAmount.remove(oldKey);
        bc.paymentEntries.remove(oldKey);
    }

    // ➕ ADD NEW KEYS
    bc.paid.putAll(newPaidMap);
    bc.paidAmount.putAll(newPaidAmountMap);
    bc.paymentEntries.putAll(newPaymentEntriesMap);

    // 🔁 ALSO update member name inside payment history list
    for (PaymentEntry pe : bc.payments) {
        if (pe.member.equals(oldName)) {
            pe.member = newName;
        }
    }

    // 🔁 Move Paid BC amount (this one uses direct member key)
    if (bc.paidBcAmount.containsKey(oldName)) {
        Double amt = bc.paidBcAmount.remove(oldName);
        bc.paidBcAmount.put(newName, amt);
    }
}

private void restoreMapsAfterRename(Bc bc, String oldName, String newName,
                                    Boolean dummy1, Double dummy2, Double dummy3) {

    if (oldName.equals(newName)) return;

    // Just reverse rename logic
    moveMapsAfterRename(bc, newName, oldName);
}

private void showEditModeToggleDialog() {

    View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_mode, null);
    SwitchCompat switchEdit = view.findViewById(R.id.switchEditMode);
    switchEdit.setChecked(isEditModeEnabled);

    switchEdit.setOnCheckedChangeListener((buttonView, isChecked) -> {
        isEditModeEnabled = isChecked;
    });

    new AlertDialog.Builder(context)
            .setTitle("Edit Mode Settings")
            .setView(view)
            .setPositiveButton("OK", (d, w) -> {
                Toast.makeText(context,
                        isEditModeEnabled ? "Edit Mode Enabled" : "View Only Mode Enabled",
                        Toast.LENGTH_SHORT).show();
                updateLockIcon();
            })
            .setNegativeButton("Cancel", null)
            .show();
}

private void animateSwitch(View show, View hide) {

    hide.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction(() -> hide.setVisibility(View.GONE));

    show.setAlpha(0f);
    show.setVisibility(View.VISIBLE);
    show.animate()
            .alpha(1f)
            .setDuration(150)
            .start();
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
