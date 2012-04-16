package org.eclipse.linuxtools.ctf.core.tests.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteOrder;

import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.Encoding;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.SequenceDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.SequenceDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.StructDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.StructDefinition;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.internal.ctf.core.event.io.BitBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>SequenceDefinitionTest</code> contains tests for the class
 * <code>{@link SequenceDefinition}</code>.
 *
 * @author ematkho
 * @version $Revision: 1.0 $
 */
public class SequenceDefinitionTest {

    private SequenceDefinition fixture;
    private final static int seqLen = 15;

    /**
     * Launch the test.
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(SequenceDefinitionTest.class);
    }

    /**
     * Perform pre-test initialization.
     * @throws CTFReaderException 
     */
    @Before
    public void setUp() throws CTFReaderException {
        StructDeclaration structDec;
        StructDefinition structDef;

        IntegerDeclaration id = new IntegerDeclaration(8, false, 8,
                ByteOrder.LITTLE_ENDIAN, Encoding.UTF8, null);
        String lengthName = "LengthName"; //$NON-NLS-1$
        structDec = new StructDeclaration(0);
        structDec.addField(lengthName, id);
        structDef = new StructDefinition(structDec, null, "x"); //$NON-NLS-1$

        structDef.lookupInteger(lengthName).setValue(seqLen);
        SequenceDeclaration sd = new SequenceDeclaration(lengthName, id);
        fixture = new SequenceDefinition(sd, structDef, "TestX"); //$NON-NLS-1$
        BitBuffer input = new BitBuffer(
                java.nio.ByteBuffer.allocateDirect(seqLen * 8));
        for (int i = 0; i < seqLen; i++) {
            input.putInt(i);
        }
        fixture.read(input);
        assert (fixture != null);
    }

    /**
     * Perform post-test clean-up.
     */
    @After
    public void tearDown() {
        // Add additional tear down code here
    }

    private static SequenceDefinition initNonString() throws CTFReaderException {
        StructDeclaration structDec;
        StructDefinition structDef;

        int len = 32;
        IntegerDeclaration id = new IntegerDeclaration(len, false, len,
                ByteOrder.LITTLE_ENDIAN, Encoding.UTF8, null);
        String lengthName = "LengthName"; //$NON-NLS-1$
        structDec = new StructDeclaration(0);
        structDec.addField(lengthName, id);
        structDef = new StructDefinition(structDec, null, "x"); //$NON-NLS-1$

        structDef.lookupInteger(lengthName).setValue(seqLen);
        SequenceDeclaration sd = new SequenceDeclaration(lengthName, id);
        SequenceDefinition ret = new SequenceDefinition(sd, structDef, "TestX"); //$NON-NLS-1$
        BitBuffer input = new BitBuffer(
                java.nio.ByteBuffer.allocateDirect(seqLen * len));
        for (int i = 0; i < seqLen; i++) {
            input.putInt(i);
        }
        ret.read(input);
        assertNotNull(ret);
        return ret;
    }

    /**
     * Run the SequenceDefinition(SequenceDeclaration,DefinitionScope,String)
     * constructor test.
     */
    @Test
    public void testSequenceDefinition() {
        assertNotNull(fixture);
    }

    /**
     * Run the SequenceDeclaration getDeclaration() method test.
     */
    @Test
    public void testGetDeclaration() {
        SequenceDeclaration result = fixture.getDeclaration();
        assertNotNull(result);
    }

    /**
     * Run the Definition getElem(int) method test.
     */
    @Test
    public void testGetElem() {
        int i = 1;
        Definition result = fixture.getElem(i);
        assertNotNull(result);
    }

    /**
     * Run the int getLength() method test.
     */
    @Test
    public void testGetLength() {
        int result = fixture.getLength();

        assertEquals(seqLen, result);
    }

    /**
     * Run the boolean isString() method test.
     */
    @Test
    public void testIsString() {
        boolean result = fixture.isString();
        assertTrue(result);
    }

    /**
     * Run the void read(BitBuffer) method test.
     */
    @Test
    public void testRead() {
        BitBuffer input = new BitBuffer(java.nio.ByteBuffer.allocateDirect(128));
        fixture.read(input);
    }

    /**
     * Run the String toString() method test.
     */
    @Test
    public void testToString() {
        String result = fixture.toString();
        assertNotNull(result);
    }

    /**
     * Run the String toString() method test.
     */
    @Test
    public void testToString_nonString() throws Exception {
        SequenceDefinition nonStringFixture = initNonString();
        String result = nonStringFixture.toString();
        assertNotNull(result);
    }
}
