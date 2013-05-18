package net.pms.util.universalchardet.prober.statemachine;

import static org.junit.Assert.*;

import org.junit.Test;

public class PkgIntTest
{
    @Test
    public void testUnpack()
    {
        int[] data = new int[] {
                PkgInt.pack4bits(0, 1, 2, 3, 4, 5, 6, 7),
                PkgInt.pack4bits(8, 9, 10, 11, 12, 13, 14, 15)
        };
        
        PkgInt pkg = new PkgInt(
                PkgInt.INDEX_SHIFT_4BITS,
                PkgInt.SHIFT_MASK_4BITS,
                PkgInt.BIT_SHIFT_4BITS,
                PkgInt.UNIT_MASK_4BITS,
                data);

        for (int i=0; i<16; ++i) {
            int n = pkg.unpack(i);
            if (n != i) {
                fail("Invalid packed value.");
            }
        }
    }
}
