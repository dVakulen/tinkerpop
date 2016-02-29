/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.SideEffectCapable;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.SupplyingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class SideEffectCapStep<S, E> extends SupplyingBarrierStep<S, E> {

    private List<String> sideEffectKeys;
    public transient Map<String, SideEffectCapable<Object, E>> sideEffectFinalizer;

    public SideEffectCapStep(final Traversal.Admin traversal, final String sideEffectKey, final String... sideEffectKeys) {
        super(traversal);
        this.sideEffectKeys = new ArrayList<>(1 + sideEffectKeys.length);
        this.sideEffectKeys.add(sideEffectKey);
        for (final String key : sideEffectKeys) {
            this.sideEffectKeys.add(key);
        }
    }

    @Override
    public String toString() {
        return StringFactory.stepString(this, this.sideEffectKeys);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (final String sideEffectKey : this.sideEffectKeys) {
            result ^= sideEffectKey.hashCode();
        }
        return result;
    }

    public List<String> getSideEffectKeys() {
        return this.sideEffectKeys;
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        return Collections.singleton(TraverserRequirement.SIDE_EFFECTS);
    }

    @Override
    protected E supply() {
        if (null == this.sideEffectFinalizer) {
            this.sideEffectFinalizer = new HashMap<>();
            for (final String key : this.sideEffectKeys) {
                // TODO: TraversalHelper.getRootTraversal() -- or SideEffectCapStepStrategy
                for (final SideEffectCapable<Object, E> sideEffectStep : TraversalHelper.getStepsOfAssignableClassRecursively(SideEffectCapable.class, this.getTraversal())) {
                    this.sideEffectFinalizer.put(key, sideEffectStep);
                }
            }
        }
        ////////////
        if (this.sideEffectKeys.size() == 1) {
            final String sideEffectKey = this.sideEffectKeys.get(0);
            final E result = this.getTraversal().getSideEffects().<E>get(sideEffectKey).get();
            final SideEffectCapable<Object, E> sideEffectCapable = this.sideEffectFinalizer.get(sideEffectKey);
            return null == sideEffectCapable ? result : sideEffectCapable.generateFinalResult(result);
        } else
            return (E) this.getMapOfSideEffects();
    }

    private Map<String, Object> getMapOfSideEffects() {
        final Map<String, Object> sideEffects = new HashMap<>();
        for (final String sideEffectKey : this.sideEffectKeys) {
            this.getTraversal().asAdmin().getSideEffects().get(sideEffectKey).ifPresent(value -> {
                final SideEffectCapable<Object, E> sideEffectCapable = this.sideEffectFinalizer.get(sideEffectKey);
                sideEffects.put(sideEffectKey, null == sideEffectCapable ? value : sideEffectCapable.generateFinalResult(value));
            });
        }
        return sideEffects;
    }

    @Override
    public SideEffectCapStep<S, E> clone() {
        final SideEffectCapStep<S, E> clone = (SideEffectCapStep<S, E>) super.clone();
        clone.sideEffectFinalizer = null;
        return clone;
    }
}
