package com.example.bcapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
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
import android.widget.ScrollView;

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

private final AppCompatActivity activity;  
private final Context context;  

// UI  
private final TextView menuButton;  
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

    setupMenu();  
    setupDatePickers();  
    setupListeners();  

    // Load saved data from Room  
    loadFromRoomAndRefreshUi();  
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
    Button buttonSaveBc = dialogView.findViewById(R.id.buttonSaveBc);
    Button buttonCancelBc = dialogView.findViewById(R.id.buttonCancelBc);

    
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
        // Title
        TextView title = new TextView(context);
        title.setText(bc.name);
        title.setTextSize(16f);
        title.setPadding(0, 8, 0, 4);
        tableContainer.addView(title);

        // 🔹 HORIZONTAL SCROLL + TABLE
        HorizontalScrollView hScroll = new HorizontalScrollView(context);
        hScroll.setHorizontalScrollBarEnabled(true);
        
        TableLayout table = new TableLayout(context);
        table.setStretchAllColumns(true);
        table.setShrinkAllColumns(true);

        // ========== HEADER ==========
        TableRow header = new TableRow(context);
        addCellFixedWidth(header, "Sr", true, 80);  // Fixed width for short text
        addCellFixedWidth(header, "Date", true, 100);
        addCellFixedWidth(header, "Amount", true, 120);
        addCellFixedWidth(header, "Receive Amount", true, 140);  // 
        for 2 lines
               if (bc.afterTaken) {
                   addCellFixedWidth(header, "After Taken", true, 120);
        }
        table.addView(header);

        // ========== ROWS ==========
        for (int i = 0; i < bc.months; i++) {
            TableRow row = new TableRow(context);
            addCellFixedWidth(row, String.valueOf(i + 1), false, 80);
            
            Calendar cal = parseIsoDate(bc.startDateIso);
            if (cal != null) cal.add(Calendar.MONTH, i);
            String dateStr = cal != null ? displayFormat.format(cal.getTime()) : "-";
            addCellFixedWidth(row, dateStr, false, 100);

            // Amount
            double amount = bc.amounts.size() > i ? bc.amounts.get(i) : 0.0;
            addCellFixedWidth(row, "₹" + String.format("%.0f", amount), false, 120);

            // Receive Amount
            double receiveAmount = 0.0;
            if (!bc.receiveAmounts.isEmpty()) {
                if (bc.isReceiveAmountFixed) {
                    receiveAmount = bc.receiveAmounts.get(0);
                } else if (bc.receiveAmounts.size() > i) {
                    receiveAmount = bc.receiveAmounts.get(i);
                }
            }
            addCellFixedWidth(row, "₹" + String.format("%.0f", receiveAmount), false, 140);

            if (bc.afterTaken) {
                addCellFixedWidth(row, "₹" + String.format("%.0f", bc.afterTakenAmount), false, 120);
            }
            table.addView(row);
        }

        hScroll.addView(table);
        tableContainer.addView(hScroll);
    }
}

private void renderMainTable(Bc bc) {
    tableContainer.removeAllViews();
    if (bc == null) return;

    TextView title = new TextView(context);
    title.setText("Main BC Table");
    title.setTextSize(16f);
    title.setPadding(0, 8, 0, 4);
    tableContainer.addView(title);

    TableLayout table = new TableLayout(context);
    table.setStretchAllColumns(false);

    TableRow header = new TableRow(context);
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
        memberCell.setTextSize(14f);

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
        memberCell.setTextColor(Color.parseColor("#424242"));
        memberCell.setTypeface(null, Typeface.NORMAL);
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
            cellContainer.setPadding(6, 6, 6, 6);
            cellContainer.setMinimumHeight(72);
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
                checkbox.setTextColor(Color.GRAY);
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
        totalCell.setTextSize(14f);
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
        paidBcCell.setTextSize(14f);
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

    tableContainer.addView(table);
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

    // ✅ ADD PAYMENT ENTRY
    PaymentEntry pe = new PaymentEntry(member, monthIndex, enteredAmount, dateVal);
    bc.payments.add(pe);

    // ✅ IMMEDIATE REBUILD (FIX FOR POPUP NOT UPDATING)
    bc.paymentEntries.clear();
    for (PaymentEntry p : bc.payments) {
        String k = bc.getPaidKey(p.member, p.monthIndex);
        List<PaymentEntry> list = bc.paymentEntries.get(k);
        if (list == null) {
            list = new ArrayList<>();
            bc.paymentEntries.put(k, list);
        }
        list.add(p);
    }

    // 🔹 Accumulate paid amount (PARTIAL SUPPORT)
    double currentPaid = bc.paidAmount.containsKey(key)
            ? bc.paidAmount.get(key)
            : 0.0;

    double newPaid = currentPaid + enteredAmount;
    bc.paidAmount.put(key, newPaid);

    // 🔹 Expected amount for that month
    double expectedAmount =
            bc.amounts.size() > monthIndex
                    ? bc.amounts.get(monthIndex)
                    : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);

    // 🔹 Mark paid only if fully completed
    bc.paid.put(key, newPaid >= expectedAmount);

    // 💾 Save + UI refresh
    saveAllToRoom();
    activity.runOnUiThread(() -> renderMainTable(bc));

    // 🧹 Clear inputs
    editPayDate.setText("");
    editPayAmount.setText("");
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
    tv.setPadding(16, 12, 16, 12);  
    tv.setGravity(Gravity.CENTER);  
    tv.setMinHeight(64);  

    if (header) {  
        tv.setTypeface(null, Typeface.BOLD);  
        tv.setTextSize(15f);  
        tv.setTextColor(Color.BLACK);  
        tv.setBackgroundResource(R.drawable.table_header_border);  // ← NEW HEADER  
    } else {  
        tv.setTextSize(14f);  
        if ("✅".equals(text)) {  
        tv.setTextColor(Color.parseColor("#2E7D32"));  
        } else {  
        tv.setTextColor(Color.parseColor("#424242"));  
        }  
        tv.setBackgroundResource(R.drawable.table_cell_border);  
    }  

    TableRow.LayoutParams lp =  
        new TableRow.LayoutParams(  
                TableRow.LayoutParams.WRAP_CONTENT,  
                TableRow.LayoutParams.MATCH_PARENT  
        );  
    lp.setMargins(1, 1, 1, 1);  
    tv.setLayoutParams(lp);  

    row.addView(tv);  
}  

private void addCellFixedWidth(TableRow row, String text, boolean header, int widthDp) {
    TextView tv = new TextView(context);
    tv.setText(text);
    tv.setGravity(Gravity.CENTER);
    tv.setPadding(8, 12, 8, 12);
    tv.setMinHeight(dpToPx(56));  // Consistent height
    
    // 🔹 TEXT WRAPPING: Single words 1 line, multi-word 2 lines max
    tv.setSingleLine(false);
    tv.setMaxLines(2);
    tv.setEllipsize(null);  // No ellipsis - show full text
    
    // Fixed width
    int widthPx = dpToPx(widthDp);
    TableRow.LayoutParams lp = new TableRow.LayoutParams(widthPx, TableRow.LayoutParams.MATCH_PARENT);
    lp.setMargins(1, 1, 1, 1);
    tv.setLayoutParams(lp);

    if (header) {
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextSize(14f);
        tv.setTextColor(Color.BLACK);
        tv.setBackgroundResource(R.drawable.table_header_border);
    } else {
        tv.setTextSize(13f);
        tv.setTextColor(Color.parseColor("#424242"));
        tv.setBackgroundResource(R.drawable.table_cell_border);
    }

    row.addView(tv);
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
    builder.setTitle("Paid BC");

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(32, 24, 32, 24);

    // BC Spinner
    Spinner bcSpinner = new Spinner(context);
    ArrayAdapter<String> bcSpinAdapter =
            new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
    bcSpinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    bcSpinAdapter.add("Select BC");
    for (Bc bc : bcData) bcSpinAdapter.add(bc.name);
    bcSpinner.setAdapter(bcSpinAdapter);
    root.addView(bcSpinner);

    // Member Spinner
    Spinner memberSpinner = new Spinner(context);
    ArrayAdapter<String> memAdapter =
            new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
    memAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    memberSpinner.setAdapter(memAdapter);
    root.addView(memberSpinner);

    // Amount
    EditText amountInput = new EditText(context);
    amountInput.setHint("Enter Paid BC Amount");
    amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    root.addView(amountInput);

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

    builder.setView(root);

    builder.setPositiveButton("OK", (d, w) -> {

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

        bc.paidBcAmount.put(member, amt);

        saveAllToRoom();
        renderMainTable(bc);
    });

    builder.setNegativeButton("Cancel", null);
    builder.show();
}

}
