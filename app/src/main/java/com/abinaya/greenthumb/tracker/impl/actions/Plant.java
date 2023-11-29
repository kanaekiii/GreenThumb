package com.abinaya.greenthumb.tracker.impl.actions;

import com.abinaya.greenthumb.tracker.interf.IPlantUpdateListener;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;

@SuppressWarnings("Since15")
public class Plant implements Serializable  {

    private static final long serialVersionUID = 2153335460648792L;

    private PlantData plantData;

    private transient ArrayList<IPlantUpdateListener> updateListeners;

    public Plant()  {
        updateListeners = new ArrayList<>();
    }

    public Plant(Calendar growStartDate, String plantName, boolean isFromSeed) {
        updateListeners = new ArrayList<>();

        plantData = new PlantData();
        plantData.plantName = plantName;
        plantData.plantId = System.currentTimeMillis();
        plantData.startDate = growStartDate;
        plantData.isFromSeed = isFromSeed;
        plantData.groupIds = new ArrayList<>();
    }

    public void addUpdateListener(IPlantUpdateListener pul) {
        if (updateListeners == null)    {
            updateListeners = new ArrayList<>();
        }

        updateListeners.add(pul);
    }

    public String getPlantName()    {
        return plantData.plantName;
    }

    public void setPlantName(String name) {
        plantData.plantName = name;

        notifyUpdateListeners();
    }

    public void setParentPlantId(long id) {
        plantData.parentPlantId = id;
    }

    public long getParentPlantId()  {
        return plantData.parentPlantId;
    }

    public long getPlantId()    {
        return plantData.plantId;
    }

    public boolean isFromSeed()
    {
        return plantData.isFromSeed;
    }

    public ArrayList<GenericRecord> getAllGenericRecords()  {
        ArrayList<GenericRecord> rec = new ArrayList<>();

        for(GenericRecord er : plantData.genericRecords)    {
            rec.add((GenericRecord)er);
        }

        return sortEvents(rec);
    }

    public GenericRecord getPhaseChangeRecord() {
        GenericRecord record = new GenericRecord("Changing Phase");
        record.setDataPoint("Phase Name", "");
        record.summaryTemplate = "Plant entered a new phase, {Phase Name}";

        return record;
    }

    public void finalizeRecord(GenericRecord record)    {

        record.hasImages = (record.images != null  && record.images.size() > 0);
        record.hasDataPoints = (record.dataPoints != null && record.dataPoints.size() > 0);

        plantData.genericRecords.add(record);

        sortEvents();
        updateSummaryInformation();
        notifyUpdateListeners();
    }

    // Days//Weeks//Counts

    public long getDaysFromStart()   {
        return getDaysFromStart(Calendar.getInstance());
    }

    public long getDaysFromStart(Calendar c)    {
        return Utility.calcDaysFromTime(plantData.startDate, c);
    }

    public long getWeeksFromStart()   {
        return getWeeksFromStart(plantData.startDate);
    }

    public long getWeeksFromStart(Calendar c)   {
        return Utility.calcWeeksFromTime(plantData.startDate, c);
    }

    public Calendar getPlantStartDate() {
        return plantData.startDate;
    }

    public void changePlantStartDate(Calendar startDate)    {
        plantData.startDate = startDate;

        updateSummaryInformation();
        notifyUpdateListeners();
    }

    // plant object maintenance
    private void sortEvents()   {
        plantData.genericRecords.sort(new Comparator<GenericRecord>() {
            @Override
            public int compare(GenericRecord o1, GenericRecord o2) {
                return o1.time.compareTo(o2.time);
            }
        });
    }
	
    private ArrayList<GenericRecord> sortEvents(ArrayList<GenericRecord> records) {
        records.sort(new Comparator<GenericRecord>() {
            @Override
            public int compare(GenericRecord o1, GenericRecord o2) {
                return o1.time.compareTo(o2.time);
            }
        });

        return records;
    }

    public void removeGenericRecord(GenericRecord rec)  {
        plantData.genericRecords.remove(rec);

        updateSummaryInformation();
        notifyUpdateListeners();
    }

    public void removeGenericRecord(int recordPosition)    {
        plantData.genericRecords.remove(recordPosition);

        updateSummaryInformation();
        notifyUpdateListeners();
    }

    private void updateSummaryInformation() {
        Calendar nearestPhaseDate = null;
        int phaseCount = 0;

        sortEvents();

        for(GenericRecord record : plantData.genericRecords)    {
            // set plant phase
            if (record.dataPoints.containsKey("Phase Name")) {
                plantData.currentStateName = (String)record.dataPoints.get("Phase Name");
                plantData.currentStateStartDate = record.time;
                nearestPhaseDate = record.time;
                phaseCount++;
            }

            if (nearestPhaseDate != null)   {
                record.phaseCount = phaseCount;
                record.weeksSincePhase = Utility.calcWeeksFromTime(nearestPhaseDate, record.time);
            }

            record.weeksSinceStart = Utility.calcWeeksFromTime(getPlantStartDate(),
                    record.time);

            if (record.images != null)  {
                if (record.images.size() > 0)   {
                    plantData.thumbnail = record.images.get(record.images.size()-1);
                }
            }

            // TODO update other plant summary fields
        }
    }

    private void notifyUpdateListeners()    {
        if (updateListeners != null)    {
            for(IPlantUpdateListener pul : updateListeners) {
                pul.plantUpdate(this);
            }
        }
    }

    public ArrayList<Long> getUniqueRecordTemplatesUsed()  {
        ArrayList<Long> uniqueRecords = new ArrayList<>();

        for(GenericRecord gr : plantData.genericRecords)    {
            if (!uniqueRecords.contains(gr.id))   {
                uniqueRecords.add(gr.id);
            }
        }

        return uniqueRecords;
    }

    public void archivePlant() {
        plantData.isArchived = true;
        notifyUpdateListeners();
    }

    public void unarchivePlant()   {
        plantData.isArchived = false;
        notifyUpdateListeners();
    }

    public boolean isArchived() {
        return plantData.isArchived;
    }

    public void regeneratePlantId() {
        plantData.plantId = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object p)  {
        return ((Plant)p).getPlantId() == getPlantId();
    }

    public void addGroup(long groupId)  {
        if (!plantData.groupIds.contains(groupId))    {
            plantData.groupIds.add(groupId);
        }
    }

    public void removeGroup(long groupId)   {
        plantData.groupIds.remove(groupId);
    }

    public ArrayList<Long> getGroups() {
        return plantData.groupIds;
    }

    public boolean isMemberOfGroup(Long groupId)    {
        return plantData.groupIds.contains(groupId);
    }

    public String getCurrentStateName() {
        return plantData.currentStateName;
    }

    public PlantData getPlantData() {
        return plantData;
    }

    public void setPlantData(PlantData pd)  {
        plantData = pd;
    }

    public void plantLoadFinished() {
        updateSummaryInformation();
    }

    public String getThumbnail()    {
        return plantData.thumbnail;
    }

    public ArrayList<String> getAllImagesForPlant() {
        ArrayList<String> files = new ArrayList<>();

        for(GenericRecord rec : plantData.genericRecords)   {
            if (rec.images != null && rec.images.size() > 0)    {
                files.addAll(rec.images);
            }
        }

        return files;
    }

    public void buildRecordStampForRecord(GenericRecord rec)  {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy",
                Locale.getDefault());
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
    }

    public void generateRecordStampData(GenericRecord rec) {
        boolean lookAtNextPhaseChange = true;
        boolean foundNextPhaseChange = false;
        int phaseCount = 0;

        for(int x=plantData.genericRecords.size()-1; x >= 0; x--) {
            GenericRecord r = plantData.genericRecords.get(x);

            // find the next earliest phase
            if (lookAtNextPhaseChange)  {
                if (r.time.compareTo(rec.time) == -1 && r.getDataPoint("Phase Name") != null) {
                    rec.weeksSincePhase = Utility.calcWeeksFromTime(r.time, rec.time);
                    lookAtNextPhaseChange = false;
                    foundNextPhaseChange = true;
                }
            }

            // count phases to the beginning to see what the phase we were looking for is numbered
            if (foundNextPhaseChange)   {
                if (r.getDataPoint("Phase Name") != null) {
                    phaseCount++;
                }
            }
        }

        rec.phaseCount = phaseCount;
        rec.weeksSinceStart = Utility.calcWeeksFromTime(plantData.startDate, rec.time);
    }
}
