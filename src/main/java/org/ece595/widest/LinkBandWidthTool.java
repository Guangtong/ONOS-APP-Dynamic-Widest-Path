package org.ece595.widest;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;


/**
 * Created by sgt on 4/6/17.
 */

//We use Mbps as uinit

public class LinkBandWidthTool {
    private DeviceService deviceService;
    private final Logger log = LoggerFactory.getLogger(getClass());
    public LinkBandWidthTool(DeviceService deviceService) {
        this.deviceService = deviceService;
    }



    public double getRemain(Link l) {

        if (l.state() == Link.State.INACTIVE) {
            return 0;
        }

        double remain = getCapacity(l) - getUsage(l);

//        log.info("\n ====Edge:" + l.toString() +
//                 "\n ====capacity:" + getCapacity(l) +
//                 "\n ====Usage:" + getUsage(l));


        if (remain < 0) {
            return 0;
        }else {
            return remain;
        }
    }

    public double getCapacity(Link l) {
        if (l.state() == Link.State.INACTIVE) {
            return 0;
        }
        ConnectPoint src = l.src();
        ConnectPoint dst = l.dst();

        double srcPortBw = deviceService.getPort(src).portSpeed() ;  //Mbps
        double dstPortBw = deviceService.getPort(dst).portSpeed() ;

        //return Math.min(srcPortBw, dstPortBw);

        //in our simulation, we read link bw from a file because ovsk in mininet always return 10Gbps BW

        Random r = new Random();

        //return 1 + 4 * r.nextDouble();   //r.nextDouble() return 0~1

        return 10;

    }

    public double getUsage(Link l) {

        if (l.state() == Link.State.INACTIVE) {
            return 0;
        }
        ConnectPoint src = l.src();
        ConnectPoint dst = l.dst();
        double srcBw = 0;
        double dstBw = 0;

        if(src.elementId() instanceof DeviceId) {
            PortStatistics stat = deviceService.getDeltaStatisticsForPort(src.deviceId(), src.port());

//            srcBw = stat.bytesSent() * 8.0e3 /stat.durationNano();  //bytes * 8bit/byte * 1e-6Mbit/bit / (nano * 1e-9s/nano) = bytes / nano * 8000 (Mbps)
//
//            log.info("\n ====Edge" + edge.link().toString() +
//                    "\n ====Stat:" + stat.toString() +
//                    "\n ====DurationNano:" + stat.durationNano());
            // durationNano is not correct: 0 or 999000000
            // durationSec is not accurate: 4 or 5

            //I decide to use 5 all the time

            srcBw = stat.bytesSent() * 8.0 / (5 * 1024 * 1024);

        }
        if(dst.elementId() instanceof DeviceId) {
            PortStatistics stat = deviceService.getDeltaStatisticsForPort(dst.deviceId(), dst.port());
            dstBw = stat.bytesReceived() * 8.0 / (5 * 1024 * 1024);
        }
        return Math.max(srcBw, dstBw);

    }
}
