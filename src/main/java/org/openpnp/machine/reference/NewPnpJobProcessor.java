package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.List;

public class NewPnpJobProcessor {
    /**
     * A simple command that performs one small unit of work. Returns true if it is done
     * or false if it has more to do.
     * 
     * TODO STOPSHIP: If a command had a name then it would give us a nice piece of status
     *                to report for each step.
     */
    public interface Command {
        public boolean execute() throws Exception;
    }

    public static class CompoundCommand implements Command {
        final List<Command> commands = new ArrayList<>();
        int index;
        
        public CompoundCommand(Command... commands) {
            for (Command command : commands) {
                this.commands.add(command);
            }
        }
        
        public boolean execute() throws Exception {
            Command command = commands.get(index);
            if (command.execute()) {
                index++;
            }
            return index >= commands.size();
        }
        
        @Override
        public String toString() {
            return String.format("CompoundCommand(%s)", commands);
        }
    }

    /**
     * RetryCommand is a thin wrapper around a Command which will retry the same command
     * a number of times until it either runs without Exception or exceeds the number of
     * retries.
     */
    public static class RetryCommand implements Command {
        final Command command;
        final int maxRetries;
        int tryCount;
        
        public RetryCommand(int maxRetries, Command command) {
            this.command = command;
            this.maxRetries = maxRetries;
        }
        
        public boolean execute() throws Exception {
            /**
             * Execute the command. If it throws an exception, increment the
             * retry counter and return false. If it does not throw, reset the
             * retry counter and return the command return value. Once the
             * retry counter exceeds the max retries, reset the counter and
             * throw the error to the caller.
             */
            try {
                boolean done = command.execute();
                if (done) {
                    tryCount = 0;
                }
                return done;
            }
            catch (Exception e) {
                tryCount++;
                if (tryCount > maxRetries) {
                    System.out.format("%s failed %d times, giving up.\n", command, tryCount);
                    tryCount = 0;
                    throw e;
                }
                else {
                    System.out.format("%s failed %d times, retrying.\n", command, tryCount);
                    return false;
                }
            }
        }
        
        @Override
        public String toString() {
            return String.format("RetryCommand(%s)", command);
        }
    }
    
    public static class StringCommand implements Command {
        final String s;
        
        public StringCommand(String s) {
            this.s = s;
        }
        
        public boolean execute() throws Exception {
            if (s.equals("Feed")) {
                throw new Exception(toString());
            }
            System.out.println(s);
            return true;
        }
        
        @Override
        public String toString() {
            return String.format("StringCommand(%s)", s);
        }
    }
    
    /**
     * Okay, this is all looking pretty good. Next up is to try to stub in actual Job stuff and
     * see how the data is going to flow.
     * 
     * There is also the question of error handling. I just realized that Alert and Defer are bad
     * names for what I actually want to accomplish. The right names are Stop and Continue. And
     * really those are specific cases for the more general case of Layers. By assigning layers
     * you can easily decide when the machine will stop or continue, and you can enforce ordering.
     * 
     * Okay, all that aside, I think those are details and the general error handling system will
     * work for all those cases. So the real questions are:
     * 
     * - How and when do we mark a feeder errored?
     * - How and when do we mark a part errored?
     * - How do we handle things like post pick which has to happen regardless of whether
     *   the vacuum check is good.
     *   - Quick thought on that: The post pick should happen after lift and before vacuum check. 
     * - Can we just collapse all the tree based retries into a single part retry? So we'd just
     *   have feeder.feedRetry, feeder.pickRetry, part.retry?  
     *   
     *   
     * So, priorities (layers) and inspect instead of alert / defer. 
     * 
     * vonniedaToday at 9:15 PM
        Well, I haven't thought that through yet. But my thought is that when you reach the end of a priority level you get a dialog with all the current errored parts, and you can fix them and run again or choose to continue to the next priority, essentially skipping those placements.
        cncmachineguyToday at 9:15 PM
        Yes, PERFECT!!
        
        
        Hi Jason, PMing so as to try not to illiciet tons of opinions in case you are not looking for them.  So in my opinion, there are 3 places for errors.
        1- pick error - this is if no part is detected on the nozzle when picking.
        2- vision error - this is when part is not found to be correct - could be missing, tombstoned, etc
        3- place error - I am not clear on this but seemed to be wanted by folks. How to handle I am not clear. I suppose trash the part and try again?
        
        So the pick error should retry without moving away for number of retries set in the part setup
        vision error flags error but checks other nozzles. Now tyrash the part either pre or post placement of the rest - chosen in the machine setup. this depends on trash can location
        
        Is this what you are thinking? 
        Again I PM because prolly you are not looking for input from 30 people about errors that will only ever occur on their machine. Happy to take this back public if you like.        
     */
    public static void main(String[] args) throws Exception {
        int feedRetryMax = 1;
        int alignRetryMax = 1;
        
        Command command = new CompoundCommand(
                new StringCommand("PreFlight"),
                new StringCommand("PanelFidCheck"),
                new StringCommand("BoardFidCheck"),
                // This will be a custom command that returns false if no more parts are to be placed
                new CompoundCommand(
                        new StringCommand("Plan"),
                        new StringCommand("LoadNozzleTips"),
                        new StringCommand("CalibrateNozzleTips"),
                        new StringCommand("CheckPartOff"),
                        // FeedPickCheck: Errors a part due to repeated pick failures.
                        new RetryCommand(1, new CompoundCommand(
                                // Errors a feeder due to repeated feed failures.
                                new RetryCommand(feedRetryMax, new StringCommand("Feed")),
                        
                                // PickCheck: Repeats picks in case of mispick.
                                new CompoundCommand(
                                        new StringCommand("Pick"), // Also includes post pick regardless of pick error.
                                        new StringCommand("CheckPartOn")
                                )
                        )),

                        // TODO STOPSHIP: When and how do we disable a part if the alignment keeps failing?
                        new RetryCommand(alignRetryMax, new StringCommand("Align")),
                        new StringCommand("CheckPartOn"),
    
                        new StringCommand("MoveToPlace"),
                        new StringCommand("CheckPartOn"),
                        new StringCommand("Place")
                ),
                new StringCommand("Cleanup"),
                new StringCommand("Park")
            );
        while (!command.execute());
    }
    
    /**
     * So the retries we have are:
     * 
     * part.feedPickAlignRetry:
     *      - Checked when any part of the feed-pick-align cycle fails.
     *      - Will primarily be triggered by vision alignment indicating an error, such as bad size.
     *      - Avoid endlessly emptying a feeder if the pick is okay but the vision fails.
     *      - Use case: Tombstone
     *          - feeder feeds
     *          - part is picked
     *          - vacuum check passes
     *          - alignment fails (bad size) (3x)
     *          - discard the part and repeat the whole cycle
     *      - Part is errored after all retries.
     *      
     * part.feedPickRetry:
     *      - Checked when either the feed or pick fails.
     *      - Lets us retry a feed pick cycle from the same or next feeder.
     *      - Most likely triggered by a vacuum check error.
     *      - Use case: Empty auto feeder
     *          - feeder feeds, but is empty
     *          - pick is performed, but no part available
     *          - vacuum check fails because of no part
     *          - repeat the feed and pick cycle from the same or next feeder
     *      - Part is errored after all retries.
     *       
     * feeder.feedRetry:
     *      - Checked when feed() fails.
     *      - Mostly for vision based or part counting feeders.
     *      - Feeder is errored after all retries.
     *      
     * part.pickAndCheckRetry:
     *      - Checked when the pick or part on check fails.
     *      - Most likely due to a empty feeder / vacuum check.
     *      - Lets us retry picking from the same feeder.
     *      - Think of this as a mispick retry.
     *      - Use case: Empty auto feeder
     *          - feeder feeds, but is empty
     *          - pick is performed, but no part available
     *          - vacuum check fails because of no part
     *          - repeat the pick and check cycle from the same feeder
     *      - I don't think this results in erroring the part or the feeder. That is handled by the
     *        higher level functions. 
     *      
     * part.alignRetry:
     *      - Checked when alignment fails.
     *      - Most likely due to tombstone detection / part size doesn't match.
     *      - But could be a bad pipeline configuration.
     *      - Or just bad lighting conditions.
     *      - Lets us retry the vision op in the hope that the problem was temporary.
     *        If the pipeline is well configured we should get the same result each time, so if
     *        we're detecting a real problem like a tombstone we should detect that each time and
     *        eventually indicate it.
     *      - This is a low level sensor, so it does not error the part or the feeder. That will
     *        handled by higher level processes.
     *        
     * TODO: Use Case: Feeders are fine, but nozzle vacuum settings are off, so every check fails.
     *      - We would pick 3x from each of 3 feeders for the part before the part was errored by
     *        feedPickRetry
     *      
     * TODO: Use Case: Auto feeder is empty, but there is another for the same part.
     *      - we would feed-pick-check the empty feeder 3x, then feedPickRetry would
     *        move us on to the next feeder.
     *      - root cause is vacuum error
     *        
     * TODO: Use Case: Strip feeder is empty, but there is another for the same part.
     *      - feed would fail 3x and then feedPickRetry would move us on the next
     *        feeder.
     *      - root cause is feed() vision error.
     *      
     * TODO: Use Case: Tombstone detected by vision, due to single poor pick.
     *      - feedPickAlignRetry would retry the entire feed-pick-check cycle.
     *      - next part would pass.
     *        
     * TODO: Use Case: Tombstone detected by vision, due to repeated wrong size nozzle.
     *      - feedPickAlignRetry would execute 3x potentially feeding across several feeders
     *      - part would be errored.
     *      - IDEA: the image showing the problem should be linked to the error.
     *      
     * TODO: Use case: Tombstone detected by vacuum, due to repeated wrong size nozzle.
     *      - feedPickRetry would execute 3x and eventually fail the part.
     */
}
