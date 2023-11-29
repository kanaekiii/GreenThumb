package com.abinaya.greenthumb.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.abinaya.greenthumb.R;
import com.abinaya.greenthumb.android.AndroidConstants;
import com.abinaya.greenthumb.android.adapters.CustomEventTileArrayAdapter;
import com.abinaya.greenthumb.tracker.impl.actions.GenericRecord;
import com.abinaya.greenthumb.tracker.impl.actions.PlantTracker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ManageRecordTemplates extends AppCompatActivity {

    private static final int CREATE_GENERIC_RECORD_TEMPLATE_INTENT = 26;
    private static final int EDIT_GENERIC_RECORD_TEMPLATE_INTENT = 27;

    private PlantTracker tracker;
    private Toolbar toolbar;
    private FloatingActionButton floatingActionButton;
    private ListView customRecordTemplateListView;

    private GenericRecord selectedRecord;

    public ManageRecordTemplates(GenericRecord selectedRecord) {
        this.selectedRecord = selectedRecord;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manage_record_templates);

        Intent intent = getIntent();

        tracker = (PlantTracker) intent.getSerializableExtra(
                AndroidConstants.INTENTKEY_PLANT_TRACKER);

        bindUi();
        fillUi();
    }

    private void bindUi()   {
        toolbar = findViewById(R.id.toolbar);
        floatingActionButton = findViewById(R.id.floatingButton);
        customRecordTemplateListView = findViewById(R.id.customRecordTemplateListView);
        floatingActionButton = findViewById(R.id.floatingButton);
    }

    private void fillUi() {
        toolbar.setTitle("Manage Record Templates");

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManageRecordTemplates.this, CreateRecordType.class);
                intent.putExtra(AndroidConstants.INTENTKEY_GENERIC_RECORD, new GenericRecord(""));

                startActivityForResult(intent, CREATE_GENERIC_RECORD_TEMPLATE_INTENT);
            }
        });

        final ArrayList<String> events = new ArrayList<>();
        if (tracker != null) {
            events.addAll(tracker.getGenericRecordTypes());
        }

        final ArrayList<String> fEvents = events;

        CustomEventTileArrayAdapter adapter = new CustomEventTileArrayAdapter(
                getBaseContext(), R.layout.tile_generic_record_list, events);

        customRecordTemplateListView.setAdapter(adapter);

        customRecordTemplateListView.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                        // Remaining code for the long click listener
                        return false;
                    }
                });

        customRecordTemplateListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Remaining code for the item click listener
            }
        });
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ManageRecordTemplates.this, GreenThumbUi.class);
        startActivity(intent);
        finish();
    }



    protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
        super.onActivityResult(requestCode, resultCode, returnedIntent);
        switch (requestCode) {

            case CREATE_GENERIC_RECORD_TEMPLATE_INTENT:
                if (resultCode == Activity.RESULT_OK)   {
                    GenericRecord record = (GenericRecord)returnedIntent.getSerializableExtra(
                            AndroidConstants.INTENTKEY_GENERIC_RECORD);

                    tracker.addGenericRecordTemplate(record);

                    saveSettings();
                    fillUi();
                }
                break;

            case EDIT_GENERIC_RECORD_TEMPLATE_INTENT:
                if (resultCode == Activity.RESULT_OK)   {
                    GenericRecord retRec = (GenericRecord) returnedIntent.getSerializableExtra(
                            AndroidConstants.INTENTKEY_GENERIC_RECORD);

                    if (selectedRecord.displayName.equals(retRec.displayName))   {
                        tracker.addGenericRecordTemplate(retRec);
                    }
                    else    {
                        tracker.removeGenericRecordTemplate(selectedRecord.displayName);
                        tracker.addGenericRecordTemplate(retRec);
                    }

                    saveSettings();
                    fillUi();
                }
                break;
        }
    }

    private void saveSettings() {
        tracker.setPlantTrackerSettings(tracker.getPlantTrackerSettings());
        tracker.savePlantTrackerSettings();
    }

    public ManageRecordTemplates() {
        // Initialize any necessary fields or perform default initialization here
    }

}
