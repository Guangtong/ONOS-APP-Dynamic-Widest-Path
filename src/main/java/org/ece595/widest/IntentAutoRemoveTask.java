package org.ece595.widest;

import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.PathIntent;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by sgt on 4/8/17.
 */

//auto remove an intent when it has no traffic


public class IntentAutoRemoveTask {

    private Logger log;

    private Intent intent;
    private ConcurrentMap<Set<Criterion>, Intent> intentMap;

    private InnnerPeriodicTask prdTask;
    private static final long prd = 1000; //ms

    private IntentService intentService;
    private DeviceService deviceService;
    private LinkBandWidthTool linkBandWidth;

    private int intentFreeTimeCount;

    //this one is for test
    public IntentAutoRemoveTask(Intent intent,
                                IntentService intentService,
                                DeviceService deviceService,
                                ConcurrentMap<Set<Criterion>, Intent> intentMap,
                                Logger log) {
        this(intent,intentService,deviceService, intentMap);
        this.log = log;
    }

    //set timeoutSec = 0 to remove a intent as soon as possible
    public IntentAutoRemoveTask(Intent intent,
                                IntentService intentService,
                                DeviceService deviceService,
                                ConcurrentMap<Set<Criterion>, Intent> intentMap) {
        this.intent = intent;
        this.intentService = intentService;
        this.intentFreeTimeCount = 0;
        this.intentMap = intentMap;
        this.deviceService = deviceService;
        this.linkBandWidth = new LinkBandWidthTool(this.deviceService);

        prdTask = new InnnerPeriodicTask();
    }



    //call this method to set delay seconds
    public void start(int delaySec) {
        prdTask.start(delaySec * 1000);
    }


    //to stop and remove the task
    public void purge() {
        prdTask.prdTimer.cancel();
        prdTask.prdTimer.purge();
    }


    class InnnerPeriodicTask extends TimerTask {

        Timer prdTimer;
        int count;

        InnnerPeriodicTask() {
            this.prdTimer = new Timer();
        }

        void start(long delay) {
            count = 0;
            prdTimer.schedule(prdTask, delay, prd);
        }

        @Override
        public void run() {

            log.info("\n ========= Polling For Intent:" + intent.key() + " count:" + intentFreeTimeCount);
            count++;

            //if free for 5 prds, remove this intent, otherwise return
            if(intentFreeTimeCount < 5) {

                if(measureTraffic(intent) < 0.2) {
                    intentFreeTimeCount++;
                    if(intentFreeTimeCount >=5) {
                        intentService.withdraw(intent);
                    }
                }else {
                    intentFreeTimeCount = 0;
                }
                return;
            }

            //reach here when intentFreeTimeCount >=5
            IntentState state = intentService.getIntentState(intent.key());

            if (state == IntentState.WITHDRAWN || state == IntentState.FAILED) {
                intentService.purge(intent);
                intentMap.remove(((PathIntent)intent).selector().criteria(), intent);
                prdTimer.cancel();
                prdTimer.purge();
            }
        }


        // the min traffic on one link in the intent path is regarded as the path traffic
        private double measureTraffic(Intent intent) {
            List<Link> links = ((PathIntent)intent).path().links();
            double minTraffic = Double.MAX_VALUE;
            for(Link l : links) {
                if(l.type() == Link.Type.DIRECT && l.state() == Link.State.ACTIVE) {    //but l.state() doesn't change when link down in mininet, why?
                    double traffic = linkBandWidth.getUsage(l);
                    if(traffic < minTraffic) {
                        minTraffic = traffic;

                        //for test
                        if(minTraffic < 0.2) {
                            log.info("\n traffic<0.2Mbps: " + l.toString());
                        }
                    }
                }
            }
            return minTraffic;
        }
    };



}
