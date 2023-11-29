package com.abinaya.greenthumb;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.BuildConfig;
//import org.acra.annotation.AcraCore;
//import org.acra.annotation.AcraDialog;
//import org.acra.annotation.AcraMailSender;

//@AcraCore(buildConfigClass = BuildConfig.class)
//@AcraMailSender(mailTo = "raghuramanabinaya2812@gmail.com")
//@AcraDialog(resText = R.string.dialog_text, resCommentPrompt = R.string.dialog_comment)

public class AndroidPlantTracker extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
