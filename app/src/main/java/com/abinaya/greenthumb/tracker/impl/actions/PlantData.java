package com.abinaya.greenthumb.tracker.impl.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

public class PlantData implements Serializable  {
    public long plantId;
    public long parentPlantId;
    public String plantName;
    public Calendar startDate;
    public boolean isFromSeed;
    public boolean isArchived;
    public Calendar currentStateStartDate;
    public String currentStateName;
    public ArrayList<GenericRecord> genericRecords;
    public ArrayList<Long> groupIds;
    public String thumbnail;

    public PlantData()  {
        genericRecords = new ArrayList<>();
        groupIds = new ArrayList<>();
    }
}
