package me.hugmanrique.taskgroup.workstealing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class Test {

    public class Region {
        private int count;

        Callable<TickResult> getTickTask() {
            return () -> {
                count++;

                return new TickResult(0);
            };
        }
    }

    public class TickResult {
        int entitiesTicked;

        public TickResult(int entitiesTicked) {
            this.entitiesTicked = entitiesTicked;
        }
    }

    public class World {
        public List<Region> getRegions() {
            return new ArrayList<>();
        }

        public void tick() {
            // We first get a list of all the regions.
            // Next, we ask each region to create its own tick task.
            List<Callable<TickResult>> tasks = getRegions().stream()
                    .map(Region::getTickTask)
                    .collect(Collectors.toList());
        }


    }
}
