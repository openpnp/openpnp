import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.jcodec.api.awt.SequenceEncoder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferencePnpJobProcessor;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.spi.Locatable.LocationOption;
import org.openpnp.spi.PnpJobPlanner;
import org.openpnp.spi.PnpJobProcessor.JobPlacement;
import org.openpnp.spi.PnpJobPlanner.PlannedPlacement;
import org.openpnp.spi.PnpJobPlanner.PlannerStepResults;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.Utils2D;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Part;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Placement;
import org.openpnp.model.Location;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import com.google.common.io.Files;
import com.google.common.eventbus.Subscribe;

public class JobProcessorTest {
    String testName;
    File workingDirectory;
    private ReferenceMachine machine;
    private ReferencePnpJobProcessor jobProcessor;
    private Job job;
    private List<PnpJobPlanner.PlannerStepResults> results;


    // NB run individual methods with:
    // mvn test -Dtest=JobProcessorTest#testNozzleTips

    @Test
    public void testNozzleTips() throws Exception {
        // This is the baseline test in the default configuration.
        // 'Nozzle tips' job oder combined with 'Minimize' is expected to be optimal performance.
        setup("testNozzleTips");
        assertEquals(jobProcessor.getJobOrder(),ReferencePnpJobProcessor.JobOrderHint.NozzleTips);
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(3, tipChanges());
        assertEquals(1.6, utilisation(), 0.01);
        assertEquals(60, cycleCount(), 1);
        assertEquals(1.56, averagePlanningCost(), 0.01);
        assertEquals(13, partChanges(), 5);
    }

    @Test
    public void testStartAsPlanned() throws Exception {
        // 'Start As Planned' is expected to be identical to 'Nozzle Tips' because
        // this test machine is configured with no tips loaded before the job
        setup("testStartAsPlanned");
        jobProcessor.planner.setStrategy(PnpJobPlanner.Strategy.StartAsPlanned);
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(3, tipChanges());
        assertEquals(1.6, utilisation(), 0.01);
        assertEquals(60, cycleCount(), 1);
        assertEquals(1.56, averagePlanningCost(), 0.01); // Efficiency unchanged
        assertEquals(13, partChanges(), 5);
    }

    @Test
    public void testFeederFocus() throws Exception {
        // The Feeder Focus option restricts the planner to select parts from the same
        // feeder as the first on its preference list. This causes it to empty one feeder
        // before moving on to the next. This increases planning cost,
        // because the planner has fewer options that it can consider.
        setup("testFeederFocus");
        jobProcessor.planner.setFeederStrategy(PnpJobPlanner.FeederStrategy.FeederFocus);
        run();
        saveCsv();
        checkRanks();
        assertEquals(3, tipChanges());
        assertEquals(1.6, utilisation(), 0.01);
        assertEquals(60, cycleCount(), 1);
        assertEquals(1.73, averagePlanningCost(), 0.01); // Efficiency somewhat worse
        assertEquals(9, partChanges()); // This is optimal
    }

    @Test
    public void testBoardPart() throws Exception {
        // 'Board:Part' sequence is a 'legacy' job order option
        setup("testBoardPart");
        jobProcessor.setJobOrder(ReferencePnpJobProcessor.JobOrderHint.BoardPart);
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(3, tipChanges());
        assertEquals(1.6, utilisation(), 0.01);
        assertEquals(60, cycleCount(), 1);
        assertEquals(1.42, averagePlanningCost(), 0.01); // This is not too bad really
        assertEquals(46, partChanges(), 5); // it cycles across all feeders as it works across all boards
    }

    @Test
    public void testUnsorted() throws Exception {
        // This test proves that the original panel and board placement order is preserved.
        // The correct operation has been confirmed by reviewing the csv file, and the numbers
        // in the assertions below confirm it is unchanged.
        // NB the original order is not quite *exactly* preserved, because there is a TSM which
        // optimises which nozzle gets placed first. But thats ok.
        setup("Unsorted");
        jobProcessor.setJobOrder(ReferencePnpJobProcessor.JobOrderHint.Unsorted);
        jobProcessor.planner.setStrategy(PnpJobPlanner.Strategy.FullyAsPlanned);
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(33, tipChanges());
        assertEquals(1.95, utilisation(), 0.01);
        assertEquals(49, cycleCount(), 1);
        assertEquals(0, averagePlanningCost()); // there is no planning
        assertEquals(72, partChanges(), 5);
    }

    @Test
    public void testFlexibility() throws Exception {
        // 'Nozzle Tips By Flexibility' schedules the special-purpose nozzle tips first, and the
        // multi-purpose tips last, as a heuristic to keep all nozzles busy at the end of the job.
        // This is expected to increase utilisation (i.e. minimise cycle count), but the planning
        // cost may be fractionally worse.
        setup("testFlexibility");
        jobProcessor.setJobOrder(ReferencePnpJobProcessor.JobOrderHint.NozzleTipsByFlexibility);
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(3, tipChanges());
        assertEquals(2.0, utilisation()); // Full utilisation has been achieved
        assertEquals(48, cycleCount()); // There are 96 placements on the board, so 48 trips with both nozzles utilised
        assertEquals(1.57, averagePlanningCost(), 0.01); // Efficiency is somewhat worse
        assertEquals(34, partChanges(), 5);
    }

    @Test
    public void testDifferentFeeders() throws Exception {
        // The Feeder Focus option restricts the planner to select parts from the same
        // feeder as the first on its preference list. This causes it to empty one feeder
        // before moving on to the next. This increases planning cost,
        // because the planner has fewer options that it can consider.
        setup("testDifferentFeeders");
        jobProcessor.planner.setFeederStrategy(PnpJobPlanner.FeederStrategy.DifferentFeeders);
        run();
        saveCsv();
        checkRanks();
        assertEquals(3, tipChanges());
        assertEquals(1.60, utilisation(), 0.01);
        assertEquals(60, cycleCount(), 1);
        assertEquals(1.72, averagePlanningCost(), 0.01); // Efficiency worse; maybe worse that we expect?
        assertEquals(24, partChanges());
        checkThatWeNeverLoadTheSamePartOnBothNozzles();
    }

    private void checkThatWeNeverLoadTheSamePartOnBothNozzles() {
        for(PlannerStepResults result: results) {
            if(result.getPlannedPlacements().size()==2)
            {
                assertNotEquals(result.getPlannedPlacements().get(0).jobPlacement.getPlacement().getPart().getId(),
                                result.getPlannedPlacements().get(1).jobPlacement.getPlacement().getPart().getId());
            }
        }
    }

    @Test
    public void testDifferentFeedersFlexibility() throws Exception {
        // Another test for "Different Feeders", using the NozzleTipsByFlexibility to keep all the nozzles
        // busy until the end of the job
        setup("testDifferentFeedersFlexibility");
        jobProcessor.setJobOrder(ReferencePnpJobProcessor.JobOrderHint.NozzleTipsByFlexibility);
        jobProcessor.planner.setFeederStrategy(PnpJobPlanner.FeederStrategy.DifferentFeeders);
        run();
        saveCsv();
        checkRanks();
        assertEquals(3, tipChanges());
        assertEquals(1.88, utilisation(), 0.01); // as optimal as reasonably expected here
        assertEquals(51, cycleCount(), 1);
        assertEquals(1.62, averagePlanningCost(), 0.01); // Efficiency worse
        assertEquals(34, partChanges());
        checkThatWeNeverLoadTheSamePartOnBothNozzles();
    }

    @Test
    public void testRank() throws Exception {
        setup("testRank");
        // For the purpose of this test we use the placement Id to set the rank.
        // For example, R24 is set to rank 24.
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                int rank = Integer.parseInt(placement.getId().substring(1));
                placement.setRank(rank);
            }
        }
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(4, tipChanges());
        assertEquals(1.84, utilisation(), 0.01);
        assertEquals(52, cycleCount(), 1);
        assertEquals(1.42, averagePlanningCost(), 0.01);
        assertEquals(31, partChanges(), 3);
    }

    @Test
    public void testRank2() throws Exception {
        setup("testRank2");
        // This is the reverse order from above
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                int rank = Integer.parseInt(placement.getId().substring(1));
                placement.setRank(-rank);
            }
        }
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(4, tipChanges());
        assertEquals(1.77, utilisation(), 0.01);
        assertEquals(54, cycleCount(), 1);
        assertEquals(1.60, averagePlanningCost(), 0.01);
        assertEquals(31, partChanges(), 3);
    }

    @Test
    public void testRankWeak() throws Exception {
        setup("testRankWeak");
        // This is a validation of a typical "weak ordering" use case.
        // A pair of parts of different height are too close together, and one overshadows the other.
        // One must be placed before the other, therefore they have strict ordering.
        // But the bulk of parts do not matter and have weak ordering with both of the
        // first two groups.
        //
        // Pass condition for the weak ording behaviour is that the first group is placed
        // efficiently near the start of the bulk group, and the second group are placed
        // efficiently near the end of the second group.
        //
        // Fail condition would be that the job planner is forced into some inefficient
        // planning. Perhaps because one of the first group was placed towards the end of
        // the bulk group, delaying the start of the second group which then do not have
        // any efficient pairings with the bulk group.
        //
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                int rank;
                if (placement.getId().equals("R10")) {
                    rank = 5;
                }
                else if (placement.getId().equals("R11")) {
                    rank = -5;
                }
                else {
                    rank = 0;
                }
                placement.setRank(rank);
            }
        }
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(3, tipChanges());
        // Utilisation is identical to the baseline test, indicating that
        // nozzles are still used efficiently.
        assertEquals(1.6, utilisation(), 0.01);
        assertEquals(60, cycleCount(), 1);
        // The overshadowed parts are all placed in the first 4 cycles. This is fine.
        assertEquals(4, lastPlacementPosition("R11"));
        // The overshadowing parts are placed in cycles nicely
        // in the middle of the bulk group.
        assertEquals(7, firstPlacementPosition("R10"));
        assertEquals(21, lastPlacementPosition("R10"));
        // Planning cost is only fractionally worse. This is indicative
        // of the weak ording working ok, and allowing efficient planning.
        assertEquals(1.44, averagePlanningCost(), 0.01);
        assertEquals(17, partChanges(), 3);
    }

    @Test
    public void testRankRounded() throws Exception {
        setup("testRankRounded");
        // For the purpose of this test we use the placement Id rounded to a multiple of 5
        // For example, R24 is set to rank 20. This sets up several groups with equal rank
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                int rank = Integer.parseInt(placement.getId().substring(1));
                placement.setRank(rank-rank%5);
            }
        }
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(5, tipChanges());
        assertEquals(1.65, utilisation(), 0.01);
        assertEquals(58, cycleCount(), 1);
        assertEquals(1.54, averagePlanningCost(), 0.01);
        assertEquals(27, partChanges(), 3);
    }

    @Test
    public void testRankRoundedFlexibility() throws Exception {
        setup("testRankRoundedFlexibility");
        jobProcessor.setJobOrder(ReferencePnpJobProcessor.JobOrderHint.NozzleTipsByFlexibility);
        // For the purpose of this test we use the placement Id rounded to a multiple of 5
        // For example, R24 is set to rank 20. This sets up several groups with equal rank
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                int rank = Integer.parseInt(placement.getId().substring(1));
                placement.setRank(rank-rank%5);
            }
        }
        run();
        saveCsv();
        saveSvg();
        checkRanks();
        assertEquals(4, tipChanges());
        assertEquals(1.84, utilisation(), 0.01); // utilisation is better as expected with NozzleTipsByFlexibility
        assertEquals(52, cycleCount(), 1);
        assertEquals(1.67, averagePlanningCost(), 0.01); // planning cost is a little worse
        assertEquals(23, partChanges(), 3);
    }

    private void setup(String testName) throws Exception {
        this.testName = testName;
        workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/JobProcessorTest/machine.xml"),
            new File(workingDirectory, "machine.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/JobProcessorTest/parts.xml"),
            new File(workingDirectory, "parts.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/JobProcessorTest/packages.xml"),
            new File(workingDirectory, "packages.xml"));

        Configurator
        .currentConfig()
        .level(Level.INFO) // change this for other log levels.
        .activate();

        Configuration.initialize(workingDirectory);
        Configuration.get().load();
        
        machine = (ReferenceMachine) Configuration.get().getMachine();

        // Disable bottom vision so the test runs faster
        ((ReferenceBottomVision) Configuration.get().getMachine().getPartAlignments().get(0)).setEnabled(false);

        SampleJobTest.makeMachineFastest();

        // Register ourselves with EventBus so that we can observe the planner results
        Configuration.get().getBus().register(this);

        jobProcessor = (ReferencePnpJobProcessor) machine.getPnpJobProcessor();
        jobProcessor.addTextStatusListener((text) -> {
            System.out.println(text);
        });

        File jobFile = new File("src/test/resources/config/JobProcessorTest/pnp-test-panelized.job.xml");
        job = Configuration.get().loadJob(jobFile);

        machine.setEnabled(true);
        machine.home();

        results = new ArrayList<PnpJobPlanner.PlannerStepResults>();
    }

    private void run() throws Exception {
        machine.execute(() -> {
            machine.home();
            jobProcessor.initialize(job);
            while (jobProcessor.next()) {
                //spin
            };
            return null;
        }, false, 10000);

        Configuration.get().getBus().unregister(this);
    }

    @Subscribe
    public void observePlacements(PnpJobPlanner.PlannerStepResults result) {
        results.add(result);
    }

    private void saveCsv() throws Exception {
        File csvFile = new File(workingDirectory,testName+".csv");
        Logger.info("Saving placement report to {}",csvFile);
        PrintWriter csv = new PrintWriter(csvFile);
        int cycle = 1;
        for(PlannerStepResults result: results) {
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                csv.format("%1$s,%2$s,%3$s,%4$s,%5$s,%6$s,%7$s\n",
                           cycle,p.nozzle.getId(),p.nozzleTip.getId(),
                           p.jobPlacement.getPlacement().getPart().getId(),
                           p.jobPlacement,p.jobPlacement.getRank(),p.planningCost);
            }
            cycle += 1;
        }
        csv.close();
    }

    private void saveSvg() throws Exception {
        File svgFile = new File(workingDirectory,testName+".svg");
        Logger.info("Saving movement cost map to {}",svgFile);
        PrintWriter out = new PrintWriter(svgFile);
        out.println(asSvg());
        out.close();
    }

    private String asSvg() throws Exception {
        StringBuilder svg1 = new StringBuilder();
        StringBuilder svg2 = new StringBuilder();
        svg1.append("<title>Job Planner "+testName+"</title>\n");
        double minX=0,minY=0,maxX=0,maxY=0;
        for(PlannerStepResults result: results) {
            Location prevLocation = null;
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                Location locationNozzle = Utils2D.calculateBoardPlacementLocation(
                    p.jobPlacement.getBoardLocation(),
                    p.jobPlacement.getPlacement().getLocation());
                Location locationHead = p.nozzle.toHeadLocation(locationNozzle, LocationOption.Quiet);
                String color = p.nozzle.getId().equals("N1")?"red":"black"; // first nozzle is red
                svg2.append("<circle cx=\""+locationHead.getX()+"\" cy=\""+locationHead.getY()+"\" r=\"1\" style=\"stroke:"+color+";fill-opacity:0;\"/>\r\n");
                svg1.append("<line x1=\""+locationHead.getX()+"\" y1=\""+locationHead.getY()+"\" x2=\""+
                            locationNozzle.getX()+"\" y2=\""+locationNozzle.getY()+"\" style=\"stroke:lightgrey\"/>");
                if(prevLocation!=null) {
                    svg1.append("<line x1=\""+prevLocation.getX()+"\" y1=\""+prevLocation.getY()+"\" x2=\""+
                               locationHead.getX()+"\" y2=\""+locationHead.getY()+"\" style=\"stroke:darkgrey;\"/>");

                }
                minX = Math.min(minX,locationHead.getX());
                minY = Math.min(minY,locationHead.getY());
                maxX = Math.max(maxX,locationHead.getX());
                maxY = Math.max(maxY,locationHead.getY());
                prevLocation = locationHead;
            }
        }
        double margin = 10;
        minX -= margin;
        minY -= margin;
        maxX += margin;
        maxY += margin;
        svg2.append("</svg>\n");
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                   "<svg xmlns=\"http://www.w3.org/2000/svg\"\n" +
                   "  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
                   "  version=\"1.1\" baseProfile=\"full\"\n" +
                   "  width=\"100%\" height=\"100%\"\n"+
                   "  viewBox=\""+minX+" "+minY+" "+(maxX-minX)+" "+(maxY-minY)+"\">\r\n";
        return header + svg1.toString() + svg2.toString();
    }

    private void dumpResults() {
        for(PlannerStepResults result: results) {
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                Logger.info("{} {} {} {}",p.nozzle,p.nozzleTip,p.jobPlacement,p.planningCost);
            }
        }
    }

    // The machine has 2 nozzles.
    // A return value of 2.0 indicates that both nozzles were in use on every cycle.
    // A smaller value indicates that some cycles only used one nozzle.
    private double utilisation() {
        double r = 0;
        for(PlannerStepResults result: results) {
            r += result.getPlannedPlacements().size();
        }
        return r/results.size();
    }

    // The number of planning cycles. That is, the number of trips between feeder, vision, placement, and back.
    // On a machine with 1 nozzle, this is just the same as part count.
    // On a machine with 2 nozzles, this is effectively measuring the same thing as "utilisation" above.
    private int cycleCount() {
        return results.size();
    }

    // The "planning cost" is a measure of movement time between feeders and placements on multi-nozzle machines
    private double averagePlanningCost() {
        double r = 0;
        int count = 0;
        for(PlannerStepResults result: results) {
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                if(p.planningCost!=null) {
                    r += p.planningCost;
                    count += 1;
                }
            }
        }
        // makeMachineFastest gives approximately 100x speedup,
        // so here we multiple the average planning travel time by
        // the same factor to give intuitive numbers
        if(count==0) {
            return 0;
        } else {
            return r * 100 / count;
        }
    }

    // How many times does the job planner swap from one part (feeder) to another?
    // If it clears one feeder before moving on then this function will return
    // the value 8, which is the number of unique parts in the test job.
    private int partChanges() {
        int r = 0;
        HashSet <Part> prev = new HashSet <Part>();
        for(PlannerStepResults result: results) {
            HashSet <Part> next = new HashSet <Part>();
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                Part part = p.jobPlacement.getPlacement().getPart();
                if(!prev.contains(part) && !next.contains(part)) {
                    r += 1;
                }
                next.add(part);
            }
            prev = next;
        }
        return r;
    }

    // How many times does the job planner load or change a tip.
    // Note this simulated machine starts with no tips loaded.
    private int tipChanges() {
        int r = 0;
        HashMap <Nozzle,NozzleTip> tips = new HashMap <Nozzle,NozzleTip>();
        for(PlannerStepResults result: results) {
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                if(tips.getOrDefault(p.nozzle,null)!=p.nozzleTip) {
                    r += 1;
                    tips.put(p.nozzle,p.nozzleTip);
                }
            }
        }
        return r;
    }

    // This checks the strict rank rule that placements at rank R
    // will definitely be placed before any at rank R+10. This rule
    // applies to all test jobs, although it is satisfied trivially
    // by those which do not have any rank values set.
    //
    // For tests which set rank values we need to review csv files
    // to manually confirm that the weak rank ordering has been
    // adequately achieved
    private void checkRanks() throws Exception {
        int working_rank = -1000;
        for(PlannerStepResults result: results) {
            int next_rank = working_rank;
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                int r = p.jobPlacement.getPlacement().getRank();
                if(r>next_rank) {
                    next_rank = r;
                }
                if(r<=working_rank-10) {
                    throw new Exception("rank error");
                }
            }
            working_rank = next_rank;
        }
    }

    // Return the cycle number when this placement was first placed
    private int firstPlacementPosition(String placementId) throws Exception {
        int cycle = 1;
        for(PlannerStepResults result: results) {
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                String id = p.jobPlacement.getPlacement().getId();
                if(id.equals(placementId)) {
                    return cycle;
                }
            }
            cycle += 1;
        }
        throw new Exception("placement not found");
    }

    // Return the cycle number when this placement was last placed
    private int lastPlacementPosition(String placementId) throws Exception {
        int cycle = 1;
        int r = -1;
        for(PlannerStepResults result: results) {
            for(PlannedPlacement p : result.getPlannedPlacements() ) {
                String id = p.jobPlacement.getPlacement().getId();
                if(id.equals(placementId)) {
                    r = cycle;
                }
            }
            cycle += 1;
        }
        if(r==-1) {
            throw new Exception("placement not found");
        }
        return r;
    }
}
