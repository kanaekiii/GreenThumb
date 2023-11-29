package com.abinaya.greenthumb.tracker.impl.actions;

import com.abinaya.greenthumb.tracker.interf.ISettingsChangedListener;
import com.google.gson.annotations.Expose;
import com.abinaya.greenthumb.tracker.exceptions.GroupNotFoundException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TreeMap;


public class PlantTrackerSettings implements Serializable {
    private static final long serialVersionUID = 6952312342L;

    @Expose(serialize = false, deserialize = false)
    private transient ISettingsChangedListener listener;

    private TreeMap<String, GenericRecord> genericRecordTemplates;
    private ArrayList<Group> groups;
    private ArrayList<String> stateAutoComplete;

    private String syncAddress = null;
    private Date lastSync = null;
    private ArrayList<String> plantChangesSinceLastSync = new ArrayList<>();
    private ArrayList<String> imageChangesSinceLastSync = new ArrayList<>();

    PlantTrackerSettings()   {
        genericRecordTemplates = new TreeMap<>();
        groups = new ArrayList<Group>();
        stateAutoComplete = new ArrayList<String>();
    }

    void setListener(PlantTracker l) {
        listener = l;
    }

    void removeListener()   {
        listener = null;
    }

    private void settingsChanged()  {
        // use of a listener is not required!
        if (listener != null)   {
            listener.settingsChanged();
        }
    }

    void addGroup(Group g)  {
        if (!groups.contains(g))    {
            groups.add(g);
        }

        settingsChanged();
    }

    void removeGroup(long groupId)   {
        Group g = new Group(groupId, "");
        removeGroup(g);
    }

    private void removeGroup(Group g)   {
        groups.remove(g);
        settingsChanged();
    }

    Group getGroup(long groupId) {
        Group g = new Group(groupId, null);
        if (groups.contains(g)) {
            return groups.get(groups.indexOf(g));
        }

        return null;
    }

    final ArrayList<Group> getGroups() {
        return groups;
    }

    public boolean addStateAutoComplete(String stateName)  {
        if (stateAutoComplete == null)  {
            stateAutoComplete = new ArrayList<String>();
        }

        if (!stateAutoComplete.contains(stateName))  {
            stateAutoComplete.add(stateName);
            settingsChanged();
            return true;
        }

        return false;
    }

    public ArrayList<String> getStateAutoComplete() {
        return stateAutoComplete;
    }

    void removeStateAutoComplete(String key) {
        stateAutoComplete.remove(key);
    }

    public void addGenericRecordTemplate(GenericRecord record)  {
        if (genericRecordTemplates.containsKey(record.displayName)) {
            genericRecordTemplates.remove(record.displayName);
        }

        genericRecordTemplates.put(record.displayName, record);

        settingsChanged();
    }

    public void removeGenericRecordTemplate(GenericRecord record) {
        if (genericRecordTemplates.containsKey(record.displayName)) {
            genericRecordTemplates.remove(record.displayName);
        }

        settingsChanged();
    }

    Set<String> getGenericRecordNames() {
        if (genericRecordTemplates == null) {
            genericRecordTemplates = new TreeMap<>();
        }

        return genericRecordTemplates.keySet();
    }

    GenericRecord getGenericRecordTemplate(String name)  {
        try {
            GenericRecord record = genericRecordTemplates.get(name);

            if (record != null) {
                return (GenericRecord)record.clone();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public GenericRecord getGenericRecordTemplate(long id)  {
        for(GenericRecord rt : genericRecordTemplates.values()) {
            if (rt.id == id)    {
                return rt;
            }
        }

        return null;
    }

    void removeGenericRecordTemplate(String name)    {
        genericRecordTemplates.remove(name);
        settingsChanged();
    }

    final TreeMap<String, GenericRecord> getAllGenericRecordTemplates()    {
        return genericRecordTemplates;

    }

    public void setSyncServerAddress(String s) {
        if (s.equals("")) {
            syncAddress = null;
        }
        else {
            syncAddress = s;
        }

        settingsChanged();
    }

    public String getSyncServerAddress() {
        return syncAddress;
    }

    public void addPlantChangeSinceSync(String plantId) {
        if (plantChangesSinceLastSync.contains(plantId) || !hasSynced())
            return;

        plantChangesSinceLastSync.add(plantId);
        settingsChanged();
    }

    public void addImageChangesSinceLastSync(String imageName) {
        if (imageChangesSinceLastSync.contains(imageName) || !hasSynced())
            return;

        imageChangesSinceLastSync.add(imageName);
        settingsChanged();
    }

    public void resetChangesSinceSync() {
        lastSync = new Date();
        plantChangesSinceLastSync.clear();
        imageChangesSinceLastSync.clear();
        settingsChanged();
    }

    public boolean hasSynced() {
        return lastSync != null;
    }

    public TreeMap<String, ArrayList<String>> getChangesSinceLastSync() {
        TreeMap<String, ArrayList<String>> changes = new TreeMap<>();

        changes.put("plants", plantChangesSinceLastSync);
        changes.put("images", imageChangesSinceLastSync);

        return changes;
    }
}
