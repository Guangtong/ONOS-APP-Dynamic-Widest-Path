/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ece595.widest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Component(immediate = true)
public class AppComponent {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IntentService intentService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private ConcurrentMap<Set<Criterion>, Intent> intentMap ;
    private WidestpathPacketProcessor packetProcessor;
    private RealtimeWidestRouting widestPathRouting;

    @Activate
    protected void activate() {

        appId = coreService.registerApplication("org.ece595.widest");
        intentMap = new ConcurrentHashMap<>();
        widestPathRouting = new RealtimeWidestRouting();
        //widestPathRouting = new BasicWidestRouting();

        packetProcessor = new WidestpathPacketProcessor(appId, intentMap, widestPathRouting,intentService);

        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));

        packetService.requestPackets(DefaultTrafficSelector.builder()
                                             .matchEthType(Ethernet.TYPE_IPV4).build(),
                                     PacketPriority.REACTIVE, appId);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        packetService.cancelPackets(DefaultTrafficSelector.builder()
                                            .matchEthType(Ethernet.TYPE_IPV4).build(),
                                    PacketPriority.REACTIVE, appId);

        intentMap.values().forEach(intent -> {
            intentService.withdraw(intent); //this is asynchronous function, not effective immediately
            intentService.purge(intent);
        });
        intentMap.clear();

        log.info("Stopped");

    }
}


