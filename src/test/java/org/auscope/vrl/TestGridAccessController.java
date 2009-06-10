package org.auscope.vrl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestGridAccessController {

    private GridAccessController gridAccess;

    @Before
    public void setUp() {
        gridAccess = new GridAccessController();
    }

    @Test
    public void testIsProxyValid() {
        Assert.assertFalse(gridAccess.isProxyValid());
    }

}

