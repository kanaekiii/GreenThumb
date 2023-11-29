package com.abinaya.greenthumb.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.abinaya.greenthumb.tracker.interf.IPlantTrackerListener;
import com.abinaya.greenthumb.R;
import com.abinaya.greenthumb.android.AndroidConstants;
import com.abinaya.greenthumb.android.adapters.GroupTileArrayAdapter;
import com.abinaya.greenthumb.android.interf.ICallback;
import com.abinaya.greenthumb.tracker.impl.actions.Group;
import com.abinaya.greenthumb.tracker.impl.actions.Plant;
import com.abinaya.greenthumb.tracker.impl.actions.PlantTracker;
import com.abinaya.greenthumb.tracker.interf.IPlantTrackerListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class GroupManagement extends AppCompatActivity implements IPlantTrackerListener {

    private PlantTracker plantTracker;
    private ListView groupListView;
    private boolean groupsModified = false;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_management);

        if (savedInstanceState != null) {
            plantTracker = (PlantTracker)savedInstanceState.getSerializable(
                    AndroidConstants.INTENTKEY_PLANT_TRACKER);
        }
        else    {
            Intent startingIntent = getIntent();

            plantTracker = (PlantTracker)startingIntent.getSerializableExtra(
                    AndroidConstants.INTENTKEY_PLANT_TRACKER);

        }

        plantTracker.setPlantTrackerListener(this);

        bindView();
        fillGroups();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

        bundle.putSerializable(AndroidConstants.INTENTKEY_PLANT_TRACKER, plantTracker);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo)   {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_group_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        long selectedGroup = plantTracker.getAllGroups().get(info.position).getGroupId();

        if (item.getItemId() == R.id.rename) {
            presentRenameGroupDialog(GroupManagement.this, plantTracker, selectedGroup, new ICallback() {
                @Override
                public void callback() {
                }
            });
            return true;
        } else if (item.getItemId() == R.id.delete) {
            presentDeleteGroupDialog(selectedGroup);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        endActivity();
    }

    @SuppressWarnings("unchecked")
    protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
        super.onActivityResult(requestCode, resultCode, returnedIntent);
        switch (requestCode) {
            case AndroidConstants.ACTIVITY_LIST_PICKER_PLANTS:
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<Plant> selected = (ArrayList<Plant>)returnedIntent
                            .getSerializableExtra(AndroidConstants.INTENTKEY_LIST_PICKER_SELECTED);

                    ArrayList<Plant> unselected = (ArrayList<Plant>)returnedIntent
                            .getSerializableExtra("unselected");

                    Long groupId = returnedIntent.getLongExtra("groupId", 0);

                    if (selected != null)   {
                        for(Plant p : selected) {
                            if (!p.isMemberOfGroup(groupId))    {
                                plantTracker.addMemberToGroup(p.getPlantId(), groupId);
                            }
                        }
                    }

                    if (unselected != null) {
                        for(Plant p : unselected)   {
                            if (p.isMemberOfGroup(groupId)) {
                                plantTracker.removeMemberFromGroup(p.getPlantId(), groupId);
                            }
                        }
                    }
                }
                break;
        }
    }

    private void bindView() {
        groupListView = findViewById(R.id.groupListView);

        Toolbar gmToolbar = findViewById(R.id.gmToolbar);
        gmToolbar.setTitle(R.string.manage_groups);
    }

    private void fillGroups()   {
        FloatingActionButton floatingButton = findViewById(R.id.floatingActionButton);
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentAddGroupDialog(GroupManagement.this, plantTracker, new ICallback() {
                    @Override
                    public void callback() {
                        fillGroups();
                    }
                });
            }
        });

        final ArrayList<Group> groups = plantTracker.getAllGroups();

        GroupTileArrayAdapter adapter = new GroupTileArrayAdapter(GroupManagement.this,
                R.layout.tile_group_list, groups);

        setEmptyViewCaption();

        registerForContextMenu(groupListView);

        groupListView.setAdapter(adapter);

        groupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Group g = groups.get(position);
                Intent i = new Intent(GroupManagement.this, ListPicker.class);

                i.putExtra(AndroidConstants.INTENTKEY_LIST_PICKER_LIST,
                        plantTracker.getAllPlants());

                i.putExtra("selected", plantTracker.getMembersOfGroup(g.getGroupId()));
                i.putExtra("groupId", g.getGroupId());
                i.putExtra("groupName", g.getGroupName());

                startActivityForResult(i, AndroidConstants.ACTIVITY_LIST_PICKER_PLANTS);
            }
        });
    }

    static void presentAddGroupDialog(Context c, PlantTracker tracker, ICallback caller) {
        final Dialog dialog = new Dialog(c);
        dialog.setContentView(R.layout.dialog_add_group);

        final EditText groupNameEditText = dialog.findViewById(R.id.groupNameEditText);
        Button okButton = dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (groupNameEditText.getText().toString().isEmpty()) {
                    return;
                }

                tracker.addGroup(groupNameEditText.getText().toString());

                caller.callback();
                dialog.dismiss();
            }
        });

        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    static void presentRenameGroupDialog(Context c, PlantTracker tracker, long groupId,
                                          ICallback caller) {
        final Dialog dialog = new Dialog(c);
        dialog.setContentView(R.layout.dialog_rename_group);

        final EditText groupNameEditText = dialog.findViewById(R.id.groupNameEditText);
        final TextView groupNameTextView = dialog.findViewById(R.id.groupNameTextView);
        groupNameTextView.setText(tracker.getGroup(groupId).getGroupName());

        final long localGroupId = groupId;

        Button okButton = dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (groupNameEditText.getText().toString().isEmpty()) {
                    return;
                }

                tracker.renameGroup(localGroupId, groupNameEditText.getText().toString());
                caller.callback();
                dialog.dismiss();
            }
        });

        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        try {
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void presentDeleteGroupDialog(long groupId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupManagement.this);
        builder.setTitle(R.string.app_name);
        builder.setMessage("Are you sure you want to delete this group?");
        builder.setIcon(R.drawable.ic_bundle_of_hay);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                plantTracker.removeGroup(groupId);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();alert.show();

    }

    private void setEmptyViewCaption() {
//        View emptyPlantListView = findViewById(R.id.emptyPlantListView);
//        TextView itemNotFoundCaptionText = (TextView) emptyPlantListView.findViewById(
//                R.id.itemNotFoundCaptionText);
//
//        if (itemNotFoundCaptionText != null) {
//            itemNotFoundCaptionText.setText(caption);
//        }
//
//        emptyPlantListView.invalidate();
    }

    private void endActivity()  {
        if (groupsModified) {
            plantTracker.savePlantTrackerSettings();
            Intent i = new Intent();
            i.putExtra(AndroidConstants.INTENTKEY_PLANT_TRACKER, plantTracker);
            setResult(RESULT_OK, i);
        }
        else    {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    @Override
    public void plantUpdated(Plant p) {

    }

    @Override
    public void groupsUpdated() {
        groupsModified = true;
        fillGroups();
    }
}
