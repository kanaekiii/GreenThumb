package com.abinaya.greenthumb.tracker.impl.actions;

import com.abinaya.greenthumb.tracker.impl.actions.GenericRecord;
import com.abinaya.greenthumb.tracker.impl.actions.Plant;


public class PlantAction {

    private GenericRecord record;

    public PlantAction(GenericRecord record)  {
        this.record = record;
    }

    public void runAction(Plant p) {
        p.generateRecordStampData(record);
        p.buildRecordStampForRecord(record);
        p.finalizeRecord(record);
    }
}
