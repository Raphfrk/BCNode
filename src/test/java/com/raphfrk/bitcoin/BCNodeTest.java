package com.raphfrk.bitcoin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class BCNodeTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public BCNodeTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( BCNodeTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testBCNode()
    {
        assertTrue( true );
    }
}
