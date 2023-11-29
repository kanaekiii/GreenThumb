package com.abinaya.greenthumb.tracker.impl.actions;

import android.graphics.Color;
import android.service.autofill.RegexValidator;
import android.util.Log;

import com.abinaya.greenthumb.tracker.interf.IPlantTrackerListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.abinaya.greenthumb.android.AndroidConstants;
import com.abinaya.greenthumb.tracker.exceptions.PlantNotFoundException;
import com.abinaya.greenthumb.tracker.impl.actions.PlantAction;
import com.abinaya.greenthumb.tracker.interf.IPlantUpdateListener;
import com.abinaya.greenthumb.tracker.interf.ISettingsChangedListener;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlantTracker implements IPlantUpdateListener, ISettingsChangedListener, Serializable {
    private PlantTrackerSettings settings;
    private ArrayList<Plant> plants;
    private transient IPlantTrackerListener uiListener;
    private transient ArrayList<Plant> activePlants;
    private transient ArrayList<Plant> archivedPlants;
    private String plantFolderPath;

    private static final String FILE_EXTENSION = ".json";
    private static final String SETTINGS_FOLDER = "/settings/";
    private static final String SETTINGS_FILE = "tracker_settings.json";
    private static final String PLANTS_FOLDER = "/plants/";


    public PlantTracker() {
        this(AndroidConstants.PATH_TRACKER_DATA);
    }

    public PlantTracker(String plantFolderPath) {
        this.plantFolderPath = plantFolderPath;

        plants = new ArrayList<>();
        archivedPlants = new ArrayList<>();
        activePlants = new ArrayList<>();

        loadSettings();

        loadPlants();

        initGenericRecords();

        temporaryLegacyActions();
    }

    private boolean loadSettings() {
        File settingsFile = new File(plantFolderPath + SETTINGS_FOLDER + SETTINGS_FILE);
        if (!settingsFile.exists()) {
            settings = new PlantTrackerSettings();
            savePlantTrackerSettings();
        } else {
            loadPlantTrackerSettings();
        }

        settings.setListener(this);

        return true;
    }

    private void temporaryLegacyActions()   {
        for(String key : settings.getAllGenericRecordTemplates().keySet())  {
            GenericRecord gr = settings.getAllGenericRecordTemplates().get(key);
            if (gr.id == 0) {
                gr.id = gr.time.getTimeInMillis();
            }
        }

        settingsChanged();
    }

    private void initGenericRecords() {
        GenericRecord record = null;

        if (settings.getGenericRecordTemplate("Phase Change") == null) {
            record = new GenericRecord("Phase Change");
            record.setDataPoint("Phase Name", new String());
            record.showNotes = true;
            record.summaryTemplate = "Plant entered a new phase, {Phase Name}";
            record.color = Color.rgb(255,192,203);  // a light pink
            settings.addGenericRecordTemplate(record);
        }

        if (settings.getGenericRecordTemplate("Feeding") == null) {
            record = new GenericRecord("Feeding");
            record.setDataPoint("pH", new Double(6.5));
            record.setDataPoint("Food Strength", new Double(0.5));
            record.showNotes = true;
            record.summaryTemplate = "pH of food {pH} with strength of {Food Strength}";
            record.color = Color.rgb(210,105,30);   // orange-brown
            settings.addGenericRecordTemplate(record);
        }

        if (settings.getGenericRecordTemplate("Water") == null) {
            record = new GenericRecord("Water");
            record.setDataPoint("pH", new Double(6.5));
            record.showNotes = true;
            record.summaryTemplate = "pH of water {pH}";
            record.color = Color.	rgb(135,206,250);   // light sky blue
            settings.addGenericRecordTemplate(record);
        }

        if (settings.getGenericRecordTemplate("Pictures") == null)  {
            record = new GenericRecord("Pictures");
            record.color = Color.rgb(220,220,220);  // light grey
            record.showNotes = true;
            settings.addGenericRecordTemplate(record);
        }

        if (settings.getGenericRecordTemplate("Potting") == null)  {
            record = new GenericRecord("Potting");
            record.color = Color.rgb(220,20,60);    // crimson
            record.setDataPoint("Pot Size", "");
            record.showNotes = true;
            record.summaryTemplate = "Planted into {Pot Size} pot";
            settings.addGenericRecordTemplate(record);
        }

        if (settings.getGenericRecordTemplate("Observation") == null)   {
            record = new GenericRecord("Observation");
            record.color = Color.rgb(255,165,0); // orange
            record.showNotes = true;
            settings.addGenericRecordTemplate(record);
        }
    }

    public Iterator<Plant> getIteratorForAllPlants() {
        return plants.iterator();
    }

    public ArrayList<Plant> getAllPlants() {
        return plants;
    }

    public ArrayList<Plant> getActivePlants() {
        if (activePlants != null)   {
            activePlants.clear();
        }
        else    {
            activePlants = new ArrayList<>();
        }

        for (Plant p : plants) {
            if (!p.isArchived()) {
                activePlants.add(p);
            }
        }

        return activePlants;
    }

    public ArrayList<Plant> getArchivedPlants() {
        archivedPlants.clear();
        for (Plant p : plants) {
            if (p.isArchived()) {
                archivedPlants.add(p);
            }
        }

        return archivedPlants;
    }

    public void addPlant(String plantName, boolean isFromSeed) {
        addPlant(Calendar.getInstance(), plantName, isFromSeed);
    }

    public Plant addPlant(Calendar c, String plantName, boolean isFromSeed) {
        Plant p = new Plant(c, plantName, isFromSeed);
        p.addUpdateListener(this);
        plants.add(p);
        plantUpdate(p);

        return p;
    }

    public Plant addPlant(Calendar c, String plantName, long parentPlantId) {
        Plant p = new Plant(c, plantName, false);
        p.addUpdateListener(this);
        p.setParentPlantId(parentPlantId);
        plants.add(p);
        plantUpdate(p);

        return p;
    }

    public void removePlant(int plantIndex) {
        deletePlantFileData(plants.get(plantIndex));
        plants.remove(plantIndex);
    }

    public void removePlant(Plant p) {
        deletePlantFileData(p);
        plants.remove(p);
    }

    public void saveAllPlants() {
        File folder = new File(plantFolderPath);
        if (!folder.exists()) {
            folder.mkdir();
        }

        // save each plant to a file individually into the plants folder
        for (Plant p : plants) {
            savePlant(p);
        }
    }

    public void savePlant(Plant p) {
        settings.addPlantChangeSinceSync("" + p.getPlantId());
        File folder = new File(plantFolderPath + PLANTS_FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
        }

        try {
            String filePath = plantFolderPath + PLANTS_FOLDER + p.getPlantId() + FILE_EXTENSION;
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
            Gson g = new Gson();
            String json = g.toJson(p.getPlantData());
            bw.write(json);
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importFinished()   {
        loadPlants();
        loadSettings();
    }


    private void loadPlants() {
        // search plants folder for plant files
        List<String> results = new ArrayList<String>();
        File[] files = new File(plantFolderPath + PLANTS_FOLDER).listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    if (file.getName().endsWith(FILE_EXTENSION)) {
                        Plant p = loadPlant(file);
                        attachPlant(p);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void attachPlant(Plant p) {
        if (p == null) {
            return;
        }

        // we're loading a copy of a plant, make the id unique so we don't overwrite the original
        if (plants.contains(p)) {
            p.regeneratePlantId();
        }

        p.addUpdateListener(this);
        plants.add(p);

        p.plantLoadFinished();

        if (p.isArchived()) {
            archivedPlants.add(p);
        } else {
            activePlants.add(p);
        }
    }

    public boolean importPlants(ArrayList<File> files) {
        for (File f : files) {
            Plant p = loadPlant(f);
            if (p != null) {
                attachPlant(p);
                savePlant(p);
            }
        }

        return true;
    }

    private Plant loadPlant(File file) {
        String filePath = plantFolderPath + PLANTS_FOLDER + file.getName();

        try {
            Plant p = new Plant();
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            StringBuilder sb = new StringBuilder();
            while (br.ready()) {
                sb.append(br.readLine());
            }

            return getPlantFromJson(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            File f = new File(filePath);
            // f.delete();
            return null;
        }
    }

    private Plant getPlantFromJson(String json)  {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy",
                Locale.getDefault());

        Gson g = new Gson();
        Plant p = new Plant();

        Type plantType = new TypeToken<PlantData>() {
        }.getType();

        PlantData plantData = g.fromJson(json, plantType);

        for(GenericRecord rec : plantData.genericRecords)   {
            StringBuilder sBuilder = new StringBuilder();
            // Build phase string
            sBuilder.append(sdf.format(rec.time.getTime()));
            sBuilder.append(" ");

            int phaseCount = rec.phaseCount;
            int stateWeekCount = rec.weeksSincePhase;
            int growWeekCount = rec.weeksSinceStart;

            if (rec.phaseCount > 0)   {
                sBuilder.append("[P");
                sBuilder.append(phaseCount);
                sBuilder.append("Wk");
                sBuilder.append(stateWeekCount);
                sBuilder.append("/");
                sBuilder.append(growWeekCount);
                sBuilder.append("]");
            }
            else    {
                sBuilder.append("[Wk ");
                sBuilder.append(growWeekCount);
                sBuilder.append("]");
            }

            rec.phaseDisplay = sBuilder.toString();

            rec.hasImages = (rec.images != null && rec.images.size() > 0);
            rec.hasDataPoints = (rec.dataPoints != null && rec.dataPoints.size() > 0);
        }

        p.setPlantData(plantData);

        return p;
    }

    public void savePlantTrackerSettings() {
        File folder = new File(plantFolderPath + SETTINGS_FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
        }

        try {
            String filePath = plantFolderPath + SETTINGS_FOLDER + SETTINGS_FILE;
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
            Gson g = new Gson();
            bw.write(g.toJson(settings));
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPlantTrackerSettings() {
        String filePath = plantFolderPath + SETTINGS_FOLDER + SETTINGS_FILE;

        try {
            Plant p = new Plant();
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            Gson g = new Gson();

            Type settingsType = new TypeToken<PlantTrackerSettings>() {
            }.getType();

            StringBuilder sb = new StringBuilder();
            while (br.ready()) {
                sb.append(br.readLine());
            }

            settings = g.fromJson(sb.toString(), settingsType);
        } catch (Exception e) {
            e.printStackTrace();
            File f = new File(filePath);
            // f.delete();
        }
    }

    public void deletePlant(Plant p) {
        deletePlantFileData(p);
        plants.remove(p);
    }

    public void deleteAllPlants() {
        for (Plant p : plants) {
            deletePlantFileData(p);
        }

        plants.clear();

        settingsChanged();
    }

    private void deletePlantFileData(Plant p) {
        File plantFile = new File(plantFolderPath + PLANTS_FOLDER + p.getPlantId() +
                FILE_EXTENSION);

        if (plantFile.exists() && plantFile.isFile()) {
            plantFile.delete();
        }
    }

    public PlantTrackerSettings getPlantTrackerSettings() {
        return settings;
    }

    @Override
    public void plantUpdate(Plant p) {
        savePlant(p);
        uiListener.plantUpdated(p);
    }

    @Override
    public void settingsChanged() {
        savePlantTrackerSettings();
    }

    public Plant getPlantById(long plantId) throws PlantNotFoundException {
        for (Plant p : plants) {
            if (p.getPlantId() == plantId) {
                return p;
            }
        }

        return null;
    }

    public long addGroup(String groupName) {
        return addGroup(System.currentTimeMillis(), groupName);
    }

    public void addGroup(Group g)   {
        settings.addGroup(g);
    }

    private long addGroup(long groupId, String groupName) {
        Group g = new Group(groupId, groupName);
        settings.addGroup(g);

        uiListener.groupsUpdated();

        return g.getGroupId();
    }

    public void removeGroup(long groupId) {
        settings.removeGroup(groupId);
        for (Plant p : getAllPlants()) {
            if (p.getGroups().remove(groupId)) {
                savePlant(p);
            }
        }

        uiListener.groupsUpdated();
    }

    public void addMemberToGroup(long plantId, long groupId) {
        Group g = settings.getGroup(groupId);
        Plant p = getPlantById(plantId);
        p.addGroup(g.getGroupId());
        savePlant(p);
        uiListener.groupsUpdated();
    }

    public void removeMemberFromGroup(long plantId, long groupId) {
        Group g = settings.getGroup(groupId);
        Plant p = getPlantById(plantId);
        p.removeGroup(g.getGroupId());
        savePlant(p);
        uiListener.groupsUpdated();
    }

    public ArrayList<Group> getGroupsPlantIsNotMemberOf(long plantId) {
        Plant p = getPlantById(plantId);
        ArrayList<Group> groups = settings.getGroups();
        ArrayList<Group> nonMemberGroups = new ArrayList<>();

        for (Group g : groups) {
            if (!p.getGroups().contains(g.getGroupId())) {
                nonMemberGroups.add(g);
            }
        }

        return nonMemberGroups;
    }

    public ArrayList<Group> getGroupsPlantIsMemberOf(long plantId) {
        Plant p = getPlantById(plantId);
        ArrayList<Group> groups = new ArrayList<>();

        for (long groupId : p.getGroups()) {
            groups.add(settings.getGroup(groupId));
        }

        return groups;
    }

    public Group getGroup(long groupId) {
        return settings.getGroup(groupId);
    }

    public ArrayList<Group> getAllGroups() {
        return settings.getGroups();
    }

    public final ArrayList<Group> getEmptyGroups() {
        ArrayList<Group> groups = settings.getGroups();
        ArrayList<Group> emptyGroups = new ArrayList<>();

        for (Group g : groups) {
            if (getMemberCountOfGroup(g.getGroupId()) == 0) {
                emptyGroups.add(g);
            }
        }

        return emptyGroups;
    }

    public final ArrayList<Group> getNonEmptyGroups() {
        ArrayList<Group> groups = new ArrayList<>();
        ArrayList<Group> emptyGroups = getEmptyGroups();

        groups.addAll(settings.getGroups());

        groups.removeAll(emptyGroups);

        return groups;
    }

    private int getMemberCountOfGroup(long groupId) {
        int total = 0;
        ArrayList<Plant> activeGroupMembers = new ArrayList<>();
        for (Plant p : getActivePlants()) {
            if (p.getGroups().contains(groupId)) {
                total++;
            }
        }

        return total;
    }

    public ArrayList<Plant> getMembersOfGroup(long groupId) {
        ArrayList<Plant> activeGroupMembers = new ArrayList<>();
        for (Plant p : getActivePlants()) {
            if (p.getGroups().contains(groupId)) {
                activeGroupMembers.add(p);
            }
        }

        return activeGroupMembers;
    }

    public TreeMap<String, Long> getAvailableGroupsForPlant(Plant p)   {
        TreeMap<String, Long> availableGroups = new TreeMap<>();

        for (Long key : p.getGroups()) {
            Group g = getGroup(key);
            if (g != null)  {
                availableGroups.put(g.getGroupName(), g.getGroupId());
            }
        }

        return availableGroups;
    }

    public void renameGroup(long groupId, String name) {
        Group g = getGroup(groupId);
        g.setGroupName(name);
        settingsChanged();
    }

    public void performEventForPlantsInGroup(long groupId, PlantAction action) {
        ArrayList<Plant> plants = getMembersOfGroup(groupId);
        for (Plant p : plants) {
            action.runAction(p);
        }
    }

    public void setPlantTrackerListener(IPlantTrackerListener listener) {
        uiListener = listener;
    }

    public void removePlantState(String key) {
        getPlantTrackerSettings().removeStateAutoComplete(key);

        settingsChanged();
    }

    public void addGenericRecordTemplate(GenericRecord record) {
        settings.addGenericRecordTemplate(record);
    }

    public Set<String> getGenericRecordTypes() {
        return settings.getGenericRecordNames();
    }

    public GenericRecord getGenericRecordTemplate(String name) {
        return settings.getGenericRecordTemplate(name);
    }

    public GenericRecord getGenericRecordTemplate(long id)  {
        return settings.getGenericRecordTemplate(id);
    }

    public void removeGenericRecordTemplate(String name) {
        settings.removeGenericRecordTemplate(name);
    }

    public void setPlantTrackerSettings(PlantTrackerSettings plantTrackerSettings) {
        settings = plantTrackerSettings;
    }

    public final TreeMap<String, GenericRecord> getAllRecordTemplates() {
        return settings.getAllGenericRecordTemplates();
    }

    public void deleteRecordFromAllPlants(GenericRecord rec) {
        boolean saveRequired = false;

        for(Plant p : plants)   {
            if (p.getPlantData().genericRecords.contains(rec))  {
                p.removeGenericRecord(rec);
                saveRequired = true;
            }
        }

        //TODO make this more efficient, we can save plant ids above and just save those plants!
        if (saveRequired)   {
            saveAllPlants();
        }
    }

    public void replacePlantData(ArrayList<Plant> allPlants) {
        for(Plant p : allPlants)    {
            Plant destPlant = getPlantById(p.getPlantId());
            destPlant.setPlantData(p.getPlantData());
        }
    }

    public void dismantle() {
        plants.clear();
        settings.removeListener();
        settings = null;
    }

    public void copyPlant(long plantId, int plantCount) {
        Plant sourcePlant = getPlantById(plantId);

        Gson g = new Gson();
        String json = g.toJson(sourcePlant.getPlantData());

        String plantNameBaseString = "";

        Pattern ptrn = Pattern.compile("\\.\\d+$");
        Matcher m = ptrn.matcher(sourcePlant.getPlantName());
        boolean foundAMatch = m.find();

        int slidingCount = 0;
        if (foundAMatch)   {
            plantNameBaseString = sourcePlant.getPlantName().substring(
                    0, sourcePlant.getPlantName().lastIndexOf('.'));

            int baseCount = Integer.parseInt(sourcePlant.getPlantName().substring(
                    sourcePlant.getPlantName().lastIndexOf('.')+1));

            slidingCount = baseCount + 1;
        }
        else    {
            plantNameBaseString = sourcePlant.getPlantName();
            slidingCount = 1;
        }

        for(int x=0; x < plantCount; x++)   {
            Plant p = getPlantFromJson(json);

            p.getPlantData().plantId = System.currentTimeMillis();

            String pName = plantNameBaseString + "." + slidingCount++;

            p.getPlantData().plantName = pName;
            savePlant(p);
            plants.add(p);

            try {
                // we're only doing this to try to ensure we're getting unique plantIds
                Thread.sleep(10);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
