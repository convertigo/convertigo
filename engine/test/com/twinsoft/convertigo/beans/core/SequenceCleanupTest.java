/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.beans.core;

import static org.junit.Assert.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import org.junit.Test;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.JsonToXmlStep;

public class SequenceCleanupTest {
    private WeakReference<Step> finish(Sequence sequence) throws Exception {
        var step = new JsonToXmlStep();
        sequence.setCurrentStep(step);
        var reference = new WeakReference<Step>(step);
        for (var name : new String[] {"copies", "childrenSteps"}) {
            var field = Sequence.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(sequence, new HashMap<>());
        }
        var clean = Sequence.class.getDeclaredMethod("clean");
        clean.setAccessible(true);
        clean.invoke(sequence);
        return reference;
    }

    @Test
    public void anIdleContextDoesNotKeepTheLastExecutedStepAndItsEvaluatedValues() throws Exception {
        var sequence = new GenericSequence();
        var reference = finish(sequence);
        for (int i = 0; i < 50 && reference.get() != null; i++) {
            System.gc(); Thread.sleep(20);
        }
        assertNull("The last executed copy can retain evaluated JavaScript values and project classes", reference.get());
        assertNull(sequence.currentStep);
    }
}
