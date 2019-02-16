package org.qcri.rheem.core.optimizer.enumeration;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.mloptimizer.LoadModel;
import org.qcri.rheem.core.optimizer.mloptimizer.LogGenerator;
import org.qcri.rheem.core.optimizer.mloptimizer.MLestimation;
import org.qcri.rheem.core.optimizer.mloptimizer.api.Tuple2;
import org.qcri.rheem.core.plan.rheemplan.ExecutionOperator;
import org.qcri.rheem.core.plan.rheemplan.Slot;
import org.qcri.rheem.core.platform.Platform;
import org.qcri.rheem.core.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This {@link PlanEnumerationPruningStrategy} follows the idea that we can prune a
 * {@link PlanImplementation}, when there is a further one that is (i) better and (ii) has the exact same
 * operators with still-to-be-connected {@link Slot}s.
 */
public class LatentOperatorPruningStrategy implements PlanEnumerationPruningStrategy {

    private static Configuration configuration = new Configuration();
    private static Logger logger = LoggerFactory.getLogger(LatentOperatorPruningStrategy.class);
    private static LogGenerator logGenerator = new LogGenerator();

    @Override
    public void configure(Configuration configuration) {
    }

    @Override
    public void prune(PlanEnumeration planEnumeration) {
        // Skip if there is nothing to do...
        if (planEnumeration.getPlanImplementations().size() < 2) return;

        // Group plans.
        final Collection<List<PlanImplementation>> competingPlans =
                planEnumeration.getPlanImplementations().stream()
                        .collect(Collectors.groupingBy(LatentOperatorPruningStrategy::getInterestingProperties))
                        .values();

        // get the best plan for each grouped plan!
        final List<PlanImplementation> bestPlans = competingPlans.stream()
                .map(this::selectBestPlanNary)
                .collect(Collectors.toList());
        planEnumeration.getPlanImplementations().retainAll(bestPlans);
    }

    @Override
    public void setLog(LogGenerator loggenerator) {
        logGenerator = loggenerator;
    }

    /**
     * Extracts the interesting properties of a {@link PlanImplementation}.
     *
     * @param implementation whose interesting properties are requested
     * @return the interesting properties of the given {@code implementation}
     */
    private static Tuple<Set<Platform>, Collection<ExecutionOperator>> getInterestingProperties(PlanImplementation implementation) {
        return new Tuple<>(
                implementation.getUtilizedPlatforms(),
                implementation.getInterfaceOperators()
        );
    }

    private PlanImplementation selectBestPlanNary(List<PlanImplementation> planImplementation) {
        assert !planImplementation.isEmpty();
        if (configuration.getBooleanProperty("rheem.core.optimizer.mloptimizer",false)) {
            return selectBestLearnedPlan(planImplementation);
        } else {
            // stream in the case of cost optimizer
            return planImplementation.stream()
                    .reduce(this::selectBestPlanBinary)
                    .orElseThrow(() -> new RheemException("No plan was selected."));
        }
    }

    private PlanImplementation selectBestLearnedPlan(List<PlanImplementation> planImplementations) {
        logGenerator.reinitializepruningLogs();

        //logGenerator =new LogGenerator();
        // add operators
        //logGenerator.

        List<Tuple<PlanImplementation,double[]>> tupleIplementationFeatureVector = new ArrayList<>();
        planImplementations.stream()
                .forEach(planImplementation-> {
                    tupleIplementationFeatureVector.add(new Tuple<>(planImplementation,logGenerator.addPruningFeatureLog(planImplementation)));
                });

        // Load Model
        LoadModel.loadModel(logGenerator.getPruningFeatureLogs());

        Tuple2<double[],Double> bestFeatureVector =  MLestimation.getBestVector(logGenerator.getPruningFeatureLogs());

        return tupleIplementationFeatureVector.stream().filter(t1->t1.field1==bestFeatureVector.getField0()).findAny().orElse(null).field0;
    }
    private PlanImplementation selectBestPlanBinary(PlanImplementation p1,
                                                    PlanImplementation p2) {

        final double   t1 = p1.getSquashedCostEstimate(true);
        final double   t2 = p2.getSquashedCostEstimate(true);

        final boolean isPickP1 = t1 <= t2;
        if (logger.isDebugEnabled()) {
            if (isPickP1) {
                LoggerFactory.getLogger(LatentOperatorPruningStrategy.class).debug(
                        "{} < {}: Choosing {} over {}.", p1.getTimeEstimate(), p2.getTimeEstimate(), p1.getOperators(), p2.getOperators()
                );
            } else {
                LoggerFactory.getLogger(LatentOperatorPruningStrategy.class).debug(
                        "{} < {}: Choosing {} over {}.", p2.getTimeEstimate(), p1.getTimeEstimate(), p2.getOperators(), p1.getOperators()
                );
            }
        }
        return isPickP1 ? p1 : p2;
    }

}
