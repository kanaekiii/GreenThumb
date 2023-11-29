package com.abinaya.greenthumb.tracker.interf;

import com.abinaya.greenthumb.tracker.impl.actions.Plant;

public interface IPlantTrackerListener {

    public void plantUpdated(Plant p);
    public void groupsUpdated();

}
