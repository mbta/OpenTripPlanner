package org.opentripplanner.routing.edgetype;

import java.util.Locale;
import org.opentripplanner.model.Stop;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * A transit vehicle's journey between departure at one stop and arrival at the next.
 * This version represents a set of such journeys specified by a TripPattern.
 */
public class PatternHop extends TablePatternEdge implements OnboardEdge, HopEdge {

    private static final long serialVersionUID = 1L;

    private Stop begin, end;

    public int stopIndex;

    private LineString geometry = null;

    public PatternHop(PatternStopVertex from, PatternStopVertex to, Stop begin, Stop end, int stopIndex) {
        super(from, to);
        this.begin = begin;
        this.end = end;
        this.stopIndex = stopIndex;
        getPattern().setPatternHop(stopIndex, this);
    }

    public double getDistance() {
        return SphericalDistanceLibrary.distance(begin.getLat(), begin.getLon(), end.getLat(),
                end.getLon());
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(getPattern().route);
    }
    
    public String getName() {
        return GtfsLibrary.getRouteName(getPattern().route);
    }
    
    @Override
    public String getName(Locale locale) {
        return this.getName();
    }

    public State optimisticTraverse(State state0) {
        RoutingRequest options = state0.getOptions();
        
        // Ignore this edge if either of its stop is banned hard
        if (!options.bannedStopsHard.isEmpty()) {
            if (options.bannedStopsHard.matches(((PatternStopVertex) fromv).getStop())
                    || options.bannedStopsHard.matches(((PatternStopVertex) tov).getStop())) {
                return null;
            }
        }

        for (AlertPatch alertPatch: options.getRoutingContext().graph.getAlertPatches(this)) {
            if (alertPatch.cannotRideThrough() && alertPatch.displayDuring(state0)) {
                return null;
            }
        }

    	int runningTime = getPattern().scheduledTimetable.getBestRunningTime(stopIndex);
    	StateEditor s1 = state0.edit(this);
    	s1.incrementTimeInSeconds(runningTime);
    	s1.setBackMode(getMode());
    	s1.incrementWeight(runningTime);
    	return s1.makeState();
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return getPattern().scheduledTimetable.getBestRunningTime(stopIndex);
    }
    
    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }
    
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        
        // Ignore this edge if either of its stop is banned hard
        if (!options.bannedStopsHard.isEmpty()) {
            if (options.bannedStopsHard.matches(((PatternStopVertex) fromv).getStop())
                    || options.bannedStopsHard.matches(((PatternStopVertex) tov).getStop())) {
                return null;
            }
        }

        for (AlertPatch alertPatch: options.getRoutingContext().graph.getAlertPatches(this)) {
            if (alertPatch.cannotRideThrough() && alertPatch.displayDuring(s0)) {
                return null;
            }
        }
        
        TripTimes tripTimes = s0.getTripTimes();
        int runningTime = tripTimes.getRunningTime(stopIndex);
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(runningTime);
        if (s0.getOptions().arriveBy)
            s1.setZone(getBeginStop().getZoneId());
        else
            s1.setZone(getEndStop().getZoneId());
        //s1.setRoute(pattern.getExemplar().route.getId());
        s1.incrementWeight(runningTime);
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    public void setGeometry(LineString geometry) {
        this.geometry = geometry;
    }

    public LineString getGeometry() {
        if (geometry == null) {

            Coordinate c1 = new Coordinate(begin.getLon(), begin.getLat());
            Coordinate c2 = new Coordinate(end.getLon(), end.getLat());

            geometry = GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] { c1, c2 });
        }
        return geometry;
    }

    @Override
    public Stop getEndStop() {
        return end;
    }

    @Override
    public Stop getBeginStop() {
        return begin;
    }

    public String toString() {
    	return "PatternHop(" + getFromVertex() + ", " + getToVertex() + ")";
    }

    @Override
    public int getStopIndex() {
        return stopIndex;
    }
}
