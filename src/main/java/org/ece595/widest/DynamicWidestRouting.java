package org.ece595.widest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onosproject.common.DefaultTopology;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DynamicWidestRouting {

    private HostService hostService;
    private DeviceService deviceService;
    private TopologyService topologyService;
    private boolean isDynamic;


    public DynamicWidestRouting(HostService hostService, TopologyService topologyService, DeviceService deviceService) {
        this.hostService = hostService;
        this.topologyService = topologyService;
        this.deviceService = deviceService;
        linkBandWidth = new LinkBandWidthTool(deviceService);
        isDynamic = true;
    }

    public void setStatic(){
        isDynamic = false;
    }
    public void setDynamic(){
        isDynamic = true;
    }


    private final ProviderId routeProviderId = new ProviderId("ECE595", "SGT,FHF");
    private final Logger log = LoggerFactory.getLogger(getClass());

    private LinkBandWidthTool linkBandWidth;


    //return one path in fact
    //use Set to be compatible with ONOS API
    public Set<Path> getPaths(ElementId src, ElementId dst) {
        Topology topo = topologyService.currentTopology();

        if (src instanceof DeviceId && dst instanceof DeviceId) {
            // no edge link.

            Set<List<TopologyEdge>> allRoutes = findAllRoutes(topo, (DeviceId) src, (DeviceId) dst);

            Set<Path> allPaths = calculateRoutesCost(allRoutes);

            Path linkPath = selectRoute(allPaths);

            return linkPath != null ? ImmutableSet.of(linkPath) : ImmutableSet.of();

        } else if (src instanceof HostId && dst instanceof HostId) {

            Host srcHost = hostService.getHost((HostId) src);
            Host dstHost = hostService.getHost((HostId) dst);
            if (srcHost == null || dstHost == null) {
                log.warn("Generate whole path but found null, hostSrc:{}, hostDst:{}", srcHost, dstHost);
                return ImmutableSet.of();
            }
            EdgeLink srcLink = getEdgeLink(srcHost, true);
            EdgeLink dstLink = getEdgeLink(dstHost, false);

            Set<List<TopologyEdge>> allRoutes = findAllRoutes(topo, srcLink.dst().deviceId(), dstLink.src().deviceId());

            Set<Path> allPaths = calculateRoutesCost(allRoutes);
            //log.info("\n ====all Paths:" + allPaths.toString() );


            Path linkPath = selectRoute(allPaths);
            //log.info("\n ====linkPath:" + linkPath.toString() );

            Path wholePath = buildWholePath(srcLink, dstLink, linkPath);
            //extract linkPath links and combine to a whole Array of Link, then transform to Path
            //log.info("\n ====WholePath:" + wholePath.toString() );

            return wholePath != null ? ImmutableSet.of(wholePath) : ImmutableSet.of();

        } else {

            return ImmutableSet.of();
        }
    }

    /**
     * Generate EdgeLink which is between Host and Device.
     *
     * @param host
     * @param isIngress whether it is Ingress to Device or not.
     * @return
     */
    private EdgeLink getEdgeLink(Host host, boolean isIngress) {
        return new DefaultEdgeLink(routeProviderId, new ConnectPoint(host.id(), PortNumber.portNumber(0)),
                                   host.location(), isIngress);
    }

    /**
     * find all route between Src and Dst.
     * Use "route" to mean a list of edges, Path is a route with cost
     *
     */
    private Set<List<TopologyEdge>> findAllRoutes(Topology topo, DeviceId src, DeviceId dst) {
        if (!(topo instanceof DefaultTopology)) {
            log.error("topology is not the object of DefaultTopology.");
            return ImmutableSet.of(); //return empty set
        }

        Set<List<TopologyEdge>> graghResult = new HashSet<>();  //a set of path (all routes)
        dfsFindAllRoutes(new DefaultTopologyVertex(src), new DefaultTopologyVertex(dst),
                         new ArrayList<>(), new ArrayList<>(),
                         ((DefaultTopology) topo).getGraph(), graghResult);

        return graghResult;
    }

    /**
     * Get all possible path between Src and Dst using DFS
     */

    private void dfsFindAllRoutes(TopologyVertex src,
                                  TopologyVertex dst,
                                  List<TopologyEdge> passedLink,
                                  List<TopologyVertex> passedDevice,
                                  TopologyGraph topoGraph,
                                  Set<List<TopologyEdge>> result) {
        if (src.equals(dst)) {
            return;
        }

        passedDevice.add(src);

        Set<TopologyEdge> egressSrc = topoGraph.getEdgesFrom(src);
        egressSrc.forEach(egress -> {
            TopologyVertex vertexDst = egress.dst();
            if (vertexDst.equals(dst)) {
                //Gain a Path
                passedLink.add(egress);
                result.add(ImmutableList.copyOf(passedLink.iterator()));
                passedLink.remove(egress);

            } else if (!passedDevice.contains(vertexDst)) {
                //DFS into
                passedLink.add(egress);
                dfsFindAllRoutes(vertexDst, dst, passedLink, passedDevice, topoGraph, result);
                passedLink.remove(egress);

            } else {
                //means - passedDevice.contains(vertexDst)
                //We hit a loop, NOT go into
            }
        });

        passedDevice.remove(src);
    }


    private Set<Path> calculateRoutesCost(Set<List<TopologyEdge>> routes) {

        Set<Path> paths = new HashSet<>();
        routes.forEach(route -> {
            //cost of the route/path
            double cost = -bottleNeckBandwidth(route);  //the more bw, the less cost

            //route to links
            ArrayList<Link> links = new ArrayList();
            route.forEach(edge -> links.add(edge.link()));

            //links to path
            paths.add(new DefaultPath(routeProviderId, links, cost));
        });

        return paths;
    }

    private double bottleNeckBandwidth(List<TopologyEdge> edges) {
        //the smallest remaining is the bottleneck
        double min = Double.MAX_VALUE;
        for (TopologyEdge edge : edges) {
            double bw;
            if(isDynamic) {
                bw = this.linkBandWidth.getRemain(edge.link());
            }else {
                bw = this.linkBandWidth.getCapacity(edge.link());
            }
            min = bw < min ? bw : min;
        }
        return min;
    }

    private Path selectRoute(Set<Path> paths) {
        if (paths.size() < 1) {
            return null;
        }
        return getMinHopPath(getMinCostPath(new ArrayList(paths)));
    }

    private List<Path> getMinCostPath(List<Path> paths) {
        final double measureTolerance = 0.2; //Mbps
        //Sort by Cost in ascending order
        paths.sort((p1, p2) -> p1.cost() > p2.cost() ? 1 : (p1.cost() < p2.cost() ? -1 : 0));
        // get paths with similar lowest cost within measureTolerance range.
        List<Path> minCostPaths = new ArrayList<>();
        Path minCostPath = paths.get(0);
        minCostPaths.add(minCostPath);
        for (int i = 1, pathCount = paths.size(); i < pathCount; i++) {
            Path p = paths.get(i);
            if (p.cost() - minCostPath.cost() < measureTolerance) {
                minCostPaths.add(p);
            }else {
                break;
            }
        }
        return minCostPaths;
    }

    private Path getMinHopPath(List<Path> paths) {
        Path result = paths.get(0);
        for (int i = 1, pathCount = paths.size(); i < pathCount; i++) {
            Path p = paths.get(i);
            result = result.links().size() > p.links().size() ? p : result;
        }
        return result;
    }

    /**
     * @param srcLink
     * @param dstLink
     * @param linkPath
     * @return At least, Path will include two edge links.
     */
    private Path buildWholePath(EdgeLink srcLink, EdgeLink dstLink, Path linkPath) {
        if (linkPath == null && !(srcLink.dst().deviceId().equals(dstLink.src().deviceId()))) {
            log.warn("no available Path is found!");
            return null;
        }

        return buildEdgeToEdgePath(srcLink, dstLink, linkPath);
    }

    /**
     * Produces a direct edge-to-edge path.
     *
     * @param srcLink
     * @param dstLink
     * @param linkPath
     * @return
     */
    private Path buildEdgeToEdgePath(EdgeLink srcLink, EdgeLink dstLink, Path linkPath) {

        List<Link> links = Lists.newArrayListWithCapacity(2);

        double cost = 0;

        // now, the cost of edge link is 0.
        links.add(srcLink);

        if (linkPath != null) {
            links.addAll(linkPath.links());
            cost += linkPath.cost();
        }

        links.add(dstLink);

        return new DefaultPath(routeProviderId, links, cost);
    }

}
