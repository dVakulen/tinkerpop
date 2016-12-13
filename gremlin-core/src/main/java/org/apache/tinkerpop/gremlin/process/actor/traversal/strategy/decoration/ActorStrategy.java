/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.tinkerpop.gremlin.process.actor.traversal.strategy.decoration;

import org.apache.tinkerpop.gremlin.process.actor.Actors;
import org.apache.tinkerpop.gremlin.process.actor.traversal.step.map.ActorStep;
import org.apache.tinkerpop.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy;
import org.apache.tinkerpop.gremlin.process.remote.traversal.strategy.decoration.RemoteStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.InjectStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.EmptyStep;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.ReadOnlyStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.VerificationException;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.structure.Partitioner;

import java.util.Collections;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class ActorStrategy extends AbstractTraversalStrategy<TraversalStrategy.DecorationStrategy>
        implements TraversalStrategy.DecorationStrategy {


    private static final Set<Class<? extends DecorationStrategy>> PRIORS = Collections.singleton(RemoteStrategy.class);
    private static final Set<Class<? extends DecorationStrategy>> POSTS = Collections.singleton(VertexProgramStrategy.class);

    private final Partitioner partitioner;
    private final Class<? extends Actors> actors;

    public ActorStrategy(final Class<? extends Actors> actors, final Partitioner partitioner) {
        this.actors = actors;
        this.partitioner = partitioner;
    }

    @Override
    public void apply(final Traversal.Admin<?, ?> traversal) {
        ReadOnlyStrategy.instance().apply(traversal);
        if (!TraversalHelper.getStepsOfAssignableClass(InjectStep.class, traversal).isEmpty())
            throw new VerificationException("Inject traversal currently not supported", traversal);

        if (!(traversal.getParent() instanceof EmptyStep))
            return;

        final ActorStep<?, ?> actorStep = new ActorStep<>(traversal, this.actors, this.partitioner);
        TraversalHelper.removeAllSteps(traversal);
        traversal.addStep(actorStep);

        // validations
        assert traversal.getStartStep().equals(actorStep);
        assert traversal.getSteps().size() == 1;
        assert traversal.getEndStep() == actorStep;
    }

    @Override
    public Set<Class<? extends DecorationStrategy>> applyPost() {
        return POSTS;
    }

    @Override
    public Set<Class<? extends DecorationStrategy>> applyPrior() {
        return PRIORS;
    }
}
