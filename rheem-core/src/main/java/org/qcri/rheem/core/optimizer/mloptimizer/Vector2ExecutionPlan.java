package org.qcri.rheem.core.optimizer.mloptimizer;

import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.enumeration.PlanImplementation;
import org.qcri.rheem.core.plan.executionplan.ExecutionPlan;

import java.util.Collection;

/**
 * Generate an {@link ExecutionPlan} from a feature vector
 */
public class Vector2ExecutionPlan {
    public static PlanImplementation generate(Collection<PlanImplementation> planImplementations, double[] bestFeatureVector) {

        // convert feature vector into plan implementation
        //plan

        assert !planImplementations.isEmpty();
        final PlanImplementation planImplementation =  planImplementations.stream()
                .reduce((p1,p2)->p1)
                .orElseThrow(() -> new RheemException("Could not find an execution Plan!"));
        return planImplementation;
    }
}
