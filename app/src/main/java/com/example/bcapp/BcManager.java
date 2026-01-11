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
import android.widget.AutoCompleteTextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


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

public class BcManager {

    private final AppCompatActivity activity;
    private final Context context;

    // UI
    private final TextView menuButton;
    private final AutoCompleteTextView spinnerBc, spinnerMember;
    private final EditText editPayDate, editPayAmount;
    private final Button buttonAdd;
    private final LinearLayout tableContainer;

    // Data
    private final List<Bc> bcData;
    private ArrayAdapter<String> bcAdapter;
    private ArrayAdapter<String> memberAdapter;
    private int selectedBcIndex = -1;
    private int selectedMemberIndex = -1;

    // Date formats
    private final SimpleDateFormat isoFormat;
    private final SimpleDateFormat displayFormat;

    // Room
    private final AppDatabase db;
    private final BcDao bcDao;

    public BcManager(AppCompatActivity activity,
                     TextView menuButton,
                     AutoCompleteTextView spinnerBc,
                     AutoCompleteTextView spinnerMember,
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
        spinnerBc.setText("Select BC", false);

        memberAdapter = new ArrayAdapter<>(context,
                R.layout.spinner_item, new ArrayList<>());
        memberAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        memberAdapter.add("Select Member");
        spinnerMember.setAdapter(memberAdapter);
        spinnerMember.setText("Select Member", false);

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
                bc.afterTaken = e.afterTaken;
                bc.afterTakenAmount = e.afterTakenAmount;   // NEW

                if (e.members != null) bc.members = e.members;
                if (e.amounts != null) bc.amounts = e.amounts;
                if (e.paid != null) bc.paid = e.paid;

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
                spinnerBc.setText("Select BC", false);

                updateMembersDropdown(-1);
            });
        }).start();
    }

    private void saveAllToRoom() {
        new Thread(() -> {
            bcDao.deleteAll();
            for (Bc bc : bcData) {
                BcEntity e = new BcEntity(bc.name, bc.months, bc.startDateIso, bc.afterTaken);
                e.afterTakenAmount = bc.afterTakenAmount; // NEW
                e.members = bc.members;
                e.amounts = bc.amounts;
                e.paid = bc.paid;
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
        spinnerBc.setOnItemClickListener((parent, view, position, id) -> {
        updateMembersDropdown(position -1);
    });
        spinnerMember.setOnItemClickListener((parent, view, position, id) -> {
        selectedMemberIndex = position > 0 ? position - 1 : -1;
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
        LinearLayout layoutMembers = dialogView.findViewById(R.id.layoutMembers);
        AutoCompleteTextView spinnerAmountType = dialogView.findViewById(R.id.spinnerAmountType);
        LinearLayout layoutAmounts = dialogView.findViewById(R.id.layoutAmounts);
        CheckBox checkAfterTaken = dialogView.findViewById(R.id.checkAfterTaken);
        Button buttonSaveBc = dialogView.findViewById(R.id.buttonSaveBc);
        Button buttonCancelBc = dialogView.findViewById(R.id.buttonCancelBc);

        // holder for after-taken amount while dialog is open
        final double[] afterTakenAmountHolder = new double[]{0.0};

        // If checked -> ask amount now
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

        // Start date picker
        editStartDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dp = new DatePickerDialog(context,
                    (view, year1, month1, dayOfMonth) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(year1, month1, dayOfMonth, 0, 0, 0);
                        editStartDate.setText(isoFormat.format(cal.getTime()));
                    },
                    year, month, day);
            dp.show();
        });

        // Amount type spinner
        ArrayAdapter<String> amountTypeAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                Arrays.asList("Select Amount Type", "Fixed", "Random")
        );
        amountTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAmountType.setAdapter(amountTypeAdapter);

        // Update Members + Amounts whenever Months changes
        editMonths.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                createMemberInputs(editMonths, layoutMembers);
                amountTypeChange(editMonths, spinnerAmountType, layoutAmounts);
            }
        });

        // Also update amounts when amount type changes
        spinnerAmountType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                amountTypeChange(editMonths, spinnerAmountType, layoutAmounts);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

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
                View child = layoutMembers.getChildAt(i);
                if (child instanceof EditText) {
                    String m = ((EditText) child).getText().toString().trim();
                    if (!m.isEmpty()) bc.members.add(m);
                }
            }

            // Amounts
            for (int i = 0; i < layoutAmounts.getChildCount(); i++) {
                View child = layoutAmounts.getChildAt(i);
                if (child instanceof EditText) {
                    String a = ((EditText) child).getText().toString().trim();
                    bc.amounts.add(a.isEmpty() ? 0.0 : Double.parseDouble(a));
                }
            }

            bc.afterTaken = checkAfterTaken.isChecked();
            bc.afterTakenAmount = bc.afterTaken ? afterTakenAmountHolder[0] : 0.0; // NEW

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

    private void showAmountDialog(
        Context context,
        LinearLayout layoutAmounts) {
        View dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_add_amount, null);
        AutoCompleteTextView spinnerAmountType = dialogView.findViewById(R.id.spinnerAmountType);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            context,
            android.R.layout.simple_dropdown_item_1line,
                        amountTypeList
                );
               spinnerAmountType.setAdapter(adapter);
        spinnerAmountType.setOnItemClickListener((parent, view, position, id) -> {

        String selected = parent.getItemAtPosition(position)
                .toString()
                .toLowerCase(Locale.ROOT);
                                  
        layoutAmounts.removeAllViews();
        int m = safeParseInt(editMonths.getText().toString());

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

    private void updateMembersDropdown(int bcIndex) {
        memberAdapter.clear();
        memberAdapter.add("Select Member");
        selectedBcIndex = bcIndex;
        selectedMemberIndex = -1;
        if (bcIndex < 0 || bcIndex >= bcData.size()) {
            memberAdapter.notifyDataSetChanged();
            renderMainTable(null);
            return;
        }
        Bc bc = bcData.get(bcIndex);
        memberAdapter.addAll(bc.members);
        memberAdapter.notifyDataSetChanged();
        spinnerMember.setText("Select Member", false);
        renderMainTable(bc);
    }

    private void showBcListTable() {
        tableContainer.removeAllViews();
        for (Bc bc : bcData) {
            TextView title = new TextView(context);
            title.setText(bc.name);
            title.setTextSize(16f);
            title.setPadding(0, 8, 0, 4);
            tableContainer.addView(title);

            TableLayout table = new TableLayout(context);
            table.setStretchAllColumns(true);
            table.setShrinkAllColumns(true);
            table.setColumnStretchable(0, true);
            table.setColumnShrinkable(0, true);

            TableRow header = new TableRow(context);
            addCell(header, "Sr", true);
            addCell(header, "Date", true);
            addCell(header, "Amount", true);
            if (bc.afterTaken) {
            addCell(header, "After Taken", true);
            }
            table.addView(header);

            for (int i = 0; i < bc.months; i++) {
                TableRow row = new TableRow(context);
                addCell(row, String.valueOf(i + 1), false);

                Calendar cal = parseIsoDate(bc.startDateIso);
                if (cal != null) cal.add(Calendar.MONTH, i);
                String dateStr = cal != null ? displayFormat.format(cal.getTime()) : "-";
                addCell(row, dateStr, false);

                double amount = bc.amounts.size() > i
                        ? bc.amounts.get(i)
                        : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);
                addCell(row, String.valueOf(amount), false);
                if (bc.afterTaken) {
                addCell(row, String.valueOf(bc.afterTakenAmount), false);
                }
                table.addView(row);
            }

            tableContainer.addView(table);
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

        TableRow header = new TableRow(context);
        addCell(header, "Sr", true);
        addCell(header, "Date", true);
        addCell(header, "Amount", true);
        addCell(header, "Member", true);

        for (int i = 0; i < bc.months; i++) addCell(header, "M" + (i + 1), true);
        table.addView(header);

        for (int r = 0; r < bc.members.size(); r++) {
            String member = bc.members.get(r);
            TableRow row = new TableRow(context);

            addCell(row, String.valueOf(r + 1), false);

            Calendar cal = parseIsoDate(bc.startDateIso);
            String dateStr = cal != null ? displayFormat.format(cal.getTime()) : "-";
            addCell(row, dateStr, false);

            double amount = bc.amounts.size() > r
                    ? bc.amounts.get(r)
                    : (!bc.amounts.isEmpty() ? bc.amounts.get(0) : 0.0);
            addCell(row, String.valueOf(amount), false);

            addCell(row, member, false);

            for (int m = 0; m < bc.months; m++) {

                String key = bc.getPaidKey(member, m);
                Boolean isPaid = bc.paid.get(key);

                TextView status = new TextView(context);
                boolean paid = isPaid != null && isPaid;
                status.setText(paid ? "✅" : "☐");
                status.setTextSize(18f);
                status.setGravity(Gravity.CENTER);
                status.setPadding(12, 12, 12, 12);
                status.setBackgroundResource(R.drawable.table_cell_border);
                if (paid) {
                    status.setTextColor(Color.parseColor("#2E7D32")); // green
                }

                TableRow.LayoutParams lp = new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(1, 1, 1, 1);
                status.setLayoutParams(lp);

                row.addView(status);
            }
            table.addView(row);
        }

        tableContainer.addView(table);
    }

    /* ---------- Installments ---------- */

    private void markInstallment() {
        int bcIndex = selectedBcIndex;
        int memberIndex = selectedMemberIndex;
        String dateVal = editPayDate.getText().toString().trim();

        if (bcIndex < 0 || memberIndex < 0 || dateVal.isEmpty()) {
            Toast.makeText(context, "Please select BC, Member and Date", Toast.LENGTH_SHORT).show();
            return;
        }

        Bc bc = bcData.get(bcIndex);
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

        String member = bc.members.get(memberIndex);
        String key = bc.getPaidKey(member, monthIndex);
        bc.paid.put(key, true);

        saveAllToRoom();
        renderMainTable(bc);

        editPayDate.setText("");
        editPayAmount.setText("");
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
}
