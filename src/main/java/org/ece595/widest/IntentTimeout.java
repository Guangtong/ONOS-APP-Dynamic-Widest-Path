package org.ece595.widest;

import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.PathIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by sgt on 4/8/17.
 */
//used to remove intent after the specified timeout



public class IntentTimeout{

    private Logger log;

    private Intent intent;
    private ConcurrentMap<Set<Criterion>, Intent> intentMap;

    private PollIntentStateTask task;
    private static final long pollingPeriod = 1000; //ms

    private IntentService intentService;
    private boolean isOut = false;

    //this one is for test
    public IntentTimeout(Intent intent, IntentService intentService, ConcurrentMap<Set<Criterion>, Intent> intentMap, Logger log) {
        this(intent,intentService,intentMap);
        this.log = log;
    }

    //set timeoutSec = 0 to remove a intent as soon as possible
    public IntentTimeout(Intent intent, IntentService intentService, ConcurrentMap<Set<Criterion>, Intent> intentMap) {
        this.intent = intent;
        this.intentService = intentService;
        this.isOut = false;
        this.intentMap = intentMap;

        task = new PollIntentStateTask();
    }

    //call this method to set timeout seconds
    public void schedule(int timeoutSec) {
        long timeoutPeriod = timeoutSec * 1000;
        task.start(timeoutPeriod);
    }

    public void purge() {
        task.pollingTimer.cancel();
        task.pollingTimer.purge();
    }




    class PollIntentStateTask extends TimerTask {

        Timer pollingTimer;
        int count;

        PollIntentStateTask() {
            this.pollingTimer = new Timer();
        }

        void start(long timeoutPeriod) {
            count = 0;
            pollingTimer.schedule(task, timeoutPeriod, pollingPeriod);
        }

        @Override
        public void run() {

            log.info("\n ========= Polling For Intent:" + intent.key() + " count:" + count);
            count++;

            if(!isOut) {
                intentService.withdraw(intent);
                isOut = true;
                return;
            }
            //isOut -> polling state

            IntentState state = intentService.getIntentState(intent.key());

            if (state == IntentState.WITHDRAWN || state == IntentState.FAILED) {
                intentService.purge(intent);
                intentMap.remove(((PathIntent)intent).selector().criteria(), intent);
                pollingTimer.cancel();
                pollingTimer.purge();
            }
        }
    };



}
