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


/**
 * Created by sgt on 4/6/17.
 */

//We use Mbps as uinit

public class EdgeBandWidth {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public double getRemain(TopologyEdge edge) {

        if (edge.link().state() == Link.State.INACTIVE) {
            return 0;
        }

        double remain = getCapacity(edge) - getUsage(edge);

        log.info("=============capacity:" + getCapacity(edge) + "\n" +
                 "=============Usage:" + getUsage(edge) + "\n" +
                 "edge" + edge.link().toString());


        if (remain < 0) {
            return 0;
        }else {
            return remain;
        }
    }

    public double getCapacity(TopologyEdge edge) {
        if (edge.link().state() == Link.State.INACTIVE) {
            return 0;
        }
        ConnectPoint src = edge.link().src();
        ConnectPoint dst = edge.link().dst();

        long srcPortBw = deviceService.getPort(src).portSpeed();  //Mbps
        long dstPortBw = deviceService.getPort(dst).portSpeed();

        return (double)Math.min(srcPortBw, dstPortBw);

        //in our simulation, we read link bw from a file because ovsk in mininet always return 10000 Mpbs


    }

    public double getUsage(TopologyEdge edge) {

        if (edge.link().state() == Link.State.INACTIVE) {
            return 0;
        }
        ConnectPoint src = edge.link().src();
        ConnectPoint dst = edge.link().dst();
        double srcBw = 0;
        double dstBw = 0;


        if(src.elementId() instanceof DeviceId) {
            PortStatistics stat = deviceService.getDeltaStatisticsForPort(src.deviceId(), src.port());
            srcBw = (stat.bytesSent() >> 20) * 8.0 /stat.durationSec();
        }
        if(dst.elementId() instanceof DeviceId) {
            PortStatistics stat = deviceService.getDeltaStatisticsForPort(dst.deviceId(), dst.port());
            dstBw = (stat.bytesReceived() >> 20) * 8.0 /stat.durationSec();
        }


        return Math.max(srcBw, dstBw);

    }
}
